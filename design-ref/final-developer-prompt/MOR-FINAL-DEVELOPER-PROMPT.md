# Mor design elevation - final prompt for your developer LLM

You are a senior mobile product designer and motion designer advising on Mor, an Indian-first calorie-tracking app with a peacock mascot called Neelu. This is a context-alignment stage only: do NOT generate screen images, exports, code or animations yet. First we agree on the current baseline and on how the proposed design direction maps onto it.

Attached alongside this prompt are verified supporting files from the repo owner. Treat them as ground truth. Where they disagree with any candidate description below, the supporting files win.

- SUPPORT-01-verified-baseline.md — the Step 0 baseline already verified against `app/src`. Confirm it by spot-checking the cited files; do not re-derive it from scratch unless you find a discrepancy, which you must flag.
- SUPPORT-02-feedback-status.md — which previously requested fixes are implemented, partial, or missing, with file evidence.
- SUPPORT-03-library-inventory.md — verified contents of the Mor Design Library.

## STEP 0 - CONFIRM THE CURRENT BASELINE (from the codebase, not from this prompt)

Spot-check the app against SUPPORT-01 and write down, before anything else:

The full route list and navigation order. The candidate map is: Splash -> Welcome -> MeetNeelu -> Goal -> Diet -> Body -> Target -> Activity -> MealTimes -> Building -> Plan -> Notify -> Home; meal-logging via AddSheet (/add), Camera (/camera), Scanning (/scanning), Result (/result), Logged (/logged), plus Search (/search), FoodItem (/food), MealDetail (/meal/:id); Insights (/insights), Streak (/streak), Weight (/weight), Challenges (/challenges), Inbox (/inbox), Profile (/profile), Reminders (/reminders). Treat this as a candidate map and correct it against the actual router in `app/src/App.jsx` (27 routes plus the Root redirect; `profile.onboarded` decides `/home` vs `/splash`).
For each screen: its purpose in the flow, its current components, every tappable element and where it leads, and any data it reads or writes.
The current style tokens, typography and mascot assets. Candidate tokens: teal `#0B4A57`, teal-dark `#08343D`, neel `#1F4FB5`, pankh `#19A39A`, panna `#1E8A5A`, genda `#F2A93B`, gold `#F2B544`, jamun `#2D2A6E`, kumkum `#D9483B`, chandan `#F8F5EE`, ink `#142027`; display `Baloo 2`, body `Mukta` (Google Fonts link in `app/index.html`); Neelu PNGs in `app/public/neelu/`, WebM clips in `app/public/neelu-video/` consumed through the `NeeluVideo` component with PNG/SVG fallback. Treat all of these as baseline candidates and confirm what actually exists in the codebase; report anything that differs.

Do not proceed until this baseline is confirmed in writing and any mismatches with SUPPORT-01 are flagged.

## SUPPLIED DESIGN DIRECTION (proposed references, not yet approved or final)

The owner has supplied a reference library - the "Mor Design Library" folder - as proposed visual direction, along with a brief (DESIGN-SECTIONS.md) and an HTML options page (mor-design-options.html) in 00-brief-and-options/. These are his proposals to react to, not settled decisions. Verified layout (see SUPPORT-03):

- 01-originals: his 11 supplied mood images, unedited (includes one duplicate pair).
- 02-organized: the same images sorted his way - 01-logo, 02-palace-door-sequence, 03-onboarding, 04-home.
- 03-pinterest-references: additional references in the same language, one folder per journey: 01-logo, 02-palace-door-sequence, 03-onboarding, 04-home, 05-meal-logging, 06-insights, 07-challenges (see SOURCES.md; direction only, licensing needed before production use).
- JOURNEY-MAP.md (library root): which references are intended for which screen/journey.

The proposed language: "walking into a palace to see the peacock" - kumkum red, sandstone cream, peacock neel/teal, pankh green, marigold gold; scalloped Mughal arches, jharokha windows, feather-eye geometry, fan bloom, fresco borders, gold halo rings; a one-ink feather-fan mark; Indian-miniature-meets-art-deco illustration with gold line work. Never cartoonish, never generic 3D stock.

## PREVIOUSLY REQUESTED FIXES (context, SUPPORT-02 is authoritative)

The owner previously sent a separate feedback list covering: notification + steps permission flows, food search matching, larger food database, quantity/servings logging, insights week/month views, voice logging reliability, MeetNeelu screen centering, consistent slider styles, Vegan above Vegetarian ordering, water/steps cards at top of Home, notification badge rendering, toast redesign, Coins tab renamed to Challenges with a fire icon, and target-weight consistency, plus preserving the meal photo scan flow and Profile as-is.

SUPPORT-02 records which of these the codebase already implements. Preserve any that are. Wherever the proposed design direction touches the same surface (e.g. Home layout, slider styling, tab naming), flag the overlap explicitly instead of silently designing over it, and design for the requested end state.

## CONSTRAINTS

1. Flow preservation is the hard rule: same screens, same order, same taps, same data. No screen merged, split, reordered, renamed or removed; no navigation target changed; no new CTA, route or step added - including on the launch/splash experience, which must fit the existing Splash's exact place in the navigation (whole-page tap to `/welcome`) and its existing tap behavior. If any proposed idea (e.g. a multi-beat launch sequence, an "Enter" call-to-action, arch-framed meal rows) would require a navigation, route, or interaction change, stop and list it as a conflict for the owner to decide instead of designing it in.
2. Use only values verified in the Step 0 baseline for any content shown on screens (chips, targets, streaks, names). Nothing illustrative may be presented as current.
3. This stage produces no code changes, no asset exports, no generated screens.

## WHAT TO PRODUCE NOW (text only, for review)

1. The confirmed baseline (Step 0), with mismatches flagged.
2. A conflict list: every place the proposed direction would change flow, taps, routes, requested fixes, or current content values - each with the options to resolve it.
3. A per-journey skin plan covering EVERY journey - launch, each onboarding step, Home (day + night), the full meal-logging flow (AddSheet / Camera / Scanning / Result / Logged, plus Search / FoodItem / MealDetail where they touch it), Insights, Challenges, and Profile (no redesign requested there) - mapping each to specific references from the library and describing intended surfaces, in words only.

Later stages, only after the owner approves this plan: per-screen static designs, then written motion specs (entrance/exit, hero movement, duration, easing, reduced-motion fallback, CSS/GPU-friendly transforms and opacity), then feeding approved statics + specs to an image-to-video generation model for final animated assets. Keep the model choice open - write specs any capable current image-to-video model could execute.
