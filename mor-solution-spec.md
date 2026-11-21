# Mor next build — solution spec for the 19 Nov 2026 walkthrough

Planning only. No code, assets or branches were changed. This file is the only deliverable; the build starts only after Sameer audits it.

---

## 0. Build and branch this spec is written against

**Branch:** `mor-next`, working tree **uncommitted**.
- `git branch --show-current` → `mor-next`
- `git log --oneline -3` → `20a040d Onboarding v5: …` / `150151e …` / `117c49a …`
- 24 files modified/untracked; the next-version work is not committed.

**Build:** `dev.mor.app.next`, `versionCode 1`, `versionName "0.2-next"`, built from this working tree as `Mor-next-logging-debug.apk` / `Mor-next-logging-release.apk` (19 Nov 07:56). The installed app talks to `http://15.135.140.52:4000` as `x-user-id: 2`.

**Why this is the build in the recording** (evidence, not assumption):
- About you is `STEP 1 OF 6` and Goal is `STEP 2 OF 6` (frames `issue-04_t1m12s`, `issue-05_t1m36s`). On `main` (v0.1, commit `20a040d`) `V2_STEPN` puts Goal first (`main:V2Onboarding.kt:124`), so the recording cannot be the released v0.1.
- Diet/Activity/Meals show `STEP 3/4/5 OF 5` after an Eat-healthier choice — the `flowFor()` 5-step path exists only in this working tree.
- The voice pill (`Tap to speak` → waveform + Stop), the AI + web bubble chooser and the growth-bar Logged screen exist only in the uncommitted files `ui/NextLog.kt`, `ui/Scan.kt` (Logged rewrite) and `ui/Home2.kt`.
- The profile footer in the recording reads `Mor v0.1 · android build`; that string is hard-coded in `Home2.kt:1648` and is not the build version.

**Uncertainty:** I cannot prove which APK Sameer installed on the phone (debug or release). Both are this tree; the UI in the recording matches both. The deployed backend differs from the working tree only where noted in §4/§5.

**Backend state:** the server at `15.135.140.52` was deployed during this session with `gemini.js` (`searchDishes`), `routes.js` (`/foods/ai-search`, insights macros, recent foods, idempotent log), `scanCsv.js` (candidate parser), `plan.js` (intent-based) and `db.js` (`client_key` migration). Nothing else was changed for this spec.

---

## 1. Method and status legend

Each issue below records:

- **Found** — the files/functions, the real cause, and the evidence (code line + walkthrough frame). Where I could not verify, it says so.
- **Change** — the exact component/file to change, the data flow before/after, and what is not touched.
- **Test** — the repro from `02-issue-detail.md` plus the exact check.

Status tags: **STATED** (Sameer said it), **SETTLED** (decision given, do not reopen), **SEEN** (only seen in the recording, verify first).

Open design questions are collected in §18 and are **not decided** here. Recommendations are labelled as recommendations only.

---

## 2. Shared components touched by several issues

| Component | File | Issues |
|---|---|---|
| Onboarding scaffold (card frame, CTA, scroll) | `ui/OnboardingScaffold.kt` | 1, 4, 12 |
| Intro sheet + avatar + CTA | `ui/WelcomeJourney.kt` | 1, 2, 4 |
| Primary button style | `ui/OnboardingScaffold.kt` (`ObPrimaryButton`) | 4, 8, 9, 12 |
| Target step + curve | `ui/V2Onboarding.kt` (`TargetStep`, `SpecCurve`; dead `pacePath`) | 5, 13 |
| Plan/build/flow state | `ui/V2Onboarding.kt` (`V2Screen`, `routeList`, `v2CommitMap`, `V2BuildingScreen`) | 1, 10, 11, 12, 13 |
| Meal-times rows + time sheet | `ui/V2Onboarding.kt` (`MealsBody`, `TimeSheet`, `SlotIcon`) | 6, 7, 8, 9 |
| Roller | `ui/WheelPicker.kt` | 9 |
| Home screen | `ui/Home2.kt` (`HomeScreen2`, `Tracker`, `Bubble`, `WeekStrip`, `DonutTile`) | 14, 15, 16, 17, 18, 19, 24, 26, 27 |
| Home background (dark) | `ui/HomeBackground.kt` | 14, 17 |
| Logging chooser + voice | `ui/NextLog.kt` | 20, 21, 22, 23, 24, 25 |
| Logged screen | `ui/Scan.kt` (`LoggedScreen`) | 24, 25 |
| Quick add | `ui/Home.kt` (`AddSheet`) | 24, 26 |
| Coins UI | `ui/Settings.kt`, `ui/Progress.kt`, `ui/Home.kt`, `ui/Scan.kt` | 24 |
| Plan math and profile | `backend/src/plan.js`, `backend/src/routes.js`, `backend/src/db.js`, `data/Models.kt`, `Home2.kt` (profile), `PlanText.kt` | 13 |
| Food lookup | `backend/src/gemini.js`, `backend/src/routes.js`, `ui/NextLog.kt` | 12 (done), 20–23 |

---

## 3. Issues

### Issue 1 — Onboarding cards are too big

**Status:** STATED

**Found**
- The "card" on every step is the full-height cream panel drawn by `OnboardingScaffold` (`OnboardingScaffold.kt:163-169`): it starts at `cardTop = 112.dp - 18.dp` and fills to the bottom. The content column uses `.weight(1f)` (`:172-175`) and the CTA is pinned at the very bottom (`:201-205`), so any short step leaves a large empty band *inside* the card before the button.
- The scaffold paints an opaque full-screen `FrameCream` background (`:141`). The onboarding root also renders the looping Neelu gate video behind every step (`NeeluVideoBackground.kt`, provided in `V2Onboarding.kt:1532-1541`); the scaffold's cream background hides it. The video is aimed using `LocalV2SheetTop` (`NeeluVideoBackground.kt:68-72, 117-139`), but the scaffold never reports its card top, so the video cannot re-aim when the card changes size.
- The intro card is a separate composable: `TldrSheet` (`WelcomeJourney.kt:546-609`) is hard-coded to `.height(596.dp)` (`:568`) with a `Spacer(Modifier.weight(1f))` (`:594`) before the CTA — the measured 31% empty band.
- Measured frames match: intro card starts y≈462, content ends ≈880, button at ≈1365; Meal times card starts y≈140, content ends ≈800, button at ≈1450.
- **Uncertain:** what Sameer calls "background art" — the gate video or the fixed header crop. Code and comments point to the video (`NeeluVideoBackground.kt:4-8`); the visual consequence differs. See Q1.

**Change**
1. `OnboardingScaffold`: stop painting the opaque full-screen cream stage. Draw the header (`onb_header`) as today and let the onboarding video show in the stage.
2. Make the card **bottom-anchored and content-height** instead of full-height:
   - `BoxWithConstraints`; card `Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = maxHeight - 16.dp)` with the existing 20dp top radius and `ScreenCream` background.
   - Inner layout: steps badge → tip → title → subtitle → content → CTA → `navigationBarsPadding`.
   - Content uses `Modifier.weight(1f, fill = false).verticalScroll(scroll)` so tall steps still scroll and the CTA stays reachable; short steps make a short card.
   - The CTA stays inside the card at its bottom. This is the recommended option; it is a "plan question" per the pack (see Q1).
3. Report the card's top to `LocalV2SheetTop` with `onGloballyPositioned`, so the video's peacock stays in the band above the card.
4. `TldrSheet`: remove `.height(596.dp)` and the weight spacer; let the sheet wrap content (`heightIn(max = …)`), with the CTA and the "6 quick questions" line directly after the bullets at the existing 12dp rhythm. Keep the apex art and the sheet's slide-in.
5. Not touched: colours, copy, header art, CTA colour/shape, the video asset.

**Test**
- For each screen (intro, About, Goal, Target, Diet, Activity, Meal times, Plan ready) screenshot and measure: card top, last content bottom, card bottom (or CTA top).
- Pass: gap between the last content and the CTA ≤ one content row (~44–56dp); card bottom is at the CTA + 8–16dp; for tall steps the card still fits without clipping.
- Run at font scale 1.0, 1.3 and 2.0; small width 320dp and 411dp.
- Fresh install: confirm the intro card is shorter and more gate video is visible above it (screenshot pair before/after).
- Repro frames: `issue-01_t0m30s`, `t0m58s`, `t1m24s`, `t1m40s`, `t2m10s`, `t2m18s`, `t2m26s`, `t3m00s`.

**Risks:** bottom-anchoring changes the card's top edge per step; the peel is screen-level so the approved motion is unaffected, but the frame will move for short steps. Revealing the video may expose the header image/video seam; needs a visual check. Q1 must be answered before building.

---

### Issue 2 — Neelu avatar and its label are too close

**Status:** STATED

**Found**
- `TldrSheet` (`WelcomeJourney.kt:575-583`) places `Avatar()` then the `Text("Neelu · the short version")` in the same `Row` with no spacer. `Avatar()` is 40dp (`:620-633`); the ring's right edge is the label's start (measured x≈118/720 in `annotated/issue-02_zoom3x.png`).
- This avatar + "Neelu" label pairing exists only here (grep for `journey_avatar`/`Avatar()`); no other screen repeats it.
- Not touched: the background arch and the card.

**Change**
- Add `Spacer(Modifier.width(10.dp))` between `Avatar()` and the label in the `Row`; keep `verticalAlignment = CenterVertically`.
- No other file changes.

**Test**
- Intro screenshot at normal and 1.3/2.0 font scale; measure a visible gap ≥8dp between the avatar ring and the "N".
- Confirm no other instance exists (grep evidence remains true after the change).

**Risks:** none; the row is width-constrained only by the sheet padding.

---

### Issue 3 — skipped

No work. Kept in numbering for traceability.

---

### Issue 4 — Button text is too small on all onboarding screens

**Status:** STATED

**Found**
- `ObPrimaryButton` sets the label to `12.sp` (`OnboardingScaffold.kt:103`). It is the single CTA used by Continue, Choose one to continue, Build my plan, Start, Go to Home, Save (time sheet) and Try again (building failure).
- The intro CTA is a different composable, `Cta`, at `16.sp` (`WelcomeJourney.kt:660`) — the inconsistency Sameer points out ("Let's begin has the largest label").
- Body text around the CTAs is 13–15.5sp (e.g. option rows `15.5.sp` in `MealsBody`).

**Change**
- Raise `ObPrimaryButton`'s label to **16.sp** (bold, unchanged colour/shape/height) so every onboarding CTA matches the intro CTA and is larger than body text.
- Keep the disabled label ("Choose one to continue") at the same 16.sp; `maxLines = 1`, centred. No other button styles change (`MorButton` and `V2Button` are for other screens and are out of scope).
- CTA inventory (old → new):
  - "Let's begin" — 16 → 16 (unchanged)
  - "Continue" — 12 → 16
  - "Choose one to continue" — 12 → 16
  - "Build my plan" — 12 → 16
  - "Start" — 12 → 16 (then removed/replaced by "Go to Home" in issue 12)
  - "Go to Home" — 12 → 16 (screen removed in issue 12; the plan button takes this label)
  - "Save" (time sheet) — 12 → 16
  - "Try again" (building failure) — 12 → 16

**Test**
- Walk every onboarding step and screenshot the CTA; confirm the measured label size is 16sp and not clipped.
- Test the longest label ("Choose one to continue") at 320dp width and font scale 2.0; must stay on one line or degrade visibly (not ellipsised mid-word).

**Risks:** at 320dp/2.0 the longest label may not fit; if it clips, the plan will ask Sameer whether to allow two lines or drop to 15sp (question, not decided here).

---

### Issue 5 — Target weight curve does not respond to the options

**Status:** STATED

**Found**
- `TargetStep` computes the projected weight from the pace and draws `SpecCurve(gain)` (`V2Onboarding.kt:874`).
- `SpecCurve` draws two **constant** paths regardless of pace (`:894-910`): gain `M14 54 → 206 12`, loss `M14 12 → 206 54`; only direction changes.
- The numbers do change: `projected` = start ± `pace.rate * HORIZON_WEEKS` (`:772`), and the pace cards update `targetKg` (`:852-855`). Frames `issue-05_t1m36s..t1m56s` show different targets (80.4/67.2) with the same slope/end point.
- A dead `pacePath()` exists (`:745-757`, never referenced) — the pace-aware curve infrastructure was written but not used.
- Maintain/Eat healthier skip the screen (`flowFor`), as Sameer confirmed.

**Change**
- Give `SpecCurve` a pace fraction: `SpecCurve(gain: Boolean, frac: Float)` where `frac = pace.rate / 0.54f` (0.33 / 0.67 / 1.0).
- End Y: loss `12 + 42*frac`, gain `54 - 42*frac`; start Y unchanged (loss 12, gain 54). Control points interpolate between start/end while preserving the pack's curve character (e.g. c1 at 38% with 12% of the delta, c2 at 78% with 70% of the delta).
- End dot moves with the end Y; start dot stays. Animate `frac` when a pace card is selected (`animateFloatAsState`, ~300ms) so the line visibly changes.
- Keep the axis labels, footnote, colours and stroke.
- No change to the numbers, the date, or Maintain/Eat-healthier.

**Test**
- Gain: screenshot Gradual, Steady, Faster — end point rises progressively; the steeper the pace, the higher the end (and the larger the displayed target/delta).
- Loss: same three with a falling line; Faster ends lowest.
- Assert the curve's end delta direction matches the sign of `change` and the magnitude order matches 0.18 < 0.36 < 0.54.

**Risks:** the line is illustrative; the visual encoding must not imply a different number from the headline. Verify against the displayed kg.

---

### Issue 6 — Snack icon is unclear

**Status:** SETTLED — chai cup, option 1

**Found**
- Meal rows render icons with `SlotIcon(slot)` (`V2Onboarding.kt:1240-1276`), a 34dp Canvas.
- The snack branch (`:1249-1254`) draws exactly what the frames show: a 5.5dp brown disc (cup) + a 5dp ivory disc (milk) + two 0.9dp grey "steam" lines — unreadable at row size.
- Breakfast/Lunch/Dinner are different Canvas styles; the pack puts them out of scope.
- The chosen mock (`mocks/snack-icon-options.png`) is a maroon-outlined, gold-filled steaming cup with a saucer, shown at row size.

**Change**
- Add `res/drawable/ic_snack_chai.xml` (vector, 24×24 viewport): saucer line + cup body filled gold `#C9A227`, maroon `#7A1D16` outline at the mock's line weight, handle, and three steam strokes; no other new art.
- In `SlotIcon`, replace the `"snack"` Canvas branch with `Image(painterResource(R.drawable.ic_snack_chai), null, Modifier.size(28.dp))` inside the 34dp slot (or `Canvas` + `draw` of the same path if the vector does not render crisply at 34dp).
- Leave Breakfast/Lunch/Dinner untouched. Do not change the row layout, time chip or switch.

**Test**
- Meal-times row screenshot at 1.0 and 2.0 font scale; compare with `annotated/issue-06_meal-icons_zoom3x.png` and the mock row; the cup is recognisable at 24–28dp and does not clip.
- Confirm no change to the other three icons.

**Risks:** a vector at 34dp needs 1.5–1.6dp strokes to read; test at 1× before building the rest.

---

### Issue 7 — Snack time: editor and row show different times

**Status:** SEEN — verify first

**Found**
- `TimeSheet` (`V2Onboarding.kt:1181-1215`) keeps two independent states: `pick` (set by the quick chips, `:1200-1202`) and the Material `TimePicker` state `st` (`:1186-1191`). The chips never touch `st`; Save reads only `st.hour`/`st.minute` (`:1205-1208`). A highlighted chip can therefore disagree with the time that gets saved — **a real defect**.
- The row text is not a live preview: `MealsBody` renders `fmtTime(a.times[slot])` (`:1131-1136`) and only updates `a.times` on Save (`:1173-1179`). So any editor/row difference *before* Save is expected.
- `fmtTime` (`:1450-1458`) converts 24h → 12h correctly (18 → 6 PM); `TimePicker(is24Hour = true)` shows 18:00. The specific frame claim "row says 6:00 AM while editor says 18:00" cannot be reproduced from code alone; it would require a save of 06:00 or a mis-read. **Unverified.**
- Verdict: the chip/picker split is a confirmed bug; the AM/PM flip in the recording is not confirmed.

**Change**
- Issue 9 replaces the sheet wholesale: no chips, one source of truth (`hour12`, `minute`, `ampm`), Save converts once to `HH:MM`. That removes the chip defect and the two-format mismatch.
- No change to the meal row layout.

**Test**
- Open each of the four rows; the row time must not change while the sheet is open.
- Change hour/minute/AM-PM; Save; the row must equal the editor's value in 12h form. Reopen: same value.
- Repeat with device 12h and 24h settings: our app renders 12h in both (one format, as required).
- Repro frames: `issue-07_t2m28s`, `t2m32s`, `t2m36s`, `t2m44s`, `t2m52s`.

---

### Issue 8 — Save is hidden in the time editor

**Status:** STATED

**Found**
- `TimeSheet` uses a default `ModalBottomSheet` (no `skipPartiallyExpanded`) and puts `ObPrimaryButton("Save")` *after* the tall Material `TimePicker` (`V2Onboarding.kt:1186, 1211-1214`). At the initial half-expanded height the picker fills the sheet and Save is below the fold (frames `t2m28s`, `t2m44s`).
- The new wheels are much shorter, but the same mistake (Save inside a scroll/after a tall element) would repeat.

**Change**
- `skipPartiallyExpanded = true` so the sheet opens at its content height.
- Sheet content: title → three wheels → Save, with Save in a fixed bottom row above `navigationBarsPadding`; no scroll around Save, `heightIn(max = …)` on the content.
- Keep the "Edit <slot>" title, the sheet radius and colours.

**Test**
- Open the snack editor (and each meal) on a 320×640 small screen and on 411×731: Save is fully visible the instant the sheet opens, no drag. Repeat at font scale 2.0.
- Repro: `issue-07_t2m28s` and `issue-08_t2m44s`.

**Risks:** at 2.0 font scale with three 220dp wheels, the sheet may exceed the screen; then the wheels (not Save) must scroll. That is the fallback; verify.

---

### Issue 9 — Clock picker looks poor

**Status:** SETTLED — option B, reuse the existing roller

**Found**
- The current sheet uses Material 3 `TimePicker` with default (purple) colours (`V2Onboarding.kt:57-58, 1186-1211`) and 24h readout plus quick chips; the frames show the purple dial and boxes. It does not use the app's design.
- The About roller is `WheelPicker` (`ui/WheelPicker.kt:69-196`): 5 rows × 44dp, maroon centre band, Cormorant 30sp centre label + Inter 11sp unit, faded neighbours, snap fling, tap-to-jump, haptics, accessibility; `WheelCard` (`V2Onboarding.kt:348-400`) wraps it with a label and unit pill.
- The About step already uses it for age/height/weight (`V2Onboarding.kt:392-394`).

**Change**
- Replace the `TimeSheet` body with **three `WheelPicker` instances in a Row**: hour (1–12), minute (00–59), AM/PM. Each is the same element, same styling/formatting, weighted 1f. No `WheelCard`, no chips, no dial.
- State: parse the initial `HH:MM` into `hour12`, `minute`, `ampm`; `onSelected` updates the corresponding value; Save converts to 24h and calls the existing `onSave`.
- `onTapCentre` for the time wheels has no About-equivalent action (About opens a numeric keypad) → pass a no-op until Sameer answers (Q2).
- Delete the `TimePicker`/`rememberTimePickerState` imports and the `TimeChip` composable (chips are not in mock B).
- Save stays pinned (issue 8).

**Open questions (Q2):** 12h + AM/PM vs 24h single wheel; minute step (1 vs 5); centre-tap behaviour; whether to show unit labels ("hr"/"min") or bare numbers like the mock.
**Recommendation:** 12h + AM/PM (matches the mock and the row's 12h formatting); 1-minute steps; no centre-tap action; bare numbers. Not decided.

**Test**
- Compare with `mocks/clock-picker-option-B.png`: three columns, maroon centre rows, neighbours faded, Save visible.
- Conversion: 12 AM → 00:00; 12 PM → 12:00; 1 PM → 13:00; 11:59 PM → 23:59.
- All four meals; small screen; font scale 2.0; TalkBack labels ("Hour", "Minute", "AM or PM").
- After Save, reopen: the wheels show the saved time.

**Risks:** three wheels in 320dp width leave ~96dp per column; the 30sp centre label "12" plus band fits, "AM"/"PM" fits; test. The roller's 5-row height (220dp) plus title and Save is ~360dp — fits most screens, not tiny ones at 2.0.

---

### Issue 10 — "Building your plan" card position

**Status:** STATED, subjective — options only

**Found**
- `V2BuildingScreen` centres the card with `Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center)` (`V2Onboarding.kt:1350-1353`), so the card's centre is ~50% of the height; frames measure 45–55%.
- Card content and the backdrop (`court.png`) are unchanged by this issue.

**Options (Sameer picks; screenshots required)**
- A. centre at **60%** of screen height.
- B. centre at **68%**.
- C. centre at **75%**.

**Implementation (after choice)**
- `BoxWithConstraints`; keep the card at its natural size and offset its centre: `Modifier.offset(y = maxHeight * (frac - 0.5f))` inside a full-size centred Box. `frac` is a single constant so the three screenshots can be produced with a one-line change.
- The failure "Try again" button follows below the card; verify it does not collide with the nav bar at 75%.

**Test**
- Screenshot the building screen at each fraction on the same device; confirm the card remains over the peacock and the checklist is fully visible.
- Repro frames: `issue-10_t2m57s`, `t2m58s`, `t2m59s`.

**Recommendation (not a decision):** 68% — visibly lower without crowding the bottom edge.

---

### Issue 11 — Confetti freezes

**Status:** STATED

**Found**
- `Confetti` (`V2Onboarding.kt:1037-1057`) maps each burst's life as `rel = (t - at) / 2f` (`:1045`) while the animation drives `t` 0→1 (`:954`). At `t = 1` the first burst (`at = 380/2700 ≈ 0.14`) has `rel ≈ 0.43` and the second (`at ≈ 0.22`) has `rel ≈ 0.39`; alpha is `1 - rel`, so ~57% of the dots remain on screen **forever** at the final frame. The frames `t3m00s–t3m12s` show identical frozen positions.
- The confetti plays once per install (`draft.confettiShown` persisted at `:950-957`), so re-entry does not replay it.

**Change**
- Normalise each burst to its own window: `val rel = ((t - at) / (1f - at)).coerceIn(0f, 1f)`; draw only while `rel > 0f && rel < 1f`; use `alpha = 1f - rel`.
- Add an early return when `t >= 1f` so the canvas is empty at the end.
- Keep the two volleys, seeds, colours and total 2700ms.

**Test**
- Open Plan ready; screenshot at ~1s, 2.7s, 5s, 15s: dots fall and by 2.7s none remain. Compare `t3m00s` and `t3m12s` positions — they must differ and then clear.
- Leave Plan ready and return (where possible): no frozen dots.

**Risks:** none; pure math.

---

### Issue 12 — Remove the whole "You're all set" screen

**Status:** SETTLED — remove the whole screen, card and chip

**Found**
- `AllSetScreen` (`V2Onboarding.kt:1066-1095`) is a full step with `OnboardingScaffold`, a "Welcome to Mor. Let's eat well." subtitle, the reminders card and a "Go to Home" button.
- It is wired in three places: `routeList()` appends `"allset"` (`:1504`), `V2Screen` dispatches `"allset"` (`:1494`), and `BackHandler` excludes `"allset"` (`:1540`).
- `PlanStep` currently says `primaryText = "Start"` (`:966`) and calls `onNext`, which advances to the all-set step.
- The reminders card copy is generated from `draft.answers.mealOn` (`:1084-1090`).

**Change**
- Delete `AllSetScreen` and the `"allset"` route/dispatch/back-handler entries.
- `PlanStep`: `primaryText = "Go to Home"`; on primary run the AllSet action: `vm.refreshProfile()`, `vm.refreshHome()`, `nav.navigate("home") { popUpTo("boot") { inclusive = true } }`, `draft.clear()`, `MorSound.done()`. The plan screen becomes the final step; the step badge already shows the last step (`step = totalFor`).
- Back from Plan continues to skip Building (`go(-1)` already steps over it, `:1516-1518`). No `"allset"` in the persisted route list.
- Stale persisted `stepKey = "allset"` (only possible on the test user) falls back to `"journey"`; no migration needed because onboarded users never re-enter onboarding.
- The reminders message disappears with the card.
- Not touched: Plan ready content, steps, peel.

**Test**
- Walk all four goal paths end to end: Plan ready → "Go to Home" → Home; system Back from Home exits the app; no "You're all set" frame appears.
- Back from Plan returns to Meal times (not Building).
- Confirm the plan badge shows the last step (e.g. 5 OF 5).
- Repro frames: `issue-12_t3m16s`, `t3m20s`, `issue-04_t3m04s`.

**Open question (Q4):** where, if anywhere, the "meal reminders are on/off" information lives. Recommendation: no replacement on this screen; the new Home notification row (issues 16/17) is the place where a user sees and changes it. Not decided.

---

### Issue 13 — Plan data may be inconsistent across screens

**Status:** SEEN — verify first; fixes only for confirmed defects

**Found (code causes)**
- **Three-decimal weights:** `plan.js:55` builds `goal_line` from raw floats: `` `${weight_kg} → ${tgt} kg` ``. `target_weight_kg` is stored unrounded from the client (`V2Onboarding.kt:791, 854`; `v2CommitMap` `:1281`). The frame shows `70.102 kg`; Profile already rounds via `fmtKg` (`Home2.kt:1686`).
- **Mismatched dates:** the target screen uses a fixed `HORIZON_WEEKS = 16.1` (`V2Onboarding.kt:727, 776`) → 12 March from 19 Nov. The server's `targetDate` uses `|weight-target| / 0.5` (`plan.js:15-19`) → ~31 Dec for a 2.9 kg target. Two formulas, no shared pace.
- **Mismatched kcal:** the target chip is `tdee + pace.dailyDelta` (`:799`; Steady −500, Gradual −300, Faster −700 in `PACES` `:717-726`). The server always applies ±500/300 (`plan.js:35-40`). Video: 1,710 vs 1,990. Only the Steady pace happens to agree.
- **Stale target on Eat healthier:** `v2CommitMap` always sends `target_weight_kg = a.targetKg` (`:1281`), even when the flow skipped Target; the draft still holds the value from an earlier goal choice. Profile then shows "Goal 73 → 70.1 kg" with "Eat healthier" (frame `t8m12s`). Maintain likewise keeps an old target instead of the current weight.
- "Built around your eat-healthier goal" is correct for the healthy goal; not a bug.
- **Unverified without a run:** the exact persisted state after the walkthrough's goal changes; whether Home's kcal target equals the plan's (server: yes, `/home` reads the plan row, `routes.js:122-162`).

**Change**
1. **Persist the chosen pace** (kg/week) with the profile: add `pace REAL` to `users` (`db.js:11-25`) + an idempotent `ALTER TABLE` migration (same pattern as `client_key`). `POST /profile` accepts `pace`; `v2CommitMap` sends `pace` = the selected `Pace.rate`; reset clears it (`routes.js:588`).
2. **One plan formula** in `plan.js`:
   - Intent from the goal (lose/gain); maintain/healthy → no deficit.
   - `kcal = tdee ± round(pace*7700/7)` (10-kcal rounding, existing sex minimum); fall back to today's ±500/300 when `pace` is null (old profiles).
   - `target_date = today + ceil(|current-target|/pace) weeks` (equals the target screen's horizon because the target is projected from the same pace); fall back to the 0.5 kg/wk formula when pace is null.
   - `goal_line` rounds both weights to 1 decimal; omit the line when there is no target (healthy).
3. **Clean commits:** `v2CommitMap` → maintain: `target_weight_kg = weight_kg`; healthy: `target_weight_kg = null`; round to 0.1 kg in both cases. Server: `target_weight_kg` accepts null in `POST /profile` (already `Number(v) || null`).
4. **Profile display:** show the Goal row only when a target exists; keep `fmtKg` (1 decimal). For maintain, "73 → 73 kg" is acceptable if Sameer wants it (Q11).
5. **Plan card date format:** use the same formatter as the target screen (`d MMMM` / `d MMM`) so "12 March" and the plan agree visually, not only numerically.
6. Keep the target screen's illustrative footnote; do not change its math except storing the pace and rounding the target.
7. Do not change calculation logic beyond those alignments.

**Verification table (run each row and fill in)**

| Goal path | Target screen (date / kg / kcal) | Plan ready (line / date / kcal) | Home kcal | Profile (goal / kcal) |
|---|---|---|---|---|
| Lose × 3 paces | | | | |
| Gain × 3 paces | | | | |
| Maintain | n/a | | | |
| Eat healthier | n/a | | | |

**Test**
- Reset user 2, run each path, record the four screens; assert Target date = Plan date; Target kcal = Plan kcal = Home kcal = Profile kcal; kg has ≤1 decimal; healthy shows no Goal row; change goals mid-flow and confirm no stale target reaches the server.
- Backend unit tests for `computePlan` with/without pace (extend `backend/test`), and a curl check that `goal_line` has 1 decimal.

**Risks:** schema migration and a deploy; existing users fall back to old math until they next save a profile; changing the plan changes Home targets (expected). If Sameer prefers not to add a column, the alternative is to display the server formula on the target screen and accept different kcal — that is a design change, not this plan.

---

### Issue 14 — Camera-off message is unreadable and badly placed

**Status:** SETTLED (placement via mock C)

**Found**
- The camera line is `QuietNudgeRow` (`Home2.kt:756-830`), placed directly under the bubble (`:349`) and above the calorie card. It draws Ink2 `#3A2F25` text on the transparent night background (`#43222D`…`#1D0F14`) with a teal action — contrast is near zero (frames `issue-14`, annotated).
- On the day theme it is still faint but visible; the issue targets the dark Home.
- It is a **hairline row**, not the selected mock's card, and it sits in the wrong place (approved: just above the streak card).
- The dead `ReminderNudgeCard` (`:391-532`) is never called; it is unrelated but should be removed while touching this code.

**Change**
- Delete `QuietNudgeRow` (and the unused `ReminderNudgeCard`) and replace with the shared rotating permission card (issues 16/17), placed after the flexible spacer and before `WeekStrip`.
- Cream card (`MorColor.Card`), Ink text, maroon action → contrast is theme-independent.
- Camera logic untouched.

**Test**
- Night Home screenshot: measure/verify the row's text contrast ≥4.5:1 on the cream card; the faint line no longer exists.
- `grep` confirms no `QuietNudgeRow`/`ReminderNudgeCard` calls remain.

---

### Issue 15 — Remove the "Eaten anything lately? Add a meal." bubble

**Status:** STATED

**Found**
- `bubbleText()` returns "Eaten anything lately? Add a meal." when `kcal_eaten <= 0.0` (`Home2.kt:533-536`); the bubble is rendered in the top row (`:328-331`).
- The kcal-left bubbles ("N kcal left today…", "Over today…") are different states and stay.
- Frames: `issue-15`, annotated item 2.

**Change**
- Do not compose the bubble when `kcal_eaten <= 0.0` (return null / conditional). Keep Neelu in the row (right-aligned) so the hero card position does not jump.
- Keep the `Bubble` composable and the tail for the remaining states.
- Not touched: kcal-left copy, neon.

**Test**
- Fresh user with zero meals: Home shows Neelu but no white bubble; screenshot.
- Log one meal: the kcal-left bubble appears as before.
- Check the zero state at font scale 2.0 and on the night theme.

---

### Issues 14/16/17 — rotating permission row (one component, three issues)

#### Issue 16 — Permission prompts: order and content

**Status:** SETTLED — order and one-row shape

**Found**
- Today only the camera hint exists (`QuietNudgeRow`). There is no notifications row, no microphone row, no rate/feedback row (grep; frames).
- `POST_NOTIFICATIONS` and `RECORD_AUDIO` are already declared (`AndroidManifest.xml`), and permissions are requested only inside the logging flows.
- The approved row is one at a time, in order: notifications → camera → microphone → rate/feedback. The mock's 4 dots show the position.

**Change**
- New `PermissionRow` composable in `Home2.kt` (or `ui/HomePermissionRow.kt` to keep Home2 focused):
  - One cream card row: 36dp tinted icon disc, title (15sp semibold ink), subtitle (11.5sp ink muted), maroon action pill, and 4 dots beneath (current dot gold, others sand).
  - State machine: candidates in order, filtered by what is still needed:
    1. Notifications — `SDK >= 33 && POST_NOTIFICATIONS != GRANTED` (and reminders are on, as onboarding sets).
    2. Camera — `CAMERA != GRANTED`.
    3. Microphone — `RECORD_AUDIO != GRANTED`.
    4. Rate/feedback — once, unless dismissed.
  - Show the first candidate only. On grant/resolve/dismiss, re-evaluate and advance; persist dismissed keys in `SharedPreferences` (`perm_row_done_<key>`) so nothing resurfaces.
  - Dots represent the fixed 4 slots, with completed ones dimmed.
- Use existing icons where they exist: `ic_bell`, `ic_scan_camera`, `ic_mic`; a simple star path for rate (mock disc has a glyph).
- No layout changes beyond inserting this row (see 17).

**Test**
- Clean install (reset app data): notifications row → "Allow" → system prompt → camera row → microphone row → rate row.
- Deny each: row must remain (or advance to the next?) per product choice; define: denying keeps the row for that state with a "Settings" fallback? **Open detail** — recommend: deny → row stays until dismissed via its secondary action; adding a secondary action is a design question (Q10).
- Android 12 (no notification permission): notifications row is skipped.
- Restart: dismissed/permission states are honoured.

#### Issue 14 — placement and readability

Covered above: cream card, above the streak card, contrast verified.

#### Issue 17 — placement, wording and tip

**Status:** placement SETTLED; wording NOT approved

**Found**
- The approved empty band is between the calorie card and the streak card (annotated `issue-18`, y≈840–1295). Current code has a `Spacer(weight(1f))` (`Home2.kt:355`) that already creates it; the old row sits above the calorie card.
- The mock C row sits **just above** the streak card; dots below it.

**Change**
- Insert `PermissionRow` after the weight spacer and before `WeekStrip`, with the same 16dp side margins as the streak card; keep the flexible spacer so the empty band remains.
- Remove the old row (issue 14).
- Proposed copy (for approval; **not settled**). Each row is title / subtitle / action:

  1. Notifications
     - "Meal reminders need notifications"
     - "Allow notifications so Neelu can nudge you at your meal times."
     - "Allow"
  2. Camera
     - "Scan meals with your camera"
     - "Allow camera access to log a plate in seconds. Choose “Allow only while using the app”."
     - "Allow"
  3. Microphone
     - "Log meals by voice"
     - "Allow microphone access to describe a meal. Choose “Allow only while using the app”."
     - "Allow"
  4. Rate/feedback
     - "Enjoying Mor?"
     - "A quick rating helps us make it better."
     - "Rate"

- The mock's copy is example text and is not used verbatim.

**Test**
- Small (320dp) and large (480dp) widths, day and night: contrast, no clipping of the subtitle, button and dots aligned.
- The row appears exactly once and above the streak card; the empty band above is unchanged.
- Walk the rotation in both orders (starting with notifications granted/ungranted).

**Open questions (Q10):** the rate/feedback action and whether the row needs "Not now". Options in §18.

---

### Issue 18 — Calorie card is not edge to edge

**Status:** STATED

**Found**
- `Tracker` wraps its card in `Box(Modifier.padding(start = 16.dp, top = 16.dp))` and caps the card at `widthIn(max = (h2ScreenW() * 0.78f).dp)` (`Home2.kt:686-688`). On 411dp that is ~321dp instead of ~379dp; frames measure 586/720 vs the streak's 696/720.
- `WeekStrip` already uses `padding(horizontal = 16.dp)` (`:940-946`), so the streak is full width.
- Contents inside the card (number, subtitle, bar, donuts, water) are spaced with fixed gaps; widening the card leaves the donuts `SpaceBetween` and the water row full width.

**Change**
- `Tracker`: `Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, top = 16.dp))`, card `Modifier.fillMaxWidth()` (remove `widthIn`).
- Keep internal horizontal padding; verify the donut row distributes evenly and the water caption/segments span the new width.
- Do not change the streak card or the bottom buttons.

**Test**
- Screenshot Home at 320/360/411/480dp widths; the calorie card's left and right edges align with the streak card (±0–1dp).
- Confirm no text wraps unexpectedly (kcal line, water caption) and the donuts stay evenly spaced.

---

### Issue 19 — Home layout needs to be better

**Status:** STATED, vague; **covered by mock C and issues 14–18**

**Found**
- The unused band and the misplaced faint row are the same problems as 14–18.
- No other change was stated.

**Change**
- None beyond issues 14–18. Do not move other elements; do not add sections.
- The flexible spacer is kept so the empty band matches the mock.

**Test**
- Visual diff against `mocks/home-mock-C-selected.png`; list any remaining difference and leave it for Sameer rather than fixing unasked.

---

### Issue 20 — Voice: a phrase said once is repeated many times

**Status:** STATED

**Found**
- `VoiceSection` (`NextLog.kt:430-500`) stores one `transcript` string. `onPartialResults` **appends** the latest partial to the existing text (`:454-462`):
  `transcript = listOf(transcript, part).filter { … }.joinToString(" ")`.
  SpeechRecognizer partials are cumulative, so this duplicates every earlier word; `onResults` appends the final text again (`:463-473`).
- The duplicated string is what `onStop` passes to `runSearch` (`:480-489`), so it is also what the lookup receives and what the "AI + web results for …" header shows (frames `t5m20s–t5m36s`).
- Real bug. No ambiguity.

**Change**
- Separate state: `settled` (confirmed utterances) and `partial` (latest partial). Display `(settled + " " + partial).trim()`.
- On partial: if `latest` starts with the previous partial (cumulative vendor) replace; otherwise append (incremental vendor).
- On results: append only if the result is not already contained in `settled`; clear `partial`.
- On Stop: send the same displayed string; never send `settled` twice.
- Extract `mergeVoiceText(settled, partial, latest, isFinal)` as a pure function (new file or top-level in `NextLog.kt`) with unit tests.
- No change to the voice UI or the Stop-only rule.

**Test**
- Unit tests with synthetic partials for the two examples:
  - `paneer` → `paneer butter` → `paneer butter masala` → final `paneer butter masala rice eggs and water`.
  - `chicken` → `chicken rogan josh` → final `chicken biryani and lassi` (or the spoken variant).
  Assert the displayed/sent string contains each word once.
- On device: speak both phrases; compare the live text with the speech; confirm the lookup header equals the live text.

**Risk:** some recognizers return incremental partials (no shared prefix). The rule above handles both; test on the Pixel emulator and Sameer's Xiaomi if possible.

---

### Issue 21 — Voice: spoken foods are dropped from the results

**Status:** STATED

**Found**
- Text and voice both call `POST /api/foods/ai-search` with the whole sentence (`NextLog.kt:100-115`); the server asks one model call for 3–5 **candidates for the whole string** (`gemini.js` `SEARCH_PROMPT`): "If the text describes several foods in one meal, treat the whole meal as ONE candidate … then add alternative meal interpretations."
- Therefore per-food coverage is not guaranteed: the model can merge or drop items. Live checks today:
  - `paneer butter masala rice eggs and water` → `Paneer Butter Masala with Rice & Eggs` (720), `… with Rice` (583), `…` (265), `Boiled Egg` (77) — water dropped.
  - `chicken biryani and lassi` → `Chicken Biryani and Sweet Lassi` (608), `Chicken Biryani` (450), `Sweet Lassi` (158), `Salted Lassi` (100) — this one included lassi, showing variance.
  - `chicken biryani` → five variants of the same dish (see 23).
  - The existing `POST /api/voice-log` already parses a transcript into **one row per food** (verified: paneer/rice/eggs → 3 items; chicken biryani + lassi → 2).
- Real bug: the lookup does not decompose a multi-food sentence; results depend on the model's choice.

**Change**
- Add `POST /api/foods/ai-search-groups` (new):
  1. `gemini.parseTranscript(query)` → items (existing parser, one row per food, ≤5; ignore plain `water` — see Q6).
  2. For each item, run `gemini.searchDishes(item.name)` **in parallel** and parse candidates.
  3. Return `{ query, groups: [{ label, items: [...] }], water: bool }`; each group labels the food (from the parsed item's name), each candidate keeps name/kcal/macros/portion; match labels by rank as today.
  4. If a group has no candidates, use the parsed item itself as its single candidate (honest fallback, no invented food).
  5. If parsing yields a single food, return one group (client can use the one-tap path).
- Client: use the groups endpoint for voice and text. Photo keeps the current single-meal path (`/scan` + `/ai-search` via `nextlog?...&auto=1`) for now — not part of the demonstrated issue.
- `/ai-search` stays for the photo/fallback.
- Do not change calorie values or the model.

**Test**
- Backend: unit tests for the grouping helper (parse + cap + water filter) and the fallback; live curl for Examples A and B asserting every food appears as a group.
- App: run Example A and B (typed and spoken). The header lists every food; each has candidates.
- Repro frames: `t5m28s`, `t5m32s`, `t6m28s`, `t6m36s`.

**Risks:** N+1 model calls per lookup (≤5 parallel, 25s timeout each) → latency ~5–8s; add a client loading state. Model/rate-limit failures per group: keep other groups, show a retry for the failed one. Prompt/parse variance.

**Open (Q6):** plain water — drop from food groups and optionally add a glass (water tracker), or include as a food candidate. Recommendation: drop from food and offer a separate "Add a glass of water" action; not decided.

---

### Issue 22 — Several foods in one sentence give one logged food

**Status:** STATED — UI options only

**Found**
- `logPick` (`NextLog.kt:140-180`) posts **one** candidate as one meal: `items` = a list of one. The Logged screen then says "+X from the <name>", and the frames show only Chicken Biryani (407) logged.
- The DB and the legacy voice/scan paths already support multiple items per meal (`items_json`), so no schema change is needed.
- The issue is compounded by 21 (no per-food candidates).

**Options (Sameer picks)**
- **A. Grouped sections (recommended).** Header "AI + web results for “…” — estimates:"; then one labelled block per food, each with 3–5 bubbles. Tap one bubble per food (selected = maroon border + check). A sticky bottom bar appears when every food has a pick: "Log N items · X kcal". Logs one meal with N items.
- **B. One food at a time.** The chooser shows the first food; after a pick, it slides to the next with a 1/3 counter; at the end, "Log N items".
- **C. Combo-first.** One section of whole-meal combos (e.g. "Biryani + Lassi"), plus an "or pick individually" section. Logs the combo as one item.

**Implementation (after choice; A preferred)**
- `NextLogScreen` holds `groups` and a `selection: Map<foodLabel, FoodItem>`.
- Single group: preserve today's one-tap-to-log behavior (fast path). Multi-group: require a pick per group; the button logs all selected items in one `POST /meals` with `name` = the joined food names (≤40 chars) or "N-item meal"; source "ai".
- Logged: the growth bar adds the summed kcal; the sub-line "+X from the <meal name>"; meal detail lists N items with their macros.
- No backend logging change.

**Test**
- Example A: pick one candidate per food → the meal has 3 items; totals = sum; Home macros move by the sum; Logged says the meal name.
- Example B: 3 groups → 3 items.
- Single food: one bubble → tap logs immediately (unchanged).
- Meal detail shows each item.
- Count entries in the UI and in `/api/meals/:id`.

**Risks:** the fixed 5-candidate bubble list per group makes the screen long; A needs a scroll and a sticky bar. Partial selection must disable Log. This is why the option is Sameer's.

---

### Issue 23 — Repeated-looking food options with different calories

**Status:** SEEN — verified as variants

**Found**
- Live curls show the same dish multiple times with different kcal and different portions, e.g. `chicken biryani` → `Chicken Biryani` 450 (`1 plate (300g)`), 600 (`1 plate with 2 pieces …`), 340 (`1 cup`), 420 (`1 serving`), 180 (`100g`). Same for paneer (265 vs 219 etc.).
- The bubble (`NextLog.kt:250-310`) shows only name, kcal and the match label; `portion` is not displayed. So real variants look like duplicates.
- Not true duplicates: the underlying rows differ by portion. Verdict: a real presentation defect, not a lookup defect.

**Change**
- Show the portion in the bubble: second line `~450 kcal est. · 1 plate (300g) · Closest match` (wrap if needed).
- Deduplicate candidates whose `name + kcal + portion` are identical (keep the first).
- Keep distinct portions visible; never merge different kcal values silently.
- Server already defaults a missing portion to "1 serving" (`scanCsv.js` new parser); no value invention in the client.

**Test**
- Curl `chicken biryani`, screenshot the chooser: five distinct portions visible; no identical duplicates.
- Check the video queries (paneer/rice, biryani) and a 320dp screen for wrapping.
- Assert no two bubbles on screen have identical name+kcal+portion.

---

### Issue 24 — Remove coins

**Status:** STATED

**Found (UI, exhaustive grep)**
- `Scan.kt:1118` — "+N coins" pill on the Logged screen (added in the growth-bar layout).
- `Home.kt:222` — quick-add toast "… · +N coins".
- `Settings.kt:220` — profile header "🔥 N-day streak 🪙 N coins".
- `Progress.kt:392` — challenges header "🪙 N coins".
- `Progress.kt:430` — claim toast "+N coins!".
- `Progress.kt:440` — claim button "+N" (coin amount).
- `Progress.kt:443` — uncompleted challenge reward "🪙N".
- `Progress.kt:483` — badge modal "+50 coins" pill.
- `Progress.kt:494` — badge toast "… +N coins".
- Models carry `coins`/`coins_awarded`; the backend awards coins and uses them in challenges (`routes.js:237`, challenge/badge routes). The coin numbers are not purely cosmetic there (they are the challenge reward ledger), but no user-facing behaviour depends on spending them.

**Change**
- Remove all coin text and icons from the UI, exactly at the lines above:
  - Logged: delete the reward pill and its animation slot.
  - Quick-add toast: "Logged N kcal to <slot>".
  - Settings header: keep the streak text, drop the coin part.
  - Challenges header: drop the coins pill.
  - Challenge claim: button label "Collect" (no amount); claimed "Done"; remove the pre-claim reward coin text.
  - Badge modal: drop the "+50 coins" pill; collection toast "Badge collected".
- Leave the backend `coins` fields and the award logic untouched (no UI reads them). Do not remove the streak/ring logic.
- If Sameer later wants to hide challenge rewards entirely, that is a layout decision (Q8).

**Test**
- `grep -ri coin android/app/src/main/java` returns no user-visible string (only model/VM fields if kept).
- Walk every logging path (quick add, text, voice, photo, logged), profile, challenges, badge; screenshots show no coin icon/text or toast.
- Confirm challenge claims still complete and the badge still awards.

**Risks:** Challenge rows lose their reward indicator; the claim button appears only when done. Q8.

---

### Issue 25 — Logged screen disappears too fast

**Status:** STATED

**Found**
- The Logged screen auto-navigates to Home after `delay(2600)` (`Scan.kt:1065-1074`), plus the ~300ms home enter transition. Frames show 6:44 → ~6:48.
- The layout also had a late reward pill (`rewardIn`, +620ms) and staggered macros; the reward pill is removed by issue 24, which removes one late content change. In the current code, the macro rows are always laid out and only their alpha changes, so they should not push content; verify.
- "Done" already works at any time (`Scan.kt:1180-1190`).

**Change**
- Change the auto-advance delay from **2600ms to 7600ms** (current + five seconds), keeping the home slide-in.
- Remove the coin pill and its spacer so no late row changes the layout.
- Ensure every delayed element occupies its final space from the first frame (fixed heights; alpha-only entrances).
- No change to the Logged layout otherwise.

**Test**
- Stopwatch from the Logged screen appearing to Home: 7.3–7.9s. "Done" exits immediately.
- Compare screenshots at 0.5s and 2s: no vertical movement of the title, bar, macro rows or buttons.
- Repro: `t6m43s–t6m48s`, `annotated/issue-24-25…`.

**Note:** "five seconds longer than now" is applied to the code's 2.6s (not to the video's ~4s); if Sameer meant 5s total, the value is 5.0s. The spec states this assumption explicitly.

---

### Issue 26 — Quick add does not update protein, carbs, fat

**Status:** STATED — the "should it" decision is open

**Found**
- Quick add posts `source: "quick"`, `name: "Quick add"`, and one item with `name`, `kcal`, `portion` only — no `protein_g`/`carbs_g`/`fat_g` (`Home.kt:217-220`).
- The backend sums macros from items (`routes.js:194-240`); missing macros default to 0, so kcal rises while the rings stay put. Frames: 1,125 → 1,725 → 2,325 with rings constant at 63/40/52.
- Confirmed cause; no data for macros exists at quick-add time.

**Options**
- **A. Calories-only, made explicit (recommended for this pass).** Keep the current data; add a one-line note in the sheet: "Adds calories only — protein, carbs and fat stay unchanged." No invented macros.
- **B. Food-backed quick add.** Tapping Add opens the AI/search chooser (or a small preset list) so the entry has real macros; "quick" becomes 2 taps and a network lookup.
- **C. Manual macros.** Add optional protein/carb/fat fields to the quick-add sheet; the user enters them; totals update.
- (A fixed split of kcal into macros would invent data and is not proposed.)

**Change**
- Blocked on Q9. Option A is a copy-only change (and a decision that the rings not moving is acceptable); B/C change the data flow.

**Test (for A)**
- Quick add ×4 → total +600 kcal, macros unchanged, note visible; Logged/meal detail show 0 macros for that item.
- For B/C (if chosen): totals and rings change by the entered/derived macros; sums match the meal detail.

---

### Issue 27 — Macro rings drop to 0% and restart on updates

**Status:** STATED (confirmed by Sameer)

**Found**
- `DonutTile` creates `Animatable(0f)` for the sweep and the number on every composition (`Home2.kt:854-856`).
- The sweep and the number run **sequentially**: the number starts only after the 1100ms sweep finishes (`:857-866`), so the arc can be at 63% while the number still reads 0% (frame `t7m20s`).
- Home is disposed while Logged/Add/profile is on top; returning recreates `DonutTile` from 0 and re-runs the entrance, producing the 0% frames (`t7m24s`, `8m00`) followed by the correct values (`t7m36s`, `8m04`).
- If the plan/macro goals were momentarily absent the target would be 0, but the server always returns fallbacks (`routes.js:122-127`) and `vm.home` is replaced only with a full response; the dominant cause is the remount/sequence above.
- Real bug. Exact frame-to-frame cause cannot be instrumented without running the app; the mechanism is proven by code.

**Change**
- Hoist the last-shown percentages into `AppVm` (e.g. `val macroPct = mutableStateMapOf<String, Float>()`).
- `DonutTile` initialises both animatables from the remembered percentage (0 on a genuine cold start), then animates ring and number **concurrently** over the same 1100ms/delay.
- On completion, write the final percentage to the memory.
- Guard `goal <= 0`: keep the last shown value and skip the animation (never animate to a fake 0).
- Reset the memory on user reset/account switch.
- Keep the look, colours and staggering between the three tiles.
- Optionally skip the entrance sweep when returning with values already known (memory does this naturally).

**Test**
- Film at 60fps: log a meal → the three numbers and arcs move together from their previous values; no 0 flash.
- Open Add sheet and close (Home not disposed) → no reset; navigate to Profile and back → no reset; quick add → values move to the new totals, no 0.
- Cold start → entrance from 0 is expected and allowed.
- Repro frames: `t7m20s`, `t7m24s`, `t7m32s`, `t7m36s`, `8m00`, `8m04`.

**Risks:** shared animation memory in the ViewModel; ensure it is updated only by the tiles (not by stale responses) and reset on `/reset`.

---

## 4. Cross-cutting changes and data flow

1. **Onboarding shell (1,2,4,12)**
   - Scaffold bottom-anchored, content-height card; CTA 16sp; intro sheet wraps; avatar gap; All-Set route removed.
   - Route list: `journey → [body, goal, (target), diet, activity, meals] → building → plan → home`.
2. **Plan truth (13 + 21/22 backend)**
   - `users.pace` persisted; `plan.js` uses it for kcal and date; `goal_line` rounded; healthy/maintain commit clean targets.
   - New `/foods/ai-search-groups`; client renders groups; one meal with N items.
3. **Home (14–19, 24, 26, 27)**
   - Full-width calorie card; no zero-meal bubble; one rotating permission card above the streak; coins removed; ring animation memory; quick-add decision.
4. **Logging (20–25)**
   - Correct transcript assembly; per-food groups; portion-labelled candidates; multi-item log; coins removed; Logged stays ~7.6s.
5. **Time picker (7–9)**
   - Three re-used rollers; one value source; Save pinned.

Nothing else changes: intro videos, Home art, colours, Notifications/Insights/Streak/Challenges logic, water, scan API, plan content, meal rows, settings.

---

## 5. Build order

**Phase 0 — baseline (30 min)**
- Reset user 2; install the current debug build; record a baseline video/screens of the 26 issues for before/after.
- Confirm test data, backend reachability, and the three test devices (emulator, Xiaomi; small width via emulator override).

**Phase 1 — data truth (13)**
- Backend: `pace` column + migration, `plan.js` pace math/rounding, `/profile` accepts pace, reset clears pace, tests.
- Client: `v2CommitMap` sends pace, rounds target, clears target for healthy/maintain; Plan pill date format; Profile hides missing target.
- Deploy; run the four-path verification table.

**Phase 2 — onboarding shell (1, 2, 4, 12, 11, 5, 6)**
- 1 then 4 then 12 (they touch the same scaffold), then 2, 11, 5, 6.
- After 1: screenshot every step and measure gaps (font scales 1.0/1.3/2.0).

**Phase 3 — time picker (7, 8, 9)**
- Build the three-roller sheet; verify conversion; then 8's sheet height; document 7's verification.

**Phase 4 — Home (18, 15, 14/16/17, 27, 26, 24-home)**
- 18 (width) → 15 (bubble) → 27 (rings) → 14/16/17 (permission row; wording pending) → 26 (pending Q9) → 24 (coins).

**Phase 5 — logging (20, 21, 22, 23, 24-logging, 25)**
- 20 (transcript) → 21 backend groups → 22 UI + multi-item log (pending Q7) → 23 portions → 24 (Logged/quick-add toasts) → 25 (timing).

**Phase 6 — position and polish (10)**
- Produce 60/68/75 screenshots; implement the chosen fraction; capture the final set.

**Phase 7 — regression and release**
- Full walkthrough of all four goal paths and all four logging paths; night/day; font scales; 320/411/480 widths; backend tests; Android unit tests; rebuild debug + release; update the report; commit/PR only on Sameer's instruction.

Dependencies: 1 blocks the layout tests for 4; 12 changes the flow that 13 and 1 test; 21 blocks 22; 24 must precede 25's layout check; 9 must precede 7/8 verification; 26 is blocked on Q9.

---

## 6. Risks

1. **Onboarding card rework (1)** is the highest visual risk: bottom anchoring changes the card top per step; revealing the video may expose a seam with the header image; the peel is screen-level but the "frame still" promise must be re-checked.
2. **All-Set removal (12)** changes navigation/back stack; test back from Home, rotation, and a stale `stepKey`.
3. **Plan math/pace (13)** touches the DB and every goal path; a bad deploy changes calorie targets for existing users. Deploy with a migration and a fallback; verify user 1 is unaffected (the live app uses the same server).
4. **Grouped lookup (21/22)** multiplies AI calls and latency, and the output is model-dependent. Cap at 5 foods, run in parallel, and keep per-group failures isolated. Water semantics are open.
5. **Three-roller time sheet (9)** on 320dp/2.0 font: width and height limits; centre-tap and 12/24h are open.
6. **Permission row (14/16/17)** involves OS permission outcomes that vary by Android version and OEM; the rate/feedback action has no defined behaviour.
7. **Coins removal (24)** leaves challenge rows without a reward indicator; the backend still awards coins invisibly.
8. **Rings (27)** share state across Home instances; ensure a reset/account switch clears it and a cold start still shows an entrance.
9. **Quick add (26)** is blocked on a product decision; do not invent macros.
10. **Confetti (11)** is a pure-math fix but the exact timing must be checked at 60fps.
11. **Logged timing (25)** assumes "five more seconds" applies to the code's 2.6s; confirm interpretation.

---

## 7. What I could not check without running the app

- The exact ring frame sequence in issue 27 (remount vs a transient 0-goal response) — the mechanism is in code; the frame timing needs a run.
- Issue 7's "6:00 AM row vs 18:00 editor": the chip/picker split is proven; the AM/PM flip itself is not reproducible statically.
- Whether Sameer's phone build is the debug or release APK (both match the UI).
- The real "background art" Sameer means in issue 1, and how the video/header seam looks when the card shrinks.
- The exact current Logged duration on the device (code says 2.6s + transition; the video suggests ~4s).
- Whether the Profile "Goal" row appeared only because of the walkthrough's goal changes or also on a fresh healthy onboarding (code sends a stale target; a fresh flow would too if the draft carried it).
- Rate/feedback: what "rate the app, rate your experience and give feedback" should open (Play in-app review vs listing vs a feedback form), and whether a dismiss action is needed.
- Large-font behaviour of the new 16sp CTA and the permission row on the Xiaomi at system font scale 2.0.
- Multi-food grouping quality on the live model (runs above show variance; the endpoint design needs live testing).

---

## 8. Open questions for Sameer (options + recommendation; not decided)

**Q1 — Issue 1 background and CTA.** Is "background art" the Neelu gate video (make the stage transparent and re-aim it) or the fixed header only? Where should the CTA sit: inside the content-height card at the bottom (recommended), or pinned to the screen bottom with art between?
- A: video visible, card content-height, CTA at the card bottom (recommended).
- B: video visible, CTA pinned at the screen bottom (card ends above it).
- C: no video, only the header; card content-height (least visual change).

**Q2 — Issue 9 roller details.**
- 12h + AM/PM wheels (recommended) or a single 24h hour wheel?
- Minute step: 1 (recommended, matches the mock's 58/59/00/01) or 5/15?
- Centre-row tap: no action (recommended) or open a numeric keypad like About?
- Keep bare numbers (recommended, as the mock) or add unit labels like the About roller?
- Remove the quick-time chips (recommended, mock B has none) or keep them?

**Q3 — Issue 10 card position.** 60%, 68% (recommended) or 75%? Implementation waits for screenshots/approval.

**Q4 — Issue 12 reminders information.** No replacement (recommended; the Home notification row covers the off state), a toast on Home, or a line in Profile/Settings?
- Note: the removed message confirmed reminders were on; no screen currently shows "on" except Settings.

**Q5 — Issue 17 wording.** Approve or edit the four proposed rows in issue 17. The mock copy is not approved.

**Q6 — Issue 21 water.** Drop plain water from food results (recommended) and optionally offer "Add a glass of water", or keep it as a food candidate? Drinks like lassi stay as food.

**Q7 — Issue 22 several foods UI.**
- A: grouped sections with one pick per food + "Log N items" (recommended).
- B: one food at a time.
- C: combo-first with individual options.

**Q8 — Issue 24 challenge rewards.** After removing coins: claim button "Collect" with no amount and no pre-claim reward (recommended), or hide the challenge reward column entirely, or keep a non-coin reward token?

**Q9 — Issue 26 quick add.** Calories-only with an explicit note (recommended for this pass), food-backed (AI lookup), or manual macro entry? Do not invent macros.

**Q10 — Issues 16/17 rate/feedback.** What does "Rate" open: Play in-app review when available (recommended), the Play listing, or a feedback form/mail? And does the row need a "Not now"/dismiss, and what happens on a denied permission (keep the row, advance, or show a Settings link)?

**Q11 — Issue 13 maintain/healthy goal line.** For Maintain, show "73 → 73 kg" or omit the Goal row? For Eat healthier, omit the Goal row (recommended) or show the current weight only?

---

## 9. Explicitly not changed

Intro videos/audio, the palace art, colours, type, Home art and mood states (beyond the listed), water tracking, the scan API, notifications/insights/streak/challenges logic (except coin UI), meal rows, settings, and the backend except the documented plan/pace and grouped-search additions.

No code, asset or branch was changed while writing this spec.
