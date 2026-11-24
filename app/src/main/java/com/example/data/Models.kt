package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MathTopic(
    val code: String,
    val labelId: String,
    val shortLabel: String,
    val symbol: String
) {
    PENJUMLAHAN("PENJUMLAHAN", "Penjumlahan", "Tambah", "+"),
    PENGURANGAN("PENGURANGAN", "Pengurangan", "Kurang", "−"),
    PERKALIAN("PERKALIAN", "Perkalian", "Kali", "×"),
    PEMBAGIAN("PEMBAGIAN", "Pembagian", "Bagi", "÷"),
    CAMPURAN("CAMPURAN", "Campuran", "Campur", "±");

    companion object {
        fun fromCode(raw: String): MathTopic =
            entries.firstOrNull { it.code == raw } ?: PENJUMLAHAN
    }
}

enum class ActivityMode(val code: String, val labelId: String) {
    WORKSHEET("WORKSHEET", "Hitung Bersusun"),
    BALLOON_GAME("BALLOON_GAME", "Tembak Balon"),
    SPEED_SPRINT("SPEED_SPRINT", "Tantangan Kilat");

    companion object {
        fun fromCode(raw: String): ActivityMode =
            entries.firstOrNull { it.code == raw } ?: WORKSHEET
    }
}

enum class RewardStatus(val code: String, val labelId: String) {
    AVAILABLE("AVAILABLE", "Tersedia"),
    REDEEMED("REDEEMED", "Menunggu Orang Tua"),
    FULFILLED("FULFILLED", "Sudah Diberikan");

    companion object {
        fun fromCode(raw: String): RewardStatus =
            entries.firstOrNull { it.code == raw } ?: AVAILABLE
    }
}

@Keep
@IgnoreExtraProperties
data class ChildProfile(
    val userId: String = "",
    val childName: String = "Adik Pintar",
    val grade: Long = 2L,
    val totalStars: Long = 0L,
    val totalXp: Long = 0L,
    val streakDays: Long = 1L,
    val totalQuestionsSolved: Long = 0L,
    val totalCorrectAnswers: Long = 0L,
    val unlockedBadges: List<String> = emptyList(),
    val ownedAvatarItems: List<String> = listOf("owl_classic"),
    val equippedAvatar: String = "owl_classic",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gradeInt: Int get() = grade.toInt().coerceIn(1, 6)
    val level: Int get() = (totalXp / 150L).toInt() + 1
    val xpInCurrentLevel: Int get() = (totalXp % 150L).toInt()
    val xpTargetForNextLevel: Int get() = 150
    val accuracyPercent: Int
        get() = if (totalQuestionsSolved <= 0L) 0
        else ((totalCorrectAnswers * 100L) / totalQuestionsSolved).toInt().coerceIn(0, 100)
}

@Keep
@IgnoreExtraProperties
data class LearningSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = ActivityMode.WORKSHEET.code,
    val topic: String = MathTopic.PENJUMLAHAN.code,
    val grade: Long = 1L,
    val totalQuestions: Long = 0L,
    val correctAnswers: Long = 0L,
    val starsEarned: Long = 0L,
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val activityMode: ActivityMode get() = ActivityMode.fromCode(mode)
    val mathTopic: MathTopic get() = MathTopic.fromCode(topic)
    val accuracyPercent: Int
        get() = if (totalQuestions <= 0L) 0
        else ((correctAnswers * 100L) / totalQuestions).toInt().coerceIn(0, 100)
}

@Keep
@IgnoreExtraProperties
data class ParentReward(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val starCost: Long = 50L,
    val iconKey: String = "gift",
    val status: String = RewardStatus.AVAILABLE.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val rewardStatus: RewardStatus get() = RewardStatus.fromCode(status)
}

data class AvatarShopItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val starCost: Long,
    val iconKey: String
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String
)

object RewardCatalog {
    val avatarItems = listOf(
        AvatarShopItem("owl_classic", "Burung Hantu Cerdas", "Maskot klasik PintarHitung", 0L, "owl"),
        AvatarShopItem("rocket_star", "Roket Angkasa", "Meluncur cepat selesaikan soal", 60L, "rocket"),
        AvatarShopItem("crown_gold", "Mahkota Juara", "Tanda jagoan berhitung", 120L, "crown"),
        AvatarShopItem("dino_math", "Dino Penjelajah", "Teman seru kelas 1–6", 150L, "pets"),
        AvatarShopItem("trophy_hero", "Piala Emas SD", "Koleksi kebanggaan bintang", 220L, "trophy"),
        AvatarShopItem("rainbow_gem", "Permata Pelangi", "Koleksi langka penuh warna", 300L, "diamond")
    )

    val badges = listOf(
        AchievementBadge("first_step", "Langkah Pertama", "Selesaikan latihan pertama", "flag"),
        AchievementBadge("stack_master", "Jagoan Bersusun", "Selesaikan 15 soal latihan", "calculate"),
        AchievementBadge("accuracy_ace", "Teliti 90%+", "Raih nilai sempurna di satu sesi", "verified"),
        AchievementBadge("star_collector", "Kolektor 200 Bintang", "Kumpulkan 200 bintang reward", "star"),
        AchievementBadge("game_champion", "Juara Game", "Mainkan game edukasi matematika", "sports_esports"),
        AchievementBadge("math_legend", "Legenda SD", "Selesaikan 50 soal matematika", "emoji_events")
    )

    fun evaluateUnlockedBadges(
        currentBadges: List<String>,
        totalSolved: Long,
        totalStars: Long,
        sessionMode: ActivityMode,
        sessionCorrect: Int,
        sessionTotal: Int
    ): List<String> {
        val result = currentBadges.toMutableSet()
        if (totalSolved >= 1L) result.add("first_step")
        if (totalSolved >= 15L) result.add("stack_master")
        if (sessionTotal > 0 && sessionCorrect == sessionTotal) result.add("accuracy_ace")
        if (totalStars >= 200L) result.add("star_collector")
        if (sessionMode == ActivityMode.BALLOON_GAME || sessionMode == ActivityMode.SPEED_SPRINT) {
            result.add("game_champion")
        }
        if (totalSolved >= 50L) result.add("math_legend")
        return result.take(20)
    }
}
