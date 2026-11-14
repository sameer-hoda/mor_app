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

const OUT = path.join(ROOT, 'assets-raw', 'goal-campaign');
fs.mkdirSync(OUT, { recursive: true });

const REFS = [
  'assets-raw/refs-campaign/palace-2.jpg',
  'assets-raw/refs-campaign/palace-3.jpg',
  'assets-raw/refs-campaign/pedestal.jpg',
];

const STYLE = [
  'Editorial luxury campaign photograph shot inside a Jaipur palace.',
  'A weathered vermilion-red lime-plaster archway with a cusped Mughal silhouette frames the shot, edges chipped and patinated, a second deeper arch falling into shadow beyond it.',
  'In the lower third, a fluted cream-marble pedestal stands on a pale inlaid marble floor.',
  'Dappled jaali lattice light falls across the wall and floor.',
  'Natural warm side light, medium format, 85mm, f/4, fine grain, matte film grade, rich but restrained colour.',
  'Medium shot: the pedestal stands close to camera and occupies the lower half of the frame; the object on it is the hero, about one third of the frame height and readable at thumbnail size.',
  'Perfectly symmetrical, straight-on one-point perspective, the pedestal on the exact vertical centreline.',
  'No text, no letters, no watermark, no people.',
].join(' ');

const SUBJECTS = {
  lose: 'On the pedestal: a single iridescent peacock feather, standing upright and leaning slightly, catching the light.',
  maintain: 'On the pedestal: a small antique brass balance scale, beam perfectly level, a feather on one pan and a matching brass weight on the other.',
  gain: 'On the pedestal: a neat stack of antique brass weights, each slightly larger than the one below.',
  healthy: 'On the pedestal: a polished brass thali holding fresh Indian produce — okra, bitter gourd, tomato, curry leaves, and a small bowl of dal.',
  empty: 'The pedestal top is completely empty. No object on it.',
};

const id = process.argv[2];
const subject = SUBJECTS[id];
if (!subject) { console.error('use:', Object.keys(SUBJECTS).join(', ')); process.exit(1); }

const refParts = REFS.filter((p) => fs.existsSync(path.join(ROOT, p))).flatMap((p) => [
  { text: `Reference (${path.basename(p)}): match its palette, plaster texture, arch geometry and lighting. Do not copy its subject.` },
  { inline_data: { mime_type: 'image/jpeg', data: fs.readFileSync(path.join(ROOT, p)).toString('base64') } },
]);

const EDIT = process.argv.includes('--edit');
const MASTER = path.join(OUT, 'goal-empty.png');
const parts = EDIT
  ? [
      { text: 'Edit this exact photograph. Keep the architecture, wall, pedestal, floor, light, shadows and framing pixel-identical. Change only what rests on top of the pedestal.' },
      { inline_data: { mime_type: 'image/png', data: fs.readFileSync(MASTER).toString('base64') } },
      { text: `${subject.replace(/^On the pedestal: /, '')} The object rests directly on the flat top cap of the pedestal: its base touches the stone exactly, with a soft contact shadow. Do not float it. Size it to fit the cap comfortably and read clearly. No text, no letters, no watermark, no people. Aspect ratio 3:4, portrait.` },
    ]
  : [...refParts, { text: `${STYLE}\n\n${subject}\n\nAspect ratio 3:4, portrait.` }];
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
