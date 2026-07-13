package com.unlockandlearn.ai.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockandlearn.ai.data.model.Flashcard

@Composable
fun FlashcardScreen(
    flashcard: Flashcard,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onMarkAsLearned: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var flipped by remember { mutableStateOf(false) }
    
    // Reset flipped state when flashcard changes
    LaunchedEffect(flashcard.id) {
        flipped = false
    }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "cardFlipRotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C20),
                        Color(0xFF15102A),
                        Color(0xFF020205)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UNLOCK & LEARN AI",
                    fontSize = 12.sp,
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = flashcard.category,
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            IconButton(
                onClick = onFavoriteToggle,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (flashcard.favorite) Color(0xFFF59E0B) else Color.Gray
                )
            ) {
                Icon(
                    imageVector = if (flashcard.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Animated Flip Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 32.dp)
                .clickable { flipped = !flipped },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                // Front and Back rendering
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // FRONT OF CARD
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = flashcard.emoji,
                                fontSize = 64.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = flashcard.title,
                                fontSize = 28.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (flashcard.pronunciation.isNotEmpty()) {
                                Text(
                                    text = flashcard.pronunciation,
                                    fontSize = 14.sp,
                                    color = Color(0xFFA5B4FC),
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            Text(
                                text = flashcard.front,
                                fontSize = 18.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = "Tap Card to Reveal Answer",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    } else {
                        // BACK OF CARD (Rotated 180 Y to prevent mirror text)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Answer",
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = flashcard.back,
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            )
                            if (flashcard.example.isNotEmpty()) {
                                Divider(
                                    color = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Text(
                                    text = "Example:",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = flashcard.example,
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            if (flashcard.notes.isNotEmpty()) {
                                Text(
                                    text = flashcard.notes,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onPrevious,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev")
            }

            Button(
                onClick = onMarkAsLearned,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981) // Green
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Learned")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Learned")
            }

            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5) // Indigo
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Skip")
            }

            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss Overlay")
            }
        }
    }
}
