# Phase 2 Deviation Report — Home / Profile / Trends / Notifications / Log-a-meal

Emulator QA complete (fresh journey → home, all surfaces exercised). Build: `android/app/build/outputs/apk/debug/app-debug.apk`.
Everything below was verified on the emulator (`mor-pixel`, 1080×1920 @2.625) with the real server (15.135.140.52:4000).

## Verified working tonight
- Full journey → home on fresh state; quick-add ×4 (single-flight guard, count-up, server round-trip, coins), water logging, mark-read (badge 3→2), Trends stats live (150/1/1), BACK closes profile → trends → notes correctly, reduced-motion pass clean.
- Evidence: `/tmp/mor-q-17…37*.png` (home q-26, trends q-27, notes q-28, toast q-34-2, add sheet q-37).

## Bugs found & fixed during QA
1. **All three H2 sheets rendered half-height** — M3 `ModalBottomSheet` defaults to partially-expanded, clipping the tail (quick-add bar was measured at 0px and never composed; Trends Close was cut). Fix: `rememberModalBottomSheetState(skipPartiallyExpanded = true)` on Add/Trends/Notes sheets. Existing `MorSheet` (Components.kt) untouched.
2. **Neelu anchored inside the tracker card** covering the caption. Prototype uses a zero-height anchor *after* the card (`.neelu{right:4px;top:-14px;width:184px}`), so the bird hangs below the card. Fix: aspect-aware offset `y = 78 + 92·aspect` (Neelu paints Fit-centered in a square box); spacer after the tracker got `heightIn(min = 150.dp)` per `.space`.
3. **Water glass icon rendered solid dark** — `Icon`'s default `LocalContentColor` tint overrode the vector's own colors. Fix: `tint = Color.Unspecified`.
4. **"+ Add glass of water" wrapped to 2 lines** (Roboto wider than the prototype's SF at 15px). Fix: 14sp label / 16sp "+", maxLines=1 — matches the reference's one-line look (see deviations).

## Deviations vs prototype (reported, not invented around)
1. **Entry points lost**: Insights, Challenges, Streak detail, Coins no longer link from Home (kit decision). They still exist in the app but Profile doesn't link them either (per reference `profile.png`). Decide later whether to surface them in Profile.
2. **Toast**: prototype meal toast is `name · N kcal` + a "View" link (opens meal detail, 4.2s), water has **no toast** (segment fills + aria-live; at 8/8 the bubble changes to "All 8 glasses! Proper hydrated."). App uses the global `MorToast`/snackbar: quick-add shows "Logged 150 kcal to Dinner · +5 coins" (server coins included), water shows "Water logged · N/8". The toast also renders higher (over the week strip) than the prototype's bottom-anchored pill. No meal-detail "View" link.
3. **Water goal bubble**: app does not switch the bubble to "All 8 glasses! Proper hydrated." at 8/8 (prototype does). Check on the phone.
4. **Trends stats**: per-day glasses aren't available from the API, so the stat trio is avg kcal / logged days / day streak (prototype's third stat differs).
5. **Notifications**: flat list (prototype groups TODAY/YESTERDAY); sheet open does not auto-mark-read — per-row tap marks read (badge decrements correctly).
6. **Profile**: rows without backing screens (Daily target, Diet, Units) are static, no chevron; the decorative "Your data lives on your private Mor server" card was dropped; no Insights/Challenges rows.
7. **Fonts**: Roboto/Noto Serif vs the prototype's SF/serif — Roboto runs wider, hence the water-label 15px→14sp compensation. Otherwise token-exact (colors, radii, sizes from the kit).
8. **Neelu render detail**: square box + ContentScale.Fit vs CSS `height:auto` → painted art ±5% from CSS intent (imperceptible at 184dp).
9. **Add sheet content is scrollable** (`verticalScroll`) as a short-screen safety net; at reference size everything fits without scrolling.
10. **hdr_*.jpg no longer used** (old Home header art deleted along with HomeScreen/Banner/StepsCard — steps counter removed per kit).

## Tomorrow (phone IRXOQGWCJR4PVWLB)
```
adb -s IRXOQGWCJR4PVWLB install -r android/app/build/outputs/apk/debug/app-debug.apk
```
Then walk: journey → home → water → quick-add → trends → profile → notifications, and compare against the kit references. Emulator retains demo state (750 kcal, 2 glasses) for side-by-side comparison.
