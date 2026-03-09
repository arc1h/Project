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
import com.google.firebase.firestore.FieldValue
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
        description = "No pain, no gain. Conquer 2 habits marked as \'Hard\' difficulty.",
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

    private var _hero = mutableStateOf<Hero?>(null)
    var hero: androidx.compose.runtime.State<Hero?> = _hero

    var activeChallenges = mutableStateOf<List<Challenge>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    private var heroListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var challengesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        refresh()
        runOneTimeDataMigration()
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return

        heroListenerRegistration?.remove()

        heroListenerRegistration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HeroVM", "Hero listener failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _hero.value = snapshot.toHero()
                }
            }

        // Fetch challenges properly when a user logs in / refreshes
        viewModelScope.launch {
            syncWeeklyChallenges()
            loadActiveChallenges()
        }
    }

    private fun DocumentSnapshot.toHero(): Hero {
        val xp = getLong("xp")?.toInt() ?: 0
        val coins = getLong("coins")?.toInt() ?: 0
        val calculatedLevel = xp / 1000

        return Hero(
            userId = id,
            level = calculatedLevel,
            xp = xp,
            coins = coins,
            char = getString("char") ?: "0",
            challengesCompleted = getLong("challengesCompleted")?.toInt() ?: 0,
            longestStreak = getLong("longestStreak")?.toInt() ?: 0,
        )
    }

    private fun getCurrentWeekOfYear(): Int = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

    private fun loadActiveChallenges() {
        val userId = auth.currentUser?.uid ?: return

        challengesListenerRegistration?.remove()

        challengesListenerRegistration = firestore.collection("users").document(userId)
            .collection("challenges")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HeroVM", "Challenges listener failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val challengeList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Challenge::class.java)?.copy(id = doc.id)
                    }
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

                if (lastSyncedWeek != currentWeek) {
                    val newChallenges = CHALLENGE_POOL.shuffled().take(2)
                    val challengesRef = userRef.collection("challenges")

                    val oldChallenges = challengesRef.get().await()
                    val batch = firestore.batch()
                    oldChallenges.documents.forEach { batch.delete(it.reference) }

                    newChallenges.forEach { challenge ->
                        val newRef = challengesRef.document()
                        batch.set(newRef, challenge.copy(id = newRef.id, isCompleted = false))
                    }

                    batch.update(userRef, "lastSyncedWeek", currentWeek)
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.e("HeroVM", "Sync failed: ${e.message}")
            }
        }
    }

    fun addXP(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.runTransaction {
                    val userRef = firestore.collection("users").document(userId)
                    val snapshot = it.get(userRef)
                    val currentXp = snapshot.getLong("xp") ?: 0L
                    val newXp = currentXp + amount
                    val newLevel = newXp / 1000

                    it.update(userRef, mapOf(
                        "xp" to newXp,
                        "level" to newLevel
                    ))
                }.await()
            } catch (e: Exception) {
                Log.e("HeroVM", "Error adding XP: ${e.message}")
            }
        }
    }

    fun addCoins(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("coins", FieldValue.increment(amount.toLong()))
                    .await()
            } catch (e: Exception) {
                Log.e("HeroVM", "Error adding coins: ${e.message}")
            }
        }
    }

    fun completeChallenge(challenge: Challenge) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(userId)
                    val challengeRef = userRef.collection("challenges").document(challenge.id)

                    transaction.update(challengeRef, mapOf(
                        "completed" to true,
                        "progress" to challenge.goal
                    ))

                    val userSnapshot = transaction.get(userRef)
                    val currentXp = userSnapshot.getLong("xp") ?: 0L
                    val currentCoins = userSnapshot.getLong("coins") ?: 0L
                    val challengesCompleted = userSnapshot.getLong("challengesCompleted") ?: 0L

                    val newXp = currentXp + challenge.xpReward
                    val newLevel = newXp / 1000
                    val newCoins = currentCoins + challenge.coinReward

                    transaction.update(userRef, mapOf(
                        "xp" to newXp,
                        "level" to newLevel,
                        "coins" to newCoins,
                        "challengesCompleted" to challengesCompleted + 1
                    ))
                }.await()
            } catch (e: Exception) {
                Log.e("HeroVM", "Error completing challenge: ${e.message}")
            }
        }
    }

    fun updateHeroAppearance(charId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("char", charId)
            .addOnFailureListener {
                Log.e("HERO_VM", "Appearance update failed", it)
            }
    }

    fun incrementChallengeProgress(challengeType: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId)
                    .collection("challenges")
                    .whereEqualTo("completed", false)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val challenge = doc.toObject(Challenge::class.java)?.copy(id = doc.id) ?: continue

                    val isMatch = when (challengeType) {
                        "habit_completed" -> challenge.title.contains("Consistency", ignoreCase = true)
                        "morning_habit" -> challenge.title.contains("Bird", ignoreCase = true)
                        "hard_habit" -> challenge.title.contains("Hard", ignoreCase = true)
                        "weekend" -> challenge.title.contains("Weekend", ignoreCase = true)
                        "night_owl" -> challenge.title.contains("Night", ignoreCase = true)
                        "streak" -> challenge.title.contains("Streak", ignoreCase = true)
                        else -> false
                    }

                    if (isMatch) {
                        val newProgress = challenge.progress + 1
                        if (newProgress >= challenge.goal) {
                            completeChallenge(challenge.copy(progress = newProgress))
                        } else {
                            doc.reference.update("progress", newProgress)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("HeroVM", "Error updating progress: ${e.message}")
            }
        }
    }

    private fun runOneTimeDataMigration() {
        viewModelScope.launch {
            try {
                Log.d("MIGRATION", "Starting user data migration...")
                val usersRef = firestore.collection("users")
                val snapshot = usersRef.get().await()
                val batch = firestore.batch()
                var migrationCount = 0

                for (doc in snapshot.documents) {
                    val currentXp = doc.getLong("xp") ?: 0L
                    val currentLevel = doc.getLong("level") ?: 0L
                    val correctLevel = currentXp / 1000

                    if (currentLevel != correctLevel) {
                        migrationCount++
                        val userRef = usersRef.document(doc.id)
                        batch.update(userRef, "level", correctLevel)
                        Log.d("MIGRATION", "User ${doc.id}: Stale level $currentLevel, correcting to $correctLevel")
                    }
                }

                if (migrationCount > 0) {
                    batch.commit().await()
                    Log.d("MIGRATION", "Successfully migrated $migrationCount users.")
                } else {
                    Log.d("MIGRATION", "No users needed migration.")
                }
            } catch (e: Exception) {
                Log.e("MIGRATION", "Data migration failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        heroListenerRegistration?.remove()
        challengesListenerRegistration?.remove()
    }
}