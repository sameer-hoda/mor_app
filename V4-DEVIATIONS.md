# v4 Enhancement Package — Deviations & QA Report

All 9 items from `mor-v4-enhancements/CHANGE-LIST-v4.md` implemented + per-item emulator QA
(`mor-pixel`, 1080×1920, real server 15.135.140.52:4000). Build: `android/app/build/outputs/apk/debug/app-debug.apk`,
`:app:assembleDebug` + `:app:testDebugUnitTest` (17 tests) green. Evidence: `/tmp/qa44–qa92.png`.

## Bugs found & fixed during QA
1. **NeeluVideoBackground crash** — `IllegalArgumentException: Cannot coerce value to an empty range:
   maximum 0.0.dp is less than minimum 381.89.dp` (NeeluVideoBackground.kt). The TLDR sheet slide-in
   reports `positionInRoot` below the screen → `minOffset > 0` → empty range. Fix:
   `rawOffset.coerceIn(minOf(minOffset, 0.dp), 0.dp)`.
2. **kcal chip ignored the pace** — `planKcal` hardcoded −500/+300, so Gradual/Faster showed Steady kcal.
   Fix: `tdee + pace.dailyDelta` (−300/−500/−700 lose, +150/+300/+450 gain), rounded to 10 like the server.
   Verified: Gradual 1,723 · Steady 1,523 → 1,520 after rounding · Faster 1,323.
3. **Reminders defaulted ON** — `v2CommitMap` never sent `reminders_on`, so the server default (1) left
   Profile showing "4 on" while the all-set screen claimed off. Fix: `v2CommitMap` sends `reminders_on = false`
   (spec 07: default off, no permission prompt). Verified: Profile → Reminders "Off".
4. **Ruler scrub regression** — earlier refactor dropped the `scrubX` state; restored (chart scrub +
   caption + haptic).
5. **lb keypad** — weight keypad in lb mode kept kg range/decimal text; fixed (77–353 lb, integer-only).

## Deviations (reported, not invented around)
1. **Server plan is pace-unaware** — `pace_idx` is sent by the client but the users table has no column
   and the server (15.135.140.52) can't be migrated/restarted from here. `backend/src/plan.js` was rewritten
   this session to take intent from target-vs-current (delta < −0.25 lose −500, > 0.25 gain +300, else
   maintain; floors 1500/1200; macros from target; `target_date`, `goal_line`, `ambitious`, `milestone` all
   target-driven), but **kcal stays −500/+300 regardless of pace** and **`target_date` uses 0.5 kg/wk**
   (plan card says "by Sat 6 Feb" where the client hero says 7 Mar at Steady). Needs a `pace_idx` column +
   deploy to match the client exactly.
2. **Client kcal chip ≠ server for Gradual/Faster** until (1) is deployed: chip 1,723/1,323 vs server plan
   1,520/1,520 (steady parity for Steady users: chip 1,520 ≈ plan 1,520 ✓).
3. **Building screen look** — black lower region, Pankh-green ticks, Ink "Skip": the existing design, left
   as-is because spec 07 says "The plan builds (unchanged)".
4. **Role.Slider dropped** on the ruler (doesn't exist in Compose 1.7); `stateDescription` + Increase/Decrease
   customActions (0.5 kg) carry the accessibility contract.
5. **Water label auto-size**: 15sp → 13sp → "+ Add water" (TextMeasurer). On the 411dp emulator it shows
   "+ Add water" (spec-correct fallback); at reference width the full label fits.
6. **Date mismatch client vs server** (same as 1): hero/chip dates are pace-driven; server `target_date`
   rounds 0.5 kg/wk.
7. **Reminder card after first meal** (spec 07 §5) exists in code (`ReminderNudgeCard`) but wasn't exercised
   in QA — needs a logged meal to appear.
8. **Backend changes not deployed** — plan.js rewrite lives in source only.

## Review pass (post-QA)
- Code review found 2 nits, both fixed: (a) the kcal-chip "rounded to 10" comment vs actual integer
  floor — now true round-half-up (`(raw + 5) / 10 * 10`) to match `Math.round(kcal/10)*10`; (b) accidental
  line collapse in `backend/src/db.js` `CREATE TABLE users`.
- Final `:app:assembleDebug` + `:app:testDebugUnitTest` green. APK installed on both `emulator-5554`
  and the physical phone `IRXOQGWCJR4PVWLB`; server profile reset for a clean onboarding.

## Still open
1. Backend deploy (plan.js + client_key migration) to 15.135.140.52 — source only.
2. Reminder nudge card (spec 07 §5) not exercised — needs a logged meal.
3. Phone run-through pending on the user's device.

## QA matrix (screenshots)
- Splash/hold/iris qa45–47 · cream boot qa50 · TLDR focal qa61 · goal/diet video band qa62–65 ·
  S1 pills qa66 · target ruler+scrub qa67–69 · pace chips qa74–79 · activity qa80 · meal times qa81 ·
  building qa82 · plan ready qa83, qa90 · all set (no reminder question) qa84, qa90 → Home qa85, qa92 ·
  water qa86 · add sheet qa87 · profile strings + reminders-off qa88, qa91 · repeat launch → Home qa44, qa92 ·
  back-from-Home exits (launcher resumed).
