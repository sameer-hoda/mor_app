# SUPPORT-01 — verified baseline (app/src, Nov 2026)

Verified file by file. Router: `app/src/App.jsx` (27 routes + Root).
Root: no profile renders nothing; `profile.onboarded` -> `/home`, else `/splash`.
`onboarded` is set server-side when `complete:true` is saved (`backend/src/routes.js:92`).
Onboarding saves each step via `saveProfile` (POST /profile). Home data: GET /home.

## Route list (navigation order)

`/splash` -> `/welcome` -> `/meet-neelu` -> `/goal` -> `/diet` -> `/body` ->
`/target` -> `/activity` -> `/meal-times` -> `/building` (auto) -> `/plan` ->
`/notify` -> `/home`. Meal logging: `/add` (AddSheet) <-> `/camera`,
`/scanning`, `/result?scan_id=`, `/logged`, `/search`, `/food`, `/meal/:id`.
Also: `/insights`, `/streak`, `/weight`, `/challenges`, `/inbox`, `/profile`,
`/reminders`. `*` falls back to Root.

## Screens (purpose, components, taps, data)

- Splash (`onboarding.jsx:27`): teal page, whole page is one tap -> `/welcome`.
  Gold-ringed circle with `NeeluVideo anim="splash"`, "Mor" 44px display,
  "Eat well, the Indian way." No data read/write.
- Welcome (`:41`): 420px mint-gradient hero, floating "590 kcal" thali chip,
  `NeeluVideo wave` 260, wave divider SVG. H1 + sub copy. `Get started` ->
  `/meet-neelu`. "No sign-up needed" caption. No data.
- MeetNeelu (`:68`): TopBar back. Centered column (already centered):
  mint2 circle + `wave` 190, H1 "Hi, I'm Neelu.", sub. `Let's go` -> `/goal`.
- Goal (`:87`): Step 1/6 (`STEP_BG`, StepDots, Continue). 4 option buttons
  (lose/maintain/gain/healthy); tap saves `goal` immediately, Continue ->
  `/diet` (disabled until chosen).
- Diet (`:113`): Step 2/6. 2x2 grid: Vegetarian, Eggetarian, Non-veg, Vegan,
  each with VegMark. Tap saves `diet`. Current order puts Vegan LAST.
- Body (`:139`): Step 3/6. Sex Segmented (Female/Male/Other), Age numeric
  field (invalid under 18), Height with ft/in-cm toggle, Weight kg. Action
  saves `sex/age/height_cm/weight_kg` -> `/target`.
- Target (`:181`): Step 4/6. Big numeric readout, range slider 35..max(80,
  w+10) step 0.5, Realistic/Ambitious pill, "By {date}" pill, "Start with
  {milestone} kg" shortcut when ambitious. Saves `target_weight_kg` ->
  `/activity`.
- Activity (`:219`): Step 5/6. 4 options with bar meters; tap saves
  `activity` -> `/meal-times`.
- MealTimes (`:262`): Step 6/6. 4 slots (breakfast/lunch/snack/dinner) with
  FoodIcon, time chip, Toggle. Tap chip opens Sheet with quick times + time
  input. Toggles drop slots. "Build my plan" saves `meal_times` +
  `complete:true` -> `/building`.
- Building (`:303`): `NeeluVideo think`, fake progress bar, auto-navigates to
  `/plan` at 100%. "Skip" textlike -> `/plan`. Reads `profile.diet/goal`
  for the subline only.
- Plan (`:326`): `dance` video, "Your plan is ready!", DAILY TARGET big
  number (default 1850 when no plan), macro bars (defaults 75/250/60g),
  `goal_line` + `target_date` pill when present. `Start` -> `/notify`.
- Notify (`:353`): `wave` video + bell badge, "Remind you at mealtimes?",
  "Max 4 a day. Never at night." `Yes, remind me` opens an IN-APP dialog
  (not a system prompt) with Don't allow / Allow; both POST `reminders_on`
  and go `/home`. `Not now` posts false -> `/home`. There is NO real
  `Notification.requestPermission` call anywhere in `app/src`.
- Home (`Home.jsx:52`): greeting + inbox bell (kumkum dot when
  `home.unread>0` -> `/inbox`). SVG Scene day/night (sun/moon, palace
  outline, ground). `NeeluVideo` mood by `home.state`. Banner by state.
  Streak diya card -> `/streak`. Kcal card (KCAL LEFT/OVER, target, eaten,
  MacroBars) + Ring FAB (tap opens AddSheet). Meals list: logged slot ->
  `/meal/:id`; unlogged -> `/add`. Water/steps grid at the BOTTOM (glasses
  +1 via POST /water; steps read-only). Navbar. AddSheet: slot segmented
  defaulting to first unlogged slot; Scan/Search/Speak/Gallery options +
  Quick add (150 kcal, POST /meals).
- Search (`food.jsx:11`): close -> `/add`. Text field (auto GET /foods?q),
  voice button, tabs Recent/Saved/Restaurant (visual only, no filtering).
  Rows -> `/food?name=&slot=`; +/- toggles cart (qty fixed 1 here). Voice
  panel: Web Speech `en-IN` dictation + chips, `Add items` POSTs
  `/voice-log`. Cart bar -> POST /meals -> `/logged`. No-match state:
  `Estimate with Neelu` (POST /foods/estimate), `Create custom food`
  (200 kcal placeholder), `Scan instead` -> `/camera`.
- FoodItem (`food.jsx:176`): back -> `/search`. Unit chips
  (Bowl/Plate/Spoon/Grams), qty stepper 0.5-5, oil Segmented
  (0.85/1/1.15 kcal; fat uses 1.25/0.75 respectively). Save = snack toast
  only. `Add to {slot}` POST /meals -> `/logged`.
- MealDetail (`food.jsx:273`): back -> `/home`, delete (DELETE /meals/:id)
  -> `/home`. Photo (scan) or FoodIcon, kcal, health-score pill, macros,
  items -> `/food`, tip row, Done -> `/home`.
- Camera (`scan.jsx:19`): dark page. Mode tabs meal/label/barcode (capture
  path identical). Flash toggle, gallery file input, close -> `/add`,
  search icon -> `/search?slot=`. Denied panel: `droop` video, `Open
  settings` = alert() with instructions, `Try again`, `Search food
  instead`. Shutter -> downscaled JPEG into `pendingScan` -> `/scanning`.
- Scanning (`scan.jsx:160`): plate thumbnail + scanline, CHECKS list
  (Reading dishes / Estimating portions / Counting oil), progress bar,
  Cancel -> `/camera`. POSTs image; success -> `/result?scan_id=`;
  failure -> error snack -> `/camera`. No image -> `/add`.
- Result (`scan.jsx:226`): plate photo + numbered item badges, back X ->
  `/camera`, discard -> `/add`. Failed state: `droop` video, Retake ->
  `/camera`, Search / Speak. Success sheet: meal name + AI-estimate pill,
  unsure-item card with up to 2 alternative buttons + Other (all resolve
  via GET /foods), item rows (unsure rows show em-dash kcal), oil
  Segmented, add-item -> `/search`. `Log {total} kcal` DISABLED while any
  item is unsure; logs POST /meals (unsure items excluded) -> `/logged`.
- Logged (`scan.jsx:373`): coins pill (+awarded, default 10), `hop` video,
  "Logged!", kcal + kcal-left. `See details` -> `/meal/{id}` (guarded if
  no log), Done -> `/home`.
- Insights (`progress.jsx:7`): `week`/`month` Segmented (month has NO
  separate rendering - display is always the 7-day view). Gated behind 3
  logged days (think video, progress bars, `Log a meal` -> `/add`).
  Average card, 7-bar week chart vs goal line, weight card (Log -> sheet,
  POST /weight), protein card. WeightSheet slider 35-150 step 0.1 + Save.
  `/weight` route renders the same sheet on its own screen.
- Streak (`:138`): diya + `flex` video, day count, month calendar (gold
  lit days, teal today ring), freezes (read-only count), Share
  (navigator.share), `Keep it going` -> `/home`.
- Challenges (`:181`): teal header "Challenges" + coins pill, Today/This
  week groups with progress + claim (+coins), Badges -> BadgeModal
  (Protein Pro, share/collect via POST /badges/award). Navbar tab for this
  screen is labeled "Coins" (not "Challenges").
- Inbox (`settings.jsx:16`): title + `Mark all read`, Today/Yesterday
  lists, tap marks read + routes by type (badge->/challenges,
  missed_meal->/add, else /home). Empty state -> `/reminders`.
- Profile (`settings.jsx:68`): teal header (name, streak, coins), local-DB
  note card, YOUR PLAN rows (Goal->/target, Daily target->/plan,
  Diet->/diet), APP rows (Reminders->/reminders, Units), DATA rows
  (Export downloads JSON, Delete all -> GET /api/reset -> `/splash`).
- Reminders (`settings.jsx:141`): off-banner + Turn on, per-meal time
  chips + toggles (POST meal_times), NUDGES toggles are VISUAL ONLY
  (`onChange={() => {}}`), quiet-hours card (10:30 PM-7 AM, Max 4/day).

## Style tokens, type, mascot (`styles.css`, `index.html`, `ui.jsx`)

- Tokens (`app/src/styles.css:1-11`): `--teal:#0B4A57`, `--teal-dark:#08343D`,
  `--neel:#1F4FB5`, `--pankh:#19A39A`, `--panna:#1E8A5A`, `--genda:#F2A93B`,
  `--gold:#F2B544`, `--jamun:#2D2A6E`, `--kumkum:#D9483B`,
  `--chandan:#F8F5EE`, `--ink:#142027`, `--sub:#4A5A63`,
  `--line:#EDE7DA`, `--line2:#D9D2C2`, `--card:#FFFFFF`, `--mint:#E3F2EF`,
  `--mint2:#D3EBE6`, plus amber/green/red/lav pill pairs. Radii 22/28px.
  Display font var `--display:'Baloo 2'`, body `--body:'Mukta'`.
- Type loading: Google Fonts link in `app/index.html:8-10`
  (Baloo 2 600/700/800 + Mukta 400/500/600/700). Offline builds must bundle.
- Components (`ui.jsx`): Btn (FeatherEye badge), Segmented, Toggle (gold
  knob), VegMark, Sheet/overlay, Ring, MacroBars, Field, StepDots, Screen,
  TopBar, Navbar (tabs Home/Insights/Log-FAB/Challenges/Profile; the
  challenges tab is labeled "Coins" with a PLUS glyph icon
  `M12 2v20M2 12h20`, `ui.jsx:134-140`), FeatherEye (gold/teal/jamun rings),
  NeeluImg/NeeluSvg (SVG fallback, 7 moods), NeeluVideo (WebM per anim,
  Safari falls back to PNG; one-shots play once, loops replay x3),
  Coin, Diya (lit/unlit), Snackbar (`ok` ink / `err` red, bottom 92px).
- Assets: PNGs `app/public/neelu/` (9 moods incl. master + chroma ref),
  WebM `app/public/neelu-video/` (11 anims), scene art is INLINE SVG in
  `Home.jsx` (no scene image files used), `app/public/scene/` unused by
  code. `app/src/assets/` mirrors neelu PNGs for bundler use.
- Backend: Express + better-sqlite3. Food DB = 6,826 USDA seed rows
  (`backend/seed/usda-seed.json`); `/foods` = case-insensitive LIKE
  variants ordered by a rank expression, LIMIT 20 (`routes.js:250-275`).
  Scan = Gemini CSV contract (`docs/SPEC-005`). Water/steps/streak/coins
  from GET /home; steps are server-supplied, read-only in client.
