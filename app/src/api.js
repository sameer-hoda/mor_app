export const API = import.meta.env.VITE_API_URL || '';
const BASE = `${API}/api`;

async function req(path, opts = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', 'x-user-id': '1' },
    ...opts,
  });
  const j = await res.json().catch(() => ({}));
  if (!res.ok) throw Object.assign(new Error(j.error || `HTTP ${res.status}`), { status: res.status, body: j });
  return j;
}

export const get = (p) => req(p);
export const post = (p, body) => req(p, { method: 'POST', body: JSON.stringify(body || {}) });
export const del = (p) => req(p, { method: 'DELETE' });

export async function scanImage(base64, mime = 'image/jpeg', slot = 'lunch') {
  return post('/scan', { image_base64: base64, mime, slot });
}

export function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(String(r.result).split(',')[1]);
    r.onerror = reject;
    r.readAsDataURL(file);
  });
}

export function downscaleToJpeg(sourceCanvasOrBitmap, maxEdge = 1024, quality = 0.85) {
  const src = sourceCanvasOrBitmap;
  const w = src.width, h = src.height;
  const scale = Math.min(1, maxEdge / Math.max(w, h));
  const c = document.createElement('canvas');
  c.width = Math.round(w * scale);
  c.height = Math.round(h * scale);
  c.getContext('2d').drawImage(src, 0, 0, c.width, c.height);
  return c.toDataURL('image/jpeg', quality).split(',')[1];
}
