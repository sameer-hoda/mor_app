import express from 'express';
import cors from 'cors';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import dotenv from 'dotenv';

dotenv.config();

import { router, uploadsDir } from './routes.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();
app.use(cors({ origin: true }));
app.use(express.json({ limit: '12mb' }));
app.use('/uploads', express.static(uploadsDir));
app.use('/api', router);

app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: 'internal_error' });
});

const PORT = Number(process.env.PORT || 4000);
app.listen(PORT, () => {
  console.log(`Mor backend on http://localhost:${PORT} · model=${process.env.GEMINI_MODEL || 'gemini-2.5-flash'} · key=${process.env.GEMINI_API_KEY ? 'loaded' : 'MISSING'}`);
});
