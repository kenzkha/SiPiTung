package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PintarHitungRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    fun observeProfile(userId: String): Flow<ChildProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Sesi pengguna berubah." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ChildProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeSessions(userId: String): Flow<List<LearningSession>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Sesi pengguna berubah." }
        val path = "users/$uid/sessions"
        emitAll(
            db.collection("users").document(uid).collection("sessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LearningSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRewards(userId: String): Flow<List<ParentReward>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Sesi pengguna berubah." }
        val path = "users/$uid/rewards"
        emitAll(
            db.collection("users").document(uid).collection("rewards")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ParentReward::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileInitialized(userId: String, defaultChildName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            val userDoc = db.collection("users").document(uid)
            val snapshot = userDoc.get().await()
            if (!snapshot.exists()) {
                val safeName = defaultChildName.trim().ifBlank { "Adik Pintar" }.take(50)
                val profilePayload = mapOf(
                    "userId" to uid,
                    "childName" to safeName,
                    "grade" to 2L,
                    "totalStars" to 30L,
                    "totalXp" to 0L,
                    "streakDays" to 1L,
                    "totalQuestionsSolved" to 0L,
                    "totalCorrectAnswers" to 0L,
                    "unlockedBadges" to emptyList<String>(),
                    "ownedAvatarItems" to listOf("owl_classic"),
                    "equippedAvatar" to "owl_classic",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.set(profilePayload).await()

                // Seed two motivating starter parent rewards so the reward system is immediately usable
                val rewardsRef = userDoc.collection("rewards")
                val starter1 = mapOf(
                    "userId" to uid,
                    "title" to "Es Krim Favorit",
                    "starCost" to 100L,
                    "iconKey" to "icecream",
                    "status" to RewardStatus.AVAILABLE.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val starter2 = mapOf(
                    "userId" to uid,
                    "title" to "30 Menit Main Sepeda / Taman",
                    "starCost" to 150L,
                    "iconKey" to "park",
                    "status" to RewardStatus.AVAILABLE.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                rewardsRef.document().set(starter1).await()
                rewardsRef.document().set(starter2).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChildSettings(userId: String, childName: String, grade: Int): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            val trimmedName = childName.trim().take(50)
            require(trimmedName.isNotEmpty()) { "Nama anak tidak boleh kosong." }
            val safeGrade = grade.coerceIn(1, 6).toLong()

            db.collection("users").document(uid).update(
                mapOf(
                    "childName" to trimmedName,
                    "grade" to safeGrade,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedSession(
        userId: String,
        currentProfile: ChildProfile,
        mode: ActivityMode,
        topic: MathTopic,
        grade: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        starsEarned: Int,
        durationSeconds: Int
    ): Result<List<String>> {
        val sessionPath = "users/$userId/sessions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            val safeTotal = totalQuestions.coerceIn(1, 100)
            val safeCorrect = correctAnswers.coerceIn(0, safeTotal)
            val safeStars = starsEarned.coerceIn(0, 500)
            val safeDuration = durationSeconds.coerceIn(0, 3600)
            val safeGrade = grade.coerceIn(1, 6)

            val sessionDoc = db.collection("users").document(uid).collection("sessions").document()
            val sessionPayload = mapOf(
                "userId" to uid,
                "mode" to mode.code,
                "topic" to topic.code,
                "grade" to safeGrade.toLong(),
                "totalQuestions" to safeTotal.toLong(),
                "correctAnswers" to safeCorrect.toLong(),
                "starsEarned" to safeStars.toLong(),
                "durationSeconds" to safeDuration.toLong(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            sessionDoc.set(sessionPayload).await()

            val newSolved = (currentProfile.totalQuestionsSolved + safeTotal.toLong()).coerceAtMost(1_000_000L)
            val newCorrect = (currentProfile.totalCorrectAnswers + safeCorrect.toLong()).coerceAtMost(newSolved)
            val newStars = (currentProfile.totalStars + safeStars.toLong()).coerceAtMost(1_000_000L)
            val xpEarned = (safeCorrect * 15L + 10L)
            val newXp = (currentProfile.totalXp + xpEarned).coerceAtMost(10_000_000L)
            val newStreak = currentProfile.streakDays.coerceAtLeast(1L)

            val updatedBadges = RewardCatalog.evaluateUnlockedBadges(
                currentBadges = currentProfile.unlockedBadges,
                totalSolved = newSolved,
                totalStars = newStars,
                sessionMode = mode,
                sessionCorrect = safeCorrect,
                sessionTotal = safeTotal
            )
            val newlyUnlocked = updatedBadges.filterNot { it in currentProfile.unlockedBadges }

            db.collection("users").document(uid).update(
                mapOf(
                    "totalStars" to newStars,
                    "totalXp" to newXp,
                    "streakDays" to newStreak,
                    "totalQuestionsSolved" to newSolved,
                    "totalCorrectAnswers" to newCorrect,
                    "unlockedBadges" to updatedBadges,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(newlyUnlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionPath)
            Result.failure(e)
        }
    }

    suspend fun purchaseOrEquipAvatar(
        userId: String,
        currentProfile: ChildProfile,
        item: AvatarShopItem
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }

            val alreadyOwned = item.id in currentProfile.ownedAvatarItems
            val updatedStars = if (alreadyOwned) {
                currentProfile.totalStars
            } else {
                require(currentProfile.totalStars >= item.starCost) {
                    "Bintang belum cukup untuk membuka ${item.title}."
                }
                currentProfile.totalStars - item.starCost
            }
            val updatedItems = if (alreadyOwned) {
                currentProfile.ownedAvatarItems
            } else {
                (currentProfile.ownedAvatarItems + item.id).distinct().take(30)
            }

            db.collection("users").document(uid).update(
                mapOf(
                    "totalStars" to updatedStars,
                    "ownedAvatarItems" to updatedItems,
                    "equippedAvatar" to item.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createParentReward(
        userId: String,
        title: String,
        starCost: Int,
        iconKey: String
    ): Result<Unit> {
        val path = "users/$userId/rewards"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            val trimmedTitle = title.trim().take(60)
            require(trimmedTitle.isNotEmpty()) { "Nama hadiah tidak boleh kosong." }
            val safeCost = starCost.coerceIn(10, 10000).toLong()
            val safeIcon = iconKey.trim().ifBlank { "gift" }.take(30)

            val docRef = db.collection("users").document(uid).collection("rewards").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to trimmedTitle,
                "starCost" to safeCost,
                "iconKey" to safeIcon,
                "status" to RewardStatus.AVAILABLE.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun redeemParentReward(
        userId: String,
        currentProfile: ChildProfile,
        reward: ParentReward
    ): Result<Unit> {
        val path = "users/$userId/rewards/${reward.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            require(reward.id.isNotBlank()) { "ID hadiah tidak valid." }
            require(reward.rewardStatus == RewardStatus.AVAILABLE) { "Hadiah ini sudah ditukar." }
            require(currentProfile.totalStars >= reward.starCost) {
                "Bintang belum cukup! Butuh ${reward.starCost} bintang."
            }

            val remainingStars = (currentProfile.totalStars - reward.starCost).coerceAtLeast(0L)
            val batch = db.batch()
            val profileRef = db.collection("users").document(uid)
            val rewardRef = profileRef.collection("rewards").document(reward.id)

            batch.update(
                profileRef,
                mapOf(
                    "totalStars" to remainingStars,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                rewardRef,
                mapOf(
                    "status" to RewardStatus.REDEEMED.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun fulfillParentReward(userId: String, rewardId: String): Result<Unit> {
        val path = "users/$userId/rewards/$rewardId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            require(rewardId.isNotBlank()) { "ID hadiah tidak valid." }

            db.collection("users").document(uid).collection("rewards").document(rewardId)
                .update(
                    mapOf(
                        "status" to RewardStatus.FULFILLED.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteParentReward(userId: String, rewardId: String): Result<Unit> {
        val path = "users/$userId/rewards/$rewardId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah." }
            require(rewardId.isNotBlank()) { "ID hadiah tidak valid." }

            db.collection("users").document(uid).collection("rewards").document(rewardId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
