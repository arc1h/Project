package com.example.project.data.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.example.project.data.model.HabitDifficulty
import com.example.project.data.util.cancelHabitAlarm
import com.example.project.data.util.scheduleHabitAlarm
import com.example.project.data.util.toFirestoreMap
import com.example.project.data.util.toHabit
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HabitViewModel : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    var habits = mutableStateListOf<Habit>()
        private set

    private var listenerRegistration: ListenerRegistration? = null
    private val TAG = "HabitViewModel"

    init {
        loadHabits()
    }

    fun loadHabits() {
        val uid = auth.currentUser?.uid ?: return

        listenerRegistration?.remove()

        listenerRegistration = firestore.collection("users").document(uid)
            .collection("habits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading habits", error)
                    return@addSnapshotListener
                }

                habits.clear()
                snapshot?.documents?.forEach { doc ->
                    // Use your utility function explicitly
                    val habit = doc.toHabit()
                    if (habit != null) {
                        habits.add(habit)
                    }
                }
            }
    }

    fun markHabitAsDone(habit: Habit) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(uid)
        val habitRef = userRef.collection("habits").document(habit.id)
        val historyRef = userRef.collection("habitHistory").document() // New history doc

        firestore.runTransaction { transaction ->
            val habitSnap = transaction.get(habitRef)
            val userSnap = transaction.get(userRef)

            // Calculate inside the transaction
            val currentStreak = habitSnap.getLong("streak")?.toInt() ?: 0
            val newStreakValue = currentStreak + 1

            // 1. Update Habit
            transaction.update(habitRef, mapOf(
                "streak" to newStreakValue,
                "lastCompleted" to System.currentTimeMillis(),
                "completed" to true // Add this so the UI knows it's checked
            ))

            // 2. Update User (XP and Best Streak)
            val currentXp = userSnap.getLong("xp") ?: 0L
            val globalBest = userSnap.getLong("longestStreak")?.toInt() ?: 0

            val userUpdates = mutableMapOf<String, Any>("xp" to currentXp + habit.difficulty.xp)
            if (newStreakValue > globalBest) {
                userUpdates["longestStreak"] = newStreakValue
            }
            transaction.update(userRef, userUpdates)

            // 3. Log History
            transaction.set(historyRef, mapOf(
                "habitId" to habit.id,
                "habitName" to habit.name,
                "timestamp" to System.currentTimeMillis(),
                "streakAtTime" to newStreakValue
            ))

            // Return the new streak so it's available in the Success listener
            newStreakValue
        }.addOnSuccessListener { finalStreak ->
            Log.d("STREAK_DEBUG", "Success! Streak is now: $finalStreak")
        }.addOnFailureListener { e ->
            Log.e("STREAK_DEBUG", "Transaction failed: ${e.message}")
        }
    }

    fun addHabit(context: Context, name: String, frequency: Frequency, difficulty: HabitDifficulty, reminderTime: String) {
        val uid = auth.currentUser?.uid ?: return

        val newHabit = Habit(
            name = name,
            frequency = frequency,
            difficulty = difficulty,
            lastCompleted = null
        )

        // Handle the alarm scheduling here using the passed context
        if (reminderTime != "Off" && reminderTime != "Not Set") {
            val parts = reminderTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                scheduleHabitAlarm(context, name, hour, minute)
            }
        }

        firestore.collection("users").document(uid)
            .collection("habits")
            .add(newHabit.toFirestoreMap())
    }

    fun editHabit(context: Context, habit: Habit, newName: String, newFrequency: Frequency, newDifficulty: HabitDifficulty, newTime: String) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Cancel the old alarm based on the OLD name
        cancelHabitAlarm(context, habit.name)

        // 2. Schedule the new alarm if it's not turned off
        if (newTime != "Off" && newTime != "Not Set") {
            val parts = newTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                scheduleHabitAlarm(context, newName, hour, minute)
            }
        }

        // 3. Update Firestore
        firestore.collection("users").document(uid)
            .collection("habits")
            .document(habit.id)
            .update(
                mapOf(
                    "name" to newName,
                    "frequency" to newFrequency.toStorageString(),
                    "difficulty" to newDifficulty.name
                )
            )
            .addOnFailureListener { e -> Log.e(TAG, "Error editing habit", e) }
    }

    fun deleteHabit(context: Context, habit: Habit) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Cancel the system alarm so it doesn't fire for a deleted habit
        cancelHabitAlarm(context, habit.name)

        // 2. Delete from Firestore
        firestore.collection("users").document(uid)
            .collection("habits")
            .document(habit.id)
            .delete()
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting habit", e)
            }
    }

    fun toggleHabit(habit: Habit) {
        val uid = auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        // ... existing logic to calculate XP based on difficulty ...
        val xpGained = when(habit.difficulty) {
            HabitDifficulty.EASY -> 50
            HabitDifficulty.MODERATE -> 100
            HabitDifficulty.HARD -> 200
        }
        val coinsGained = xpGained / 10

        // 1. Update the Habit itself
        val habitRef = db.collection("users").document(uid)
            .collection("habits").document(habit.id)

        // 2. CREATE THE LOG ENTRY
        val logRef = db.collection("users").document(uid)
            .collection("logs").document() // Auto-generate ID

        val logData = hashMapOf(
            "habitName" to habit.name,
            "timestamp" to com.google.firebase.Timestamp.now(), // Crucial for ordering
            "xpGained" to xpGained,
            "coinsGained" to coinsGained,
            "difficulty" to habit.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
        )

        db.runBatch { batch ->
            // Update habit completion status
            batch.update(habitRef, "completed", true)
            batch.update(habitRef, "lastCompleted", System.currentTimeMillis())

            // Save the log
            batch.set(logRef, logData)
        }.addOnSuccessListener {
            Log.d("HabitVM", "Habit logged successfully!")
        }
    }

    fun skipHabit(habit: Habit) {
        val uid = auth.currentUser?.uid ?: return
        val habitRef = firestore.collection("users").document(uid)
            .collection("habits").document(habit.id)

        habitRef.update("skippedCount", FieldValue.increment(1))
            .addOnFailureListener { e -> Log.e(TAG, "Error skipping habit", e) }
    }

    fun reset() {
        listenerRegistration?.remove()
        listenerRegistration = null
        habits.clear()
    }
}