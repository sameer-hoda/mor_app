import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseScanCsv, classify } from '../src/scanCsv.js';
import * as gemini from '../src/gemini.js';

const FIXTURE = process.env.E2E_FIXTURE_IMAGE || new URL('../fixtures/test-food.jpg', import.meta.url).pathname;

test('LIVE: real Gemini scan of fixture image', { skip: process.env.GEMINI_API_KEY ? false : 'no key' }, async () => {
  const fs = await import('node:fs');
  if (!fs.existsSync(FIXTURE)) return assert.ok(true, 'fixture missing, skipping');
  const base64 = fs.readFileSync(FIXTURE).toString('base64');
  const raw = await gemini.analyzeImage({ base64, mime: 'image/png' });
  const parsed = parseScanCsv(raw);
  assert.equal(parsed.ok, true, `expected parseable CSV, got: ${raw.slice(0, 200)}`);
  assert.ok(parsed.items.length >= 1);
  assert.ok(parsed.items.every((i) => i.kcal > 0 && i.kcal <= 2000));
  const status = classify(parsed.items, true);
  assert.ok(['detected', 'unsure'].includes(status));
});
