import { chromium } from 'playwright';
const routes = ['splash','welcome','meet-neelu','goal','diet','body','target','activity','meal-times','building','plan','notify','home','add','camera','scanning','result','logged','meal/1','search','food','insights','streak','challenges','weight','inbox','profile','reminders'];
const b = await chromium.launch();
const p = await b.newPage({ viewport: { width: 360, height: 800 }, ignoreHTTPSErrors: true });
for (const r of routes) {
  const errs = [];
  const h1 = (e) => errs.push(e.message.slice(0, 180));
  const h2 = (m) => { if (m.type() === 'error' && !m.text().includes('Failed to load resource')) errs.push(m.text().slice(0, 180)); };
  p.on('pageerror', h1); p.on('console', h2);
  await p.goto(`https://localhost:5173/#/${r}`, { waitUntil: 'domcontentloaded' });
  await p.reload({ waitUntil: 'domcontentloaded' });
  await p.waitForTimeout(1100);
  const txt = await p.evaluate(() => document.body.innerText.trim().length);
  console.log(`${txt === 0 ? 'CRASH' : 'ok   '} /${r} ${errs.length ? '→ ' + errs[0] : ''}`);
  p.removeAllListeners('pageerror'); p.removeAllListeners('console');
}
await b.close();
