package com.unlockandlearn.ai.data.local

import androidx.room.*
import com.unlockandlearn.ai.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY dateCreated DESC")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE favorite = 1 ORDER BY dateCreated DESC")
    fun getFavoriteFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE learned = 0 ORDER BY dateCreated DESC")
    fun getUnlearnedFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getFlashcardById(id: Int): Flashcard?

    @Query("SELECT * FROM flashcards WHERE (title LIKE '%' || :query || '%' OR front LIKE '%' || :query || '%' OR back LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')")
    fun searchFlashcards(query: String): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE learned = 1")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE favorite = 1")
    fun getFavoriteCount(): Flow<Int>
}
