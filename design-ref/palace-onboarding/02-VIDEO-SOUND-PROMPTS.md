# Video + sound generation pack (Gemini / Veo / belt)

Status: image + audio pipelines PROVEN in this repo. Video prompts are ready
to run; no key of yours is printed anywhere here — pass it via env.

## Proven: Nano Banana (gemini-2.5-flash-image) — WORKS

`assets-ai/a1-corridor.png`, `a2-door.png`, `a3-sanctum.png` were generated
with the repo's Gemini key via REST, image-to-image against
`design-ref/organized/…` references. Exact recipe (key via
`backend/.env`, never logged):

```bash
python3 - <<'EOF'
import re, json, base64, urllib.request
key = re.search(r'GEMINI_API_KEY=(\S+)', open('backend/.env').read()).group(1)
ref = base64.b64encode(open('REF.jpg','rb').read()).decode()
body = {"contents":[{"parts":[{"text":"PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":ref}}}]}],
        "generationConfig":{"responseModalities":["IMAGE"]}}
req = urllib.request.Request('https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key='+key,
    data=json.dumps(body).encode(), headers={'Content-Type':'application/json'})
d = json.load(urllib.request.urlopen(req, timeout=180))
open('out.png','wb').write(base64.b64decode(d['candidates'][0]['content']['parts'][0]['inlineData']['data']))
EOF
```

Remaining rooms (A4–A12 in `01-GEMINI-ASSET-PROMPTS.md`) run the same way —
swap REF.jpg and PROMPT. Belt equivalent (needs `belt login` first):

```bash
belt app run google/gemini-3-pro-image-preview --input '{"prompt":"PROMPT","images":["file://REF.jpg"]}'
```

## Proven: sound stems (ffmpeg, free, local) — DONE

`audio/bed.mp3` (24s tanpura-ish drone loop), `audio/chime.mp3` (bright
pluck, pitched per room via `playbackRate = 2^(n/12)` for the ascending
sargam), `audio/rumble.mp3` (door rumble). The v2 mock plays all three;
regeneration commands are in shell history — re-run with different seeds
for variations. Production: duck the bed -12 dB under any voiceover.

## Ready to run: Veo image-to-video (per beat, 9:16)

General Veo prompt grammar: subject + action + camera + style. Feed each
beat its A-room still as the first frame; no readable text in frame
(models garble it — wall inscriptions are composited in code).

1. Corridor dolly: "Slow forward dolly through a red-sandstone palace
   arch corridor toward a glowing far archway, dust motes drifting,
   warm sunlight, photorealistic architectural film, 9:16."
2. Bloom: "Static corridor cross-dissolving from black-and-white into
   full kumkum-red and gold color, light swelling at the far arch."
3. Door part: "Massive red double doors with brass studs slowly parting,
   warm light flooding through the widening slit, stone dust falling."
4. Sanctum reveal: "Camera pushes into a domed sanctum; a peacock fans
   its tail from 40% to full display on a carved pedestal, gold halo
   ring glowing behind, diyas flickering."
5. Gallery glide: "Lateral glide past four arched niches, one niche
   igniting gold as the camera passes."
6. Courtyard rise: "Slow crane up over a sunlit courtyard of four
   pavilions, diyas lighting one by one."

Run via the Gemini API Veo endpoint or `belt` video apps once billing is
on; keep each clip 4-8s and assemble on the ROOM_TIMELINE in
`00-JOURNEY-DESIGN.md` (visuals lead audio by 120ms).

## Sound map for a sound designer (or the stems above)

Bed: tanpura-like drone (Sa–Pa, ~110/165Hz), -20 LUFS under UI.
Accents: door rumble (brown-noise lowpass sweep) on every room change;
ceramic tick on option taps; ascending sargam C4→C5, one note per
Continue, shimmer octave on Plan reveal; duck -12 dB under voiceover.
Reference bed measured from the HUKUM cut: continuous groove, mean
-14.9 dB, peak -1.7 dB, no hard hits — match that density.

## Frozen palace set (Nano Banana PRO, consistency lock) — DONE

`assets-ai/frozen/f-*.png` (11 rooms + trim, ~850KB each): every call sent
the a1 corridor as style anchor + room ref + CONSISTENCY LOCK (identical
materials, arch profile, fresco motif, floor, light). Verified: sanctum,
durbar and spot checks read as ONE palace. Recipe = same REST call as
above with model `gemini-3-pro-image-preview` and two `inline_data` parts.
`assets-ai/a*.png` (v2 set) is kept as history.

## Animated peacock — Omni note + what ships now

`belt` (inference.sh, the Omni route) is installed but NOT logged in on
this machine — run `belt login` (or set `INFERENCE_API_KEY`) and tell me,
and I will run the image-to-video peacock pass. Until then, two things
already deliver the living peacock:
1. In-code animation (v4 sanctum): layered SVG fan — 7 feather shafts with
   staggered sway, neel eyes, breathing Neelu PNG, shimmer on reveal.
2. Veo prompt for the true clip (feed `f-sanctum.png` + Neelu cutout as
   first frame): "Locked-off shot in a sunlit palace sanctum; a peacock
   on a carved pedestal slowly fans its tail from folded to full display,
   feathers shimmering, diyas flickering, dust motes drifting. No camera
   move. Photorealistic, 9:16." Then a second clip: "Same locked shot;
   peacock turns its head toward camera, crest feathers trembling."
   Assemble: clip 1 on room entry, clip 2 looped behind the question.
