package com.example.project.data.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.data.model.Challenge
import com.example.project.data.model.Hero
import com.example.project.data.model.HeroAppearance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HeroViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var hero = mutableStateOf<Hero?>(null)
        private set

    var activeChallenges = mutableStateOf<List<Challenge>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    init {
        loadHeroData()
        loadActiveChallenges()
    }

    fun refresh() {
        loadHeroData()
        loadActiveChallenges()
    }

    private fun loadHeroData() {
        val userId = auth.currentUser?.uid ?: return
        isLoading.value = true

        viewModelScope.launch {
            try {
                val heroDoc = db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("stats")
                    .get()
                    .await()

                if (heroDoc.exists()) {
                    hero.value = heroDoc.toObject(Hero::class.java)
                } else {
                    // Create default hero if it doesn't exist
                    val newHero = Hero(userId = userId)
                    db.collection("users")
                        .document(userId)
                        .collection("hero")
                        .document("stats")
                        .set(newHero)
                        .await()
                    hero.value = newHero
                }
            } catch (e: Exception) {
                println("Error loading hero: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun loadActiveChallenges() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val challengesSnapshot = db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("challenges")
                    .collection("active")
                    .whereEqualTo("isCompleted", false)
                    .get()
                    .await()

                activeChallenges.value = challengesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Challenge::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {
                println("Error loading challenges: ${e.message}")
            }
        }
    }

    fun updateHeroAppearance(appearance: HeroAppearance) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("stats")
                    .update("appearance", appearance)
                    .await()

                hero.value = hero.value?.copy(appearance = appearance)
            } catch (e: Exception) {
                println("Error updating appearance: ${e.message}")
            }
        }
    }

    fun addXP(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        val currentHero = hero.value ?: return

        viewModelScope.launch {
            try {
                val newXP = currentHero.xp + amount
                // FIX: Change 'newXp' to 'newXP' to match the line above
                val newLevel = newXP / 1000

                db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("stats")
                    .update(
                        mapOf(
                            "xp" to newXP,
                            "level" to newLevel
                        )
                    )
                    .await()

                // Also update the local state so the UI reflects the change immediately
                hero.value = currentHero.copy(xp = newXP, level = newLevel)
            } catch (e: Exception) {
                println("Error adding XP: ${e.message}")
            }
        }
    }

    fun addCoins(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        val currentHero = hero.value ?: return

        viewModelScope.launch {
            try {
                val newCoins = currentHero.coins + amount

                db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("stats")
                    .update("coins", newCoins)
                    .await()

                hero.value = currentHero.copy(coins = newCoins)
            } catch (e: Exception) {
                println("Error adding coins: ${e.message}")
            }
        }
    }

    private fun calculateLevel(xp: Int): Int {
        return (Math.sqrt(xp / 100.0).toInt()) + 1
    }

    fun completeChallenge(challenge: Challenge) {
        val userId = auth.currentUser?.uid ?: return
        val currentHero = hero.value ?: return

        viewModelScope.launch {
            try {
                // Mark challenge as completed
                db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("challenges")
                    .collection("active")
                    .document(challenge.id)
                    .update("isCompleted", true)
                    .await()

                // Update hero stats
                val newXP = currentHero.xp + challenge.xpReward
                val newCoins = currentHero.coins + challenge.coinReward
                val newChallengesCompleted = currentHero.challengesCompleted + 1
                val newLevel = calculateLevel(newXP)

                db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("stats")
                    .update(
                        mapOf(
                            "xp" to newXP,
                            "coins" to newCoins,
                            "challengesCompleted" to newChallengesCompleted,
                            "level" to newLevel
                        )
                    )
                    .await()

                hero.value = currentHero.copy(
                    xp = newXP,
                    coins = newCoins,
                    challengesCompleted = newChallengesCompleted,
                    level = newLevel
                )

                loadActiveChallenges()
            } catch (e: Exception) {
                println("Error completing challenge: ${e.message}")
            }
        }
    }

    fun incrementChallengeProgress(challengeType: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val challenges = db.collection("users")
                    .document(userId)
                    .collection("hero")
                    .document("challenges")
                    .collection("active")
                    .whereEqualTo("isCompleted", false)
                    .get()
                    .await()

                challenges.documents.forEach { doc ->
                    val challenge = doc.toObject(Challenge::class.java) ?: return@forEach

                    val shouldIncrement = when (challengeType) {
                        "habit_completed" -> challenge.title.contains("complete", ignoreCase = true)
                        "habit_created" -> challenge.title.contains("create", ignoreCase = true)
                        "streak" -> challenge.title.contains("streak", ignoreCase = true)
                        else -> false
                    }

                    if (shouldIncrement) {
                        val newProgress = challenge.progress + 1

                        if (newProgress >= challenge.goal) {
                            completeChallenge(challenge.copy(id = doc.id, progress = newProgress))
                        } else {
                            doc.reference.update("progress", newProgress).await()
                        }
                    }
                }

                loadActiveChallenges()
            } catch (e: Exception) {
                println("Error updating challenge progress: ${e.message}")
            }
        }
    }
}