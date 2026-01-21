package com.example.project.data.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val TAG = "UserViewModel"

    private var listenerRegistration: ListenerRegistration? = null

    var username by mutableStateOf<String?>(null)
        private set

    var notificationCount by mutableIntStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isLoading = false
            return
        }

        viewModelScope.launch {
            try {
                val uid = currentUser.uid

                // Set up real-time listener for user data
                listenerRegistration = firestore.collection("users")
                    .document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error loading user data", error)
                            username = currentUser.displayName ?: "User"
                            isLoading = false
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            username = snapshot.getString("username")
                                ?: currentUser.displayName
                                        ?: "User"

                            // Get notification count
                            val receivedRequests = snapshot.get("receivedFriendRequests") as? List<String>
                                ?: emptyList()
                            notificationCount = receivedRequests.size
                        } else {
                            username = currentUser.displayName ?: "User"
                        }

                        isLoading = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up user listener", e)
                username = currentUser.displayName ?: "User"
                isLoading = false
            }
        }
    }

    /**
     * Call this when user logs out to reset state
     */
    fun reset() {
        listenerRegistration?.remove()
        listenerRegistration = null
        username = null
        notificationCount = 0
        isLoading = true
    }

    /**
     * Call this when a new user logs in
     */
    fun refresh() {
        reset()
        loadUserData()
    }

    suspend fun updateUsername(newUsername: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        val uid = currentUser.uid

        return try {
            // Check if username is already taken
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", newUsername)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val existingUserId = querySnapshot.documents.firstOrNull()?.id
                if (existingUserId != uid) {
                    return false // Username taken
                }
            }

            // Update username
            firestore.collection("users")
                .document(uid)
                .update("username", newUsername)
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating username", e)
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}