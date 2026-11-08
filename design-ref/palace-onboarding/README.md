# Palace onboarding — mock pack

End-to-end mock of Mor's first-time onboarding as one continuous palace walk,
modeled on the HUKUM reference (`WhatsApp Video 2026-11-08 at 13.27.03.mp4`).
Flow, questions, order, taps and routes are unchanged from the
[verified baseline](../final-developer-prompt/SUPPORT-01-verified-baseline.md).

- [00-JOURNEY-DESIGN.md](00-JOURNEY-DESIGN.md) — beat-by-beat design, the
  12-room map, movement+sound sync rules, Figma build spec.
- [01-GEMINI-ASSET-PROMPTS.md](01-GEMINI-ASSET-PROMPTS.md) — 12 copy-paste
  image-to-image prompts (style lock + reference pairing) for the LLM to
  call Gemini with; 1080x1920 PNG per room/master.
- [mor-palace-onboarding-mock.html](mor-palace-onboarding-mock.html) —
  v1 throwaway mock (CSS rooms, WebAudio synth). Verified: offline, JS
  parses, journey logic exercised headlessly.
- [mor-palace-entry-v2.html](mor-palace-entry-v2.html) — v2 cinematic mock:
  real Nano-Banana art (`assets-ai/`: corridor, door, sanctum), splitting
  doors, fan bloom, film grain + letterbox + dust, REAL audio stems
  (`audio/bed.mp3`, `rumble.mp3`, chime pitched per room). Same 12
  questions/taps; answers NOT saved. Verified: offline, JS parses,
  journey logic exercised headlessly.
- [02-VIDEO-SOUND-PROMPTS.md](02-VIDEO-SOUND-PROMPTS.md) — proven Nano
  Banana recipe, ffmpeg stem recipe, 6 Veo image-to-video prompts, sound
  map. `assets-ai/` + `audio/` hold the generated media.
- [mor-palace-entry-v3.html](mor-palace-entry-v3.html) — v3 better version
  of the Instinct prototype: 12 AI-painted rooms, EXACT current copy,
  correct Body fields, 4 meal slots, "Mor" spelling, Vegan 2nd, real
  StepDots, 11.5s audio-clock entry, descending two-tone bells, drone,
  haptics, mute, reduced motion. Verified: offline, JS parses, journey
  exercised (incl. all 4 slots).
- [03-REVIEW-vs-INSTINCT.md](03-REVIEW-vs-INSTINCT.md) — review notes:
  what was kept from Instinct, 10 fixed gaps, open owner decisions.
- [mor-palace-entry-v4-frozen.html](mor-palace-entry-v4-frozen.html) — v4
  FROZEN palace edition: one consistent palace (Nano Banana Pro +
  consistency lock, `assets-ai/frozen/`), sophisticated palette (lacquer,
  aged gold, deep teal, pankh), app-identical cards/bubbles/dots/toggles,
  living backgrounds (drift, dust, light), animated SVG fan + breathing
  Neelu, original stems. Verified as v3 was.

Fig. note: there is no `.fig` binary here — the Figma rebuild target is
specified in `00-JOURNEY-DESIGN.md` and the HTML mock is its visual
reference. Final motion is implemented in code, not in Figma.
