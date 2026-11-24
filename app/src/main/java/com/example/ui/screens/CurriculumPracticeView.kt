package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityMode
import com.example.data.ChildProfile
import com.example.data.CurriculumProblemGenerator
import com.example.data.CurriculumVisual
import com.example.data.MathTopic
import com.example.data.ShapeKind
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.PintarTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CurriculumPracticeView(
    profile: ChildProfile,
    mode: ActivityMode,
    refreshNonce: Int,
    onRequestNewSet: () -> Unit,
    onCompleteSession: (
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
    val questions = remember(mode, profile.gradeInt, refreshNonce) {
        CurriculumProblemGenerator.generateSet(mode = mode, grade = profile.gradeInt, count = 5)
    }
    val sessionStartMs = remember(questions) { mutableLongStateOf(System.currentTimeMillis()) }

    var currentIdx by remember(questions) { mutableIntStateOf(0) }
    var selectedOption by remember(questions, currentIdx) { mutableStateOf<Int?>(null) }
    var correctCount by remember(questions) { mutableIntStateOf(0) }
    var showCompletionDialog by remember(questions) { mutableStateOf(false) }
    var earnedStars by remember(questions) { mutableIntStateOf(0) }

    val question = questions.getOrNull(currentIdx) ?: return
    val isAnswered = selectedOption != null
    val isCorrect = selectedOption == question.correctIndex

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Progress Stepper + Phase & Competency Badges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    questions.indices.forEach { idx ->
                        val color = when {
                            idx < currentIdx -> extended.success
                            idx == currentIdx -> MaterialTheme.colorScheme.primary
                            else -> extended.cardBorder
                        }
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (idx == currentIdx) 28.dp else 14.dp)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = extended.sunnyPill
                ) {
                    Text(
                        text = question.phaseLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.sunnyPillContent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Main Interactive Question Card + Custom Visual Aid
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curriculum_question_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = question.competencyTag,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Soal ${currentIdx + 1}/${questions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = question.prompt,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )

                    if (question.visual != CurriculumVisual.None) {
                        CurriculumVisualRenderer(
                            visual = question.visual,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Answer Option Tiles (Comparison 3-in-a-row or 2x2 / Full Cards)
        item {
            val isComparisonOptions = question.options.size == 3 &&
                question.options.containsAll(listOf("<", "=", ">"))

            if (isComparisonOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    question.options.forEachIndexed { idx, label ->
                        CurriculumOptionButton(
                            label = label,
                            badgeLetter = null,
                            isLargeSymbol = true,
                            isAnswered = isAnswered,
                            isSelected = selectedOption == idx,
                            isCorrectOption = idx == question.correctIndex,
                            onClick = {
                                if (!isAnswered) {
                                    selectedOption = idx
                                    if (idx == question.correctIndex) correctCount++
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("curriculum_option_$idx")
                        )
                    }
                }
            } else {
                val letters = listOf("A", "B", "C", "D")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    question.options.chunked(2).forEachIndexed { rowIdx, rowOpts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowOpts.forEachIndexed { colIdx, label ->
                                val optIndex = rowIdx * 2 + colIdx
                                CurriculumOptionButton(
                                    label = label,
                                    badgeLetter = letters.getOrElse(optIndex) { "" },
                                    isLargeSymbol = false,
                                    isAnswered = isAnswered,
                                    isSelected = selectedOption == optIndex,
                                    isCorrectOption = optIndex == question.correctIndex,
                                    onClick = {
                                        if (!isAnswered) {
                                            selectedOption = optIndex
                                            if (optIndex == question.correctIndex) correctCount++
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("curriculum_option_$optIndex")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Instant Pedagogical Explanation + Next Button
        item {
            AnimatedVisibility(visible = isAnswered) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCorrect) extended.successContainer else extended.coralContainer,
                        border = BorderStroke(
                            1.5.dp,
                            if (isCorrect) extended.success else extended.coral
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = if (isCorrect) extended.success else extended.coral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isCorrect) "Hebat! Jawabanmu Benar!" else "Yuk Lihat Pembahasannya:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentIdx + 1 < questions.size) {
                                currentIdx++
                            } else {
                                val stars = (correctCount * 16) + 15
                                val durationSec = ((System.currentTimeMillis() - sessionStartMs.longValue) / 1000L)
                                    .toInt().coerceIn(5, 3600)
                                earnedStars = stars
                                showCompletionDialog = true
                                onCompleteSession(
                                    mode,
                                    question.topic,
                                    profile.gradeInt,
                                    questions.size,
                                    correctCount,
                                    stars,
                                    durationSec
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("curriculum_next_button")
                    ) {
                        Text(
                            text = if (currentIdx + 1 < questions.size) "Soal Berikutnya" else "Selesai & Ambil Bintang",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
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
                    text = "Keren, ${profile.childName}!",
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
                        text = "Sesi ${mode.labelId} (${question.phaseLabel}) selesai!",
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
                                Text("+$earnedStars ★", style = MaterialTheme.typography.titleLarge, color = extended.starGold)
                                Text("Bintang", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$correctCount/${questions.size}", style = MaterialTheme.typography.titleLarge, color = extended.success)
                                Text("Benar", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text(
                        text = "Progres Kurikulum Merdeka sudah tersinkron ke Laporan Orang Tua secara real-time.",
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
                        onRequestNewSet()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("curriculum_new_set_button")
                ) {
                    Text("Latihan Set Baru")
                }
            }
        )
    }
}

@Composable
private fun CurriculumOptionButton(
    label: String,
    badgeLetter: String?,
    isLargeSymbol: Boolean,
    isAnswered: Boolean,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    val bgColor = when {
        isAnswered && isCorrectOption -> extended.successContainer
        isAnswered && isSelected && !isCorrectOption -> extended.coralContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isAnswered && isCorrectOption -> extended.success
        isAnswered && isSelected && !isCorrectOption -> extended.coral
        else -> extended.cardBorder
    }

    Surface(
        onClick = onClick,
        enabled = !isAnswered,
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(if (isAnswered && (isCorrectOption || isSelected)) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (!isAnswered) 2.dp else 0.dp,
        modifier = modifier.height(if (isLargeSymbol) 70.dp else 62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isLargeSymbol) Arrangement.Center else Arrangement.Start
        ) {
            if (badgeLetter != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isAnswered && isCorrectOption -> extended.success
                                isAnswered && isSelected -> extended.coral
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnswered && (isCorrectOption || isSelected)) {
                        Icon(
                            imageVector = if (isCorrectOption) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = badgeLetter,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = label,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isLargeSymbol) 32.sp else 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CurriculumVisualRenderer(
    visual: CurriculumVisual,
    modifier: Modifier = Modifier
) {
    val extended = PintarTheme.extended
    val primary = MaterialTheme.colorScheme.primary
    val secondary = extended.coral

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            when (visual) {
                CurriculumVisual.None -> Unit

                is CurriculumVisual.ComparisonCard -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        NumberBadgeTile(visual.leftLabel, primary)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(extended.sunnyPill),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = extended.sunnyPillContent
                            )
                        }
                        NumberBadgeTile(visual.rightLabel, secondary)
                    }
                }

                is CurriculumVisual.FractionBar -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SingleFractionBarRow(
                            numerator = visual.numerator,
                            denominator = visual.denominator,
                            fillColor = primary,
                            showLabel = visual.secondNumerator != null
                        )
                        if (visual.secondNumerator != null && visual.secondDenominator != null) {
                            Text(
                                text = "dibandingkan dengan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleFractionBarRow(
                                numerator = visual.secondNumerator,
                                denominator = visual.secondDenominator,
                                fillColor = secondary,
                                showLabel = true
                            )
                        }
                    }
                }

                is CurriculumVisual.ClockFace -> {
                    AnalogClockCanvas(hour = visual.hour, minute = visual.minute)
                }

                is CurriculumVisual.GeometryDiagram -> {
                    GeometryDiagramCanvas(visual = visual)
                }

                is CurriculumVisual.BarChart -> {
                    MiniBarChartView(title = visual.chartTitle, bars = visual.bars)
                }

                is CurriculumVisual.PatternSequence -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visual.tiles.forEach { tile ->
                            val isMystery = tile.contains("?")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMystery) extended.sunnyPill else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isMystery) 2.dp else 1.5.dp,
                                    color = if (isMystery) extended.starGold else primary.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tile,
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (isMystery) extended.sunnyPillContent else MaterialTheme.colorScheme.onSurface
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
private fun NumberBadgeTile(label: String, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, accentColor),
        shadowElevation = 2.dp
    ) {
        Text(
            text = label,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SingleFractionBarRow(
    numerator: Int,
    denominator: Int,
    fillColor: Color,
    showLabel: Boolean
) {
    val safeDenom = denominator.coerceAtLeast(1)
    val safeNum = numerator.coerceIn(0, safeDenom)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val emptyColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showLabel) {
            Text(
                text = "$safeNum/$safeDenom",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = fillColor,
                modifier = Modifier.width(44.dp)
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            val segW = size.width / safeDenom
            for (i in 0 until safeDenom) {
                val left = i * segW
                drawRect(
                    color = if (i < safeNum) fillColor else emptyColor,
                    topLeft = Offset(left, 0f),
                    size = Size(segW, size.height)
                )
                drawRect(
                    color = borderColor,
                    topLeft = Offset(left, 0f),
                    size = Size(segW, size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun AnalogClockCanvas(hour: Int, minute: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val coral = PintarTheme.extended.coral
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(132.dp)) {
            val radius = size.minDimension / 2f - 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = surface, radius = radius, center = center)
            drawCircle(
                color = primary,
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // 12 hour tick marks
            for (i in 0 until 12) {
                val angle = (i * 30.0 - 90.0) * PI / 180.0
                val isQuarter = i % 3 == 0
                val innerR = radius * if (isQuarter) 0.78f else 0.87f
                val outerR = radius * 0.94f
                drawLine(
                    color = if (isQuarter) primary else onSurface.copy(alpha = 0.45f),
                    start = Offset(
                        center.x + (innerR * cos(angle)).toFloat(),
                        center.y + (innerR * sin(angle)).toFloat()
                    ),
                    end = Offset(
                        center.x + (outerR * cos(angle)).toFloat(),
                        center.y + (outerR * sin(angle)).toFloat()
                    ),
                    strokeWidth = if (isQuarter) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Hour hand
            val hourAngle = (((hour % 12) + minute / 60.0) * 30.0 - 90.0) * PI / 180.0
            val hourLen = radius * 0.50f
            drawLine(
                color = onSurface,
                start = center,
                end = Offset(
                    center.x + (hourLen * cos(hourAngle)).toFloat(),
                    center.y + (hourLen * sin(hourAngle)).toFloat()
                ),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute hand
            val minAngle = (minute * 6.0 - 90.0) * PI / 180.0
            val minLen = radius * 0.74f
            drawLine(
                color = coral,
                start = center,
                end = Offset(
                    center.x + (minLen * cos(minAngle)).toFloat(),
                    center.y + (minLen * sin(minAngle)).toFloat()
                ),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(color = primary, radius = 5.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun GeometryDiagramCanvas(visual: CurriculumVisual.GeometryDiagram) {
    val primary = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    val coral = PintarTheme.extended.coral

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (visual.topLabel.isNotBlank()) {
            Text(
                text = visual.topLabel,
                style = MaterialTheme.typography.labelLarge,
                color = primary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 140.dp, height = 96.dp)) {
                val stroke = Stroke(width = 3.dp.toPx())
                when (visual.shape) {
                    ShapeKind.SQUARE -> {
                        val s = 80.dp.toPx()
                        val left = (size.width - s) / 2f
                        val top = (size.height - s) / 2f
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(left, top),
                            size = Size(s, s),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(left, top),
                            size = Size(s, s),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = stroke
                        )
                    }
                    ShapeKind.RECTANGLE -> {
                        val w = 124.dp.toPx()
                        val h = 72.dp.toPx()
                        val left = (size.width - w) / 2f
                        val top = (size.height - h) / 2f
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = stroke
                        )
                    }
                    ShapeKind.TRIANGLE -> {
                        val path = Path().apply {
                            moveTo(size.width / 2f, 8.dp.toPx())
                            lineTo(size.width - 14.dp.toPx(), size.height - 8.dp.toPx())
                            lineTo(14.dp.toPx(), size.height - 8.dp.toPx())
                            close()
                        }
                        drawPath(path = path, color = fill)
                        drawPath(path = path, color = primary, style = stroke)
                    }
                    ShapeKind.CUBE -> {
                        val s = 56.dp.toPx()
                        val dx = 20.dp.toPx()
                        val dy = 16.dp.toPx()
                        val frontLeft = (size.width - s - dx) / 2f
                        val frontTop = (size.height - s + dy) / 2f
                        // Back face
                        drawRect(
                            color = fill.copy(alpha = 0.45f),
                            topLeft = Offset(frontLeft + dx, frontTop - dy),
                            size = Size(s, s)
                        )
                        drawRect(
                            color = primary.copy(alpha = 0.65f),
                            topLeft = Offset(frontLeft + dx, frontTop - dy),
                            size = Size(s, s),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // Front face
                        drawRect(
                            color = fill,
                            topLeft = Offset(frontLeft, frontTop),
                            size = Size(s, s)
                        )
                        drawRect(
                            color = primary,
                            topLeft = Offset(frontLeft, frontTop),
                            size = Size(s, s),
                            style = stroke
                        )
                        // Connecting edges
                        val corners = listOf(
                            Offset(frontLeft, frontTop) to Offset(frontLeft + dx, frontTop - dy),
                            Offset(frontLeft + s, frontTop) to Offset(frontLeft + s + dx, frontTop - dy),
                            Offset(frontLeft + s, frontTop + s) to Offset(frontLeft + s + dx, frontTop + s - dy)
                        )
                        corners.forEach { (start, end) ->
                            drawLine(color = primary, start = start, end = end, strokeWidth = 2.5.dp.toPx())
                        }
                    }
                }
            }
            if (visual.sideLabel.isNotBlank()) {
                Text(
                    text = visual.sideLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = coral
                )
            }
        }
    }
}

@Composable
private fun MiniBarChartView(
    title: String,
    bars: List<Pair<String, Int>>
) {
    val maxVal = (bars.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        PintarTheme.extended.coral,
        PintarTheme.extended.success,
        PintarTheme.extended.starGold
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { idx, (label, value) ->
                val heightFraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0.18f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette[idx % palette.size]
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height((70f * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(palette[idx % palette.size])
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
