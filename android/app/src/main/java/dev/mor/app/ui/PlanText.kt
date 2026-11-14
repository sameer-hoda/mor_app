package dev.mor.app.ui

/** v4 #12: every plan string is built from the saved goal and diet values
 *  through these mappings — no hard-coded diet or goal words anywhere else.
 *
 *  Saved values (backend/src/routes.js + v2CommitMap):
 *  - goal: lose / maintain / gain / healthy
 *  - diet: vegan / veg / egg / non ("non" is the backend's id for non-veg;
 *    "nonveg" is the onboarding option key — accept both).
 */

fun dietAdj(diet: String?): String = when (diet?.lowercase()) {
    "vegan" -> "vegan"
    "veg" -> "vegetarian"
    "egg" -> "eggetarian"
    "non", "nonveg" -> "non-veg"
    else -> "vegetarian"
}

fun dietDisplay(diet: String?): String = when (diet?.lowercase()) {
    "vegan" -> "Vegan"
    "veg" -> "Vegetarian"
    "egg" -> "Eggetarian"
    "non", "nonveg" -> "Non-veg"
    else -> "—"
}

fun goalReading(goal: String?): String = when (goal?.lowercase()) {
    "lose" -> "Reading your weight-loss goal"
    "maintain" -> "Reading your maintain-weight goal"
    "gain" -> "Reading your weight-gain goal"
    "healthy" -> "Reading your eat-healthier goal"
    else -> "Reading your eat-healthier goal"
}

fun goalSubtitle(goal: String?): String = when (goal?.lowercase()) {
    "lose" -> "weight-loss"
    "maintain" -> "maintenance"
    "gain" -> "weight-gain"
    "healthy" -> "eat-healthier"
    else -> "eat-healthier"
}

fun goalDisplay(goal: String?): String = when (goal?.lowercase()) {
    "lose" -> "Lose weight"
    "maintain" -> "Maintain weight"
    "gain" -> "Gain weight"
    "healthy" -> "Eat healthier"
    else -> ""
}

fun planSubtitle(diet: String?, goal: String?): String =
    "Balancing your ${dietAdj(diet)} meals and ${goalSubtitle(goal)} goal."

fun planChecklist(diet: String?, goal: String?): List<String> = listOf(
    goalReading(goal),
    "Matching ${dietAdj(diet)} Indian meals",
    "Setting your daily calories",
)
