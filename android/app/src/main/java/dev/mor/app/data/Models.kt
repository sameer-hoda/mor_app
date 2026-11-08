package dev.mor.app.data

import com.google.gson.annotations.SerializedName

/** Mirrors backend/src/routes.js JSON shapes. All fields tolerant: the app must
 *  never crash on a missing or mistyped field. */

data class Plan(
    val kcal_target: Int = 2000,
    val protein_g: Double = 75.0,
    val carbs_g: Double = 250.0,
    val fat_g: Double = 60.0,
    val goal_line: String? = null,
    val target_date: String? = null,
)

data class Profile(
    val id: Int = 1,
    val name: String? = null,
    val goal: String? = null,
    val diet: String? = null,
    val sex: String? = null,
    val age: Int? = null,
    val height_cm: Double? = null,
    val weight_kg: Double? = null,
    val target_weight_kg: Double? = null,
    val activity: String? = null,
    val units: String? = null,
    val meal_times: Map<String, String>? = null,
    val reminders_on: Int? = null,
    val onboarded: Int? = null,
    val coins: Int = 0,
    val plan: Plan? = null,
)

data class FoodItem(
    val name: String = "",
    val kcal: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val portion: String? = null,
    val confidence: Double? = null,
    val unsure: Boolean? = null,
    val alternatives: List<String>? = null,
    val idx: Int? = null,
    val veg: String? = null,
    val verified: Int? = null,
    val source: String? = null,
)

data class SlotMeal(
    val logged: Boolean = false,
    val open: Boolean = false,
    val id: Long? = null,
    val name: String? = null,
    val kcal: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val items: List<FoodItem>? = null,
    val time: String? = null,
    val source: String? = null,
)

data class MacroSet(
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
)

data class Macros(
    val eaten: MacroSet = MacroSet(),
    val goal: MacroSet = MacroSet(),
)

data class Home(
    val date: String = "",
    val state: String = "default",
    val greeting: String = "Hello",
    val kcal_target: Double = 2000.0,
    val kcal_eaten: Double = 0.0,
    val kcal_left: Double = 2000.0,
    val macros: Macros = Macros(),
    val streak: Int = 0,
    val coins: Int = 0,
    val water: Int = 0,
    val water_goal: Int = 8,
    val steps: Int = 0,
    val weight_kg: Double? = null,
    val meals: Map<String, SlotMeal> = emptyMap(),
    val unread: Int = 0,
)

data class ScanResult(
    val scan_id: Long = 0,
    val status: String = "",
    val meal_name: String? = null,
    val items: List<FoodItem>? = null,
    val totals: FoodItem? = null,
    val raw_csv: String? = null,
    val model: String? = null,
    val image_url: String? = null,
)

data class ScanDetail(
    val id: Long = 0,
    val status: String = "",
    val meal_name: String? = null,
    val items: List<FoodItem>? = null,
    val image_path: String? = null,
)

data class MealLog(
    val id: Long = 0,
    val slot: String = "",
    val date: String = "",
    val name: String = "",
    val kcal: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val items: List<FoodItem>? = null,
    val coins_awarded: Int = 0,
    val coins: Int = 0,
    val streak: Int = 0,
)

data class MealDetail(
    val id: Long = 0,
    val slot: String = "",
    val date: String = "",
    val time: String? = null,
    val name: String = "",
    val kcal: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val items: List<FoodItem> = emptyList(),
    val scan_id: Long? = null,
    val source: String? = null,
    val scan: ScanDetail? = null,
)

data class DayKcal(
    val date: String = "",
    val kcal: Double = 0.0,
    val on_track: Boolean = false,
    val weekday: String = "",
)

data class WeightPt(
    val date: String = "",
    val kg: Double = 0.0,
)

data class Insights(
    val kcal_target: Double = 2000.0,
    val days: List<DayKcal> = emptyList(),
    val avg: Int = 0,
    val logged_days: Int = 0,
    val on_track_days: Int = 0,
    val weights: List<WeightPt> = emptyList(),
    val weight_trend: Double = 0.0,
    val protein_today: Double = 0.0,
    val protein_goal: Double = 75.0,
)

data class StreakDay(
    val date: String = "",
    val lit: Boolean = false,
    val future: Boolean = false,
    val today: Boolean = false,
)

data class StreakInfo(
    val streak: Int = 0,
    val best: Int = 0,
    val freezes: Int = 2,
    val days: List<StreakDay> = emptyList(),
    val first_weekday: Int = 0,
)

data class Challenge(
    val key: String = "",
    val scope: String = "",
    val title: String = "",
    val target: Double = 1.0,
    val coins: Int = 0,
    val progress: Double = 0.0,
    val done: Boolean = false,
    val claimed: Boolean = false,
)

data class Challenges(
    val coins: Int = 0,
    val challenges: List<Challenge> = emptyList(),
    val badges_unlocked: Int = 0,
)

data class Notif(
    val id: Long = 0,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val today: Boolean = false,
    val created_at: String = "",
)

data class Health(val ok: Boolean = false, val model: String? = null, val key: Boolean = false)

data class ClaimResp(val ok: Boolean = false, val coins: Int = 0, val awarded: Int = 0, val already: Boolean? = null)
data class WaterResp(val glasses: Int = 0)
data class WeightResp(val kg: Double = 0.0, val delta: Double = 0.0)
data class FoodsResp(val items: List<FoodItem> = emptyList())
data class NotifsResp(val items: List<Notif> = emptyList())
data class EstimateResp(val item: FoodItem = FoodItem(), val raw_csv: String? = null)
data class OkResp(val ok: Boolean = false)
