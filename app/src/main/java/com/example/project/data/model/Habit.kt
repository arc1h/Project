package com.example.project.data.model

import com.example.project.data.model.Frequency
import com.google.firebase.firestore.PropertyName

enum class HabitDifficulty(val xp: Long) {
    EASY(10L),
    MODERATE(20L),
    HARD(30L);

    fun toDisplayString() = name.lowercase().replaceFirstChar { it.uppercase() }
}
data class Habit(
    val id: String = "",
    val name: String = "",
    val frequency: Frequency = Frequency.Daily(1),
    val lastCompleted: Long? = null,

    // ADD THIS LINE
    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,

    val difficulty: HabitDifficulty = HabitDifficulty.EASY,
    val streak: Int = 0,
    val skippedCount: Int = 0,
    val reminderTime: String = "Off"
) {
    fun isDoneNow(): Boolean {
        val completedTime = lastCompleted ?: return false
        val now = System.currentTimeMillis()
        return now - completedTime < frequency.toMillis()
    }
}