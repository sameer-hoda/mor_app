import Database from 'better-sqlite3';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DATA_DIR = process.env.MOR_DATA_DIR || path.join(__dirname, '..', 'data');
fs.mkdirSync(DATA_DIR, { recursive: true });

export const db = new Database(path.join(DATA_DIR, 'mor.db'));
db.pragma('journal_mode = WAL');

db.exec(`
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY,
  onboarded INTEGER DEFAULT 0,
  name TEXT DEFAULT 'Guest',
  goal TEXT, diet TEXT, sex TEXT, age INTEGER,
  height_cm REAL, weight_kg REAL, target_weight_kg REAL, activity TEXT,
  units TEXT DEFAULT 'kg-ft',
  reminders_on INTEGER DEFAULT 1,
  meal_times TEXT DEFAULT '{"breakfast":"08:30","lunch":"13:30","snack":"17:00","dinner":"21:00"}',
  reminders_nudges TEXT DEFAULT '{"missed":true,"streak_saver":true,"water":true,"weekly":true}',
  coins INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS plan (
  user_id INTEGER PRIMARY KEY,
  kcal_target INTEGER, protein_g INTEGER, carbs_g INTEGER, fat_g INTEGER,
  goal_line TEXT, target_date TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS meals (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER, slot TEXT, date TEXT, time TEXT,
  name TEXT, kcal INTEGER, protein_g REAL, carbs_g REAL, fat_g REAL,
  items_json TEXT, source TEXT, scan_id INTEGER, oil_level TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS scans (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER, status TEXT, model TEXT, meal_name TEXT,
  raw_csv TEXT, items_json TEXT, totals_json TEXT, image_path TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS foods (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE, kcal INTEGER, protein_g REAL, carbs_g REAL, fat_g REAL,
  portion TEXT, veg TEXT, verified INTEGER DEFAULT 1, source TEXT DEFAULT 'db',
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS weights (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER, date TEXT, kg REAL,
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS water (
  user_id INTEGER, date TEXT, glasses INTEGER DEFAULT 0,
  PRIMARY KEY (user_id, date)
);
CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER, type TEXT, title TEXT, body TEXT,
  read INTEGER DEFAULT 0, created_at TEXT DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS badges (
  user_id INTEGER, key TEXT, awarded_at TEXT DEFAULT (datetime('now')),
  PRIMARY KEY (user_id, key)
);
`);

db.prepare(`INSERT OR IGNORE INTO users (id) VALUES (1)`).run();

const FOODS = [
  ['Dal tadka', 150, 6, 15, 6, '1 bowl', 'veg'],
  ['Aloo gobhi', 120, 3, 14, 6, '1 bowl', 'veg'],
  ['Jeera rice', 170, 4, 36, 2, '¾ plate', 'veg'],
  ['Phulka roti', 60, 3, 11, 1.5, '1 piece', 'veg'],
  ['Raita + salad', 30, 2, 4, 1, '½ bowl', 'veg'],
  ['Samosa', 130, 2, 13, 7, '1 piece', 'veg'],
  ['Masala chai', 80, 3, 9, 3, '1 cup', 'veg'],
  ['Paneer tikka', 260, 14, 10, 18, '6 pieces', 'veg'],
  ['Poha', 210, 4, 32, 6, '1 plate', 'veg'],
  ['Idli', 60, 2, 12, 0.4, '1 piece', 'veg'],
  ['Dosa', 170, 4, 26, 5, '1 piece', 'veg'],
  ['Rajma', 190, 10, 28, 4, '1 bowl', 'veg'],
  ['Chole', 200, 10, 30, 5, '1 bowl', 'veg'],
  ['Veg biryani', 400, 9, 55, 15, '1 plate', 'veg'],
  ['Aloo paratha', 320, 7, 38, 15, '1 piece', 'veg'],
  ['Curd', 60, 4, 4, 3, '1 katori', 'veg'],
  ['Sprouts', 90, 7, 14, 1, '1 bowl', 'veg'],
  ['Makhana', 110, 3, 16, 4, '1 bowl', 'veg'],
  ['Bhindi masala', 130, 3, 10, 8, '1 bowl', 'veg'],
  ['Upma', 230, 5, 36, 7, '1 plate', 'veg'],
  ['Khichdi', 250, 9, 40, 5, '1 bowl', 'veg'],
  ['Pav bhaji', 450, 10, 60, 18, '1 plate', 'veg'],
  ['Chole bhature', 620, 16, 70, 30, '1 plate', 'veg'],
  ['Gulab jamun', 150, 2, 22, 6, '1 piece', 'veg'],
  ['Gajar halwa', 310, 4, 38, 16, '1 bowl', 'veg'],
  ['Plain rice', 130, 3, 28, 0.4, '1 katori', 'veg'],
  ['Lassi', 220, 6, 36, 5, '1 glass', 'veg'],
  ['Filter coffee', 90, 3, 9, 4, '1 cup', 'veg'],
  ['Fruit chaat', 120, 1, 28, 1, '1 bowl', 'veg'],
  ['Green salad', 50, 2, 8, 1, '1 bowl', 'veg'],
  ['Chicken curry', 320, 25, 8, 21, '1 bowl', 'non'],
  ['Butter chicken', 490, 30, 9, 38, '1 bowl', 'non'],
  ['Tandoori chicken', 520, 45, 4, 34, '4 pieces', 'non'],
  ['Fish curry', 310, 26, 6, 20, '1 bowl', 'non'],
  ['Egg curry', 280, 15, 6, 21, '1 bowl', 'egg'],
  ['Omelette', 200, 13, 2, 15, '2 eggs', 'egg'],
  ['Boiled egg', 78, 6, 0.6, 5.3, '1 egg', 'egg'],
];

const insertFood = db.prepare(
  'INSERT OR IGNORE INTO foods (name, kcal, protein_g, carbs_g, fat_g, portion, veg, verified) VALUES (?, ?, ?, ?, ?, ?, ?, 1)'
);
for (const f of FOODS) insertFood.run(...f);

export function today(offsetDays = 0) {
  const d = new Date(Date.now() + offsetDays * 86400000);
  return d.toISOString().slice(0, 10);
}
