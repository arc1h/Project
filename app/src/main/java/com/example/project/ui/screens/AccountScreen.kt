package com.example.project.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.project.data.viewmodel.HabitViewModel
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.ui.theme.LightGray
import com.example.project.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun AccountScreen(
    navController: NavController? = null,
    userViewModel: UserViewModel,
    habitViewModel: HabitViewModel = viewModel(),
    onAccountDetails: () -> Unit = {},
    onAppSettings: () -> Unit = {}
) {
    val currentUser = Firebase.auth.currentUser
    val userName = userViewModel.username ?: "User"
    val notificationCount = userViewModel.notificationCount
    var joinedDate by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            userViewModel.listenToNotifications()

            // 1. Get the real metadata from Auth
            val timestamp = currentUser.metadata?.creationTimestamp

            if (timestamp != null && timestamp > 0) {
                // 2. Format for current UI
                val date = java.util.Date(timestamp)
                val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                joinedDate = formatter.format(date)

                // 3. Force-write the real timestamp to Firestore
                // We use set with merge so it creates the field if it doesn't exist
                Firebase.firestore.collection("users").document(currentUser.uid)
                    .set(mapOf("createdAt" to timestamp), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("FIX", "Successfully synced real date: $timestamp")
                    }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Your Account",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 20.dp)
                )

                // Name with edit icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold).copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = Purple.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Menu Options
                AccountMenuItem(
                    text = "Notifications",
                    onClick = {
                        navController?.navigate("notifications")
                    },
                    badge = if (notificationCount > 0) notificationCount else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountMenuItem(
                    text = "History",
                    onClick = {
                        navController?.navigate("history")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountMenuItem(
                    text = "Account Details",
                    onClick = {
                        navController?.navigate("account_details")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountMenuItem(
                    text = "App Settings",
                    onClick = {
                        navController?.navigate("app_settings")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountMenuItem(
                    text = "Profile Search",
                    onClick = {
                        navController?.navigate("profile_search")
                    }
                )

                if (joinedDate != null) {
                    Spacer(modifier = Modifier.height(64.dp))
                    Text(
                        text = "Joined in $joinedDate",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Bottom Section - Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        Firebase.auth.signOut() // 1. Sign out from Firebase
                        showLogoutDialog = false // 2. Close dialog
                        // 3. Navigate back to Login (and clear backstack)
                        navController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Log out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        EditNameDialog(
            currentName = userName,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                scope.launch {
                    val success = userViewModel.updateUsername(newName)
                    if (success) {
                        showEditNameDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun AccountMenuItem(
    text: String,
    onClick: () -> Unit,
    badge: Int? = null
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp), // Matches HabitCard
        border = BorderStroke(1.dp, LightGray), // Matches HabitCard border
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Flat design
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (badge != null && badge > 0) {
                Surface(
                    color = Purple,
                    shape = RoundedCornerShape(8.dp), // Sharper badge to match buttons
                    modifier = Modifier.height(24.dp).widthIn(min = 24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = badge.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Edit Username") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = null
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    enabled = !isLoading
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isBlank()) {
                        errorMessage = "Username cannot be empty"
                        return@Button
                    }
                    if (newName == currentName) {
                        onDismiss()
                        return@Button
                    }
                    isLoading = true
                    onConfirm(newName)
                },
                enabled = !isLoading && newName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}