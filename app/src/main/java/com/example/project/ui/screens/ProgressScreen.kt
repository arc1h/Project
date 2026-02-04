package com.example.project.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.project.data.model.Habit
import com.example.project.data.viewmodel.HabitViewModel
import com.example.project.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class ProgressStats(
    val longestStreakHabit: String = "No data",
    val longestStreakDays: Int = 0,
    val shortestStreakHabit: String = "No data",
    val shortestStreakDays: Int = 0,
    val mostSkippedHabit: String = "No data",
    val mostSkippedDays: Int = 0
)

@Composable
fun ProgressScreen(
    habitViewModel: HabitViewModel = viewModel()
) {
    val currentUser = Firebase.auth.currentUser
    val habits = habitViewModel.habits

    var selectedTab by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var pictureUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var stats by remember { mutableStateOf(ProgressStats()) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val imageUrl = uri.toString()
            currentUser?.uid?.let { uid ->
                val firestore = Firebase.firestore
                firestore.collection("users").document(uid)
                    .update("progressPictures", FieldValue.arrayUnion(imageUrl))
            }
        }
    }

    // Load notes and pictures
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val firestore = Firebase.firestore
                val doc = firestore.collection("users").document(uid).get().await()

                notes = doc.getString("progressNotes") ?: ""
                pictureUrls = doc.get("progressPictures") as? List<String> ?: emptyList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Calculate stats from habits
    LaunchedEffect(habits) {
        if (habits.isEmpty()) {
            stats = ProgressStats()
            return@LaunchedEffect
        }

        val habitStats = habits.map { habit ->
            val streak = calculateCurrentStreak(habit)
            val skipped = calculateSkippedDays(habit)
            Triple(habit.name, streak, skipped)
        }

        val longestStreak = habitStats.maxByOrNull { it.second }
        val shortestStreak = habitStats.minByOrNull { it.second }
        val mostSkipped = habitStats.maxByOrNull { it.third }

        stats = ProgressStats(
            longestStreakHabit = longestStreak?.first ?: "No data",
            longestStreakDays = longestStreak?.second ?: 0,
            shortestStreakHabit = shortestStreak?.first ?: "No data",
            shortestStreakDays = shortestStreak?.second ?: 0,
            mostSkippedHabit = mostSkipped?.first ?: "No data",
            mostSkippedDays = mostSkipped?.third ?: 0
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = Purple
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload Picture", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Records",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Stats Cards Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Longest Streak",
                    value = "${stats.longestStreakDays} days",
                    subtitle = stats.longestStreakHabit,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Shortest Streak",
                    value = "${stats.shortestStreakDays} days",
                    subtitle = stats.shortestStreakHabit,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Card Row 2
            StatCard(
                title = "Most Skipped Days",
                value = "${stats.mostSkippedDays} days",
                subtitle = stats.mostSkippedHabit,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Purple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Purple
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                ) {
                    Text(
                        "Notes",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (selectedTab == 0) Purple else Color.Gray
                    )
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                ) {
                    Text(
                        "Pictures",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (selectedTab == 1) Purple else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> NotesTab(
                    notes = notes,
                    onNotesChange = { newNotes ->
                        notes = newNotes
                        currentUser?.uid?.let { uid ->
                            Firebase.firestore.collection("users").document(uid)
                                .update("progressNotes", newNotes)
                        }
                    }
                )
                1 -> PicturesTab(
                    pictureUrls = pictureUrls,
                    onImageClick = { url -> selectedImageUrl = url }
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }

    // Fullscreen image dialog
    selectedImageUrl?.let { url ->
        ImageFullscreenDialog(
            imageUrl = url,
            onDismiss = { selectedImageUrl = null }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun NotesTab(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    TextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        placeholder = { Text("Write a note...", color = Color.Gray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun PicturesTab(
    pictureUrls: List<String>,
    onImageClick: (String) -> Unit
) {
    if (pictureUrls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pictures yet\nTap + to upload",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(400.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(pictureUrls) { url ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onImageClick(url) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = "Progress Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ImageFullscreenDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = "Fullscreen Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Calculate current streak based on habit frequency
 */
fun calculateCurrentStreak(habit: Habit): Int {
    val lastCompleted = habit.lastCompleted ?: return 0
    val now = System.currentTimeMillis()
    val intervalMillis = habit.frequency.toMillis()

    // Check if habit is still active (completed within the last interval)
    if (now - lastCompleted > intervalMillis) {
        return 0 // Streak broken
    }

    // For now, return 1 if completed recently
    // In a full app, you'd track completion history in Firestore
    return 1
}

/**
 * Calculate skipped days based on when habit was created and frequency
 */
fun calculateSkippedDays(habit: Habit): Int {
    // This requires tracking habit creation date in Firestore
    // For now, we'll use a simplified calculation

    val lastCompleted = habit.lastCompleted ?: return 0
    val now = System.currentTimeMillis()
    val intervalMillis = habit.frequency.toMillis()

    // Calculate how many intervals have passed since last completion
    val missedIntervals = ((now - lastCompleted) / intervalMillis).toInt()

    // Subtract 1 because we're within the current interval
    val skippedCount = (missedIntervals - 1).coerceAtLeast(0)

    // Convert to days for display
    val intervalDays = TimeUnit.MILLISECONDS.toDays(intervalMillis).toInt()
    return skippedCount * intervalDays.coerceAtLeast(1)
}