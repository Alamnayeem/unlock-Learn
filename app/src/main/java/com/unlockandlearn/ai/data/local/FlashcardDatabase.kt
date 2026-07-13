package com.unlockandlearn.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unlockandlearn.ai.data.model.Flashcard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Flashcard::class], version = 2, exportSchema = false)
abstract class FlashcardDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao

    companion object {
        @Volatile
        private var INSTANCE: FlashcardDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FlashcardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlashcardDatabase::class.java,
                    "unlock_and_learn_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(FlashcardDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class FlashcardDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.flashcardDao())
                }
            }
        }

        suspend fun populateDatabase(dao: FlashcardDao) {
            // Seed beautiful educational flashcards
            val seedCards = listOf(
                Flashcard(
                    title = "Quantum Entanglement",
                    front = "What is Quantum Entanglement?",
                    back = "A physical phenomenon where pairs of particles remain connected, such that actions performed on one instantly affect the other, regardless of distance.",
                    category = "Physics",
                    difficulty = "Hard",
                    color = "#8B5CF6", // Purple
                    emoji = "🌌",
                    example = "Einstein famously called it 'spooky action at a distance.'",
                    pronunciation = "/ˈkwɒntəm ɪnˈtæŋɡlmənt/",
                    notes = "Core concept in quantum computing and quantum cryptography."
                ),
                Flashcard(
                    title = "Komorebi",
                    front = "What does the Japanese word 'Komorebi' mean?",
                    back = "The beautiful, scattered sunlight filtering through the leaves of trees.",
                    category = "Languages",
                    difficulty = "Easy",
                    color = "#10B981", // Emerald
                    emoji = "🌳",
                    example = "As we walked through the cedar forest, we were bathed in komorebi.",
                    pronunciation = "/ko-mo-reh-bee/",
                    notes = "A poetic untranslatable word from Japanese, showing appreciation of nature's subtle textures."
                ),
                Flashcard(
                    title = "The Fibonacci Sequence",
                    front = "What is the Fibonacci Sequence rule?",
                    back = "Each number is the sum of the two preceding ones, starting from 0 and 1. (0, 1, 1, 2, 3, 5, 8, 13, 21...)",
                    category = "Mathematics",
                    difficulty = "Medium",
                    color = "#F59E0B", // Amber
                    emoji = "🌀",
                    example = "Petals on a flower, pinecones, and snail shells follow this golden ratio pattern.",
                    pronunciation = "/ˌfɪbəˈnɑːtʃi/",
                    notes = "Closely related to the Golden Ratio (approximately 1.618)."
                ),
                Flashcard(
                    title = "Photosynthesis",
                    front = "What is the primary product of photosynthesis?",
                    back = "Glucose (sugar) and Oxygen. Plants convert carbon dioxide, water, and sunlight into chemical energy.",
                    category = "Biology",
                    difficulty = "Easy",
                    color = "#3B82F6", // Blue
                    emoji = "🌱",
                    example = "6CO₂ + 6H₂O + light energy ➔ C₆H₁₂O₆ + 6O₂",
                    pronunciation = "/ˌfəʊtəʊˈsɪnθəsɪs/",
                    notes = "Occurs inside chloroplasts using the green pigment chlorophyll."
                ),
                Flashcard(
                    title = "Mnemonic device",
                    front = "What is a mnemonic device?",
                    back = "A learning technique that helps index or retrieve information from long-term memory (e.g. ROYGBIV for rainbow colors).",
                    category = "Psychology",
                    difficulty = "Medium",
                    color = "#EC4899", // Pink
                    emoji = "🧠",
                    example = "'My Very Educated Mother Just Served Us Noodles' for the planets in order.",
                    pronunciation = "/nɪˈmɒnɪk/",
                    notes = "From the ancient Greek word 'mnemonikos', meaning 'of memory'."
                )
            )

            for (card in seedCards) {
                dao.insertFlashcard(card)
            }
        }
    }
}
