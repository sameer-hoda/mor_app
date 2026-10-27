export const CSV_HEADER = ['name', 'kcal', 'protein_g', 'carbs_g', 'fat_g', 'portion', 'confidence', 'unsure', 'alternatives'];

const UNSURE_THRESHOLD = 0.6;
const MAX_ITEMS = 12;
const MAX_KCAL = 2000;

function splitCsvLine(line) {
  const out = [];
  let cur = '';
  let inQ = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (inQ) {
      if (c === '"' && line[i + 1] === '"') { cur += '"'; i++; }
      else if (c === '"') inQ = false;
      else cur += c;
    } else if (c === '"') inQ = true;
    else if (c === ',') { out.push(cur); cur = ''; }
    else cur += c;
  }
  out.push(cur);
  return out.map((s) => s.trim());
}

function toNum(v, { min = 0, max = 10000, allowEmpty = true } = {}) {
  if (v === undefined || v === null || String(v).trim() === '') {
    if (allowEmpty) return 0;
    return null;
  }
  const n = Number(String(v).replace(/[^0-9.\-]/g, ''));
  if (!Number.isFinite(n)) return allowEmpty ? 0 : null;
  return Math.min(max, Math.max(min, n));
}

function titleCase(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function parseScanCsv(raw) {
  if (typeof raw !== 'string') return { ok: false, reason: 'not_text' };
  let text = raw.trim()
    .replace(/^```(?:csv)?/i, '')
    .replace(/```$/, '')
    .trim();
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  if (lines.length === 0) return { ok: false, reason: 'empty' };

  let mealName = '';
  let start = 0;
  const first = splitCsvLine(lines[0]);
  if (first[0].toLowerCase() === 'meal') {
    mealName = first.slice(1).join(',').trim().slice(0, 40);
    start = 1;
  }
  const header = splitCsvLine(lines[start] || '').map((c) => c.toLowerCase().replace(/\s/g, '_'));
  const isHeader = header[0] === 'name';
  if (!isHeader) return { ok: false, reason: 'missing_meta_or_header' };
  for (let i = 0; i < CSV_HEADER.length; i++) {
    if (header[i] !== CSV_HEADER[i]) return { ok: false, reason: 'bad_header' };
  }

  const items = [];
  const dropped = [];
  for (const line of lines.slice(start + 1)) {
    if (items.length >= MAX_ITEMS) break;
    if (line.toLowerCase().startsWith('meal,')) continue;
    const cols = splitCsvLine(line);
    if (cols[0].toLowerCase() === 'name') continue;
    const name = (cols[0] || '').replace(/^["']|["']$/g, '').trim().slice(0, 60);
    if (!name) { dropped.push({ line, reason: 'no_name' }); continue; }
    const kcal = toNum(cols[1], { max: MAX_KCAL, allowEmpty: false });
    if (kcal === null || kcal <= 0) { dropped.push({ line, reason: 'bad_kcal' }); continue; }
    const confidence = toNum(cols[6], { min: 0, max: 1 });
    const unsureFlag = String(cols[7] || '').trim();
    const alts = String(cols[8] || '').split('|').map((a) => a.trim()).filter(Boolean).slice(0, 3);
    const unsure = confidence < UNSURE_THRESHOLD || unsureFlag === '1';
    if (unsure && alts.length === 0) { dropped.push({ line, reason: 'unsure_no_alternatives' }); continue; }
    items.push({
      idx: items.length + 1,
      name: titleCase(name),
      kcal: Math.round(kcal),
      protein_g: toNum(cols[2], { max: 500 }),
      carbs_g: toNum(cols[3], { max: 500 }),
      fat_g: toNum(cols[4], { max: 500 }),
      portion: (cols[5] || '1 serving').slice(0, 40) || '1 serving',
      confidence: Math.round(confidence * 100) / 100,
      unsure,
      alternatives: alts,
    });
  }

  if (items.length === 0) return { ok: false, reason: 'no_items', dropped };
  return { ok: true, meal_name: mealName, items, dropped };
}

export function classify(items, ok) {
  if (!ok || items.length === 0) return 'failed';
  return items.some((it) => it.unsure) ? 'unsure' : 'detected';
}

export function totals(items) {
  const t = { kcal: 0, protein_g: 0, carbs_g: 0, fat_g: 0 };
  for (const it of items) {
    t.kcal += it.kcal;
    t.protein_g += it.protein_g;
    t.carbs_g += it.carbs_g;
    t.fat_g += it.fat_g;
  }
  return {
    kcal: Math.round(t.kcal),
    protein_g: Math.round(t.protein_g),
    carbs_g: Math.round(t.carbs_g),
    fat_g: Math.round(t.fat_g),
  };
}

export const OIL_MULT = { less: 0.85, normal: 1, more: 1.15 };

export function applyOil(tot, oilLevel) {
  const m = OIL_MULT[oilLevel] ?? 1;
  return {
    kcal: Math.round(tot.kcal * m),
    protein_g: tot.protein_g,
    carbs_g: tot.carbs_g,
    fat_g: Math.round(tot.fat_g * (oilLevel === 'more' ? 1.25 : oilLevel === 'less' ? 0.75 : 1)),
  };
}
