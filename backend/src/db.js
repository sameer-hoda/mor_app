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
  ['Fried egg', 90, 6, 0.6, 7, '1 egg', 'egg'],
  ['Scrambled eggs', 180, 12, 2, 14, '2 eggs', 'egg'],
  ['Masala omelette', 230, 14, 4, 17, '2 eggs', 'egg'],
  ['Egg bhurji', 180, 12, 4, 13, '1 bowl', 'egg'],
  // Curated home-style Indian dishes (verified, source 'curated').
  ['Masala dosa', 250, 6, 40, 7, '1 piece', 'veg'],
  ['Rava dosa', 300, 7, 44, 10, '1 piece', 'veg'],
  ['Onion dosa', 220, 5, 36, 6, '1 piece', 'veg'],
  ['Uthappam', 220, 6, 34, 7, '1 piece', 'veg'],
  ['Medu vada', 150, 4, 15, 8, '2 pieces', 'veg'],
  ['Sambar', 90, 4, 12, 3, '1 bowl', 'veg'],
  ['Rasam', 40, 2, 6, 1, '1 bowl', 'veg'],
  ['Coconut chutney', 120, 2, 4, 12, '2 tbsp', 'veg'],
  ['Tomato chutney', 40, 1, 7, 1, '2 tbsp', 'veg'],
  ['Pongal', 280, 8, 44, 8, '1 plate', 'veg'],
  ['Lemon rice', 250, 5, 46, 6, '1 plate', 'veg'],
  ['Curd rice', 220, 7, 36, 5, '1 plate', 'veg'],
  ['Tamarind rice', 260, 5, 48, 6, '1 plate', 'veg'],
  ['Pesarattu', 200, 8, 28, 7, '1 piece', 'veg'],
  ['Appam', 90, 2, 18, 1, '1 piece', 'veg'],
  ['Puttu', 200, 4, 42, 2, '1 plate', 'veg'],
  ['Kadala curry', 180, 8, 24, 6, '1 bowl', 'veg'],
  ['Potato masala', 140, 3, 22, 5, '1 bowl', 'veg'],
  ['Chapati', 80, 3, 15, 1, '1 piece', 'veg'],
  ['Tandoori roti', 90, 3, 17, 1, '1 piece', 'veg'],
  ['Naan', 260, 7, 45, 6, '1 piece', 'veg'],
  ['Butter naan', 310, 7, 46, 10, '1 piece', 'veg'],
  ['Missi roti', 150, 6, 22, 6, '1 piece', 'veg'],
  ['Bajra roti', 110, 4, 20, 2, '1 piece', 'veg'],
  ['Jowar roti', 110, 4, 22, 1, '1 piece', 'veg'],
  ['Kulcha', 180, 5, 34, 3, '1 piece', 'veg'],
  ['Bhatura', 240, 5, 32, 10, '1 piece', 'veg'],
  ['Puri', 120, 2, 14, 6, '2 pieces', 'veg'],
  ['Thepla', 140, 4, 20, 6, '1 piece', 'veg'],
  ['Parotta', 290, 6, 40, 12, '1 piece', 'veg'],
  ['Laccha paratha', 280, 6, 38, 13, '1 piece', 'veg'],
  ['Dal makhani', 300, 10, 20, 20, '1 bowl', 'veg'],
  ['Dal fry', 160, 7, 16, 7, '1 bowl', 'veg'],
  ['Baingan bharta', 130, 3, 12, 8, '1 bowl', 'veg'],
  ['Shahi paneer', 350, 12, 12, 30, '1 bowl', 'veg'],
  ['Paneer butter masala', 380, 14, 12, 32, '1 bowl', 'veg'],
  ['Palak paneer', 280, 12, 12, 22, '1 bowl', 'veg'],
  ['Matar paneer', 260, 11, 14, 18, '1 bowl', 'veg'],
  ['Kadai paneer', 300, 13, 12, 24, '1 bowl', 'veg'],
  ['Paneer bhurji', 220, 14, 8, 16, '1 bowl', 'veg'],
  ['Malai kofta', 380, 10, 22, 30, '1 bowl', 'veg'],
  ['Veg kolhapuri', 250, 7, 22, 16, '1 bowl', 'veg'],
  ['Gobi manchurian', 280, 6, 30, 16, '1 bowl', 'veg'],
  ['Veg manchurian', 300, 6, 34, 16, '1 bowl', 'veg'],
  ['Chili paneer', 320, 16, 14, 24, '1 bowl', 'veg'],
  ['Veg fried rice', 320, 7, 58, 6, '1 plate', 'veg'],
  ['Veg noodles', 350, 8, 60, 8, '1 plate', 'veg'],
  ['Bisibelebath', 350, 11, 58, 8, '1 plate', 'veg'],
  ['Vangi bath', 280, 6, 50, 7, '1 plate', 'veg'],
  ['Avial', 160, 4, 14, 10, '1 bowl', 'veg'],
  ['Cabbage thoran', 110, 3, 10, 7, '1 bowl', 'veg'],
  ['Kootu', 120, 5, 16, 4, '1 bowl', 'veg'],
  ['Chicken tikka', 240, 28, 6, 12, '6 pieces', 'non'],
  ['Chicken 65', 350, 24, 14, 22, '1 plate', 'non'],
  ['Chicken biryani', 550, 25, 60, 20, '1 plate', 'non'],
  ['Mutton curry', 380, 28, 8, 28, '1 bowl', 'non'],
  ['Rogan josh', 400, 28, 10, 30, '1 bowl', 'non'],
  ['Keema matar', 320, 22, 10, 22, '1 bowl', 'non'],
  ['Fish fry', 280, 24, 8, 16, '2 pieces', 'non'],
  ['Prawn curry', 260, 26, 8, 14, '1 bowl', 'non'],
  ['Bhel puri', 180, 4, 32, 5, '1 plate', 'veg'],
  ['Pani puri', 150, 4, 26, 4, '6 pieces', 'veg'],
  ['Sev puri', 200, 4, 30, 8, '6 pieces', 'veg'],
  ['Dahi puri', 220, 6, 30, 9, '6 pieces', 'veg'],
  ['Aloo tikki', 200, 4, 30, 8, '2 pieces', 'veg'],
  ['Vada pav', 290, 6, 38, 12, '1 piece', 'veg'],
  ['Dabeli', 250, 5, 40, 9, '1 piece', 'veg'],
  ['Samosa chaat', 350, 8, 44, 16, '1 plate', 'veg'],
  ['Kachori', 200, 4, 24, 10, '2 pieces', 'veg'],
  ['Dhokla', 160, 6, 28, 3, '4 pieces', 'veg'],
  ['Khandvi', 120, 5, 14, 8, '4 pieces', 'veg'],
  ['Veg momos', 200, 6, 32, 6, '6 pieces', 'veg'],
  ['Chicken momos', 260, 14, 28, 10, '6 pieces', 'non'],
  ['Veg kathi roll', 350, 8, 44, 16, '1 roll', 'veg'],
  ['Chicken kathi roll', 450, 20, 42, 20, '1 roll', 'non'],
  ['Egg roll', 380, 12, 40, 18, '1 roll', 'egg'],
  ['Rasgulla', 140, 4, 32, 1, '2 pieces', 'veg'],
  ['Jalebi', 300, 2, 60, 8, '4 pieces', 'veg'],
  ['Motichoor ladoo', 110, 2, 16, 5, '1 piece', 'veg'],
  ['Besan ladoo', 130, 3, 16, 7, '1 piece', 'veg'],
  ['Kaju katli', 120, 3, 14, 6, '2 pieces', 'veg'],
  ['Mysore pak', 280, 4, 30, 18, '2 pieces', 'veg'],
  ['Payasam', 250, 6, 40, 8, '1 bowl', 'veg'],
  ['Kheer', 220, 6, 34, 8, '1 bowl', 'veg'],
  ['Kulfi', 180, 5, 24, 8, '1 piece', 'veg'],
  ['Rasmalai', 250, 8, 30, 11, '2 pieces', 'veg'],
  ['Shrikhand', 230, 6, 32, 10, '1 bowl', 'veg'],
  ['Chaas', 40, 3, 5, 1, '1 glass', 'veg'],
  ['Badam milk', 200, 7, 24, 9, '1 glass', 'veg'],
  ['Sattu sharbat', 150, 6, 26, 2, '1 glass', 'veg'],
  ['Coconut water', 45, 1, 9, 0, '1 glass', 'veg'],
  ['Sugarcane juice', 180, 0, 45, 0, '1 glass', 'veg'],
  ['Aam panna', 120, 1, 30, 0, '1 glass', 'veg'],
  ['Jaljeera', 30, 1, 6, 0, '1 glass', 'veg'],
  ['Fresh lime soda', 120, 0, 30, 0, '1 glass', 'veg'],
  ['Mango lassi', 280, 7, 48, 6, '1 glass', 'veg'],
  ['Cold coffee', 250, 6, 36, 10, '1 glass', 'veg'],
];

// Alias → canonical food name. Powers plural/dish-name/regional queries
// (items 3–4) without duplicating nutrition rows.
const ALIASES = [
  ['eggs', 'Boiled egg'],
  ['boiled eggs', 'Boiled egg'],
  ['anda', 'Boiled egg'],
  ['omelet', 'Omelette'],
  ['omelette', 'Omelette'],
  ['masala omelet', 'Masala omelette'],
  ['masala omelette', 'Masala omelette'],
  ['sunny side up', 'Fried egg'],
  ['sunny-side up', 'Fried egg'],
  ['sunny side eggs', 'Fried egg'],
  ['fried eggs', 'Fried egg'],
  ['anda fry', 'Fried egg'],
  ['scrambled egg', 'Scrambled eggs'],
  ['anda bhurji', 'Egg bhurji'],
  ['egg bhurji', 'Egg bhurji'],
  ['anda curry', 'Egg curry'],
  ['channa', 'Chole'],
  ['chana', 'Chole'],
  ['chana masala', 'Chole'],
  ['chole masala', 'Chole'],
  ['idly', 'Idli'],
  ['idlies', 'Idli'],
  ['wada', 'Medu vada'],
  ['medu wada', 'Medu vada'],
  ['dosa', 'Dosa'],
  ['dosai', 'Dosa'],
  ['roti', 'Phulka roti'],
  ['chapathi', 'Chapati'],
  ['parantha', 'Aloo paratha'],
  ['paneer butter masala', 'Paneer butter masala'],
  ['palak paneer', 'Palak paneer'],
  ['dal', 'Dal tadka'],
  ['dhal', 'Dal tadka'],
  ['sambhar', 'Sambar'],
  ['rasam', 'Rasam'],
  ['curd rice', 'Curd rice'],
  ['daddojanam', 'Curd rice'],
  ['bhature', 'Chole bhature'],
  ['puri bhaji', 'Puri'],
  ['gol gappa', 'Pani puri'],
  ['golgappa', 'Pani puri'],
  ['puchka', 'Pani puri'],
  ['vada pao', 'Vada pav'],
  ['momo', 'Veg momos'],
  ['momos', 'Veg momos'],
  ['kathi roll', 'Veg kathi roll'],
  ['biriyani', 'Chicken biryani'],
  ['chicken biriyani', 'Chicken biryani'],
];

const insertFood = db.prepare(
  'INSERT OR IGNORE INTO foods (name, kcal, protein_g, carbs_g, fat_g, portion, veg, verified) VALUES (?, ?, ?, ?, ?, ?, ?, 1)'
);
for (const f of FOODS) insertFood.run(...f);

db.exec(`CREATE TABLE IF NOT EXISTS food_aliases (
  alias TEXT PRIMARY KEY, name TEXT
)`);
const insertAlias = db.prepare('INSERT OR IGNORE INTO food_aliases (alias, name) VALUES (?, ?)');
for (const a of ALIASES) insertAlias.run(a[0].toLowerCase(), a[1]);

// USDA SR Legacy reference rows (public domain; see seed/NOTICE.md).
// Loaded from the committed generated JSON so every environment — local,
// test, EC2 — gets the same catalog without network access at boot.
try {
  const seedUrl = new URL('../seed/usda-seed.json', import.meta.url);
  const usda = JSON.parse(fs.readFileSync(seedUrl, 'utf8'));
  const insertUsda = db.prepare(
    `INSERT OR IGNORE INTO foods (name, kcal, protein_g, carbs_g, fat_g, portion, veg, verified, source)
     VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'USDA SR Legacy')`
  );
  const seedMany = db.transaction((rows) => {
    for (const r of rows) insertUsda.run(r.name, r.kcal, r.protein_g, r.carbs_g, r.fat_g, r.portion, r.veg);
  });
  seedMany(usda);
} catch (e) {
  console.warn('[db] USDA seed not loaded:', e.message);
}

export function today(offsetDays = 0) {
  const d = new Date(Date.now() + offsetDays * 86400000);
  return d.toISOString().slice(0, 10);
}
