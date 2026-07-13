package com.unlockandlearn.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockandlearn.ai.ui.viewmodel.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: FlashcardViewModel) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(viewModel.getThemeSetting()) }
    var speed by remember { mutableStateOf(viewModel.getAnimationSpeedSetting()) }
    var dailyGoal by remember { mutableStateOf(viewModel.getDailyGoalSetting()) }
    var learningMode by remember { mutableStateOf(viewModel.getLearningModeSetting()) }
    var defaultCategory by remember { mutableStateOf(viewModel.getDefaultCategorySetting()) }

    var soundEnabled by remember { mutableStateOf(viewModel.getSoundSetting()) }
    var hapticEnabled by remember { mutableStateOf(viewModel.getHapticSetting()) }

    // AI States
    var apiKeyText by remember { mutableStateOf(viewModel.getApiKey()) }
    var selectedModel by remember { mutableStateOf(viewModel.getSelectedModel()) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    // Dialog state
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val categoriesList = listOf("All", "Physics", "Languages", "Mathematics", "Biology", "Psychology", "General")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // SECTION 1: LEARNING MODES
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF8B5CF6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Micro-Learning Modes", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Text("Choose how flashcards are selected on phone unlock:", fontSize = 12.sp, color = Color.Gray)

                // Mode selection dropdown replacement (Chips)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Random", "Sequential", "Category Mode", "Favorites Only", "Review Wrong Cards").forEach { mode ->
                        val active = learningMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (active) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .border(1.dp, if (active) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable {
                                    learningMode = mode
                                    viewModel.setLearningModeSetting(mode)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(mode, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                val desc = when (mode) {
                                    "Random" -> "Pick random cards from entire collection."
                                    "Sequential" -> "Step-by-step sequential cards."
                                    "Category Mode" -> "Focus on one specific category."
                                    "Favorites Only" -> "Only show cards you have starred."
                                    else -> "Only show unlearned or failed cards."
                                }
                                Text(desc, color = Color.Gray, fontSize = 11.sp)
                            }
                            if (active) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Category selection if Category Mode is chosen
                AnimatedVisibility(visible = learningMode == "Category Mode" || learningMode == "Random" || learningMode == "Sequential") {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Active Category Focus:", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categoriesList.size) { index ->
                                val cat = categoriesList[index]
                                val active = defaultCategory == cat
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        defaultCategory = cat
                                        viewModel.setDefaultCategorySetting(cat)
                                        viewModel.loadNextOverlayCard()
                                    },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: AI FEATURES (GEMINI SETUP)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Configuration (Gemini API)", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Text("Enter your Gemini API key to unlock the AI Flashcard Generator. Your key is stored 100% securely on-device using EncryptedSharedPreferences.", fontSize = 12.sp, color = Color.Gray)

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Model Selection
                Text("Select Gemini Model:", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro").forEach { model ->
                        val active = selectedModel == model
                        Button(
                            onClick = {
                                selectedModel = model
                                viewModel.saveSelectedModel(model)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) Color(0xFF10B981) else Color(0xFF2E2E3E),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(model.substringAfter("gemini-"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // AI Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (apiKeyText.isNotBlank()) {
                                viewModel.saveApiKey(apiKeyText)
                                Toast.makeText(context, "API Key Saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "API Key cannot be empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Key", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (apiKeyText.isNotBlank()) {
                                isTestingConnection = true
                                connectionTestResult = "Testing connection..."
                                viewModel.testConnection(apiKeyText, selectedModel) { success, msg ->
                                    isTestingConnection = false
                                    connectionTestResult = msg
                                }
                            } else {
                                Toast.makeText(context, "Please enter a key to test", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test API", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            apiKeyText = ""
                            viewModel.deleteApiKey()
                            Toast.makeText(context, "API Key Deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                }

                // Connection test result text
                if (connectionTestResult.isNotEmpty()) {
                    Text(
                        text = "Result: $connectionTestResult",
                        color = if (connectionTestResult.contains("successful", ignoreCase = true)) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // SECTION 3: SYSTEM CONTROLS (SOUND, HAPTIC, THEME)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sounds, Feedback & Themes", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Sound Effect Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sound Effects", color = Color.White, fontSize = 14.sp)
                        Text("Play high-pitch bleeps on flips/unlocks", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            viewModel.setSoundSetting(it)
                        }
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Haptic Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tactile Haptic Feedback", color = Color.White, fontSize = 14.sp)
                        Text("Vibrate gently when flipping cards", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            viewModel.setHapticSetting(it)
                        }
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Theme Settings Selection
                Text("App Color Theme Preset", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Light", "Dark", "System").forEach { theme ->
                        val active = selectedTheme == theme
                        Button(
                            onClick = {
                                selectedTheme = theme
                                viewModel.setThemeSetting(theme)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) Color(0xFF3B82F6) else Color(0xFF2E2E3E),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(theme, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SECTION 4: DATA HUB (IMPORT, EXPORT, BACKUP, RESTORE)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFFEC4899))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data Backup & Import / Export Hub", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Text("Transfer your collections between devices. Export your cards into formatted CSV/JSON, or paste templates to restore.", fontSize = 12.sp, color = Color.Gray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import / Restore", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export / Backup", fontSize = 11.sp)
                    }
                }
            }
        }

        // ABOUT & PRIVACY INFO
        ListItem(
            headlineContent = { Text("About Unlock & Learn AI", color = Color.White) },
            supportingContent = { Text("Version 2.0 (Premium AI Edition)", color = Color.Gray) },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF8B5CF6)) },
            colors = ListItemDefaults.colors(containerColor = Color(0xFF1E1E2E)),
            modifier = Modifier
                .padding(bottom = 4.dp)
                .clickable { showAboutDialog = true }
        )

        ListItem(
            headlineContent = { Text("Privacy Policy", color = Color.White) },
            supportingContent = { Text("100% Offline & Private storage", color = Color.Gray) },
            leadingContent = { Icon(Icons.Default.PrivacyTip, contentDescription = "Privacy", tint = Color(0xFF10B981)) },
            colors = ListItemDefaults.colors(containerColor = Color(0xFF1E1E2E)),
            modifier = Modifier.clickable { showPrivacyDialog = true }
        )

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Unlock & Learn AI") },
                text = {
                    Text("Unlock & Learn AI v2.0 is a premium micro-learning companion. It intercepts your daily screen unlock patterns to help you practice English words, languages, general knowledge, or custom study subjects. Enhanced with beautiful fluid Material 3 layouts, local storage, and AI-powered Gemini flashcard synthesis.")
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Privacy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy") },
                text = {
                    Text("Your privacy is absolute. All your flashcards, notes, stats, streaks, and even your private Gemini API Key are stored strictly on-device inside securely encrypted storage. No analytics are uploaded to external services.")
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("I Understand")
                    }
                }
            )
        }

        // IMPORT DIALOG
        if (showImportDialog) {
            var importText by remember { mutableStateOf("") }
            var selectedFormat by remember { mutableStateOf("JSON") }
            var isBackupRestore by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Data / Restore Backup") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Select Data Format:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("JSON", "CSV", "TXT", "Full Backup").forEach { fmt ->
                                val active = if (fmt == "Full Backup") isBackupRestore else (selectedFormat == fmt && !isBackupRestore)
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        if (fmt == "Full Backup") {
                                            isBackupRestore = true
                                        } else {
                                            isBackupRestore = false
                                            selectedFormat = fmt
                                        }
                                    },
                                    label = { Text(fmt) }
                                )
                            }
                        }

                        Text(
                            text = if (isBackupRestore) 
                                "⚠️ Warning: Restoring a full backup will clear ALL existing flashcards and replace them with the backup content." 
                                else "Paste your text content below (JSON array, comma-separated CSV lines, or TXT lines formatted as 'Title | Front | Back'):", 
                            fontSize = 12.sp, 
                            color = if (isBackupRestore) Color(0xFFF87171) else Color.LightGray
                        )

                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            label = { Text("Paste Data Content") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            placeholder = {
                                if (isBackupRestore || selectedFormat == "JSON") {
                                    Text("[\n  {\n    \"title\": \"Example\",\n    \"front\": \"Front text\",\n    \"back\": \"Back text\"\n  }\n]")
                                } else if (selectedFormat == "CSV") {
                                    Text("Title,Front,Back,Category\n\"Example\",\"Front Text\",\"Back Text\",\"General\"")
                                } else {
                                    Text("Word | Definition / Question | Answer")
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                if (isBackupRestore) {
                                    val ok = viewModel.restoreData(importText)
                                    if (ok) {
                                        Toast.makeText(context, "Full Backup restored successfully!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                    } else {
                                        Toast.makeText(context, "Error: Invalid Backup JSON format.", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val count = when (selectedFormat) {
                                        "JSON" -> viewModel.importFromJson(importText)
                                        "CSV" -> viewModel.importFromCsv(importText)
                                        else -> viewModel.importFromTxt(importText)
                                    }
                                    if (count >= 0) {
                                        Toast.makeText(context, "Successfully imported $count flashcards!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                    } else {
                                        Toast.makeText(context, "Error during import. Check format.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // EXPORT DIALOG
        if (showExportDialog) {
            var selectedFormat by remember { mutableStateOf("JSON") }
            var isFullBackup by remember { mutableStateOf(false) }

            val exportText = remember(selectedFormat, isFullBackup) {
                if (isFullBackup) {
                    viewModel.backupData()
                } else if (selectedFormat == "JSON") {
                    viewModel.exportToJson()
                } else {
                    viewModel.exportToCsv()
                }
            }

            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export / Backup Collection") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Select Export Mode:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("JSON", "CSV", "Full Backup").forEach { fmt ->
                                val active = if (fmt == "Full Backup") isFullBackup else (selectedFormat == fmt && !isFullBackup)
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        if (fmt == "Full Backup") {
                                            isFullBackup = true
                                        } else {
                                            isFullBackup = false
                                            selectedFormat = fmt
                                        }
                                    },
                                    label = { Text(fmt) }
                                )
                            }
                        }

                        Text("Your exported text content is below. Click 'Copy to Clipboard' to share or save it in a text file.", fontSize = 12.sp, color = Color.LightGray)

                        OutlinedTextField(
                            value = exportText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exported Data") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Flashcard Export", exportText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Exported content copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy to Clipboard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
