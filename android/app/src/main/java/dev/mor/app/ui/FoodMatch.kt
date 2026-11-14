package dev.mor.app.ui

import dev.mor.app.data.FoodItem
import java.text.Normalizer
import java.util.Locale

/** v4 #10: the deterministic voice → search match rule.
 *
 *  A result is a "good match" when its normalised name equals or starts with
 *  the normalised query, or its Jaro-Winkler similarity to the query is
 *  >= 0.88. Among good matches, prefer:
 *  1. foods the user logged before
 *  2. foods matching the user's diet
 *  3. the shortest name
 *
 *  Never selects by list index; never falls back to Recent, Saved or row 0.
 */

fun normalizeFoodName(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

fun jaroWinkler(a: String, b: String): Double {
    if (a == b) return 1.0
    val maxLen = maxOf(a.length, b.length)
    if (maxLen == 0) return 1.0
    val window = maxOf(0, maxLen / 2 - 1)
    val aMatch = BooleanArray(a.length)
    val bMatch = BooleanArray(b.length)
    var matches = 0
    for (i in a.indices) {
        val lo = maxOf(0, i - window)
        val hi = minOf(b.length - 1, i + window)
        for (j in lo..hi) {
            if (!bMatch[j] && a[i] == b[j]) {
                aMatch[i] = true
                bMatch[j] = true
                matches++
                break
            }
        }
    }
    if (matches == 0) return 0.0
    var transpositions = 0.0
    var k = 0
    for (i in a.indices) {
        if (!aMatch[i]) continue
        while (!bMatch[k]) k++
        if (a[i] != b[k]) transpositions += 0.5
        k++
    }
    val jaro = (
        matches / a.length.toDouble() +
            matches / b.length.toDouble() +
            (matches - transpositions) / matches
        ) / 3.0
    var prefix = 0
    val pmax = minOf(4, a.length, b.length)
    while (prefix < pmax && a[prefix] == b[prefix]) prefix++
    return jaro + prefix * 0.1 * (1.0 - jaro)
}

fun isGoodMatch(queryNorm: String, nameNorm: String): Boolean =
    queryNorm.isNotEmpty() &&
        (nameNorm == queryNorm || nameNorm.startsWith(queryNorm) || jaroWinkler(queryNorm, nameNorm) >= 0.88)

/** Hinglish synonyms: the detected word plus the standard dish it usually
 *  means (the backend's food_aliases cover the search itself; these make the
 *  good-match rule pass on the alias target). */
private val SYNONYMS = mapOf(
    "pulka" to "phulka roti",
    "roti" to "phulka roti",
    "dal" to "dal tadka",
    "channa" to "chole",
    "chana" to "chole",
    "idly" to "idli",
    "momo" to "veg momos",
    "golgappa" to "pani puri",
    "wada" to "medu vada",
)

fun searchQueries(detected: String): List<String> {
    val q = detected.trim()
    if (q.isEmpty()) return emptyList()
    val out = mutableListOf(q)
    SYNONYMS[q.lowercase(Locale.ROOT)]?.let { out.add(it) }
    return out.distinctBy { normalizeFoodName(it) }
}

/** veg values in the food DB: 'veg' / 'non' / 'egg'. */
fun dietOk(veg: String?, diet: String?): Boolean = when (diet?.lowercase()) {
    "veg" -> veg == "veg"
    "vegan" -> veg == "veg"
    "egg" -> veg == "veg" || veg == "egg"
    else -> true
}

/** Runs the normal food search for each query (the name, then its synonym)
 *  and returns the top good match — or null, so the UI can show "Find '…'".
 *  The search function is injected so the rule is unit-testable. */
suspend fun pickMatch(
    search: suspend (String) -> List<FoodItem>,
    detected: String,
    diet: String?,
    recentNorm: Set<String>,
): FoodItem? {
    for (query in searchQueries(detected)) {
        val qn = normalizeFoodName(query)
        val res = runCatching { search(query) }.getOrDefault(emptyList())
        val good = res.filter { isGoodMatch(qn, normalizeFoodName(it.name)) }
        if (good.isNotEmpty()) {
            return good.sortedWith(
                compareByDescending<FoodItem> { normalizeFoodName(it.name) in recentNorm }
                    .thenByDescending { dietOk(it.veg, diet) }
                    .thenBy { it.name.length },
            ).first()
        }
    }
    return null
}
