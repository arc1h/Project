// FILE LOCATION: app/src/main/java/com/example/project/data/util/FriendRequestUtils.kt
// Create this new file in the same util folder as FirestoreExtensions.kt

package com.example.project.data.util

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

/**
 * Send a friend request from currentUserId to targetUserId
 */
suspend fun sendFriendRequest(currentUserId: String, targetUserId: String) {
    val firestore = Firebase.firestore

    try {
        // Add to current user's sentFriendRequests
        firestore.collection("users").document(currentUserId)
            .update("sentFriendRequests", FieldValue.arrayUnion(targetUserId))
            .await()

        // Add to target user's receivedFriendRequests
        firestore.collection("users").document(targetUserId)
            .update("receivedFriendRequests", FieldValue.arrayUnion(currentUserId))
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}

/**
 * Accept a friend request
 */
suspend fun acceptFriendRequest(currentUserId: String, requesterId: String) {
    val firestore = Firebase.firestore

    try {
        // Remove from received requests and add to friends
        firestore.collection("users").document(currentUserId)
            .update(
                mapOf(
                    "receivedFriendRequests" to FieldValue.arrayRemove(requesterId),
                    "friends" to FieldValue.arrayUnion(requesterId)
                )
            )
            .await()

        // Remove from sent requests and add to friends
        firestore.collection("users").document(requesterId)
            .update(
                mapOf(
                    "sentFriendRequests" to FieldValue.arrayRemove(currentUserId),
                    "friends" to FieldValue.arrayUnion(currentUserId)
                )
            )
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}

/**
 * Cancel a friend request
 */
suspend fun cancelFriendRequest(currentUserId: String, targetUserId: String) {
    val firestore = Firebase.firestore

    try {
        // Remove from current user's sentFriendRequests
        firestore.collection("users").document(currentUserId)
            .update("sentFriendRequests", FieldValue.arrayRemove(targetUserId))
            .await()

        // Remove from target user's receivedFriendRequests
        firestore.collection("users").document(targetUserId)
            .update("receivedFriendRequests", FieldValue.arrayRemove(currentUserId))
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}

/**
 * Decline a friend request
 */
suspend fun declineFriendRequest(currentUserId: String, requesterId: String) {
    val firestore = Firebase.firestore

    try {
        // Remove from current user's receivedFriendRequests
        firestore.collection("users").document(currentUserId)
            .update("receivedFriendRequests", FieldValue.arrayRemove(requesterId))
            .await()

        // Remove from requester's sentFriendRequests
        firestore.collection("users").document(requesterId)
            .update("sentFriendRequests", FieldValue.arrayRemove(currentUserId))
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}