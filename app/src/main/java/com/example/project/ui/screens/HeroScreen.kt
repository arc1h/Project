package com.example.project.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.project.R
import com.example.project.data.model.Challenge
import com.example.project.data.viewmodel.HeroViewModel
import com.example.project.ui.theme.HeroTypography
import com.example.project.ui.theme.PixelFont
import com.example.project.ui.theme.PixelTitle
import com.example.project.ui.theme.Purple
import kotlinx.coroutines.delay

@Composable
fun HeroScreen(
    heroViewModel: HeroViewModel = viewModel(),
    navController: NavController? = null
) {
    MaterialTheme(
        typography = HeroTypography
    ) {

    val hero = heroViewModel.hero.value
    val challenges = heroViewModel.activeChallenges.value
    val isLoading = heroViewModel.isLoading.value
    var showAppearanceDialog by remember { mutableStateOf(false) }

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
                        fontFamily = PixelTitle,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 12.dp, top = 24.dp)
                    )
                    Spacer(Modifier.height(12.dp))
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
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(3.dp, Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                // This padding ensures the hero sprite never touches the border
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .padding(4.dp) // Add a little margin so it doesn't touch the Card's border
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surface), // Use surface color to distinguish
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedHero(charId = hero.char)
                                }
                            }

                            // Stats
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                PixelStatRow("Level:", hero.level.toString())
                                PixelStatRow("XP:", hero.xp.toString())
                                PixelStatRow("Coins:", hero.coins.toString())
                                PixelStatRow("★ Streak:", hero.longestStreak.toString())
                                PixelStatRow("Challenges", hero.challengesCompleted.toString())
                            }
                        }
                    }

                    // Challenges
                    Text(
                        text = "Challenges",
                        fontFamily = PixelTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 24.dp)
                    )

                    if (challenges.isEmpty()) {
                        PixelatedCard(
                            borderColor = Color.LightGray,
                            backgroundColor = MaterialTheme.colorScheme.background
                        ) {
                            Text(
                                text = "No active challenges! \n Wait for next week..",
                                fontFamily = PixelFont,
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(challenges, key = { it.id }) { challenge ->
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
                            onClick = { showAppearanceDialog = true },
                            borderColor = Color.LightGray
                        )

                        if (showAppearanceDialog) {
                            AppearanceDialog(
                                currentXp = hero.xp,
                                onDismiss = { showAppearanceDialog = false },
                                onSelectChar = { newId ->
                                    heroViewModel.updateHeroAppearance(newId)
                                    showAppearanceDialog = false
                                }
                            )
                        }

                        PixelatedButton(
                            text = "Shop",
                            onClick = { navController?.navigate("shop") },
                            borderColor = Color.LightGray
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
            .clip(RoundedCornerShape(4.dp)) // Clip EVERYTHING to the shape first
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(2.dp) // Content starts inside the border
    ) {
        content()
    }
}

@Composable
fun AnimatedHero(charId: String?) {
    // Force "0" if the database has anything else for now to test
    val safeId = if (charId.isNullOrBlank() || charId == "default") "0" else charId

    var isFrameTwo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isFrameTwo = !isFrameTwo
        }
    }

    val frameSuffix = if (isFrameTwo) "1" else "0"
    val resourceName = "hero$safeId$frameSuffix"

    val imageRes = remember(resourceName) {
        val id = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        if (id == 0) {
            // Log exactly what's failing
            Log.e("ANIM", "FAILED TO FIND: $resourceName")
            R.drawable.hero00 // Absolute fallback to your first frame
        } else id
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
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
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontFamily = PixelFont,
                fontSize = 16.sp,
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
            .width(220.dp)
            .height(160.dp),
        color = backgroundColor,
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = challenge.title,
                    fontFamily = PixelFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                // THE DESCRIPTION
                Text(
                    text = challenge.description,
                    fontFamily = PixelFont,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            // Progress Bar Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${challenge.progress}/${challenge.goal}",
                    fontFamily = PixelFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Thick Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp) // Slightly taller
                        .border(2.dp, borderColor)
                        .background(Color.Black.copy(alpha = 0.2f)) // Darker track for better contrast
                ) {
                    val ratio = if (challenge.goal > 0)
                        (challenge.progress.toFloat() / challenge.goal.toFloat()).coerceIn(0f, 1f)
                    else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .background(
                                // Turn Purple if progress matches goal OR if marked completed
                                if (ratio >= 1f || challenge.isCompleted) Purple else Color(0xFF4CAF50)
                            )
                    )
                }
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
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AppearanceDialog(
    currentXp: Int,
    onDismiss: () -> Unit,
    onSelectChar: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Hero", fontFamily = PixelFont, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val characters = listOf(
                    Triple("0", "Rookie", 0),
                    Triple("1", "Knight", 1000),
                    Triple("2", "Cowboy", 2000),
                    Triple("3", "Ninja", 3000),
                    Triple("4", "Astronaut", 4000),
                    Triple("5", "Skeleton", 5000),
                )

                characters.forEach { (id, name, reqXp) ->
                    val isUnlocked = currentXp >= reqXp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray)
                            .background(if (isUnlocked) Color.Transparent else Color.Black.copy(alpha = 0.1f))
                            .clickable(enabled = isUnlocked) { onSelectChar(id) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small preview of frame 0
                        val resName = "hero${id}0"
                        val context = LocalContext.current
                        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

                        Image(
                            painter = painterResource(if (resId != 0) resId else R.drawable.hero00),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            // Gray out if locked
                            alpha = if (isUnlocked) 1f else 0.4f
                        )

                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(name, fontFamily = PixelFont, fontSize = 16.sp)
                            if (!isUnlocked) {
                                Text("Req: $reqXp XP", color = Purple, fontSize = 12.sp, fontFamily = PixelFont)
                            } else {
                                Text("Unlocked", color = Color(0xFF3CD250), fontSize = 12.sp, fontFamily = PixelFont)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontFamily = PixelFont)
            }
        }
    )
}