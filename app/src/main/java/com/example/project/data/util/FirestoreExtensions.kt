package com.example.project.data.util

import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Extension functions for Firestore conversions
 */

/**
 * Convert a Firestore DocumentSnapshot to a Habit object
 */
fun DocumentSnapshot.toHabit(): Habit? {
    return try {
        val frequencyString = getString("frequency") ?: "Daily (Every 1 Day)"
        Habit(
            id = id,
            name = getString("name") ?: "",
            frequency = Frequency.fromStorageString(frequencyString),
            lastCompleted = getLong("lastCompleted")
        )
    } catch (e: Exception) {
        null // Return null if conversion fails
    }
}

/**
 * Convert a Habit to a map for Firestore storage
 */
fun Habit.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "frequency" to frequency.toStorageString(),
        "lastCompleted" to lastCompleted
    )
}