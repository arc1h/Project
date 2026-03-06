package com.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project.data.model.Challenge
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun initializeNewUser(userId: String, username: String, email: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection("users").document(userId)

        val userData = hashMapOf(
            "username" to username,
            "email" to email,
            "uid" to userId,
            "xp" to 0,
            "coins" to 0,
            "level" to 0,
            "char" to "0",
            "challengesCompleted" to 0,
            "longestStreak" to 0
        )

        val challenges = listOf(
            Challenge(
                title = "Achieve a streak of 4 days",
                description = "Complete habits for 4 consecutive days",
                xpReward = 100,
                coinReward = 50,
                goal = 4
            ),
            Challenge(
                title = "Create a new habit",
                description = "Add a new habit to your list",
                xpReward = 50,
                coinReward = 25,
                goal = 1
            ),
            Challenge(
                title = "Complete 10 habits",
                description = "Mark 10 habits as done total",
                xpReward = 150,
                coinReward = 75,
                goal = 10
            )
        )

        val batch = db.batch()
        batch.set(userRef, userData)

        val challengesRef = userRef.collection("challenges")
        challenges.forEach { challenge ->
            val newChallengeRef = challengesRef.document()
            batch.set(newChallengeRef, challenge)
        }

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                errorMessage = "Failed to initialize user: ${it.message}"
                onComplete(false)
            }
    }

    fun handleAuth() {
        loading = true
        errorMessage = null

        if (isLoginMode) {
            val isEmail = username.contains("@")
            val query = if (isEmail) {
                db.collection("users").whereEqualTo("email", username)
            } else {
                db.collection("users").whereEqualTo("username", username)
            }
            query.get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val userEmail = result.documents[0].getString("email")
                        if (userEmail != null) {
                            auth.signInWithEmailAndPassword(userEmail, password)
                                .addOnCompleteListener { task ->
                                    loading = false
                                    if (task.isSuccessful) {
                                        onLoginSuccess()
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
                        errorMessage = if (isEmail) "Email not found" else "Username not found"
                    }
                }
                .addOnFailureListener { e ->
                    loading = false
                    errorMessage = "Error: ${e.message}"
                }

        } else {
            // REGISTER: First check if username is already taken in Firestore
            db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener { usernameResult ->
                    if (!usernameResult.isEmpty) {
                        loading = false
                        errorMessage = "Username is already taken"
                    } else {
                        // Username is free, now create the Auth account
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                                    
                                    // Initialize user and WAIT for completion before navigating
                                    initializeNewUser(userId, username, email) { success ->
                                        loading = false
                                        if (success) {
                                            onLoginSuccess()
                                            navController.navigate(Screen.Habits.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                    }
                                } else {
                                    loading = false
                                    errorMessage = task.exception?.message ?: "Registration failed"
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    loading = false
                    errorMessage = "Error checking username: ${it.message}"
                }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoginMode) "Welcome Back!" else "Create Account",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !loading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (loading)
                        if (isLoginMode) "Logging in..." else "Registering..."
                    else
                        if (isLoginMode) "Login" else "Register",
                    style = MaterialTheme.typography.bodyLarge
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
