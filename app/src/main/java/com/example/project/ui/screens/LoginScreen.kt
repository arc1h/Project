package com.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleAuth() {
        loading = true
        errorMessage = null

        if (isLoginMode) {
            // LOGIN: using username → fetch email → login with email/password
            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val userEmail = result.documents[0].getString("email")
                        if (userEmail != null) {
                            auth.signInWithEmailAndPassword(userEmail, password)
                                .addOnCompleteListener { task ->
                                    loading = false
                                    if (task.isSuccessful) {
                                        val currentUser = auth.currentUser
                                        currentUser?.let { user ->
                                            val userDoc = db.collection("users").document(user.uid)
                                            userDoc.get().addOnSuccessListener { doc ->
                                                if (!doc.exists()) {
                                                    // Create Firestore doc if missing
                                                    val userData = mapOf(
                                                        "username" to username,
                                                        "email" to user.email,
                                                        "uid" to user.uid
                                                    )
                                                    userDoc.set(userData)
                                                }
                                            }
                                        }

                                        // Refresh user data
                                        onLoginSuccess()

                                        // Navigate to habits
                                        navController.navigate(Screen.Habits.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        errorMessage = task.exception?.message ?: "Login failed"
                                    }
                                }
                        } else {
                            loading = false
                            errorMessage = "Email not found for user"
                        }
                    } else {
                        loading = false
                        errorMessage = "Username not found"
                    }
                }
                .addOnFailureListener {
                    loading = false
                    errorMessage = "Error: ${it.message}"
                }

        } else {
            // REGISTER: username + email + password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    loading = false
                    if (task.isSuccessful) {
                        val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                        val userData = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "uid" to userId
                        )

                        db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener {
                                // Refresh user data
                                onLoginSuccess()

                                navController.navigate(Screen.Habits.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Failed to save user: ${e.message}"
                            }
                    } else {
                        errorMessage = task.exception?.message ?: "Registration failed"
                    }
                }
        }
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoginMode) "Welcome Back!" else "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Username field (only for register)
            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Email field (hidden during login)
            if (!isLoginMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Username field for login mode (instead of email)
            if (isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { handleAuth() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text(
                    if (loading)
                        if (isLoginMode) "Logging in..." else "Registering..."
                    else
                        if (isLoginMode) "Login" else "Register"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(
                    if (isLoginMode)
                        "Don't have an account? Register"
                    else
                        "Already have an account? Login"
                )
            }
        }
    }
}