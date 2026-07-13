package com.unlockandlearn.ai.data.repository

import com.unlockandlearn.ai.data.local.FlashcardDao
import com.unlockandlearn.ai.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val flashcardDao: FlashcardDao) {
    val allFlashcards: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()
    val favoriteFlashcards: Flow<List<Flashcard>> = flashcardDao.getFavoriteFlashcards()
    val unlearnedFlashcards: Flow<List<Flashcard>> = flashcardDao.getUnlearnedFlashcards()
    val totalCount: Flow<Int> = flashcardDao.getCount()
    val learnedCount: Flow<Int> = flashcardDao.getLearnedCount()
    val favoriteCount: Flow<Int> = flashcardDao.getFavoriteCount()

    fun searchFlashcards(query: String): Flow<List<Flashcard>> {
        return flashcardDao.searchFlashcards(query)
    }

    suspend fun getFlashcardById(id: Int): Flashcard? {
        return flashcardDao.getFlashcardById(id)
    }

    suspend fun insertFlashcard(flashcard: Flashcard): Long {
        return flashcardDao.insertFlashcard(flashcard)
    }

    suspend fun updateFlashcard(flashcard: Flashcard) {
        flashcardDao.updateFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) {
        flashcardDao.deleteFlashcard(flashcard)
    }

    suspend fun duplicateFlashcard(flashcard: Flashcard) {
        val duplicated = flashcard.copy(
            id = 0, // Reset ID for auto-generation
            title = "${flashcard.title} (Copy)",
            favorite = false,
            learned = false,
            dateCreated = System.currentTimeMillis()
        )
        flashcardDao.insertFlashcard(duplicated)
    }
}
