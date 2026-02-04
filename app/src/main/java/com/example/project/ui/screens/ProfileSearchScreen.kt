package com.example.project.ui.screens

import android.R.attr.top
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
import com.example.project.data.util.sendFriendRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NONE
)

enum class FriendshipStatus {
    NONE,           // No relationship
    PENDING_SENT,   // Current user sent request
    PENDING_RECEIVED, // Current user received request
    FRIENDS         // Already friends
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSearchScreen(navController: NavController? = null) {
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = Firebase.auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    // Load all users and their friendship status
    LaunchedEffect(Unit) {
        isLoading = true
        currentUserId ?: return@LaunchedEffect

        try {
            val firestore = Firebase.firestore

            // Get current user's friend requests
            val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
            val sentRequests = currentUserDoc.get("sentFriendRequests") as? List<String> ?: emptyList()
            val receivedRequests = currentUserDoc.get("receivedFriendRequests") as? List<String> ?: emptyList()
            val friends = currentUserDoc.get("friends") as? List<String> ?: emptyList()

            // Get all users
            val snapshot = firestore.collection("users").get().await()

            val users = snapshot.documents.mapNotNull { doc ->
                if (doc.id != currentUserId) {
                    val status = when {
                        friends.contains(doc.id) -> FriendshipStatus.FRIENDS
                        sentRequests.contains(doc.id) -> FriendshipStatus.PENDING_SENT
                        receivedRequests.contains(doc.id) -> FriendshipStatus.PENDING_RECEIVED
                        else -> FriendshipStatus.NONE
                    }

                    UserProfile(
                        uid = doc.id,
                        username = doc.getString("username") ?: "Unknown",
                        friendshipStatus = status
                    )
                } else null
            }

            allUsers = users.sortedBy { it.username.lowercase() }
            filteredUsers = allUsers
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    // Filter users when search query changes
    LaunchedEffect(searchQuery) {
        filteredUsers = if (searchQuery.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(
                    "Profile Search",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .padding(top = 36.dp, bottom = 8.dp)
                ) },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }, modifier = Modifier.padding(top = 36.dp, bottom = 8.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search users...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Results
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredUsers.isEmpty() && searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No other users yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredUsers) { user ->
                            UserProfileCard(
                                user = user,
                                onAction = { action ->
                                    scope.launch {
                                        when (action) {
                                            UserAction.ADD_FRIEND -> sendFriendRequest(currentUserId!!, user.uid)
                                            UserAction.ACCEPT -> acceptFriendRequest(currentUserId!!, user.uid)
                                            UserAction.UNSEND -> cancelFriendRequest(currentUserId!!, user.uid)
                                        }
                                        // Refresh the list
                                        navController?.navigateUp()
                                        navController?.navigate("profile_search")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class UserAction {
    ADD_FRIEND,
    ACCEPT,
    UNSEND
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}