# Mor — Indian calorie tracker

Mobile-format web app (360×800) with a local backend. Scan food photos with the camera;
a Gemini model on the **server** returns a strict CSV that is parsed, corrected for oil
level, and logged. All user data lives in the backend (SQLite).

```
mor_app/
├── app/       Vite + React 19 frontend (HashRouter, port 5173)
├── backend/   Express + better-sqlite3 (port 4000) — holds the Gemini key
├── docs/      SPEC-005 camera → Gemini CSV contract
├── scripts/   Asset pipeline (Gemini image gen + chroma keying)
└── inputs /   Design canvases & asset prompts (note the trailing space)
```

## Quick start

```bash
# 1. Backend (needs a Gemini key)
cd backend
cp .env.example .env          # then set GEMINI_API_KEY
npm install
npm start                     # http://localhost:4000

# 2. Frontend (second terminal)
cd app
npm install
npm run dev                   # http://localhost:5173
```

Open http://localhost:5173 in a phone-sized viewport (DevTools → 360×800).
Onboarding → Plan → Home. Tap **+** → *Scan food* to use the camera
(`localhost` is treated as a secure context, so `getUserMedia` works).

## Test on your phone

Backend stays on the Mac; the phone only ever talks to the Vite server,
which proxies `/api` and `/uploads` to `localhost:4000` — so the Gemini key
never leaves the Mac.

```bash
cd app
npm run dev:phone     # HTTPS + LAN exposure (uses a self-signed cert)
```

Then on the phone (same Wi-Fi), find the Mac's LAN IP:

```bash
ipconfig getifaddr en0      # e.g. 192.168.0.101
```

Open **https://192.168.0.101:5173** on the phone.

- **Android/Chrome:** you'll see a cert warning → *Advanced* → *Proceed*.
  Camera (`getUserMedia`) then works for the scan flow.
- **iOS/Safari:** self-signed certs are stricter; if the camera doesn't open,
  test the scan flow via the gallery picker (it exercises the identical
  backend → Gemini path), or install the generated cert as a trusted profile.

Plain `npm run dev` (HTTP, desktop) still works as before.

## How the scan works

1. App captures a JPEG frame (≤ 1280px, downscaled in-browser) → `POST /api/scan`
   with `image_base64`.
2. Backend sends it to `gemini-2.5-flash` with the prompt in
   `backend/src/gemini.js` (SPEC-005). Response must be CSV:
   `meal,<name>` then `name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives`.
3. `backend/src/scanCsv.js` parses it strictly. Confidence < 0.6 → the item is
   marked **unsure** and alternatives are offered on the Result screen.
   Oil-level multipliers (less 0.85 / normal 1 / more 1.15) adjust kcal & fat.
4. User reviews → `POST /api/meals` writes the meal; Home ring, macros, coins
   (+10 for scans) and streak update from `GET /api/home`.

One automatic retry on malformed CSV; parse failure → `status: "failed"` and the
Result screen offers *Take another photo* / *Search instead*.

## Tests

```bash
cd backend
npm test                      # unit + API integration (no network)
GEMINI_API_KEY=... node --test test/api.scan.test.js   # live Gemini round-trip
```

## API surface (`/api`)

health · profile (GET/POST) · home · meals (POST/GET/DELETE) · foods ·
foods/estimate · scan · scans/:id · voice-log · water · weight · insights ·
streak · challenges · challenges/claim · badges/award · notifications ·
notifications/read · reset

All requests carry `x-user-id` (default 1). Reset wipes that user's data.

## Regenerating assets

```bash
cd scripts
GEMINI_API_KEY=... node generate-assets.mjs   # mascot posters, icon, scenes, fixture
python3 key_chroma.py                         # chroma-key magenta → transparent PNGs
```

Mascot chroma color is **#FF00FF magenta** (green collided with Neelu's tail).

### Mascot videos (Gemini Omni)

11 short story clips live in `app/public/neelu-video/neelu-<slot>.webm`
(silent, alpha VP9 — Neelu composites over any background). Regenerate:

```bash
cd scripts
GEMINI_KEY=... node omni-video.mjs   # generate raw clips (Interactions API)
python3 key_video.py                 # key magenta → alpha, encode VP9 WebM
```

Notes: Omni models require the Interactions API with
`response_modalities: ['video','audio']` (video-only fails) and short prompts;
key via `x-goog-api-key`. One-shots play once and freeze; ambient slots
(think/hungry/sleep/droop) soft-loop ≤3× then freeze. Safari falls back to the
PNG posters (no alpha video there). See `docs/SPEC-005` §10 for the slot map.
