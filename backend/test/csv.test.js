import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseScanCsv, classify, totals, applyOil } from '../src/scanCsv.js';

const GOOD = `meal,Thali lunch
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
Dal tadka,150,6,15,6,1 bowl,0.90,0,
Aloo gobhi,120,3,14,6,1 bowl,0.88,0,
Phulka roti,120,4,20,3,2 pieces,0.90,0,
`;

test('parses happy path', () => {
  const r = parseScanCsv(GOOD);
  assert.equal(r.ok, true);
  assert.equal(r.meal_name, 'Thali lunch');
  assert.equal(r.items.length, 3);
  assert.equal(r.items[0].name, 'Dal tadka');
  assert.equal(r.items[0].kcal, 150);
  assert.equal(r.items[2].portion, '2 pieces');
  assert.equal(classify(r.items, true), 'detected');
});

test('strips code fences', () => {
  const r = parseScanCsv('```csv\n' + GOOD + '```');
  assert.equal(r.ok, true);
  assert.equal(r.items.length, 3);
});

test('low confidence becomes unsure and requires alternatives', () => {
  const raw = `meal,Thali lunch
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
Rajma / chole,150,7,16,5,1 bowl,0.45,1,Rajma|Chole|Chana masala
Dal tadka,150,6,15,6,1 bowl,0.90,0,
`;
  const r = parseScanCsv(raw);
  assert.equal(r.ok, true);
  assert.equal(r.items[0].unsure, true);
  assert.deepEqual(r.items[0].alternatives, ['Rajma', 'Chole', 'Chana masala']);
  assert.equal(classify(r.items, true), 'unsure');
});

test('dropped rows: bad kcal, empty name, unsure without alternatives', () => {
  const raw = `meal,Broken
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
,150,6,15,6,1 bowl,0.9,0,
Mystery thing,abc,6,15,6,1 bowl,0.9,0,
Ambiguous,150,6,15,6,1 bowl,0.4,1,
Good item,100,2,10,2,1 cup,0.9,0,
`;
  const r = parseScanCsv(raw);
  assert.equal(r.ok, true);
  assert.equal(r.items.length, 1);
  assert.equal(r.items[0].name, 'Good item');
  assert.equal(r.dropped.length, 3);
});

test('header-only means failed (no food)', () => {
  const raw = `meal,
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
`;
  const r = parseScanCsv(raw);
  assert.equal(r.ok, false);
  assert.equal(classify([], false), 'failed');
});

test('missing header row fails', () => {
  assert.equal(parseScanCsv('just some text,150\nmore text').ok, false);
  assert.equal(parseScanCsv('').ok, false);
  assert.equal(parseScanCsv(null).ok, false);
});

test('meta row alone still fails (no items)', () => {
  const raw = `meal,Only name
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
`;
  assert.equal(parseScanCsv(raw).ok, false);
});

test('clamps insane values and limits rows', () => {
  let raw = `meal,Huge
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
`;
  for (let i = 1; i <= 15; i++) raw += `Item ${i},${9000},400,400,400,1 plate,0.9,0,\n`;
  const r = parseScanCsv(raw);
  assert.equal(r.items.length, 12);
  assert.equal(r.items[0].kcal, 2000);
});

test('totals sums', () => {
  const t = totals([{ kcal: 150, protein_g: 6, carbs_g: 15, fat_g: 6 }, { kcal: 120, protein_g: 3, carbs_g: 14, fat_g: 6 }]);
  assert.deepEqual(t, { kcal: 270, protein_g: 9, carbs_g: 29, fat_g: 12 });
});

test('oil multipliers', () => {
  const base = { kcal: 590, protein_g: 31, carbs_g: 79, fat_g: 26 };
  assert.equal(applyOil(base, 'less').kcal, 502);
  assert.equal(applyOil(base, 'more').kcal, 679);
  assert.equal(applyOil(base, 'more').fat_g, 33);
  assert.equal(applyOil(base, 'normal').kcal, 590);
});
