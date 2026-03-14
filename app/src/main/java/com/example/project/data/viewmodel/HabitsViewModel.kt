package com.example.project.data.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
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
        val db = Firebase.firestore

        val xpGained = when(habit.difficulty) {
            HabitDifficulty.EASY -> 50L
            HabitDifficulty.MODERATE -> 100L
            HabitDifficulty.HARD -> 200L
        }
        val coinsGained = xpGained / 10
        val newStreakValue = habit.streak + 1

        val userRef = db.collection("users").document(uid)
        val habitRef = userRef.collection("habits").document(habit.id)
        val historyRef = userRef.collection("habitHistory").document()

        db.runBatch { batch ->
            batch.update(habitRef, mapOf(
                "completed" to true,
                "lastCompleted" to System.currentTimeMillis(),
                "streak" to newStreakValue
            ))

            batch.update(userRef, mapOf(
                "xp" to FieldValue.increment(xpGained),
                "coins" to FieldValue.increment(coinsGained)
            ))

            batch.set(historyRef, hashMapOf(
                "habitName" to habit.name,
                "timestamp" to com.google.firebase.Timestamp.now(), // Firestore timestamp
                "xpGained" to xpGained.toInt(),
                "coinsGained" to coinsGained.toInt(),
                "difficulty" to habit.difficulty.toDisplayString(),
                "streakAtTime" to newStreakValue,
                "habitId" to habit.id
            ))
        }.addOnSuccessListener {
            userRef.get().addOnSuccessListener { snapshot ->
                val globalBest = snapshot.getLong("longestStreak") ?: 0L
                if (newStreakValue > globalBest) {
                    userRef.update("longestStreak", newStreakValue)
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to mark habit as done: ${e.message}")
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