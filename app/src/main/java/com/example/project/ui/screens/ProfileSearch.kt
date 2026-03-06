package com.example.project.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project.data.util.acceptFriendRequest
import com.example.project.data.util.cancelFriendRequest
import com.example.project.data.util.removeFriend
import com.example.project.data.util.sendFriendRequest
import com.example.project.ui.theme.LightGray
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NONE,
    val char: String = "0"
)

enum class UserAction {
    ADD_FRIEND,
    ACCEPT,
    UNSEND,
    UNFRIEND
}

enum class FriendshipStatus {
    NONE,           // No relationship
    PENDING_SENT,   // Current user sent request
    PENDING_RECEIVED, // Current user received request
    FRIENDS         // Already friends
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSearch(navController: NavController? = null) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var rawUsers by remember { mutableStateOf<List<com.google.firebase.firestore.DocumentSnapshot>>(emptyList()) }
    var sentIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var receivedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var friendIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentUserId = Firebase.auth.currentUser?.uid
    val firestore = Firebase.firestore
    val scope = rememberCoroutineScope()
    var userToUnfriend by remember { mutableStateOf<UserProfile?>(null) }
    var processingIds by remember { mutableStateOf(setOf<String>()) }

    androidx.compose.runtime.DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val myDocListener = firestore.collection("users").document(currentUserId)
            .addSnapshotListener { doc, _ ->
                if (doc != null) {
                    // Only update if the document actually exists to avoid flicker on deletion
                    sentIds = doc.get("sentFriendRequests") as? List<String> ?: emptyList()
                    receivedIds = doc.get("receivedFriendRequests") as? List<String> ?: emptyList()
                    friendIds = doc.get("friends") as? List<String> ?: emptyList()
                }
            }

        val usersListener = firestore.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) { rawUsers = snapshot.documents }
                isLoading = false
            }

        onDispose {
            myDocListener.remove()
            usersListener.remove()
        }
    }

    val allUsersMapped = remember(rawUsers, sentIds, receivedIds, friendIds) {
        rawUsers.mapNotNull { doc ->
            if (doc.id == currentUserId) return@mapNotNull null
            val status = when {
                friendIds.contains(doc.id) -> FriendshipStatus.FRIENDS
                sentIds.contains(doc.id) -> FriendshipStatus.PENDING_SENT
                receivedIds.contains(doc.id) -> FriendshipStatus.PENDING_RECEIVED
                else -> FriendshipStatus.NONE
            }
            UserProfile(uid = doc.id, username = doc.getString("username") ?: "Unknown", friendshipStatus = status)
        }.sortedBy { it.username.lowercase() }
    }

    val filteredUsers = remember(searchQuery, allUsersMapped) {
        if (searchQuery.isBlank()) allUsersMapped
        else allUsersMapped.filter { it.username.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profile Search", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)) },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search users...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filteredUsers, key = { it.uid }) { user ->
                        UserProfileCard(
                            user = user,
                            onAction = { action ->
                                scope.launch {
                                    val uid = currentUserId ?: return@launch
                                    try {
                                        when (action) {
                                            UserAction.ADD_FRIEND -> {
                                                sentIds = sentIds + user.uid
                                                sendFriendRequest(uid, user.uid)
                                            }
                                            UserAction.UNSEND -> {
                                                if (processingIds.contains(user.uid)) return@launch

                                                // Start processing
                                                processingIds = processingIds + user.uid
                                                // Optimistic UI: Remove it immediately
                                                val previousSentIds = sentIds
                                                sentIds = sentIds - user.uid

                                                try {
                                                    cancelFriendRequest(uid, user.uid)
                                                    // If successful, we don't need to do anything,
                                                    // the listener will eventually confirm this.
                                                } catch (e: Exception) {
                                                    // ROLLBACK: If it fails, put the ID back so the button flips back to 'Unsend'
                                                    sentIds = previousSentIds
                                                    Log.e("ProfileSearch", "Unsend failed", e)
                                                } finally {
                                                    processingIds = processingIds - user.uid
                                                }
                                            }
                                            UserAction.ACCEPT -> {
                                                receivedIds = receivedIds - user.uid
                                                friendIds = friendIds + user.uid
                                                acceptFriendRequest(uid, user.uid)
                                            }
                                            UserAction.UNFRIEND -> userToUnfriend = user
                                        }
                                    } catch (e: Exception) {
                                        // If network fails, the listener will eventually sync back to reality
                                        Log.e("ProfileSearch", "Action $action failed", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Alert Dialog for Unfriend remains the same...
        if (userToUnfriend != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { userToUnfriend = null },
                title = { Text("Remove Friend") },
                text = { Text("Are you sure you want to remove ${userToUnfriend?.username}?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        scope.launch {
                            val uid = currentUserId ?: return@launch
                            removeFriend(uid, userToUnfriend!!.uid)
                            userToUnfriend = null
                        }
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { userToUnfriend = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
@Composable
fun UserProfileCard(
    user: UserProfile,
    onAction: (UserAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Username
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Action Button
            when (user.friendshipStatus) {
                FriendshipStatus.NONE -> {
                    Button(
                        onClick = { onAction(UserAction.ADD_FRIEND) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB565D8)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Friend")
                    }
                }
                FriendshipStatus.PENDING_SENT -> {
                    OutlinedButton(
                        onClick = { onAction(UserAction.UNSEND) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Unsend")
                    }
                }
                FriendshipStatus.PENDING_RECEIVED -> {
                    Button(
                        onClick = { onAction(UserAction.ACCEPT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Accept")
                    }
                }
                FriendshipStatus.FRIENDS -> {
                    OutlinedButton(
                        onClick = { onAction(UserAction.UNFRIEND) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        ),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Unfriend")
                    }
                }
            }
        }
    }
}