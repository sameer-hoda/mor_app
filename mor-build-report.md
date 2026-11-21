# Mor next version — build report

- **Branch:** `mor-next` at `20a040d` (uncommitted working tree)
- **Build:** `dev.mor.app.next`, `versionName 0.2-next`, `versionCode 1`
- **APK:** `/Users/sameerhoda/Desktop/Mor-next-final-debug.apk` (61 MB)
  - SHA-256 `82f29fac517568f64fdedec48708c522bd676b74d139abe321187d5bc5ab6efb`
- **Spec:** `/Users/sameerhoda/Downloads/FINAL build pack/01-BUILD-SPEC.md` (`01-BUILD-SPEC.md` used over `03-issue-evidence.md`)
- **Date:** 2026-11-20
- **Tests:** `:app:testDebugUnitTest` — 21 tests, 0 failures, 0 errors.
- **Device:** emulator `emulator-5554` (1080×1920, 420dpi). Phone not connected; not run on a physical device.

---

## 0. The 413 image-payload failure and workflow fix

### Symptom
The session stopped on reading `/tmp/v2-53-confetti-clear.jpg`:

```
{"code":413,"message":"Downloaded image content cannot exceed 30MB",
 "metadata":{"error_type":"payload_too_large"}}
```

### Diagnosed cause (evidence)
This was **not** a per-image limit:

- The failing image is `157,447` bytes, `720×1280` JPEG. The other recent captures are `56–176 KB`, `720×1280` JPEG.
- All 64 `/private/tmp/v2-*.jpg` captures total `7.80 MB` on disk. The whole `/private/tmp` image set (154 files) totals `47.80 MB` raw (~64 MB base64-est), still above the per-request budget when accumulated.
- The OpenCode DB for the session (`ses_e2b4058f2ffeJW6S6AzOS50PPg`, `~/.local/share/opencode/opencode.db`) showed:
  - 816 `read` tool parts, ~401 MB total.
  - 418 `read` parts containing `data:image`, ~394 MB; largest single part ~4.55 MB.
  - 280 `read` parts containing PNG base64, ~370 MB.
  - DB size 4.1 GB.
- This matches the documented upstream behaviour: a **per-request 30 MB image-content budget** reached through accumulated history images (same wording reported in upstream issues).
- OpenCode docs (`/docs/compaction`): older conversation is replaced by a summary, roughly the recent 15k tokens are kept, “attachment data is replaced by a short description”, and “earlier messages remain stored even when they are no longer sent to the model.”
- After the latest automatic compaction (`2026-11-20 01:33:21 UTC`) the active context had only 4 image parts / 64,369 bytes, while the session still stored 433 image parts / ~396.5 MB. Requests after that compaction succeeded. So compaction removes images from the **request**, while ordinary “compress one screenshot” does not clear accumulated history.

### Prevention added (project-local, app behaviour unchanged)
1. **`scripts/mor-shot.sh`** — capture or prepare one preview:
   - captures via `adb` or `--prepare <input>`; preserves the full-resolution original in `~/.mor-evidence/originals`;
   - writes a compressed JPEG preview to `~/.mor-evidence/previews`;
   - budget `MOR_MAX_PREVIEW_BYTES=180000` (conservative, below the 30 MB request limit);
   - validates JPEG bytes and dimensions, tries smaller widths/qualities, and **exits 32 without a preview** if no candidate fits. Prints `PREVIEW`, `BYTES`, `DIMS`, `BUDGET`, `ORIGINAL`.
2. **`scripts/mor-session-image-budget.sh`** — rollover guard:
   - measures only the active context (parts after the latest compaction), not the stored backlog;
   - WARNING at 20 MB, CRITICAL at 28 MB;
   - prints the manual compaction command, e.g.
     `opencode api post /api/session/<id>/compact --data '{}'`.
3. **`AGENTS.md`** — workflow rules: one relevant preview at a time; no repeated unchanged images; logs/UI text for non-visual checks; actual image inspection for layout; never call a visual issue verified from text alone; checkpoint `/tmp/mor-progress.md` after each issue; keep originals outside tracked source; do not embed images in reports.
4. **Checkpoint:** `/tmp/mor-progress.md` records branch, spec, changed files, completed checks, remaining work and this failing step.

### Tests of the fix
| Input | Original | Preview | Result |
|---|---|---|---|
| failing capture `v2-53` | 157,447 B, 720×1280 | 85,911 B, 540×960 | fits; inspected |
| recent capture `v2-54` | 158,303 B, 720×1280 | 86,083 B, 540×960 | fits; inspected |
| synthetic oversized input | 46 MB, 4000×4000 PNG | 46,242 B, 540×540 | reduced and inspected |
| rejection path | 157,447 B input, budget 1,000 B | none | exit 32, no sendable preview |

Previews were inspected **one at a time**:
- `~/.mor-evidence/previews/20261120-071153-failed-413.jpg` — Plan ready, no confetti, 2,840 kcal / 93 g / 453 g / 73 g, pill `72 → 80.7 kg · by 25 Mar`.
- `~/.mor-evidence/previews/20261120-071154-gain-plan-413.jpg` — same Plan ready state.
- `~/.mor-evidence/previews/20261120-071154-oversized-413.jpg` — valid 540×540 preview of the synthetic input.

Budget guard after those reads: `12` active image parts / `455,377` bytes — `OK` (below 20 MB).

### Compaction / rollover status
- Automatic compaction is supported and enabled by default in OpenCode; the latest automatic compaction is the evidence that it removes old image payloads from the request.
- Manual compaction is available at `opencode api post /api/session/<id>/compact --data '{}'`.
- **Limitation:** this is a workflow-level fix, not a permanent provider-side change. The guard must be run before image-heavy work; if the 413 recurs, stop and compact/rollover immediately. Ordinary image compression alone does not clear accumulated history.
- **No coding-tool config change was applied.** No global config, provider, model or subscription was changed. No `opencode.json`/`opencode.jsonc` was added or edited.

---

## 1. Git status

### At the start
Branch `mor-next`, HEAD `20a040d`, dirty working tree.
Modified: `android/app/build.gradle.kts`, `MainActivity.kt`, `data/Models.kt`, `data/MorApi.kt`, `data/Service.kt`, `ui/Home.kt`, `ui/Home2.kt`, `ui/HomeBackground.kt`, `ui/MorDesign.kt`, `ui/OnboardingScaffold.kt`, `ui/Progress.kt`, `ui/Scan.kt`, `ui/Settings.kt`, `ui/V2Onboarding.kt`, `ui/WelcomeJourney.kt`, `res/raw/journey_c1.mp4`, `backend/src/gemini.js`, `backend/src/routes.js`, `backend/src/scanCsv.js`.
Untracked: `NEXT-VERSION-REPORT.md`, `ui/NextLog.kt`, four `drawable-nodpi` PNGs, `drawable/ic_snack_chai.xml`, test directory, `docs/superpowers/`, `mor-solution-spec*.md`.

The `backend/` working-tree diffs pre-existed this task (19 Nov deploy work). No backend file was changed, deployed, restarted or rolled back in this task.

### At the end
Same branch and HEAD; no reset/stash/checkout/revert/commit.
Added for the 413 workflow fix: `AGENTS.md`, `scripts/mor-shot.sh`, `scripts/mor-session-image-budget.sh`.
App diff end state: `19 files changed, 1505 insertions(+), 825 deletions(-)` (tracked files; untracked files not counted). `BUILDING_CARD_FRAC` was restored to `0.68f`; the temporary `MorCta` debug log in `WelcomeJourney.kt` was removed.

---

## 2. Issue status (in-scope)

| Issue | Status | Files changed | Acceptance result / evidence |
|---|---|---|---|
| 1 — Intro card height | Done at 411×731 | `OnboardingScaffold.kt`, `WelcomeJourney.kt`, `V2Onboarding.kt` | Content-height card with art visible above; header joined; body scrolls; CTA inside. `v2-47-about`, `v2-49-cta`, `v2-02-tldr`, `v2-03-tldr2`, `v2-04-about`. 320 dp / font 2.0 / all steps not run. |
| 2 — Avatar gap | Done | `OnboardingScaffold.kt` | 10 dp gap above label; `v2-02-tldr`, `v2-03-tldr2`. |
| 4 — CTA text size | Done at 411×731 | `OnboardingScaffold.kt` | 16 sp, single line; `v2-49-cta`. 320 dp / 2.0 not run; 15 sp fallback not needed at tested width. |
| 5 — Pace-responsive target curve | Done | `V2Onboarding.kt` | Three paces visibly differ; end dot moves; `v2-10-target`, `v2-10b-target-gradual`, `v2-10c-target-faster`, `v2-50-gain-target-faster`. |
| 6 — Snack icon | Done | `V2Onboarding.kt`, `res/drawable/ic_snack_chai.xml` | Chai cup visible on Meals; other icons untouched; `v2-51-meals`. |
| 7 — Snack editor/row times | Partly done | `V2Onboarding.kt` | One slot saved as 5:01 PM and row matched; opening did not change it. All four rows, 12/24 h, 320 dp and 2.0 not run. `v2-14-timesheet`, `v2-15-saved`. |
| 8 — Save hidden | Partly done | `V2Onboarding.kt` | `skipPartiallyExpanded = true`; Save pinned and visible at 411×731; `v2-14-timesheet`. 320×640 and 2.0 not run. |
| 9 — Clock picker option B | Partly done | `V2Onboarding.kt`, `WheelPicker` reuse | Three wheel cards, maroon centre rows, faded neighbours, Save visible; `v2-14-timesheet`, `v2-14b-hour6`, `v2-15-saved`. Conversion verified for 17:01 → 5:01 PM. Full 12 AM/12 PM/1 PM/11:59 PM matrix, 320 dp, 2.0 and TalkBack labels not run. |
| 10 — Building card position | Partly done | `V2Onboarding.kt` | Constant `0.68f`; 68% screenshot `v2-16-building` shows card over peacock with checklist visible. 60% and 75% comparison captures not obtained: the injected draft state made the system splash overlay linger, so captures showed the splash, not the card. Not faked. |
| 11 — Confetti freezes | Done | `V2Onboarding.kt` | Mid-flight `v2-52-confetti-mid`; clear `v2-53-confetti-clear` inspected through the new preview helper (no dots remain). |
| 12 — Remove “You’re all set” | Done | `MainActivity.kt`, `V2Onboarding.kt` | Screen/route removed; Plan ready button is “Go to Home”; `v2-17-plan`, `v2-36-gain-plan`, `v2-53`. Back from Home, rotation and stale saved step not run. |
| 13 — Plan data consistency (app-side only) | Partly done | `V2Onboarding.kt`, `Models.kt`, `MorApi.kt`, `Service.kt` | Four paths in section 3. Weights ≤ 1 decimal; no pill/Goal row for Maintain/healthy; target rounded before send. Stale-target request inspection not run. Date/kcal mismatch remains (13b out of scope). |
| 14/16/17 — Rotating permission row | Partly done | `Home2.kt`, `Home.kt` | `PermissionRow` replaces `QuietNudgeRow`; order notifications → camera → microphone verified with prompts; dots advance; `v2-20-notifperm`, `v2-21-cameraperm`, `v2-23-micperm`, `v2-19-home-compact`. Rate row, 320/480 dp, night and 2.0 not run. `ReminderNudgeCard` untouched. |
| 15 — Zero-calorie bubble | Done | `Home2.kt` | Bubble hidden while `kcal_eaten == 0`; `v2-19-home-compact`. |
| 18 — Calorie card edge to edge | Done at 411 | `Home2.kt` | Tracker `fillMaxWidth()` + 16 dp sides; edges match streak; `v2-19-home-compact`. 320/360/480 not run. |
| 19 — Home layout | Covered by 14–18 | — | No separate change; spacer kept, other elements not moved. |
| 20 — Repeated voice phrase | Done (unit) | `NextLog.kt`, `MergeVoiceTest.kt` | `mergeVoiceText` settled/partial/final handling; 4 unit tests pass. Device voice re-run not done. |
| 21 — Spoken foods dropped (single-food only) | Done (findings + hint) | `NextLog.kt` | Findings in section 5. Hint “Log one food at a time for the best match.” `v2-27-results`. Multi-food remains deferred; 320 dp / 2.0 hint not run. |
| 23 — Repeated-looking options | Done / partly | `NextLog.kt` | Portion shown on bubble; dedupe on `name+kcal+portion`; `v2-27-results`. 320 dp wrap not run. |
| 24 — Remove coins | Done / partly | `Scan.kt`, `Home.kt`, `Settings.kt`, `Progress.kt`, `Home2.kt` | Logged pill/toast, Settings header, Progress header pill, claim toast, badge pill/toast and `🪙` branch removed; `v2-28-logged`, `v2-31-quickadd`, `v2-29-profile`, `v2-30-homeback`. Challenge/badge device paths not all re-run. Backend coin fields untouched. |
| 25 — Logged screen too fast | Partly done | `Scan.kt` | Delay `7600 ms`; target ~7.9 s to Home. Observed tap 23:39:21 → still Logged 23:39:29 → Home by 23:39:34 (Logged→Home ~7.9 s). 0.5 s / 2 s no-shift screenshots and ±0.3 s stopwatch precision not captured. |
| 26 — Quick add macros | Done | `Home.kt` | Note “Adds calories only. Protein, carbs and fat stay the same.”; +150 kcal, macros unchanged; `v2-31-quickadd`. |
| 27 — Ring animation from zero | Done / partly | `Service.kt`, `Home2.kt`, `Settings.kt` | `macroPct` memory; returning from Profile shows values with no 0% flash; `v2-29-profile`, `v2-30-homeback`. Reset clears map in code; reset path not separately device-verified. |

Out of scope and not built: issues 3, 22, 28, 29; the multi-food part of 21; issue 13 plan maths/DB/server guard; `ReminderNudgeCard` cleanup; any new feature/refactor.

---

## 3. Issue 13 table — all four goal paths

| Path | Target screen | Plan ready | Home | Profile |
|---|---|---|---|---|
| Lose, Steady | **Seen on device:** `BY 12 MAR · 66.2 kg · −5.8 · 1,770 kcal` (`v2-10-target`) | **Seen on device:** `2,060 kcal · P76/C304/F60 · 72 → 66.2 kg · by 11 Feb` (`v2-17-plan`) | **Seen on device:** `2,060` (`v2-18-home`) | **Seen on device:** `Lose weight · 2,060`; Goal `72 → 66.2` (`v2-29-profile`) |
| Gain, Faster | **Seen on device:** `BY 13 MAR · 80.7 kg · +8.7 · 2,700 kcal` (`v2-50-gain-target-faster`) | **Seen on device:** `2,840 kcal · P93/C453/F73 · 72 → 80.7 kg · by 25 Mar` (`v2-36-gain-plan`, `v2-53`) | **Not run** | **Seen on device:** `Gain weight · 2,840`; Goal `72 → 80.7` (`v2-37-gain-profile`) |
| Maintain | **Skipped by design (seen):** Target step skipped | **Seen on device:** `2,540 kcal · P83/C406/F65`; no goal pill (`v2-41-maint-plan`) | **Not run** | **Seen on device:** `Maintain weight · 2,540`; no Goal row (`v2-42-maint-profile`) |
| Eat healthier | **Skipped by design (seen):** Target step skipped | **Seen on device:** `2,540 kcal`; no goal pill (`v2-45-healthy-plan`) | **Not run** | **Seen on device:** `Eat healthier · 2,540`; no Goal row (`v2-46-healthy-profile`) |

Notes:
- Weights show at most one decimal (66.2, 80.7).
- No goal pill / Goal row for Maintain and Eat healthier.
- Date style is now one `d MMM` pattern on target and plan, but the target renders it uppercase (`BY 12 MAR`) and the plan lowercase (`by 11 Feb`). The date **values** still differ because of the excluded target-vs-server maths.
- Target-screen kcal and plan kcal still differ (1,770 vs 2,060; 2,700 vs 2,840). This is the known issue 13b difference; no server/plan maths change was made.
- “Change goals mid-flow and inspect the request for a stale target” was **not run**.

---

## 4. Issue 25 measured durations

| Point | Duration |
|---|---|
| Before (code baseline) | `delay(2600)` + 300 ms Home enter ≈ **2.9 s** from Logged to Home visible |
| After (target) | `delay(7600)` + 300 ms Home enter ≈ **7.9 s** |
| After (observed) | Tap 23:39:21 → still Logged 23:39:29 → Home by 23:39:34; Logged→Home ≈ **7.9 s** |

`v2-28-logged`, `v2-28b-logged-later`, `v2-28c-after` show no coin pill/placeholder and no vertical shift. The acceptance’s 0.5 s / 2 s comparison screenshots and a ±0.3 s stopwatch reading were **not captured**.

---

## 5. Issue 21 findings (single-food scope)

Exact queries and what the app showed:

1. `chicken biryani` — returned 5 variants with distinct portions: `1 plate (300g)`, `1 plate with 2 pieces`, `1 cup`, `1 serving`, `100g`. A single food was **not** dropped. Each bubble shows name, kcal estimate, portion and match label; one pick logs one candidate.
2. `paneer butter masala rice eggs and water` — candidates for the combined paneer/rice text plus egg; `water` was dropped by the whole-sentence lookup. The app does **not** group per food; one pick logs only the picked candidate. Nothing on screen says the other foods were logged.
3. `chicken biryani and lassi` — candidates included lassi; again no per-food grouping.

Data-integrity result: **no data-integrity failure observed** (nothing was shown as logged that was not). The limitation is that a multi-food sentence is treated as one search; that part remains deferred. The hint “Log one food at a time for the best match.” is shown in the text/voice chooser (`v2-27-results`). No new backend route and no new server call were added.

---

## 6. Assumptions used / changed

| Issue | Assumption | Status |
|---|---|---|
| 9 | 12-hour wheels with AM/PM, 1-minute steps; no unit pill; AM/PM has no typing; hour/minute keep centre-tap + `ObKeypad` | Used; no change |
| 10 | Card centre at 68% (one constant); 60/68/75 screenshots | 68% shipped; 60/75 **not captured** |
| 12 | No replacement for the “reminders are on” message | Used; no change |
| 14/16/17 | Copy from the issue text; rate opens Play review/listing; no feedback form; deny advances for the session | Used; rate path not device-tested |
| 21 | One hint line | Used; no change |
| 24 | Challenge “Collect” then “Done”, no amounts; badge toast “Badge collected” | Used; not all device paths re-run |
| 25 | New delay = code baseline 2,600 + 5,000 = 7,600 ms | Used; measured ~7.9 s |
| 26 | Calories only, with a one-line note | Used; no change |
| 4 | 16 sp; 15 sp only if it does not fit at 320 dp / 2.0 | 16 sp at 411×731; 320/2.0 not run |

---

## 7. Not checked / blocked / limitations

**Not run on a device:**
- Widths 320/360/480 dp and font scales 1.3/2.0 across the app.
- Night Home and permission-row contrast at 2.0.
- All four meal rows and the full 12 AM / 12 PM / 1 PM / 11:59 PM conversion matrix; TalkBack labels.
- Rate row and Play review path; challenge/badge device paths; ring-memory reset path.
- Back from Home, rotation, and stale saved onboarding step.
- Issue 10 60%/75% comparison captures (see issue 10).
- Issue 25 0.5 s / 2 s no-shift screenshots and ±0.3 s stopwatch precision.
- Physical phone (not connected); Xiaomi permission behaviour.

**Blocked by the no-server-change rule:**
- Issue 13 plan maths, `users.pace` migration and `plan.js` guard.
- Issue 22 / multi-food handling and its per-food lookup route.
- Issues 3, 28, 29.

**Server statement:** no server source file was changed, deployed, restarted or rolled back in this task. The pre-existing `backend/` working-tree diffs were not touched. The only data operation was the documented test-user reset `POST /api/reset` with `x-user-id: 2`, used for onboarding verification; no schema, production or user-1 data was changed.

---

## 8. Files changed

App/build:
- `android/app/build.gradle.kts`
- `android/app/src/main/java/dev/mor/app/MainActivity.kt`
- `.../data/Models.kt`, `MorApi.kt`, `Service.kt`
- `.../ui/OnboardingScaffold.kt`, `WelcomeJourney.kt`, `V2Onboarding.kt`
- `.../ui/Home.kt`, `Home2.kt`, `HomeBackground.kt`, `MorDesign.kt`
- `.../ui/Progress.kt`, `Scan.kt`, `Settings.kt`
- `.../ui/NextLog.kt` (new)
- `.../ui/HomePermissionRow` logic inside `Home2.kt`
- `.../res/drawable/ic_snack_chai.xml` (new)
- `.../res/raw/journey_c1.mp4`
- `.../test/java/dev/mor/app/ui/MergeVoiceTest.kt` (new)

413 workflow fix (new):
- `AGENTS.md`
- `scripts/mor-shot.sh`
- `scripts/mor-session-image-budget.sh`

Evidence (not tracked):
- Originals: `~/.mor-evidence/originals/`
- Previews: `~/.mor-evidence/previews/`
- Existing captures: `/private/tmp/v2-*.jpg`, `/private/tmp/nx-*.png`
- Checkpoint: `/tmp/mor-progress.md`

---

## 9. Build and test

- `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./gradlew :app:testDebugUnitTest :app:assembleDebug`
- `BUILD SUCCESSFUL`; 21 tests, 0 failures/errors (MergeVoice 4, FoodMatch 11, PlanText 6).
- Final APK built at `android/app/build/outputs/apk/debug/app-debug.apk` and copied to `~/Desktop/Mor-next-final-debug.apk`.
- Existing APKs (`Mor-next-logging-*`, `Mor-next-onboarding-*`) were not overwritten.
- Nothing was committed, pushed or opened as a PR.
