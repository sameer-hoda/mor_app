# Mor next version — status (overnight run)

Branch: `mor-next` (uncommitted). Pack: `Mor next version - implementation pack (2026-11-18)`.

## Isolation
- `applicationId = dev.mor.app.next`, `versionName = 0.2-next` — installs beside the live app.
- Backend: the existing EC2 server (`http://15.135.140.52:4000`), as a **separate test user** (`x-user-id: 2`). The live app (user 1) is untouched.
- Reset test user: `curl -X POST -H "x-user-id: 2" http://15.135.140.52:4000/api/reset`
- Fresh onboarding: `adb shell pm clear dev.mor.app.next`

## Done + verified on the emulator (items 1–8 of the pack)
1. **Intro**: door retimed to ~2s (asset trimmed 0.6s + 1.7× playback), reveal untouched at 1×, hard cut.
2. **Order**: About you → Objective → Target (reduce/gain only) → Diet → Activity → Meal times. Maintain/Eat-healthier skips Target (5 steps instead of 6).
3. **Objective**: Lose / Maintain / Gain / Eat healthier with the pack subtitles.
4. **Target**: fixed computed horizon date, pace cards (0.18/0.36/0.54 kg/wk), delta + kcal chips, exact rising/falling curve, axis labels, "Illustrative figures only."
5. **Diet**: Vegan, Vegetarian, Eggetarian, Non-veg in the pack order.
6. **Chrome**: fixed fan header, `STEP n OF m` badge + segments, tip bubble, serif title, pinned maroon button.
7. **Transition**: peel-with-curl (0.55s, origin 12% 100%); incoming card static; header/frame fixed.
8. **Plan build**: court.png backdrop, floating card, teal tick rows cascading at 0.55s.

Also fixed: meal reminders were hardcoded off; now on when any meal slot is on, and the "You're all set" copy reflects it.

Verified flow: gate → door → reveal → greeting → TLDR → About → Objective → Target → Diet → Activity → Meal times → Building → Plan ready → All set → Home.

## Builds
- `~/Desktop/Mor-next-logging-debug.apk`
- `~/Desktop/Mor-next-logging-release.apk` (signed with the tester keystore)
- Install: `adb install -r ~/Desktop/Mor-next-logging-debug.apk`

## Notes / interpretations
- Activity and Meal-times steps are kept after Diet (they feed TDEE/plan) and counted in the badge — the pack's S2 lists only 4 steps.
- The client Target screen shows the pack's fixed 16-week illustrative horizon; the server plan uses a 500 kcal/day deficit (~12 weeks). Target weight agrees; the target screen says "Illustrative figures only."
- Server deploy this session: `routes.js` (ai-search, recent foods, insights macros, idempotent log), `scanCsv.js` (candidate parser), `gemini.js` (web-grounded search), plus the intent-based `plan.js` and the `client_key` migration in `db.js`. Backend restarted via `systemctl restart mor-backend`; all 40 backend tests pass locally.
- Intro clips in the repo are video-only (no audio stream); sound stays generated.

## Done after the first pass (items 9–11)
9. **Macro gauge tiles**: three full-ring donuts (Protein blue / Carbs gold / Fat teal), percent in the hole, "n of goal g" beneath, animated sweeps.
10. **Home layout**: Neelu now stands beside the tip bubble (64dp) instead of hanging over the cards.
11. **Quiet nudge row**: one hairline row, camera-off state with "Allow" (permission request), reminders-off and tip states with dismissal that never returns.

## Done in the second pass (items 12–18)

### 12. AI + web food lookup (S12) — server deployed
- New backend endpoint `POST /api/foods/ai-search` (`backend/src/gemini.js` `searchDishes`, Google Search grounding on `gemini-2.5-flash`) returns 3–5 candidates, best match first, each with kcal/macros/portion and a plain-word match label.
- Deployed to EC2 (`gemini.js`, `routes.js`, `scanCsv.js`; `plan.js`, `db.js` from main too) and verified live: `2 masala dosa` → 4 candidates, `dhokla, 2 pieces` → 3.
- The logging paths no longer read the static food catalog: Add-meal → Search/Speak, camera "Search food instead"/mic, and failed-scan buttons now all open the new flow.
- Every nutrition figure is labeled `est.` ("~700 kcal est.").

### 13. Voice pill (S13) — verified
- Idle: one centered maroon pill, mic mark + "Tap to speak".
- Recording: the pill expands to a bar with a 14-bar pulsing waveform, running timer, explicit **Stop**. `SpeechRecognizer` auto-restarts on end-of-utterance/error, so silence never ends the recording.
- Verified: recording ran 24s+ with no cutoff; Stop with no speech shows "Nothing was heard…".

### 14. Text field + gold Search (S14) — verified
- Plain field (`e.g. dhokla, 2 pieces`) + explicit gold Search button; nothing lists while typing.

### 15. Rising-bubble chooser (S15) — verified
- 3–5 white bubbles (16/16/16/4 radius, sand border), name + "~N kcal est. · Closest match / Also likely / Possible", rising 14dp with 0.22s stagger.
- Nothing is written until a bubble is tapped; the tap logs that candidate and opens the celebration.
- Same flow for text, voice and photo (photo scans route through the vision read, then the same chooser via `nextlog?...&auto=1`).

### 16. Logged celebration (S16) — verified
- Perched Neelu, "Logged!", kcal/left line, See details / Done.
- Day bar: existing teal progress, then a gold segment grows by the meal's share (~900ms), "+700 from the Masala Dosa" label, macro lines tick in at 0.2s.
- At 2.6s the updated home slides in over it (nav enter transition) with totals, gauges and streak already updated.
- Item 18 beat 2 folded in: hop → fan opens inside a maroon/gold Red Court frame → "+5 coins" lands.

### 17. Stacked-day trends (S17) — verified
- Week view: one 18dp stacked bar per day on a 100dp `#F3EAD3` track, bottom-up fat → carbs → protein, each segment proportional to its share of that macro's target, 0.12s per-day stagger.
- Day with no logs renders as an empty track (visible gap); future days too. Tapping a day shows color-bordered macro chips ("Protein 16g" etc.).
- Backend `/api/insights` now returns per-day `protein_g/carbs_g/fat_g` and `logged`; deployed and verified.

### 18. Neelu behaviours (S18) — proposed defaults, staged motion
- **Idle**: the real `neelu_cut.png` sits directly on the UI with a slow breathing bob and a soft contact shadow.
- **Meal logged**: the three-beat hop → fan → coins sequence above.
- **Behind on water** (14:00–20:00, water < goal): a small tilt+bob waddle and the water track answers with a gold glow.
- **Late evening, nothing logged** (21:00–05:00): the stage dims and Neelu settles (slower bob, dimmed alpha, "sleep" mood).
- These are whole-artwork motion studies, as the pack allows: no articulated walk rig or tail animation. Open to adjustment.

## Verification captures (emulator)
`/tmp/nx6-bubbles2.jpg` (chooser), `/tmp/nx6-logged.jpg` (celebration), `/tmp/nx6-homeafter2.jpg` (updated home), `/tmp/nx6-trends.jpg` (stacked days), `/tmp/nx6-voice3.jpg` (recording), `/tmp/nx6-camera4.jpg` (photo path).

## Remaining / notes
- Home height was tightened (number 64sp, 60dp donuts, short action labels) so tip + hero + streak + actions fit one screen on a 731dp-tall device.
- Play: still needs HTTPS before production (`usesCleartextTraffic` is on) and a bumped `versionCode` per upload.
- The `mor-next` branch remains uncommitted.
