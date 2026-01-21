package com.example.project.data.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.example.project.data.util.toFirestoreMap
import com.example.project.data.util.toHabit
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore

class HabitViewModel : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val _habits = mutableStateListOf<Habit>()
    val habits: List<Habit> = _habits

    private var listenerRegistration: ListenerRegistration? = null
    private val TAG = "HabitViewModel"

    init {
        loadHabits()
    }

    fun loadHabits() {
        val uid = auth.currentUser?.uid ?: return

        // Remove existing listener if any
        listenerRegistration?.remove()

        // Set up real-time listener
        listenerRegistration = firestore.collection("users").document(uid)
            .collection("habits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading habits", error)
                    return@addSnapshotListener
                }

                // Clear and rebuild from snapshot (single source of truth)
                _habits.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toHabit()?.let { habit ->
                        _habits.add(habit)
                    }
                }
            }
    }

    fun markHabitAsDone(habit: Habit) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        firestore.collection("users").document(uid)
            .collection("habits")
            .document(habit.id)
            .update("lastCompleted", now)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error marking habit as done", e)
            }
    }

    fun addHabit(name: String, frequency: Frequency) {
        val uid = auth.currentUser?.uid ?: return

        val newHabit = Habit(
            name = name,
            frequency = frequency,
            lastCompleted = null
        )

        firestore.collection("users").document(uid)
            .collection("habits")
            .add(newHabit.toFirestoreMap())
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding habit", e)
            }
    }

    fun editHabit(habit: Habit, newName: String, newFrequency: Frequency) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid)
            .collection("habits")
            .document(habit.id)
            .update(
                mapOf(
                    "name" to newName,
                    "frequency" to newFrequency.toStorageString()
                )
            )
            .addOnFailureListener { e ->
                Log.e(TAG, "Error editing habit", e)
            }
    }

    fun deleteHabit(habit: Habit) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid)
            .collection("habits")
            .document(habit.id)
            .delete()
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting habit", e)
            }
    }

    /**
     * Public method to reset habits when user logs out
     */
    fun reset() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _habits.clear()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listener to prevent memory leaks
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}