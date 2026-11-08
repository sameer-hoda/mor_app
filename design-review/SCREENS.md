# Mor app — design review package

Purpose: review and enhance the visual design of every screen **without
changing the backend or the screen layouts**. All screenshots are fresh from
the current build (Sun 8 Nov 2026, emulator, 1080×1920). All image assets the
screens use are in `assets/`.

- `screens/` — one PNG per screen, numbered in app order.
- `assets/` — the exact image files bundled in the app (copies; the live
  originals are in `android/app/src/main/res/drawable-nodpi/`).

## What a designer can safely change

- Any file in `assets/` (same filename, same aspect ratio — sizes below).
- Any color in the palette (single source: `android/.../ui/Theme.kt`).
- Copy/text wording (in `ui/*.kt` next to each screen).
- Everything else — layout structure, navigation, spacing logic, API calls —
  stays as is. Each screen row below names the one layout file to avoid.

## Palette (`ui/Theme.kt` — single source of truth)

| Token | Hex | Used for |
|---|---|---|
| Teal / TealDark | `#0B4A57` / `#08343D` | Primary buttons, selected states, headers |
| Chandan | `#F8F5EE` | Screen background |
| Ink / Sub | `#142027` / `#4A5A63` | Headings / secondary text |
| Mint / Mint2 | `#E3F2EF` / `#D3EBE6` | Selected cards, steppers, toggles-on |
| Genda / Gold | `#F2A93B` / `#F2B544` | Accents, camera ring, coin pill |
| Panna / Green | `#1E8A5A` / `#155E3D` | Success, veg badges |
| Kumkum / Red | `#D9483B` / `#9A2A20` | Errors, unread badge |
| Amber | `#7A4B05` on `#FDEBC8` | Warnings |
| Jamun on LavBg | `#2D2A6E` on `#E6E4F6` | AI estimates |
| Line / Disabled | `#EDE7DA` / `#E3DDD0` | Borders / disabled CTA |

Type: system typeface everywhere (no bundled fonts). Headings ExtraBold
28–56sp, body 13–17sp Semibold/Regular.

## Assets (`assets/`)

| File | Size | Where it appears |
|---|---|---|
| `hdr_dawn/morning/noon/afternoon/evening/sunset/night/latenight.jpg` | 1080×810 | Home header, picked by time of day (5–6, 7–10, 11–13, 14–16, 17–18, 19–20, 21–23, else) — `Home.kt` `homeArt()` |
| `feather_tile.jpg` | 512×512, tiled | Welcome band + onboarding wallpaper (`Components.kt` `FeatherBackdrop`) |
| `neelu_happy.png` | 640² | Welcome, meet, add-sheet, search panel |
| `neelu_joy.png` | 640² | Plan-ready, challenges |
| `neelu_cool.png` | 640² | Splash, streak |
| `neelu_thinking.png` | 640² | Building, voice-recognising, insights locked/empty |
| `neelu_hungry.png` | 640² | Scan failures, no-food result |
| `neelu_love.png` | 640² | Logged confirmation |
| `neelu_sleepy.png` | 640² | Reminders off state, profile |
| `mor_icon.png` | 640² | Launcher icon only |

Mood names in code map to files (`Components.kt` `moodFor`): splash→cool,
wave/peek→happy, hop→love, dance→joy, think/shrug→thinking, hungry/droop→
hungry, sleep→sleepy. Swapping art = replacing these PNGs; moods stay.

## Screens

| # | Screen | Shot | Layout (do not restructure) | Live data (backend, do not change) | Design notes |
|---|---|---|---|---|---|
| 01 | Splash | `01-splash.png` | `Onboarding.kt` `SplashScreen` | none (honest loading state) | Teal bg, cool Neelu, version line. |
| 02 | Welcome | `02-welcome.png` | `Onboarding.kt` `WelcomeScreen` | none | Feather band + happy Neelu + headline + Get started. |
| 03 | Neelu intro | `03-neelu-intro.png` | `Onboarding.kt` `MeetNeeluScreen` | none | Centered column (verified, do not "fix"). |
| 04 | Goal | `04-goal.png` | `Onboarding.kt` `GoalScreen` | `POST /api/profile` (goal) | 4 option cards with right-side art slots. |
| 05 | Diet | `05-diet.png` | `Onboarding.kt` `DietScreen` | `POST /api/profile` (diet) | Order: Vegan, Vegetarian, Eggetarian, Non-veg. |
| 06 | About you | `06-about-you.png` | `Onboarding.kt` `BodyScreen` | profile sex/age/height/weight | Big sex boxes + 3 shared slider cards. |
| 07 | Target weight | `07-target-weight.png` | `Onboarding.kt` `TargetScreen` | `target_weight_kg` | Same slider card + pills + date caption. |
| 08 | Activity | `08-activity.png` | `Onboarding.kt` `ActivityScreen` | `POST /api/profile` (activity) | Ascending bar icons are intentional. |
| 09 | Meal times | `09-mealtimes.png` | `Onboarding.kt` `MealTimesScreen` | meal_times + plan build | Custom toggle component. |
| 10 | Building plan | — (transient, ~2s) | `Onboarding.kt` `BuildingScreen` | `POST /api/profile` complete:true | Thinking Neelu + progress; has honest error+retry. Capture on demand. |
| 11 | Plan ready | `11-plan-ready.png` | `Onboarding.kt` `PlanScreen` | plan targets | Joy Neelu, macro allocation bars (targets, not intake). |
| 12 | Notify | `12-notify.png` | `Onboarding.kt` `NotifyScreen` | reminders_on + OS permission | Sheet + real system dialog (see 17-item report). |
| 13 | Home | `13-home.png`, `13-home-afternoon.png` (2 header variants) | `Home.kt` | `GET /api/home` | Header art swaps by time; water+steps top; week strip; meal cards; bottom nav. |
| 14 | Log-a-meal sheet | `14-add-sheet.png` | `Home.kt` `AddSheet` | slot default from `relevantSlot()` | Slot pills + 5 entry rows + quick-add stepper. |
| 15 | Quick add | `15-quick-add.png` | `Home.kt` (in `AddSheet`) | `POST /api/meals` source=quick | –/+ ×1–×4, 150 kcal each. |
| 16 | Search food | `16-search.png` | `Food.kt` `SearchScreen` | `GET /api/foods?q=` | Verified/AI pills wrap, never squeeze (fixed). |
| 17 | Food item | `17-food-item.png` | `Food.kt` `FoodItemScreen` | food row + estimate | Unit chips + qty stepper + oil selector. |
| 18 | Servings sheet | `18-cart-servings.png` | `Food.kt` (cart sheet) | same cart payload | Per-line ×0.5 steppers + remove. |
| 19 | Voice | `19-voice.png` | `Food.kt` `VoicePanel` | `POST /api/voice-log` | Listen/Stop/Add states + red retry hint. |
| 20 | Camera | `20-camera.png` | `Scan.kt` `CameraScreen` | none until capture | Meal/Label/Barcode toggle, guide frame, shutter + gallery + search. |
| 21a | Scan result | `21-scan-result.png` | `Scan.kt` result | `POST /api/scan`, `GET /api/scans/{id}` | Photo + items + oil less/normal/more + log CTA. |
| 21b | Logged | `21-logged.png` | `Scan.kt` logged | `POST /api/meals` | Love Neelu + remaining + coins + tip. |
| 21c | Scanning | — (transient) | `Scan.kt` scanning | upload in flight | Spinner + "Reading your plate…"; keeps photo on retry. |
| 22 | Meal detail | `22-meal-detail.png` | `Scan.kt` meal detail | `GET /api/meals/{id}` | Totals + item lines + tip + delete. |
| 23 | Insights | `23-insights-week.png`, `23-insights-month.png`, `23-insights-locked.png` | `Progress.kt` | `GET /api/insights?range=` | Week 7 bars / month 30-bar scroll / locked empty state (thinking Neelu). |
| 25 | Challenges | `25-challenges.png` | `Progress.kt` `ChallengesScreen` | `GET /api/challenges` | Coin pill + Today/Week groups. (Tab renamed from Coins + fire icon.) |
| 26 | Inbox | `26-inbox.png` | `Settings.kt` inbox | `GET /api/notifications` | Relative times are device-local; unread dots. |
| 27 | Profile | `27-profile.png` | `Settings.kt` profile | `GET /api/profile` | No backend URL in consumer UI (dev-only). |
| 28 | Reminders | `28-reminders.png` | `Settings.kt` reminders | reminders_on + toggles | Off banner + meal/nudge switches. |
| 29 | Streak | `29-streak.png` | `Progress.kt` streak | `GET /api/streak` | Cool Neelu + diyas + month grid + freeze. |
| 24 | Weight (full screen) | — | `Progress.kt` `WeightScreen`, route `weight` | weights | **Currently unreachable** (no in-app entry; weight is a sheet on Insights). Flag for product decision. |
| — | Toast | `99-toast.png` | `Components.kt` `MorToast` | app snackbar state | Dark card, ✓/! dot; auto-dismiss 2.8s, tap to dismiss. |
| — | Health Connect | `98-health-connect.png` | system sheet via `Steps.kt` | Health Connect SDK | Honest states: off / tap-for-settings / count / unavailable. |

Numbers 14–22 open with the meal context preselected (relevant slot by time,
or the card that opened them) — preserved through retry/back/recovery.
