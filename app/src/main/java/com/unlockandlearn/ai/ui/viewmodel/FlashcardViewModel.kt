package com.unlockandlearn.ai.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unlockandlearn.ai.data.local.FlashcardDatabase
import com.unlockandlearn.ai.data.model.Flashcard
import com.unlockandlearn.ai.data.repository.FlashcardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FlashcardRepository
    private val prefs: SharedPreferences = application.getSharedPreferences("unlock_learn_prefs", Context.MODE_PRIVATE)

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

    init {
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

        loadNextOverlayCard()
        updateStreak()
    }

    // Load next random unlearned card for lockscreen display
    fun loadNextOverlayCard() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = unlearnedFlashcards.value
            if (list.isNotEmpty()) {
                val nextCard = list.random()
                _currentOverlayCard.value = nextCard
            } else {
                val fullList = allFlashcards.value
                if (fullList.isNotEmpty()) {
                    _currentOverlayCard.value = fullList.random()
                } else {
                    _currentOverlayCard.value = null
                }
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
        val lastActiveDay = prefs.getString("last_active_day", "")
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
}
