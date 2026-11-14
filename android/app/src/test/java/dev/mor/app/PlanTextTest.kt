package dev.mor.app

import dev.mor.app.ui.dietAdj
import dev.mor.app.ui.dietDisplay
import dev.mor.app.ui.goalReading
import dev.mor.app.ui.goalSubtitle
import dev.mor.app.ui.planChecklist
import dev.mor.app.ui.planSubtitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v4 #12: every goal x diet combination must produce the spec'd subtitle and
 *  the three checklist lines. */
class PlanTextTest {

    private val goals = listOf("lose", "maintain", "gain", "healthy")
    private val diets = listOf("vegan", "veg", "egg", "non")

    @Test
    fun `all 16 goal x diet combinations`() {
        for (g in goals) for (d in diets) {
            val sub = planSubtitle(d, g)
            val lines = planChecklist(d, g)

            assertEquals(
                "Balancing your ${dietAdj(d)} meals and ${goalSubtitle(g)} goal.",
                sub,
            )
            assertEquals(
                listOf(
                    goalReading(g),
                    "Matching ${dietAdj(d)} Indian meals",
                    "Setting your daily calories",
                ),
                lines,
            )
            assertEquals(3, lines.size)
        }
    }

    @Test
    fun `non saved value maps to non-veg in both spellings`() {
        assertEquals("non-veg", dietAdj("non"))
        assertEquals("non-veg", dietAdj("nonveg"))
        assertEquals("Non-veg", dietDisplay("non"))
        assertEquals("Non-veg", dietDisplay("nonveg"))
    }

    @Test
    fun `the walkthrough case - non-veg plus eat healthier`() {
        assertEquals(
            "Balancing your non-veg meals and eat-healthier goal.",
            planSubtitle("non", "healthy"),
        )
        assertEquals("Reading your eat-healthier goal", goalReading("healthy"))
        assertEquals("Matching non-veg Indian meals", planChecklist("non", "healthy")[1])
        assertEquals("Non-veg", dietDisplay("non"))
    }

    @Test
    fun `maintain uses maintain-weight in the reading line and maintenance in the subtitle`() {
        assertEquals("Reading your maintain-weight goal", goalReading("maintain"))
        assertEquals("maintenance", goalSubtitle("maintain"))
        assertEquals(
            "Balancing your vegetarian meals and maintenance goal.",
            planSubtitle("veg", "maintain"),
        )
    }

    @Test
    fun `diet adjectives and display names`() {
        assertEquals("vegan", dietAdj("vegan"))
        assertEquals("vegetarian", dietAdj("veg"))
        assertEquals("eggetarian", dietAdj("egg"))
        assertEquals("Vegan", dietDisplay("vegan"))
        assertEquals("Vegetarian", dietDisplay("veg"))
        assertEquals("Eggetarian", dietDisplay("egg"))
    }

    @Test
    fun `subtitle is always three parts`() {
        for (g in goals) for (d in diets) {
            val sub = planSubtitle(d, g)
            assertTrue(sub.startsWith("Balancing your "))
            assertTrue(sub.contains(" meals and "))
            assertTrue(sub.endsWith(" goal."))
        }
    }
}
