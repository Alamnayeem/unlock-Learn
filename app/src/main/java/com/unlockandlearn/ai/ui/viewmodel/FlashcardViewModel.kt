package com.unlockandlearn.ai.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.unlockandlearn.ai.data.local.FlashcardDatabase
import com.unlockandlearn.ai.data.model.Flashcard
import com.unlockandlearn.ai.data.repository.FlashcardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FlashcardRepository
    private val prefs: SharedPreferences = application.getSharedPreferences("unlock_learn_prefs", Context.MODE_PRIVATE)
    private val securePrefs: SharedPreferences

    // Room Database flows
    val allFlashcards: StateFlow<List<Flashcard>>
    val favoriteFlashcards: StateFlow<List<Flashcard>>
    val unlearnedFlashcards: StateFlow<List<Flashcard>>
    val totalCount: StateFlow<Int>
    val learnedCount: StateFlow<Int>
    val favoriteCount: StateFlow<Int>

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filter results
    val searchResults: StateFlow<List<Flashcard>>

    // Current flashcard shown on lock overlay
    private val _currentOverlayCard = MutableStateFlow<Flashcard?>(null)
    val currentOverlayCard = _currentOverlayCard.asStateFlow()

    private val _isOverlayLoading = MutableStateFlow(true)
    val isOverlayLoading = _isOverlayLoading.asStateFlow()

    // AI Generation states
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress = _generationProgress.asStateFlow()

    private val _generationStatus = MutableStateFlow("")
    val generationStatus = _generationStatus.asStateFlow()

    private var lastSequentialIndex = 0

    init {
        // Initialize secure preferences
        securePrefs = try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                application,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Safe fallback if KeyStore fails on some custom emulators/devices
            application.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE)
        }

        val database = FlashcardDatabase.getDatabase(application, viewModelScope)
        val dao = database.flashcardDao()
        repository = FlashcardRepository(dao)

        allFlashcards = repository.allFlashcards
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        favoriteFlashcards = repository.favoriteFlashcards
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        unlearnedFlashcards = repository.unlearnedFlashcards
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        totalCount = repository.totalCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        learnedCount = repository.learnedCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        favoriteCount = repository.favoriteCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        searchResults = _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.allFlashcards
                } else {
                    repository.searchFlashcards(query)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Launch initial loading & seeding check
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getCountVal()
            if (count == 0) {
                seedTwentyDefaultCards()
                // Wait for the database insert to propagate
                var retries = 0
                while (repository.getCountVal() == 0 && retries < 10) {
                    delay(100)
                    retries++
                }
            }
            loadNextOverlayCard()
            updateStreak()
        }
    }

    // Load next random/sequential unlearned card for lockscreen display based on active mode
    fun loadNextOverlayCard() {
        _isOverlayLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mode = getLearningModeSetting()
                val defaultCategory = getDefaultCategorySetting()
                
                var filteredList = repository.allFlashcards.first()
                
                // Filter by unlearned first for standard, unless we want wrong cards specifically
                if (mode == "Review Wrong Cards") {
                    filteredList = repository.unlearnedFlashcards.first()
                } else if (mode == "Favorites Only") {
                    filteredList = repository.favoriteFlashcards.first()
                } else {
                    // For general, we can prefer unlearned cards to avoid showing learned ones
                    val unlearned = repository.unlearnedFlashcards.first()
                    if (unlearned.isNotEmpty()) {
                        filteredList = unlearned
                    }
                }
                
                // Apply category filter if in Category Mode or if a default category is set
                if (mode == "Category Mode" || (defaultCategory != "All" && defaultCategory.isNotBlank())) {
                    val filterCat = if (mode == "Category Mode") defaultCategory else defaultCategory
                    filteredList = filteredList.filter { it.category.equals(filterCat, ignoreCase = true) }
                }
                
                if (filteredList.isNotEmpty()) {
                    val nextCard = if (mode == "Sequential") {
                        val index = lastSequentialIndex % filteredList.size
                        lastSequentialIndex++
                        filteredList[index]
                    } else {
                        filteredList.random()
                    }
                    _currentOverlayCard.value = nextCard
                } else {
                    // Fallback to all cards if filtered is empty
                    val all = repository.allFlashcards.first()
                    if (all.isNotEmpty()) {
                        _currentOverlayCard.value = all.random()
                    } else {
                        _currentOverlayCard.value = null
                    }
                }
            } catch (e: Exception) {
                _currentOverlayCard.value = null
            } finally {
                _isOverlayLoading.value = false
            }
        }
    }

    // CRUD database actions
    fun insertFlashcard(flashcard: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFlashcard(flashcard)
        }
    }

    fun updateFlashcard(flashcard: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFlashcard(flashcard)
            // If the card being updated was shown in overlay, update its live state too
            if (_currentOverlayCard.value?.id == flashcard.id) {
                _currentOverlayCard.value = flashcard
            }
        }
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFlashcard(flashcard)
            if (_currentOverlayCard.value?.id == flashcard.id) {
                loadNextOverlayCard()
            }
        }
    }

    fun duplicateFlashcard(flashcard: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.duplicateFlashcard(flashcard)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Statistics Logic & Persistent SharedPreferences Settings
    fun incrementTodayCount() {
        val todayKey = getTodayDateKey()
        val currentCount = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, currentCount + 1).apply()
        
        // Add active learning study time (avg 15 seconds per session)
        incrementStudyTime(15)
    }

    fun getTodayCount(): Int {
        return prefs.getInt(getTodayDateKey(), 0)
    }

    private fun incrementStudyTime(seconds: Int) {
        val currentSeconds = prefs.getInt("total_study_seconds", 0)
        prefs.edit().putInt("total_study_seconds", currentSeconds + seconds).apply()
    }

    fun getStudyTimeMinutes(): Int {
        val seconds = prefs.getInt("total_study_seconds", 0)
        return seconds / 60
    }

    fun getStreak(): Int {
        return prefs.getInt("learning_streak", 1)
    }

    private fun updateStreak() {
        val lastActiveDay = prefs.getString("last_active_day", "") ?: ""
        val today = getTodayDateKey()
        
        if (lastActiveDay.isEmpty()) {
            prefs.edit().putString("last_active_day", today).putInt("learning_streak", 1).apply()
            return
        }
        
        if (lastActiveDay != today) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val lastDate = sdf.parse(lastActiveDay)
                val todayDate = sdf.parse(today)
                if (lastDate != null && todayDate != null) {
                    val diff = todayDate.time - lastDate.time
                    val diffDays = diff / (24 * 60 * 60 * 1000)
                    
                    if (diffDays == 1L) {
                        // Consecutive day streak increased
                        val currentStreak = prefs.getInt("learning_streak", 1)
                        prefs.edit().putString("last_active_day", today).putInt("learning_streak", currentStreak + 1).apply()
                    } else if (diffDays > 1L) {
                        // Streak broken
                        prefs.edit().putString("last_active_day", today).putInt("learning_streak", 1).apply()
                    }
                }
            } catch (e: Exception) {
                // Fail-safe
                prefs.edit().putString("last_active_day", today).apply()
            }
        }
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Seeding beautiful default English flashcards
    private suspend fun seedTwentyDefaultCards() {
        val defaultCards = listOf(
            Flashcard(
                title = "Quantum Entanglement",
                front = "What is Quantum Entanglement?",
                back = "A physical phenomenon that occurs when particles share spatial proximity such that the state of each particle cannot be described independently of the others.",
                category = "Physics",
                difficulty = "Hard",
                color = "#8B5CF6",
                emoji = "⚛️",
                example = "The two entangled photons reacted instantaneously over miles of separation.",
                pronunciation = "/ˈkwɒntəm ɪnˈtæŋɡlmənt/",
                notes = "Einstein famously called this 'spooky action at a distance'.",
                tags = "science, quantum, physics"
            ),
            Flashcard(
                title = "Komorebi",
                front = "What does the Japanese word 'Komorebi' refer to?",
                back = "The sunlight filtering through the leaves of trees.",
                category = "Languages",
                difficulty = "Easy",
                color = "#10B981",
                emoji = "🌿",
                example = "Walking through the forest, we enjoyed the beautiful komorebi warming the path.",
                pronunciation = "/ko-mo-reh-bee/",
                notes = "A beautiful untranslatable Japanese word expressing nature's subtle art.",
                tags = "vocabulary, nature, japanese"
            ),
            Flashcard(
                title = "Fibonacci Sequence",
                front = "What is the Fibonacci Sequence?",
                back = "A series of numbers where each number is the sum of the two preceding ones: 0, 1, 1, 2, 3, 5, 8, 13, 21, and so on.",
                category = "Mathematics",
                difficulty = "Medium",
                color = "#3B82F6",
                emoji = "🌀",
                example = "Pinecones and sunflowers show spirals arranged in Fibonacci sequences.",
                pronunciation = "/ˌfɪbəˈnɑːtʃi ˈsiːkwəns/",
                notes = "Closely related to the golden ratio and found repeatedly in biological structures.",
                tags = "math, sequences, biology"
            ),
            Flashcard(
                title = "Photosynthesis",
                front = "How do plants make their own food?",
                back = "Photosynthesis: The process by which green plants use sunlight to synthesize nutrients from CO2 and water, generating oxygen as a byproduct.",
                category = "Biology",
                difficulty = "Easy",
                color = "#10B981",
                emoji = "🍃",
                example = "Chlorophyll is essential for photosynthesis as it absorbs light energy.",
                pronunciation = "/ˌfəʊtəʊˈsɪnθəsɪs/",
                notes = "Without this process, life on Earth wouldn't have oxygen to breathe.",
                tags = "science, biology, plants"
            ),
            Flashcard(
                title = "Mnemonic Device",
                front = "What is a Mnemonic Device?",
                back = "Any learning technique that aids information retention or retrieval in the human memory, like acronyms or rhymes.",
                category = "Psychology",
                difficulty = "Medium",
                color = "#F59E0B",
                emoji = "🧠",
                example = "'ROYGBIV' is a mnemonic device for remembering the colors of the rainbow.",
                pronunciation = "/nɪˈmɒnɪk dɪˈvaɪs/",
                notes = "Helpful for studying complex terms or structures.",
                tags = "memory, study, psychology"
            ),
            Flashcard(
                title = "Ephemeral",
                front = "What does 'Ephemeral' mean?",
                back = "Lasting for a very short time; transient, fleeting, or short-lived.",
                category = "Languages",
                difficulty = "Medium",
                color = "#EC4899",
                emoji = "🌸",
                example = "The beauty of cherry blossoms is ephemeral, lasting only a few days.",
                pronunciation = "/ɪˈfemərəl/",
                notes = "From Greek 'ephemeros', literally 'lasting only a day'.",
                tags = "words, vocabulary, english"
            ),
            Flashcard(
                title = "Schadenfreude",
                front = "What is 'Schadenfreude'?",
                back = "Pleasure derived by someone from another person's misfortune.",
                category = "Languages",
                difficulty = "Hard",
                color = "#EF4444",
                emoji = "😈",
                example = "He felt a twinge of schadenfreude when his rival's project failed.",
                pronunciation = "/ˈʃɑːdnfrɔɪdə/",
                notes = "A compound German word: 'Schaden' (damage/harm) and 'Freude' (joy).",
                tags = "vocabulary, human-nature, german"
            ),
            Flashcard(
                title = "Paradigm Shift",
                front = "What is a Paradigm Shift?",
                back = "A fundamental change in approach or underlying assumptions within a field or system.",
                category = "General",
                difficulty = "Medium",
                color = "#8B5CF6",
                emoji = "🔄",
                example = "The rise of the internet caused a huge paradigm shift in communication.",
                pronunciation = "/ˈpærədaɪm ʃɪft/",
                notes = "Coined by physicist Thomas Kuhn to describe scientific revolutions.",
                tags = "general-knowledge, concepts"
            ),
            Flashcard(
                title = "Golden Ratio",
                front = "What is the Golden Ratio?",
                back = "A mathematical ratio (~1.618) represented by the Greek letter Phi (φ), representing perfect aesthetic proportion.",
                category = "Mathematics",
                difficulty = "Medium",
                color = "#F59E0B",
                emoji = "📐",
                example = "Architects often use the golden ratio to create aesthetically pleasing buildings.",
                pronunciation = "/ˈɡəʊldən ˈreɪʃiəʊ/",
                notes = "Found in the Parthenon, the Mona Lisa, and seashells.",
                tags = "math, design, art"
            ),
            Flashcard(
                title = "Mitochondria",
                front = "What is the role of Mitochondria in a cell?",
                back = "Known as the 'powerhouse of the cell', mitochondria generate most of the chemical energy needed to power cellular reactions.",
                category = "Biology",
                difficulty = "Easy",
                color = "#10B981",
                emoji = "🔋",
                example = "Muscle cells contain high numbers of mitochondria to supply energy for movement.",
                pronunciation = "/ˌmaɪtəʊˈkɒndriə/",
                notes = "The energy produced is stored in a small molecule called ATP.",
                tags = "biology, science, health"
            ),
            Flashcard(
                title = "Gravity",
                front = "What is Gravity?",
                back = "The fundamental force by which all physical bodies with mass attract each other.",
                category = "Physics",
                difficulty = "Easy",
                color = "#3B82F6",
                emoji = "🍎",
                example = "Gravity pulled the apple down from the branch.",
                pronunciation = "/ˈɡrævəti/",
                notes = "Explained by Newton's law of universal gravitation and Einstein's general relativity.",
                tags = "physics, nature, science"
            ),
            Flashcard(
                title = "Synergy",
                front = "What is 'Synergy'?",
                back = "The interaction of two or more agents to produce a combined effect greater than the sum of their separate effects.",
                category = "General",
                difficulty = "Easy",
                color = "#8B5CF6",
                emoji = "🤝",
                example = "Teamwork creates a synergy that makes large tasks easy.",
                pronunciation = "/ˈsɪnədʒi/",
                notes = "Comes from Greek 'synergos', meaning 'working together'.",
                tags = "business, cooperation, concepts"
            ),
            Flashcard(
                title = "Serendipity",
                front = "What does 'Serendipity' mean?",
                back = "The occurrence of events by chance in a happy, fortunate, or beneficial way.",
                category = "Languages",
                difficulty = "Medium",
                color = "#EC4899",
                emoji = "🍀",
                example = "Finding my lost keys while looking for my wallet was pure serendipity.",
                pronunciation = "/ˌserənˈdɪpəti/",
                notes = "Coined by Horace Walpole in 1754 from the fairy tale 'The Three Princes of Serendip'.",
                tags = "words, english, luck"
            ),
            Flashcard(
                title = "Occam's Razor",
                front = "What is the principle of Occam's Razor?",
                back = "The philosophical rule that the simplest explanation is usually the correct one.",
                category = "Philosophy",
                difficulty = "Hard",
                color = "#6B7280",
                emoji = "🪒",
                example = "Rather than assuming a complex conspiracy, Occam's razor suggests it was a simple mistake.",
                pronunciation = "/ˌɒkəmz ˈreɪzə/",
                notes = "Attributed to 14th-century English friar William of Ockham.",
                tags = "philosophy, thinking, logic"
            ),
            Flashcard(
                title = "Absolute Zero",
                front = "What is Absolute Zero?",
                back = "The lowest possible temperature where all thermodynamic motion of particles ceases: exactly 0 Kelvin.",
                category = "Physics",
                difficulty = "Medium",
                color = "#3B82F6",
                emoji = "❄️",
                example = "Scientists have cooled molecules to just a fraction above absolute zero.",
                pronunciation = "/ˌæbsəluːt ˈzɪərəʊ/",
                notes = "At this point, the entropy of a pure crystalline substance is exactly zero.",
                tags = "physics, science, cold"
            ),
            Flashcard(
                title = "Cognitive Dissonance",
                front = "What is Cognitive Dissonance?",
                back = "The psychological discomfort felt by someone holding two or more contradictory beliefs simultaneously.",
                category = "Psychology",
                difficulty = "Hard",
                color = "#F59E0B",
                emoji = "🎭",
                example = "Smoking despite knowing it causes harm creates strong cognitive dissonance.",
                pronunciation = "/ˈkɒɡnətɪv ˈdɪsənəns/",
                notes = "Individuals usually try to resolve it by justifying their actions.",
                tags = "mind, human-behavior, psychology"
            ),
            Flashcard(
                title = "Doppelganger",
                front = "What is a Doppelganger?",
                back = "A non-biologically related look-alike or double of a living person.",
                category = "Languages",
                difficulty = "Medium",
                color = "#EC4899",
                emoji = "👥",
                example = "I saw someone at the station who looked exactly like you - your doppelganger!",
                pronunciation = "/ˈdɒplɡæŋə/",
                notes = "From German, literally meaning 'double-walker'.",
                tags = "vocabulary, german, words"
            ),
            Flashcard(
                title = "Entropy",
                front = "What is Entropy?",
                back = "A measure of the degree of disorder, randomness, or uncertainty in a system.",
                category = "Physics",
                difficulty = "Hard",
                color = "#3B82F6",
                emoji = "🌌",
                example = "The melting of ice into water is an increase in entropy.",
                pronunciation = "/ˈentrəpi/",
                notes = "Can be viewed as nature's natural tendency toward decay and chaos.",
                tags = "physics, thermodynamics, science"
            ),
            Flashcard(
                title = "Metaphor",
                front = "What is a Metaphor?",
                back = "A figure of speech where a word/phrase is applied to an object/action to which it is not literally applicable.",
                category = "Languages",
                difficulty = "Easy",
                color = "#10B981",
                emoji = "✍️",
                example = "'Time is a thief' is a metaphor representing how time slips away unnoticed.",
                pronunciation = "/ˈmetəfə/",
                notes = "Unlike similes, metaphors do not use 'like' or 'as'.",
                tags = "literature, english, words"
            ),
            Flashcard(
                title = "Heliocentrism",
                front = "What is Heliocentrism?",
                back = "The astronomical model in which the Earth and other planets revolve around the Sun at the center.",
                category = "Physics",
                difficulty = "Medium",
                color = "#3B82F6",
                emoji = "☀️",
                example = "Nicolaus Copernicus proposed heliocentrism, challenging geocentrism.",
                pronunciation = "/ˌhiːliəʊˈsentrɪzəm/",
                notes = "Galileo was famously persecuted for defending this model.",
                tags = "astronomy, science, history"
            )
        )
        for (card in defaultCards) {
            repository.insertFlashcard(card)
        }
    }

    // AI Features Settings (Securely using EncryptedSharedPreferences)
    fun getApiKey(): String {
        return securePrefs.getString("gemini_api_key", "") ?: ""
    }

    fun saveApiKey(key: String) {
        securePrefs.edit().putString("gemini_api_key", key).apply()
    }

    fun deleteApiKey() {
        securePrefs.edit().remove("gemini_api_key").apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
    }

    fun saveSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
    }

    // Test Gemini connection with API Key and Model selection
    fun testConnection(apiKey: String, model: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.doOutput = true

                val requestBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": "Hello, answer with 'OK' in a JSON object: {\"status\":\"OK\"}"
                            }
                          ]
                        }
                      ],
                      "generationConfig": {
                        "responseMimeType": "application/json"
                      }
                    }
                """.trimIndent()

                connection.outputStream.use { os ->
                    val input = requestBody.toByteArray(charset("utf-8"))
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    onResult(true, "Connection successful!")
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP error $responseCode"
                    onResult(false, "Failed: $errorText")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    // AI Flashcard Generator
    private fun parseCountFromPrompt(prompt: String): Int {
        val regex = Regex("\\d+")
        val match = regex.find(prompt)
        return match?.value?.toIntOrNull() ?: 10 // default to 10
    }

    fun generateAiFlashcards(prompt: String) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            _generationStatus.value = "Error: Please configure your Gemini API Key in Settings first."
            return
        }

        val totalToGenerate = parseCountFromPrompt(prompt)
        val batchSize = 10
        val numBatches = (totalToGenerate + batchSize - 1) / batchSize

        _isGenerating.value = true
        _generationProgress.value = 0f
        _generationStatus.value = "Starting AI Generation..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var generatedCount = 0
                for (batch in 1..numBatches) {
                    val currentBatchSize = if (batch == numBatches) {
                        totalToGenerate - (batch - 1) * batchSize
                    } else {
                        batchSize
                    }

                    _generationStatus.value = "Generating batch $batch of $numBatches ($currentBatchSize cards)..."
                    
                    val generatedList = fetchCardsFromGemini(prompt, currentBatchSize, batch, apiKey)
                    
                    if (generatedList.isNotEmpty()) {
                        for (card in generatedList) {
                            repository.insertFlashcard(card)
                        }
                        generatedCount += generatedList.size
                    }

                    val progress = (batch.toFloat() / numBatches.toFloat())
                    _generationProgress.value = progress
                }
                
                _generationStatus.value = "Successfully generated $generatedCount cards!"
                _generationProgress.value = 1.0f
                loadNextOverlayCard()
            } catch (e: Exception) {
                _generationStatus.value = "Error: ${e.message}"
            } finally {
                delay(3000)
                _isGenerating.value = false
            }
        }
    }

    private suspend fun fetchCardsFromGemini(userPrompt: String, count: Int, batchNum: Int, apiKey: String): List<Flashcard> {
        val model = getSelectedModel()
        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        
        val systemPrompt = """
            You are an expert AI educational content generator.
            Your task is to generate exactly $count high-quality study flashcards based on this topic/prompt: "$userPrompt".
            This is batch number $batchNum of the generation process, so make sure these flashcards are unique and don't duplicate.
            
            You MUST respond with a valid JSON array of objects representing the flashcards. Do not include any markdown backticks (like ```json) or explanation outside the JSON.
            
            Each flashcard object MUST contain the following fields:
            - "title": A short, clear title/keyword of the card (e.g. the term or question title). Max 4 words.
            - "front": The front of the card. A question or description prompting the user.
            - "back": The back of the card. A detailed, clear explanation or answer.
            - "category": The study category (e.g. Physics, IELTS, Bangla, Biology, General, Programming, BCS, etc.). Max 2 words.
            - "difficulty": Must be exactly one of "Easy", "Medium", or "Hard".
            - "color": A beautiful HEX color code matching the category mood (e.g., "#8B5CF6", "#10B981", "#3B82F6", "#F59E0B", "#EC4899").
            - "emoji": A single illustrative emoji.
            - "pronunciation": Optional pronunciation guide or transcription.
            - "example": An optional illustrative sentence or example usage.
            - "notes": Optional study notes, mnemonic devices, or extra details.
            - "tags": Comma-separated keywords/tags (e.g., "science, space, exam").
        """.trimIndent()

        val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        connection.doOutput = true

        val escapedSystem = systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

        val requestBody = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "$escapedSystem"
                    }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        connection.outputStream.use { os ->
            val input = requestBody.toByteArray(charset("utf-8"))
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val rootJson = JSONObject(responseText)
            val candidates = rootJson.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            var jsonText = parts.getJSONObject(0).getString("text").trim()
            
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.substringAfter("```json").substringAfter("```")
                jsonText = jsonText.substringBeforeLast("```")
            }
            jsonText = jsonText.trim()

            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<Flashcard>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Flashcard(
                        title = obj.optString("title", "Untitled"),
                        front = obj.optString("front", ""),
                        back = obj.optString("back", ""),
                        category = obj.optString("category", "General"),
                        difficulty = obj.optString("difficulty", "Medium"),
                        color = obj.optString("color", "#8B5CF6"),
                        emoji = obj.optString("emoji", "💡"),
                        example = obj.optString("example", ""),
                        pronunciation = obj.optString("pronunciation", ""),
                        notes = obj.optString("notes", ""),
                        tags = obj.optString("tags", ""),
                        favorite = false,
                        learned = false
                    )
                )
            }
            return list
        } else {
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP error $responseCode"
            throw Exception(errorText)
        }
    }

    // Import / Export & Backup / Restore logic
    fun exportToJson(): String {
        val list = allFlashcards.value
        val array = JSONArray()
        for (c in list) {
            val obj = JSONObject()
            obj.put("title", c.title)
            obj.put("front", c.front)
            obj.put("back", c.back)
            obj.put("category", c.category)
            obj.put("difficulty", c.difficulty)
            obj.put("color", c.color)
            obj.put("emoji", c.emoji)
            obj.put("example", c.example)
            obj.put("pronunciation", c.pronunciation)
            obj.put("notes", c.notes)
            obj.put("tags", c.tags)
            obj.put("favorite", c.favorite)
            obj.put("learned", c.learned)
            array.put(obj)
        }
        return array.toString(2)
    }

    fun exportToCsv(): String {
        val list = allFlashcards.value
        val sb = java.lang.StringBuilder()
        sb.append("Title,Front,Back,Category,Difficulty,Color,Emoji,Example,Pronunciation,Notes,Tags,Favorite,Learned\n")
        for (c in list) {
            sb.append("\"${escapeCsv(c.title)}\",")
            sb.append("\"${escapeCsv(c.front)}\",")
            sb.append("\"${escapeCsv(c.back)}\",")
            sb.append("\"${escapeCsv(c.category)}\",")
            sb.append("\"${escapeCsv(c.difficulty)}\",")
            sb.append("\"${escapeCsv(c.color)}\",")
            sb.append("\"${escapeCsv(c.emoji)}\",")
            sb.append("\"${escapeCsv(c.example)}\",")
            sb.append("\"${escapeCsv(c.pronunciation)}\",")
            sb.append("\"${escapeCsv(c.notes)}\",")
            sb.append("\"${escapeCsv(c.tags)}\",")
            sb.append("${c.favorite},")
            sb.append("${c.learned}\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }

    fun importFromJson(jsonStr: String): Int {
        return try {
            val array = JSONArray(jsonStr)
            viewModelScope.launch(Dispatchers.IO) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val card = Flashcard(
                        title = obj.optString("title", "Untitled"),
                        front = obj.optString("front", ""),
                        back = obj.optString("back", ""),
                        category = obj.optString("category", "General"),
                        difficulty = obj.optString("difficulty", "Medium"),
                        color = obj.optString("color", "#8B5CF6"),
                        emoji = obj.optString("emoji", "💡"),
                        example = obj.optString("example", ""),
                        pronunciation = obj.optString("pronunciation", ""),
                        notes = obj.optString("notes", ""),
                        tags = obj.optString("tags", ""),
                        favorite = obj.optBoolean("favorite", false),
                        learned = obj.optBoolean("learned", false)
                    )
                    repository.insertFlashcard(card)
                }
                loadNextOverlayCard()
            }
            array.length()
        } catch (e: Exception) {
            -1
        }
    }

    fun importFromCsv(csvStr: String): Int {
        return try {
            val lines = csvStr.lines()
            if (lines.size <= 1) return 0
            var count = 0
            viewModelScope.launch(Dispatchers.IO) {
                // Skip header line
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) continue
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= 3) {
                        val card = Flashcard(
                            title = tokens.getOrNull(0) ?: "Untitled",
                            front = tokens.getOrNull(1) ?: "",
                            back = tokens.getOrNull(2) ?: "",
                            category = tokens.getOrNull(3) ?: "General",
                            difficulty = tokens.getOrNull(4) ?: "Medium",
                            color = tokens.getOrNull(5) ?: "#8B5CF6",
                            emoji = tokens.getOrNull(6) ?: "💡",
                            example = tokens.getOrNull(7) ?: "",
                            pronunciation = tokens.getOrNull(8) ?: "",
                            notes = tokens.getOrNull(9) ?: "",
                            tags = tokens.getOrNull(10) ?: "",
                            favorite = tokens.getOrNull(11)?.toBoolean() ?: false,
                            learned = tokens.getOrNull(12)?.toBoolean() ?: false
                        )
                        repository.insertFlashcard(card)
                        count++
                    }
                }
                loadNextOverlayCard()
            }
            lines.size - 1
        } catch (e: Exception) {
            -1
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    currentToken.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString().trim())
                currentToken = java.lang.StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim())
        return result
    }

    fun importFromTxt(txtStr: String): Int {
        return try {
            val lines = txtStr.lines()
            var count = 0
            viewModelScope.launch(Dispatchers.IO) {
                for (line in lines) {
                    if (line.isBlank()) continue
                    val parts = line.split("|")
                    if (parts.size >= 2) {
                        val title = parts.getOrNull(0)?.trim() ?: "Untitled"
                        val front = parts.getOrNull(1)?.trim() ?: ""
                        val back = parts.getOrNull(2)?.trim() ?: ""
                        val card = Flashcard(
                            title = title,
                            front = front,
                            back = back,
                            category = "Imported",
                            difficulty = "Medium",
                            color = "#8B5CF6",
                            emoji = "💡"
                        )
                        repository.insertFlashcard(card)
                        count++
                    }
                }
                loadNextOverlayCard()
            }
            lines.size
        } catch (e: Exception) {
            -1
        }
    }

    fun backupData(): String {
        return exportToJson()
    }

    fun restoreData(backupStr: String): Boolean {
        return try {
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearAll()
                delay(300)
                importFromJson(backupStr)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Play Haptic & Sound Effects helpers
    fun getSoundSetting(): Boolean = prefs.getBoolean("sound_effects_enabled", true)
    fun setSoundSetting(enabled: Boolean) = prefs.edit().putBoolean("sound_effects_enabled", enabled).apply()

    fun getHapticSetting(): Boolean = prefs.getBoolean("haptic_feedback_enabled", true)
    fun setHapticSetting(enabled: Boolean) = prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()

    fun triggerFlipSoundAndHaptic(context: Context) {
        if (getSoundSetting()) {
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    fun triggerUnlockSoundAndHaptic(context: Context) {
        if (getSoundSetting()) {
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    // Settings Values
    fun getThemeSetting(): String = prefs.getString("theme", "Dark") ?: "Dark"
    fun setThemeSetting(theme: String) = prefs.edit().putString("theme", theme).apply()

    fun getAnimationSpeedSetting(): Float = prefs.getFloat("animation_speed", 1.0f)
    fun setAnimationSpeedSetting(speed: Float) = prefs.edit().putFloat("animation_speed", speed).apply()

    fun getDefaultCategorySetting(): String = prefs.getString("default_category", "All") ?: "All"
    fun setDefaultCategorySetting(category: String) = prefs.edit().putString("default_category", category).apply()

    fun getFlashcardOrderSetting(): String = prefs.getString("flashcard_order", "Newest First") ?: "Newest First"
    fun setFlashcardOrderSetting(order: String) = prefs.edit().putString("flashcard_order", order).apply()

    fun getDailyGoalSetting(): Int = prefs.getInt("daily_goal", 10)
    fun setDailyGoalSetting(goal: Int) = prefs.edit().putInt("daily_goal", goal).apply()

    fun getLearningModeSetting(): String = prefs.getString("learning_mode", "Random") ?: "Random"
    fun setLearningModeSetting(mode: String) = prefs.edit().putString("learning_mode", mode).apply()
}
