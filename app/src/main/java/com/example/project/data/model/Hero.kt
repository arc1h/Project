package com.example.project.data.model

data class Hero(
    val userId: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val challengesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val appearance: HeroAppearance = HeroAppearance()
)

data class HeroAppearance(
    val bodyColor: String = "#FFFFFF",
    val accessory: String = "none",
    val hat: String = "none"
)

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Int = 0,
    val coinReward: Int = 0,
    val isCompleted: Boolean = false,
    val progress: Int = 0,
    val goal: Int = 1
)