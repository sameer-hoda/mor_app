import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { searchVariants } from '../src/routes.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, '..');
const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'mor-foods-test-'));
const PORT = 4112;
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

const names = (items) => items.map((f) => f.name);

test('searchVariants singularizes plurals', () => {
  assert.deepEqual(searchVariants('eggs'), ['eggs', 'egg']);
  assert.deepEqual(searchVariants('rotis'), ['rotis', 'roti']);
  assert.deepEqual(searchVariants('dosa'), ['dosa']);
  assert.deepEqual(searchVariants('boiled eggs'), ['boiled eggs', 'boiled egg']);
});

test('"eggs" returns egg dishes (was: no match)', async () => {
  const j = await fetch(`${B}/foods?q=eggs`).then((r) => r.json());
  assert.ok(j.items.length > 0, 'expected matches for "eggs"');
  assert.equal(j.items[0].name, 'Boiled egg');
});

test('"egg" matches curated egg rows', async () => {
  const j = await fetch(`${B}/foods?q=egg`).then((r) => r.json());
  const n = names(j.items);
  assert.ok(n.includes('Boiled egg'));
  assert.ok(n.includes('Egg curry'));
  assert.ok(n.includes('Fried egg'));
});

test('"omelet" ranks the curated Omelette first', async () => {
  const j = await fetch(`${B}/foods?q=omelet`).then((r) => r.json());
  assert.equal(j.items[0].name, 'Omelette');
});

test('"sunny side up" resolves to Fried egg', async () => {
  const j = await fetch(`${B}/foods?q=${encodeURIComponent('sunny side up')}`).then((r) => r.json());
  assert.ok(names(j.items).includes('Fried egg'));
});

test('"channa" resolves to Chole (was: no match)', async () => {
  const j = await fetch(`${B}/foods?q=channa`).then((r) => r.json());
  assert.ok(names(j.items).includes('Chole'));
});

test('"paneer butter masala" has a dedicated row', async () => {
  const j = await fetch(`${B}/foods?q=${encodeURIComponent('paneer butter masala')}`).then((r) => r.json());
  assert.equal(j.items[0].name, 'Paneer butter masala');
});

test('USDA reference catalog is seeded', async () => {
  const j = await fetch(`${B}/foods?q=tofu`).then((r) => r.json());
  assert.ok(j.items.length > 0, 'expected USDA tofu rows');
  assert.ok(j.items.some((f) => f.source === 'USDA SR Legacy'));
});

test('genuine no-match still returns empty', async () => {
  const j = await fetch(`${B}/foods?q=zzzqqq`).then((r) => r.json());
  assert.equal(j.items.length, 0);
});
