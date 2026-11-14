import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const env = Object.fromEntries(
  fs.readFileSync(path.join(ROOT, 'backend', '.env'), 'utf8').split('\n')
    .filter((l) => l.includes('=') && !l.trim().startsWith('#'))
    .map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()])
);
const KEY = process.env.GEMINI_API_KEY || env.GEMINI_API_KEY;
const MODEL = 'nano-banana-pro-preview';
const BASE = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`;

const OUT = path.join(ROOT, 'assets-raw', 'goal-art');
fs.mkdirSync(OUT, { recursive: true });

const REFS = [
  'assets-raw/goal-art/goal-lose.png',
  'design-ref/palace-onboarding/assets-ai/a5-jharokha.png',
  'design-ref/palace-onboarding/assets-ai/a7-arches.png',
  'design-ref/organized/03-onboarding/ref-commons-mughal-peacock-01-1.jpg',
];

const STYLE = [
  'An ornate still-life painted inside a cusped Rajasthani jharokha arch, viewed straight on.',
  'Aged lime-plaster wall in deep terracotta and rose, fine gold-leaf linework, peacock-green and cream accents, visible hand-painted fresco texture.',
  'Warm dawn light enters the niche from the upper left and falls across the object; soft, real shadow.',
  'Centred symmetrical composition. The arch fills the frame edge to edge, no wall margin beyond it.',
  'The object is large, centred and clearly readable at thumbnail size, about one third of the frame width, sitting in the lower third on the ledge.',
  'Photorealistic photograph of a hand-painted palace wall, high detail, matte finish.',
  'No text, no letters, no watermark, no people, no deities, no extra frames.',
].join(' ');

const SUBJECTS = {
  lose:
    'Inside the arch: a single iridescent peacock feather rests on a carved cream-stone ledge, the hero of the shot, catching warm light. A small antique brass balance sits softly out of focus behind it. Mood: lightness, ease, letting go.',
  maintain:
    'Inside the arch: a small antique brass balance scale in perfect equilibrium on a carved cream-stone ledge. A peacock feather rests on one pan, a matching brass weight on the other, beam dead level. Mood: steady, calm, unchanged.',
  gain:
    'Inside the arch: a neat stack of antique brass weights of increasing size on a carved cream-stone ledge, each one slightly larger than the last. A small brass balance stands behind them. Mood: building gently, getting stronger.',
  healthy:
    'Inside the arch: a polished brass thali plate seen from above holding fresh Indian produce — okra, bitter gourd, tomato, curry leaves, and a small bowl of dal — on a carved cream-stone ledge. Mood: fresh, colourful, everyday nourishment.',
};

function refParts() {
  return REFS.filter((p) => fs.existsSync(path.join(ROOT, p))).flatMap((p) => {
    const mime = p.endsWith('.png') ? 'image/png' : 'image/jpeg';
    return [
      { text: `Reference (${path.basename(p)}): match its palette, plaster texture, ornament and arch geometry. Do not copy its subject.` },
      { inline_data: { mime_type: mime, data: fs.readFileSync(path.join(ROOT, p)).toString('base64') } },
    ];
  });
}

const id = process.argv[2] || 'lose';
const subject = SUBJECTS[id];
if (!subject) { console.error('unknown id. use:', Object.keys(SUBJECTS).join(', ')); process.exit(1); }

const EDIT = process.argv.includes('--edit');
const BASE_IMG = path.join(ROOT, 'assets-raw/goal-art/goal-maintain.png');

const parts = EDIT
  ? [
      { text: 'Edit this exact photograph. Keep the niche, arch, wall, ledge, ornament, lighting, camera angle and colours pixel-identical. Replace ONLY the objects resting on the ledge.' },
      { inline_data: { mime_type: 'image/png', data: fs.readFileSync(BASE_IMG).toString('base64') } },
      { text: `New objects on the ledge: ${subject.replace(/^Inside the arch: /, '')}\n\nDo not add text, letters, watermarks or people. Do not change the architecture. Aspect ratio 3:4, portrait.` },
    ]
  : [...refParts(), { text: `${STYLE}\n\n${subject}\n\nAspect ratio 3:4, portrait.` }];
const outFile = path.join(OUT, `goal-${id}${EDIT ? '-v2' : ''}.png`);

for (let attempt = 1; attempt <= 3; attempt++) {
  const t0 = Date.now();
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'x-goog-api-key': KEY },
    body: JSON.stringify({
      contents: [{ role: 'user', parts }],
      generationConfig: { responseModalities: ['IMAGE'], imageConfig: { aspectRatio: '3:4' } },
    }),
    signal: AbortSignal.timeout(180000),
  });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch { console.error('non-JSON', res.status, text.slice(0, 300)); continue; }
  if (!res.ok || json.error) { console.error('API', res.status, JSON.stringify(json.error || json).slice(0, 400)); continue; }
  const cand = json.candidates?.[0];
  const img = (cand?.content?.parts || []).find((p) => p.inlineData || p.inline_data);
  const data = img?.inlineData?.data || img?.inline_data?.data;
  if (!data) { console.error('no image:', cand?.finishReason, JSON.stringify(cand).slice(0, 300)); continue; }
  fs.writeFileSync(outFile, Buffer.from(data, 'base64'));
  console.log(JSON.stringify({ file: path.relative(ROOT, outFile), kb: Math.round(fs.statSync(outFile).size / 1024), seconds: +((Date.now() - t0) / 1000).toFixed(1) }, null, 2));
  process.exit(0);
}
console.error('failed after 3 attempts');
process.exit(1);
