import express from 'express';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { db, today } from './db.js';
import { parseScanCsv, classify, totals, applyOil } from './scanCsv.js';
import { computePlan } from './plan.js';
import * as gemini from './gemini.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
export const router = express.Router();
export const uploadsDir = process.env.MOR_UPLOADS_DIR || path.join(__dirname, '..', 'uploads');
fs.mkdirSync(uploadsDir, { recursive: true });

const SLOT_ORDER = ['breakfast', 'lunch', 'snack', 'dinner'];
const DEFAULT_TIMES = { breakfast: '08:30', lunch: '13:30', snack: '17:00', dinner: '21:00' };

function getUser(req) {
  const id = Number(req.header('x-user-id') || 1);
  let u = db.prepare('SELECT * FROM users WHERE id=?').get(id);
  if (!u) { db.prepare('INSERT INTO users (id) VALUES (?)').run(id); u = db.prepare('SELECT * FROM users WHERE id=?').get(id); }
  return u;
}

function mealTimes(u) {
  try { return { ...DEFAULT_TIMES, ...JSON.parse(u.meal_times || '{}') }; } catch { return DEFAULT_TIMES; }
}

function mealDates(userId) {
  return db.prepare('SELECT DISTINCT date FROM meals WHERE user_id=? ORDER BY date DESC').all(userId).map((r) => r.date);
}

export function calcStreak(dates) {
  const set = new Set(dates);
  let streak = 0;
  const t = today(0);
  if (set.has(t)) streak++;
  for (let i = 1; i < 400; i++) {
    if (set.has(today(-i))) streak++;
    else break;
  }
  return streak;
}

export function bestStreak(dates) {
  const sorted = [...new Set(dates)].sort();
  let best = 0, cur = 0, prev = null;
  for (const d of sorted) {
    if (prev) {
      const diff = (new Date(d) - new Date(prev)) / 86400000;
      cur = diff === 1 ? cur + 1 : 1;
    } else cur = 1;
    best = Math.max(best, cur);
    prev = d;
  }
  return best;
}

function seededSteps(userId, date) {
  let h = 0;
  const s = `${userId}:${date}`;
  for (const c of s) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  return 3000 + (h % 6000);
}

function coinsAward(source) { return source === 'scan' ? 10 : 5; }

router.get('/health', (req, res) => {
  res.json({ ok: true, model: gemini.model, key: Boolean(process.env.GEMINI_API_KEY) });
});

router.get('/profile', (req, res) => {
  const u = getUser(req);
  const plan = db.prepare('SELECT * FROM plan WHERE user_id=?').get(u.id) || null;
  res.json({ ...u, meal_times: JSON.parse(u.meal_times), plan });
});

router.post('/profile', (req, res) => {
  const u = getUser(req);
  const b = req.body || {};
  const fields = ['goal', 'diet', 'sex', 'activity', 'units', 'name'];
  const nums = ['age', 'height_cm', 'weight_kg', 'target_weight_kg'];
  const updates = {};
  for (const f of fields) if (b[f] !== undefined) updates[f] = String(b[f]);
  for (const f of nums) if (b[f] !== undefined) updates[f] = Number(b[f]) || null;
  if (b.meal_times) {
    const merged = { ...mealTimes(u), ...b.meal_times };
    for (const s of SLOT_ORDER) merged[s] = merged[s] || DEFAULT_TIMES[s];
    updates.meal_times = JSON.stringify(merged);
  }
  if (b.reminders_on !== undefined) updates.reminders_on = b.reminders_on ? 1 : 0;
  updates.onboarded = b.complete ? 1 : u.onboarded;
  updates.updated_at = new Date().toISOString();

  const keys = Object.keys(updates);
  if (keys.length) {
    db.prepare(`UPDATE users SET ${keys.map((k) => `${k}=?`).join(', ')} WHERE id=?`).run(...keys.map((k) => updates[k]), u.id);
  }
  let plan = db.prepare('SELECT * FROM plan WHERE user_id=?').get(u.id);
  if (b.complete) {
    const fresh = db.prepare('SELECT * FROM users WHERE id=?').get(u.id);
    const computed = computePlan(fresh);
    db.prepare(`INSERT INTO plan (user_id, kcal_target, protein_g, carbs_g, fat_g, goal_line, target_date)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(user_id) DO UPDATE SET kcal_target=excluded.kcal_target, protein_g=excluded.protein_g,
      carbs_g=excluded.carbs_g, fat_g=excluded.fat_g, goal_line=excluded.goal_line, target_date=excluded.target_date`)
      .run(u.id, computed.kcal_target, computed.protein_g, computed.carbs_g, computed.fat_g, computed.goal_line, computed.target_date);
    plan = db.prepare('SELECT * FROM plan WHERE user_id=?').get(u.id);
    const hasPlanReady = db.prepare("SELECT 1 FROM notifications WHERE user_id=? AND type='plan_ready'").get(u.id);
    if (!hasPlanReady) {
      db.prepare('INSERT INTO notifications (user_id, type, title, body) VALUES (?, ?, ?, ?)')
        .run(u.id, 'plan_ready', 'Your plan is ready!', `Neelu set ${computed.kcal_target} kcal a day for you. Scan your next meal to start.`);
    }
  }
  const fresh = db.prepare('SELECT * FROM users WHERE id=?').get(u.id);
  res.json({ ...fresh, meal_times: JSON.parse(fresh.meal_times), plan });
});

router.get('/home', (req, res) => {
  const u = getUser(req);
  const date = req.query.date || today();
  const plan = db.prepare('SELECT * FROM plan WHERE user_id=?').get(u.id);
  const kcalTarget = plan?.kcal_target || 2000;
  const macroGoals = { carbs_g: plan?.carbs_g || 250, protein_g: plan?.protein_g || 75, fat_g: plan?.fat_g || 60 };

  const meals = db.prepare('SELECT * FROM meals WHERE user_id=? AND date=? ORDER BY created_at').all(u.id, date);
  const bySlot = {};
  for (const slot of SLOT_ORDER) {
    const m = meals.find((r) => r.slot === slot);
    bySlot[slot] = m
      ? { logged: true, id: m.id, name: m.name, kcal: m.kcal, protein_g: m.protein_g, carbs_g: m.carbs_g, fat_g: m.fat_g,
          items: JSON.parse(m.items_json || '[]'), time: m.time, source: m.source }
      : { logged: false, open: true, time: mealTimes(u)[slot] };
  }

  const eaten = meals.reduce((a, m) => ({
    kcal: a.kcal + m.kcal,
    protein_g: a.protein_g + m.protein_g,
    carbs_g: a.carbs_g + m.carbs_g,
    fat_g: a.fat_g + m.fat_g,
  }), { kcal: 0, protein_g: 0, carbs_g: 0, fat_g: 0 });

  const water = db.prepare('SELECT glasses FROM water WHERE user_id=? AND date=?').get(u.id, date)?.glasses || 0;
  const wRow = db.prepare('SELECT kg FROM weights WHERE user_id=? ORDER BY date DESC LIMIT 1').get(u.id);

  const hour = Number((req.query.hour ?? new Date().getHours() + new Date().getMinutes() / 60).toFixed(2));
  const anyMeal = meals.length > 0;
  let state = 'default';
  if (!anyMeal && hour < 12.5) state = 'firstday';
  else if (!bySlot.lunch.logged && hour >= mealTimes(u).lunch && hour < 17) state = 'missed';
  else if (eaten.kcal > kcalTarget * 1.1) state = 'over';
  else if (eaten.kcal >= kcalTarget) state = 'goal';
  else if (hour >= 20.5) state = 'night';

  missedCheck(u, bySlot, date, mealTimes(u));

  const notifications = db.prepare('SELECT COUNT(*) AS n FROM notifications WHERE user_id=? AND read=0').get(u.id).n;

  res.json({
    date, state,
    greeting: hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening',
    kcal_target: kcalTarget, kcal_eaten: eaten.kcal, kcal_left: Math.max(0, kcalTarget - eaten.kcal),
    macros: { eaten: { protein_g: Math.round(eaten.protein_g), carbs_g: Math.round(eaten.carbs_g), fat_g: Math.round(eaten.fat_g) }, goal: macroGoals },
    streak: calcStreak(mealDates(u.id)), coins: u.coins,
    water, water_goal: 8, steps: seededSteps(u.id, date),
    weight_kg: wRow?.kg ?? u.weight_kg,
    meals: bySlot, unread: notifications,
  });
});

function missedCheck(u, bySlot, date, times) {
  const now = new Date();
  if (date !== today()) return;
  const h = now.getHours() + now.getMinutes() / 60;
  for (const slot of SLOT_ORDER) {
    if (slot === 'snack') continue;
    if (bySlot[slot].logged) continue;
    const [hh, mm] = (times[slot] || '').split(':').map(Number);
    if (!hh) continue;
    const late = hh + mm / 60 + 1.5;
    if (h >= late) {
      const key = `missed_${slot}_${date}`;
      const exists = db.prepare("SELECT 1 FROM notifications WHERE user_id=? AND type=? AND body LIKE ?").get(u.id, 'missed_meal', `%${date}%${slot}%`);
      if (!exists) {
        db.prepare('INSERT INTO notifications (user_id, type, title, body) VALUES (?, ?, ?, ?)')
          .run(u.id, 'missed_meal', `${cap(slot)} skipped?`, `Neelu is hungry. Log your ${slot} — ${date} ${slot} not logged. Log it in 10 seconds.`);
      }
    }
  }
}

function cap(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

router.post('/meals', (req, res) => {
  const u = getUser(req);
  const b = req.body || {};
  const slot = SLOT_ORDER.includes(b.slot) ? b.slot : 'snack';
  const date = b.date || today();
  const items = Array.isArray(b.items) ? b.items.slice(0, 20) : [];
  if (!items.length) return res.status(400).json({ error: 'items_required' });

  let t = totals(items.map((it, i) => ({
    kcal: Number(it.kcal) || 0, protein_g: Number(it.protein_g) || 0,
    carbs_g: Number(it.carbs_g) || 0, fat_g: Number(it.fat_g) || 0,
    name: it.name || `Item ${i + 1}`, portion: it.portion || '', confidence: 1, unsure: false, alternatives: [], idx: i + 1,
  })));
  if (b.oil_level) t = applyOil(t, b.oil_level);

  const times = mealTimes(u);
  const info = db.prepare(`INSERT INTO meals (user_id, slot, date, time, name, kcal, protein_g, carbs_g, fat_g, items_json, source, scan_id, oil_level)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`).run(
    u.id, slot, date, b.time || times[slot], b.name || 'Meal',
    t.kcal, t.protein_g, t.carbs_g, t.fat_g,
    JSON.stringify(items), b.source || 'quick', b.scan_id || null, b.oil_level || null
  );
  const coins = coinsAward(b.source);
  db.prepare('UPDATE users SET coins = coins + ? WHERE id=?').run(coins, u.id);

  const challengeResp = updateChallenges(u.id, { slot, date });
  res.json({
    id: info.lastInsertRowid, slot, date, name: b.name || 'Meal', ...t,
    items, coins_awarded: coins, coins: u.coins + coins,
    streak: calcStreak(mealDates(u.id)), ...challengeResp,
  });
});

router.get('/meals/:id', (req, res) => {
  const u = getUser(req);
  const m = db.prepare('SELECT * FROM meals WHERE id=? AND user_id=?').get(req.params.id, u.id);
  if (!m) return res.status(404).json({ error: 'not_found' });
  const scan = m.scan_id ? db.prepare('SELECT id, status, model, raw_csv, image_path FROM scans WHERE id=?').get(m.scan_id) : null;
  res.json({ ...m, items: JSON.parse(m.items_json || '[]'), scan });
});

router.delete('/meals/:id', (req, res) => {
  const u = getUser(req);
  db.prepare('DELETE FROM meals WHERE id=? AND user_id=?').run(req.params.id, u.id);
  res.json({ ok: true });
});

router.get('/foods', (req, res) => {
  const q = (req.query.q || '').trim().toLowerCase();
  let rows;
  if (q) {
    rows = db.prepare('SELECT * FROM foods WHERE lower(name) LIKE ? ORDER BY verified DESC, name LIMIT 20').all(`%${q}%`);
    const recent = db.prepare(`SELECT DISTINCT name FROM meals WHERE user_id=? AND items_json LIKE ? LIMIT 5`).all(getUser(req).id, `%${cap(q)}%`);
    const recentItems = recent.map((r) => r.name);
    rows = [...rows, ...db.prepare('SELECT * FROM foods WHERE name IN (SELECT name FROM foods WHERE lower(name) = ?)').all(q)];
  } else {
    rows = db.prepare('SELECT * FROM foods ORDER BY verified DESC, name LIMIT 20').all();
  }
  res.json({ items: rows });
});

router.post('/foods/estimate', async (req, res) => {
  const b = req.body || {};
  const name = String(b.name || '').trim().slice(0, 80);
  if (!name) return res.status(400).json({ error: 'name_required' });
  try {
    const raw = await gemini.estimateDish(name);
    const parsed = parseScanCsv(raw);
    if (!parsed.ok) return res.status(502).json({ error: 'estimate_unparseable' });
    const it = parsed.items[0];
    db.prepare(`INSERT INTO foods (name, kcal, protein_g, carbs_g, fat_g, portion, veg, verified, source)
      VALUES (?, ?, ?, ?, ?, ?, ?, 0, 'ai')
      ON CONFLICT(name) DO NOTHING`).run(it.name, it.kcal, it.protein_g, it.carbs_g, it.fat_g, it.portion, b.veg || 'veg');
    res.json({ item: it, raw_csv: raw });
  } catch (e) {
    res.status(e.status || 500).json({ error: e.message });
  }
});

router.post('/scan', async (req, res) => {
  const u = getUser(req);
  const b = req.body || {};
  const base64 = String(b.image_base64 || '').replace(/^data:[^;]+;base64,/, '');
  if (!base64) return res.status(400).json({ error: 'image_required' });
  const size = Buffer.byteLength(base64, 'base64');
  if (size > 6 * 1024 * 1024) return res.status(413).json({ error: 'image_too_large' });
  const mime = ['image/jpeg', 'image/png', 'image/webp'].includes(b.mime) ? b.mime : 'image/jpeg';

  try {
    let raw = await gemini.analyzeImage({ base64, mime });
    let parsed = parseScanCsv(raw);
    if (!parsed.ok) {
      raw = await gemini.analyzeImage({ base64, mime });
      parsed = parseScanCsv(raw);
    }
    const status = classify(parsed.ok ? parsed.items : [], parsed.ok);
    const t = parsed.ok ? totals(parsed.items) : null;

    const imageFile = `scan_${Date.now()}_${Math.floor(Math.random() * 1e6)}.jpg`;
    fs.writeFileSync(path.join(uploadsDir, imageFile), Buffer.from(base64, 'base64'));

    const info = db.prepare(`INSERT INTO scans (user_id, status, model, meal_name, raw_csv, items_json, totals_json, image_path)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)`).run(
      u.id, status, gemini.model, parsed.ok ? parsed.meal_name : '',
      raw, JSON.stringify(parsed.ok ? parsed.items : []), t ? JSON.stringify(t) : null, imageFile
    );

    res.json({
      scan_id: info.lastInsertRowid, status,
      meal_name: parsed.ok ? parsed.meal_name : '',
      items: parsed.ok ? parsed.items : [],
      totals: t || { kcal: 0, protein_g: 0, carbs_g: 0, fat_g: 0 },
      raw_csv: raw, model: gemini.model, image_url: `/uploads/${imageFile}`,
    });
  } catch (e) {
    res.status(e.status || 500).json({ error: e.message, status: 'failed' });
  }
});

router.get('/scans/:id', (req, res) => {
  const u = getUser(req);
  const s = db.prepare('SELECT * FROM scans WHERE id=? AND user_id=?').get(req.params.id, u.id);
  if (!s) return res.status(404).json({ error: 'not_found' });
  res.json({ ...s, items: JSON.parse(s.items_json || '[]'), totals: JSON.parse(s.totals_json || '{}') });
});

router.post('/voice-log', async (req, res) => {
  const b = req.body || {};
  const transcript = String(b.transcript || '').trim().slice(0, 500);
  if (!transcript) return res.status(400).json({ error: 'transcript_required' });
  try {
    const raw = await gemini.parseTranscript(transcript);
    const parsed = parseScanCsv(raw);
    if (!parsed.ok) return res.status(502).json({ error: 'unparseable', raw_csv: raw });
    res.json({
      status: classify(parsed.items, true), meal_name: parsed.meal_name,
      items: parsed.items, totals: totals(parsed.items), raw_csv: raw, model: gemini.model,
    });
  } catch (e) {
    res.status(e.status || 500).json({ error: e.message });
  }
});

router.post('/water', (req, res) => {
  const u = getUser(req);
  const date = req.body?.date || today();
  const delta = Math.max(-8, Math.min(8, Number(req.body?.delta) || 0));
  db.prepare(`INSERT INTO water (user_id, date, glasses) VALUES (?, ?, max(0, min(8, ?)))
    ON CONFLICT(user_id, date) DO UPDATE SET glasses = max(0, min(8, glasses + excluded.glasses))`)
    .run(u.id, date, delta);
  const g = db.prepare('SELECT glasses FROM water WHERE user_id=? AND date=?').get(u.id, date)?.glasses || 0;
  updateChallenges(u.id, { water: g });
  res.json({ glasses: g });
});

router.post('/weight', (req, res) => {
  const u = getUser(req);
  const kg = Number(req.body?.kg);
  if (!kg || kg < 25 || kg > 300) return res.status(400).json({ error: 'invalid_weight' });
  const date = today();
  const last = db.prepare('SELECT kg FROM weights WHERE user_id=? ORDER BY date DESC, id DESC LIMIT 1').get(u.id);
  db.prepare('INSERT INTO weights (user_id, date, kg) VALUES (?, ?, ?)').run(u.id, date, kg);
  db.prepare('UPDATE users SET weight_kg=? WHERE id=?').run(kg, u.id);
  res.json({ kg, delta: last ? Math.round((kg - last.kg) * 10) / 10 : 0 });
});

router.get('/insights', (req, res) => {
  const u = getUser(req);
  const plan = db.prepare('SELECT * FROM plan WHERE user_id=?').get(u.id);
  const kcalTarget = plan?.kcal_target || 2000;
  const days = [];
  for (let i = 6; i >= 0; i--) {
    const date = today(-i);
    const kcal = db.prepare('SELECT COALESCE(SUM(kcal),0) AS k FROM meals WHERE user_id=? AND date=?').get(u.id, date).k;
    days.push({ date, kcal, on_track: kcal > 0 && kcal <= kcalTarget * 1.05, weekday: new Date(date).toLocaleDateString('en', { weekday: 'short' }) });
  }
  const logged = days.filter((d) => d.kcal > 0);
  const avg = logged.length ? Math.round(logged.reduce((a, d) => a + d.kcal, 0) / logged.length) : 0;
  const weights = db.prepare('SELECT date, kg FROM weights WHERE user_id=? ORDER BY date DESC LIMIT 8').all(u.id).reverse();
  const proteinToday = db.prepare(`SELECT COALESCE(SUM(protein_g),0) AS p FROM meals WHERE user_id=? AND date=?`).get(u.id, today()).p;
  res.json({
    kcal_target: kcalTarget, days, avg, logged_days: logged.length,
    on_track_days: days.filter((d) => d.on_track).length,
    weights, weight_trend: weights.length >= 2 ? Math.round((weights[weights.length - 1].kg - weights[0].kg) * 10) / 10 : 0,
    protein_today: Math.round(proteinToday), protein_goal: plan?.protein_g || 75,
  });
});

router.get('/streak', (req, res) => {
  const u = getUser(req);
  const dates = mealDates(u.id);
  const set = new Set(dates);
  const year = Number(req.query.year) || new Date().getFullYear();
  const month = Number(req.query.month) || new Date().getMonth() + 1;
  const first = new Date(year, month - 1, 1);
  const days = [];
  for (let d = 1; d <= new Date(year, month, 0).getDate(); d++) {
    const date = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    const isFuture = date > today();
    days.push({ date, lit: set.has(date), future: isFuture, today: date === today() });
  }
  res.json({
    streak: calcStreak(dates), best: bestStreak(dates), freezes: 2, days,
    first_weekday: first.getDay() === 0 ? 6 : first.getDay() - 1,
  });
});

const CHALLENGES = [
  { key: 'first_bite', scope: 'today', title: 'First bite', target: 1, coins: 10 },
  { key: 'three_meals', scope: 'today', title: 'Three meals', target: 3, coins: 15 },
  { key: 'protein_60', scope: 'today', title: 'Protein 60', target: 60, coins: 20 },
  { key: 'hydrate', scope: 'today', title: 'Hydrate', target: 8, coins: 10 },
  { key: 'early_dinner', scope: 'week', title: 'Early dinner', target: 4, coins: 100 },
  { key: 'home_cooked', scope: 'week', title: 'Home-cooked', target: 10, coins: 150 },
  { key: 'full_week', scope: 'week', title: 'Full week', target: 7, coins: 80 },
];

function challengeProgress(userId) {
  const t = today();
  const weekStart = today(-6);
  const todayMeals = db.prepare('SELECT * FROM meals WHERE user_id=? AND date=?').all(userId, t);
  const weekMeals = db.prepare('SELECT * FROM meals WHERE user_id=? AND date>=?').all(userId, weekStart);
  const water = db.prepare('SELECT glasses FROM water WHERE user_id=? AND date=?').get(userId, t)?.glasses || 0;
  const protein = todayMeals.reduce((a, m) => a + m.protein_g, 0);
  const sweetRe = /halwa|jamun|jalebi|gulab|kheer|laddu|dessert|mithai/i;
  const sweetFreeDays = new Set(weekMeals.filter((m) => !sweetRe.test(m.items_json)).map((m) => m.date)).size;
  const loggedDays = new Set(weekMeals.map((m) => m.date)).size;
  return {
    first_bite: todayMeals.length >= 1 ? 1 : 0,
    three_meals: new Set(todayMeals.map((m) => m.slot)).size,
    protein_60: Math.round(protein),
    hydrate: water,
    early_dinner: weekMeals.filter((m) => m.slot === 'dinner' && (m.time || '') <= '20:00').length,
    home_cooked: weekMeals.filter((m) => m.source === 'scan').length,
    full_week: loggedDays,
    less_sugar: sweetFreeDays,
  };
}

function updateChallenges(userId, evt = {}) {
  const progress = challengeProgress(userId);
  const done = CHALLENGES.filter((c) => progress[c.key] >= c.target);
  const coinsFromChallenges = done.length;
  return { challenges_done: coinsFromChallenges, progress };
}

router.get('/challenges', (req, res) => {
  const u = getUser(req);
  const progress = challengeProgress(u.id);
  const badges = db.prepare('SELECT key FROM badges WHERE user_id=?').all(u.id).map((r) => r.key);
  res.json({
    coins: u.coins,
    challenges: CHALLENGES.map((c) => ({ ...c, progress: progress[c.key], done: progress[c.key] >= c.target, claimed: badges.includes(`chal_${c.key}`) })),
    badges_unlocked: badges.filter((b) => !b.startsWith('chal_')).length,
  });
});

router.post('/challenges/claim', (req, res) => {
  const u = getUser(req);
  const key = String(req.body?.key || '');
  const c = CHALLENGES.find((x) => x.key === key);
  if (!c) return res.status(400).json({ error: 'unknown_challenge' });
  const progress = challengeProgress(u.id);
  if (progress[key] < c.target) return res.status(400).json({ error: 'not_done' });
  const badgeKey = `chal_${key}`;
  const already = db.prepare('SELECT 1 FROM badges WHERE user_id=? AND key=?').get(u.id, badgeKey);
  if (already) return res.json({ ok: true, coins: u.coins, already: true });
  db.prepare('INSERT INTO badges (user_id, key) VALUES (?, ?)').run(u.id, badgeKey);
  db.prepare('UPDATE users SET coins = coins + ? WHERE id=?').run(c.coins, u.id);
  db.prepare('INSERT INTO notifications (user_id, type, title, body) VALUES (?, ?, ?, ?)')
    .run(u.id, 'badge', `${c.title} done!`, `Challenge complete — +${c.coins} coins.`);
  const nu = db.prepare('SELECT coins FROM users WHERE id=?').get(u.id);
  res.json({ ok: true, coins: nu.coins, awarded: c.coins });
});

router.post('/badges/award', (req, res) => {
  const u = getUser(req);
  const key = String(req.body?.key || '').slice(0, 40);
  if (!key) return res.status(400).json({ error: 'key_required' });
  db.prepare('INSERT OR IGNORE INTO badges (user_id, key) VALUES (?, ?)').run(u.id, key);
  db.prepare('UPDATE users SET coins = coins + 50 WHERE id=?').run(u.id);
  const nu = db.prepare('SELECT coins FROM users WHERE id=?').get(u.id);
  res.json({ ok: true, coins: nu.coins, awarded: 50 });
});

router.post('/reset', (req, res) => {
  const u = getUser(req);
  for (const t of ['meals', 'scans', 'weights', 'notifications', 'badges']) db.prepare(`DELETE FROM ${t} WHERE user_id=?`).run(u.id);
  db.prepare('DELETE FROM water WHERE user_id=?').run(u.id);
  db.prepare(`UPDATE users SET coins=0, onboarded=0, goal=NULL, diet=NULL, sex=NULL, age=NULL, height_cm=NULL, weight_kg=NULL, target_weight_kg=NULL, activity=NULL WHERE id=?`).run(u.id);
  db.prepare('DELETE FROM plan WHERE user_id=?').run(u.id);
  res.json({ ok: true });
});

router.get('/notifications', (req, res) => {
  const u = getUser(req);
  const rows = db.prepare('SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 50').all(u.id);
  res.json({ items: rows.map((r) => ({ ...r, read: !!r.read, today: r.created_at.slice(0, 10) === today() })) });
});

router.post('/notifications/read', (req, res) => {
  const u = getUser(req);
  if (req.body?.id) db.prepare('UPDATE notifications SET read=1 WHERE id=? AND user_id=?').run(req.body.id, u.id);
  else db.prepare('UPDATE notifications SET read=1 WHERE user_id=?').run(u.id);
  res.json({ ok: true });
});
