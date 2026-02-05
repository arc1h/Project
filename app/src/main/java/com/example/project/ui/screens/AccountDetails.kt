package com.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.navigation.Screen
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun AccountDetails(
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // User data from ViewModel
    val username = userViewModel.username ?: "Loading..."
    val email = currentUser?.email ?: ""
    val userId = currentUser?.uid ?: ""

    val scope = rememberCoroutineScope()

    // Dialog states
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Feedback states
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = "Account Details",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // User ID (read-only)
            Text(
                text = "User ID: ${userId.take(8)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Feedback message
            if (feedbackMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = feedbackMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = if (isError)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Account Information
            Text(
                text = "Account Information",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Username
            AccountDetailCard(
                label = "Username",
                value = username,
                buttonText = "Edit",
                onClick = { showEditUsernameDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email (read-only)
            AccountDetailCard(
                label = "Email",
                value = email,
                buttonText = null,
                onClick = { }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            AccountDetailCard(
                label = "Password",
                value = "••••••••",
                buttonText = "Change",
                onClick = { showChangePasswordDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Account Actions
            Text(
                text = "Account Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sign Out
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Danger Zone
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete Account")
            }
        }
    }

    // Edit Username Dialog
    if (showEditUsernameDialog) {
        EditUsernameDialog(
            currentUsername = username,
            onDismiss = { showEditUsernameDialog = false },
            onConfirm = { newUsername ->
                // Use the 'scope' we just defined to launch the suspend function
                scope.launch {
                    val success = userViewModel.updateUsername(newUsername)

                    if (success) {
                        feedbackMessage = "Username updated successfully"
                        isError = false
                        showEditUsernameDialog = false
                        // Note: No need to call refresh()!
                        // Your ViewModel's SnapshotListener will see the DB change
                        // and update the UI automatically.
                    } else {
                        feedbackMessage = "Username is already taken or update failed"
                        isError = true
                    }
                }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            user.updatePassword(newPassword)
                                .addOnSuccessListener {
                                    feedbackMessage = "Password changed successfully"
                                    isError = false
                                    showChangePasswordDialog = false
                                }
                                .addOnFailureListener { e ->
                                    feedbackMessage = "Failed to change password: ${e.message}"
                                    isError = true
                                }
                        }
                        .addOnFailureListener {
                            feedbackMessage = "Current password is incorrect"
                            isError = true
                        }
                } else {
                    feedbackMessage = "User not found"
                    isError = true
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password ->
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, password)

                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            // Delete Firestore data first
                            db.collection("users").document(userId).delete()
                                .addOnSuccessListener {
                                    // Then delete auth account
                                    user.delete()
                                        .addOnSuccessListener {
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            feedbackMessage = "Failed to delete account: ${e.message}"
                                            isError = true
                                        }
                                }
                                .addOnFailureListener { e ->
                                    feedbackMessage = "Failed to delete user data: ${e.message}"
                                    isError = true
                                }
                        }
                        .addOnFailureListener {
                            feedbackMessage = "Password is incorrect"
                            isError = true
                        }
                } else {
                    feedbackMessage = "User not found"
                    isError = true
                }
            }
        )
    }
}

@Composable
fun AccountDetailCard(
    label: String,
    value: String,
    buttonText: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (buttonText != null) {
                TextButton(onClick = onClick) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
fun EditUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Username") },
        text = {
            Column {
                Text(
                    text = "Enter your new username",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = {
                        newUsername = it
                        error = it.isBlank()
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) {
                        { Text("Username cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newUsername.isNotBlank()) {
                        onConfirm(newUsername)
                    } else {
                        error = true
                    }
                },
                enabled = newUsername.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Current Password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (currentPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    isError = error,
                    supportingText = if (error) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isBlank() -> {
                            error = true
                            errorMessage = "Enter current password"
                        }
                        newPassword.length < 6 -> {
                            error = true
                            errorMessage = "Password must be at least 6 characters"
                        }
                        newPassword != confirmPassword -> {
                            error = true
                            errorMessage = "Passwords don't match"
                        }
                        else -> {
                            onConfirm(currentPassword, newPassword)
                        }
                    }
                }
            ) {
                Text("Change Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        title = {
            Text(
                "Delete Account",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "⚠️ This action cannot be undone!",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Deleting your account will permanently remove:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• All your habits\n• Your hero progress\n• All account data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter your password to confirm") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = confirmDelete,
                        onCheckedChange = { confirmDelete = it }
                    )
                    Text(
                        text = "I understand this action is permanent",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank() && confirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete Forever")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}