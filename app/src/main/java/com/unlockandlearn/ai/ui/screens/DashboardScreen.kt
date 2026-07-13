package com.unlockandlearn.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockandlearn.ai.data.model.Flashcard
import com.unlockandlearn.ai.ui.viewmodel.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FlashcardViewModel) {
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }

    val categories = listOf("All", "Physics", "Languages", "Mathematics", "Biology", "Psychology", "General")

    // Filter results based on selected category tab
    val finalResults = remember(searchResults, selectedCategory) {
        if (selectedCategory == "All") {
            searchResults
        } else {
            searchResults.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF8B5CF6),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Flashcard")
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Flashcard Collection",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search cards by title, text, or category...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E2E),
                    unfocusedContainerColor = Color(0xFF1E1E2E),
                    disabledContainerColor = Color(0xFF1E1E2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                } else null
            )

            // Horizontal Category Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val active = selectedCategory == cat
                    Card(
                        modifier = Modifier.clickable { selectedCategory = cat },
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) Color(0xFF8B5CF6) else Color(0xFF1E1E2E)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = cat,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Flashcards List
            if (finalResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No flashcards found",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Tap the + button to add one manually!",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(finalResults) { card ->
                        FlashcardListItem(
                            flashcard = card,
                            onEdit = { editingCard = card },
                            onDelete = { viewModel.deleteFlashcard(card) },
                            onDuplicate = { viewModel.duplicateFlashcard(card) },
                            onFavoriteToggle = { viewModel.updateFlashcard(card.copy(favorite = !card.favorite)) }
                        )
                    }
                }
            }
        }
    }

    // Add Card Dialog
    if (showAddDialog) {
        FlashcardEditDialog(
            card = null,
            onDismiss = { showAddDialog = false },
            onSave = { newCard ->
                viewModel.insertFlashcard(newCard)
                showAddDialog = false
            }
        )
    }

    // Edit Card Dialog
    if (editingCard != null) {
        FlashcardEditDialog(
            card = editingCard,
            onDismiss = { editingCard = null },
            onSave = { updatedCard ->
                viewModel.updateFlashcard(updatedCard)
                editingCard = null
            }
        )
    }
}

@Composable
fun FlashcardListItem(
    flashcard: Flashcard,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = flashcard.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text section
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flashcard.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (flashcard.favorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = flashcard.front,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Category badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = flashcard.category,
                            color = Color(0xFFA78BFA),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // Difficulty badge
                    val diffColor = when (flashcard.difficulty.lowercase()) {
                        "easy" -> Color(0xFF10B981)
                        "medium" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = diffColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = flashcard.difficulty,
                            color = diffColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Trigger Dropdown
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF2E2E3E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White) },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White) },
                        onClick = {
                            expanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Toggle Favorite", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Favorite", tint = Color.White) },
                        onClick = {
                            expanded = false
                            onFavoriteToggle()
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF4444)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444)) },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardEditDialog(
    card: Flashcard?,
    onDismiss: () -> Unit,
    onSave: (Flashcard) -> Unit
) {
    var title by remember { mutableStateOf(card?.title ?: "") }
    var front by remember { mutableStateOf(card?.front ?: "") }
    var back by remember { mutableStateOf(card?.back ?: "") }
    var category by remember { mutableStateOf(card?.category ?: "General") }
    var difficulty by remember { mutableStateOf(card?.difficulty ?: "Medium") }
    var color by remember { mutableStateOf(card?.color ?: "#6366F1") }
    var emoji by remember { mutableStateOf(card?.emoji ?: "💡") }
    var example by remember { mutableStateOf(card?.example ?: "") }
    var pronunciation by remember { mutableStateOf(card?.pronunciation ?: "") }
    var notes by remember { mutableStateOf(card?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "New Flashcard" else "Edit Flashcard") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = front,
                        onValueChange = { front = it },
                        label = { Text("Front Side (Question)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = back,
                        onValueChange = { back = it },
                        label = { Text("Back Side (Answer)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = pronunciation,
                        onValueChange = { pronunciation = it },
                        label = { Text("Pronunciation Guide") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = example,
                        onValueChange = { example = it },
                        label = { Text("Example Usage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Extra Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && front.isNotBlank() && back.isNotBlank()) {
                        val newCard = Flashcard(
                            id = card?.id ?: 0,
                            title = title,
                            front = front,
                            back = back,
                            category = category,
                            difficulty = difficulty,
                            color = color,
                            emoji = emoji,
                            example = example,
                            pronunciation = pronunciation,
                            notes = notes,
                            favorite = card?.favorite ?: false,
                            learned = card?.learned ?: false,
                            dateCreated = card?.dateCreated ?: System.currentTimeMillis()
                        )
                        onSave(newCard)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
