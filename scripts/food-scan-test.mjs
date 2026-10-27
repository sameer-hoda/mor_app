import fs from 'node:fs';
import path from 'node:path';

const DIR = '/tmp/mor-foods';
fs.mkdirSync(DIR, { recursive: true });
const dishes = ['masala dosa', 'chicken biryani', 'idli sambar', 'poha', 'chole bhature', 'samosa chaat', 'aloo paratha', 'pav bhaji', 'vada pav', 'dhokla', 'paneer tikka', 'medu vada', 'dal makhani', 'tandoori chicken', 'rajma rice'];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function grab(q) {
  const api = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(q)}&gsrlimit=3&gsrnamespace=6&prop=imageinfo&iiprop=url|mime&iiurlwidth=900&format=json`;
  const j = await fetch(api, { headers: { 'User-Agent': 'mor-e2e/1.0 (testing)' } }).then(r => r.json());
  const pages = Object.values(j.query?.pages || {});
  for (const p of pages) {
    const info = p.imageinfo?.[0];
    if (!info || !['image/jpeg', 'image/png'].includes(info.mime)) continue;
    const buf = Buffer.from(await fetch(info.thumburl || info.url).then(r => r.arrayBuffer()));
    if (buf.length < 20000) continue;
    const f = path.join(DIR, q.replace(/[^a-z]+/gi, '_') + '.jpg');
    fs.writeFileSync(f, buf);
    return f;
  }
  return null;
}

for (const d of dishes) {
  let f = null;
  try { f = await grab(d); } catch { /* rate limited or miss */ }
  await sleep(1800);
  console.log(f ? `got ${d} → ${f} (${(fs.statSync(f).size / 1024).toFixed(0)} KB)` : `MISS ${d}`);
}

await sleep(1500);
for (const f of fs.readdirSync(DIR)) {
  await sleep(1200);
  const b64 = fs.readFileSync(path.join(DIR, f)).toString('base64');
  const t0 = Date.now();
  let r;
  try {
    r = await fetch('http://localhost:4000/api/scan', { method: 'POST', headers: { 'content-type': 'application/json', 'x-user-id': '1' }, body: JSON.stringify({ image_base64: b64, mime: 'image/jpeg', slot: 'lunch' }) }).then((x) => x.json());
  } catch (e) {
    console.log(`  scan error: ${e.message}`);
    continue;
  }
  const ms = Date.now() - t0;
  console.log(`\n${f} → ${r.status} (${ms} ms)`);
  if (r.items?.length) console.log('  ' + r.items.map(i => `${i.name} ${i.kcal}kcal c${i.confidence}${i.unsure ? ' UNSURE' : ''}`).join(' | '));
  if (r.error) console.log('  error:', r.error);
}
