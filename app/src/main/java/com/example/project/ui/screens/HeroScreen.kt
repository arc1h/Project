package com.example.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project.data.model.Challenge
import com.example.project.data.viewmodel.HeroViewModel
import com.example.project.R
import com.example.project.ui.theme.HeroTypography
import com.example.project.ui.theme.PixelatedFont

@Composable
fun HeroScreen(
    heroViewModel: HeroViewModel = viewModel()
) {
    MaterialTheme(
        typography = HeroTypography
    ) {

    val hero = heroViewModel.hero.value
    val challenges = heroViewModel.activeChallenges.value
    val isLoading = heroViewModel.isLoading.value

    // Refresh data when screen is opened
    LaunchedEffect(Unit) {
        heroViewModel.refresh()
    }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 12.dp, top = 24.dp, bottom = 24.dp)
                    )

                    // Hero Stats Card
                    PixelatedCard(
                        borderColor = MaterialTheme.colorScheme.background,
                        backgroundColor = MaterialTheme.colorScheme.background
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Hero Avatar (placeholder for now)
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.background)
                                    .background(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.heros00),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            }

                            // Stats
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 12.dp, top = 24.dp)
                    )

                    if (challenges.isEmpty()) {
                        PixelatedCard(
                            borderColor = MaterialTheme.colorScheme.onSurface,
                            backgroundColor = MaterialTheme.colorScheme.background
                        ) {
                            Text(
                                text = "No active challenges",
                                fontFamily = PixelatedFont,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(12.dp)
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
                                    borderColor = MaterialTheme.colorScheme.onSurface,
                                    backgroundColor = MaterialTheme.colorScheme.background,
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
                            borderColor = MaterialTheme.colorScheme.onSurface
                        )

                        PixelatedButton(
                            text = "Shop",
                            onClick = { /* TODO: Navigate to shop */ },
                            borderColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent) // image goes under this
        // .border(3.dp, borderColor) // ← ONLY while debugging layout
    ) {
        content()
    }
}

@Composable
fun PixelStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = PixelatedFont,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontFamily = PixelatedFont,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
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
        color = MaterialTheme.colorScheme.background,
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
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )

            if (challenge.goal > 1) {
                Text(
                    text = "${challenge.progress}/${challenge.goal}",
                    fontFamily = PixelatedFont,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
    ) {
        // Background image
        Image(
            painter = painterResource(R.drawable.transparent),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().border(2.dp, Color.Transparent, RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillBounds
        )

        Text(
            text = text,
            fontFamily = PixelatedFont,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}