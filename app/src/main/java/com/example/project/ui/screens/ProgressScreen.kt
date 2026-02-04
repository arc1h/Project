package com.example.project.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
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
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var pictureUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var stats by remember { mutableStateOf(ProgressStats()) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            // 1. Create a unique filename
            val fileName = "progress_${System.currentTimeMillis()}.jpg"

            // 2. Get the app's internal "files" directory
            val destinationFile = File(context.filesDir, fileName)

            try {
                // 3. Copy the bits from the Gallery/Camera to your internal folder
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 4. Save the LOCAL PATH to Firestore (instead of a URL)
                val localPath = destinationFile.absolutePath
                currentUser?.uid?.let { uid ->
                    Firebase.firestore.collection("users").document(uid)
                        .update("progressPictures", FieldValue.arrayUnion(localPath))
                        .addOnSuccessListener {
                            pictureUrls = pictureUrls + localPath
                        }
                }
            } catch (e: Exception) {
                Log.e("UploadError", "Failed to save locally", e)
            }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            val doc = Firebase.firestore.collection("users").document(uid).get().await()
            notes = doc.getString("progressNotes") ?: ""
            val raw = doc.get("progressPictures") as? List<*>
            pictureUrls = raw?.filterIsInstance<String>() ?: emptyList()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { imagePickerLauncher.launch("image/*") }, containerColor = Purple) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            // Stats Section
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Records", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Longest Streak", "${stats.longestStreakDays} days", stats.longestStreakHabit, Modifier.weight(1f))
                    StatCard("Shortest Streak", "${stats.shortestStreakDays} days", stats.shortestStreakHabit, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                StatCard("Most Skipped", "${stats.mostSkippedDays} days", stats.mostSkippedHabit, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Purple) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Notes", modifier = Modifier.padding(12.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Pictures", modifier = Modifier.padding(12.dp)) }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> NotesTab(notes, onNotesChange = { notes = it })
                    1 -> PicturesTab(pictureUrls) { selectedImageUrl = it }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    selectedImageUrl?.let { path ->
        ImageFullscreenDialog(
            imagePath = path,
            onDismiss = { selectedImageUrl = null },
            onDelete = { pathToDelete ->
                currentUser?.uid?.let { uid ->
                    // 1. Remove from Firestore
                    Firebase.firestore.collection("users").document(uid)
                        .update("progressPictures", FieldValue.arrayRemove(pathToDelete))
                        .addOnSuccessListener {
                            // 2. Delete the actual file from internal storage
                            val file = File(pathToDelete)
                            if (file.exists()) file.delete()

                            // 3. Update the UI list
                            pictureUrls = pictureUrls.filter { it != pathToDelete }
                            selectedImageUrl = null // Close the dialog
                        }
                }
            }
        )
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun PicturesTab(urls: List<String>, onImageClick: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp)) {
        items(urls) { path ->
            Card(modifier = Modifier.aspectRatio(1f).padding(4.dp).clickable { onImageClick(path) }) {
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
        onValueChange = onNotesChange,
        modifier = Modifier.fillMaxSize(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun ImageFullscreenDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit // New callback for deletion
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // This allows it to go edge-to-edge
        )
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
                    .clickable { onDismiss() }, // Tap image to close
                contentScale = ContentScale.Fit // Ensures the whole photo is visible
            )

            // TOP BUTTON BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close Button
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Delete Button
                IconButton(onClick = { onDelete(imagePath) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    }
}