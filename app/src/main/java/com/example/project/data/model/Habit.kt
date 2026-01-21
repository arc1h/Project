package com.example.project.data.model

data class Habit(
    val id: String = "",
    val name: String = "",
    val frequency: Frequency = Frequency.Daily(1),
    val lastCompleted: Long? = null // Unix timestamp in milliseconds
) {
    fun isDoneNow(): Boolean {
        val completedTime = lastCompleted ?: return false
        val now = System.currentTimeMillis()
        val intervalMillis = frequency.toMillis()
        return now - completedTime < intervalMillis
    }
}