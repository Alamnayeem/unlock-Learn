package com.unlockandlearn.ai

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockandlearn.ai.ui.screens.FlashcardScreen
import com.unlockandlearn.ai.ui.theme.UnlockAndLearnAITheme
import com.unlockandlearn.ai.ui.viewmodel.FlashcardViewModel

class UnlockActivity : ComponentActivity() {

    private val viewModel: FlashcardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        configureLockScreenOverlay()

        setContent {
            UnlockAndLearnAITheme {
                val currentCard by viewModel.currentOverlayCard.collectAsState()
                val isLoading by viewModel.isOverlayLoading.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.loadNextOverlayCard()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF020205)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        }
                    } else {
                        currentCard?.let { card ->
                            FlashcardScreen(
                                flashcard = card,
                                onNext = {
                                    viewModel.incrementTodayCount()
                                    viewModel.loadNextOverlayCard()
                                },
                                onPrevious = {
                                    viewModel.loadNextOverlayCard()
                                },
                                onSkip = {
                                    viewModel.incrementTodayCount()
                                    viewModel.loadNextOverlayCard()
                                },
                                onMarkAsLearned = {
                                    viewModel.incrementTodayCount()
                                    viewModel.updateFlashcard(card.copy(learned = true))
                                    viewModel.loadNextOverlayCard()
                                },
                                onFavoriteToggle = {
                                    viewModel.updateFlashcard(card.copy(favorite = !card.favorite))
                                },
                                onClose = {
                                    finish()
                                }
                            )
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "No Flashcards Found",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Open the Dashboard to create some cards or import educational templates!",
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                                Button(
                                    onClick = { finish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    Text("Unlock Phone", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureLockScreenOverlay() {
        // Configure flags to allow activity to be drawn on top of the secure keyguard lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
