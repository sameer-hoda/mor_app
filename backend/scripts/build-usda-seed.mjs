// Builds backend/data/usda-seed.json from USDA FoodData Central SR Legacy CSVs.
// Source: U.S. Department of Agriculture, Agricultural Research Service,
// FoodData Central, SR Legacy (April 2018) — public domain (CC0).
// https://fdc.nal.usda.gov/download-datasets
// Usage: node scripts/build-usda-seed.mjs <csv-dir> [out-json]
// Values are scaled to the first household portion when a gram weight exists,
// otherwise per 100 g. Excludes branded / restaurant / fast-food / baby-food
// categories that pollute search.
import fs from 'node:fs';
import path from 'node:path';

const dir = process.argv[2] || '/tmp/sr_legacy/FoodData_Central_sr_legacy_food_csv_2018-04';
const out = process.argv[3] || new URL('../seed/usda-seed.json', import.meta.url).pathname;

function parseCsv(file) {
  const text = fs.readFileSync(path.join(dir, file), 'utf8');
  const rows = [];
  let cur = [''];
  let inQ = false;
  const pushRow = () => { rows.push(cur); cur = ['']; };
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (inQ) {
      if (c === '"') {
        if (text[i + 1] === '"') { cur[cur.length - 1] += '"'; i++; }
        else inQ = false;
      } else cur[cur.length - 1] += c;
    } else if (c === '"') inQ = true;
    else if (c === ',') cur.push('');
    else if (c === '\n') pushRow();
    else if (c === '\r') { /* skip */ }
    else cur[cur.length - 1] += c;
  }
  if (cur.length > 1 || cur[0] !== '') pushRow();
  const head = rows.shift();
  return rows.filter((r) => r.length === head.length).map((r) => Object.fromEntries(head.map((h, i) => [h, r[i]])));
}

const EXCLUDE_CATS = new Set(['3', '21', '24', '25', '26', '27']); // baby, fast food, native, restaurant, branded, QC
const VEG = new Set(['2', '8', '9', '11', '12', '16', '20']); // spices, cereals, fruit, veg, nuts, legumes, grains
const NON = new Set(['5', '7', '10', '13', '15', '17']); // poultry, sausage, pork, beef, fish, lamb/game

const foods = parseCsv('food.csv').filter((f) => f.data_type === 'sr_legacy_food' && !EXCLUDE_CATS.has(f.food_category_id));
const nuts = parseCsv('food_nutrient.csv');
const portions = parseCsv('food_portion.csv');
const units = Object.fromEntries(parseCsv('measure_unit.csv').map((u) => [u.id, u.name]));

const MACRO = { '1008': 'kcal', '1003': 'protein_g', '1004': 'fat_g', '1005': 'carbs_g' };
const byFood = new Map();
for (const n of nuts) {
  const k = MACRO[n.nutrient_id];
  if (!k || n.fdc_id === '') continue;
  if (!byFood.has(n.fdc_id)) byFood.set(n.fdc_id, {});
  const m = byFood.get(n.fdc_id);
  if (m[k] === undefined) m[k] = parseFloat(n.amount);
}
const firstPortion = new Map();
for (const p of portions) {
  if (!firstPortion.has(p.fdc_id) && p.seq_num === '1') firstPortion.set(p.fdc_id, p);
}

const r1 = (v) => Math.round(v * 10) / 10;
const seed = [];
for (const f of foods) {
  const m = byFood.get(f.fdc_id);
  if (!m || m.kcal === undefined || m.kcal <= 0) continue;
  const name = f.description.trim();
  if (!name) continue;
  let kcal = m.kcal, protein = m.protein_g ?? 0, fat = m.fat_g ?? 0, carbs = m.carbs_g ?? 0;
  let portion = '100 g';
  const p = firstPortion.get(f.fdc_id);
  const gw = p ? parseFloat(p.gram_weight) : NaN;
  if (p && gw > 0) {
    const f2 = gw / 100;
    kcal = m.kcal * f2; protein *= f2; fat *= f2; carbs *= f2;
    const amt = parseFloat(p.amount);
    let unit = units[p.measure_unit_id] || '';
    if (unit.toLowerCase() === 'undetermined') unit = '';
    const mod = (p.modifier || '').trim();
    portion = `${Number.isInteger(amt) ? amt : r1(amt)}${unit ? ` ${unit}` : ''}${mod ? ` ${mod}` : ''}`.trim().slice(0, 40) || `${r1(gw)} g`;
  }
  const cat = f.food_category_id;
  const veg = VEG.has(cat) ? 'veg' : NON.has(cat) ? 'non' : '';
  seed.push({
    name,
    kcal: Math.round(kcal),
    protein_g: r1(protein), carbs_g: r1(carbs), fat_g: r1(fat),
    portion, veg, verified: 1, source: 'USDA SR Legacy',
  });
}
seed.sort((a, b) => a.name.localeCompare(b.name));
fs.writeFileSync(out, JSON.stringify(seed, null, 0));
console.log(`wrote ${seed.length} rows → ${out} (${Math.round(fs.statSync(out).size / 1024)} KB)`);
