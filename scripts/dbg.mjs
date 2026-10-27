import { chromium } from 'playwright';
const b = await chromium.launch();
const p = await b.newPage({ viewport: { width: 360, height: 800 }, ignoreHTTPSErrors: true });
p.on('console', (m) => { if (m.text().includes('same key')) console.log('FULL TEXT >>>\n' + m.text() + '\n<<<'); });
for (const r of ['home', 'add', 'insights', 'streak', 'challenges', 'weight', 'inbox', 'profile', 'reminders', 'search', 'food?name=Dosa&slot=lunch', 'meal/10']) {
  await p.goto('https://localhost:5173/#/' + r, { waitUntil: 'domcontentloaded' });
  await p.reload({ waitUntil: 'domcontentloaded' });
  await p.waitForTimeout(1000);
}
await b.close();
