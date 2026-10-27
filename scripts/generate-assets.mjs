import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const env = Object.fromEntries(
  fs.readFileSync(path.join(ROOT, 'backend', '.env'), 'utf8').split('\n')
    .filter((l) => l.includes('=') && !l.trim().startsWith('#'))
    .map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()])
);
const KEY = process.env.GEMINI_API_KEY || env.GEMINI_API_KEY;
const MODEL = 'gemini-3-pro-image';
const BASE = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`;

const OUT_ASSETS = path.join(ROOT, 'app', 'src', 'assets');
const OUT_RAW = path.join(ROOT, 'assets-raw');
const OUT_FIXTURES = path.join(ROOT, 'backend', 'fixtures');
for (const d of [OUT_ASSETS, OUT_RAW, OUT_FIXTURES, path.join(OUT_ASSETS, 'neelu'), path.join(OUT_ASSETS, 'scene')]) fs.mkdirSync(d, { recursive: true });

const STYLE = 'Clean flat vector illustration, soft cel shading, rounded friendly shapes, sticker clarity. No text, no letters, no watermark, no logos, no humans, no religious symbols.';

function refImage() {
  const p = path.join(OUT_RAW, 'neelu-master.png');
  if (!fs.existsSync(p)) return null;
  return { mime_type: 'image/png', data: fs.readFileSync(p).toString('base64') };
}

async function gen(prompt, outName, { aspect = '1:1', reference = true } = {}) {
  const out = path.join(OUT_RAW, outName);
  if (fs.existsSync(out)) { console.log(`skip ${outName}`); return out; }
  const parts = [];
  if (reference) {
    const ref = refImage();
    if (ref) parts.push({ text: 'Use this exact character design as the reference. Keep the same design, colours and proportions.' });
    if (ref) parts.push({ inline_data: ref });
  }
  parts.push({ text: `${prompt}\n\n${STYLE}\nAspect ratio: ${aspect}.` });
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await fetch(BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': KEY },
        body: JSON.stringify({
          contents: [{ role: 'user', parts }],
          generationConfig: { responseModalities: ['IMAGE'], imageConfig: { aspectRatio: aspect } },
        }),
        signal: AbortSignal.timeout(120000),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}: ${(await res.text()).slice(0, 300)}`);
      const json = await res.json();
      const imgPart = (json.candidates?.[0]?.content?.parts || []).find((p) => p.inlineData || p.inline_data);
      const data = imgPart?.inlineData?.data || imgPart?.inline_data?.data;
      if (!data) throw new Error('no image part: ' + JSON.stringify(json.candidates?.[0]?.finishReason || json.error || {}));
      fs.writeFileSync(out, Buffer.from(data, 'base64'));
      console.log(`done ${outName} (${Math.round(data.length * 0.75 / 1024)} KB)`);
      return out;
    } catch (e) {
      console.error(`attempt ${attempt} failed for ${outName}: ${e.message}`);
      if (attempt === 3) throw e;
      await new Promise((r) => setTimeout(r, 3000));
    }
  }
}

const MOODS = {
  happy: 'happy and smiling, arms relaxed',
  cool: 'wearing black sunglasses with thin gold rims, confident smirk',
  joy: 'overjoyed, eyes closed in happy arcs, beak wide open, tiny gold sparkles around',
  love: 'with pink heart-shaped eyes, delighted',
  hungry: 'hungry and pleading, eyes looking up, worried brows, tiny drool drop',
  thinking: 'thinking, eyes glancing up-right, one raised brow, one wing-tip touching the beak',
  sleepy: 'sleepy, eyes closed, two small blue Z letters floating above',
};

const master = await gen(
  'Character turnaround sheet of Neelu, a chibi Indian peacock mascot, shown four times in a row: front view, three-quarter view, side view, back view, all standing, same scale, evenly spaced on a flat ivory #F8F5EE background. Oversized round cobalt-blue head and small round cobalt body with a lighter blue belly, three thin crest stems on top each ending in a small teal bulb with a gold centre, big glossy black eyes with two white highlights, short white stripe under each eye, soft pink blush cheeks, small rounded golden-yellow beak, tiny orange feet, half-open fanned green tail with six large feather eyes ringed gold, teal and deep indigo. Happy neutral expression.',
  'neelu-master.png', { aspect: '1:1', reference: false }
);
console.log('MASTER DONE');
await gen('Full-body front view of the chibi peacock mascot Neelu, standing centred, feet at 88% of the frame height, fanned tail fully visible. Plain flat pure magenta #FF00FF background with no shadow on it, generous empty margin on all sides.', 'neelu-chroma-ref.png');
for (const [mood, desc] of Object.entries(MOODS)) {
  await gen(`Full-body front view of the chibi peacock mascot Neelu, standing centred, feet at 88% of the frame height, fanned tail fully visible, mood: ${desc}. Exactly the same design, colours and proportions as the reference. Plain flat pure magenta #FF00FF background with no shadow on it, generous empty margin on all sides.`, `neelu-${mood}.png`);
}
await gen('Head-and-crest close-up of the chibi peacock Neelu wearing black sunglasses with thin gold rims, confident smile, facing camera, centred, occupying the central 60% of the frame so it survives circular and squircle masks. Flat vector, bold simple shapes that read at 48 px. Plain flat pure magenta #FF00FF background.', 'icon.png', { reference: true });
await gen('A realistic photograph of an Indian thali on a steel plate: dal tadka, aloo gobhi, jeera rice, two phulka rotis, raita and salad, shot from directly above on a wooden table, warm daylight, appetising, no text.', 'thali-photo.jpg', { aspect: '1:1', reference: false });
await gen('Minimal flat vector scene for a mobile app header, no character: a soft pale teal sky (#CFE7E1), a large cream Mughal jharokha arch (#EAF5F1) centred with a thin sage inner outline, a warm sandstone terrace strip along the bottom (#E3C394), a small pale sun top-right. Lots of calm empty space in the middle of the arch where a mascot will stand on the terrace. Very subtle, airy, premium, Indian.', 'scene-day.png', { aspect: '4:3', reference: false });
await gen('Minimal flat vector night scene for a mobile app header, no character: a deep indigo night sky (#1E2D4F) with a crescent moon and a few small stars, a large cream Mughal jharokha arch (#2A3B5E) centred with a thin outline, a warm sandstone terrace strip along the bottom (#3B2F46). Lots of calm empty space in the middle of the arch where a mascot will stand on the terrace. Very subtle, calm, premium, Indian.', 'scene-night.png', { aspect: '4:3', reference: false });
console.log('ALL ASSETS DONE');
