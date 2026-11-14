package dev.mor.app

import dev.mor.app.data.FoodItem
import dev.mor.app.ui.dietOk
import dev.mor.app.ui.isGoodMatch
import dev.mor.app.ui.jaroWinkler
import dev.mor.app.ui.normalizeFoodName
import dev.mor.app.ui.pickMatch
import dev.mor.app.ui.searchQueries
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v4 #10: the good-match rule for the voice → search hand-off. */
class FoodMatchTest {

    private fun f(name: String, kcal: Double = 100.0, veg: String = "veg") =
        FoodItem(name = name, kcal = kcal, veg = veg, verified = 1)

    /** Mimics the backend: rows whose name contains the query (plus aliases). */
    private fun likeSearch(vararg all: FoodItem): suspend (String) -> List<FoodItem> = { q ->
        val nq = normalizeFoodName(q)
        all.filter { normalizeFoodName(it.name).contains(nq) || nq in listOf("roti", "dal", "channa", "chana", "idly", "momo", "golgappa", "wada") && it.name == aliasTarget(nq) }
    }

    private fun aliasTarget(q: String): String = when (q) {
        "roti" -> "Phulka roti"
        "dal" -> "Dal tadka"
        "channa", "chana" -> "Chole"
        "idly" -> "Idli"
        "momo" -> "Veg momos"
        "golgappa" -> "Pani puri"
        "wada" -> "Medu vada"
        else -> q
    }

    @Test
    fun `paneer resolves to the shortest paneer dish`() = runTest {
        val res = pickMatch(
            likeSearch(
                f("Chili paneer"), f("Paneer tikka", 260.0), f("Paneer bhurji", 220.0),
                f("Paneer butter masala", 380.0), f("Shahi paneer", 350.0),
            ),
            "paneer", "veg", emptySet(),
        )
        assertEquals("Paneer tikka", res?.name)
    }

    @Test
    fun `pulka resolves to Phulka roti through the hinglish synonym`() = runTest {
        val res = pickMatch(likeSearch(f("Phulka roti", 60.0)), "pulka", "veg", emptySet())
        assertEquals("Phulka roti", res?.name)
    }

    @Test
    fun `do roti aur dal - roti and dal each resolve`() = runTest {
        val search = likeSearch(
            f("Phulka roti", 60.0), f("Tandoori roti", 90.0), f("Dal tadka", 150.0),
            f("Dal fry", 160.0), f("Dal makhani", 300.0),
        )
        assertEquals("Phulka roti", pickMatch(search, "Roti", "veg", emptySet())?.name)
        assertEquals("Dal tadka", pickMatch(search, "Dal", "veg", setOf(normalizeFoodName("Dal tadka")))?.name)
    }

    @Test
    fun `logged-before food wins over the shortest name`() = runTest {
        val res = pickMatch(
            likeSearch(f("Paneer tikka", 260.0), f("Paneer bhurji", 220.0)),
            "paneer", "veg", setOf(normalizeFoodName("Paneer bhurji")),
        )
        assertEquals("Paneer bhurji", res?.name)
    }

    @Test
    fun `diet preference - a veg user never gets a non-veg match`() = runTest {
        val res = pickMatch(
            likeSearch(f("Paneer tikka", veg = "veg"), f("Paneer curry", veg = "non")),
            "paneer", "veg", emptySet(),
        )
        assertEquals("Paneer tikka", res?.name)
    }

    @Test
    fun `gibberish never auto-matches`() = runTest {
        val res = pickMatch(
            likeSearch(f("Aam panna"), f("Alcoholic Beverage, wine, table, red, Barbera")),
            "xyzzy blurp", "non", emptySet(),
        )
        assertNull(res)
    }

    @Test
    fun `no item ever resolves to a row from Recent`() = runTest {
        // The search result list is the Recent dump; nothing in it is a good
        // match for the detected food, so the result must be null, never row 0.
        val recentDump = listOf(
            f("Alcoholic Beverage, wine, table, red, Barbera"),
            f("Aam panna"),
            f("Acerola"),
        )
        val res = pickMatch({ _ -> recentDump }, "paneer", "non", emptySet())
        assertNull(res)
    }

    @Test
    fun `good match - equality, prefix and jaro-winkler paths`() {
        assertTrue(isGoodMatch("paneer", normalizeFoodName("Paneer tikka")))
        assertTrue(isGoodMatch("paneer tikka", normalizeFoodName("Paneer tikka")))
        assertTrue(isGoodMatch("pulka", normalizeFoodName("Phulka")))
        assertFalse(isGoodMatch("pulka", normalizeFoodName("Paneer tikka")))
        assertFalse(isGoodMatch("paneer", normalizeFoodName("Chili paneer")))
        assertFalse(isGoodMatch("", normalizeFoodName("Paneer")))
    }

    @Test
    fun `jaro-winkler threshold is 0_88`() {
        assertEquals(1.0, jaroWinkler("same", "same"), 1e-9)
        assertEquals(0.0, jaroWinkler("abc", "xyz"), 1e-9)
        assertTrue(jaroWinkler("pulka", "phulka") >= 0.88)
        assertTrue(jaroWinkler("paneer", "chili paneer") < 0.88)
    }

    @Test
    fun `diet filter rules`() {
        assertTrue(dietOk("non", "non"))
        assertTrue(dietOk("veg", "non"))
        assertFalse(dietOk("non", "veg"))
        assertTrue(dietOk("veg", "vegan"))
        assertFalse(dietOk("egg", "vegan"))
        assertTrue(dietOk("egg", "egg"))
        assertTrue(dietOk("veg", "egg"))
        assertTrue(dietOk(null, "non"))
    }

    @Test
    fun `search queries include the synonym`() {
        assertEquals(listOf("pulka", "phulka roti"), searchQueries("pulka"))
        assertEquals(listOf("Paneer tikka"), searchQueries("Paneer tikka"))
        assertTrue(searchQueries("").isEmpty())
    }
}
