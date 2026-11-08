# Mor design elevation — palace + peacock

App today: Vite + React 19 HashRouter, 360×800 phone frame (`app/src/`).
`Splash` → `Welcome` → `MeetNeelu` → Goal/Diet/Body/Target/Activity/MealTimes → `Building` → `Plan` → `Notify` → `Home` → `AddSheet/Camera/Scanning/Result/Logged`.
Style tokens in `app/src/styles.css`: `--teal:#0B4A57 --gold:#F2B544 --kumkum:#D9483B --chandan:#F8F5EE --neel:#1F4FB5 --pankh:#19A39A`.
Neelu mascot: PNG set in `app/src/assets/neelu/` + SVG fallback `NeeluSvg`, video moods via `NeeluVideo`.

Mood board: 11 files in `modd_board/`, organized copies in `design-ref/organized/` (originals untouched).
Palette from board: kumkum red walls, sandstone cream, peacock neel/teal, pankh green, marigold gold.

## Section 1 — Logo (first)

Current: `FeatherEye` circles (gold/teal/jamun) + `NeeluSvg` cartoon + teal Splash with gold ring.
Board direction: `01-logo/logo-minimal-feather-fan.jpg` — one-ink fan, works at 16px to 180px.

- Keep FeatherEye as product sparkle, promote fan-mark to app icon + Splash.
- Single-color fan (teal-ink `#0B4A57` on chandan, cream on teal) + gold dot-eye variant for premium.
- Animate: dots bloom into fan on load (`logo-dot-eye.jpg` idea, 600ms stagger).

To source (don't hotlink, license later):
- Minimal peacock fan marks — search `minimal luxury peacock feather eye logo` — refs:
  [BrandCrowd peacock feather tail](https://logo.crazydomains.com.au/maker/logo/peacock-feather-tail-8285759?text=Peacock%20Boutique&layoutOrientation=2-line-break-at-14&isSearch=True&searchImpressionId=4907da9d-9a2f-47ed-89e7-253b2bdd3418) ·
  [Elegant peacock feather](https://logo.crazydomains.com.au/maker/logo/elegant-peacock-feather-8725670?text=Peacock%20Boutique&layoutOrientation=2-line-break-at-14&isSearch=True&searchImpressionId=0dce1fed-0e15-4b4c-a3c1-8e5b6ef6af76) ·
  [Peacock eye on teal](https://www.templatemonster.com/logo-templates/peacock-eye-logo-design-on-teal-background-576089.html)

## Section 2 — First-launch palace walk (hero sequence)

Goal: feel like walking into a palace to see the peacock.
Use the 3 door images as 3 beats, vertical 360×800, ~3.2s total, skippable, reduced-motion = crossfade.

1. Closed door — `02-palace-door-sequence/door-closed-peacock-mural.jpg`
  Full-bleed red door + feather fresco. Title: “Mor”. Sub: “Eat well, the Indian way.” Doors breathe (scale 1.0→1.04).
2. Crack + profile — `door-side-peacock-niche.jpg`
  Doors split 12px, warm light leaks, profile peacock turns head (parallax translateX). Haptic tick.
3. Alley reveal — `door-alley-peacock-display.jpg`
  Camera dolly through red arch tunnel (scale 1.15→1.0, arches slide out). Peacock fans from 40%→100% + feather shimmer. CTA: “Enter → Meet Neelu”.

Build: 3 preloaded images, CSS transform/opacity only, WebM optional later. Replace current flat teal `Splash`.

Photo refs to license/shoot-like:
- Peacock Gate, Pritam Niwas Chowk (autumn/Vishnu gate) — [Tripoto Jaipur doors](https://www.tripoto.com/rajasthan/trips/tripoto-6055c77218ae3) · [City Palace guide](https://www.goldentriangleindiatravel.com/city-palace.html) · [Laure Wanders](https://www.laurewanders.com/city-palace-jaipur/)
- Arch-corridor dolly feel — [Amer Fort pillared halls](https://www.artbazaar.net/products-details/2128) · [Fatehpur Sikri passageway](https://www.artbazaar.net/products-details/2113) · [10 Jaipur photo spots](https://www.localgrapher.com/photographers-jaipur/10-best-jaipur-photo-spots-from-our-local-photographers/)

## Section 3 — Onboarding images + animation

Current: mint wave hero + `NeeluVideo wave`, white option cards, 6 steps with `StepDots`.
Keep structure, reskin per step with board tones:

- Welcome: replace mint wave with boho arch scene (`home-boho-arch.jpg` style, cream + terracotta sun). Keep 590kcal chip.
- Meet Neelu: art-deco halo behind Neelu (`03-onboarding/onboarding-artdeco-peacock.jpg` — teal/gold rays).
- Goal/Diet/Activity: playful flat blobs (`onboarding-playful-modern.jpg`) as left-edge spot art, keep tappable `.opt` cards.
- Target slider: halo ring from `home-geometric-halo.jpg` — progress = tail-eyes filling.
- MealTimes: Mughal arch frames per meal row.

Pattern refs:
- [Mughal seamless peacock pattern — Behance](https://www.behance.net/gallery/202833131/Mughal-Seamless-pattern) ·
  [Adobe Stock Mughal arch mural](https://stock.adobe.com/images/indian-mughal-garden-arch-peacock-bird-plant-vector-illustration-for-wallpaper-mural-art/741337699)

## Section 4 — Home

Current: SVG `Scene` (flat sky + palace outline) + banner + streak diya + kcal ring + meals + water/steps.
Keep IA, elevate surfaces:

- Day scene: cream/terracotta boho wash (`home-boho-arch.jpg`), night scene: gold-jungle (`home-gold-jungle.jpg`, black-gold leaves).
- Ring: gold track, teal fill, feather-eye center (already `FeatherEye`) — halo variant for goal state.
- Streak: diya flame + dot-fan that blooms per day (logo-dot-eye idea).
- Meals: arch-topped thumbnails instead of circles.

## What I did not change

- No edits to `modd_board/`. Only copies in `design-ref/organized/`.
- No code changes yet. No image downloads — links above are sourcing list to license.

## Suggested next build order

1. Logo fan-mark SVG + Splash swap.
2. Palace-walk 3-beat animation (CSS only).
3. Onboarding reskin (Welcome + Meet Neelu first).
4. Home day/night scenes.
