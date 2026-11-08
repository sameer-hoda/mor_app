# SUPPORT-02 — previously requested fixes: status in codebase

Legend: DONE = implemented, PARTIAL = partly, MISSING = not present.
Design for the requested end state; flag overlaps where the palace
direction touches the same surface.

- Notification permission flow — PARTIAL. Notify screen exists with an
  in-app Allow/Don't-allow dialog that POSTs `reminders_on` (onboarding.jsx:353).
  No real `Notification.requestPermission` call exists in `app/src`.
  Design must keep the same taps (Yes/Not now/Allow/Don't allow -> /home).
- Steps permission flow — MISSING. `home.steps` renders read-only
  (Home.jsx:181); no permission request or step-counter code in `app/src`.
- Food search matching — DONE (baseline). Case-insensitive LIKE variants
  with ranking, LIMIT 20; Verified rows first with pill, AI-estimate
  fallback, unsure-alternative swap on Result (routes.js:250-275,
  food.jsx Search + Result pickAlt).
- Larger food database — DONE (baseline). 6,826 USDA seed rows plus
  user/AI-added rows over time.
- Quantity/servings logging — DONE. FoodItem unit chips + 0.5-5 stepper
  (food.jsx:224-241); Search cart logs with portions; Result logs portions.
- Insights week/month views — PARTIAL. `week`/`month` Segmented exists but
  only the 7-day view renders (progress.jsx:21-67). Designing the month view
  is in scope; it must show real data.
- Voice logging reliability — DONE (baseline). Web Speech en-IN dictation +
  POST /voice-log + chips + Add items (food.jsx:132-174). Keep the flow.
- MeetNeelu screen centering — DONE. Already a centered column
  (onboarding.jsx:68-85). Preserve.
- Consistent slider styles — PARTIAL. Target slider and weight slider both
  use `accentColor teal` range inputs but with different min/max/steps and
  different surrounding cards (onboarding.jsx:203, progress.jsx:115).
  Unify the skin, not the values.
- Vegan above Vegetarian ordering — MISSING. Diet order is Vegetarian,
  Eggetarian, Non-veg, Vegan (onboarding.jsx:117-125). Design for Vegan
  second (after Vegetarian), i.e. the requested end state.
- Water/steps cards at top of Home — MISSING. They render in a 2-col grid
  BELOW Today's meals (Home.jsx:169-185). Skin plan must move them to the
  top in the requested end state — flag as layout change for owner sign-off
  (positions move, no route/tap changes).
- Notification badge rendering — DONE. Bell with kumkum dot when
  `home.unread>0` (Home.jsx:73-78); per-item unread dots + Mark-all-read in
  Inbox (settings.jsx:26-44). Skin only.
- Toast redesign — OPEN. Only one toast system exists (Snackbar, bottom
  92px, ok/err). No prior redesign found; any redesign is new skin.
- Coins tab renamed to Challenges with a fire icon — MISSING.
  Navbar tab label is still "Coins" with a plus glyph (ui.jsx:134-140);
  the screen itself is titled "Challenges". Skin plan must rename the tab
  to "Challenges" with a fire icon — flag as label/icon change for sign-off.
- Target-weight consistency — DONE (baseline). Target slider, Plan card,
  and Profile Goal row all read `target_weight_kg`/`plan` values.
- Preserve meal photo scan flow — intact per SUPPORT-01 (Camera persisting
  pendingScan through Scanning/Result/Logged). Skin, don't restructure.
- Profile as-is — intact per SUPPORT-01. Tokens only.
