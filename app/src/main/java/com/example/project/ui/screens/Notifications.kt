package com.example.project.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project.data.util.acceptFriendRequest
import com.example.project.data.util.cancelFriendRequest
import com.example.project.data.util.declineFriendRequest
import com.example.project.data.util.scheduleHabitAlarm
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.ui.theme.LightGray
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// --- Data Models ---

data class FriendRequest(
    val requesterId: String,
    val requesterUsername: String
)

data class InAppNotification(
    val title: String,
    val timestamp: String,
    val type: String // "REMINDER" or "FRIEND_REQUEST"
)

// --- Main Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Notifications(navController: NavController? = null, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val firestore = Firebase.firestore
    val currentUserId = Firebase.auth.currentUser?.uid
    val prefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var friendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var inAppNotifications by remember { mutableStateOf<List<InAppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var reminderTime by remember {
        mutableStateOf(prefs.getString("reminder_time", "Not Set") ?: "Not Set")
    }

    val isMasterEnabled = prefs.getBoolean("notifications_enabled", true)

    val timePickerDialog = TimePickerDialog(context, { _, hour, minute ->
        if (!isMasterEnabled) {
            Toast.makeText(context, "Enable 'Master Notifications' in Settings first!", Toast.LENGTH_LONG).show()
        } else {
            val formattedTime = String.format("%02d:%02d", hour, minute)
            reminderTime = formattedTime
            prefs.edit().putString("reminder_time", formattedTime).apply()
            scheduleHabitAlarm(context, "Daily Reminder", hour, minute)
            Toast.makeText(context, "Daily reminder set for $formattedTime", Toast.LENGTH_SHORT).show()
        }
    }, 12, 0, false)

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            isLoading = false
            onDispose { }
        } else {
            // Logic: Clear the badge visual when the user enters the screen
            userViewModel.clearBadge()

            val historyRegistration = firestore.collection("users").document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error -> // Added 'error' back in
                    if (error == null && snapshot != null) {
                        inAppNotifications = snapshot.documents.map { doc ->
                            InAppNotification(
                                title = doc.getString("title") ?: "Reminder",
                                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(doc.getLong("timestamp") ?: 0L)),
                                type = doc.getString("type") ?: "REMINDER"
                            )
                        }.distinctBy { it.title + it.timestamp }
                    }
                    isLoading = false
                }

            val userDocReg = firestore.collection("users").document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    val requestIds = snapshot?.get("receivedFriendRequests") as? List<String> ?: emptyList()

                    if (requestIds.isEmpty()) {
                        friendRequests = emptyList()
                    } else {
                        scope.launch {
                            val updatedList = mutableListOf<FriendRequest>()
                            for (id in requestIds) {
                                try {
                                    val userDoc = firestore.collection("users").document(id).get().await()
                                    updatedList.add(FriendRequest(
                                        requesterId = id,
                                        requesterUsername = userDoc.getString("username") ?: "Unknown User"
                                    ))
                                } catch (e: Exception) {
                                    Log.e("Notifs", "Error fetching user $id", e)
                                }
                            }
                            friendRequests = updatedList
                        }
                    }
                }

            onDispose {
                historyRegistration.remove()
                userDocReg.remove()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
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
            // REMOVE the old "Notifications" Text() and Spacer() from here
            // because they are now in the TopAppBar

            Spacer(modifier = Modifier.height(8.dp)) // Small breathing room after TopAppBar

            // Daily Reminder Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Reminder",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (reminderTime == "Not Set") "Keep your streak alive!" else "Set for $reminderTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { timePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Time")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Habits & Requests", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (friendRequests.isEmpty() && inAppNotifications.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Friend Requests Section
                    items(friendRequests) { request ->
                        FriendRequestCard(
                            request = request,
                            onAccept = {
                                scope.launch {
                                    currentUserId?.let { uid ->
                                        // Call your utility function
                                        acceptFriendRequest(uid, request.requesterId)
                                        // The SnapshotListener in Notifications.kt will
                                        // automatically refresh the list for us!
                                        Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    currentUserId?.let { uid ->
                                        // Use the correct semantic function
                                        declineFriendRequest(uid, request.requesterId)
                                        Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    // 2. Habit History Notifications Section
                    items(inAppNotifications) { notification ->
                        NotificationHistoryCard(notification)
                    }
                }

                if (inAppNotifications.isNotEmpty()) {
                    TextButton(
                        onClick = { clearAllNotifications(currentUserId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear All History")
                    }
                }
            }
        }
    }
}

// --- Supporting UI Components ---

@Composable
fun NotificationHistoryCard(notification: InAppNotification) {
    val isNudge = notification.type == "NUDGE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNudge)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isNudge) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isNudge) Icons.Default.Favorite else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (isNudge) MaterialTheme.colorScheme.secondary else Color.Gray,
                modifier = Modifier.size(24.dp).padding(end = 12.dp)
            )
            Column {
                Text(notification.title, style = MaterialTheme.typography.bodyLarge)
                Text(notification.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun FriendRequestCard(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LightGray),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${request.requesterUsername} sent you a friend request",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No notifications",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "You're all caught up!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

fun clearAllNotifications(uid: String?) {
    if (uid == null) return
    val db = Firebase.firestore

    // Batch delete is better for performance
    db.collection("users").document(uid).collection("notifications")
        .get()
        .addOnSuccessListener { snapshot ->
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit()
        }
}