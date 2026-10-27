# SPEC-005 · Camera capture → Gemini analysis → CSV nutrition output

> Companion to the Mor design spec (`inputs /project/`). Covers the flow the design
> canvas leaves open: **what happens between the shutter press and the Result screen.**
>
> Screens affected: `Camera` (L1) · `Scanning` (L2) · `Result` detected/unsure/failed (L3) · `Logged` (L4)

---

## 1. Principles

1. **The Gemini API key never ships to the phone.** All model calls happen on the local
   backend server (`backend/`, Express). The mobile app talks only to the backend.
   The key lives in `backend/.env` (`GEMINI_API_KEY`) and is loaded once at boot.
2. **Real APIs, no mocks.** The backend calls `gemini-2.5-flash` (multimodal, low-cost)
   through `generativelanguage.googleapis.com`. Verified working 2026-10-27.
3. **Gemini's output is a strict CSV.** The prompt forces a machine-readable CSV that the
   backend parses, validates, clamps, and stores. The raw CSV is persisted (scans table)
   so any row can be traced back to the model's exact words.
4. **Every failure path has a screen.** `Result_unsure` and `Result_failed` are first-class
   outcomes driven by backend-classified status codes, not generic errors.
5. **Low cost by design.** gemini-2.5-flash is the default vision model (multimodal,
   cheap). Only one call per scan; one automatic retry on parse failure.

## 2. Architecture

```
┌────────────────────────┐   ① POST /api/scan      ┌──────────────────────────┐   ② generateContent   ┌─────────────────────┐
│  App (360×800 lab)     │  {image_base64, slot} → │  Local backend  :4000    │  {image, csv prompt} →│  Gemini 2.5 Flash   │
│  getUserMedia camera   │                          │  Express + SQLite        │                        │  (multimodal)       │
│  captures JPEG frame   │  ← ③ parsed JSON result  │  parses CSV, validates,  │  ← ④ strict CSV text ──│                     │
└────────────────────────┘     {items, status, …}   │  stores scan + image     └──────────────────────────┘  ⑤ raw CSV reply   └─────────────────────┘
        │                                            │
        └── ⑥ POST /api/meals (on "Log") ────────────┘ → meal row, coins, streak, notifications
```

- App never sees `GEMINI_API_KEY`; there is no key in the client bundle.
- Images are stored on the backend (`backend/uploads/`) and referenced by `image_url`;
  the app fetches them from the backend, keeping one source of truth for user data.

## 3. Camera capture (app side)

| Aspect | Behaviour |
|---|---|
| Stream | `navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment', width: 1280, height: 1280 } })` |
| UI | Full-bleed video in the dark `#0A0F12` stage, gold corner-bracket viewport (312×312), "Fit the whole plate" chip with Neelu-cool head |
| Shutter | `canvas.drawImage(video)` → JPEG, long edge downscaled to ≤1024 px, quality 0.85 (≈100–300 KB) |
| Denied state | `NotAllowedError`/`NotFoundError` → camera-denied card (Neelu-hungry, "Camera access needed", Open settings, Search food instead) |
| Gallery | `<input type="file" accept="image/*">` — chosen image runs the exact same pipeline from step 4 |
| Modes | Meal / Label / Barcode tabs are UI state; Label & Barcode fall back to Meal analysis in v1 |

## 4. API contract

### 4.1 `POST /api/scan`

Request (app → backend):

```json
{ "image_base64": "<base64, no data: prefix>", "mime": "image/jpeg", "slot": "lunch" }
```

Response (backend → app) — status `200` with one of three `status` values:

```json
{
  "scan_id": "7f3a…",
  "status": "detected",            // "detected" | "unsure" | "failed"
  "meal_name": "Thali lunch",
  "items": [
    { "idx": 1, "name": "Dal tadka",      "kcal": 150, "protein_g": 6,  "carbs_g": 15, "fat_g": 6,  "portion": "1 bowl",  "confidence": 0.90, "unsure": false, "alternatives": [] },
    { "idx": 2, "name": "Rajma / chole",  "kcal": 150, "protein_g": 7,  "carbs_g": 16, "fat_g": 5,  "portion": "1 bowl",  "confidence": 0.45, "unsure": true,  "alternatives": ["Rajma", "Chole"] }
  ],
  "totals": { "kcal": 590, "protein_g": 31, "carbs_g": 79, "fat_g": 26 },
  "raw_csv": "name,kcal,…\nDal tadka,150,…",
  "model": "gemini-2.5-flash",
  "image_url": "/uploads/scan_7f3a.jpg"
}
```

Classification (backend):

| Status | Trigger |
|---|---|
| `detected` | ≥1 valid item and **no** item has `confidence < 0.6` |
| `unsure` | ≥1 valid item and ≥1 item has `confidence < 0.6` (that item shows the choice card) |
| `failed` | 0 valid items, or Gemini unavailable / unparseable after retry |

Errors: `400` (bad image, >6 MB), `413` (oversize), `502` (Gemini downstream), `503` (key missing).
All error bodies: `{ "error": "message" }` — the app routes them to `Result_failed` / snackbar.

### 4.2 `POST /api/meals`

```json
{ "slot": "lunch", "name": "Thali lunch", "source": "scan", "scan_id": "7f3a…",
  "items": [ { "name": "Dal tadka", "kcal": 150, "protein_g": 6, "carbs_g": 15, "fat_g": 6, "portion": "1 bowl" } ],
  "oil_level": "normal" }
```

Response: the stored meal + `{ coins_awarded, streak, kcal_left_today }`.
Oil level multiplies kcal (less 0.85 / normal 1 / more 1.15) before totals are finalised.

## 5. Gemini prompt (exact)

System instructions (verbatim, sent as the text part next to the image):

```text
You are Mor's food vision engine. You look at a photo of Indian food and return a
strict CSV — nothing else. No markdown, no code fences, no commentary.

Output exactly this shape, in this order:

meal,<meal name in Title Case, ≤30 chars>
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
<one row per visible dish or drink>

Rules:
- kcal: integer, per the portion you state.
- protein_g, carbs_g, fat_g: integers or empty.
- portion: short human text like "1 bowl", "2 pieces", "¾ plate".
- confidence: 0.0–1.0, your certainty the identification is right.
- unsure: 1 if confidence < 0.6 (e.g. two dishes look alike), else 0.
- alternatives: for unsure rows only, 2–3 candidate names separated by "|", else empty.
- One row per distinct food; sauces/pickles counted only if clearly visible.
- Use standard Indian dish names (Dal tadka, Phulka roti, Jeera rice, Aloo gobhi…).
- If the photo has no recognisable food: output only the header row, nothing else.
```

Call parameters:

```js
model: "gemini-2.5-flash"
temperature: 0.2                      // deterministic-ish for parsing
maxOutputTokens: 1024
thinkingConfig: { thinkingBudget: 0 } // keep latency + cost low
contents: [{ parts: [{ text: PROMPT }, { inline_data: { mime_type, data: base64 } }] }]
```

## 6. CSV contract (the "exact CSV output")

Row 1 is always a meta row: `meal,<name>`. Row 2 is the header. Rows 3+ are items.

```csv
meal,Thali lunch
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
Dal tadka,150,6,15,6,1 bowl,0.90,0,
Aloo gobhi,120,3,14,6,1 bowl,0.88,0,
Jeera rice,170,4,36,2,¾ plate,0.85,0,
Phulka roti,120,4,20,3,2 pieces,0.90,0,
Raita + salad,30,2,4,1,½ bowl,0.80,0,
```

Parser rules (backend, `scanCsv.ts`):

1. Strip code fences / whitespace, split lines, ignore blanks.
2. First line must match `^meal,<name>$` (else retry once, then fail).
3. Header must contain exactly the 9 columns above (order fixed).
4. Item rows: `kcal` integer 0–2000; grams 0–500; `confidence` 0–1; max 12 rows.
5. `unsure=1` requires non-empty `alternatives` (split on `|`, trim, ≤3).
6. `confidence < 0.6` → `unsure=true` regardless of the model's flag.
7. Name: trimmed, ≤60 chars, first letter capitalised.
8. Any malformed item row is dropped; if none remain → `failed`.

## 7. UX mapping

| Backend status | Screen state | Behaviour |
|---|---|---|
| — (in flight) | `Scanning` | Animated scanline + "Reading your plate…" checklist; backend call has a 25 s timeout → `failed` |
| `detected` | `Result.detected` | Item list (gold AI-estimate spark), portions editable later, Log CTA shows CSV totals |
| `unsure` | `Result.unsure` | Same list + amber "Rajma or chole?" choice card on the unsure row; picking an alternative swaps name & re-derives kcal from the food DB |
| `failed` | `Result.failed` | Neelu-hungry, "Couldn't see the food", Retake photo / Search food / Speak |
| camera denied | `Camera.denied` | Permission card (no backend call) |

Coins: +10 on every logged scan-sourced meal (spec: `Logged` screen "+10 coins").

## 8. Storage & privacy

- `scans` table: `id, user_id, created_at, status, model, meal_name, raw_csv, items_json, totals_json, image_path`.
- Images: `backend/uploads/scan_<id>.jpg`, served at `/uploads/...`, never uploaded elsewhere.
- The raw CSV is the audit trail: any number the app shows can be diffed against what Gemini returned.
- Everything (profile, meals, scans, weights, water, notifications, coins) lives in the
  local SQLite DB on the backend — "all the user information is stored back in the backend".

## 9. Test plan

| Level | What | How |
|---|---|---|
| Unit | CSV parser: happy path, meta row, unsure rows, malformed rows, fence stripping, limits | `backend/test/csv.test.ts` |
| Unit | Plan calculator: BMR/TDEE, realistic vs ambitious target date | `backend/test/plan.test.ts` |
| Unit | Oil multipliers on meal totals | `backend/test/meals.test.ts` |
| Integration | `/api/scan` with a fixture image against the **real** Gemini API (skipped if no key) | `backend/test/api.scan.test.ts` |
| Live E2E | Real camera photo of a thali → scan → result → logged → Home shows kcal | manual, script `scripts/e2e-scan.sh` |
| Client | Typecheck + production build | `npm run build` in `app/` |

## 10. Mascot videos (shipped after v1)

The out-of-scope v1 note ("video assets later") is now done. Neelu appears as
**generated motion clips** in 11 story slots.

### Pipeline

1. **Generation** — `scripts/omni-video.mjs` batch-generates ~3s clips from the
   magenta-keyed Neelu posters using **Gemini Omni** (`gemini-omni-1.1-flash`
   / `gemini-omni-flash-preview`). These models only support the **Interactions
   API** — `POST https://generativelanguage.googleapis.com/v1beta/interactions`
   with `input: [{type:'image',...},{type:'text',...}]` and
   `response_modalities: ['video','audio']` (**both** modalities required;
   video-only fails). Key must be sent as `x-goog-api-key` header. Prompts must
   stay short — long constrained prompts make the model answer with text.
   Key is supplied via `GEMINI_KEY` env var, never committed.
2. **Keying** — `scripts/key_video.py` extracts frames (ffmpeg, 512²@24fps),
   flood-fills the magenta border to alpha (TARGET 255,0,255 / NEAR 105 /
   FEATHER 150), despills magenta bleed at the touch edge band (with
   unpremultiply), erodes alpha slightly, and encodes **silent alpha VP9 WebM**
   (`-pix_fmt yuva420p -auto-alt-ref 0 -an`) so clips composite over any
   background.
3. **Placement** — `NeeluVideo` component (`app/src/components/ui.jsx`) renders
   `<video autoplay muted playsinline>` with the matching PNG as poster.
   One-shots (wave, peek, hop, dance, shrug, splash, flex) play **once and
   freeze on the last frame**; ambient slots (think, hungry, sleep, droop)
   soft-loop at most 3 times, then freeze — nothing loops forever.
   Safari is feature-gated back to the PNG (iOS can't composite alpha video).

### Slot map

| Slot (webm) | Story moment | Plays |
|---|---|---|
| splash | Onboarding splash, welcome, meet-neelu | once |
| wave | Home (normal/first day), Notify | once |
| think | Building plan, Search empty, Insights empty, scanning overlay | ≤3 loop |
| dance | Plan revealed, badge modal, goal-met Home | once |
| hop | Logged celebration | once |
| peek | AddSheet header | once |
| hungry | Missed-goal Home, Inbox empty | ≤3 loop |
| sleep | Night Home, Reminders | ≤3 loop |
| droop | Scan failed / camera denied | ≤3 loop |
| flex | Streak screen | once |
| shrug | Over-goal Home | once |

### Test plan (updated)

| Level | What | How |
|---|---|---|
| Live scan | 15 internet Indian dishes → detected, sane kcal | `scripts/food-scan-test.mjs` → `docs/FOOD-SCAN-RESULTS.md` |
| Browser E2E | Full journey 22 checks incl. real Gemini scan | `NODE_TLS_REJECT_UNAUTHORIZED=0 node scripts/e2e-browser.mjs` |
| Client | Typecheck + production build | `npm run build` in `app/` |

## 11. Out of scope (v1)

- Barcode & nutrition-label OCR (tabs exist; both route to Meal analysis).
- Cloud deploy — this is explicitly the *local* backend lab.

