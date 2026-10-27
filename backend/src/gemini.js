const BASE = 'https://generativelanguage.googleapis.com/v1beta/models';
const MODEL = process.env.GEMINI_MODEL || 'gemini-2.5-flash';
const TIMEOUT_MS = 25000;

const SCAN_PROMPT = `You are Mor's food vision engine. You look at a photo of Indian food and return a strict CSV — nothing else. No markdown, no code fences, no commentary.

Output exactly this shape, in this order:

meal,<meal name in Title Case, <=30 chars>
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
<one row per visible dish or drink>

Rules:
- kcal: integer, per the portion you state.
- protein_g, carbs_g, fat_g: integers or empty.
- portion: short human text like "1 bowl", "2 pieces", "¾ plate".
- confidence: 0.0-1.0, your certainty the identification is right.
- unsure: 1 if confidence < 0.6 (e.g. two dishes look alike), else 0.
- alternatives: for unsure rows only, 2-3 candidate names separated by "|", else empty.
- One row per distinct food; sauces/pickles counted only if clearly visible.
- Use standard Indian dish names (Dal tadka, Phulka roti, Jeera rice, Aloo gobhi...).
- If the photo has no recognisable food: output only the header row, nothing else.`;

const ESTIMATE_PROMPT = `You are Mor's food estimator. Given the name of an Indian dish or drink, return a strict CSV with exactly one item row — nothing else. No markdown, no code fences.

meal,<dish name in Title Case>
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
<the single row>

Rules: kcal integer per the stated portion; grams integers or empty; portion short human text like "1 bowl"; confidence 0.0-1.0; unsure 1 only if the dish name is ambiguous, with alternatives separated by "|".`;

const VOICE_PROMPT = `You are Mor's meal parser. Given a voice transcript describing a meal in English (or Hinglish), return a strict CSV — nothing else. No markdown, no code fences.

meal,<meal name in Title Case>
name,kcal,protein_g,carbs_g,fat_g,portion,confidence,unsure,alternatives
<one row per food mentioned>

Rules: standard Indian dish names; realistic kcal per stated portion; confidence 0.0-1.0; unsure 1 only if ambiguous, alternatives separated by "|".`;

function key() {
  const k = process.env.GEMINI_API_KEY;
  if (!k) throw Object.assign(new Error('GEMINI_API_KEY missing'), { status: 503 });
  return k;
}

async function call(body) {
  const res = await fetch(`${BASE}/${MODEL}:generateContent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key() },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw Object.assign(new Error(`gemini_${res.status}: ${text.slice(0, 200)}`), { status: 502 });
  }
  const json = await res.json();
  const parts = json.candidates?.[0]?.content?.parts || [];
  const text = parts.map((p) => p.text || '').join('').trim();
  if (!text) throw Object.assign(new Error('gemini_empty_response'), { status: 502 });
  return text;
}

function contents(text, image) {
  const parts = [{ text }];
  if (image) parts.push({ inline_data: { mime_type: image.mime || 'image/jpeg', data: image.base64 } });
  return [{ role: 'user', parts }];
}

export async function analyzeImage({ base64, mime }) {
  const body = {
    contents: contents(SCAN_PROMPT, { base64, mime }),
    generationConfig: { temperature: 0.2, maxOutputTokens: 1024, thinkingConfig: { thinkingBudget: 0 } },
  };
  return call(body);
}

export async function estimateDish(name) {
  const body = {
    contents: contents(`${ESTIMATE_PROMPT}\nDish: ${name}`),
    generationConfig: { temperature: 0.2, maxOutputTokens: 512, thinkingConfig: { thinkingBudget: 0 } },
  };
  return call(body);
}

export async function parseTranscript(transcript) {
  const body = {
    contents: contents(`${VOICE_PROMPT}\nTranscript: ${transcript}`),
    generationConfig: { temperature: 0.2, maxOutputTokens: 768, thinkingConfig: { thinkingBudget: 0 } },
  };
  return call(body);
}

export const model = MODEL;
