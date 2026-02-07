package com.example.project.data.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.data.model.Challenge
import com.example.project.data.model.Hero
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

val CHALLENGE_POOL = listOf(
    Challenge(
        title = "Consistency Star",
        description = "Prove your resolve! Complete any 10 habit check-ins.",
        goal = 10, xpReward = 500, coinReward = 50
    ),
    Challenge(
        title = "Early Bird",
        description = "The early hero catches the XP. Finish 3 habits before 11 AM.",
        goal = 3, xpReward = 300, coinReward = 20
    ),
    Challenge(
        title = "Hard Mode",
        description = "No pain, no gain. Conquer 2 habits marked as 'Hard' difficulty.",
        goal = 2, xpReward = 600, coinReward = 100
    ),
    Challenge(
        title = "Weekend Warrior",
        description = "Evil never rests, and neither do you. Finish 5 habits on a Sunday.",
        goal = 5, xpReward = 400, coinReward = 30
    ),
    Challenge(
        title = "Habit Streak",
        description = "Keep the flame alive! Reach a 3-day streak on any habit.",
        goal = 3, xpReward = 450, coinReward = 40
    ),
    Challenge(
        title = "Night Owl",
        description = "Burning the midnight oil? Complete a habit after 8 PM.",
        goal = 1, xpReward = 200, coinReward = 10
    )
)

class HeroViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val firestore = Firebase.firestore

    // FIX: Define the private backing property
    private var _hero = mutableStateOf<Hero?>(null)
    // The public read-only version
    var hero: androidx.compose.runtime.State<Hero?> = _hero

    var activeChallenges = mutableStateOf<List<Challenge>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    init {
        refresh()
        // We launch a coroutine to handle the sequence correctly
        viewModelScope.launch {
            syncWeeklyChallenges()
            loadActiveChallenges()
        }
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return

        // This MUST be a snapshot listener, not a .get().await()
        firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    // This triggers the UI to recompose
                    _hero.value = snapshot.toHero()
                    Log.d("HERO_VM", "UI Refreshing: Longest Streak is now ${_hero.value?.longestStreak}")
                }
            }
    }

    fun DocumentSnapshot.toHero(): Hero {
        return Hero(
            userId = id,
            level = getLong("level")?.toInt() ?: 1,
            xp = getLong("xp")?.toInt() ?: 0,
            char = getString("char") ?: "0", // Default to "0"
            challengesCompleted = getLong("challengesCompleted")?.toInt() ?: 0,
            longestStreak = getLong("longestStreak")?.toInt() ?: 0,
        )
    }

    private fun loadHeroData() {
        val userId = auth.currentUser?.uid ?: return
        isLoading.value = true

        viewModelScope.launch {
            try {
                // Path Fix: Get data directly from the user document
                val heroDoc = db.collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (heroDoc.exists()) {
                    // Use .value to update the state within the 'val'
                    _hero.value = heroDoc.toObject(Hero::class.java)
                } else {
                    val newHero = Hero(userId = userId)
                    db.collection("users")
                        .document(userId)
                        .set(newHero)
                        .await()
                    _hero.value = newHero
                }
            } catch (e: Exception) {
                Log.e("HeroVM", "Error loading hero: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun getCurrentWeekOfYear(): Int = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

    private fun loadActiveChallenges() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .collection("challenges")
            .whereEqualTo("completed", false) // Changed from "isCompleted" to "completed"
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HeroVM", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val challengeList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Challenge::class.java)?.copy(id = doc.id)
                    }
                    Log.d("HeroVM", "Challenges found: ${challengeList.size}")
                    activeChallenges.value = challengeList
                }
            }
    }

    fun syncWeeklyChallenges() {
        val uid = auth.currentUser?.uid ?: return
        val currentWeek = getCurrentWeekOfYear()
        val userRef = firestore.collection("users").document(uid)

        viewModelScope.launch {
            try {
                val userDoc = userRef.get().await()
                val lastSyncedWeek = userDoc.getLong("lastSyncedWeek")?.toInt() ?: -1

                // FORCE SYNC: Change the check to see if the week is different
                // If you want to force it right now to see them, use: if (true)
                if (lastSyncedWeek != currentWeek) {
                    Log.d("HeroVM", "New week detected. Generating challenges...")
                    val newChallenges = CHALLENGE_POOL.shuffled().take(2)

                    // 1. Clear old active challenges
                    val oldChallenges = userRef.collection("challenges").get().await()
                    val batch = firestore.batch()
                    oldChallenges.documents.forEach { batch.delete(it.reference) }

                    // 2. Add new ones
                    newChallenges.forEach { challenge ->
                        val newRef = userRef.collection("challenges").document()
                        // IMPORTANT: Ensure isCompleted is false
                        batch.set(newRef, challenge.copy(id = newRef.id, isCompleted = false))
                    }

                    // 3. Update the week tracker
                    batch.update(userRef, "lastSyncedWeek", currentWeek)
                    batch.commit().await()
                    Log.d("HeroVM", "Batch commit successful")
                }
            } catch (e: Exception) {
                Log.e("HeroVM", "Sync failed: ${e.message}")
            }
        }
    }

    fun addXP(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        val currentHero = _hero.value ?: return

        viewModelScope.launch {
            try {
                val newXP = currentHero.xp + amount
                val newLevel = (newXP / 1000) + 1

                db.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "xp" to newXP,
                            "level" to newLevel
                        )
                    ).await()

                // Local state update
                _hero.value = currentHero.copy(xp = newXP, level = newLevel)
            } catch (e: Exception) {
                Log.e("HeroVM", "Error adding XP: ${e.message}")
            }
        }
    }

    fun addCoins(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        val currentHero = _hero.value ?: return

        viewModelScope.launch {
            try {
                val newCoins = currentHero.coins + amount

                db.collection("users")
                    .document(userId)
                    .update("coins", newCoins)
                    .await()

                _hero.value = currentHero.copy(coins = newCoins)
            } catch (e: Exception) {
                Log.e("HeroVM", "Error adding coins: ${e.message}")
            }
        }
    }

    private fun calculateLevel(xp: Int): Int {
        return (Math.sqrt(xp / 100.0).toInt()) + 1
    }

    fun completeChallenge(challenge: Challenge) {
        val userId = auth.currentUser?.uid ?: return
        val currentHero = _hero.value ?: return

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("challenges")
                    .document(challenge.id)
                    .update(
                        mapOf(
                            "completed" to true,       // Matches @PropertyName
                            "progress" to challenge.goal // Visual fill
                        )
                    ).await()

                // Calculate new stats
                val newXP = currentHero.xp + challenge.xpReward
                val newCoins = currentHero.coins + challenge.coinReward
                val newChallengesCompleted = currentHero.challengesCompleted + 1
                val newLevel = calculateLevel(newXP)

                // Update user stats
                db.collection("users").document(userId).update(
                    mapOf(
                        "xp" to newXP,
                        "coins" to newCoins,
                        "challengesCompleted" to newChallengesCompleted,
                        "level" to newLevel
                    )
                ).await()

                // Refresh the list to remove the completed card
                loadActiveChallenges()

            } catch (e: Exception) {
                Log.e("HeroVM", "Error completing challenge: ${e.message}")
            }
        }
    }

    fun updateHeroAppearance(charId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("char", charId) // charId would be "0", "1", "2" etc.
            .addOnSuccessListener {
                Log.d("HERO_VM", "Appearance updated to $charId")
            }
    }

    fun checkRepeatingChallenges(uid: String) {
        val challengesRef = firestore.collection("users").document(uid).collection("challenges")

        challengesRef.whereEqualTo("type", "REPEATING_HABITS").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val progress = doc.getLong("progress") ?: 0L
                val goal = doc.getLong("goal") ?: 5L

                val newProgress = progress + 1

                if (newProgress >= goal) {
                    // 1. Give Bonus
                    addXP(500) // Huge bonus for finishing the challenge
                    // 2. Reset Progress to 0 instead of deleting
                    doc.reference.update("progress", 0)
                } else {
                    doc.reference.update("progress", newProgress)
                }
            }
        }
    }

    // In HeroViewModel.kt
    fun incrementChallengeProgress(challengeType: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val challenges = firestore.collection("users")
                    .document(userId)
                    .collection("challenges")
                    // Ensure this matches the field name in your Firestore DB
                    .whereEqualTo("completed", false)
                    .get()
                    .await()

                Log.d("HeroVM", "Found ${challenges.size()} active challenges to check")

                challenges.documents.forEach { doc ->
                    val challenge = doc.toObject(Challenge::class.java)?.copy(id = doc.id) ?: return@forEach

                    // Use 'challengeType' here to match the parameter above
                    val shouldIncrement = when (challengeType) {
                        "habit_completed" -> challenge.title.contains("Star", ignoreCase = true)
                        "morning_habit" -> challenge.title.contains("Bird", ignoreCase = true)
                        "hard_habit" -> challenge.title.contains("Hard", ignoreCase = true) // <--- This looks for the word "Hard"
                        "streak" -> challenge.title.contains("Streak", ignoreCase = true)
                        else -> false
                    }

                    if (shouldIncrement) {
                        val newProgress = challenge.progress + 1
                        if (newProgress >= challenge.goal) {
                            // Trigger completion if goal is hit
                            completeChallenge(challenge.copy(progress = newProgress))
                        } else {
                            // Update the specific field name "progress"
                            doc.reference.update("progress", newProgress).await()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HeroVM", "Error updating progress: ${e.message}")
            }
        }
    }
}