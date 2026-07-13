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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF020205)
                ) {
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
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        // Quick fallback if cards are still loading
                        viewModel.loadNextOverlayCard()
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
