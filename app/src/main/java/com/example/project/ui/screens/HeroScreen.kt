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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    var showFriendPicker by remember { mutableStateOf(false) }

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
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surface),
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
                                PixelStatRow("Challenges:", hero.challengesCompleted.toString())
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
                            .padding(start = 8.dp, top = 12.dp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = "Friends",
                            onClick = { showFriendPicker = true },
                            borderColor = Color.LightGray
                        )

                        if (showFriendPicker) {
                            FriendPickerDialog(
                                onDismiss = { showFriendPicker = false },
                                onFriendSelected = { friendUid ->
                                    showFriendPicker = false
                                    // Navigate to the dynamic friend profile
                                    navController?.navigate("friend_hero/$friendUid")
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
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        content()
    }
}

@Composable
fun AnimatedHero(charId: String?) {
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
                Text(
                    text = challenge.description,
                    fontFamily = PixelFont,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val displayProgress = if (challenge.isCompleted) challenge.goal else challenge.progress
                Text(
                    text = "$displayProgress/${challenge.goal}",
                    fontFamily = PixelFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Thick Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .border(2.dp, borderColor)
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    val ratio = if (challenge.isCompleted) 1f else if (challenge.goal > 0)
                        (challenge.progress.toFloat() / challenge.goal.toFloat()).coerceIn(0f, 1f)
                    else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .background(
                                // Turn Purple if progress matches goal OR if marked completed
                                if (ratio >= 1f || challenge.isCompleted) Purple else Color(
                                    0xFF4CAF50
                                )
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
                            .border(
                                2.dp,
                                if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                            .background(
                                if (isUnlocked) Color.Transparent else Color.Black.copy(
                                    alpha = 0.1f
                                )
                            )
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

@Composable
fun FriendPickerDialog(
    onDismiss: () -> Unit,
    onFriendSelected: (String) -> Unit
) {
    val firestore = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // Change to SnapshotListener for real-time sync
    androidx.compose.runtime.DisposableEffect(uid) {
        if (uid == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        // Listen to the current user's document for changes in the 'friends' array
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                val friendIds = snapshot?.get("friends") as? List<String> ?: emptyList()
                // When the friend list changes, fetch their profile info
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    val updatedList = friendIds.map { friendId ->
                        val friendDoc = firestore.collection("users").document(friendId).get().await()
                        UserProfile(
                            uid = friendId,
                            username = friendDoc.getString("username") ?: "Hero",
                            char = friendDoc.getString("char") ?: "0"
                        )
                    }
                    friends = updatedList
                    isLoading = false
                }
            }

        onDispose {
            listener.remove()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visit a Hero", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)) },
        text = {
            if (isLoading) {
                Box(Modifier
                    .fillMaxWidth()
                    .height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
            } else if (friends.isEmpty()) {
                Text("Your friend list is empty. Add heroes to compare progress!")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(friends) { friend ->
                        Surface(
                            onClick = { onFriendSelected(friend.uid) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Hero Icon (Left)
                                val context = LocalContext.current
                                val resName = "hero${friend.char}0"
                                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                                Image(
                                    painter = painterResource(id = if (resId != 0) resId else R.drawable.hero00),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = friend.username,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Purple, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    )
}

// Helper to map "char" string to a drawable
@Composable
fun getHeroImageResource(charId: String): Int {
    return when (charId) {
        "0" -> R.drawable.hero00
        "1" -> R.drawable.hero10
        "2" -> R.drawable.hero20
        "3" -> R.drawable.hero30
        "4" -> R.drawable.hero40
        "5" -> R.drawable.hero50
        else -> R.drawable.hero00
    }
}