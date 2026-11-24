package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityMode
import com.example.data.ChildProfile
import com.example.data.MathProblemGenerator
import com.example.data.MathTopic
import com.example.data.StackedProblem
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.PintarTheme

@Composable
fun WorksheetScreen(
    profile: ChildProfile,
    selectedTopic: MathTopic,
    onSelectTopic: (MathTopic) -> Unit,
    onSelectGrade: (Int) -> Unit,
    onCompleteWorksheet: (
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
    val extended = PintarTheme.extended
    val availableTopics = remember(profile.gradeInt) {
        MathProblemGenerator.topicsForGrade(profile.gradeInt)
    }
    val effectiveTopic = remember(selectedTopic, availableTopics) {
        if (selectedTopic in availableTopics) selectedTopic else availableTopics.first()
    }

    var setNonce by remember { mutableIntStateOf(0) }
    var sessionStartMs by remember(profile.gradeInt, effectiveTopic, setNonce) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    val problems = remember(profile.gradeInt, effectiveTopic, setNonce) {
        MathProblemGenerator.generateWorksheetSet(
            grade = profile.gradeInt,
            topic = effectiveTopic,
            count = 5
        )
    }

    // Per-card digit entries: Map<problemIndex, List<String>> (left-to-right digit slots)
    val digitEntries = remember(problems) {
        mutableStateMapOf<Int, List<String>>().apply {
            problems.forEach { p -> put(p.index, List(p.digitCount) { "" }) }
        }
    }
    val solvedCorrectly = remember(problems) { mutableStateMapOf<Int, Boolean>() }
    val firstTryMistakes = remember(problems) { mutableStateMapOf<Int, Boolean>() }
    val feedbackWrong = remember(problems) { mutableStateMapOf<Int, Boolean>() }
    val visibleHints = remember(problems) { mutableStateMapOf<Int, Boolean>() }

    var activeProblemIndex by remember(problems) { mutableIntStateOf(1) }
    // Right-to-left entry cursor: starts at rightmost digit (satuan) like vertical school math!
    var activeDigitSlot by remember(problems) {
        mutableIntStateOf((problems.firstOrNull()?.digitCount ?: 1) - 1)
    }
    var showCompletionDialog by remember(problems) { mutableStateOf(false) }
    var lastEarnedStars by remember(problems) { mutableIntStateOf(0) }
    var lastFirstTryCorrect by remember(problems) { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    LaunchedEffect(activeProblemIndex) {
        val targetIdx = (activeProblemIndex - 1).coerceIn(0, (problems.size - 1).coerceAtLeast(0))
        listState.animateScrollToItem(targetIdx)
    }

    fun selectProblem(problem: StackedProblem) {
        activeProblemIndex = problem.index
        val digits = digitEntries[problem.index] ?: List(problem.digitCount) { "" }
        // Focus rightmost empty box (satuan first), or rightmost box if all filled
        val rightmostEmpty = digits.indexOfLast { it.isEmpty() }
        activeDigitSlot = if (rightmostEmpty >= 0) rightmostEmpty else (problem.digitCount - 1)
    }

    fun handleDigitInput(digit: String) {
        val problem = problems.firstOrNull { it.index == activeProblemIndex } ?: return
        if (solvedCorrectly[problem.index] == true) return

        val current = (digitEntries[problem.index] ?: List(problem.digitCount) { "" }).toMutableList()
        val slot = activeDigitSlot.coerceIn(0, current.lastIndex)
        current[slot] = digit
        digitEntries[problem.index] = current
        feedbackWrong[problem.index] = false

        // Advance right-to-left (satuan -> puluhan -> ratusan) like real vertical calculation!
        if (slot > 0 && current[slot - 1].isEmpty()) {
            activeDigitSlot = slot - 1
        } else {
            val nextEmptyFromRight = current.indexOfLast { it.isEmpty() }
            if (nextEmptyFromRight >= 0) {
                activeDigitSlot = nextEmptyFromRight
            }
        }
    }

    fun handleBackspace() {
        val problem = problems.firstOrNull { it.index == activeProblemIndex } ?: return
        if (solvedCorrectly[problem.index] == true) return

        val current = (digitEntries[problem.index] ?: List(problem.digitCount) { "" }).toMutableList()
        val slot = activeDigitSlot.coerceIn(0, current.lastIndex)
        if (current[slot].isNotEmpty()) {
            current[slot] = ""
            digitEntries[problem.index] = current
        } else if (slot < current.lastIndex) {
            activeDigitSlot = slot + 1
            current[activeDigitSlot] = ""
            digitEntries[problem.index] = current
        }
        feedbackWrong[problem.index] = false
    }

    fun handleCheckActiveProblem() {
        val problem = problems.firstOrNull { it.index == activeProblemIndex } ?: return
        if (solvedCorrectly[problem.index] == true) {
            val nextUnsolved = problems.firstOrNull { solvedCorrectly[it.index] != true }
            if (nextUnsolved != null) selectProblem(nextUnsolved)
            return
        }

        val entered = (digitEntries[problem.index] ?: emptyList()).joinToString("")
        if (entered.length < problem.digitCount) {
            feedbackWrong[problem.index] = true
            return
        }

        if (entered.toIntOrNull() == problem.answer) {
            solvedCorrectly[problem.index] = true
            feedbackWrong[problem.index] = false

            val nextUnsolved = problems.firstOrNull { solvedCorrectly[it.index] != true }
            if (nextUnsolved != null) {
                selectProblem(nextUnsolved)
            } else {
                // Entire 5-problem worksheet finished!
                val firstTryCount = problems.count { firstTryMistakes[it.index] != true }
                val stars = (firstTryCount * 15) + ((problems.size - firstTryCount) * 8) + 15
                val durationSec = ((System.currentTimeMillis() - sessionStartMs) / 1000L)
                    .toInt().coerceIn(5, 3600)
                lastFirstTryCorrect = firstTryCount
                lastEarnedStars = stars
                showCompletionDialog = true
                onCompleteWorksheet(
                    ActivityMode.WORKSHEET,
                    effectiveTopic,
                    profile.gradeInt,
                    problems.size,
                    firstTryCount,
                    stars,
                    durationSec
                )
            }
        } else {
            firstTryMistakes[problem.index] = true
            feedbackWrong[problem.index] = true
            visibleHints[problem.index] = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: Grade selector + Topic selector + Progress pill
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lembar Hitung Bersusun",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kelas ${profile.gradeInt} SD • ${solvedCorrectly.size}/${problems.size} Selesai",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = extended.starContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = extended.starGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${profile.totalStars}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        IconButton(
                            onClick = { setNonce++ },
                            modifier = Modifier.testTag("worksheet_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Buat Soal Baru",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Grade 1-6 + Topic horizontal chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items((1..6).toList()) { grade ->
                        val selected = profile.gradeInt == grade
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectGrade(grade) },
                            label = {
                                Text("Kls $grade", style = MaterialTheme.typography.labelMedium)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extended.sunnyPill,
                                selectedLabelColor = extended.sunnyPillContent
                            ),
                            modifier = Modifier.height(34.dp)
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .width(1.dp)
                                .height(20.dp)
                                .background(extended.cardBorder)
                        )
                    }
                    items(availableTopics) { topic ->
                        val selected = effectiveTopic == topic
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTopic(topic) },
                            label = {
                                Text(
                                    text = "${topic.symbol} ${topic.shortLabel}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("worksheet_topic_${topic.code.lowercase()}")
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = extended.cardBorder)

        // Scrollable Vertical Stacked Worksheet Cards (inspired directly by the uploaded screenshot)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(problems, key = { _, p -> "${setNonce}_${p.index}" }) { _, problem ->
                val isActive = problem.index == activeProblemIndex
                val isSolved = solvedCorrectly[problem.index] == true
                val isWrong = feedbackWrong[problem.index] == true
                val showHint = visibleHints[problem.index] == true
                val digits = digitEntries[problem.index] ?: List(problem.digitCount) { "" }

                StackedWorksheetCard(
                    problem = problem,
                    digits = digits,
                    isActive = isActive,
                    activeDigitSlot = if (isActive) activeDigitSlot else -1,
                    isSolved = isSolved,
                    isWrong = isWrong,
                    showHint = showHint,
                    onSelectCard = { selectProblem(problem) },
                    onSelectDigitSlot = { slot ->
                        activeProblemIndex = problem.index
                        activeDigitSlot = slot
                    },
                    onToggleHint = {
                        visibleHints[problem.index] = !(visibleHints[problem.index] ?: false)
                    }
                )
            }
        }

        // Fixed Tactile Numeric Keypad (cleanly mirrors the screenshot's bottom number pad)
        TactileMathKeypad(
            onDigit = ::handleDigitInput,
            onBackspace = ::handleBackspace,
            onSubmit = ::handleCheckActiveProblem
        )
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(extended.sunnyPill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = extended.sunnyPillContent,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Keren Banget, ${profile.childName}!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kamu menyelesaikan ${problems.size} soal hitung bersusun (${effectiveTopic.labelId} Kelas ${profile.gradeInt})!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = extended.starContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+$lastEarnedStars ★", style = MaterialTheme.typography.titleLarge, color = extended.starGold)
                                Text("Bintang", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$lastFirstTryCorrect/${problems.size}", style = MaterialTheme.typography.titleLarge, color = extended.success)
                                Text("Tepat 1x", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text(
                        text = "Hasil latihan ini sudah langsung terkirim ke Laporan Orang Tua secara real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        setNonce++
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("worksheet_next_set_button")
                ) {
                    Text("Latihan Set Baru")
                }
            }
        )
    }
}

@Composable
private fun StackedWorksheetCard(
    problem: StackedProblem,
    digits: List<String>,
    isActive: Boolean,
    activeDigitSlot: Int,
    isSolved: Boolean,
    isWrong: Boolean,
    showHint: Boolean,
    onSelectCard: () -> Unit,
    onSelectDigitSlot: (Int) -> Unit,
    onToggleHint: () -> Unit
) {
    val extended = PintarTheme.extended
    val borderColor = when {
        isSolved -> extended.success
        isWrong -> extended.coral
        isActive -> MaterialTheme.colorScheme.onSurface
        else -> extended.cardBorder
    }
    val borderWidth = if (isActive || isSolved || isWrong) 2.5.dp else 1.5.dp

    val topStr = problem.topNumber.toString()
    val botStr = problem.bottomNumber.toString()
    val columnCount = maxOf(topStr.length, botStr.length, problem.digitCount)
    val paddedTop = topStr.padStart(columnCount, ' ')
    val paddedBot = botStr.padStart(columnCount, ' ')

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSolved) extended.successContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onSelectCard)
            .testTag("worksheet_card_${problem.index}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Top Row: [1] 82 − 57 + Hint / Solved badge (matches reference image!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSolved) extended.success else MaterialTheme.colorScheme.primary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSolved) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Benar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "${problem.index}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = problem.horizontalLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSolved) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = extended.successContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = extended.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Benar! +15★",
                                style = MaterialTheme.typography.labelMedium,
                                color = extended.success
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = extended.starContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleHint)
                            .testTag("worksheet_hint_${problem.index}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = "Bantuan",
                                tint = extended.starGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bantuan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vertical Stacked Arithmetic Layout (strictly column-aligned!)
            val cellWidth = if (columnCount >= 5) 42.dp else 52.dp
            Column(horizontalAlignment = Alignment.End) {
                // Row 1: Top Number
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.width(34.dp))
                    paddedTop.forEach { ch ->
                        Box(
                            modifier = Modifier.width(cellWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ch == ' ') "" else ch.toString(),
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Operator + Bottom Number
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.width(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = problem.operator,
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    paddedBot.forEach { ch ->
                        Box(
                            modifier = Modifier.width(cellWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ch == ' ') "" else ch.toString(),
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Calculation Bar
                val totalBarWidth = 34.dp + (cellWidth * columnCount) + (8.dp * columnCount)
                Box(
                    modifier = Modifier
                        .width(totalBarWidth)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Row 3: Individual Digit Input Boxes (Active blue highlight + dashed inactive boxes!)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.width(34.dp))
                    val leadingSpacers = (columnCount - problem.digitCount).coerceAtLeast(0)
                    repeat(leadingSpacers) {
                        Spacer(modifier = Modifier.width(cellWidth))
                    }
                    digits.forEachIndexed { idx, value ->
                        DigitInputBox(
                            value = value,
                            isFocused = isActive && idx == activeDigitSlot && !isSolved,
                            isSolved = isSolved,
                            isWrong = isWrong,
                            boxWidth = cellWidth,
                            onClick = { if (!isSolved) onSelectDigitSlot(idx) },
                            modifier = Modifier.testTag("digit_box_${problem.index}_$idx")
                        )
                    }
                }
            }

            if (isWrong && !isSolved) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Belum tepat nih! Coba hitung lagi dari kanan (satuan) ya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.coral,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(visible = showHint && !isSolved) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = extended.starContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = "Tip: ${problem.hintText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitInputBox(
    value: String,
    isFocused: Boolean,
    isSolved: Boolean,
    isWrong: Boolean,
    boxWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    val boxBg = when {
        isSolved -> extended.successContainer
        isWrong -> extended.coralContainer
        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .size(width = boxWidth, height = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(boxBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = if (isFocused || isSolved || isWrong) 3.dp.toPx() else 2.dp.toPx()
            val dashEffect = if (!isFocused && !isSolved && !isWrong && value.isEmpty()) {
                PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            } else {
                null
            }
            val color = when {
                isSolved -> extended.success
                isWrong -> extended.coral
                isFocused -> primaryColor
                else -> outlineColor
            }
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = strokePx, pathEffect = dashEffect)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = when {
                        isSolved -> extended.success
                        isWrong -> extended.coral
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .padding(start = if (value.isNotEmpty()) 2.dp else 0.dp)
                        .width(2.5.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(primaryColor.copy(alpha = cursorAlpha))
                )
            }
        }
    }
}

@Composable
private fun TactileMathKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val extended = PintarTheme.extended
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )
            rows.forEach { rowDigits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowDigits.forEach { d ->
                        KeypadDigitButton(
                            label = d,
                            onClick = { onDigit(d) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("keypad_digit_$d")
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onBackspace)
                        .testTag("keypad_backspace")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "Hapus Angka",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                KeypadDigitButton(
                    label = "0",
                    onClick = { onDigit("0") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keypad_digit_0")
                )
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("keypad_submit")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Cek Jawaban",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cek", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extended.cardBorder),
        shadowElevation = 1.dp,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
