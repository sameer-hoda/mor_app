import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, '..');
const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'mor-test-'));
const PORT = 4111;
const B = `http://localhost:${PORT}/api`;

let child;

before(async () => {
  child = spawn(process.execPath, ['src/server.js'], {
    cwd: ROOT,
    env: { ...process.env, PORT: String(PORT), MOR_DATA_DIR: TMP, MOR_UPLOADS_DIR: path.join(TMP, 'uploads'), GEMINI_API_KEY: '' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  child.stderr.on('data', (d) => process.env.DEBUG_TESTS && process.stderr.write(d));
  for (let i = 0; i < 40; i++) {
    try { const r = await fetch(`${B}/health`); if (r.ok) return; } catch {}
    await new Promise((r) => setTimeout(r, 250));
  }
  throw new Error('server did not start');
});

after(() => { child.kill('SIGTERM'); fs.rmSync(TMP, { recursive: true, force: true }); });

test('health', async () => {
  const r = await fetch(`${B}/health`);
  const j = await r.json();
  assert.equal(j.ok, true);
  assert.equal(j.key, false);
});

test('profile: onboarding upsert + plan computed', async () => {
  const post = (body) => fetch(`${B}/profile`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }).then((r) => r.json());
  await post({ goal: 'lose', diet: 'veg' });
  await post({ sex: 'female', age: 28, height_cm: 170, weight_kg: 72 });
  const j = await post({ target_weight_kg: 66, activity: 'light', complete: true });
  assert.equal(j.onboarded, 1);
  assert.ok(j.plan.kcal_target >= 1200);
  assert.ok(j.plan.protein_g > 0);
  assert.equal(j.plan.goal_line, '72 → 66 kg');
});

test('meal create → home summary updates', async () => {
  const r = await fetch(`${B}/meals`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    slot: 'lunch', name: 'Thali lunch', source: 'scan',
    items: [
      { name: 'Dal tadka', kcal: 150, protein_g: 6, carbs_g: 15, fat_g: 6, portion: '1 bowl' },
      { name: 'Phulka roti', kcal: 120, protein_g: 4, carbs_g: 20, fat_g: 3, portion: '2 pieces' },
    ],
  }) }).then((r) => r.json());
  assert.equal(r.kcal, 270);
  assert.equal(r.coins_awarded, 10);

  const home = await fetch(`${B}/home`).then((r) => r.json());
  assert.equal(home.meals.lunch.logged, true);
  assert.equal(home.meals.lunch.kcal, 270);
  assert.equal(home.kcal_eaten, 270);
  assert.equal(home.coins, 10);
  assert.equal(home.streak, 1);
  assert.ok(home.meals.breakfast.open);
});

test('meal with oil level multiplies kcal', async () => {
  const r = await fetch(`${B}/meals`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    slot: 'snack', source: 'search', oil_level: 'more',
    items: [{ name: 'Samosa', kcal: 130, protein_g: 2, carbs_g: 13, fat_g: 7, portion: '1 piece' }],
  }) }).then((r) => r.json());
  assert.equal(r.kcal, 150);
  assert.equal(r.fat_g, 9);
});

test('food search', async () => {
  const j = await fetch(`${B}/foods?q=paneer`).then((r) => r.json());
  assert.ok(j.items.some((f) => f.name === 'Paneer tikka'));
  const none = await fetch(`${B}/foods?q=zzz`).then((r) => r.json());
  assert.equal(none.items.length, 0);
});

test('water increments', async () => {
  const j = await fetch(`${B}/water`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ delta: 3 }) }).then((r) => r.json());
  assert.equal(j.glasses, 3);
  const j2 = await fetch(`${B}/water`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ delta: 99 }) }).then((r) => r.json());
  assert.equal(j2.glasses, 8);
});

test('weight logging', async () => {
  const j = await fetch(`${B}/weight`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ kg: 71.8 }) }).then((r) => r.json());
  assert.equal(j.kg, 71.8);
  const bad = await fetch(`${B}/weight`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ kg: 500 }) });
  assert.equal(bad.status, 400);
});

test('insights shape', async () => {
  const j = await fetch(`${B}/insights`).then((r) => r.json());
  assert.equal(j.days.length, 7);
  assert.equal(j.protein_goal, 76);
  assert.ok(j.days[6].kcal > 0);
});

test('streak calendar', async () => {
  const j = await fetch(`${B}/streak`).then((r) => r.json());
  assert.ok(j.days.length >= 28);
  assert.equal(j.best, 1);
});

test('challenges progress + claim', async () => {
  const j = await fetch(`${B}/challenges`).then((r) => r.json());
  assert.equal(j.challenges.length, 7);
  const fb = j.challenges.find((c) => c.key === 'first_bite');
  assert.equal(fb.done, true);
  const claim = await fetch(`${B}/challenges/claim`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ key: 'first_bite' }) }).then((r) => r.json());
  assert.equal(claim.awarded, 10);
  const notDone = await fetch(`${B}/challenges/claim`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ key: 'home_cooked' }) });
  assert.equal(notDone.status, 400);
});

test('scan rejects empty body', async () => {
  const r = await fetch(`${B}/scan`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({}) });
  assert.equal(r.status, 400);
});

test('notifications list + read-all', async () => {
  await fetch(`${B}/notifications`).then((r) => r.json());
  const j = await fetch(`${B}/notifications/read`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' }).then((r) => r.json());
  assert.equal(j.ok, true);
});

test('insights week vs month ranges', async () => {
  const w = await fetch(`${B}/insights`).then((r) => r.json());
  assert.equal(w.days.length, 7);
  assert.equal(w.total_days, 7);
  const m = await fetch(`${B}/insights?range=month`).then((r) => r.json());
  assert.equal(m.range, 'month');
  assert.equal(m.days.length, 30);
  assert.equal(m.total_days, 30);
  // Same trailing day in both views.
  assert.equal(m.days[m.days.length - 1].date, w.days[w.days.length - 1].date);
});
