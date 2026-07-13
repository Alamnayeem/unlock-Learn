package com.unlockandlearn.ai.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Theme Settings
        Text(
            text = "App Theme",
            fontSize = 14.sp,
            color = Color(0xFF8B5CF6),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
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
                        containerColor = if (active) Color(0xFF8B5CF6) else Color(0xFF1E1E2E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(theme)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animation Speed Setting
        Text(
            text = "Animation Speed multiplier (${String.format("%.1fx", speed)})",
            fontSize = 14.sp,
            color = Color(0xFF8B5CF6),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                viewModel.setAnimationSpeedSetting(it)
            },
            valueRange = 0.5f..2.0f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF8B5CF6),
                activeTrackColor = Color(0xFF8B5CF6)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Goal Setting
        Text(
            text = "Daily Goal",
            fontSize = 14.sp,
            color = Color(0xFF8B5CF6),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show $dailyGoal flashcards daily",
                fontSize = 16.sp,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (dailyGoal > 1) {
                            dailyGoal -= 1
                            viewModel.setDailyGoalSetting(dailyGoal)
                        }
                    }
                ) {
                    Text("-", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$dailyGoal",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(
                    onClick = {
                        dailyGoal += 1
                        viewModel.setDailyGoalSetting(dailyGoal)
                    }
                ) {
                    Text("+", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // About & Information
        ListItem(
            headlineContent = { Text("About Unlock & Learn AI", color = Color.White) },
            supportingContent = { Text("Version 1.0.0 (Core Edition)", color = Color.Gray) },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF8B5CF6)) },
            colors = ListItemDefaults.colors(containerColor = Color(0xFF1E1E2E)),
            modifier = Modifier
                .padding(bottom = 12.dp)
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
                    Text("Unlock & Learn AI is a micro-learning utility that turns everyday phone triggers like unlocks into quick micro-study opportunities. Fully offline, extensible, and designed using Material Design 3 and Jetpack Compose.")
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
                    Text("Your privacy is absolute. Unlock & Learn AI operates entirely on-device, preserving your flashcards, scores, streaks, and analytics in a local Room database. No data is harvested, analyzed, or shared with cloud providers.")
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("I Understand")
                    }
                }
            )
        }
    }
}
