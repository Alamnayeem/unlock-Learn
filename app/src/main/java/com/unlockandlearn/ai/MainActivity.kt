package com.unlockandlearn.ai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.unlockandlearn.ai.service.ScreenUnlockService
import com.unlockandlearn.ai.ui.screens.DashboardScreen
import com.unlockandlearn.ai.ui.screens.SettingsScreen
import com.unlockandlearn.ai.ui.screens.StatsScreen
import com.unlockandlearn.ai.ui.theme.UnlockAndLearnAITheme
import com.unlockandlearn.ai.ui.viewmodel.FlashcardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlashcardViewModel by viewModels()
    private val OVERLAY_PERMISSION_REQ_CODE = 1245

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkOverlayPermission()
        startUnlockService()

        setContent {
            UnlockAndLearnAITheme {
                var selectedTab by remember { mutableStateOf(0) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    label = { Text("Cards") },
                                    icon = { Icon(Icons.Default.Collections, contentDescription = "Flashcards") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    label = { Text("Stats") },
                                    icon = { Icon(Icons.Default.QueryStats, contentDescription = "Statistics") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    label = { Text("Settings") },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                                )
                            }
                        }
                    ) { paddingValues ->
                        Surface(modifier = Modifier.padding(paddingValues)) {
                            when (selectedTab) {
                                0 -> DashboardScreen(viewModel = viewModel)
                                1 -> StatsScreen(viewModel = viewModel)
                                2 -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startUnlockService() {
        val serviceIntent = Intent(this, ScreenUnlockService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start lock screen service", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Overlay permission required to display flashcards when unlocking",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission Granted! Service active.", Toast.LENGTH_SHORT).show()
                    startUnlockService()
                } else {
                    Toast.makeText(this, "Permission denied. App overlay will not show on unlock.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
