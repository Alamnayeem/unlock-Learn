package com.unlockandlearn.ai.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockandlearn.ai.data.model.Flashcard
import com.unlockandlearn.ai.ui.viewmodel.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FlashcardViewModel) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // AI States
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val generationStatus by viewModel.generationStatus.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Generator FAB
                FloatingActionButton(
                    onClick = { showAiGeneratorDialog = true },
                    containerColor = Color(0xFF10B981), // Emerald
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Flashcard Generator")
                }

                // Add Card FAB
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF8B5CF6), // Purple
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Flashcard")
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flashcard Collection",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                // Active generation indicator
                if (isGenerating) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI active...", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by title, text, tags, difficulty...", color = Color.Gray) },
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
                            text = "Tap + to add manually or ✦ for AI generation!",
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

    // AI Generator Dialog
    if (showAiGeneratorDialog) {
        var promptText by remember { mutableStateOf("") }
        val apiConfigured = viewModel.getApiKey().isNotBlank()

        AlertDialog(
            onDismissRequest = { if (!isGenerating) showAiGeneratorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Flashcard Generator")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!apiConfigured) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Gemini API Key is not configured. Please go to the Settings tab -> AI settings to save your API Key first.",
                                color = Color(0xFFF87171),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Text(
                        text = "Enter a prompt describing the flashcards you want to create (e.g., 'Generate 10 English Words', '20 Biology Terms'). The AI will create terms, pronunciation, definitions, examples, and beautiful emojis!",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Generation Prompt") },
                        placeholder = { Text("e.g., Generate 20 Spanish Basics") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating && apiConfigured,
                        maxLines = 3
                    )

                    // Quick Templates
                    if (!isGenerating && apiConfigured) {
                        Text("Quick Templates:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("10 English Words", "15 IELTS Vocab", "20 Physics Terms").forEach { tmpl ->
                                Card(
                                    modifier = Modifier.clickable { promptText = tmpl },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tmpl,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                }
                            }
                        }
                    }

                    // Progress section
                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = generationProgress,
                            color = Color(0xFF10B981),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = generationStatus,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(generationProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            viewModel.generateAiFlashcards(promptText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isGenerating && apiConfigured && promptText.isNotBlank()
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAiGeneratorDialog = false },
                    enabled = !isGenerating
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Add Card Dialog
    if (showAddDialog) {
        FlashcardEditDialog(
            card = null,
            onDismiss = { showAddDialog = false },
            onSave = { newCard ->
                viewModel.insertFlashcard(newCard)
                showAddDialog = false
                Toast.makeText(context, "Flashcard added!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Flashcard updated!", Toast.LENGTH_SHORT).show()
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
            val parsedColor = try {
                Color(android.graphics.Color.parseColor(flashcard.color))
            } catch (e: Exception) {
                Color(0xFF8B5CF6)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(parsedColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, parsedColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
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
                
                // Tags displaying
                if (flashcard.tags.isNotBlank()) {
                    Text(
                        text = "Tags: " + flashcard.tags,
                        color = Color(0xFFA5B4FC),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Category badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = parsedColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = flashcard.category,
                            color = parsedColor,
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
                        colors = CardDefaults.cardColors(containerColor = diffColor.copy(alpha = 0.15f)),
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
    var color by remember { mutableStateOf(card?.color ?: "#8B5CF6") }
    var emoji by remember { mutableStateOf(card?.emoji ?: "💡") }
    var example by remember { mutableStateOf(card?.example ?: "") }
    var pronunciation by remember { mutableStateOf(card?.pronunciation ?: "") }
    var notes by remember { mutableStateOf(card?.notes ?: "") }
    var tags by remember { mutableStateOf(card?.tags ?: "") }

    val colorsPreset = listOf(
        "#8B5CF6", // Purple
        "#10B981", // Emerald
        "#3B82F6", // Blue
        "#F59E0B", // Gold
        "#EC4899", // Pink
        "#EF4444"  // Red
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "New Flashcard" else "Edit Flashcard") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
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
                        label = { Text("Front Side (Question/Term)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = back,
                        onValueChange = { back = it },
                        label = { Text("Back Side (Answer/Definition)") },
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

                // Difficulty Chips Selector
                item {
                    Column {
                        Text("Difficulty:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                val active = difficulty.equals(diff, ignoreCase = true)
                                FilterChip(
                                    selected = active,
                                    onClick = { difficulty = diff },
                                    label = { Text(diff) }
                                )
                            }
                        }
                    }
                }

                // Color Presets Selector
                item {
                    Column {
                        Text("Theme Color Preset:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            colorsPreset.forEach { hex ->
                                val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(parsed)
                                        .border(
                                            width = if (color.equals(hex, ignoreCase = true)) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { color = hex }
                                )
                            }
                        }
                    }
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
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("vocab, physics, exam") },
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
                            tags = tags,
                            favorite = card?.favorite ?: false,
                            learned = card?.learned ?: false,
                            dateCreated = card?.dateCreated ?: System.currentTimeMillis()
                        )
                        onSave(newCard)
                    }
                },
                enabled = title.isNotBlank() && front.isNotBlank() && back.isNotBlank()
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
