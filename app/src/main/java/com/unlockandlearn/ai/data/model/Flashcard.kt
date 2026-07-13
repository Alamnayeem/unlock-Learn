package com.unlockandlearn.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val front: String,
    val back: String,
    val category: String,
    val difficulty: String, // Easy, Medium, Hard
    val color: String, // Hex color like #6366F1
    val emoji: String,
    val example: String = "",
    val pronunciation: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val learned: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)
