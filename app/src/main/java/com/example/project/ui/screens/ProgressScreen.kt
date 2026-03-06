package com.example.project.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.project.data.viewmodel.HabitViewModel
import com.example.project.data.viewmodel.HeroViewModel
import com.example.project.ui.theme.LightGray
import com.example.project.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

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
    habitViewModel: HabitViewModel = viewModel(),
    heroViewModel: HeroViewModel = viewModel() // Added HeroViewModel
) {
    val currentUser = Firebase.auth.currentUser
    val habits = habitViewModel.habits
    // Observe the hero state from the ViewModel for real-time streak updates
    val hero by heroViewModel.hero

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var globalLongestRecord by remember { mutableIntStateOf(0) }

    var pictureUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // --- REACTIVE STATS CALCULATION ---
    val stats by remember(habits, hero) { // Remove globalLongestRecord, use hero
        derivedStateOf {
            val bestHabit = habits.maxByOrNull { it.streak }

            // Use the real-time streak from the hero listener
            val currentGlobalRecord = hero?.longestStreak ?: 0

            val startedHabits = habits.filter { it.streak > 0 }
            val worstHabit = startedHabits.minByOrNull { it.streak } ?: habits.firstOrNull()

            val skippedHabit = habits.filter { it.skippedCount > 0 }
                .maxByOrNull { it.skippedCount }

            ProgressStats(
                // Logic: Use whichever is higher—the current habit's streak or the saved record
                longestStreakHabit = bestHabit?.name ?: "No Habit",
                longestStreakDays = maxOf(currentGlobalRecord, bestHabit?.streak ?: 0),

                shortestStreakHabit = worstHabit?.name ?: "No Habit",
                shortestStreakDays = worstHabit?.streak ?: 0,

                mostSkippedHabit = skippedHabit?.name ?: "None",
                mostSkippedDays = skippedHabit?.skippedCount ?: 0
            )
        }
    }

    // --- LOAD DATA ON STARTUP ---
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val userDoc = Firebase.firestore.collection("users").document(uid).get().await()
                notes = userDoc.getString("notes") ?: ""
                globalLongestRecord = userDoc.getLong("longestStreak")?.toInt() ?: 0
                val savedPaths = userDoc.get("progressPictures") as? List<String>
                pictureUrls = savedPaths ?: emptyList()
            } catch (e: Exception) {
                Log.e("LoadError", "Failed: ${e.message}")
            }
        }
    }

    // --- SAVE NOTES WITH DEBOUNCE ---
    LaunchedEffect(notes) {
        delay(1000)
        currentUser?.uid?.let { uid ->
            Firebase.firestore.collection("users").document(uid).update("notes", notes)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        scope.launch(Dispatchers.IO) {
            try {
                val fileName = "progress_${System.currentTimeMillis()}.jpg"
                val destinationFile = File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destinationFile.outputStream().use { output -> input.copyTo(output) }
                }
                val localPath = destinationFile.absolutePath

                currentUser?.uid?.let { uid ->
                    Firebase.firestore.collection("users").document(uid)
                        .update("progressPictures", FieldValue.arrayUnion(localPath))
                        .addOnSuccessListener {
                            pictureUrls = pictureUrls + localPath
                            isUploading = false
                        }
                }
            } catch (e: Exception) { isUploading = false }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = Purple,
                    contentColor = Color.White
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Add Picture")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Progress", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(16.dp))

                // TOP ROW
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Find the actual habit object to get its frequency type
                    val longestHabitObj = habits.find { it.name == stats.longestStreakHabit }
                    StatCard(
                        title = "Longest Streak",
                        value = "${stats.longestStreakDays}",
                        habitName = stats.longestStreakHabit,
                        frequency = longestHabitObj?.frequency?.getTypeName() ?: "daily", // Fix: pass lowercase type
                        modifier = Modifier.weight(1f)
                    )

                    val shortestHabitObj = habits.find { it.name == stats.shortestStreakHabit }
                    StatCard(
                        title = "Shortest Streak",
                        value = "${stats.shortestStreakDays}",
                        habitName = stats.shortestStreakHabit,
                        frequency = shortestHabitObj?.frequency?.getTypeName() ?: "daily",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTTOM CARD (MOST SKIPPED)
                val skippedFreq = habits.find { it.name == stats.mostSkippedHabit }?.frequency?.getTypeName() ?: ""
                StatCard(
                    title = "Most Skipped",
                    value = "${stats.mostSkippedDays}",
                    habitName = stats.mostSkippedHabit,
                    frequency = skippedFreq,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- FIXED TABROW ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background, // Match background to prevent blending
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Purple
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = Purple,
                    unselectedContentColor = Color.Gray
                ) { Text("Notes", modifier = Modifier.padding(12.dp)) }

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = Purple,
                    unselectedContentColor = Color.Gray
                ) { Text("Pictures", modifier = Modifier.padding(12.dp)) }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> NotesTab(notes, onNotesChange = { notes = it })
                    1 -> PicturesTab(pictureUrls) { selectedImageUrl = it }
                }
            }
        }
    }

    // Dialogs...
    selectedImageUrl?.let { path ->
        ImageFullscreenDialog(
            imagePath = path,
            onDismiss = { selectedImageUrl = null },
            onDelete = { pathToDelete ->
                currentUser?.uid?.let { uid ->
                    Firebase.firestore.collection("users").document(uid)
                        .update("progressPictures", FieldValue.arrayRemove(pathToDelete))
                        .addOnSuccessListener {
                            File(pathToDelete).delete()
                            pictureUrls = pictureUrls.filter { it != pathToDelete }
                            selectedImageUrl = null
                        }
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    habitName: String,
    frequency: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LightGray),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = Color.Gray)
            Spacer(Modifier.height(2.dp))

            val unit = when (frequency.lowercase()) {
                "hourly" -> "h"
                "daily" -> "d"
                "weekly" -> "w"
                "monthly" -> "m"
                else -> ""
            }
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = habitName,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp).copy(fontWeight = FontWeight.Normal),
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PicturesTab(urls: List<String>, onImageClick: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp)) {
        items(urls) { path ->
            Card(
                modifier = Modifier.aspectRatio(1f).padding(4.dp).clickable { onImageClick(path) },
                border = BorderStroke(2.dp, LightGray),
            ) {
                Image(
                    // Wrap path in File() so Coil knows it's local
                    painter = rememberAsyncImagePainter(File(path)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun NotesTab(notes: String, onNotesChange: (String) -> Unit) {
    TextField(
        value = notes,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal),
        onValueChange = onNotesChange,
        modifier = Modifier.fillMaxSize(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
    )
}

@Composable
fun ImageFullscreenDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    // Local state to track if the confirmation dialog is open
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // THE IMAGE
            Image(
                painter = rememberAsyncImagePainter(File(imagePath)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDismiss() },
                contentScale = ContentScale.Fit
            )

            // TOP BUTTON BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Trash icon now just triggers the confirmation state
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }

            // ACTUAL CONFIRMATION DIALOG
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Picture?") },
                    text = { Text("This will permanently remove this progress photo from your records.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                onDelete(imagePath)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}