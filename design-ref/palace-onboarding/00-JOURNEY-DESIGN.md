# Mor palace onboarding — journey design (mock stage)

Status: DESIGN MOCK. Nothing here changes the app. Existing questions, order,
taps, routes and data behavior stay exactly as in
[verified baseline](../final-developer-prompt/SUPPORT-01-verified-baseline.md).
The mock in `mor-palace-onboarding-mock.html` is throwaway (prototype): no
persistence, options do not save, Continue only moves the camera.

## What the HUKUM reference does (13s ideal cut, `WhatsApp Video …13.27.03.mp4`)

Frame-by-frame, 360px-wide screen recording of holydrip.club/comet-hukum:

- 0-2s: BLACK-AND-WHITE palace arch corridor, slow dolly forward. Masthead
  top-center (HOLY DRIP x COMET), big serif HUKUM mid-frame, scroll chevron
  + carousel dots bottom. Audio: continuous warm bed, no hard hits
  (measured mean -14.9 dB, peak -1.7 dB, crest ~4.6 = dense groove).
- 2-3s: camera pushes THROUGH the foreground arch; masthead fades out.
- 3-4s: COLOR BLOOMS in: red double-door close-up, carved sandstone frame.
- 4-5s: doors part (red frame edges visible), sunlit gallery revealed: two
  carved pedestals, floral floor medallions, HUKUM + "100 ONLY / FIRST COME
  FIRST SERVED" wall inscription, dust motes in sunlight.
- 5-13s: inside the gallery. Horizontal swipe swaps the hero product
  (IVORY <-> MIDNIGHT); variant name is carved on the pedestal frieze.
  Carousel continues; UI chrome stays minimal.

The transferable idea is NOT shoes: enter in B&W, earn color, walk one
continuous dolly, each stop is a room, the hero object on a pedestal is the
payoff, variant-swipe = choice. For Mor the hero on the pedestal is Neelu
the peacock, and each room asks one existing onboarding question.

The 4.6-min video (`…13.03.15.mp4`) is a screen recording of the CURRENT Mor
build (MeetNeelu, Home + AddSheet) — useful as the before-state, not as
direction. Current Neelu is a chubby cartoon blue peacock; the palace
direction keeps him but stages him like the HUKUM shoe: lit, centered,
pedestal, sun.

## The 12 rooms (order and function locked to the current flow)

| # | Current screen -> tap | Room | Camera + reveal |
|---|---|---|---|
| 1 | Splash, whole page -> /welcome | Outer corridor | B&W arch corridor, MOR masthead + chevron. Any tap: color blooms as camera pushes through the arch. |
| 2 | Welcome, Get started -> /meet-neelu | Red door | Full-bleed red double door, fresco arch above. Tap: doors part with rumble, light slit widens. |
| 3 | MeetNeelu, Let's go -> /goal | Sanctum | Peacock on carved pedestal, fan half-open, halo ring. Tap: fan blooms 40->100% with shimmer + ascending chime. |
| 4 | Goal -> /diet | Gallery Hall I | 4 sandstone tablets (lose/maintain/gain/healthy). Selecting lights the tablet edge gold; Continue glides right. |
| 5 | Diet -> /body | Gallery Hall II | 4 jharokha windows with veg marks; chosen window glows from within. Vegan stays second (requested end state). |
| 6 | Body -> /target | Jharokha chamber | Sex segmented as 3 arch inlays; age/height/weight as inscription panels. Invalid age shakes the panel + kumkum edge. |
| 7 | Target -> /activity | Halo rotunda | Gold ring slider around a medallion; number counts in the medallion; ambitious state warms the ring to amber. |
| 8 | Activity -> /meal-times | Arch corridor | 4 archways with bar meters; chosen arch steps forward (scale) and lights. |
| 9 | MealTimes -> /building | Courtyard | 4 meal pavilions, toggle = lighting the pavilion diya; time chip = inscription; sheet = side shrine. |
| 10 | Building -> /plan (auto) | Stairway ascent | Camera climbs; progress % carved on risers; Skip = side door (same target, same as today). |
| 11 | Plan, Start -> /notify | Durbar hall | Farman card descends on a cushion: DAILY TARGET, macro bars, goal-line seal. Start = doors ahead open. |
| 12 | Notify -> /home | Bell pavilion | Brass bell + Neelu; Yes opens the permission shrine (same Allow/Don't allow -> /home); Not now side arch -> /home. |

Journey progress is a palace map, not step dots: a small top strip showing
arch icons for visited rooms (gold) vs ahead (outline). Same information as
StepDots, palace language.

## One continuous experience: timing + sound together

Total guided pass ~75s; every room is also free-play (answer at your pace).
The camera NEVER cuts: one dolly with room cross-dissolves on Continue.

- Movement grammar: push-in 1.15->1.0 over 900ms ease-out per room;
  lateral glide 120px for choice screens; rise 60px for Building/Plan.
  Parallax: back layer 0.3x, mid 0.6x, question card 1.0x. Transform and
  opacity only (GPU). Reduced motion: still cross-dissolves, no travel.
- Sound grammar (WebAudio synth in the mock; production: same map with
  recorded stems): low tanpura-like drone beds the whole journey (starts on
  first tap, autoplay-safe); door rumble (filtered-noise sweep) under every
  room change; a soft pulse tick while a slider/stepper moves; and one
  ascending sargam note per Continue (Sa Re Ga Ma Pa Dha Ni Sa' across the
  12 rooms, then shimmer) so progress is audible. Answer taps get a
  same-note ceramic tick. Mute toggle persists only for the session.
  Production must duck the drone -12 dB under any voiceover.
- Sync rule: visuals lead by 120ms, sound lands ON the visual settle
  (door fully parted = rumble tail + chime attack). Every beat in the mock
  is scheduled from one `ROOM_TIMELINE` table so movement and audio share
  timestamps — that table is the contract the motion spec will formalize.

## Figma build spec (rebuild target, not a .fig file)

Recreate in Figma as page "Palace Onboarding", 12 frames 360x800 named
`01-Splash-Corridor` … `12-Notify-Bell`. One background component set
`PalaceRoom` with variants (corridor, door, sanctum, gallery, jharokha,
rotunda, courtyard, stair, durbar, pavilion) so all rooms share the arch,
fresco-border and floor-medallion masters. Components: `TabletOption`
(selected: gold edge + lit), `JharokhaWindow`, `InscriptionField`,
`HaloSlider`, `ArchSteps`, `PavilionToggle` (diya lit/unlit), `FarmanCard`,
`PermissionShrine`, `JourneyMap` (12 arch icons), `Masthead`. Color styles
exactly: kumkum #BE2A24, sandstone #F8F5EE, neel #1B4FB0, teal #0B4A57,
pankh #19A39A, gold #E9A820, ink #142027. Text styles: Baloo 2 800 for
display/numbers, Mukta 400/600 body — same sizes as the current app
(H1 30, body 16, small 13.5). Auto-layout vertical, 20px margins, 56px
primary buttons. Prototype tab: Smart Animate, ease-out 600ms, push
direction matching the camera (forward = left, ascent = up); each Continue
also navigates the JourneyMap selection. Handoff must include the
ROOM_TIMELINE table, export list (room backgrounds 2x/3x PNG, trim
tileable, medallion, Neelu pedestal cutout) and the note that final motion
is implemented in code, not in Figma. The HTML mock next to this doc is the
visual reference for every frame.
