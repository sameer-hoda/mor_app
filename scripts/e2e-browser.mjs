import { chromium } from 'playwright';
import fs from 'node:fs';

const BASE = process.env.E2E_URL || 'https://localhost:5173';
const OUT = '/tmp/mor-e2e';
fs.mkdirSync(OUT, { recursive: true });
const errors = [];
const shots = [];

const browser = await chromium.launch({
  args: ['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream'],
});
const page = await browser.newPage({ viewport: { width: 360, height: 800 }, deviceScaleFactor: 2, ignoreHTTPSErrors: true });

page.on('console', (m) => { if (m.type() === 'error') errors.push(`console: ${m.text()} [@ ${m.location().url.split('/').pop()}:${m.location().lineNumber}]`); });
page.on('pageerror', (e) => errors.push(`pageerror: ${e.message}`));
page.on('requestfailed', (r) => errors.push(`reqfail: ${r.url()} ${r.failure()?.errorText}`));
page.on('response', (r) => { if (r.status() >= 400) errors.push(`http ${r.status()}: ${r.url()}`); });

const shot = async (name) => { const f = `${OUT}/${shots.length + 1}-${name}.png`; await page.screenshot({ path: f }); shots.push(f); };
const api = (p, opts = {}) => fetch(`${BASE}/api${p}`, { headers: { 'x-user-id': '1' }, ...opts }).then((r) => r.json());
const apiPost = (p, body) => api(p, { method: 'POST', headers: { 'content-type': 'application/json', 'x-user-id': '1' }, body: JSON.stringify(body || {}) });
const step = (t) => console.log(`\n== ${t}`);
const ok = (c, label) => console.log(`${c ? 'PASS' : 'FAIL'} ${label}`);

await fetch(`${BASE}/api/reset`, { method: 'POST', headers: { 'x-user-id': '1' } }).catch(() => {});
await fetch(`${BASE}/api/profile`, { method: 'POST', headers: { 'content-type': 'application/json', 'x-user-id': '1' }, body: '{}' }).catch(() => {});

step('1. Splash → Welcome → Meet Neelu');
await page.goto(BASE, { waitUntil: 'networkidle' });
await page.waitForTimeout(600);
await shot('splash');
if (page.url().includes('#/splash')) { await page.locator('.page').click(); }
await page.waitForURL('**/#/welcome', { timeout: 5000 }).catch(() => {});
await shot('welcome');
await page.getByRole('button', { name: 'Get started' }).click();
await page.waitForURL('**/#/meet-neelu');
await shot('meet-neelu');
ok(true, 'onboarding reached');

step('2. Goal → Diet → Body → Target → Activity → MealTimes');
await page.getByRole('button', { name: "Let's go" }).click();
await page.waitForURL('**/#/goal');
await page.getByRole('button', { name: /Lose weight/ }).click();
await shot('goal');
await page.getByRole('button', { name: 'Continue' }).click();
await page.waitForURL('**/#/diet');
await page.getByRole('button', { name: /Vegetarian/ }).click();
await shot('diet');
await page.getByRole('button', { name: 'Continue' }).click();
await page.waitForURL('**/#/body');
await page.getByRole('button', { name: 'Male', exact: true }).click();
const ageField = page.getByPlaceholder('28');
await ageField.fill('32');
await shot('body');
await page.getByRole('button', { name: 'Continue' }).click();
await page.waitForURL('**/#/target');
await shot('target');
await page.getByRole('button', { name: 'Continue' }).click();
await page.waitForURL('**/#/activity');
await shot('activity');
await page.getByRole('button', { name: /Lightly active|Light/i }).click();
await page.getByRole('button', { name: 'Continue' }).click();
await page.waitForURL('**/#/meal-times');
await shot('meal-times');

step('3. Build my plan → Building → Plan → Notify → Home');
await page.getByRole('button', { name: 'Build my plan' }).click();
await page.waitForURL('**/#/building');
await shot('building');
await page.waitForURL('**/#/plan', { timeout: 15000 });
await shot('plan');
const prof1 = await api('/profile');
ok(!!prof1.plan, `plan computed server-side: ${prof1.plan?.kcal_target} kcal`);
await page.getByRole('button', { name: 'Start' }).click();
await page.waitForURL('**/#/notify');
await shot('notify');
await page.getByRole('button', { name: 'Yes, remind me' }).click();
await page.waitForTimeout(400);
await page.getByRole('button', { name: 'Allow', exact: true }).click();
await page.waitForURL('**/#/home', { timeout: 8000 });
await page.waitForTimeout(1200);
await shot('home');

const home1 = await api('/home');
ok(home1.kcal_target > 0, `home loaded: target ${home1.kcal_target}, state ${home1.state}`);
ok(prof1.onboarded === 1, 'onboarded flag set');

step('4. Quick add via search (dosa)');
await page.getByRole('button', { name: 'Log a meal' }).click();
await page.waitForTimeout(400);
await shot('add-sheet');
await page.getByRole('button', { name: /Search food/ }).click();
await page.waitForURL('**/#/search**');
await page.locator('input.field').fill('dosa');
await page.waitForTimeout(400);
await shot('search');
await page.locator('.rowitem, .foodrow, [class*=food]').filter({ hasText: /Dosa/i }).first().click().catch(async () => {
  await page.getByText(/Dosa/i).first().click();
});
await page.waitForURL('**/#/food/**', { timeout: 5000 }).catch(() => {});
await page.waitForTimeout(400);
await shot('food-item');
const addBtn = page.getByRole('button', { name: /Add to/ });
ok(await addBtn.count() > 0, 'food item screen with add button');
await addBtn.click();
await page.waitForURL('**/#/home', { timeout: 5000 }).catch(async () => {
  if (page.url().includes('#/logged')) { await page.getByRole('button', { name: 'Done' }).click(); }
  await page.waitForURL('**/#/home', { timeout: 5000 });
});
await page.waitForTimeout(800);
const home2 = await api('/home');
ok(home2.kcal_eaten > 0, `quick-add logged: ${home2.kcal_eaten} kcal (was 0)`);
await shot('home-after-quickadd');

step('5. Scan flow with real Gemini (gallery → fixture thali)');
const homeBeforeScan = (await api('/home')).kcal_eaten;
await page.getByRole('button', { name: 'Log a meal' }).click();
await page.waitForTimeout(300);
await page.getByRole('button', { name: /Scan meal/ }).click();
await page.waitForURL('**/#/camera**');
await page.waitForTimeout(1500);
await shot('camera-live');
const camState = await page.evaluate(() => document.body.innerText.match(/Starting camera|Camera access needed/) ? 'blocked' : 'ok');
ok(camState === 'ok', `camera stream live (fake device) ${camState}`);
await page.setInputFiles('input[type=file]', '/Users/sameerhoda/Projects/mor_app/app/public/fixture/thali.jpg');
await page.waitForURL('**/#/scanning', { timeout: 8000 });
await shot('scanning');
await page.waitForURL('**/#/result**', { timeout: 90000 });
await page.waitForTimeout(1500);
await shot('result');
const logBtn = page.getByRole('button', { name: /Log \d+ kcal/ });
ok(await logBtn.count() > 0, 'result screen shows parsed items + total');
await logBtn.click();
await page.waitForURL('**/#/logged', { timeout: 8000 });
await page.waitForTimeout(900);
await shot('logged');
const homeAfterScan = (await api('/home')).kcal_eaten;
ok(homeAfterScan > homeBeforeScan, `scan logged: ${homeBeforeScan} → ${homeAfterScan} kcal`);

step('6. Meal detail (via Logged → See details)');
await page.getByRole('button', { name: 'See details' }).click();
await page.waitForURL('**/#/meal/**', { timeout: 6000 });
await page.waitForTimeout(700);
await shot('meal-detail');
const mid = page.url().split('/').pop();
ok(!!Number(mid), `meal persisted id=${mid}`);

step('7. Insights / Streak / Challenges / Weight / Inbox / Profile / Reminders');
await page.goto(`${BASE}/#/insights`);
await page.waitForTimeout(900);
await shot('insights');
const ins = await api('/insights');
ok(ins.days?.length === 7, `insights 7-day window, avg ${ins.avg}`);

await page.goto(`${BASE}/#/streak`);
await page.waitForTimeout(700);
await shot('streak');
const stk = await api('/streak');
ok(stk.streak >= 1, `streak ${stk.streak} lit today`);

await page.goto(`${BASE}/#/challenges`);
await page.waitForTimeout(700);
await shot('challenges');
const ch = await api('/challenges');
const claimable = ch.challenges?.find((c) => c.progress >= c.goal && !c.claimed);
if (claimable) {
  await page.getByRole('button', { name: /claim/i }).first().click().catch(() => {});
  await page.waitForTimeout(600);
}
await shot('challenges-after');

await page.goto(`${BASE}/#/weight`);
await page.waitForTimeout(700);
await shot('weight');
await page.getByRole('button', { name: /log|add/i }).first().click().catch(() => {});
await page.waitForTimeout(400);
const wInput = page.locator('input[type=number], input[inputmode=decimal]').last();
if (await wInput.count()) {
  await wInput.fill('79.4');
  await page.getByRole('button', { name: /save|log|done/i }).first().click().catch(() => {});
  await page.waitForTimeout(500);
}
const homeW = await api('/home');
ok(!!homeW.weight_kg, `weight logged: ${homeW.weight_kg} kg`);

await page.goto(`${BASE}/#/inbox`);
await page.waitForTimeout(700);
await shot('inbox');
const notifs = await api('/notifications');
ok(notifs.items?.length > 0, `${notifs.items?.length || 0} notifications`);

await page.goto(`${BASE}/#/profile`);
await page.waitForTimeout(700);
await shot('profile');
await page.getByText(/Daily target/).click().catch(() => {});
await page.waitForTimeout(600);
await shot('profile-edit');
await page.goBack().catch(() => {});

await page.goto(`${BASE}/#/reminders`);
await page.waitForTimeout(700);
await shot('reminders');

step('8. Coins & gamification sanity');
const homeF = await api('/home');
console.log(`final: kcal ${homeF.kcal_eaten}/${homeF.kcal_target}, coins ${homeF.coins}, streak ${homeF.streak}, water ${homeF.water}/8`);

step('ERROR SUMMARY');
const real = errors.filter((e) => !e.includes('favicon') && !e.includes('net::ERR_ABORTED'));
if (real.length) { console.log(`${real.length} issues:`); real.slice(0, 25).forEach((e) => console.log('  ' + e.slice(0, 220))); } else console.log('ZERO console errors / failed requests / 4xx-5xx 🎉');

await browser.close();
console.log(`\n${shots.length} screenshots in ${OUT}`);
