package com.example.project.data.util

import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.example.project.data.model.HabitDifficulty
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toHabit(): Habit? {
    val data = data ?: return null
    return try {
        Habit(
            id = id,
            name = getString("name") ?: "",
            frequency = Frequency.fromStorageString(getString("frequency") ?: "DAILY|1|NONE"),
            lastCompleted = getLong("lastCompleted"),
            // ADD THIS: Maps the Firestore field "completed" to our Kotlin "isCompleted"
            isCompleted = getBoolean("completed") ?: false,
            difficulty = HabitDifficulty.valueOf(getString("difficulty") ?: "EASY"),
            streak = getLong("streak")?.toInt() ?: 0,
            skippedCount = getLong("skippedCount")?.toInt() ?: 0,
            reminderTime = getString("reminderTime") ?: "Off"
        )
    } catch (e: Exception) {
        null
    }
}

fun Habit.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "frequency" to frequency.toStorageString(),
        "lastCompleted" to lastCompleted,
        "completed" to isCompleted, // ADD THIS
        "difficulty" to difficulty.name,
        "streak" to streak,
        "skippedCount" to skippedCount,
        "reminderTime" to reminderTime
    )
}