import fs from 'node:fs';

const KEY = process.env.GEMINI_KEY;
const MODEL = 'gemini-omni-1.1-flash';

export function posterB64(mood) {
  const f = `/tmp/omni/inputs/neelu-${mood}-magenta.png`;
  return fs.readFileSync(f).toString('base64');
}

export async function makeVideo({ mood, action, seconds, oneShot }) {
  const motion = oneShot
    ? `One shot about ${seconds}s, ends holding the final pose`
    : `About ${seconds}s, returns to the same pose (loop)`;
  const ladder = [
    `Animate this chibi peacock mascot. Static camera, centred. Keep the magenta background uniform and unchanged. Motion: ${action}. ${motion}. Output video only.`,
    `Animate this mascot. Camera fixed. Magenta background unchanged. Motion: ${action}. ${motion}. Video only.`,
    `Animate this mascot: ${action}. ${motion}. Video only, magenta background.`,
  ];
  let lastErr;
  for (const prompt of ladder) {
    const body = {
      model: MODEL,
      input: [
        { type: 'image', mime_type: 'image/png', data: posterB64(mood) },
        { type: 'text', text: prompt },
      ],
      response_modalities: ['video', 'audio'],
    };
    const r = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
      method: 'POST',
      headers: { 'x-goog-api-key': KEY, 'content-type': 'application/json' },
      body: JSON.stringify(body),
    });
    const j = await r.json();
    if (j.error) { lastErr = JSON.stringify(j.error).slice(0, 240); continue; }
    const out = (j.steps || []).flatMap((s) => s.outputs || s.content || []).find((c) => c?.type === 'video');
    if (out) {
      const f = `/tmp/omni/raw/${mood}-${action.slice(0, 18).replace(/[^a-z]+/gi, '_')}.mp4`;
      const fs = await import('node:fs');
      fs.writeFileSync(f, Buffer.from(out.data, 'base64'));
      return { file: f, bytes: fs.statSync(f).size };
    }
    lastErr = 'no video output';
  }
  return { error: lastErr };
}

const SLOTS = [
  { id: 'wave', mood: 'happy', action: 'lifts one wing and waves twice, small hop, crest bulbs bounce', seconds: 1.5, oneShot: true },
  { id: 'peek', mood: 'happy', action: 'rises up from below the bottom edge, peeks with a quick blink, settles', seconds: 1, oneShot: true },
  { id: 'hop', mood: 'love', action: 'two little hops, small pink hearts pop around the head and fade', seconds: 1.5, oneShot: true },
  { id: 'dance', mood: 'joy', action: 'shimmies the fanned tail side to side, gold sparkles twinkle, ends with wings up', seconds: 2, oneShot: true },
  { id: 'think', mood: 'thinking', action: 'taps beak with a wing tip, eyes dart left and right, a tiny golden question mark pops above the head and fades', seconds: 2, oneShot: false },
  { id: 'hungry', mood: 'hungry', action: 'rubs belly with one wing, looks up pleading, slight sway', seconds: 2, oneShot: false },
  { id: 'sleep', mood: 'sleepy', action: 'dozes with slow breathing, head dips slightly, two small blue Z letters drift up and fade', seconds: 3, oneShot: false },
  { id: 'droop', mood: 'hungry', action: 'crest stems droop, small sigh, then crest perks back up hopefully', seconds: 2, oneShot: false },
  { id: 'shrug', mood: 'thinking', action: 'shrugs both wings, small reassuring smile, settles', seconds: 1.5, oneShot: true },
  { id: 'splash', mood: 'cool', action: 'tail feathers shimmer with a quick fan bounce, sunglasses glint once, he gives a confident little nod', seconds: 1.5, oneShot: true },
  { id: 'flex', mood: 'cool', action: 'bounces once with wings up, smirks confidently, sunglasses glint', seconds: 1.2, oneShot: true },
];

async function runBatch() {
  const fs = await import('node:fs');
  const out = [];
  for (const s of SLOTS) {
    let r;
    for (let a = 0; a < 2 && !r?.file; a++) {
      r = await makeVideo({ mood: s.mood, action: s.action, seconds: s.seconds, oneShot: s.oneShot });
      if (r?.error) console.error(`retry ${s.id}: ${r.error}`);
    }
    const rec = { ...s, ...r };
    out.push(rec);
    console.log(rec.file ? `OK ${s.id} (${rec.ms} ms, ${(rec.bytes / 1024).toFixed(0)} KB)` : `FAIL ${s.id}: ${rec.error}`);
    fs.appendFileSync('/tmp/omni/progress.log', JSON.stringify(rec) + '\n');
  }
  fs.writeFileSync('/tmp/omni/batch.json', JSON.stringify(out, null, 1));
  console.log('BATCH DONE', out.filter((o) => o.file).length + '/' + out.length);
}

if (process.argv[2] === 'batch') await runBatch();
else if (process.argv[2] === 'test') {
  const r = await makeVideo({ mood: 'happy', action: 'lifts one wing and waves twice, small hop, crest bulbs bounce', seconds: 1.5, oneShot: true });
  console.log(r);
}
