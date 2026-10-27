# Food Scan Results — Internet-Sourced Indian Dishes

Ran `scripts/food-scan-test.mjs`: downloads real photos of 15 popular Indian
dishes from Wikimedia Commons, POSTs each to the live backend `/api/scan`
(Gemini 2.5 Flash), and records status, latency, parsed items.

**Date:** 2026-10-28 · **Backend:** local :4000 · **Model:** gemini-2.5-flash

## Results — 15/15 detected

| Dish (searched) | Status | Latency | Items parsed (kcal, confidence) |
|---|---|---|---|
| masala dosa | detected | 4.0s | Dosa 160 c1 · Coconut Chutney 50 c1 · Sambar 70 c1 |
| chicken biryani* | detected | 2.3s | Prawn Biryani 450 c0.9 · Boiled Egg 78 c1 |
| idli sambar | detected | 3.8s | Idli 39 c1 · Sambar 100 c1 |
| poha | detected | 5.0s | Poha (Flattened Rice) 170 c1 |
| chole bhature | detected | 4.5s | Bhature 350 c0.9 · Chole 250 c0.9 · Green Chillies 10 · Onion 15 · Lime 5 |
| samosa chaat | detected | 2.5s | Samosa 260 c1 · Chaat 350 c0.9 · Sev 100 c0.9 · Onion 10 c1 |
| aloo paratha | detected | 2.3s | Aloo Paratha 300 c0.9 · Butter 50 c0.9 |
| pav bhaji | detected | 4.0s | Pav Bhaji 350 c1 · Pav 150 c1 · Red Onion 10 c1 |
| vada pav | detected | 2.1s | Vada Pav 250 c0.8 · Mirchi Powder 20 c0.9 · Green Chilli 5 c0.9 |
| dhokla | detected | 3.7s | Dhokla 150 c0.9 |
| paneer tikka | detected | 2.5s | Paneer Tikka 300 c0.9 · Tomato 10 · Lime 5 · Cabbage 10 c0.8 |
| medu vada | detected | 2.2s | Medu Vada 150 c0.9 |
| dal makhani | detected | 4.2s | Dal Makhani 250 c0.9 |
| tandoori chicken | detected | 3.7s | Tandoori Chicken 1000 c1 (large platter photo) |
| rajma rice | detected | 2.4s | Rajma Masala 300 c0.9 · Basmati Rice 200 c0.9 · Kachumber Salad 50 c0.8 |

\* The Wikimedia photo returned for "chicken biryani" is actually a prawn
biryani — Gemini correctly identified **what is in the picture** rather than
the search label. This is the desired behaviour.

Non-food control (chair photo, earlier run): correctly returned `failed`.

## Assessment

- **Detection:** 15/15 correct dish identification, no hallucinated
  Western-generic items, no false UNSURE flags.
- **Confidence:** all 0.8–1.0.
- **Latency:** 2.1–5.0s (well under the 15s timeout; progressive UI hides wait).
- **Itemisation:** sides/condiments (chutney, lime, onion, sev) picked up
  consistently — matches the `INSIDEITEMS` guidance in the prompt.
- **Portion realism:** kcal values sit in sensible single-meal ranges
  (idli 39/pc → dal makhani 250 → biryani plate 450). Tandoori platter reads
  high but the photo is a full platter; user can uncheck items.

## Repro

```bash
# backend must be running on :4000 with a Gemini key in backend/.env
node scripts/food-scan-test.mjs   # writes photos to /tmp/mor-foods
```
