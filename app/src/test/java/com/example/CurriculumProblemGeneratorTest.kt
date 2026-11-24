package com.example

import com.example.data.ActivityMode
import com.example.data.CurriculumProblemGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CurriculumProblemGeneratorTest {

    @Test
    fun allCurriculumModesProduceValidQuestionsAcrossGrades1To6() {
        val modes = listOf(
            ActivityMode.STORY_PROBLEM,
            ActivityMode.FRACTION_COMPARE,
            ActivityMode.GEOMETRY_MEASURE,
            ActivityMode.PATTERN_ALGEBRA
        )
        val rng = Random(42)

        for (grade in 1..6) {
            for (mode in modes) {
                val questions = CurriculumProblemGenerator.generateSet(
                    mode = mode,
                    grade = grade,
                    count = 10,
                    random = rng
                )
                assertEquals(10, questions.size)
                questions.forEach { q ->
                    assertTrue("Prompt empty for $mode grade $grade", q.prompt.isNotBlank())
                    assertTrue("Explanation empty for $mode grade $grade", q.explanation.isNotBlank())
                    assertTrue(
                        "Options should have 3 or 4 items for $mode grade $grade",
                        q.options.size in 3..4
                    )
                    assertEquals(
                        "Duplicate options found for $mode grade $grade: ${q.options}",
                        q.options.size,
                        q.options.distinct().size
                    )
                    assertTrue(
                        "Invalid correctIndex ${q.correctIndex} for $mode grade $grade",
                        q.correctIndex in q.options.indices
                    )
                }
            }
        }
    }
}
