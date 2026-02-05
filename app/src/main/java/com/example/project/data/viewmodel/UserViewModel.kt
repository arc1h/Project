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
    private var notificationsListener: ListenerRegistration? = null

    var username by mutableStateOf<String?>(null)
        private set

    // Must be mutableStateOf so the Badge in AccountScreen updates automatically
    var notificationCount by mutableIntStateOf(0)
        private set

    private var requestsCount = 0
    private var remindersCount = 0

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
                            username = snapshot.getString("username") ?: "User"

                            // Count 1: Friend Requests Array
                            val receivedRequests = snapshot.get("receivedFriendRequests") as? List<String> ?: emptyList()
                            requestsCount = receivedRequests.size
                            updateTotalCount()
                        }
                        isLoading = false
                    }
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    fun listenToNotifications() {
        val uid = auth.currentUser?.uid ?: return
        notificationsListener?.remove()

        notificationsListener = firestore.collection("users").document(uid).collection("notifications")
            .addSnapshotListener { snapshot, _ ->
                // Count 2: Sub-collection documents
                remindersCount = snapshot?.size() ?: 0
                updateTotalCount()
            }
    }

    private fun updateTotalCount() {
        notificationCount = requestsCount + remindersCount
    }

    fun clearBadge() {
        notificationCount = 0
    }

    fun refresh() {
        reset()         // Clears all existing listeners and state
        loadUserData()  // Starts the main user document listener
        // Note: listenToNotifications() is usually called by the
        // LaunchedEffect in AccountScreen/Notifications, but you can
        // also trigger it here if you want it active immediately.
        listenToNotifications()
    }

    fun reset() {
        listenerRegistration?.remove()
        notificationsListener?.remove()
        listenerRegistration = null
        notificationsListener = null
        username = null
        notificationCount = 0
        requestsCount = 0
        remindersCount = 0
        isLoading = true
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