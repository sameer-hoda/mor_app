# Instinct prototype review -> what v3 does better

Reviewed: `Instinct Files.pdf` (spec, 7,181 chars, 5 embedded frames) and the
39s screen recording (`WhatsApp Video …14.01.14.mp4`). The Instinct work is
strong: audio-clock entry (6 beats, 0-11.5s), exact motion numbers, 650ms
chamber advances, drone + descending bells, decisions D1-D4. v3 keeps all of
that and fixes what it flagged as assumptions plus what it got wrong:

1. Exact current copy everywhere (their copy was provisional). Verified
   against `app/src`: real headlines, subs, option labels.
2. Correct Body screen: sex segmented + age field (18+) + height + weight —
   theirs showed Age/Height/Weight steppers, which is NOT the app.
3. All 4 meal slots (Breakfast/Lunch/Snack/Dinner) — theirs showed 3.
4. "Mor" spelling (theirs wrote "MoRa"; the app, brief and library say Mor).
5. Vegan 2nd in Diet (requested end state; theirs unspecified).
6. Real StepDots on steps 1-6 (theirs kept StepDots; v3 matches exactly).
7. 12 distinct AI-painted rooms (Nano Banana, image-to-image on the mood
   refs) instead of 4 placeholder scenes.
8. Shippable original sound: bed loop + rumble + pitched chime, same
   envelope as the HUKUM reference (which cannot ship — D2). No licensed
   track in the mock.
9. Entry lives inside Welcome -> MeetNeelu navigation (their D1
   recommendation), driven by the audio clock (`bed.currentTime`).
10. Mute top-right in-phone, 44px+ targets, reduced-motion crossfades,
    haptics on crest + advances (navigator.vibrate).

Open issues for the owner (from their D-list + mine): D2 music needs a
commissioned ~13s piece for production (mock stems are placeholders that
can ship, but a composed track will beat them); D4 return-visit shortening
undecided; `assets-ai/` PNGs are ~2.5MB each — production needs
compressed WebP/AVIF + preload budget (~1.5MB total per their spec).
