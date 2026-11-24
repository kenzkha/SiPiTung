package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityMode
import com.example.data.ChildProfile
import com.example.data.GameQuestion
import com.example.data.MathProblemGenerator
import com.example.data.MathTopic
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.PintarTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class ActiveMiniGame {
    HUB,
    BALLOON_POP,
    SPEED_SPRINT
}

@Composable
fun GamesScreen(
    profile: ChildProfile,
    onSelectGrade: (Int) -> Unit,
    onCompleteGame: (
        mode: ActivityMode,
        topic: MathTopic,
        grade: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        starsEarned: Int,
        durationSeconds: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeGame by remember { mutableStateOf(ActiveMiniGame.HUB) }
    var selectedTopic by remember { mutableStateOf(MathTopic.CAMPURAN) }

    BackHandler(enabled = activeGame != ActiveMiniGame.HUB) {
        activeGame = ActiveMiniGame.HUB
    }

    when (activeGame) {
        ActiveMiniGame.HUB -> GameHubContent(
            profile = profile,
            selectedTopic = selectedTopic,
            onSelectTopic = { selectedTopic = it },
            onSelectGrade = onSelectGrade,
            onLaunchBalloonGame = { activeGame = ActiveMiniGame.BALLOON_POP },
            onLaunchSpeedSprint = { activeGame = ActiveMiniGame.SPEED_SPRINT },
            modifier = modifier
        )

        ActiveMiniGame.BALLOON_POP -> BalloonPopGame(
            grade = profile.gradeInt,
            topic = selectedTopic,
            onExit = { activeGame = ActiveMiniGame.HUB },
            onFinish = { total, correct, stars, duration ->
                onCompleteGame(
                    ActivityMode.BALLOON_GAME,
                    selectedTopic,
                    profile.gradeInt,
                    total,
                    correct,
                    stars,
                    duration
                )
            },
            modifier = modifier
        )

        ActiveMiniGame.SPEED_SPRINT -> SpeedSprintGame(
            grade = profile.gradeInt,
            topic = selectedTopic,
            onExit = { activeGame = ActiveMiniGame.HUB },
            onFinish = { total, correct, stars, duration ->
                onCompleteGame(
                    ActivityMode.SPEED_SPRINT,
                    selectedTopic,
                    profile.gradeInt,
                    total,
                    correct,
                    stars,
                    duration
                )
            },
            modifier = modifier
        )
    }
}

@Composable
private fun GameHubContent(
    profile: ChildProfile,
    selectedTopic: MathTopic,
    onSelectTopic: (MathTopic) -> Unit,
    onSelectGrade: (Int) -> Unit,
    onLaunchBalloonGame: () -> Unit,
    onLaunchSpeedSprint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    val availableTopics = remember(profile.gradeInt) {
        MathProblemGenerator.topicsForGrade(profile.gradeInt)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Arena Game Matematika",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Main seru sambil kumpulkan bintang untuk Kelas ${profile.gradeInt} SD!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Grade & Topic Filter Bar
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, extended.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Atur Tingkat & Topik Game",
                        style = MaterialTheme.typography.titleSmall
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((1..6).toList()) { grade ->
                            FilterChip(
                                selected = profile.gradeInt == grade,
                                onClick = { onSelectGrade(grade) },
                                label = { Text("Kelas $grade", style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = extended.sunnyPill,
                                    selectedLabelColor = extended.sunnyPillContent
                                )
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableTopics) { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { onSelectTopic(topic) },
                                label = {
                                    Text(
                                        "${topic.symbol} ${topic.shortLabel}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Game 1: Tembak Balon Angka
        item {
            GameModeCard(
                title = "Tembak Balon Angka",
                subtitle = "Pecahkan balon dengan jawaban benar! 8 ronde seru, 3 nyawa hati, dan bonus kombo bintang.",
                badgeText = "Hingga +100 ★",
                icon = Icons.Rounded.BubbleChart,
                gradientColors = listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
                buttonLabel = "Main Tembak Balon",
                testTag = "play_balloon_game_button",
                onPlay = onLaunchBalloonGame
            )
        }

        // Game 2: Tantangan Kilat 45 Detik
        item {
            GameModeCard(
                title = "Tantangan Kilat 45 Detik",
                subtitle = "Jawab sebanyak mungkin sebelum waktu habis! Pertahankan streak api untuk skor x3.",
                badgeText = "Mode Kecepatan",
                icon = Icons.Rounded.Bolt,
                gradientColors = listOf(Color(0xFFFF5C58), Color(0xFFF59E0B)),
                buttonLabel = "Mulai Tantangan Kilat",
                testTag = "play_speed_sprint_button",
                onPlay = onLaunchSpeedSprint
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    buttonLabel: String,
    testTag: String,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.24f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = gradientColors.first()
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(testTag)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BalloonPopGame(
    grade: Int,
    topic: MathTopic,
    onExit: () -> Unit,
    onFinish: (total: Int, correct: Int, stars: Int, duration: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    val totalRounds = 8
    var round by remember { mutableIntStateOf(1) }
    var hearts by remember { mutableIntStateOf(3) }
    var correctCount by remember { mutableIntStateOf(0) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableIntStateOf(0) }
    val startMs = remember { mutableLongStateOf(System.currentTimeMillis()) }

    var question by remember(grade, topic, round) {
        mutableStateOf(MathProblemGenerator.generateGameQuestion(round, grade, topic))
    }

    fun completeGame() {
        if (finished) return
        finished = true
        val safeTotal = answeredCount.coerceAtLeast(1)
        val stars = (correctCount * 12 + combo * 5).coerceIn(10, 120)
        val duration = ((System.currentTimeMillis() - startMs.longValue) / 1000L).toInt().coerceIn(3, 3600)
        earnedStars = stars
        onFinish(safeTotal, correctCount, stars, duration)
    }

    fun pickOption(option: Int) {
        if (finished) return
        answeredCount++
        if (option == question.correctAnswer) {
            correctCount++
            combo++
            feedbackBanner = if (combo >= 3) "Kombo x$combo! Mantap!" else "Tepat! Balon pecah!"
        } else {
            combo = 0
            hearts = (hearts - 1).coerceAtLeast(0)
            feedbackBanner = "Ups! Jawaban tepat: ${question.correctAnswer}"
        }

        if (hearts <= 0 || round >= totalRounds) {
            completeGame()
        } else {
            round++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "balloon_bob")
    val bob1 by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob1"
    )
    val bob2 by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(1750, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob2"
    )

    val balloonColors = listOf(
        Color(0xFFEF4444),
        Color(0xFF2563EB),
        Color(0xFF16A34A),
        Color(0xFF9333EA)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.testTag("balloon_game_back")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Ronde $round/$totalRounds",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { idx ->
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Nyawa",
                        tint = if (idx < hearts) extended.coral else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (finished) {
            GameResultCard(
                title = if (hearts > 0) "Juara Balon Angka!" else "Bagus! Coba Lagi Yuk!",
                correctCount = correctCount,
                totalCount = answeredCount.coerceAtLeast(1),
                earnedStars = earnedStars,
                onPlayAgain = {
                    round = 1
                    hearts = 3
                    correctCount = 0
                    answeredCount = 0
                    combo = 0
                    feedbackBanner = null
                    finished = false
                    startMs.longValue = System.currentTimeMillis()
                    question = MathProblemGenerator.generateGameQuestion(1, grade, topic)
                },
                onBackToHub = onExit
            )
        } else {
            // Question Prompt Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 4.dp,
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
                        text = "Pecahkan balon jawaban dari:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = question.prompt,
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (feedbackBanner != null) {
                        Text(
                            text = feedbackBanner ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2x2 Floating Balloons Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val rows = question.options.chunked(2)
                rows.forEachIndexed { rowIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEachIndexed { colIdx, optionVal ->
                            val flatIdx = rowIdx * 2 + colIdx
                            val bobOffset = if (flatIdx % 2 == 0) bob1 else bob2
                            val color = balloonColors[flatIdx % balloonColors.size]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset { IntOffset(0, bobOffset.roundToInt()) }
                            ) {
                                Surface(
                                    onClick = { pickOption(optionVal) },
                                    shape = CircleShape,
                                    color = color,
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(3.dp, Color.White.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .size(116.dp)
                                        .testTag("balloon_option_$flatIdx")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = optionVal.toString(),
                                            fontFamily = FredokaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 32.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(26.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedSprintGame(
    grade: Int,
    topic: MathTopic,
    onExit: () -> Unit,
    onFinish: (total: Int, correct: Int, stars: Int, duration: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    val totalSeconds = 45
    var timeLeft by remember { mutableIntStateOf(totalSeconds) }
    var questionCounter by remember { mutableIntStateOf(1) }
    var correctCount by remember { mutableIntStateOf(0) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableIntStateOf(0) }

    var question by remember(grade, topic, questionCounter) {
        mutableStateOf(MathProblemGenerator.generateGameQuestion(questionCounter, grade, topic))
    }

    LaunchedEffect(finished) {
        if (!finished) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            if (!finished) {
                finished = true
                val safeTotal = answeredCount.coerceAtLeast(1)
                val stars = (correctCount * 10 + (score / 25)).coerceIn(10, 150)
                earnedStars = stars
                onFinish(safeTotal, correctCount, stars, totalSeconds)
            }
        }
    }

    fun answerOption(option: Int) {
        if (finished) return
        answeredCount++
        if (option == question.correctAnswer) {
            correctCount++
            streak++
            val mult = when {
                streak >= 6 -> 3
                streak >= 3 -> 2
                else -> 1
            }
            score += 10 * mult
        } else {
            streak = 0
        }
        questionCounter++
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.testTag("sprint_game_back")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (timeLeft <= 10) extended.coralContainer else extended.starContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = if (timeLeft <= 10) extended.coral else extended.starGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${timeLeft}d",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = extended.coral,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Skor $score",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        LinearProgressIndicator(
            progress = { (timeLeft.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) },
            color = if (timeLeft <= 10) extended.coral else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
        )

        if (finished) {
            GameResultCard(
                title = "Waktu Habis! Skor $score",
                correctCount = correctCount,
                totalCount = answeredCount.coerceAtLeast(1),
                earnedStars = earnedStars,
                onPlayAgain = {
                    timeLeft = totalSeconds
                    questionCounter = 1
                    correctCount = 0
                    answeredCount = 0
                    streak = 0
                    score = 0
                    finished = false
                    question = MathProblemGenerator.generateGameQuestion(1, grade, topic)
                },
                onBackToHub = onExit
            )
        } else {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, extended.sunnyPill),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (streak >= 3) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = extended.coralContainer
                        ) {
                            Text(
                                text = "Streak Api $streak! (x${if (streak >= 6) 3 else 2})",
                                style = MaterialTheme.typography.labelLarge,
                                color = extended.coral,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = question.prompt,
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val tileColors = listOf(
                MaterialTheme.colorScheme.primary,
                extended.coral,
                extended.success,
                extended.berry
            )
            val rows = question.options.chunked(2)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rows.forEachIndexed { rowIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { colIdx, optionVal ->
                            val idx = rowIdx * 2 + colIdx
                            Surface(
                                onClick = { answerOption(optionVal) },
                                shape = RoundedCornerShape(20.dp),
                                color = tileColors[idx % tileColors.size],
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(92.dp)
                                    .testTag("sprint_option_$idx")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = optionVal.toString(),
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameResultCard(
    title: String,
    correctCount: Int,
    totalCount: Int,
    earnedStars: Int,
    onPlayAgain: () -> Unit,
    onBackToHub: () -> Unit
) {
    val extended = PintarTheme.extended
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, extended.sunnyPill),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(extended.sunnyPill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = extended.sunnyPillContent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = extended.starContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$correctCount/$totalCount",
                            style = MaterialTheme.typography.headlineSmall,
                            color = extended.success
                        )
                        Text("Jawaban Benar", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = extended.starGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+$earnedStars",
                                style = MaterialTheme.typography.headlineSmall,
                                color = extended.starGold
                            )
                        }
                        Text("Bintang Reward", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(
                text = "Skor dan bintang sudah langsung dilaporkan secara real-time ke dashboard Orang Tua.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToHub,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Pilih Game")
                }
                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Main Lagi")
                }
            }
        }
    }
}
