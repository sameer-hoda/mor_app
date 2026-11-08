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

## Android app (`android/`)

Native Kotlin app (Jetpack Compose + CameraX + Retrofit) — same screens and
flows as the web app, with this Mac's backend as its server.

```bash
cd android
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug     # APK: app/build/outputs/apk/debug/app-debug.apk
```

**Run it on your phone:**

1. Start the backend on this Mac: `cd backend && npm start` (port 4000).
   It already listens on all interfaces — no code change needed.
2. Copy `app-debug.apk` to your phone (USB, Drive, Bluetooth…) and install
   it (allow *Install unknown apps* once).
3. Same Wi-Fi as the Mac. On first launch the app can't reach the default
   emulator URL, so it shows a server field — enter
   `http://<your-Mac-IP>:4000` (e.g. `http://192.168.0.101:4000`;
   find yours with `ipconfig getifaddr en0`, or the address your router
   shows). You can change it later in Profile → Backend URL.
4. Onboarding → Plan → Home, then **+** → *Scan meal*. Camera and voice
   search use the normal Android permission prompts.

Verified on an emulator: boot → home with live data, insights, food search
with live rows, camera screen, and a full capture → `/scan` → result
round-trip (correct "couldn't see the food" for the emulator's fake scene;
real-food scans are covered by `docs/FOOD-SCAN-RESULTS.md`).

## AWS backend (EC2)

The backend also runs on an EC2 `t3.micro` (Amazon Linux 2023,
`15.135.140.52:4000`, `systemd` service `mor-backend`, auto-restarts and
starts on boot). The Android app uses it by default — no URL to type.

```bash
# redeploy after backend changes (key lives in .ssh/, never committed)
cd backend
rsync -avz --exclude=node_modules --exclude=data --exclude=uploads --exclude=.env \
  -e "ssh -i ../.ssh/mor-app.pem" ./ ec2-user@15.135.140.52:~/mor/backend/
ssh -i ../.ssh/mor-app.pem ec2-user@15.135.140.52 \
  'cd ~/mor/backend && npm install --omit=dev && sudo systemctl restart mor-backend'
curl http://15.135.140.52:4000/api/health
```

Server notes: Node 22 from the official binary tarball (`/usr/local`),
plus `gcc-c++` + `make` because `better-sqlite3` v13 compiles from source
at install. Security group must allow inbound TCP 4000. Still to do:
allocate an Elastic IP (the current public IP changes if the instance
stops), and add API auth before storing anything sensitive.

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
