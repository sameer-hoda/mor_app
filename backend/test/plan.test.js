import { test } from 'node:test';
import assert from 'node:assert/strict';
import { bmr, tdee, computePlan, targetDate, isAmbitious, milestone } from '../src/plan.js';

const base = { sex: 'female', age: 28, height_cm: 170, weight_kg: 72, target_weight_kg: 66, goal: 'lose', activity: 'light' };

test('bmr mifflin-st jeor', () => {
  assert.equal(Math.round(bmr(base)), 1482);
  assert.equal(Math.round(bmr({ ...base, sex: 'male' })), 1648);
  assert.equal(Math.round(bmr({ ...base, sex: 'other' })), 1565);
});

test('tdee applies activity multiplier', () => {
  assert.equal(Math.round(tdee(base)), 2037);
  assert.equal(Math.round(tdee({ ...base, activity: 'sit' })), 1778);
});

test('plan: lose goal subtracts deficit and derives macros', () => {
  const p = computePlan({ ...base, sex: 'male' });
  assert.equal(p.kcal_target, 1770);
  assert.equal(p.protein_g, 76);
  assert.equal(p.fat_g, 59);
  assert.ok(p.carbs_g >= 100);
  assert.ok(!p.ambitious);
  assert.equal(p.milestone, 66);
});

test('plan: realistic target date ~12 weeks out for 6 kg', () => {
  const p = computePlan(base);
  const weeks = (new Date(p.target_date) - Date.now()) / (7 * 86400000);
  assert.ok(weeks >= 11 && weeks <= 14, `weeks=${weeks}`);
});

test('ambitious detection and milestone', () => {
  const ambitious = { ...base, target_weight_kg: 55 };
  assert.ok(isAmbitious(ambitious.weight_kg, ambitious.target_weight_kg));
  const p = computePlan(ambitious);
  assert.ok(p.ambitious);
  assert.equal(p.milestone, 64.8);
  assert.ok(!isAmbitious(72, 66));
});

test('gain goal adds surplus', () => {
  const p = computePlan({ ...base, sex: 'male', goal: 'gain', target_weight_kg: 78 });
  assert.ok(p.kcal_target > 1500);
});

test('target date math', () => {
  assert.ok(new Date(targetDate(72, 66)) > new Date());
});
