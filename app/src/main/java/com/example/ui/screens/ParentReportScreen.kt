package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChildProfile
import com.example.data.LearningSession
import com.example.data.MathTopic
import com.example.data.ParentReward
import com.example.data.RewardStatus
import com.example.data.curriculumPhaseForGrade
import com.example.ui.theme.PintarTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ParentReportScreen(
    profile: ChildProfile,
    sessions: List<LearningSession>,
    rewards: List<ParentReward>,
    onUpdateProfile: (childName: String, grade: Int) -> Unit,
    onCreateReward: (title: String, starCost: Int, iconKey: String) -> Unit,
    onFulfillReward: (ParentReward) -> Unit,
    onDeleteReward: (ParentReward) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddRewardDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "live_sync_pulse")
    val liveAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live_alpha"
    )

    // Compute per-topic mastery statistics from real-time sessions
    val topicStats = remember(sessions) {
        MathTopic.entries.map { topic ->
            val topicSessions = sessions.filter { it.mathTopic == topic }
            val totalQ = topicSessions.sumOf { it.totalQuestions }
            val correctQ = topicSessions.sumOf { it.correctAnswers }
            val accuracy = if (totalQ > 0L) ((correctQ * 100L) / totalQ).toInt() else -1
            Triple(topic, accuracy, totalQ.toInt())
        }
    }

    val strongestTopic = remember(topicStats) {
        topicStats.filter { it.second >= 0 }.maxByOrNull { it.second }
    }
    val needsPracticeTopic = remember(topicStats) {
        topicStats.filter { it.second >= 0 }.minByOrNull { it.second }
    }

    // 7-day activity counts
    val dailyActivity = remember(sessions) {
        val dayFmt = SimpleDateFormat("EEE", Locale("id", "ID"))
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val oneDayMs = 24L * 60L * 60L * 1000L

        (6 downTo 0).map { daysAgo ->
            val start = todayStart - daysAgo * oneDayMs
            val end = start + oneDayMs
            val count = sessions.filter { s ->
                val ms = (s.createdAt ?: Timestamp.now()).toDate().time
                ms in start until end
            }.sumOf { it.totalQuestions.toInt() }
            dayFmt.format(Date(start)) to count
        }
    }

    val dateTimeFmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Real-Time Sync Indicator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Laporan Orang Tua",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${profile.childName} • Kelas ${profile.gradeInt} SD (${curriculumPhaseForGrade(profile.gradeInt)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = extended.successContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(extended.success.copy(alpha = liveAlpha))
                        )
                        Text(
                            text = stringResource(R.string.parent_live_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = extended.success
                        )
                    }
                }
            }
        }

        // 2. Real-Time KPI Summary Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ParentKpiCard(
                        label = "Total Soal",
                        value = "${profile.totalQuestionsSolved}",
                        subtitle = "${sessions.size} sesi selesai",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ParentKpiCard(
                        label = "Akurasi Belajar",
                        value = "${profile.accuracyPercent}%",
                        subtitle = "${profile.totalCorrectAnswers} jawaban tepat",
                        accentColor = extended.success,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ParentKpiCard(
                        label = "Saldo Bintang",
                        value = "${profile.totalStars} ★",
                        subtitle = "Level ${profile.level} (${profile.totalXp} XP)",
                        accentColor = extended.starGold,
                        modifier = Modifier.weight(1f)
                    )
                    ParentKpiCard(
                        label = "Streak Aktif",
                        value = "${profile.streakDays} Hari",
                        subtitle = "${profile.unlockedBadges.size} lencana terbuka",
                        accentColor = extended.coral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Learning Insights & Topic Mastery Chart
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Penguasaan per Topik",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (strongestTopic != null && needsPracticeTopic != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Kelebihan: ${strongestTopic.first.labelId} (${strongestTopic.second}%)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = extended.success
                                )
                                Text(
                                    text = if (needsPracticeTopic.first == strongestTopic.first) {
                                        "Rekomendasi: Coba topik variasi lain agar semua keterampilan seimbang."
                                    } else {
                                        "Perlu Latihan: ${needsPracticeTopic.first.labelId} (${needsPracticeTopic.second}%)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    topicStats.forEach { (topic, accuracy, solvedCount) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${topic.symbol} ${topic.labelId}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (accuracy >= 0) "$accuracy% ($solvedCount soal)" else "Belum dicoba",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (accuracy >= 80) extended.success
                                    else if (accuracy in 0..79) extended.starGold
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { if (accuracy >= 0) accuracy / 100f else 0f },
                                color = when {
                                    accuracy >= 80 -> extended.success
                                    accuracy >= 50 -> extended.starGold
                                    accuracy >= 0 -> extended.coral
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        // 4. 7-Day Activity Bar Chart
        item {
            val maxDaily = (dailyActivity.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
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
                    Text(
                        text = "Aktivitas 7 Hari Terakhir",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyActivity.forEach { (dayLabel, count) ->
                            val fraction = (count.toFloat() / maxDaily.toFloat()).coerceIn(0.08f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (count > 0) "$count" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height((82.dp * fraction).coerceAtLeast(8.dp))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (count > 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Manage Parent Rewards Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kelola Hadiah Anak",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(
                    onClick = { showAddRewardDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("parent_add_reward_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (rewards.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = extended.starGold
                        )
                        Text(
                            text = "Tambahkan hadiah nyata agar anak makin semangat mengumpulkan bintang!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(rewards, key = { it.id }) { reward ->
                val status = reward.rewardStatus
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.5.dp,
                        if (status == RewardStatus.REDEEMED) MaterialTheme.colorScheme.primary else extended.cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reward.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${reward.starCost} Bintang • ${status.labelId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (status == RewardStatus.REDEEMED) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (status == RewardStatus.REDEEMED) {
                                Button(
                                    onClick = { onFulfillReward(reward) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = extended.success),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("fulfill_reward_${reward.id}")
                                ) {
                                    Text("Berikan ✓", style = MaterialTheme.typography.labelMedium)
                                }
                            } else if (status == RewardStatus.FULFILLED) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Sudah diberikan",
                                    tint = extended.success,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteReward(reward) },
                                modifier = Modifier.testTag("delete_reward_${reward.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Hapus hadiah",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Real-Time Learning Session Feed
        item {
            Text(
                text = "Riwayat Belajar Real-Time",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (sessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Belum ada sesi belajar tercatat",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Setiap latihan hitung bersusun atau game yang selesai akan otomatis tampil di sini secara real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sessions.take(15), key = { it.id }) { session ->
                val tsDate = (session.createdAt ?: Timestamp.now()).toDate()
                Surface(
                    shape = RoundedCornerShape(18.dp),
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${session.activityMode.labelId} • ${session.mathTopic.labelId}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Kelas ${session.grade} • ${session.correctAnswers}/${session.totalQuestions} benar (${session.accuracyPercent}%) • ${session.durationSeconds}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateTimeFmt.format(tsDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = extended.starContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = extended.starGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "+${session.starsEarned}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = extended.starGold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Profile Settings & Sign Out Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditProfileDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("parent_edit_profile_button")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ubah Profil")
                }
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = extended.coral),
                    border = BorderStroke(1.dp, extended.coral.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("parent_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.sign_out_label))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(profile.childName) }
        var gradeInput by remember { mutableIntStateOf(profile.gradeInt) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Ubah Profil Anak") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 50) nameInput = it },
                        label = { Text("Nama Panggilan Anak") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_child_name_input")
                    )
                    Text("Tingkat Kelas SD:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((1..6).toList()) { g ->
                            FilterChip(
                                selected = gradeInput == g,
                                onClick = { gradeInput = g },
                                label = { Text("Kls $g") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.trim().isNotEmpty()) {
                            onUpdateProfile(nameInput.trim(), gradeInput)
                            showEditProfileDialog = false
                        }
                    },
                    enabled = nameInput.trim().isNotEmpty(),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showAddRewardDialog) {
        var rewardTitle by remember { mutableStateOf("") }
        var costText by remember { mutableStateOf("100") }
        var selectedIcon by remember { mutableStateOf("gift") }
        val iconChoices = listOf(
            "gift" to "Kado",
            "icecream" to "Es Krim",
            "park" to "Taman",
            "trophy" to "Piala"
        )

        AlertDialog(
            onDismissRequest = { showAddRewardDialog = false },
            title = { Text("Tambah Hadiah Bintang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = rewardTitle,
                        onValueChange = { if (it.length <= 60) rewardTitle = it },
                        label = { Text("Nama Hadiah (mis. Nonton Film)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_reward_title_input")
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) costText = input
                        },
                        label = { Text("Harga Bintang (10–10000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_reward_cost_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(iconChoices) { (key, label) ->
                            FilterChip(
                                selected = selectedIcon == key,
                                onClick = { selectedIcon = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val parsedCost = costText.toIntOrNull() ?: 0
                Button(
                    onClick = {
                        onCreateReward(rewardTitle.trim(), parsedCost.coerceIn(10, 10000), selectedIcon)
                        showAddRewardDialog = false
                    },
                    enabled = rewardTitle.trim().isNotEmpty() && parsedCost in 10..10000,
                    modifier = Modifier.testTag("confirm_add_reward_button")
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRewardDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ParentKpiCard(
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, extended.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
