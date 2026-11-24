package com.example.data

import kotlin.math.abs
import kotlin.random.Random

data class StackedProblem(
    val index: Int,
    val topNumber: Int,
    val bottomNumber: Int,
    val operator: String,
    val answer: Int,
    val topic: MathTopic,
    val grade: Int,
    val hintText: String
) {
    val answerString: String get() = answer.toString()
    val digitCount: Int get() = answerString.length
    val horizontalLabel: String get() = "$topNumber $operator $bottomNumber"
}

data class GameQuestion(
    val id: Int,
    val prompt: String,
    val correctAnswer: Int,
    val options: List<Int>,
    val topic: MathTopic
)

object MathProblemGenerator {

    fun topicsForGrade(grade: Int): List<MathTopic> = when (grade.coerceIn(1, 6)) {
        1 -> listOf(MathTopic.PENJUMLAHAN, MathTopic.PENGURANGAN, MathTopic.CAMPURAN)
        2 -> listOf(MathTopic.PENJUMLAHAN, MathTopic.PENGURANGAN, MathTopic.PERKALIAN, MathTopic.CAMPURAN)
        else -> MathTopic.entries.filter { it.isArithmetic }
    }

    fun gradeCurriculumSummary(grade: Int): String = when (grade.coerceIn(1, 6)) {
        1 -> "Fase A: Bilangan 1–20, nilai tempat, pola loncat & bangun datar"
        2 -> "Fase A: Bersusun simpan/pinjam, waktu jam, satuan cm/kg & soal cerita"
        3 -> "Fase B: Bersusun 3 digit, pecahan visual, keliling & pola bilangan"
        4 -> "Fase B: Bilangan ribuan, pecahan senilai, luas bangun & sudut"
        5 -> "Fase C: Desimal & persen, volume kubus/balok, KPK-FPB & diagram"
        else -> "Fase C: Rasio & skala, rata-rata data, aljabar awal & soal AKM"
    }

    fun generateWorksheetSet(
        grade: Int,
        topic: MathTopic,
        count: Int = 5,
        random: Random = Random.Default
    ): List<StackedProblem> {
        val safeGrade = grade.coerceIn(1, 6)
        return (1..count).map { idx ->
            val effectiveTopic = if (topic == MathTopic.CAMPURAN) {
                val allowed = topicsForGrade(safeGrade).filter { it != MathTopic.CAMPURAN }
                allowed[random.nextInt(allowed.size)]
            } else {
                topic
            }
            generateSingleStackedProblem(idx, safeGrade, effectiveTopic, random)
        }
    }

    private fun generateSingleStackedProblem(
        index: Int,
        grade: Int,
        topic: MathTopic,
        random: Random
    ): StackedProblem {
        return when (topic) {
            MathTopic.PENJUMLAHAN -> generateAddition(index, grade, random)
            MathTopic.PENGURANGAN -> generateSubtraction(index, grade, random)
            MathTopic.PERKALIAN -> generateMultiplication(index, grade, random)
            MathTopic.PEMBAGIAN -> generateDivision(index, grade, random)
            else -> generateAddition(index, grade, random)
        }
    }

    private fun generateAddition(index: Int, grade: Int, random: Random): StackedProblem {
        val (a, b) = when (grade) {
            1 -> random.nextInt(4, 16) to random.nextInt(2, 10)
            2 -> random.nextInt(18, 79) to random.nextInt(9, 48)
            3 -> random.nextInt(125, 680) to random.nextInt(45, 315)
            4 -> random.nextInt(1120, 4850) to random.nextInt(320, 2940)
            5 -> random.nextInt(2350, 6450) to random.nextInt(1240, 3490)
            else -> random.nextInt(3450, 7950) to random.nextInt(1560, 4950)
        }
        val ans = a + b
        val onesSum = (a % 10) + (b % 10)
        val hint = if (onesSum >= 10) {
            "Mulai dari kanan (satuan): ${a % 10} + ${b % 10} = $onesSum. Tulis ${onesSum % 10}, simpan 1 ke puluhan!"
        } else {
            "Mulai dari kolom paling kanan (satuan): ${a % 10} + ${b % 10} = $onesSum, lalu lanjut ke kolom kiri."
        }
        return StackedProblem(index, a, b, "+", ans, MathTopic.PENJUMLAHAN, grade, hint)
    }

    private fun generateSubtraction(index: Int, grade: Int, random: Random): StackedProblem {
        val (a, b) = when (grade) {
            1 -> {
                val top = random.nextInt(8, 20)
                top to random.nextInt(2, top)
            }
            2 -> {
                // Deliberately includes borrowing problems like 82 - 57 and 34 - 7
                if (index == 1 && random.nextBoolean()) {
                    val tensTop = random.nextInt(4, 9)
                    val onesTop = random.nextInt(1, 6)
                    val tensBot = random.nextInt(1, tensTop)
                    val onesBot = random.nextInt(onesTop + 1, 10)
                    (tensTop * 10 + onesTop) to (tensBot * 10 + onesBot)
                } else {
                    val top = random.nextInt(25, 96)
                    val bot = random.nextInt(6, top - 3)
                    top to bot
                }
            }
            3 -> {
                val top = random.nextInt(240, 890)
                top to random.nextInt(65, top - 25)
            }
            4 -> {
                val top = random.nextInt(1500, 6500)
                top to random.nextInt(420, top - 200)
            }
            5 -> {
                val top = random.nextInt(3200, 8900)
                top to random.nextInt(1150, top - 400)
            }
            else -> {
                val top = random.nextInt(5400, 9850)
                top to random.nextInt(1850, top - 600)
            }
        }
        val ans = a - b
        val onesTop = a % 10
        val onesBot = b % 10
        val hint = if (onesTop < onesBot) {
            "Karena $onesTop lebih kecil dari $onesBot, pinjam 1 puluhan: ${onesTop + 10} − $onesBot = ${onesTop + 10 - onesBot}. Lalu kurangi kolom puluhan!"
        } else {
            "Mulai dari kanan (satuan): $onesTop − $onesBot = ${onesTop - onesBot}, lalu lanjut kurangi kolom kirinya."
        }
        return StackedProblem(index, a, b, "−", ans, MathTopic.PENGURANGAN, grade, hint)
    }

    private fun generateMultiplication(index: Int, grade: Int, random: Random): StackedProblem {
        val (a, b) = when (grade) {
            1 -> random.nextInt(2, 6) to random.nextInt(1, 4)
            2 -> random.nextInt(2, 7) to random.nextInt(2, 6)
            3 -> random.nextInt(3, 12) to random.nextInt(3, 10)
            4 -> random.nextInt(12, 48) to random.nextInt(3, 9)
            5 -> random.nextInt(14, 56) to random.nextInt(11, 25)
            else -> random.nextInt(24, 89) to random.nextInt(12, 36)
        }
        val ans = a * b
        val hint = "Kalikan angka satuan terlebih dahulu (${a % 10} × ${b % 10} = ${(a % 10) * (b % 10)}), jangan lupa angka simpanannya!"
        return StackedProblem(index, a, b, "×", ans, MathTopic.PERKALIAN, grade, hint)
    }

    private fun generateDivision(index: Int, grade: Int, random: Random): StackedProblem {
        val (divisor, quotient) = when (grade) {
            1, 2 -> random.nextInt(2, 5) to random.nextInt(2, 6)
            3 -> random.nextInt(2, 9) to random.nextInt(3, 10)
            4 -> random.nextInt(3, 10) to random.nextInt(6, 22)
            5 -> random.nextInt(4, 13) to random.nextInt(10, 35)
            else -> random.nextInt(6, 16) to random.nextInt(12, 48)
        }
        val dividend = divisor * quotient
        val hint = "Pikirkan perkalian kebalikannya: $divisor × berapa yang hasilnya $dividend?"
        return StackedProblem(index, dividend, divisor, "÷", quotient, MathTopic.PEMBAGIAN, grade, hint)
    }

    fun generateGameQuestion(
        id: Int,
        grade: Int,
        topic: MathTopic = MathTopic.CAMPURAN,
        random: Random = Random.Default
    ): GameQuestion {
        val safeGrade = grade.coerceIn(1, 6)
        val effectiveTopic = if (topic == MathTopic.CAMPURAN) {
            val allowed = topicsForGrade(safeGrade).filter { it != MathTopic.CAMPURAN }
            allowed[random.nextInt(allowed.size)]
        } else {
            topic
        }

        // Slightly brisker mental math ranges for arcade games
        val (prompt, answer) = when (effectiveTopic) {
            MathTopic.PENJUMLAHAN -> {
                val maxVal = when (safeGrade) {
                    1 -> 12; 2 -> 35; 3 -> 95; 4 -> 220; 5 -> 450; else -> 750
                }
                val a = random.nextInt(2, maxVal)
                val b = random.nextInt(2, maxVal)
                "$a + $b = ?" to (a + b)
            }
            MathTopic.PENGURANGAN -> {
                val maxVal = when (safeGrade) {
                    1 -> 18; 2 -> 50; 3 -> 120; 4 -> 260; 5 -> 500; else -> 800
                }
                val a = random.nextInt(maxVal / 2 + 2, maxVal)
                val b = random.nextInt(2, a)
                "$a − $b = ?" to (a - b)
            }
            MathTopic.PERKALIAN -> {
                val (a, b) = when (safeGrade) {
                    1, 2 -> random.nextInt(2, 6) to random.nextInt(2, 6)
                    3 -> random.nextInt(3, 10) to random.nextInt(3, 10)
                    4 -> random.nextInt(4, 13) to random.nextInt(3, 10)
                    5 -> random.nextInt(6, 16) to random.nextInt(4, 12)
                    else -> random.nextInt(8, 20) to random.nextInt(5, 15)
                }
                "$a × $b = ?" to (a * b)
            }
            MathTopic.PEMBAGIAN -> {
                val (d, q) = when (safeGrade) {
                    1, 2 -> random.nextInt(2, 5) to random.nextInt(2, 6)
                    3 -> random.nextInt(2, 9) to random.nextInt(2, 10)
                    4 -> random.nextInt(3, 10) to random.nextInt(4, 15)
                    5 -> random.nextInt(4, 12) to random.nextInt(5, 18)
                    else -> random.nextInt(5, 15) to random.nextInt(6, 24)
                }
                "${d * q} ÷ $d = ?" to q
            }
            else -> {
                val a = random.nextInt(5, 20)
                val b = random.nextInt(2, 10)
                "$a + $b = ?" to (a + b)
            }
        }

        val distractors = mutableSetOf<Int>()
        val offsets = listOf(-10, -2, -1, 1, 2, 3, 5, 10, -5, 4, -3)
        var attempts = 0
        while (distractors.size < 3 && attempts < 40) {
            attempts++
            val delta = offsets[random.nextInt(offsets.size)]
            val candidate = answer + delta
            if (candidate >= 0 && candidate != answer) {
                distractors.add(candidate)
            }
        }
        var fallback = 1
        while (distractors.size < 3) {
            val c = abs(answer + fallback)
            if (c != answer) distractors.add(c)
            fallback++
        }

        val options = (distractors.toList() + answer).shuffled(random)
        return GameQuestion(
            id = id,
            prompt = prompt,
            correctAnswer = answer,
            options = options,
            topic = effectiveTopic
        )
    }
}
