package com.example.project.data.model

import com.google.firebase.firestore.PropertyName

data class Hero(
    val userId: String = "",
    val level: Int = 0,
    val xp: Int = 0,
    val coins: Int = 0,
    val challengesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val char: String = "0" // "0" maps to hero00/hero01, "1" to hero10/hero11
)

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Int = 0,
    val coinReward: Int = 0,

    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,

    @get:PropertyName("progress")
    @set:PropertyName("progress")
    var progress: Int = 0, // Changed from val to var

    @get:PropertyName("goal")
    @set:PropertyName("goal")
    var goal: Int = 1 // Changed from val to var
)