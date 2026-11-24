package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.PieChart
import com.example.R
import com.example.data.ActivityMode
import com.example.data.AvatarShopItem
import com.example.data.ChildProfile
import com.example.data.MathProblemGenerator
import com.example.data.MathTopic
import com.example.data.ParentReward
import com.example.data.RewardCatalog
import com.example.data.RewardStatus
import com.example.data.curriculumPhaseForGrade
import com.example.ui.components.AsyncDrawableImage
import com.example.ui.theme.PintarTheme

@Composable
fun HomeScreen(
    profile: ChildProfile,
    rewards: List<ParentReward>,
    onSelectGrade: (Int) -> Unit,
    onStartWorksheet: (MathTopic) -> Unit,
    onOpenLearningMode: (ActivityMode) -> Unit,
    onOpenGames: () -> Unit,
    onPurchaseOrEquipAvatar: (AvatarShopItem) -> Unit,
    onRedeemParentReward: (ParentReward) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    var rewardShopTab by remember { mutableIntStateOf(0) } // 0 = Koleksi, 1 = Hadiah Orang Tua
    val availableTopics = remember(profile.gradeInt) {
        MathProblemGenerator.topicsForGrade(profile.gradeInt)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Greeting + Mascot + Stars & Streak Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.5.dp, extended.sunnyPill),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(56.dp)
                        ) {
                            AsyncDrawableImage(
                                resId = R.drawable.img_mascot_buddy_1795489344526,
                                contentDescription = "Avatar Anak",
                                targetWidthPx = 200,
                                targetHeightPx = 200,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconForRewardKey(profile.equippedAvatar),
                                    contentDescription = "Lencana Aktif",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Halo, ${profile.childName}!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Level ${profile.level} • Kelas ${profile.gradeInt} SD",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadgePill(
                        icon = Icons.Rounded.LocalFireDepartment,
                        iconTint = extended.coral,
                        bgColor = extended.coralContainer,
                        text = "${profile.streakDays}"
                    )
                    StatBadgePill(
                        icon = Icons.Rounded.Star,
                        iconTint = extended.starGold,
                        bgColor = extended.starContainer,
                        text = "${profile.totalStars}"
                    )
                }
            }
        }

        // 2. Hero Adventure Banner with CTA
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, extended.cardBorder),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncDrawableImage(
                        resId = R.drawable.img_hero_adventure_1795489344190,
                        contentDescription = "Ilustrasi Petualangan Matematika",
                        targetWidthPx = 800,
                        targetHeightPx = 400,
                        contentScale = ContentScale.Crop,
                        placeholderColor = Color(0xFF132247),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE0132247),
                                        Color(0x99132247),
                                        Color(0x22132247)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = extended.sunnyPill
                        ) {
                            Text(
                                text = "Kurikulum Merdeka • ${curriculumPhaseForGrade(profile.gradeInt)} (Kls ${profile.gradeInt})",
                                style = MaterialTheme.typography.labelMedium,
                                color = extended.sunnyPillContent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Siap Hitung Bersusun?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = MathProblemGenerator.gradeCurriculumSummary(profile.gradeInt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onStartWorksheet(MathTopic.PENGURANGAN) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = extended.sunnyPill,
                                    contentColor = extended.sunnyPillContent
                                ),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("home_start_worksheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mulai Belajar", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = onOpenGames,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("home_open_games_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Main Game", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // 3. Level Progress + Grade 1-6 Switcher
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, extended.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pilih Tingkat Kelas SD",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${profile.xpInCurrentLevel}/${profile.xpTargetForNextLevel} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            (profile.xpInCurrentLevel.toFloat() / profile.xpTargetForNextLevel.toFloat())
                                .coerceIn(0f, 1f)
                        },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items((1..6).toList()) { grade ->
                            val selected = profile.gradeInt == grade
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectGrade(grade) },
                                label = {
                                    Text(
                                        text = "Kelas $grade",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = extended.sunnyPill,
                                    selectedLabelColor = extended.sunnyPillContent
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = extended.cardBorder,
                                    selectedBorderColor = extended.starGold
                                ),
                                modifier = Modifier.testTag("home_grade_chip_$grade")
                            )
                        }
                    }
                }
            }
        }

        // 4. Kurikulum Merdeka Modes Strip (Fase A / B / C)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mode Soal Kurikulum Merdeka",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = curriculumPhaseForGrade(profile.gradeInt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ActivityMode.learningModes) { modeItem ->
                        CurriculumModeQuickCard(
                            mode = modeItem,
                            grade = profile.gradeInt,
                            onClick = { onOpenLearningMode(modeItem) }
                        )
                    }
                }
            }
        }

        // 4b. Quick Arithmetic Topics for Hitung Bersusun
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Latihan Cepat Hitung Bersusun",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(availableTopics) { topic ->
                        TopicQuickCard(
                            topic = topic,
                            onClick = { onStartWorksheet(topic) }
                        )
                    }
                }
            }
        }

        // 5. Achievement Badges Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lencana Prestasi",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${profile.unlockedBadges.size}/${RewardCatalog.badges.size} Terbuka",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(RewardCatalog.badges) { badge ->
                        val unlocked = badge.id in profile.unlockedBadges
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (unlocked) extended.starContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.5.dp,
                                if (unlocked) extended.starGold else extended.cardBorder
                            ),
                            modifier = Modifier.width(134.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (unlocked) extended.sunnyPill
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) iconForRewardKey(badge.iconKey) else Icons.Rounded.Lock,
                                        contentDescription = badge.title,
                                        tint = if (unlocked) extended.sunnyPillContent
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Star Reward Shop (Collectibles + Parent Real-World Rewards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Toko Hadiah Bintang",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    StatBadgePill(
                        icon = Icons.Rounded.Star,
                        iconTint = extended.starGold,
                        bgColor = extended.starContainer,
                        text = "${profile.totalStars} ★"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShopTabButton(
                        title = "Hadiah Orang Tua",
                        selected = rewardShopTab == 0,
                        onClick = { rewardShopTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_tab_parent_rewards")
                    )
                    ShopTabButton(
                        title = "Koleksi Lencana",
                        selected = rewardShopTab == 1,
                        onClick = { rewardShopTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_tab_avatar_items")
                    )
                }
            }
        }

        if (rewardShopTab == 0) {
            if (rewards.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, extended.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CardGiftcard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Belum ada hadiah dari Orang Tua",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Orang tua dapat menambahkan hadiah nyata di tab Orang Tua.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rewards, key = { it.id }) { reward ->
                    ParentRewardRedeemCard(
                        reward = reward,
                        currentStars = profile.totalStars,
                        onRedeem = { onRedeemParentReward(reward) }
                    )
                }
            }
        } else {
            items(RewardCatalog.avatarItems, key = { it.id }) { item ->
                val owned = item.id in profile.ownedAvatarItems
                val equipped = profile.equippedAvatar == item.id
                AvatarRewardShopCard(
                    item = item,
                    owned = owned,
                    equipped = equipped,
                    canAfford = profile.totalStars >= item.starCost,
                    onClick = { onPurchaseOrEquipAvatar(item) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatBadgePill(
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TopicQuickCard(
    topic: MathTopic,
    onClick: () -> Unit
) {
    val extended = PintarTheme.extended
    val (bg, accent) = when (topic) {
        MathTopic.PENJUMLAHAN -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        MathTopic.PENGURANGAN -> extended.coralContainer to extended.coral
        MathTopic.PERKALIAN -> extended.starContainer to extended.starGold
        MathTopic.PEMBAGIAN -> extended.successContainer to extended.success
        MathTopic.CAMPURAN -> extended.berryContainer to extended.berry
        MathTopic.SOAL_CERITA -> extended.coralContainer to extended.coral
        MathTopic.PECAHAN -> extended.successContainer to extended.success
        MathTopic.GEOMETRI -> extended.starContainer to extended.starGold
        MathTopic.POLA_ALJABAR -> extended.berryContainer to extended.berry
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, extended.cardBorder),
        modifier = Modifier
            .width(114.dp)
            .testTag("home_topic_${topic.code.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = topic.symbol,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent
                )
            }
            Text(
                text = topic.shortLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShopTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) extended.sunnyPill else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (selected) extended.starGold else extended.cardBorder),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) extended.sunnyPillContent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParentRewardRedeemCard(
    reward: ParentReward,
    currentStars: Long,
    onRedeem: () -> Unit
) {
    val extended = PintarTheme.extended
    val status = reward.rewardStatus
    val canRedeem = status == RewardStatus.AVAILABLE && currentStars >= reward.starCost

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(extended.starContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForRewardKey(reward.iconKey),
                        contentDescription = null,
                        tint = extended.starGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${reward.starCost} Bintang • ${status.labelId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (status) {
                RewardStatus.AVAILABLE -> {
                    Button(
                        onClick = onRedeem,
                        enabled = canRedeem,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("redeem_reward_${reward.id}")
                    ) {
                        Text(
                            text = if (canRedeem) "Tukar" else "Kurang ★",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                RewardStatus.REDEEMED -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Menunggu",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                RewardStatus.FULFILLED -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Sudah diberikan",
                        tint = extended.success,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarRewardShopCard(
    item: AvatarShopItem,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    val extended = PintarTheme.extended
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (equipped) extended.starContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (equipped) extended.starGold else extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForRewardKey(item.iconKey),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClick,
                enabled = owned || canAfford,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        equipped -> extended.success
                        owned -> MaterialTheme.colorScheme.primary
                        else -> extended.starGold
                    },
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("avatar_item_${item.id}")
            ) {
                Text(
                    text = when {
                        equipped -> "Dipakai"
                        owned -> "Pakai"
                        else -> "${item.starCost} ★"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun iconForRewardKey(key: String): ImageVector = when (key) {
    "rocket" -> Icons.Rounded.RocketLaunch
    "crown" -> Icons.Rounded.WorkspacePremium
    "pets" -> Icons.Rounded.Pets
    "trophy", "emoji_events" -> Icons.Rounded.EmojiEvents
    "diamond" -> Icons.Rounded.Diamond
    "icecream" -> Icons.Rounded.Icecream
    "park" -> Icons.Rounded.Park
    "flag" -> Icons.Rounded.Flag
    "calculate" -> Icons.Rounded.Calculate
    "verified" -> Icons.Rounded.Verified
    "star" -> Icons.Rounded.Star
    "sports_esports" -> Icons.Rounded.SportsEsports
    "school" -> Icons.Rounded.School
    "owl" -> Icons.Rounded.AutoAwesome
    else -> Icons.Rounded.CardGiftcard
}

@Composable
private fun CurriculumModeQuickCard(
    mode: ActivityMode,
    grade: Int,
    onClick: () -> Unit
) {
    val extended = PintarTheme.extended
    val (accent, container, icon, subtitle) = when (mode) {
        ActivityMode.WORKSHEET -> Quad(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Rounded.Calculate,
            "Simpan & pinjam kelas $grade"
        )
        ActivityMode.STORY_PROBLEM -> Quad(
            extended.coral,
            extended.coralContainer,
            Icons.Rounded.AutoStories,
            "Literasi numerasi & AKM"
        )
        ActivityMode.FRACTION_COMPARE -> Quad(
            extended.success,
            extended.successContainer,
            Icons.Rounded.PieChart,
            if (grade <= 2) "Bandingkan < = >" else if (grade <= 4) "Pecahan visual & senilai" else "Pecahan, desimal & %"
        )
        ActivityMode.GEOMETRY_MEASURE -> Quad(
            extended.starGold,
            extended.starContainer,
            Icons.Rounded.Category,
            if (grade <= 2) "Jam, sisi bangun & cm" else if (grade <= 4) "Keliling, luas & waktu" else "Volume & diagram data"
        )
        else -> Quad(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
            Icons.Rounded.Extension,
            if (grade <= 2) "Pola loncat & kotak □" else "Deret bilangan & aljabar"
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, extended.cardBorder),
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(156.dp)
            .testTag("home_mode_${mode.code.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode.labelId,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = mode.labelId,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
