# Mor next build — solution spec v2 (19 Nov 2026 walkthrough)

Revision 2 of `mor-solution-spec.md`. Planning only: no code, asset, branch or server changes were made, and nothing was rolled back. This file supersedes v1; the original issue numbers and settled choices are unchanged. Build starts only after Sameer audits this.

**Claim convention used throughout**

- **[V] VERIFIED** — I re-opened the file and line(s) while writing this revision.
- **[C] CLAIMED** — from memory or from the earlier spec; not re-opened in this revision. No earlier claim is presented as fact unless re-checked.

No `[C]` reference remains below: every code reference in this revision was reopened. Frame measurements are quoted from the pack as evidence, not code.

---

## 0. Build, branch and backend chronology

### 0.1 Branch and build [V]

- `git branch --show-current` → **`mor-next`**; `git log --oneline -3` → `20a040d` (14 Nov 19:54 IST), `150151e` (13 Nov), `117c49a` (10 Nov). Nothing has been committed since 14 Nov.
- `git status --short` → 26 uncommitted paths: 17 modified, 9 untracked. Two of the untracked are this spec and its v1; the other 24 are the next-version work. Nothing from the next version is committed.
- App identity [V]: `app/build.gradle.kts:21` `applicationId = "dev.mor.app.next"`, `:24 versionCode = 1`, `:25 versionName = "0.2-next"`.
- Builds on disk [V]: `~/Desktop/Mor-next-logging-debug.apk` and `…-release.apk`, both 19 Nov 07:56. The installed app targets `http://15.135.140.52:4000` as `x-user-id: 2` [V] (`data/Service.kt`).

**Why the recording is this build** (evidence, not assumption) [V]:
- About you is STEP 1 OF 6 and Goal is STEP 2 OF 6; on `main` (v0.1) Goal is step 1 (`git show main:…/V2Onboarding.kt` line 124: `"goal" to 1`).
- The 5-step Diet/Activity/Meals path after Eat-healthier exists only in this tree (`flowFor`, `V2Onboarding.kt:512-521`).
- The voice pill, the AI + web bubble chooser and the growth-bar Logged screen exist only in the uncommitted files (`ui/NextLog.kt`, `ui/Scan.kt` Logged rewrite).
- The profile footer reads "Mor v0.1 · android build"; that string is hard-coded (`Home2.kt:1648` [V]), not the build version.

**Uncertain:** whether Sameer installed the debug or the release APK. Both are the same tree and match the recording.

### 0.2 Backend chronology — evidence [V]

Clock basis: the Mac is IST (UTC+5:30); the server reports UTC. Subtract 5:30 to compare.

| Evidence | Local (IST) | Server (UTC) |
|---|---|---|
| Last git commit | 14 Nov 19:54 | — |
| Local backend file mtimes | `gemini.js` 19 Nov 07:37:13; `scanCsv.js` 07:37:34; `routes.js` 07:38:53; `plan.js` 14 Nov 14:11; `db.js` 14 Nov 15:24 | — |
| Server file mtimes | 07:38:20–07:42:28 | 02:08:20–02:12:28 |
| Server backups created | 07:38 | 02:08 (`~/backend-bak-20261119-0208`) |
| Service restarts | 07:38:29 and 07:42:31 | 02:08:29 and 02:12:31 |
| Code writes since | none | none after 02:12:28 UTC |

- Server md5s equal the **local working tree** for `gemini.js`, `routes.js`, `scanCsv.js`, `plan.js`, `db.js` [V].
- Server md5s of all five differ from the pre-deploy backup [V].
- `plan.js` and `db.js` are **not** in `git status`: they match commit `20a040d` (14 Nov) and were merely deployed for the first time [V].
- `gemini.js`, `scanCsv.js`, `routes.js` are modified in the working tree (+27, +35, +35/−3 lines vs HEAD) [V] and were deployed.
- The database file's WAL changed at 15:10/15:11 UTC (app data from using the app), not code [V].
- No git on the server and no per-user audit trail; file ownership is `ec2-user`, and the changes were made over SSH from this machine. **Attribution:** these were made by the assistant in the earlier "finish the next-version build" task (the session record shows the scp/restart commands). There is no evidence of any other author.

### 0.3 Two lists

**Changes that existed before this walkthrough task** — i.e. the previous task's work, 19 Nov morning:

- Working tree: the entire next-version app (items 1–18), including `ui/NextLog.kt` (07:42), `ui/Scan.kt` (07:53), `ui/Home2.kt` (07:46), the onboarding files (14–18 Nov).
- Backend working tree: `gemini.js` (`searchDishes` + prompt), `scanCsv.js` (`parseCandidateCsv`), `routes.js` (`/foods/ai-search`, insights per-day macros, recent foods, `client_key` idempotency); `plan.js`/`db.js` from HEAD (14 Nov).
- Server deploy at 07:38–07:42 IST on 19 Nov (the two restarts above), bringing the server to the working tree: new `/foods/ai-search`, `/api/insights` extra fields, `/api/foods/recent`, idempotent `/api/meals`, intent-based plan maths, `meals.client_key` migration.

**Changes made for this walkthrough task (the spec/audit):**

- **None.** No local file, server file, branch or service was changed while producing v1 or this v2. The only new file is `mor-solution-spec.md` (v1) and this `mor-solution-spec-v2.md`.

### 0.4 What each live account can see right now [V]

- **User 1 (v0.1 live app):** no new screen. The deploy brought the server in line with endpoints the v0.1 app already expects (`/api/foods/recent`, idempotent `/api/meals`), so its Recent tab and retry-safe logging now work. Its stored plan row is untouched; the intent-based `plan.js` only recomputes when a profile is saved again. The one visible effect of `/api/insights` is extra JSON fields the old app ignores.
- **User 2 (next app):** the whole morning feature set, including `/foods/ai-search` (verified live earlier: `2 masala dosa` → 4 candidates; `dhokla, 2 pieces` → 3) and per-day macro fields in insights.
- **Nothing was reverted.** The v2 proposals for the backend (pace, groups) are **not** deployed and are not referenced by the live app.

---

## 1. Method and status legend

Each issue records: **Found** (files/functions, cause, evidence, uncertainty), **Change** (exact files, data-flow change, what stays), **Test** (repro from `02-issue-detail.md`).

Status: **STATED** (Sameer said it), **SETTLED** (decision given), **SEEN** (verify first).

All open decisions are consolidated in §8; nothing there is decided here.

---

## 2. Shared components touched by several issues

| Component | File | Issues | Verification |
|---|---|---|---|
| Onboarding scaffold (card frame, CTA) | `ui/OnboardingScaffold.kt` | 1, 4, 12 | [V] |
| Intro sheet, avatar, CTA | `ui/WelcomeJourney.kt` | 1, 2, 4 | [V] |
| `ObPrimaryButton` | `ui/OnboardingScaffold.kt` | 4, 8, 9, 12 | [V] |
| Target step/curve, flow, building, commit map | `ui/V2Onboarding.kt` | 1, 5, 10, 11, 12, 13 | [V] |
| Meal-times rows, sheet, slot icons, roller card | `ui/V2Onboarding.kt`, `ui/WheelPicker.kt` | 6, 7, 8, 9 | [V] |
| Home | `ui/Home2.kt` | 14, 15, 16, 17, 18, 19, 24, 26, 27 | [V] |
| Home background | `ui/HomeBackground.kt` | 14, 17 | [V] |
| Chooser/voice | `ui/NextLog.kt` | 20, 21, 22, 23, 24, 25 | [V] |
| Logged | `ui/Scan.kt` | 24, 25 | [V] |
| Quick add | `ui/Home.kt` | 24, 26 | [V] |
| Coins UI | `ui/Settings.kt`, `ui/Progress.kt`, `ui/Home.kt`, `ui/Scan.kt` | 24 | [V] |
| Plan maths/profile | `backend/src/plan.js`, `routes.js`, `db.js`, `Home2.kt`, `PlanText.kt` | 13 | [V] |
| Food lookup | `backend/src/gemini.js`, `routes.js`, `NextLog.kt` | 12 (shipped), 20–23 | [V] |

"Food lookup (done)" in v1 was imprecise: `/foods/ai-search` is deployed; the **per-food groups endpoint is not built** (issue 21/22).

---

## 3. Issues

### Issue 1 — Onboarding cards are too big

**Status:** STATED

**Found** [V]
- The step "card" is the full-height cream panel in `OnboardingScaffold`: it starts at `cardTop = 112.dp - 18.dp`, `fillMaxSize()` + `.padding(top = cardTop)` (`OnboardingScaffold.kt:138-169`); the content column has `.weight(1f)` (`:172-175`) and the CTA is pinned at the bottom (`:201-205`). Short steps therefore leave a large empty band inside the card before the button.
- The scaffold paints an opaque full-screen `FrameCream` background (`:141`). The onboarding root renders the Neelu gate video behind every step (`V2Onboarding.kt:1532-1541`; `NeeluVideoBackground.kt`), but this cream stage covers it. `LocalV2SheetTop` is used to aim the video, but the scaffold never reports its card top (`OnboardingScaffold.kt` has no `onGloballyPositioned`).
- The intro card is `TldrSheet` (`WelcomeJourney.kt:546-609`): fixed `.height(596.dp)` (`:568`) with `Spacer(Modifier.weight(1f))` (`:594`) before the CTA. It is a separate composable over the welcome video.
- Recorded measurements: intro card top ≈462, content ends ≈880, button ≈1365; Meal times card top ≈140, content ends ≈800, button ≈1450 (frames `issue-01_*`).

**Change (kept as small as the ask: card height only)**
1. `OnboardingScaffold`: keep the header image, the cream stage, colours and art exactly as they are. Change only the card's height:
   - `BoxWithConstraints`; card `Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = maxHeight - 16.dp)`, same 20dp top radius, same `ScreenCream`.
   - Content: badge → tip → title → subtitle → content (`.weight(1f, fill = false).verticalScroll(...)`) → CTA → `navigationBarsPadding`.
   - Bottom-anchored so the CTA stays at the bottom of the screen, which is where it is today. This resolves the pack's "button pinned" question without moving it.
2. `TldrSheet`: remove `.height(596.dp)` and the `weight(1f)` spacer; wrap content at the existing 12dp rhythm, CTA and the "6 quick questions" line directly after the bullets. Keep the apex and slide-in.
3. **No video work:** do not make the stage transparent, do not add `LocalV2SheetTop` reporting, do not touch the gate video or the header art.
4. Not touched: colours, copy, art, CTA style, the peel.

**What it exposes (stated plainly)**
- On the step screens the card shrinks toward the bottom; the area it vacates is the scaffold's existing **cream stage**, not the gate video (the stage is opaque and stays so). Because the card is bottom-anchored, that cream band appears between the header image and the card, and **the header image's lower edge becomes a visible seam** at 112dp. No video seam is exposed; the video stays hidden below the header as it is today.
- On the intro, the sheet floats over the welcome video, so shrinking it genuinely shows more video above the sheet (no seam; the video is the background there).
- If Sameer's "more background" meant the palace video on the step cards, that is a separate change (transparent stage + video re-aiming) and is **not** proposed here.

**Test**
- Every step: screenshot and measure card top, last content bottom, card bottom/CTA top. Pass: content→CTA gap ≤ one content row (~44–56dp); card bottom at CTA + 8–16dp; tall steps still scroll without clipping.
- Font scale 1.0/1.3/2.0; widths 320/411dp. Intro before/after pair.
- Repro frames: `issue-01_t0m30s`, `t0m58s`, `t1m24s`, `t1m40s`, `t2m10s`, `t2m18s`, `t2m26s`, `t3m00s`.

**Risks:** the card top varies per step (the peel is screen-level, but the incoming card's top will differ from the outgoing one); the header seam needs a visual check; tall steps at 2.0 still need the scroll.

---

### Issue 2 — Neelu avatar and its label are too close

**Status:** STATED

**Found** [V]
- `TldrSheet` (`WelcomeJourney.kt:575-583`) places `Avatar()` and the "Neelu · the short version" text in one Row with no spacer; the avatar is 40dp (`:620-633`). The ring's right edge equals the label's start (x≈118/720, `annotated/issue-02_zoom3x.png`).
- Grep confirms this is the only avatar + "Neelu" label pairing [V] (`journey_avatar` appears only here).

**Change**
- Add `Spacer(Modifier.width(10.dp))` between `Avatar()` and the label; keep `CenterVertically`. No other change.

**Test**
- Intro at 1.0/1.3/2.0; measure a gap ≥8dp; confirm no other instance exists.

---

### Issue 3 — skipped

No work; kept for traceability.

---

### Issue 4 — Button text is too small on all onboarding screens

**Status:** STATED

**Found** [V]
- `ObPrimaryButton` label is `12.sp` (`OnboardingScaffold.kt:103`); it is shared by Continue, Choose one to continue, Build my plan, Start, Go to Home, Save and Try again.
- The intro CTA is a separate composable at `16.sp` (`WelcomeJourney.kt:660`), which is why "Let's begin" looks larger.
- Body/option text is 13–15.5sp (e.g. meal rows 15.5sp, `V2Onboarding.kt:1127`).

**Change**
- Raise `ObPrimaryButton` to **16.sp** (bold; colour/shape/height unchanged). Keep the disabled label at 16.sp, `maxLines = 1`, centred.
- CTA inventory (old → new): Let's begin 16→16; Continue 12→16; Choose one to continue 12→16; Build my plan 12→16; Start 12→16 (then replaced by Go to Home in issue 12); Go to Home 12→16 (the plan button takes this label); Save 12→16; Try again 12→16.

**Test**
- Screenshot every step; label measures 16sp and is not clipped. Longest label at 320dp/2.0 must stay on one line or degrade visibly (no mid-word ellipsis). If it cannot fit, this becomes a small question in the decisions table (two lines vs 15sp); not decided here.

---

### Issue 5 — Target weight curve does not respond to the options

**Status:** STATED

**Found** [V]
- `TargetStep` calls `SpecCurve(gain)` (`V2Onboarding.kt:874`). `SpecCurve` (`:894-910`) draws constant paths: gain `M14 54 → 206 12`, loss `M14 12 → 206 54`; only the direction changes.
- Numbers do change: `projected = start ± pace.rate * HORIZON_WEEKS` (`:772`), pace cards update `targetKg` (`:852-855`). Frames `issue-05_t1m36s..t1m56s` show different targets with the same slope/end point.
- Dead `pacePath()` exists (`:745-757`) and is never called — pace-aware curve code was written but unused.
- Maintain/Eat healthier skip this screen (`flowFor`, `:515-516`).

**Change**
- `SpecCurve(gain: Boolean, frac: Float)` with `frac = pace.rate / 0.54f` (0.33/0.67/1.0).
- End Y: loss `12 + 42*frac`, gain `54 - 42*frac`; start Y unchanged. Control points interpolate between start and end while preserving the pack's shape.
- End dot moves with the end Y; animate `frac` (~300ms) when a pace is selected.
- Keep axis, footnote, colours, stroke, and the numbers.
- No change for Maintain/Eat healthier.

**Test**
- Gain and loss × Gradual/Steady/Faster: screenshot; endpoint order matches 0.18 < 0.36 < 0.54; direction matches the sign of `change`; the line cannot contradict the headline kg.

---

### Issue 6 — Snack icon is unclear

**Status:** SETTLED — chai cup, option 1

**Found** [V]
- Meal icons are a 34dp Canvas in `SlotIcon` (`V2Onboarding.kt:1240-1276`). The snack branch (`:1258-1263`) draws a 5.5dp brown disc + 5dp ivory disc + two 0.9dp grey steam lines — exactly the unreadable mark in the frames.
- Breakfast/Lunch/Dinner use different Canvas styles; the pack puts them out of scope.
- The chosen mock is a maroon-outlined, gold-filled steaming cup with a saucer, shown large and at row size (`mocks/snack-icon-options.png`).

**Change**
- Add `res/drawable/ic_snack_chai.xml` (24×24 viewport): saucer + cup filled gold `#C9A227`, maroon `#7A1D16` outline at the mock's weight, handle, three steam strokes.
- Replace only the `"snack"` branch with the drawable, rendered at 28dp inside the 34dp slot (or Canvas paths if the vector is not crisp). Other icons untouched.

**Test**
- Meal-times row at 1.0/2.0, compare with `annotated/issue-06_meal-icons_zoom3x.png` and the mock row; recognisable, not clipped; other icons unchanged.

---

### Issue 7 — Snack time: editor and row show different times

**Status:** SEEN — verify first

**Found** [V]
- `TimeSheet` (`V2Onboarding.kt:1181-1215`) keeps two states: `pick` (set by the quick chips, `:1206-1208`) and the Material picker state `st` (`:1186-1191`). Chips never update `st`; Save reads only `st.hour`/`st.minute` (`:1214`). **Confirmed defect: a highlighted chip can disagree with the time that saves.**
- The row is not a live preview: `MealsBody` renders `fmtTime(a.times[slot])` (`:1139-1143`) and updates `a.times` only in `onSave` (`:1170-1173`). Differences before Save are by design.
- `fmtTime` (`:1450-1458`) is correct for 24h→12h (18 → 6 PM). The recorded "row 6:00 AM while editor 18:00" is **not reproducible** from code alone; it needs a replay/run. **Unverified.**
- Verdict: chip/picker split is real; the AM/PM flip is unconfirmed.

**Change**
- Issue 9 removes the chips and makes one state the source of truth; Save converts once. No row changes.

**Test**
- All four rows: opening does not change the row; Save makes the row equal the editor; reopen equals saved. Device 12h/24h: the app renders 12h in both (one format).
- Repro: `t2m28s`, `t2m32s`, `t2m36s`, `t2m44s`, `t2m52s`.

---

### Issue 8 — Save is hidden in the time editor

**Status:** STATED

**Found** [V]
- Default `ModalBottomSheet` (no `skipPartiallyExpanded`) with a tall `TimePicker` and Save after it (`V2Onboarding.kt:1186, 1211-1214`). At the initial half height Save is below the fold (frames `t2m28s`, `t2m44s`).

**Change**
- `skipPartiallyExpanded = true`; sheet content heights to its content.
- Save in a fixed bottom row above `navigationBarsPadding`, outside any scroll; only the wheels may scroll if height is exceeded.
- Keep the "Edit <slot>" title and colours.

**Test**
- Open each meal at 320×640 and 411×731, and at 2.0 font scale: Save fully visible immediately, no drag.

---

### Issue 9 — Clock picker looks poor

**Status:** SETTLED — option B, complete reuse of the About roller

**What the About roller actually is** [V]
1. **`WheelCard`** (`V2Onboarding.kt:347-400`): a card at `MorShape.Tile` (18dp) radius, `MorColor.Card` background, 1dp `SheetBorder`, 6dp inner padding; a 28dp header row with an uppercase label (Inter 10.5sp semibold, 0.14em letter-spacing, `Muted`); an optional **unit pill** (26dp high, `Ivory`, 1dp border, 13dp radius, maroon 10.5sp text, a 6×4 caret) that is a *switch* (`onUnit`); then the roller; then a "Tap to type" caption (Inter 11sp, `#8A7A62`).
2. **`WheelPicker`** (`ui/WheelPicker.kt:69-196`): 5 visible rows × 44dp (220dp tall), maroon centre band (12dp radius, shadow), centre label Cormorant 30sp semibold + Inter 11sp gold unit suffix, neighbours Inter 18sp at 0.94/0.86 scale and 0.85/0.45 alpha, top/bottom fade, snap fling, tick haptics, tap a row to jump, tap the centre to type, accessibility `progressBarRangeInfo` + custom actions.
3. **`ObKeypad`** (`V2Onboarding.kt:410+`): the centre-tap numeric entry, validated against the wheel's range.

**Change**
- `TimeSheet` becomes a Row of three `WheelCard`s, each with a `WheelPicker`:
  - **HOUR** — items 1..12, centre label with the component's unit suffix "hr", `onTapCentre` opens `ObKeypad` for 1–12.
  - **MINUTE** — items 00..59, unit suffix "min", `ObKeypad` 0–59, **1-minute steps** (the mock's neighbours 58/59/00/01; the component renders any list).
  - **AM/PM** — items AM/PM; see the question below for the caption/keypad and unit pill.
- 12-hour display + AM/PM (mock B); Save converts to `HH:MM` 24h for storage.
- Remove the Material `TimePicker`, `rememberTimePickerState` imports and `TimeChip`.
- Keep the "Edit <slot>" title; Save pinned per issue 8.
- **Do not** design a new control; every visible part comes from `WheelCard`/`WheelPicker`/`ObKeypad`.

**What cannot be reused 1:1 (question, §8)**
- The **unit pill is a unit switcher** (`unitPill != null && onUnit != null` draws it). Time has no alternate unit, so the pill cannot function as designed.
- The **"Tap to type" caption + keypad cannot type AM/PM**.
- These two are the only parts of the roller that do not map 1:1 to time. They are **not resolved here**; they are one decision (Q6) in §8, with options. No replacement control is designed.

**Test**
- Compare with `mocks/clock-picker-option-B.png`; all four meals; conversion 12 AM→00:00, 12 PM→12:00, 11:59 PM→23:59; small screen; 2.0 font; TalkBack.
- The row shows the saved time; reopening shows it on the wheels.

**Risks:** three cards at 320dp width (~96dp each) with a 30sp centre label; the roller is 220dp tall + card header/caption + Save ≈ 360dp. Test at 2.0.

---

### Issue 10 — "Building your plan" card position

**Status:** STATED, subjective — options only

**Found** [V]
- `V2BuildingScreen` centres the card via `Column(fillMaxSize(), verticalArrangement = Arrangement.Center)` (`V2Onboarding.kt:1350-1353`); centre ≈50%, frames 45–55%. Content/art untouched by this issue.

**Options (Sameer picks; screenshots first):** centre at **60%**, **68%**, **75%**.
**Implementation after choice:** `BoxWithConstraints`, keep the card's size, offset its centre by `maxHeight * (frac - 0.5f)`; one constant so the three screenshots are a one-line change. Verify "Try again" at 75% does not hit the nav bar.

**Test:** three screenshots; card over the peacock; checklist fully visible. Frames `t2m57s–t2m59s`.
**Recommendation (not a decision):** 68%.

---

### Issue 11 — Confetti freezes

**Status:** STATED

**Found** [V]
- `Confetti` (`V2Onboarding.kt:1037-1057`) maps each burst's life as `rel = (t - at) / 2f` (`:1045`) while `t` runs 0→1 (`:954`). At `t=1`, burst 1 (`at≈0.14`) has `rel≈0.43`, burst 2 (`at≈0.22`) `rel≈0.39`; alpha `1-rel` leaves ~57% of dots permanently at the final frame. Frames `t3m00s–t3m12s` show identical dots.
- It plays once per install (`confettiShown` persisted, `:950-957`).

**Change**
- `val rel = ((t - at) / (1f - at)).coerceIn(0f, 1f)`; draw while `rel > 0f`; `alpha = 1f - rel`; early return when `t >= 1f`.
- Keep volleys, seeds, colours, 2700ms.

**Test**
- Plan ready screenshots at 1s/2.7s/5s/15s: dots fall, none remain by 2.7s; `t3m00s` vs `t3m12s` differ and then clear.

---

### Issue 12 — Remove the whole "You're all set" screen

**Status:** SETTLED — whole screen removed

**Found** [V]
- `AllSetScreen` (`V2Onboarding.kt:1066-1095`): subtitle chip, reminders card, "Go to Home" button.
- Wired at `routeList()` (`:1510` appends `"allset"`), `V2Screen` (`:1494`), `BackHandler` (`:1562` excludes it).
- `PlanStep` says `"Start"` (`:966`) and advances; the reminders copy is derived from `draft.answers.mealOn` (`:1097-1100`).

**Change**
- Delete `AllSetScreen`, the `"allset"` route/dispatch, and the back-handler exclusion.
- `PlanStep`: `primaryText = "Go to Home"`; on primary run the old AllSet action: `refreshProfile()`, `refreshHome()`, `navigate("home") { popUpTo("boot") { inclusive = true } }`, `draft.clear()`, `MorSound.done()`.
- The plan screen is the final step; its badge already shows the last step. Back from Plan skips Building as today (`:1516-1518`).
- A stale persisted `stepKey = "allset"` falls back to journey; onboarded users never re-enter. No migration.
- The reminders message disappears with the card. **Resolved by investigation:** no replacement; the Home notification row (16/17) is where the off state is seen, and Settings shows the current state. Not a question.
- Not touched: plan content, steps, peel.

**Test**
- All four goal paths: Plan → Go to Home → Home; Back from Home exits; no all-set frame; plan badge is the last step; Back from Plan → Meal times.
- Frames `t3m16s`, `t3m20s`, `issue-04_t3m04s`.

---

### Issue 13 — Plan data may be inconsistent across screens

**Status:** SEEN — verify first; definite fixes separated from proposed backend maths

#### What the code does today (all [V])

- Client target screen (`TargetStep`, `V2Onboarding.kt:765-890`):
  - `projected = weight ± pace.rate * 16.1` (`:727, :772`); the target screen runs **before** Activity (`flowFor`, `:512-516`), so `tdeeOf()` uses the draft's default activity `"light"` (`:150`, `:730-735`).
  - kcal chip `= ((round(tdee_light) + pace.dailyDelta) + 5)/10*10` (`:799`); paces are 0.18/−300, 0.36/−500, 0.54/−700 (loss) and +150/+300/+450 (gain) (`:717-726`).
  - Date: today + round(16.1×7)=113 days (`:776`).
  - Stores `targetKg` unrounded (`:791`, `:854`); `v2CommitMap` always sends `target_weight_kg` (`:1281`), even for Maintain/Eat healthier, which skip the target screen.
- Server (`plan.js:28-59` [V]): intent from the **target delta**, not the goal; kcal = TDEE ±500/300; `target_date = today + ceil(|delta|/0.5) weeks` (`:15-19`); `goal_line` uses raw floats (`:55`).
- Profile (`Home2.kt:1604-1605` [V]) always shows the Goal row from `weight → target`, and `profileGoalLine` (`:1689-1696`) shows "Goal label, kcal a day"; `fmtKg` rounds to 1dp (`:1685` [V]).
- Home kcal target reads the stored plan row (`routes.js:122-162` [V]).

#### Verification table — code-traced, not run in the app

Fixture F (the final recorded profile): male, 41 y, 172 cm, **73 kg**, activity **moderately active** (1.55 ⇒ TDEE 2487.75); at the Target step the draft's default activity is light (1.375 ⇒ TDEE 2206.88). All cells computed by executing `backend/src/plan.js` and by replicating the Kotlin target formula; **no cell is from a live app run**. The Eat-healthier row is corroborated by the recording.

| Path | Target screen | Plan ready | Home | Profile |
|---|---|---|---|---|
| **Lose, Steady 0.36** | BY 12 MARCH · 67.2 kg · −5.8 kg · 1,710 kcal (matches `t1m56s`) | 1,990 kcal · P77/C286/F60 · “73 → 67.204 kg · by Thu 11 Feb” | 1,990 kcal | Lose weight, 1,990 kcal · Goal 73 → 67.2 kg |
| **Gain, Faster 0.54** | BY 12 MARCH · 81.7 kg · +8.7 kg · 2,660 kcal (recording: 80.4 at weight 71.7, +8.7, 2,660) | 2,790 kcal · P94/C437/F74 · “73 → 81.694 kg · by Thu 25 Mar” | 2,790 kcal | Gain weight, 2,790 kcal · Goal 73 → 81.7 kg |
| **Maintain (clean commit)** | skipped (no screen) | 2,490 kcal · P84/C390/F66 · “73 → 73 kg · by Thu 26 Nov” | 2,490 kcal | Maintain weight, 2,490 kcal · Goal 73 → 73 kg |
| **Maintain (today's commit: stale 67.204)** | skipped | 1,990 kcal · “73 → 67.204 kg · by Thu 11 Feb” (server sees a loss) | 1,990 kcal | Maintain weight, 1,990 · Goal 73 → 67.2 kg |
| **Eat healthier (recorded stale 70.102)** | skipped | **1,990 kcal · P81/C275/F63 · “73 → 70.102 kg · by Thu 31 Dec” — matches `t3m04s` exactly** | **1,990 kcal — matches `t3m28s`** | **Eat healthier, 1,990 kcal · Goal 73 → 70.1 kg — matches `t8m12s`** |
| **Eat healthier (clean commit)** | skipped | 2,490 kcal · “73 → 73 kg · by Thu 26 Nov” | 2,490 kcal | Eat healthier, 2,490 · no target |

Two mismatches are visible in the table: the target screen's date (113 days) versus the plan's date (0.5 kg/wk), and the target screen's kcal (light activity + pace) versus the plan's (chosen activity ± fixed deficit). The recording confirms both (1,710 vs 1,990; 12 March vs 31 Dec).

#### 13a. Definite display and data fixes (no product decision)

1. **Three-decimal weight — confirmed.** Cause: server string from raw floats (`plan.js:55`) + unrounded `targetKg` commit. Fix: round `target_weight_kg` to 0.1 kg at commit and format `goal_line` to 1 dp on the server. No schema, no formula change. Affects only display of the same target.
2. **Stale target after a goal change — confirmed** (profile frame + code). Cause: `v2CommitMap` always sends `targetKg`; the server derives intent from the target, so Eat healthier/Maintain can carry an old deficit. Fix: send `target_weight_kg = weight` for Maintain and `null` for Eat healthier; server guard: goal `maintain`/`healthy` ⇒ maintenance calories regardless of any target (defensive for existing rows). This is a data-cleanup + guard, not a new formula. No migration.
3. **Date format — confirmed display inconsistency.** Cause: the plan pill prints `goal_line · by Thu 31 Dec` from the server (`V2Onboarding.kt:1000-1006`), while the target screen prints `BY 12 MARCH`. Fix: one formatter for both (e.g. `12 Mar`); does not change the date value. No approval needed for the format itself.
4. **Profile Goal row when there is no target change — resolved by investigation.** Hide the Goal row when `weight == target` (Maintain/Eat healthier clean) so no meaningless "73 → 73" line appears. This is part of the stale-data cleanup, not a design choice.
5. **Not confirmed / not changed:** "Built around your eat-healthier goal" is correct for the healthy goal; the Home kcal target does equal the plan row today.

#### 13b. Proposed shared maths or backend changes — **needs Sameer's approval**

**b1. Persist the chosen pace (`users.pace REAL` + migration + `/profile` accepts it + reset clears it + commit sends it).**
- Why: the plan cannot reproduce the chosen pace from the goal word; today it uses a fixed ±500/300.
- Affects: all users once they save a profile; the live server; user 1 only on their next onboarding/profile save; existing plan rows stay until then.
- Fallback: `pace IS NULL` ⇒ today's ±500/300 and 0.5 kg/wk, so no existing plan changes on deploy.
- Smallest alternative: **no schema** — the server infers the rate from `|target − weight| / 16.1 weeks` (the target was projected from the pace), and falls back if the inference is out of range. Fragile if the weight later changes; adequate for the recorded flow.
- Approval needed before building.

**b2. kcal from the chosen pace (`tdee ± round(rate×7700/7)`), keeping the sex minimum.**
- Why: the target chip and the plan disagree (1,710 vs 1,990).
- **Caveat found in code:** with b2 alone they still disagree, because the target chip is computed before Activity exists and always uses the default light activity, while the plan uses the finally chosen activity (`flowFor` order; video: light 1,710 → active 1,990). Sameer's instruction keeps Activity after Diet; moving it before Target is therefore not available. So kcal alignment is a product decision:
  - (i) treat the target chip as illustrative (it already says "Illustrative figures only") and let the plan be the only authoritative kcal; relabel or remove the chip. Smallest, no backend change.
  - (ii) keep the chip and accept that it is provisional (it changes meaning after Activity).
  - (iii) persist pace and show the plan value everywhere the user can see a final target (Home/Profile/Plan), never calling the chip final.
- Affects: the plan kcal for everyone on save; user 1 on next save; fallback as in b1.
- Approval needed (bundle with b1 and the decision row in §8).

**b3. Align the date.**
- Why: 12 March (target) vs 11 Feb/31 Dec (plan).
- Smallest alternative (no schema): make the target screen display the server formula `today + ceil(|delta|/0.5) weeks`, or make the server return `today + 113 days` for onboarding targets. The first needs no backend; the second needs a small `plan.js` change.
- With b1, the date can be `today + ceil(|delta|/pace)` weeks, which equals 16.1 weeks for targets set by this flow.
- Affects: the plan date string for everyone; fallback unchanged.
- Approval needed.

**Do not fold 13b into 13a**; they are separate changes and 13b waits for the decision.

**Test (after approval)**
- Re-run the table for all four paths on a reset user; assert Target date = Plan date (format and value), plan kcal = Home kcal = Profile kcal, one-decimal weights, no Goal row for healthy/maintain, no stale target after changing goal mid-flow.
- Backend unit tests for `computePlan` with/without pace; a curl check of `goal_line` decimals; a profile-save check that `target_weight_kg` is null/weight as specified.

---

### Issue 14 — Camera-off message is unreadable and badly placed

**Status:** SETTLED (placement via mock C)

**Found** [V]
- `QuietNudgeRow` (`Home2.kt:756-830`) is a hairline row under the bubble (`:349`), above the tracker. It draws Ink2 `#3A2F25` on the dark home gradient with a teal action — near-zero contrast (`annotated/issue-14-zoom2x.png`).
- It is the wrong shape/place: mock C is a cream card just above the streak card.
- Note: `ReminderNudgeCard` (`:391-532`) is unrelated dead code. **The v1 plan proposed deleting it; that is removed from v2.** It stays untouched.

**Change**
- Remove `QuietNudgeRow` and its call site only; replace with the shared permission card (16/17) placed above `WeekStrip`. Camera logic untouched.
- No other cleanup.

**Test**
- Night Home: the faint line is gone; the new card's text ≥4.5:1 contrast (cream card). Grep confirms no `QuietNudgeRow` references.

---

### Issue 15 — Remove the "Eaten anything lately? Add a meal." bubble

**Status:** STATED

**Found** [V]
- `bubbleText` returns that string when `kcal_eaten <= 0.0` (`Home2.kt:533-536`); the bubble renders in the top row (`:328-331`). kcal-left bubbles are different states.
- Frames `issue-15`, annotated item 2.

**Change**
- Do not compose the bubble when `kcal_eaten <= 0.0`; keep Neelu in the row (right-aligned) so the card does not jump. Keep `Bubble` and the tail for the other states.

**Test**
- Zero-meal Home: Neelu, no white bubble. After one meal: kcal-left bubble returns. 2.0 font, night theme.

---

### Issues 14/16/17 — one rotating permission row

#### Issue 16 — order and content

**Status:** SETTLED

**Found** [V]
- Only the camera hint exists (above). Permissions are declared (`AndroidManifest.xml:6,7,9`: CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS) but requested only inside the logging flows.
- Approved order: notifications → camera → microphone → rate/feedback, one row at a time, 4 dots (mock C).

**Change**
- New `PermissionRow` composable (in `Home2.kt` or a new `ui/HomePermissionRow.kt`): cream card, 36dp tinted icon disc, 15sp semibold title, 11.5sp muted subtitle, maroon action pill, 4 dots.
- Candidates in order, filtered by need:
  1. Notifications — SDK≥33 and `POST_NOTIFICATIONS` not granted (reminders are on after onboarding).
  2. Camera — `CAMERA` not granted.
  3. Microphone — `RECORD_AUDIO` not granted.
  4. Rate/feedback — once, until dismissed.
- Show only the first candidate; after a grant/resolve/dismiss advance; persist `perm_row_done_<key>` so nothing resurfaces. Dots show the fixed four slots.
- Icons: `ic_bell`, `ic_scan_camera`, `ic_mic`; a small star path for rate.
- Deny/dismiss behaviour and the rate action are in the decision row (§8/Q1), not decided here.
- No other Home layout change beyond inserting the row (issue 17).

**Test**
- Clean install: notification row → prompt → camera → mic → rate; Android 12 skips notifications; restart honours states; already-granted skipped.
- List which permissions were granted in the test notes (issue 16's verification asks for this list).

#### Issue 14 (above) — readability/placement covered by the same card.

#### Issue 17 — placement, wording, tip

**Status:** placement SETTLED; wording NOT approved

**Found** [V]
- The approved empty band is between the calorie card and the streak card; the code already has `Spacer(Modifier.weight(1f).heightIn(min = 0.dp))` (`Home2.kt:355`) creating it. The old row sits above the tracker.
- The mock C row sits just above the streak card, with dots.

**Change**
- Insert `PermissionRow` after the spacer and before `WeekStrip`, same 16dp margins; keep the spacer so the empty band remains.
- Remove the old row (14). No other element moves.
- **Proposed copy (NOT approved; the mock's text is an example):**

  1. Notifications — “Meal reminders need notifications” / “Allow notifications so Neelu can nudge you at your meal times.” / “Allow”
  2. Camera — “Scan meals with your camera” / “Allow camera access to log a plate in seconds. Choose “Allow only while using the app”.” / “Allow”
  3. Microphone — “Log meals by voice” / “Allow microphone access to describe a meal. Choose “Allow only while using the app”.” / “Allow”
  4. Rate/feedback — “Enjoying Mor?” / “A quick rating helps us make it better.” / “Rate”

**Test**
- 320/480dp, day/night, 1.0/2.0: contrast, no subtitle clipping, row exactly once above the streak, empty band unchanged, dots correct through the sequence.

---

### Issue 18 — Calorie card is not edge to edge

**Status:** STATED

**Found** [V]
- `Tracker` is wrapped in `Box(Modifier.padding(start = 16.dp, top = 16.dp))` and capped at `widthIn(max = h2ScreenW() * 0.78f)` (`Home2.kt:663-688`). Frames measure 586/720 vs the streak's 696/720.
- `WeekStrip` uses 16dp horizontal padding (`:919-925`), i.e. it is already full width.

**Change**
- `Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, top = 16.dp))`; card `fillMaxWidth()`; remove `widthIn`. Keep internal padding/spacing; donuts stay `SpaceBetween`; water row spans.
- Do not touch the streak or buttons.

**Test**
- 320/360/411/480dp: card edges match the streak (±0–1dp); no unexpected wraps; donuts evenly spaced.

---

### Issue 19 — Home layout needs to be better

**Status:** covered by 14–18

**Found** [V]: the unused band and misplaced row are issues 14–18; nothing else stated.

**Change:** none beyond 14–18; keep the spacer; do not move other elements. Visual diff against mock C; leave unasked differences listed, not fixed.

---

### Issue 20 — Voice: a phrase said once is repeated many times

**Status:** STATED

**Found** [V]
- `VoiceSection` (`NextLog.kt:427-500`) has one `transcript` string. `onPartialResults` appends the latest (cumulative) partial to it (`:454-462`: `transcript = listOf(transcript, part)…joinToString`); `onResults` appends again. The repeated string is what Stop sends to `runSearch` (`:571`) and what the results header shows (frames `t5m20s–t5m36s`).
- Real bug, no ambiguity.

**Change**
- Separate `settled` and `partial`; display `(settled + " " + partial).trim()`.
- On partial: replace if the latest starts with the previous partial (cumulative vendor); else append (incremental vendor).
- On results: append only if not already contained; clear partial. Reset both on a new recording.
- Send the displayed string on Stop.
- Extract `mergeVoiceText(settled, partial, latest, isFinal)` as a pure function + unit tests.
- UI unchanged.

**Test**
- Unit tests for the two sequences (paneer/rice/eggs/water; chicken/rogan josh/biryani/lassi): every word once in the display and the sent text.
- Device: say each phrase; live text = spoken words once; header = live text.

**Risk:** vendor partials differ (cumulative vs incremental); the rule handles both; test on emulator and the Xiaomi.

---

### Issue 21 — Voice: spoken foods are dropped from the results

**Status:** STATED

**Found** [V]
- Voice and text call `POST /api/foods/ai-search` with the whole sentence (`NextLog.kt:120-145`). The server prompt asks for 3–5 candidates for the whole string, explicitly treating several foods as one meal (`gemini.js` `SEARCH_PROMPT`).
- Live checks (re-run today against the deployed server):
  - `paneer butter masala rice eggs and water` → `Paneer Butter Masala with Rice & Eggs` (720), `… with Rice` (583), `…` (265), `Boiled Egg` (77); **water dropped**.
  - `chicken biryani and lassi` → `Chicken Biryani and Sweet Lassi` (608), `Chicken Biryani` (450), `Sweet Lassi` (158), `Salted Lassi` (100); lassi present this time (model variance).
  - `chicken biryani` → five variants of the same dish (see 23).
- `POST /api/voice-log` already parses one row per food (verified: paneer/rice/eggs → 3; chicken biryani/lassi → 2 [V]).
- Real bug: no per-food decomposition; results depend on the model's choice.

**Change**
- New `POST /api/foods/ai-search-groups`:
  1. `parseTranscript(query)` → items (existing VOICE_PROMPT, ≤5).
  2. Plain water is not a food group (question in §8).
  3. For each item, `searchDishes(item.name)` in parallel; parse candidates.
  4. Return `{ query, groups: [{ label, items }], water }`; fallback: a group with no candidates uses the parsed item as its single candidate (no invented food).
  5. One food ⇒ one group (client fast path).
- Client: voice and text use groups; photo keeps the existing single-meal path.
- `/ai-search` stays for photo/fallback.
- Calorie values and model unchanged.

**Test**
- Backend unit tests for the grouping helper (cap, water, fallback); curl Examples A/B: every food has a group.
- App: typed and spoken A/B; each food listed; one candidate set per food.
- Frames `t5m28s`, `t5m32s`, `t6m28s`, `t6m36s`.

**Risks:** ≤5 parallel model calls → ~5–8s; per-group failure isolated; prompt variance. Water/UI decisions in §8.

---

### Issue 22 — Several foods in one sentence give one logged food

**Status:** STATED — layout options only

**Found** [V]
- `logPick` (`NextLog.kt:147-180`) posts one candidate as one meal with a one-item list; the Logged line says "+X from the <name>"; frames show only Chicken Biryani (407) logged.
- The DB/API already accept multiple items per meal (`items_json`; the legacy voice/scan path uses it) [V].

**Options (Sameer picks)**
- **A. Grouped sections (recommended).** One labelled block per food, 3–5 bubbles each, one pick per food; sticky “Log N items · X kcal”.
- **B. One food at a time.** A stepper (1/3 …) with a pick per step, then Log all.
- **C. Combo-first.** Whole-meal combos first, with an “or pick individually” section.

**Implementation after choice (A assumed):**
- `groups` + `selection: Map<label, FoodItem>`; single group keeps one-tap logging; multi-group requires a pick per group.
- Log one `POST /meals` with `items` = selected items, `name` = joined names (≤40) or “N-item meal”, source `ai`.
- Logged sums kcal and names the meal; meal detail lists N items.
- No backend logging change.

**Test**
- A/B with one pick per food → N items in the meal, totals = sum, Home macros move, meal detail lists them; single food unchanged.

**Risk:** long screen; partial selection disables Log.

---

### Issue 23 — Repeated-looking food options with different calories

**Status:** SEEN — verified as variants

**Found** [V]
- Live curl `chicken biryani` returns `Chicken Biryani` 450 (`1 plate (300g)`), 600 (`1 plate with 2 pieces…`), 340 (`1 cup`), 420 (`1 serving`), 180 (`100g`); paneer likewise. Different portions, not duplicates.
- The bubble (`NextLog.kt:379-415`) shows name, kcal and match only; `portion` is hidden. Real presentation defect.

**Change**
- Show the portion: second line `~450 kcal est. · 1 plate (300g) · Closest match` (wrap allowed).
- Deduplicate identical `name+kcal+portion`; never merge different kcal silently.
- Server already defaults a missing portion to “1 serving” [V]; no client invention.

**Test**
- Curl the video queries; screenshot: distinct portions visible, no identical duplicates; 320dp wrap.

---

### Issue 24 — Remove coins

**Status:** STATED

**Found** — all UI occurrences [V]
- `Scan.kt:1118-1119` — Logged "+N coins" pill.
- `Home.kt:222` — quick-add toast "… · +N coins".
- `Settings.kt:220` — "🔥 N-day streak 🪙 N coins".
- `Progress.kt:392` — challenges header "🪙 N coins".
- `Progress.kt:430` — claim toast "+N coins!".
- `Progress.kt:440` — claim button "+N".
- `Progress.kt:443` — uncompleted challenge reward "🪙N".
- `Progress.kt:483` — badge modal "+50 coins".
- `Progress.kt:494` — badge toast "… +N coins".
- Backend coins/awards exist (`routes.js` [V]); models carry the fields.

**Change**
- Remove all coin text/icons at those lines:
  - Logged: remove the pill and its slot.
  - Quick-add toast: “Logged N kcal to <slot>”.
  - Settings header: streak only.
  - Challenges header: no coin pill.
  - Claim: “Collect” (no amount); claimed “Done”; no pre-claim reward text. (**Resolved by investigation**; no decision needed.)
  - Badge modal: no “+50 coins” pill; “Badge collected”.
- Leave backend coin fields/awards untouched (no UI reads them). No streak/ring changes.

**Test**
- `grep -ri coin` in app UI returns nothing user-visible.
- Walk all logging paths, profile, challenges, badge: no coin text/icon/toast; claims/awards still complete.

---

### Issue 25 — Logged screen disappears too fast

**Status:** STATED

**Found** [V]
- Code: `LoggedScreen` auto-navigates after `delay(2600)` (`Scan.kt:1065`); the Home destination enters with `slideInVertically(tween(300)) + fadeIn(tween(220))` (`MainActivity.kt:191`). So **Logged composition → Home visible ≈ 2.6s + 0.3s = 2.9s** (plus one frame). `Done` works at any time (`Scan.kt` buttons).
- Recording: first Logged frame at **6:44**, Home by about **6:48** → **≈4s**, but the frames are 1s apart and the compressed video includes the pre-navigation tap and encoding; treat this as a sanity range, not a stopwatch.
- **Which is more reliable:** the code (deterministic 2.6s delay + 0.3s transition). The recording cannot resolve below 1s and includes user latency.
- Recording frames 6:44 vs 6:45: the coin pill appears between Neelu and “Logged!”, and the title/bar/labels move down slightly; the macro lines appear between “+407 from the Chicken Biryani” and the buttons.

**What causes the movement (code)** [V]
- The reward pill is conditionally composed at 520+620 = **1.14s** and replaces a **25dp** spacer (`Scan.kt:1111-1123`: `if (rewardIn) Box(...) else Spacer(25.dp)`). The pill is a 12sp text (~14–15dp line) plus 6dp vertical padding ≈ **26–28dp**, so when it replaces the 25dp placeholder everything below shifts by ~1–3dp.
- The macro rows are **always composed** and only animate `alpha` (`Scan.kt:1180-1195`), so they add no height and cannot push content in this build. The fan image is inside a fixed 150dp box. The only conditional height is the pill.
- If the frames suggest a larger move, the mechanism is still the pill vs its placeholder; measure on device, since the 1s frames cannot separate a few dp from compression artifacts.

**Change**
- New auto-advance delay = **current measured code time + five seconds**:
  - Delay: 2,600ms + 5,000ms = **7,600ms**.
  - To Home visible: 7,600 + 300ms transition = **≈7.9s**.
  - If the code-timing interpretation is not what Sameer meant and the recording's ~4s is the baseline, the equivalent is ~9.0s; the code baseline is the one proposed.
- Issue 24 removes the coin pill and its placeholder entirely ⇒ no insertion, no shift.
- Keep every delayed element occupying its final space from frame one (the macro rows already do; verify; never make one conditional without reserving its height).
- No other Logged layout change.

**Test**
- Stopwatch from Logged appearing to Home: 7.5–7.9s; Done exits immediately.
- Screenshots at 0.5s and 2s: no vertical movement of title/bar/macros/buttons.
- Frames `t6m43s–t6m48s`, side-by-side annotated.

---

### Issue 26 — Quick add does not update protein, carbs, fat

**Status:** STATED — "should it" is open

**Found** [V]
- Quick add posts source `quick`, name `"Quick add"`, one item with name/kcal/portion only (`Home.kt:209-220`). No macros.
- The server sums macros from items (`routes.js:194-240` [V]); missing ⇒ 0. Frames: 1,125→1,725→2,325 with rings 63/40/52 unchanged. Confirmed cause; no macro data exists at that point.

**Options**
- **A. Calories-only, made explicit (recommended).** Keep the data; add “Adds calories only — protein, carbs and fat stay unchanged.” No invented macros.
- **B. Food-backed quick add.** Add opens the AI/search chooser (or presets) so real macros exist; “quick” becomes 2 taps + lookup.
- **C. Manual macros.** Optional protein/carb/fat fields in the sheet.
- (A fixed kcal split would invent data; not proposed.)

**Change:** blocked on §8/Q4. A is copy-only; B/C change the flow.

**Test (A):** ×4 quick add → +600 kcal, macros unchanged, note visible, meal detail shows 0 macros. B/C: rings/totals change by the real/entered values.

---

### Issue 27 — Macro rings drop to 0% and restart on updates

**Status:** STATED

**Found** [V]
- `DonutTile` creates `Animatable(0f)` for sweep and number on each composition (`Home2.kt:852-906`; animatables `:854-856`).
- Sweep and number run **sequentially**: the number starts only after the 1100ms sweep (`:865-866`), so an arc can be at 63% while the text shows 0% (`t7m20s`).
- Home is disposed while Logged/Add/Profile is on top; returning recreates the tiles from 0 and re-runs the entrance (`t7m24s`, `8m00`), then the correct values (`t7m36s`, `8m04`).
- The server always returns macro-goal fallbacks (`routes.js:122-127` [V]) and `vm.home` is replaced only with a full response, so a 0-goal response is unlikely; the remount/sequence is the dominant cause. Exact frame timing is not instrumented; the mechanism is proven by code.
- Real bug.

**Change**
- Hoist the last-shown percentages into `AppVm` (`macroPct: SnapshotStateMap<String, Float>`).
- Tiles start from the remembered value (0 on a cold start), animate ring and number **concurrently** over the same duration/delay, then write the final value back.
- If `goal <= 0`, keep the last value and skip the animation.
- Reset the memory on user reset.
- Keep look/colours/staggering.

**Test**
- Log a meal → numbers and arcs move together from the previous values, no 0 flash; open/close Add, go to Profile and back, quick add → no reset; cold start from 0 is allowed.
- Frames `t7m20s`, `t7m24s`, `t7m32s`, `t7m36s`, `8m00`, `8m04`.

---

## 4. Cross-cutting data flow

1. Onboarding: `journey → [body, goal, (target), diet, activity, meals] → building → plan → home` (no all-set).
2. Target: pace controls numbers and the curve; if 13b is approved, the pace is persisted and the plan owns the date/calorie truth.
3. Logging: transcript merge → parse into foods → per-food candidate groups → one meal with N items → Logged → Home.
4. Home: full-width calorie card; no zero-meal bubble; one permission card; rings animate from memory; coins gone; quick-add decision.

---

## 5. Build order

**Phase 0 — baseline**
1. Reset user 2; install the current build; record before screens for all 26 issues; confirm devices and font scales.

**Phase 1 — data truth (13a; 13b after approval)**
2. 13a fixes (rounding, stale target, format, Profile row) + tests.
3. If approved: 13b pace/maths, migration, deploy, table re-run.

**Phase 2 — onboarding shell**
4. Issue 1 (card height only) → 4 (CTA size) → 12 (all-set removal) → 2 (avatar gap) → 11 (confetti) → 5 (curve) → 6 (chai cup).

**Phase 3 — time picker**
5. Issues 9 → 8 → 7 verification.

**Phase 4 — Home**
6. 18 (full width) → 15 (bubble) → 27 (rings) → 14/16/17 (permission row after Q1) → 26 (after Q4) → 24 (coins).

**Phase 5 — logging**
7. 20 (transcript) → 21 (groups) → 22 (after Q3) → 23 (portions) → 24 (Logged/toast) → 25 (timing).

**Phase 6 — position**
8. Issue 10: produce 60/68/75 screenshots; implement the chosen fraction.

**Phase 7 — regression/release**
9. Four goal paths × four logging paths; day/night; 320/411/480; font 1.0/1.3/2.0; backend tests; Android unit tests; rebuild debug/release; update the report; no commit/PR without instruction.

Dependencies: 1 before 4/12 layout tests; 12 before the flow tests; 21 before 22; 24 before 25's layout check; 9 before 7/8; 13b and Q1–Q4 gate their phases.

---

## 6. Risks

1. **Issue 1 card rework** changes the card top per step; the peel is screen-level, but the incoming card's top differs from the outgoing one; the header seam appears (stated in issue 1). Tall steps at 2.0 need the scroll.
2. **Issue 12** changes the end of the flow/back stack; test rotation and a stale `stepKey`.
3. **Issue 13a** server guard changes plan kcal for any profile whose goal and target disagree; deploy carefully; fallback for 13b.
4. **Issue 13b** would touch the DB/live server; no deploy without approval; existing plan rows persist until save.
5. **Issue 21/22** adds parallel AI calls and model-dependent output; cap at 5 foods, isolate failures, show latency.
6. **Issue 9** three cards at 320dp/2.0; AM/PM typing; unit-pill omission.
7. **Issues 14/16/17** OS permission outcomes vary; rate action undefined; deny/dismiss behaviour pending.
8. **Issue 24** removes challenge reward indicators; the backend still awards coins invisibly.
9. **Issue 27** shared animation memory across Home instances; reset on account switch.
10. **Issue 26** blocked on a product decision; do not invent macros.
11. **Issue 25** duration assumes "five more" applies to the code's 2.6s; the recording's ~4s would give a longer total.

---

## 7. What I could not check without running the app / server

- Live app values for the four paths (the table is code-traced + formula execution, not a UI run).
- Issue 7's 18:00→6:00 AM flip (chip defect proven; flip unverified).
- Issue 27's exact frame-by-frame cause (remount vs any transient 0-goal response); the code mechanism is proven.
- Issue 25's on-device duration and the exact pixel movement in 6:44→6:45 (frames are 1s and compressed).
- Which APK Sameer installed (debug/release); both match.
- The permission row's rate action and deny/dismiss behaviour (not defined anywhere).
- Large-font behaviour of the 16sp CTA and the permission card on the Xiaomi.
- Model output stability for per-food groups (the design is not built; current `/ai-search` variance was observed).
- Issue 9's three-card layout at 320dp/2.0 (not built).

---

## 8. Decisions for Sameer (short table; nothing here is decided)

| # | Decision | Options | Recommendation (reason) | Blocked until answered |
|---|---|---|---|---|
| Q1 | **Permission row: wording + rate action + deny/dismiss** (issues 14/16/17) | (a) Approve the four proposed rows as written; rate opens the Play in-app review when available, else the Play listing; denied permission advances for the session and returns next launch; 4 dots. (b) Same, but rate opens a feedback email/form and adds “Not now”. (c) Keep the row on deny with a Settings link. | **(a)** — matches mock C's one-row/one-action shape, and in-app review needs no new screen; “Allow only while using the app” appears in the OS prompt. | Final strings, action and behaviour of the row; phases 4/7. |
| Q2 | **“Building your plan” card position** (10) | 60% / 68% / 75% of screen height (current ≈50%). | **68%** — visibly lower than today without crowding the bottom edge or the failure button. | Final constant; screenshots for release; phase 6. |
| Q3 | **Several foods in one sentence — layout** (22) | A grouped sections + “Log N items”; B one food at a time; C combo-first. | **A** — one scroll, one pick per food, one meal with N items; the DB/API already support multi-item meals. | The chooser UI and multi-item logging; 21/22; phase 5. |
| Q4 | **Quick add and macros** (26) | A calories-only + an explicit note (no invented macros); B food-backed quick add; C manual macro fields. | **A** for this pass — the sheet has no food data; B/C change the flow and need real data. | Issue 26 implementation; phase 4. |
| Q5 | **Approve issue 13b (pace/back end)** | (a) Full: persist pace + migration + plan kcal/date from pace. (b) Smallest: infer the rate from the target/horizon, fixed/horizon date, no schema. (c) Display-only: label the target kcal as illustrative, align only the date format. | **(a)** if we want the plan to match the pace and the activity choice; **(b)** if no migration is acceptable (fragile if weight changes); **(c)** if the target chip may stay provisional. Note b2 cannot align kcal fully because Activity is chosen after Target by design. | Any backend maths deploy; the issue 13 table acceptance; phase 1. |
| Q6 | **Issue 9 card details: AM/PM and the unit pill** | (a) Three `WheelCard`s; the AM/PM card omits the “Tap to type” caption/keypad; all cards omit the unit pill (it is a unit switcher with nothing to switch). (b) Same, but show a static unit pill “hr”/“min” (a small `WheelCard` change: draw the pill when `onUnit` is null). (c) 24-hour hour wheel in two full cards (no AM/PM; the pill can stay as a static unit marker). | **(a)** — keeps the 12h rows and the mock's three columns, uses the wheel/card/label/band/fonts/keypad unchanged, and avoids a dead control. **(b)** if the pill's presence matters more than it being interactive. | The time sheet; phase 3. |
| Q7 | **Water in voice/text results** (21) | (a) Plain water is not a food group; optionally offer “Add a glass of water”; (b) include water as a food candidate; (c) drop and ignore. | **(a)** — water is tracked separately and “water, 0 of 8” already exists; drinks like lassi stay foods. | The groups endpoint behaviour; phase 5. |

**Resolved by investigation — not asking:**
- Issue 1: bottom-anchored content-height card so the CTA stays at the bottom; the cream stage (not the video) is what shows; the header's lower edge becomes the seam.
- Issue 9: 12-hour + AM/PM, 1-minute steps (mock neighbours 58/59/00/01), centre tap opens the existing `ObKeypad` for hour/minute, unit suffix “hr”/“min” from the component. The unit pill and the AM/PM caption/keypad are **not** resolved — see Q6.
- Issue 12: no replacement for the reminders message; the permission row covers the off state, Settings shows the state.
- Issue 13: hide the Goal row when weight == target; “Built around your eat-healthier goal” is correct.
- Issue 24: challenge claim “Collect”, claimed “Done”, no amounts; badge toast without coins.
- Issue 25: new delay 2,600 + 5,000 = 7,600ms; Home visible at ≈7.9s (code baseline).
- Issue 23: show the portion and dedupe identical rows.
- Issue 6: chai cup per the mock. Issue 10 recommendation is still his. Issue 26 A is still his.

---

## 9. Explicitly not changed

Intro assets, palace art, colours, type, Home art/mood, water logic, scan API, notifications/insights/streak/challenges logic (except coin UI), meal rows, settings, `ReminderNudgeCard` (left untouched), and the backend except the possible issue 13b and the proposed groups endpoint (not built, not deployed).

No code, asset, branch or server was changed while producing this file.
