# Gemini asset prompts — palace onboarding rooms

How to call: image-to-image with the listed reference image(s) from this
repo (`design-ref/organized/…`) plus Commons refs where noted. Your LLM
calls the Gemini image model with `{prompt, reference_images}` and saves
1080x1920 PNG per asset. Generate the lock block once as a style seed, then
reuse it verbatim in every call.

STYLE LOCK (paste verbatim into every prompt): "Vertical 9:19 Rajasthani
palace interior, kumkum-red #BE2A24 sandstone, cream sandstone #F8F5EE,
peacock neel-teal #1B4FB0/#0B4A57 inlay, marigold-gold #E9A820 line work,
scalloped Mughal arches, jharokha lattice, fresco feather-eye borders, warm
sunlight with dust motes. Indian-miniature-meets-art-deco, gold line work.
Never cartoonish, never generic 3D stock. No people, no text, no watermark."

Global negatives for every call: "cartoon, 3d render, stock photo, people,
tourists, text, words, letters, watermark, oversaturated, purple neon".

## A1 corridor (Splash B&W source)
Reference: `02-palace-door-sequence/door-alley-peacock-display.jpg` +
Commons `ref-commons-palace-arch-02-1.jpg` (entrance arch framing).
Prompt: STYLE LOCK + "Long symmetrical arch corridor receding to a glowing
far doorway, layered scalloped arches left and right, fresco feather-eye
band above the far arch, polished stone floor with single floral medallion,
soft haze at the far end. Generate IN FULL COLOR — the app desaturates to
B&W in code and blooms color on entry."
Use: Splash. Code animates grayscale(1)->grayscale(0) + push-in.

## A2 red door (Welcome)
Reference: `02-palace-door-sequence/door-closed-peacock-mural.jpg` +
Commons `ref-commons-peacock-gate-01-1.jpg` (real Peacock Gate).
Prompt: STYLE LOCK + "Head-on closed red double door filling the frame,
brass studs and ring handles, cream carved sandstone surround, large
peacock-feather fresco fan arching ABOVE the door (not on it), two slim
marble pillars flanking. Flat frontal composition, door centered with
margins for title text top and button bottom."
Use: Welcome hero. Code splits the door (two halves as separate layers).

## A3 sanctum (MeetNeelu)
Reference: `02-palace-door-sequence/door-side-peacock-niche.jpg` (profile
peacock mood) + `04-home/home-geometric-halo.jpg` (halo).
Prompt: STYLE LOCK + "Small domed sanctum chamber, empty carved stone
pedestal centered on a dais, large circular gold halo ring on the wall
behind it, teal medallion inlay on the floor, two oil diyas flanking the
dais, gentle spotlight cone from above. NO bird — the mascot is composited
in code. Leave the pedestal top clean and the wall behind it calm."
Use: MeetNeelu. Neelu PNG/WebM sits on the pedestal; fan bloom is code.

## A4 gallery hall (Goal + Diet)
Reference: Commons `ref-commons-palace-arch-02-3.jpg` (Chowk doorway).
Prompt: STYLE LOCK + "Wide gallery hall wall with FOUR identical shallow
arched niches in a row (2x2 for Diet variant: same wall, niches arranged
two by two), each niche empty with a small unlit diya shelf, fresco vine
border running along the top, calm warm plaster between niches for option
labels. Generate both framings: ROW and GRID."
Use: Goal (row), Diet (grid). Options are code tablets; lit state is code.

## A5 jharokha chamber (Body)
Reference: `02-organized/03-onboarding` Mughal arch pins (jharokha frames).
Prompt: STYLE LOCK + "Intimate chamber dominated by one large jharokha
bay window with lattice screen, three arch inlay panels below it glowing
faintly teal, two blank stone inscription slabs left and right at hand
height, soft daylight through the lattice casting patterned shadows."
Use: Body. Sex segmented maps to the 3 inlays; age/height/weight to slabs.

## A6 halo rotunda (Target)
Reference: `04-home/home-geometric-halo.jpg`.
Prompt: STYLE LOCK + "Circular rotunda, concentric gold ring inlay on the
floor rising as a glowing halo band on the curved wall, central round
medallion dais viewed slightly from above, teal and marigold concentric
rings, oculus of daylight above. Symmetrical, meditative, uncluttered
center for a large number."
Use: Target. The number + slider are code; ring warms to amber in code.

## A7 arch corridor (Activity)
Reference: Commons `ref-commons-amber-arch-04-1.jpg` (Amber arch).
Prompt: STYLE LOCK + "Corridor of four receding red-sandstone arches seen
in perspective, each arch progressively more distant and hazier, small
brass bar-meter inlay set into each arch pier at hand height, floor runner
carpet with border pattern leading the eye forward."
Use: Activity. Chosen arch scales forward + lights in code.

## A8 courtyard (MealTimes)
Reference: Commons `ref-commons-courtyard-02-1.jpg` (Laxmi Vilas courtyard).
Prompt: STYLE LOCK + "Sunlit square courtyard, FOUR small identical meal
pavilions (open chhatri kiosks) one per side, each with an unlit hanging
diya and a blank inscription plaque, central floor medallion, morning
light, mango-leaf torans. Calm and symmetrical."
Use: MealTimes. Pavilion diyas light = toggles; plaques = time chips.

## A9 stairway (Building)
Reference: Commons arch/stair geometry (Amber Fort refs).
Prompt: STYLE LOCK + "Narrow palace stair ascending toward a bright
doorway at top, stone risers with faint inlaid percentage ticks, carved
balustrade one side, warm light growing stronger toward the top landing."
Use: Building. Progress % is code text on risers.

## A10 durbar hall (Plan)
Reference: Commons Peacock Throne miniature mood (regal, gold).
Prompt: STYLE LOCK + "Grand but serene durbar hall, empty low royal
cushion-dais centered, large blank farman-style wall panel with gold
border behind it awaiting an inscription, two tall brass lamps, symmetric
marble floor with a single central medallion."
Use: Plan. The farman card (targets, macros, seal) is code.

## A11 bell pavilion (Notify)
Reference: `01-originals` minimal marks (one-ink calm).
Prompt: STYLE LOCK + "Small open bell pavilion at dusk-warm light, single
brass temple bell hanging centered under a scalloped dome, empty stone
shrine niche beside it, quiet courtyard beyond, a few diyas. Peaceful,
restrained, uncluttered."
Use: Notify. Permission dialog is the code shrine beside the bell.

## A12 trim + medallion (shared masters)
Reference: `02-palace-door-sequence/door-closed-peacock-mural.jpg` (fresco
fan) + Beardsley pattern mood.
Prompt: STYLE LOCK + "Two tiles side by side on cream: (1) horizontal
fresco border band of peacock-feather eyes in teal/gold/maroon, seamless
left-right; (2) single round floral floor medallion, maroon petals with
teal center and gold outline. Flat, frontal, no perspective."
Use: sliced in code as repeating border + floor medallion everywhere.
