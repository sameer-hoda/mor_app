import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const env = Object.fromEntries(
  fs.readFileSync(path.join(ROOT, 'backend', '.env'), 'utf8').split('\n')
    .filter((l) => l.includes('=') && !l.trim().startsWith('#'))
    .map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()])
);
const KEY = process.env.GEMINI_API_KEY || env.GEMINI_API_KEY;
const MODEL = 'gemini-omni-1.2-flash';

const REF = path.join(ROOT, 'design-ref/organized/02-palace-door-sequence/door-closed-peacock-mural.jpg');
const OUT_DIR = path.join(ROOT, 'design-ref/palace-onboarding/generated');
fs.mkdirSync(OUT_DIR, { recursive: true });

const PROMPT = [
  'Locked-off camera in a Rajasthani palace courtyard at dawn.',
  'The ornate carved wooden palace door stays CLOSED the entire shot. Do not open the door.',
  'Warm low sun rakes across the carving. Fine dust drifts through the light shaft.',
  'A small brass diya at the door base flickers gently. Faint haze.',
  'Camera pushes in very slowly, about 8 percent over the shot.',
  'Photorealistic, cinematic, shallow depth of field. No text, no people, no watermark.',
].join(' ');

const body = {
  model: MODEL,
  input: [
    { type: 'image', mime_type: 'image/jpeg', data: fs.readFileSync(REF).toString('base64') },
    { type: 'text', text: PROMPT },
  ],
  response_format: { type: 'video', aspect_ratio: '9:16', resolution: '720p', duration: '3s' },
  generation_config: { video_config: { task: 'image_to_video' } },
};

const t0 = Date.now();
const res = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
  method: 'POST',
  headers: { 'x-goog-api-key': KEY, 'content-type': 'application/json' },
  body: JSON.stringify(body),
  signal: AbortSignal.timeout(300000),
});

const text = await res.text();
let json;
try { json = JSON.parse(text); } catch { console.error('non-JSON', res.status, text.slice(0, 400)); process.exit(1); }
if (!res.ok || json.error) {
  console.error('API error', res.status, JSON.stringify(json.error || json).slice(0, 600));
  process.exit(1);
}

const parts = (json.steps || []).flatMap((s) => s.outputs || s.content || []);
const video = parts.find((c) => c?.type === 'video');
if (!video?.data) {
  console.error('no video in response. keys:', Object.keys(json), 'partTypes:', parts.map((p) => p?.type).join(','));
  process.exit(1);
}

const out = path.join(OUT_DIR, process.argv[2] || 'door-plate-test.mp4');
fs.writeFileSync(out, Buffer.from(video.data, 'base64'));
console.log(JSON.stringify({
  file: path.relative(ROOT, out),
  mb: +(fs.statSync(out).size / 1048576).toFixed(2),
  seconds: +((Date.now() - t0) / 1000).toFixed(1),
  model: json.model,
  status: json.status,
  id: json.id,
  audio: parts.some((p) => p?.type === 'audio'),
}, null, 2));
