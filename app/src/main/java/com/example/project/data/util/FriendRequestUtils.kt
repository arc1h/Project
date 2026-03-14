package com.example.project.data.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

suspend fun sendFriendRequest(currentUserId: String, targetUserId: String) {
    val firestore = Firebase.firestore
    val batch = firestore.batch()

    val currentUserRef = firestore.collection("users").document(currentUserId)
    val targetUserRef = firestore.collection("users").document(targetUserId)
    val notificationRef = targetUserRef.collection("notifications").document()
    val notificationData = mapOf(
        "title" to "New friend request!",
        "timestamp" to System.currentTimeMillis(),
        "type" to "FRIEND_REQUEST",
        "senderId" to currentUserId
    )

    batch.update(currentUserRef, "sentFriendRequests", FieldValue.arrayUnion(targetUserId))
    batch.update(targetUserRef, "receivedFriendRequests", FieldValue.arrayUnion(currentUserId))
    batch.set(notificationRef, notificationData)

    try {
        batch.commit().await()
    } catch (e: Exception) {
        // Fallback for new accounts
        val data = mapOf("sentFriendRequests" to FieldValue.arrayUnion(targetUserId))
        firestore.collection("users").document(currentUserId).set(data, SetOptions.merge())
    }
}

suspend fun acceptFriendRequest(currentUserId: String, requesterId: String) {
    val firestore = Firebase.firestore
    val batch = firestore.batch()

    val currentUserRef = firestore.collection("users").document(currentUserId)
    val requesterRef = firestore.collection("users").document(requesterId)

    val notifications = firestore.collection("users").document(currentUserId)
        .collection("notifications")
        .whereEqualTo("type", "FRIEND_REQUEST")
        .get().await()

    for (doc in notifications.documents) {
        batch.delete(doc.reference)
    }

    // Update the person who clicked ACCEPT
    batch.update(currentUserRef,
        "receivedFriendRequests", com.google.firebase.firestore.FieldValue.arrayRemove(requesterId),
        "friends", com.google.firebase.firestore.FieldValue.arrayUnion(requesterId)
    )

    // Update the person who SENT the request
    batch.update(requesterRef,
        "sentFriendRequests", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId),
        "friends", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
    )

    try {
        batch.commit().await()
    } catch (e: Exception) {
        val data1 = mapOf("friends" to com.google.firebase.firestore.FieldValue.arrayUnion(requesterId))
        val data2 = mapOf("friends" to com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))

        firestore.collection("users").document(currentUserId).set(data1, com.google.firebase.firestore.SetOptions.merge())
        firestore.collection("users").document(requesterId).set(data2, com.google.firebase.firestore.SetOptions.merge())
    }
}

suspend fun cancelFriendRequest(currentUserId: String, targetUserId: String) {
    val firestore = Firebase.firestore
    val batch = firestore.batch()

    val currentUserRef = firestore.collection("users").document(currentUserId)
    val targetUserRef = firestore.collection("users").document(targetUserId)

    // 1. Remove IDs from both documents
    batch.update(currentUserRef, "sentFriendRequests", FieldValue.arrayRemove(targetUserId))
    batch.update(targetUserRef, "receivedFriendRequests", FieldValue.arrayRemove(currentUserId))

    // 2. Clear the notification without a query
    try {
        batch.commit().await()
    } catch (e: Exception) {
        Log.e("FriendDebug", "Cancel failed: ${e.message}")
    }
}

suspend fun declineFriendRequest(currentUserId: String, requesterId: String) {
    val firestore = Firebase.firestore
    val batch = firestore.batch()

    val currentUserRef = firestore.collection("users").document(currentUserId)
    val requesterRef = firestore.collection("users").document(requesterId)

    batch.update(currentUserRef, "receivedFriendRequests", FieldValue.arrayRemove(requesterId))
    batch.update(requesterRef, "sentFriendRequests", FieldValue.arrayRemove(currentUserId))

    try {
        batch.commit().await()
    } catch (e: Exception) {
        Log.e("FriendUtils", "Failed to decline request", e)
    }
}

suspend fun removeFriend(currentUserId: String, targetUserId: String) {
    val db = Firebase.firestore
    val batch = db.batch()

    val userRef = db.collection("users").document(currentUserId)
    val targetRef = db.collection("users").document(targetUserId)

    // 1. Clean up Current User's Document
    batch.update(userRef, mapOf(
        "friends" to com.google.firebase.firestore.FieldValue.arrayRemove(targetUserId),
        "sentFriendRequests" to com.google.firebase.firestore.FieldValue.arrayRemove(targetUserId),
        "receivedFriendRequests" to com.google.firebase.firestore.FieldValue.arrayRemove(targetUserId)
    ))

    // 2. Clean up Target User's Document
    batch.update(targetRef, mapOf(
        "friends" to com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId),
        "sentFriendRequests" to com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId),
        "receivedFriendRequests" to com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId)
    ))

    try {
        batch.commit().await()
    } catch (e: Exception) {
        android.util.Log.e("FriendUtils", "Error during unfriend batch", e)
    }
}