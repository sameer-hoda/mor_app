# Mor Next Version — 01 Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Mor's onboarding (intro pacing, demographic-first order, conditional target, peel transition, cascade plan build) to the approved pack specs in an isolated next-version app.

**Architecture:** Same Compose app, separate `applicationId` (`dev.mor.app.next`) and separate backend instance/DB so it installs and runs beside the live app. Specs win over existing code; `reference-code/mor-chosen.css` wins over specs on disagreement. Assets come verbatim from the pack.

**Tech Stack:** Kotlin, Jetpack Compose, ExoPlayer (media3), Node/Express + better-sqlite3, ffmpeg for asset retiming.

## Global Constraints

- Source pack: `/Users/sameerhoda/Downloads/Mor next version - implementation pack (2026-11-18)`
- Work only on branch `mor-next`. Never deploy. Never touch the live app's backend/DB.
- `applicationId = "dev.mor.app.next"`, `versionName = "0.2-next"`, `versionCode = 1`.
- Backend: `PORT=4001`, `MOR_DB=backend/data/mor-next.db`, `MOR_UPLOADS=backend/uploads-next`.
- Do NOT ship `assets/intro/door.mp4` or `assets/intro/reveal.mp4` (timing references only).
- Palette: cream `#FAF4E6`, screenCream `#F6F1E5`, frameCream `#F4EEDF`, card `#FFFDF6`, teal `#14524A`, tealMid `#1D6F64`, maroon `#7B1E26`, gold `#C9A227`, goldSoft `#E4C95C`, goldText `#A8963F`, ink `#2B2118`, inkSoft `#8A7F68`, faint `#C9BFA4`, noteFaint `#B9AE93`, sand `#E8DFC8`, track `#EFE8D3`, protein blue `#3E5F8A`.
- Motion: peel 0.55s cubic-bezier(0.45,0,0.65,0.4); cascade 0.55s stagger; count-up 900ms cubic-out; ring sweeps 1.0–1.2s, +0.18s stagger.
- Macros = protein, carbs, fat only. All AI figures labeled estimates. All demo figures labeled illustrative.

---

### Task 0: Isolation, assets, and a runnable side-by-side app

**Files:**
- Modify: `android/app/build.gradle.kts` (applicationId/versionName)
- Modify: `backend/src/index.js` (PORT/DB/uploads env)
- Create: `android/app/src/main/res/drawable-nodpi/onb_header.png`, `court.png`, `neelu_cut.png`, `neelu_fan.png`
- Create: `docs/superpowers/plans/` (this file)

**Interfaces:**
- Produces: package `dev.mor.app.next` installable next to `dev.mor.app`; backend at `http://10.0.2.2:4001`.

- [ ] **Step 1: Point the build at a new app id**

```kotlin
applicationId = "dev.mor.app.next"
versionCode = 1
versionName = "0.2-next"
```

- [ ] **Step 2: Backend reads its own port/DB/uploads**

```js
const PORT = Number(process.env.PORT || 4001);
const DB_FILE = process.env.MOR_DB || path.join(__dirname, '..', 'data', 'mor-next.db');
const UPLOADS = process.env.MOR_UPLOADS || path.join(__dirname, '..', 'uploads-next');
```

- [ ] **Step 3: Copy ship artwork verbatim**

```bash
P="/Users/sameerhoda/Downloads/Mor next version - implementation pack (2026-11-18)"
cp "$P/assets/onb_header.png" "$P/assets/court.png" "$P/assets/neelu_cut.png" "$P/assets/neelu_fan.png" android/app/src/main/res/drawable-nodpi/
```

- [ ] **Step 4: Verify isolation**

Run: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk` with the old app installed.
Expected: both `dev.mor.app` and `dev.mor.app.next` listed in `adb shell pm list packages | grep dev.mor`; old app's data untouched.

---

### Task 1: Intro door at 1.7x, reveal untouched

**Files:**
- Modify: `android/app/src/main/java/dev/mor/app/ui/WelcomeJourney.kt` (door player speed)
- Re-encode: `android/app/src/main/res/raw/journey_c1.mp4` (trim closed-door hold; keep duration ~2.1s)

**Interfaces:**
- Consumes: current stage chain 0→1→2→3→4→5.
- Produces: door phase ≈2s; reveal phase unchanged; hard cut between them.

- [ ] **Step 1: Set door playback speed**

```kotlin
// door phase only; reveal stays 1.0f
player.setPlaybackSpeed(1.7f)
```

- [ ] **Step 2: Trim the hold, then apply the speed at source**

```bash
cd android/app/src/main/res/raw
# drop the leading closed-door hold (~1.0s), keep the remaining door motion
ffmpeg -y -ss 1.0 -i journey_c1.mp4 -an -c:v libx264 -pix_fmt yuv420p /tmp/door_trim.mp4 && mv /tmp/door_trim.mp4 journey_c1.mp4
```

- [ ] **Step 3: Verify pacing**

Record the intro. Expected: doors open in ~2.1s; reveal starts the same frame; total door+reveal matches the reference clips' pacing (A10).

---

### Task 2: Demographic-first order, conditional target

**Files:**
- Modify: `android/app/src/main/java/dev/mor/app/ui/V2Onboarding.kt` (`V2_ORDER`, `go()`)

**Interfaces:**
- Produces: `V2_ORDER = journey, body, goal, target, diet, activity, meals, building, plan, allset`; `go(+1)` skips `target` unless `answers.goal in {"reduce","gain"}`.

- [ ] **Step 1: Reorder**

```kotlin
private val V2_ORDER = listOf(
    "journey", "body", "goal", "target", "diet", "activity", "meals", "building", "plan", "allset",
)
```

- [ ] **Step 2: Skip target for maintain/eat-healthier**

```kotlin
fun nextKey(from: String, d: Int): String {
    var i = V2_ORDER.indexOf(from) + d
    while (i in V2_ORDER.indices) {
        val k = V2_ORDER[i]
        if (k == "target" && draft.answers.goal !in setOf("reduce", "gain")) { i += d; continue }
        return k
    }
    return V2_ORDER[if (d > 0) V2_ORDER.lastIndex else 0]
}
```

- [ ] **Step 3: Verify**

Run the flow twice (reduce, maintain). Expected: reduce sees About → Objective → Target → Diet; maintain sees About → Objective → Diet; the step badge shows m-1 segments for maintain.

**Interpretation to record:** S2 lists four steps, but the app also collects Activity and Meals-per-day (needed by TDEE/plan). This plan keeps them after Diet; the badge counts all steps. Flag in the final report.

---

### Task 3: Fixed fan header, badge, progress, tip bubble, card peel

**Files:**
- Create: `android/app/src/main/java/dev/mor/app/ui/OnbStepChrome.kt`
- Modify: `android/app/src/main/java/dev/mor/app/ui/OnboardingScaffold.kt`
- Modify: `android/app/src/main/java/dev/mor/app/ui/V2Onboarding.kt` (replace AnimatedContent with a peeling stack)

**Interfaces:**
- Produces: `@Composable OnbStepChrome(step:Int, total:Int, tip:String, title:String, content:@Composable ColumnScope.()->Unit)`; `Modifier.peelOut(progress:Float)`.

- [ ] **Step 1: Chrome**

```kotlin
Column {
    Image(painterResource(R.drawable.onb_header), null, Modifier.fillMaxWidth().height(92.dp), contentScale = ContentScale.Crop)
    Text("STEP ${step} OF ${total}", fontSize = 8.5.sp, letterSpacing = 1.5.sp, color = GoldText, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i -> Box(Modifier.weight(1f).height(3.dp).background(if (i < step) Gold else Sand)) }
    }
    TipBubble(tip)
    Text(title, fontFamily = FontFamily.Serif, fontSize = 21.sp, color = Ink)
    content()
}
```

- [ ] **Step 2: Peel**

```kotlin
fun Modifier.peelOut(p: Float): Modifier = this
    .graphicsLayer {
        transformOrigin = TransformOrigin(0.12f, 1f)
        translationY = size.height * (if (p < 0.35f) p / 0.35f * 0.12f else 0.12f + (p - 0.35f) / 0.65f * 0.94f)
        rotationZ = if (p < 0.35f) -2.5f * (p / 0.35f) else -2.5f - 2.5f * ((p - 0.35f) / 0.65f)
        alpha = 1f - 0.1f * p
        shadowElevation = 18.dp.toPx()
    }
```

- [ ] **Step 3: Stack transition**

```kotlin
// incoming card is mounted static underneath; only the outgoing card animates
val peel = remember { Animatable(1f) }
// on advance: snap 0f, animateTo(1f, tween(550, easing = CubicBezierEasing(0.45f,0f,0.65f,0.4f)))
```

- [ ] **Step 4: Verify**

Record the peel forward and back. Expected: header/frame static; only the outgoing card tips and drops (35%: 12% down / −2.5°; 100%: 106% down / −5° / alpha 0.9); incoming card never moves (A1/S7).

---

### Task 4: About you (demographics) step

**Files:**
- Modify: `BodyStep` in `V2Onboarding.kt` (sex row labels Female/Male; three wheels; summary)

**Interfaces:**
- Consumes: `draft.answers` (sex, age, height, weight).
- Produces: `"38 yrs · 170 cm · 72 kg"` live summary.

- [ ] **Step 1: Sex row** — two equal `#FFFDF6` boxes, 12dp radius, sand border; selected = maroon fill, white text, labels Female / Male.
- [ ] **Step 2: Wheels** — Age/Height/Weight cards; center 15sp extra-bold maroon; one faint neighbor above/below (10sp `#C9BFA4`); 8sp inkSoft label beneath.
- [ ] **Step 3: Summary line** — 10sp centered, live values.
- [ ] **Step 4: Verify** against `assets/onb_about.png`.

---

### Task 5: Objective step — four cards

**Files:**
- Modify: `GoalStep` in `V2Onboarding.kt`

- [ ] **Step 1: Cards** — Lose weight / A gentle deficit; Maintain weight / Hold where you are; Gain weight / A steady surplus; Eat healthier / Quality first. 12dp radius, card fill, sand border; title 12sp bold ink, subtitle 9sp inkSoft.
- [ ] **Step 2: Selected state** — 2dp maroon border + `#FBF3EC` fill.
- [ ] **Step 3: Verify** against `assets/onb_goal.png`; changing objective re-runs the peel with the new flow length.

---

### Task 6: Target weight — paces, chips, both curve directions

**Files:**
- Modify: `TargetStep` in `V2Onboarding.kt` (rates 0.18/0.36/0.54 kg/wk; upward curve for gain)

- [ ] **Step 1: Eyebrow** — `"BY ${horizonDate.uppercase()} YOU COULD BE"` 8.5sp goldText, letterSpacing 1.5.
- [ ] **Step 2: Big number** — Georgia 40sp ink + 14sp inkSoft "kg"; chips delta + daily kcal.
- [ ] **Step 3: Pace cards** — Gradual/Steady/Faster with "loss"/"gain" wording; selected maroon.
- [ ] **Step 4: Curves (exact paths)**

```kotlin
val reduce = Path().apply { moveTo(14f, 12f); cubicTo(70f, 16f, 150f, 40f, 206f, 54f) }
val gain   = Path().apply { moveTo(14f, 54f); cubicTo(70f, 50f, 150f, 26f, 206f, 12f) }
// viewBox 220x84, stroke 2.5dp maroon, round caps; start dot maroon r4, end dot gold r4
```

- [ ] **Step 5: Axis + footnote** — Today/Dec/Jan/Feb/<target>; "Illustrative figures only." 8sp `#B9AE93`.
- [ ] **Step 6: Verify** — reduce falls, gain rises; maintain never reaches this screen.

---

### Task 7: Diet cards in exact order

**Files:**
- Modify: `DietStep` in `V2Onboarding.kt`

- [ ] **Step 1:** Vegan / Vegetarian / Eggetarian / Non-veg with subtitles from S6.
- [ ] **Step 2:** Continue reads "Choose one to continue" until a pick; verify against `assets/onb_diet.png`.

---

### Task 8: Plan build — cascade on the Red Court

**Files:**
- Modify: `V2BuildingScreen` in `V2Onboarding.kt`

- [ ] **Step 1:** Backdrop `court.png` full-bleed; content card over it.
- [ ] **Step 2:** Title "Building your plan..." + five rows; teal 14dp tick disc with white check + 13sp ink label.
- [ ] **Step 3:** Row i: from translateY(8dp)+alpha 0 → settled, delay `i*550ms`, 450ms each.

```kotlin
val p = remember { Animatable(0f) }
LaunchedEffect(Unit) { p.animateTo(1f, tween(450, delayMillis = i * 550, easing = FastOutSlowInEasing)) }
```

- [ ] **Step 4: Verify** — rows rise one at a time ~0.55s apart; backdrop static.

---

## Self-Review

- Spec coverage: requirements 1–8 mapped to Tasks 1–8; S1–S8 covered.
- Placeholders: none — each step names files and exact values; verification steps record video.
- Type consistency: `V2_ORDER`, `nextKey`, `peelOut`, `OnbStepChrome` used consistently.

**Open interpretations (report at delivery):** Activity/Meals retained after Diet (S2 lists four steps); the repo's intro clips are video-only (no audio stream), so "trim the hold" is done at the source with ffmpeg and sound stays generated; `journey_c1.mp4` identified as the door phase.
