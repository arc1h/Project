package com.example.project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project.data.model.Challenge
import com.example.project.data.viewmodel.HeroViewModel

// Temporarily use Monospace font until you add a pixelated font file
// When you add the font, uncomment these lines:
// import androidx.compose.ui.text.font.Font
// import com.example.project.R
// val PixelatedFont = FontFamily(Font(R.font.pixelated_font, FontWeight.Normal))

val PixelatedFont = FontFamily.Monospace // Temporary placeholder

@Composable
fun HeroScreen(
    heroViewModel: HeroViewModel = viewModel()
) {
    val hero = heroViewModel.hero.value
    val challenges = heroViewModel.activeChallenges.value
    val isLoading = heroViewModel.isLoading.value

    // Refresh data when screen is opened
    LaunchedEffect(Unit) {
        heroViewModel.refresh()
    }

    // Pixelated theme colors
    val pixelBackground = Color(0xFFF5F5F5)
    val pixelBorder = Color(0xFF333333)
    val pixelCard = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pixelBackground)
            .padding(16.dp)
    ) {
        if (isLoading || hero == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Your Hero",
                    fontFamily = PixelatedFont,
                    fontSize = 24.sp,
                    color = pixelBorder
                )

                // Hero Stats Card
                PixelatedCard(
                    borderColor = pixelBorder,
                    backgroundColor = pixelCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero Avatar (placeholder for now)
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(3.dp, pixelBorder, RoundedCornerShape(4.dp))
                                .background(Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 48.sp,
                                fontFamily = PixelatedFont
                            )
                        }

                        // Stats
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelStatRow("Level:", hero.level.toString())
                            PixelStatRow("XP:", hero.xp.toString())
                            PixelStatRow("Coins:", hero.coins.toString())
                            PixelStatRow("Challenges", "")
                            PixelStatRow("Completed:", hero.challengesCompleted.toString())
                        }
                    }
                }

                // Active Challenges
                Text(
                    text = "Active Challenges",
                    fontFamily = PixelatedFont,
                    fontSize = 18.sp,
                    color = pixelBorder
                )

                if (challenges.isEmpty()) {
                    PixelatedCard(
                        borderColor = pixelBorder,
                        backgroundColor = pixelCard
                    ) {
                        Text(
                            text = "No active challenges",
                            fontFamily = PixelatedFont,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(challenges) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                borderColor = pixelBorder,
                                backgroundColor = pixelCard,
                                onComplete = { heroViewModel.completeChallenge(challenge) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PixelatedButton(
                        text = "Change Appearance",
                        onClick = { /* TODO: Navigate to appearance screen */ },
                        borderColor = pixelBorder
                    )

                    PixelatedButton(
                        text = "Shop",
                        onClick = { /* TODO: Navigate to shop */ },
                        borderColor = pixelBorder
                    )
                }
            }
        }
    }
}

@Composable
fun PixelatedCard(
    borderColor: Color,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, borderColor, RoundedCornerShape(4.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        content()
    }
}

@Composable
fun PixelStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = PixelatedFont,
            fontSize = 14.sp,
            color = Color.Gray
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontFamily = PixelatedFont,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    borderColor: Color,
    backgroundColor: Color,
    onComplete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .border(3.dp, borderColor, RoundedCornerShape(4.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = challenge.title,
                fontFamily = PixelatedFont,
                fontSize = 12.sp,
                color = Color.Black,
                lineHeight = 16.sp
            )

            if (challenge.goal > 1) {
                Text(
                    text = "${challenge.progress}/${challenge.goal}",
                    fontFamily = PixelatedFont,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .border(2.dp, borderColor)
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(challenge.progress.toFloat() / challenge.goal.toFloat())
                        .background(Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
fun PixelatedButton(
    text: String,
    onClick: () -> Unit,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(3.dp, borderColor)
    ) {
        Text(
            text = text,
            fontFamily = PixelatedFont,
            fontSize = 14.sp
        )
    }
}