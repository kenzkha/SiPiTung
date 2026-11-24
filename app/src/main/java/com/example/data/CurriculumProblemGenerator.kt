package com.example.data

import kotlin.math.abs
import kotlin.random.Random

enum class ShapeKind {
    SQUARE,
    RECTANGLE,
    TRIANGLE,
    CUBE
}

sealed interface CurriculumVisual {
    data object None : CurriculumVisual
    data class ComparisonCard(val leftLabel: String, val rightLabel: String) : CurriculumVisual
    data class FractionBar(
        val numerator: Int,
        val denominator: Int,
        val secondNumerator: Int? = null,
        val secondDenominator: Int? = null
    ) : CurriculumVisual
    data class ClockFace(val hour: Int, val minute: Int) : CurriculumVisual
    data class GeometryDiagram(
        val shape: ShapeKind,
        val topLabel: String,
        val sideLabel: String,
        val extraLabel: String = ""
    ) : CurriculumVisual
    data class BarChart(
        val chartTitle: String,
        val bars: List<Pair<String, Int>>
    ) : CurriculumVisual
    data class PatternSequence(val tiles: List<String>) : CurriculumVisual
}

data class CurriculumQuestion(
    val id: Int,
    val mode: ActivityMode,
    val topic: MathTopic,
    val grade: Int,
    val phaseLabel: String,
    val competencyTag: String,
    val prompt: String,
    val visual: CurriculumVisual = CurriculumVisual.None,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object CurriculumProblemGenerator {

    fun generateSet(
        mode: ActivityMode,
        grade: Int,
        count: Int = 5,
        random: Random = Random.Default
    ): List<CurriculumQuestion> {
        val safeGrade = grade.coerceIn(1, 6)
        return (1..count).map { idx ->
            when (mode) {
                ActivityMode.STORY_PROBLEM -> generateStoryProblem(idx, safeGrade, random)
                ActivityMode.FRACTION_COMPARE -> generateFractionOrCompare(idx, safeGrade, random)
                ActivityMode.GEOMETRY_MEASURE -> generateGeometryOrMeasure(idx, safeGrade, random)
                ActivityMode.PATTERN_ALGEBRA -> generatePatternOrAlgebra(idx, safeGrade, random)
                else -> generateStoryProblem(idx, safeGrade, random)
            }
        }
    }

    private fun phaseTitle(grade: Int): String = "${curriculumPhaseForGrade(grade)} • Kelas $grade SD"

    // =========================================================================
    // MODE 1: SOAL CERITA AKM (Literasi Numerasi Kurikulum Merdeka Kelas 1–6)
    // =========================================================================
    private fun generateStoryProblem(id: Int, grade: Int, random: Random): CurriculumQuestion {
        val names = listOf("Raka", "Siti", "Budi", "Ayu", "Dina", "Fajar", "Nadia", "Bima")
        val name = names[random.nextInt(names.size)]
        val phase = phaseTitle(grade)

        return when (grade) {
            1 -> when (id % 3) {
                1 -> {
                    val a = random.nextInt(5, 12)
                    val b = random.nextInt(3, 9)
                    val ans = a + b
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Literasi Numerasi",
                        "$name mempunyai $a pensil warna. Ibu membelikan lagi $b pensil warna baru. Berapa jumlah pensil warna $name sekarang?",
                        ans, "pensil",
                        "Karena ditambah pensil baru: $a + $b = $ans pensil.",
                        random
                    )
                }
                2 -> {
                    val total = random.nextInt(11, 19)
                    val eaten = random.nextInt(3, 8)
                    val ans = total - eaten
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Selisih Benda",
                        "Di piring ada $total buah jeruk. $name dan adiknya memakan $eaten jeruk. Berapa sisa jeruk di piring?",
                        ans, "jeruk",
                        "Sisa benda dihitung dengan pengurangan: $total − $eaten = $ans jeruk.",
                        random
                    )
                }
                else -> {
                    val tens = random.nextInt(1, 5)
                    val ones = random.nextInt(1, 9)
                    val ans = tens * 10 + ones
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Nilai Tempat",
                        "$name menyusun $tens ikat lidi berisi 10 batang dan $ones batang lidi lepas. Berapa jumlah seluruh lidi $name?",
                        ans, "batang",
                        "$tens puluhan = ${tens * 10}, ditambah $ones satuan = $ans batang.",
                        random
                    )
                }
            }

            2 -> when (id % 3) {
                1 -> {
                    val boxes = random.nextInt(3, 7)
                    val perBox = random.nextInt(3, 6)
                    val ans = boxes * perBox
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Perkalian Kontekstual",
                        "$name membeli $boxes kotak donat. Setiap kotak berisi $perBox donat. Berapa jumlah seluruh donat yang dibeli?",
                        ans, "donat",
                        "$boxes kotak × $perBox donat = $ans donat.",
                        random
                    )
                }
                2 -> {
                    val start = random.nextInt(35, 78)
                    val given = random.nextInt(12, 29)
                    val ans = start - given
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Pengurangan Dua Digit",
                        "Perpustakaan kelas punya $start buku cerita. Sebanyak $given buku sedang dipinjam siswa. Berapa buku yang masih ada di rak?",
                        ans, "buku",
                        "Sisa buku di rak = $start − $given = $ans buku.",
                        random
                    )
                }
                else -> {
                    val priceA = random.nextInt(2, 6) * 1000
                    val priceB = random.nextInt(2, 5) * 1000
                    val ans = priceA + priceB
                    buildRupiahChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Uang & Belanja",
                        "Di kantin sekolah, $name membeli roti seharga Rp${formatIdr(priceA)} dan susu seharga Rp${formatIdr(priceB)}. Berapa total yang harus dibayar?",
                        ans,
                        "Total belanja = Rp${formatIdr(priceA)} + Rp${formatIdr(priceB)} = Rp${formatIdr(ans)}.",
                        random
                    )
                }
            }

            3 -> when (id % 3) {
                1 -> {
                    val groups = random.nextInt(4, 9)
                    val each = random.nextInt(4, 9)
                    val total = groups * each
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Pembagian Merata",
                        "Bu Guru memiliki $total stiker bintang yang dibagikan sama rata kepada $groups siswa. Berapa stiker yang diterima tiap siswa?",
                        each, "stiker",
                        "Dibagi sama rata: $total ÷ $groups = $each stiker per siswa.",
                        random
                    )
                }
                2 -> {
                    val a = random.nextInt(135, 320)
                    val b = random.nextInt(115, 260)
                    val ans = a + b
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Bilangan Cacah 3 Digit",
                        "Pada lomba lari SD, peserta putra berjumlah $a siswa dan peserta putri $b siswa. Berapa total seluruh peserta lomba?",
                        ans, "siswa",
                        "Total peserta = $a + $b = $ans siswa.",
                        random
                    )
                }
                else -> {
                    val money = 10000
                    val buy = random.nextInt(3, 8) * 1000
                    val ans = money - buy
                    buildRupiahChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Kembalian Belanja",
                        "$name membayar buku tulis seharga Rp${formatIdr(buy)} dengan selembar uang Rp10.000. Berapa uang kembalian $name?",
                        ans,
                        "Kembalian = Rp10.000 − Rp${formatIdr(buy)} = Rp${formatIdr(ans)}.",
                        random
                    )
                }
            }

            4 -> when (id % 3) {
                1 -> {
                    val denom = listOf(2, 3, 4, 5)[random.nextInt(4)]
                    val unitVal = random.nextInt(4, 11)
                    val total = denom * unitVal
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Pecahan dari Kumpulan",
                        "Ibu membuat $total kue bolu. Sebanyak 1/$denom bagian diberikan kepada tetangga. Berapa buah kue yang diberikan?",
                        unitVal, "kue",
                        "1/$denom dari $total = $total ÷ $denom = $unitVal kue.",
                        random
                    )
                }
                2 -> {
                    val rows = random.nextInt(6, 13)
                    val cols = random.nextInt(12, 24)
                    val ans = rows * cols
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Operasi Perkalian",
                        "Aula sekolah disusun menjadi $rows baris kursi. Setiap baris berisi $cols kursi. Berapa kapasitas seluruh kursi di aula?",
                        ans, "kursi",
                        "Total kursi = $rows × $cols = $ans kursi.",
                        random
                    )
                }
                else -> {
                    val p = random.nextInt(8, 16)
                    val l = random.nextInt(5, 11)
                    val ans = 2 * (p + l)
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Keliling di Kehidupan",
                        "Taman kelas berbentuk persegi panjang berukuran panjang $p m dan lebar $l m. Jika $name berjalan 1 putaran mengelilingi taman, berapa meter jaraknya?",
                        ans, "m",
                        "Keliling persegi panjang = 2 × ($p + $l) = 2 × ${p + l} = $ans m.",
                        random
                    )
                }
            }

            5 -> when (id % 3) {
                1 -> {
                    val speed = listOf(40, 50, 60)[random.nextInt(3)]
                    val hours = random.nextInt(2, 5)
                    val ans = speed * hours
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Kecepatan & Jarak",
                        "Bus karya wisata melaju dengan kecepatan tetap $speed km/jam selama $hours jam. Berapa kilometer jarak yang ditempuh bus?",
                        ans, "km",
                        "Jarak = Kecepatan × Waktu = $speed × $hours = $ans km.",
                        random
                    )
                }
                2 -> {
                    val discountPct = listOf(10, 20, 25)[random.nextInt(3)]
                    val price = listOf(40000, 60000, 80000, 100000)[random.nextInt(4)]
                    val discount = (price * discountPct) / 100
                    val ans = price - discount
                    buildRupiahChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Persentase Diskon",
                        "Tas sekolah berharga Rp${formatIdr(price)} mendapat diskon $discountPct%. Berapa harga tas setelah diskon?",
                        ans,
                        "Diskon = $discountPct% × Rp${formatIdr(price)} = Rp${formatIdr(discount)}. Harga akhir = Rp${formatIdr(ans)}.",
                        random
                    )
                }
                else -> {
                    val pairs = listOf(Triple(4, 6, 12), Triple(3, 4, 12), Triple(6, 8, 24), Triple(4, 5, 20), Triple(6, 9, 18))
                    val (a, b, lcm) = pairs[random.nextInt(pairs.size)]
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "KPK Sehari-hari",
                        "$name berenang setiap $a hari sekali, sedangkan temannya setiap $b hari sekali. Jika hari ini mereka berenang bersama, berapa hari lagi mereka bertemu kembali?",
                        lcm, "hari",
                        "Cari KPK dari $a dan $b. Kelipatan persekutuan terkecilnya adalah $lcm hari.",
                        random
                    )
                }
            }

            else -> when (id % 3) {
                1 -> {
                    val mean = random.nextInt(76, 91)
                    val s1 = mean - 4
                    val s2 = mean + 2
                    val s3 = mean + 6
                    val s4 = mean - 4
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Rata-rata (Mean)",
                        "Nilai 4 kali ulangan matematika $name adalah $s1, $s2, $s3, dan $s4. Berapa nilai rata-rata ulangan $name?",
                        mean, "",
                        "Rata-rata = ($s1 + $s2 + $s3 + $s4) ÷ 4 = ${s1 + s2 + s3 + s4} ÷ 4 = $mean.",
                        random
                    )
                }
                2 -> {
                    val ratioA = random.nextInt(2, 5)
                    val ratioB = random.nextInt(ratioA + 1, 8)
                    val factor = random.nextInt(4, 9)
                    val valA = ratioA * factor
                    val ansB = ratioB * factor
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Rasio & Perbandingan",
                        "Perbandingan kelereng $name dan temannya adalah $ratioA : $ratioB. Jika kelereng $name ada $valA butir, berapa kelereng temannya?",
                        ansB, "butir",
                        "1 bagian = $valA ÷ $ratioA = $factor. Maka kelereng temannya = $ratioB × $factor = $ansB butir.",
                        random
                    )
                }
                else -> {
                    val scaleKmPerCm = listOf(2, 3, 5, 10)[random.nextInt(4)]
                    val mapCm = random.nextInt(4, 10)
                    val ansKm = scaleKmPerCm * mapCm
                    val scaleDenominator = scaleKmPerCm * 100000
                    buildNumericChoiceQuestion(
                        id, ActivityMode.STORY_PROBLEM, MathTopic.SOAL_CERITA, grade, phase,
                        "Skala Peta",
                        "Pada peta berskala 1 : ${formatIdr(scaleDenominator)}, jarak dua kota adalah $mapCm cm. Berapa kilometer jarak sebenarnya?",
                        ansKm, "km",
                        "1 cm di peta = $scaleKmPerCm km sebenarnya. Jadi $mapCm × $scaleKmPerCm = $ansKm km.",
                        random
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODE 2: PECAHAN & PERBANDINGAN (<, =, >, Pecahan Visual, Desimal, Persen)
    // =========================================================================
    private fun generateFractionOrCompare(id: Int, grade: Int, random: Random): CurriculumQuestion {
        val phase = phaseTitle(grade)
        return when (grade) {
            1, 2 -> {
                val maxRange = if (grade == 1) 20 else 99
                val a = random.nextInt(4, maxRange)
                val b = if (id % 3 == 0) a else random.nextInt(4, maxRange)
                val correctSymbol = when {
                    a < b -> "<"
                    a > b -> ">"
                    else -> "="
                }
                val options = listOf("<", "=", ">")
                CurriculumQuestion(
                    id = id,
                    mode = ActivityMode.FRACTION_COMPARE,
                    topic = MathTopic.PECAHAN,
                    grade = grade,
                    phaseLabel = phase,
                    competencyTag = "Membandingkan Bilangan",
                    prompt = "Pilih tanda perbandingan yang tepat untuk mengisi titik-titik di antara dua bilangan berikut:",
                    visual = CurriculumVisual.ComparisonCard(a.toString(), b.toString()),
                    options = options,
                    correctIndex = options.indexOf(correctSymbol),
                    explanation = when (correctSymbol) {
                        "<" -> "$a lebih kecil daripada $b, jadi tandanya $a < $b."
                        ">" -> "$a lebih besar daripada $b, jadi tandanya $a > $b."
                        else -> "$a nilainya sama dengan $b, jadi tandanya $a = $b."
                    }
                )
            }

            3 -> {
                if (id % 2 == 1) {
                    val denom = listOf(3, 4, 5, 6, 8)[random.nextInt(5)]
                    val num = random.nextInt(1, denom)
                    val correct = "$num/$denom"
                    val distractors = mutableSetOf<String>()
                    val candidates = listOf(
                        "${(num % denom) + 1}/$denom",
                        "${(denom - num).coerceAtLeast(1)}/$denom",
                        "$num/${denom + 1}",
                        "$denom/$num"
                    )
                    candidates.forEach { if (it != correct && distractors.size < 3) distractors.add(it) }
                    var extra = 2
                    while (distractors.size < 3) {
                        val c = "$num/${denom + extra}"
                        if (c != correct) distractors.add(c)
                        extra++
                    }
                    val opts = (distractors.toList() + correct).shuffled(random)
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.FRACTION_COMPARE,
                        topic = MathTopic.PECAHAN,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Mengenal Pecahan Visual",
                        prompt = "Perhatikan batang pecahan di bawah ini. Bagian yang berwarna menunjukkan pecahan berapa?",
                        visual = CurriculumVisual.FractionBar(num, denom),
                        options = opts,
                        correctIndex = opts.indexOf(correct),
                        explanation = "Ada $num bagian berwarna dari total $denom bagian sama besar, sehingga pecahannya adalah $num/$denom."
                    )
                } else {
                    val denom = listOf(4, 5, 6, 8)[random.nextInt(4)]
                    val n1 = random.nextInt(1, denom)
                    val n2 = random.nextInt(1, denom)
                    val sym = when {
                        n1 < n2 -> "<"
                        n1 > n2 -> ">"
                        else -> "="
                    }
                    val options = listOf("<", "=", ">")
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.FRACTION_COMPARE,
                        topic = MathTopic.PECAHAN,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Bandingkan Pecahan",
                        prompt = "Bandingkan dua batang pecahan berpenyebut sama ini! Tanda mana yang benar?",
                        visual = CurriculumVisual.FractionBar(n1, denom, n2, denom),
                        options = options,
                        correctIndex = options.indexOf(sym),
                        explanation = "Karena penyebutnya sama ($denom), bandingkan pembilangnya: $n1 $sym $n2, jadi $n1/$denom $sym $n2/$denom."
                    )
                }
            }

            4 -> {
                if (id % 2 == 1) {
                    val basePairs = listOf(
                        Triple(1, 2, 2 to 4),
                        Triple(1, 2, 3 to 6),
                        Triple(1, 3, 2 to 6),
                        Triple(2, 3, 4 to 6),
                        Triple(3, 4, 6 to 8),
                        Triple(2, 5, 4 to 10)
                    )
                    val (n, d, eq) = basePairs[random.nextInt(basePairs.size)]
                    val correct = "${eq.first}/${eq.second}"
                    val distractors = listOf(
                        "${eq.first + 1}/${eq.second}",
                        "${eq.first}/${eq.second + 2}",
                        "${(eq.first - 1).coerceAtLeast(1)}/${eq.second + 1}"
                    ).filter { it != correct }.distinct().take(3)
                    val opts = (distractors + correct).shuffled(random)
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.FRACTION_COMPARE,
                        topic = MathTopic.PECAHAN,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Pecahan Senilai",
                        prompt = "Pecahan manakah yang senilai (sama besar) dengan pecahan $n/$d pada gambar di bawah?",
                        visual = CurriculumVisual.FractionBar(n, d),
                        options = opts,
                        correctIndex = opts.indexOf(correct),
                        explanation = "Kalikan pembilang dan penyebut $n/$d dengan angka yang sama sehingga diperoleh $correct."
                    )
                } else {
                    val pairs = listOf(
                        Pair(1 to 2, 2 to 4),
                        Pair(1 to 2, 2 to 5),
                        Pair(3 to 4, 2 to 3),
                        Pair(1 to 3, 1 to 4),
                        Pair(2 to 3, 4 to 6),
                        Pair(3 to 5, 3 to 4)
                    )
                    val (left, right) = pairs[random.nextInt(pairs.size)]
                    val lhs = left.first * right.second
                    val rhs = right.first * left.second
                    val sym = when {
                        lhs < rhs -> "<"
                        lhs > rhs -> ">"
                        else -> "="
                    }
                    val options = listOf("<", "=", ">")
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.FRACTION_COMPARE,
                        topic = MathTopic.PECAHAN,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Bandingkan Pecahan Beda Penyebut",
                        prompt = "Pilih tanda <, =, atau > yang tepat untuk membandingkan kedua pecahan ini!",
                        visual = CurriculumVisual.FractionBar(left.first, left.second, right.first, right.second),
                        options = options,
                        correctIndex = options.indexOf(sym),
                        explanation = "Perkalian silang: ${left.first}×${right.second}=$lhs dan ${right.first}×${left.second}=$rhs. Jadi ${left.first}/${left.second} $sym ${right.first}/${right.second}."
                    )
                }
            }

            else -> {
                // Kelas 5–6 (Fase C): Konversi Pecahan ↔ Desimal ↔ Persen & Perbandingan
                val triples = listOf(
                    Triple("1/2", "0,5", "50%"),
                    Triple("1/4", "0,25", "25%"),
                    Triple("3/4", "0,75", "75%"),
                    Triple("1/5", "0,2", "20%"),
                    Triple("2/5", "0,4", "40%"),
                    Triple("3/5", "0,6", "60%"),
                    Triple("4/5", "0,8", "80%"),
                    Triple("3/10", "0,3", "30%")
                )
                if (id % 2 == 1) {
                    val chosen = triples[random.nextInt(triples.size)]
                    val askPercent = grade == 6 || random.nextBoolean()
                    val correct = if (askPercent) chosen.third else chosen.second
                    val pool = triples.map { if (askPercent) it.third else it.second }.filter { it != correct }.shuffled(random).take(3)
                    val opts = (pool + correct).shuffled(random)
                    val targetLabel = if (askPercent) "persen (%)" else "bilangan desimal"
                    val parts = chosen.first.split("/")
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.FRACTION_COMPARE,
                        topic = MathTopic.PECAHAN,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = if (askPercent) "Pecahan ke Persen" else "Pecahan ke Desimal",
                        prompt = "Ubahlah pecahan ${chosen.first} menjadi bentuk $targetLabel yang benar!",
                        visual = CurriculumVisual.FractionBar(parts[0].toInt(), parts[1].toInt()),
                        options = opts,
                        correctIndex = opts.indexOf(correct),
                        explanation = "${chosen.first} sama dengan ${chosen.second} atau ${chosen.third}."
                    )
                } else {
                    val idxA = random.nextInt(triples.size)
                    val idxB = random.nextInt(triples.size)
                    val a = triples[idxA]
                    val b = triples[idxB]
                    val valA = a.third.removeSuffix("%").toInt()
                    val valB = b.third.removeSuffix("%").toInt()
                    val sym = when {
                        valA < valB -> "<"
                        valA > valB -> ">"
                        else -> "="
                    }
                    val options = listOf("<", "=", ">")
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.FRACTION_COMPARE,
                        topic = MathTopic.PECAHAN,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Banding Pecahan & Desimal",
                        prompt = "Pilih tanda perbandingan yang tepat antara pecahan dan bentuk desimal/persen berikut:",
                        visual = CurriculumVisual.ComparisonCard(a.first, if (grade == 6) b.third else b.second),
                        options = options,
                        correctIndex = options.indexOf(sym),
                        explanation = "${a.first} = ${a.third}, sedangkan ${if (grade == 6) b.third else b.second} = ${b.third}. Maka tandanya adalah $sym."
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODE 3: GEOMETRI, PENGUKURAN & ANALISIS DATA (Jam, Bangun, Satuan, Diagram)
    // =========================================================================
    private fun generateGeometryOrMeasure(id: Int, grade: Int, random: Random): CurriculumQuestion {
        val phase = phaseTitle(grade)
        return when (grade) {
            1, 2 -> when (id % 3) {
                1 -> {
                    val hour = random.nextInt(1, 12)
                    val minute = if (grade == 1) 0 else listOf(0, 30)[random.nextInt(2)]
                    val correct = String.format("%02d.%02d", hour, minute)
                    val hNext = if (hour == 12) 1 else hour + 1
                    val hPrev = if (hour == 1) 12 else hour - 1
                    val distractors = listOf(
                        String.format("%02d.%02d", hNext, minute),
                        String.format("%02d.%02d", hPrev, minute),
                        String.format("%02d.%02d", hour, if (minute == 0) 30 else 0)
                    ).distinct()
                    val opts = (distractors + correct).shuffled(random)
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.GEOMETRY_MEASURE,
                        topic = MathTopic.GEOMETRI,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Pengukuran Waktu",
                        prompt = "Pukul berapakah yang ditunjukkan oleh jam analog di bawah ini?",
                        visual = CurriculumVisual.ClockFace(hour, minute),
                        options = opts,
                        correctIndex = opts.indexOf(correct),
                        explanation = if (minute == 0) {
                            "Jarum pendek di angka $hour dan jarum panjang di angka 12 menunjukkan pukul $correct."
                        } else {
                            "Jarum pendek lewat angka $hour dan jarum panjang di angka 6 (30 menit) menunjukkan pukul $correct."
                        }
                    )
                }
                2 -> {
                    val shapes = listOf(
                        Triple(ShapeKind.TRIANGLE, "Segitiga", 3),
                        Triple(ShapeKind.SQUARE, "Persegi", 4),
                        Triple(ShapeKind.RECTANGLE, "Persegi Panjang", 4)
                    )
                    val (kind, label, sides) = shapes[random.nextInt(shapes.size)]
                    val opts = listOf("3 sisi", "4 sisi", "5 sisi", "6 sisi")
                    CurriculumQuestion(
                        id = id,
                        mode = ActivityMode.GEOMETRY_MEASURE,
                        topic = MathTopic.GEOMETRI,
                        grade = grade,
                        phaseLabel = phase,
                        competencyTag = "Mengenal Bangun Datar",
                        prompt = "Perhatikan gambar bangun datar $label ini. Ada berapa banyak sisinya?",
                        visual = CurriculumVisual.GeometryDiagram(kind, "", ""),
                        options = opts,
                        correctIndex = opts.indexOf("$sides sisi"),
                        explanation = "Bangun $label memiliki tepat $sides buah sisi lurus."
                    )
                }
                else -> {
                    val meters = random.nextInt(2, 8)
                    val cm = meters * 100
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        "Satuan Panjang",
                        "Panjang pita hias di kelas adalah $meters meter. Jika diubah ke sentimeter (cm), berapa panjangnya?",
                        cm, "cm",
                        "1 meter = 100 cm, maka $meters meter = $meters × 100 = $cm cm.",
                        random
                    )
                }
            }

            3, 4 -> when (id % 3) {
                1 -> {
                    val s = random.nextInt(5, 14)
                    val askArea = grade == 4 && random.nextBoolean()
                    val ans = if (askArea) s * s else 4 * s
                    val unit = if (askArea) "cm²" else "cm"
                    val prompt = if (askArea) {
                        "Hitunglah LUAS bangun persegi dengan panjang sisi $s cm berikut ini!"
                    } else {
                        "Hitunglah KELILING bangun persegi dengan panjang sisi $s cm berikut ini!"
                    }
                    val expl = if (askArea) {
                        "Luas persegi = sisi × sisi = $s × $s = $ans cm²."
                    } else {
                        "Keliling persegi = 4 × sisi = 4 × $s = $ans cm."
                    }
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        if (askArea) "Luas Persegi" else "Keliling Persegi",
                        prompt,
                        ans, unit, expl, random,
                        visual = CurriculumVisual.GeometryDiagram(ShapeKind.SQUARE, "$s cm", "$s cm")
                    )
                }
                2 -> {
                    val p = random.nextInt(7, 15)
                    val l = random.nextInt(3, p - 1)
                    val askArea = grade == 4
                    val ans = if (askArea) p * l else 2 * (p + l)
                    val unit = if (askArea) "cm²" else "cm"
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        if (askArea) "Luas Persegi Panjang" else "Keliling Persegi Panjang",
                        if (askArea) "Berapakah LUAS persegi panjang berukuran $p cm × $l cm di bawah ini?"
                        else "Berapakah KELILING persegi panjang berukuran $p cm × $l cm di bawah ini?",
                        ans, unit,
                        if (askArea) "Luas = panjang × lebar = $p × $l = $ans cm²."
                        else "Keliling = 2 × ($p + $l) = $ans cm.",
                        random,
                        visual = CurriculumVisual.GeometryDiagram(ShapeKind.RECTANGLE, "$p cm", "$l cm")
                    )
                }
                else -> {
                    val hours = random.nextInt(2, 6)
                    val minutes = hours * 60
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        "Konversi Satuan Waktu",
                        "Kegiatan pramuka berlangsung selama $hours jam. Berapa menit lama kegiatan tersebut?",
                        minutes, "menit",
                        "1 jam = 60 menit, jadi $hours jam = $hours × 60 = $minutes menit.",
                        random,
                        visual = CurriculumVisual.ClockFace(hours, 0)
                    )
                }
            }

            else -> when (id % 3) {
                1 -> {
                    val base = random.nextInt(3, 9) * 2
                    val height = random.nextInt(5, 13)
                    val ans = (base * height) / 2
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        "Luas Segitiga",
                        "Hitunglah luas bangun segitiga dengan alas $base cm dan tinggi $height cm berikut!",
                        ans, "cm²",
                        "Luas segitiga = ½ × alas × tinggi = ½ × $base × $height = $ans cm².",
                        random,
                        visual = CurriculumVisual.GeometryDiagram(ShapeKind.TRIANGLE, "t = $height cm", "a = $base cm")
                    )
                }
                2 -> {
                    val s = random.nextInt(4, 10)
                    val vol = s * s * s
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        "Volume Kubus",
                        "Sebuah kotak berbentuk kubus memiliki panjang rusuk $s cm. Berapa volume kotak kubus tersebut?",
                        vol, "cm³",
                        "Volume kubus = s × s × s = $s × $s × $s = $vol cm³.",
                        random,
                        visual = CurriculumVisual.GeometryDiagram(ShapeKind.CUBE, "$s cm", "$s cm", "$s cm")
                    )
                }
                else -> {
                    val a = random.nextInt(4, 10)
                    val b = random.nextInt(5, 12)
                    val c = random.nextInt(6, 14)
                    val d = random.nextInt(4, 10)
                    val bars = listOf("Sen" to a, "Sel" to b, "Rab" to c, "Kam" to d)
                    val total = a + b + c + d
                    buildNumericChoiceQuestion(
                        id, ActivityMode.GEOMETRY_MEASURE, MathTopic.GEOMETRI, grade, phase,
                        "Analisis Data Diagram",
                        "Diagram batang menunjukkan jumlah buku yang dipinjam di perpustakaan selama 4 hari. Berapa TOTAL seluruh buku yang dipinjam?",
                        total, "buku",
                        "Jumlahkan seluruh batang: $a + $b + $c + $d = $total buku.",
                        random,
                        visual = CurriculumVisual.BarChart("Buku Dipinjam per Hari", bars)
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODE 4: POLA BILANGAN & ALJABAR AWAL (Elemen Aljabar Kurikulum Merdeka)
    // =========================================================================
    private fun generatePatternOrAlgebra(id: Int, grade: Int, random: Random): CurriculumQuestion {
        val phase = phaseTitle(grade)
        return when (grade) {
            1, 2 -> {
                if (id % 2 == 1) {
                    val step = if (grade == 1) listOf(2, 5)[random.nextInt(2)] else listOf(3, 4, 5, 10)[random.nextInt(4)]
                    val start = random.nextInt(1, 10)
                    val seq = (0..4).map { start + it * step }
                    val ans = seq[3]
                    val tiles = listOf(seq[0].toString(), seq[1].toString(), seq[2].toString(), "?", seq[4].toString())
                    buildNumericChoiceQuestion(
                        id, ActivityMode.PATTERN_ALGEBRA, MathTopic.POLA_ALJABAR, grade, phase,
                        "Pola Bilangan Loncat",
                        "Amati pola bilangan loncat di bawah ini. Angka berapakah yang tepat untuk mengisi kotak tanda tanya (?)?",
                        ans, "",
                        "Polanya bertambah +$step setiap langkah: ${seq[2]} + $step = $ans.",
                        random,
                        visual = CurriculumVisual.PatternSequence(tiles)
                    )
                } else {
                    val a = random.nextInt(4, if (grade == 1) 11 else 25)
                    val missing = random.nextInt(3, if (grade == 1) 10 else 20)
                    val total = a + missing
                    buildNumericChoiceQuestion(
                        id, ActivityMode.PATTERN_ALGEBRA, MathTopic.POLA_ALJABAR, grade, phase,
                        "Kalimat Matematika Terbuka",
                        "Carilah angka yang hilang pada kotak misteri: $a + □ = $total",
                        missing, "",
                        "Untuk mencari □, kurangi hasil dengan angka awal: $total − $a = $missing.",
                        random,
                        visual = CurriculumVisual.PatternSequence(listOf("$a", "+", "?", "=", "$total"))
                    )
                }
            }

            3, 4 -> {
                if (id % 2 == 1) {
                    val step = random.nextInt(6, 15)
                    val start = random.nextInt(12, 45)
                    val seq = (0..4).map { start + it * step }
                    val ans = seq[4]
                    val tiles = listOf(seq[0].toString(), seq[1].toString(), seq[2].toString(), seq[3].toString(), "?")
                    buildNumericChoiceQuestion(
                        id, ActivityMode.PATTERN_ALGEBRA, MathTopic.POLA_ALJABAR, grade, phase,
                        "Pola Bilangan Membesar",
                        "Lanjutkan pola bilangan berikut ini! Angka ke-5 yang tepat adalah...",
                        ans, "",
                        "Selisih antar angka selalu +$step, sehingga ${seq[3]} + $step = $ans.",
                        random,
                        visual = CurriculumVisual.PatternSequence(tiles)
                    )
                } else {
                    val factor = random.nextInt(4, 10)
                    val missing = random.nextInt(5, 13)
                    val prod = factor * missing
                    buildNumericChoiceQuestion(
                        id, ActivityMode.PATTERN_ALGEBRA, MathTopic.POLA_ALJABAR, grade, phase,
                        "Aljabar Perkalian",
                        "Tentukan bilangan yang tepat untuk mengganti kotak misteri: □ × $factor = $prod",
                        missing, "",
                        "Gunakan kebalikan perkalian (pembagian): $prod ÷ $factor = $missing.",
                        random,
                        visual = CurriculumVisual.PatternSequence(listOf("?", "×", "$factor", "=", "$prod"))
                    )
                }
            }

            else -> {
                if (id % 2 == 1) {
                    // Increasing difference pattern: +2, +4, +6, +8 or square numbers
                    val base = random.nextInt(2, 6)
                    val seq = listOf(
                        base,
                        base + 3,
                        base + 8,
                        base + 15,
                        base + 24
                    )
                    val ans = seq[4]
                    val tiles = listOf(seq[0].toString(), seq[1].toString(), seq[2].toString(), seq[3].toString(), "?")
                    buildNumericChoiceQuestion(
                        id, ActivityMode.PATTERN_ALGEBRA, MathTopic.POLA_ALJABAR, grade, phase,
                        "Pola Bilangan Bertingkat",
                        "Temukan aturan pola bertingkat di bawah ini, lalu tentukan angka yang tepat untuk kotak (?)!",
                        ans, "",
                        "Penambahannya naik +3, +5, +7, lalu +9! Jadi ${seq[3]} + 9 = $ans.",
                        random,
                        visual = CurriculumVisual.PatternSequence(tiles)
                    )
                } else {
                    val m = random.nextInt(2, 6)
                    val x = random.nextInt(4, 12)
                    val c = random.nextInt(3, 15)
                    val rhs = m * x + c
                    buildNumericChoiceQuestion(
                        id, ActivityMode.PATTERN_ALGEBRA, MathTopic.POLA_ALJABAR, grade, phase,
                        "Persamaan Aljabar Awal",
                        "Berapakah nilai □ yang memenuhi kalimat matematika: ($m × □) + $c = $rhs?",
                        x, "",
                        "Kurangi dulu $rhs − $c = ${m * x}, lalu bagi dengan $m: ${m * x} ÷ $m = $x.",
                        random,
                        visual = CurriculumVisual.PatternSequence(listOf("$m×?", "+", "$c", "=", "$rhs"))
                    )
                }
            }
        }
    }

    private fun buildNumericChoiceQuestion(
        id: Int,
        mode: ActivityMode,
        topic: MathTopic,
        grade: Int,
        phase: String,
        competencyTag: String,
        prompt: String,
        answer: Int,
        unit: String,
        explanation: String,
        random: Random,
        visual: CurriculumVisual = CurriculumVisual.None
    ): CurriculumQuestion {
        val distractors = mutableSetOf<Int>()
        val stepScale = when {
            answer >= 100 -> listOf(-20, -10, -5, 5, 10, 20, 15, -15)
            answer >= 20 -> listOf(-6, -4, -2, -1, 1, 2, 4, 6, 10)
            else -> listOf(-3, -2, -1, 1, 2, 3, 4)
        }
        var attempts = 0
        while (distractors.size < 3 && attempts < 40) {
            attempts++
            val candidate = answer + stepScale[random.nextInt(stepScale.size)]
            if (candidate > 0 && candidate != answer) distractors.add(candidate)
        }
        var fallback = 1
        while (distractors.size < 3) {
            val c = abs(answer + fallback)
            if (c != answer && c > 0) distractors.add(c)
            fallback++
        }

        val suffix = if (unit.isNotBlank()) " $unit" else ""
        val allInts = (distractors.toList() + answer).shuffled(random)
        val options = allInts.map { "$it$suffix" }
        return CurriculumQuestion(
            id = id,
            mode = mode,
            topic = topic,
            grade = grade,
            phaseLabel = phase,
            competencyTag = competencyTag,
            prompt = prompt,
            visual = visual,
            options = options,
            correctIndex = allInts.indexOf(answer),
            explanation = explanation
        )
    }

    private fun buildRupiahChoiceQuestion(
        id: Int,
        mode: ActivityMode,
        topic: MathTopic,
        grade: Int,
        phase: String,
        competencyTag: String,
        prompt: String,
        answer: Int,
        explanation: String,
        random: Random
    ): CurriculumQuestion {
        val step = if (answer >= 20000) 5000 else 1000
        val distractors = mutableSetOf<Int>()
        val deltas = listOf(-2, -1, 1, 2, 3, -3)
        for (d in deltas.shuffled(random)) {
            val c = answer + d * step
            if (c > 0 && c != answer && distractors.size < 3) distractors.add(c)
        }
        var f = 1
        while (distractors.size < 3) {
            val c = answer + f * step
            if (c != answer) distractors.add(c)
            f++
        }
        val allVals = (distractors.toList() + answer).shuffled(random)
        val options = allVals.map { "Rp${formatIdr(it)}" }
        return CurriculumQuestion(
            id = id,
            mode = mode,
            topic = topic,
            grade = grade,
            phaseLabel = phase,
            competencyTag = competencyTag,
            prompt = prompt,
            options = options,
            correctIndex = allVals.indexOf(answer),
            explanation = explanation
        )
    }

    private fun formatIdr(amount: Int): String {
        val str = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            sb.append(str[i])
            count++
            if (count % 3 == 0 && i > 0) sb.append('.')
        }
        return sb.reverse().toString()
    }
}
