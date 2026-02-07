package com.example.project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HabitLog(
    val id: String = "",
    val habitName: String = "",
    val timestamp: String = "",
    val xpGained: Int = 0,
    val coinsGained: Int = 0,
    val difficulty: String = "Easy"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(navController: NavController) {
    val firestore = Firebase.firestore
    val currentUserId = Firebase.auth.currentUser?.uid

    var habitLogs by remember { mutableStateOf<List<HabitLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showWipeDialog by remember { mutableStateOf(false) }

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            isLoading = false
            onDispose { }
        } else {
            val historyRegistration = firestore.collection("users").document(currentUserId)
                .collection("logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        habitLogs = snapshot.documents.map { doc ->
                            HabitLog(
                                id = doc.id,
                                habitName = doc.getString("habitName") ?: "Unknown",
                                timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                    .format(doc.getTimestamp("timestamp")?.toDate() ?: Date()),
                                xpGained = doc.getLong("xpGained")?.toInt() ?: 0,
                                coinsGained = doc.getLong("coinsGained")?.toInt() ?: 0,
                                // Ensure this key "difficulty" matches exactly what you put in the transaction hashmap
                                difficulty = doc.getString("difficulty") ?: "Easy"
                            )
                        }
                    }
                    isLoading = false
                }
            onDispose { historyRegistration.remove() }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            // Note: The manual "History" Text and Spacer have been removed
            // as they are now part of the topBar.

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (habitLogs.isEmpty()) {
                HistoryEmptyState()
            } else {
                Spacer(modifier = Modifier.height(8.dp)) // Slight gap before the list starts

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(habitLogs) { log ->
                        HistoryLogCard(log)
                    }
                }

                if (habitLogs.isNotEmpty()) {
                    TextButton(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Wipe History")
                    }
                }
            }
        }
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Wipe History?") },
            text = { Text("This will permanently delete all your completion logs. Your level and XP will stay the same.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearAllLogs(currentUserId)
                        showWipeDialog = false
                    }
                ) { Text("Wipe", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HistoryLogCard(log: HabitLog) {

    val lightGrayBorder = Color.LightGray.copy(alpha = 0.5f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, lightGrayBorder),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(log.habitName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(log.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                // Difficulty Tag
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = log.difficulty,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("+${log.xpGained} XP", color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("+${log.coinsGained} Coins", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun HistoryEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hmm.. No logs yet!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Complete habits to see your progress here!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),)
        }
    }
}

fun clearAllLogs(uid: String?) {
    if (uid == null) return
    val db = Firebase.firestore
    db.collection("users").document(uid).collection("logs").get()
        .addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit()
        }
}