import React, { useEffect, useRef, useState, useCallback } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { scanImage, fileToBase64, downscaleToJpeg, get, post, API } from '../api.js';
import { NeeluImg, NeeluVideo, NeeluSvg, Segmented, FeatherEye, Screen } from '../components/ui.jsx';
import FoodIcon, { kindForFood } from '../components/FoodIcon.jsx';

export const pendingScan = { image: null, slot: 'lunch', gallery: false };

function Chip({ children }) {
  return <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: 'rgba(10,15,18,0.72)', borderRadius: 22, padding: '6px 16px 6px 6px', fontSize: 15, fontWeight: 600, color: '#fff' }}>
    <span style={{ width: 28, height: 28, borderRadius: '50%', background: 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
      <NeeluSvg size={30} mood="cool" />
    </span>
    {children}
  </span>;
}

export function Camera() {
  const [params] = useSearchParams();
  const slot = params.get('slot') || 'lunch';
  const autoGallery = params.get('gallery') === '1';
  const nav = useNavigate();
  const videoRef = useRef(null);
  const fileRef = useRef(null);
  const [state, setState] = useState('starting');
  const [mode, setMode] = useState('meal');
  const [flash, setFlash] = useState(false);
  const streamRef = useRef(null);

  const stop = useCallback(() => { streamRef.current?.getTracks().forEach((t) => t.stop()); }, []);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 1280 } }, audio: false });
        if (!alive) { stream.getTracks().forEach((t) => t.stop()); return; }
        streamRef.current = stream;
        if (videoRef.current) { videoRef.current.srcObject = stream; await videoRef.current.play().catch(() => {}); }
        setState('live');
      } catch (e) {
        setState(e.name === 'NotAllowedError' || e.name === 'NotFoundError' ? 'denied' : 'denied');
      }
    })();
    return () => { alive = false; stop(); };
  }, [stop]);

  useEffect(() => { if (autoGallery) fileRef.current?.click(); }, [autoGallery]);

  const capture = async () => {
    const v = videoRef.current;
    if (!v || !v.videoWidth) return;
    const c = document.createElement('canvas');
    c.width = v.videoWidth; c.height = v.videoHeight;
    const ctx = c.getContext('2d');
    ctx.drawImage(v, 0, 0);
    if (flash) { ctx.fillStyle = 'rgba(255,255,255,0.25)'; ctx.fillRect(0, 0, c.width, c.height); }
    pendingScan.image = downscaleToJpeg(c);
    pendingScan.slot = slot;
    pendingScan.gallery = false;
    stop();
    nav('/scanning');
  };

  const pickFile = async (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    const b64 = await fileToBase64(f);
    pendingScan.image = downscaleToJpeg(await createImageBitmap(new Blob([new Uint8Array(atob(b64).split('').map((ch) => ch.charCodeAt(0)))])));
    pendingScan.slot = slot;
    pendingScan.gallery = true;
    stop();
    nav('/scanning');
  };

  const dark = { background: '#0A0F12', color: '#fff' };
  return (
    <div className="page" style={dark}>
      <video ref={videoRef} playsInline muted autoPlay
        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', filter: 'brightness(0.96)' }} />
      {state === 'live' && (
        <>
          <div style={{ position: 'absolute', left: 24, top: 194, width: 312, height: 312, boxShadow: '0 0 0 100vmax rgba(10,15,18,0.35)', borderRadius: 6 }} aria-hidden="true" />
          <svg width="312" height="312" viewBox="0 0 312 312" style={{ position: 'absolute', left: 24, top: 194 }}>
            <path d="M2 50 V14 Q2 2 14 2 H50 M262 2 H298 Q310 2 310 14 V50 M310 262 V298 Q310 310 298 310 H262 M50 310 H14 Q2 310 2 298 V262"
              stroke="#F2B544" strokeWidth="4" fill="none" strokeLinecap="round" />
          </svg>
          <div style={{ position: 'absolute', left: 0, right: 0, top: 524, display: 'flex', justifyContent: 'center' }}>
            <Chip>Fit the whole plate</Chip>
          </div>
        </>
      )}

      <div style={{ position: 'absolute', left: 16, right: 16, top: 18, display: 'flex', alignItems: 'center', justifyContent: 'space-between', zIndex: 5 }}>
        <Link to="/add" aria-label="Close scanner" style={{ width: 44, height: 44, borderRadius: '50%', background: 'rgba(255,255,255,0.14)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.6" strokeLinecap="round"><path d="M6 6 L18 18 M18 6 L6 18" /></svg>
        </Link>
        <div role="tablist" style={{ display: 'flex', background: 'rgba(10,15,18,0.6)', borderRadius: 20, padding: 3 }}>
          {['meal', 'label', 'barcode'].map((m) => (
            <button key={m} role="tab" aria-selected={mode === m} onClick={() => setMode(m)}
              style={{ height: 34, padding: '0 12px', border: 'none', borderRadius: 17, background: mode === m ? '#fff' : 'transparent', color: mode === m ? '#142027' : '#E5ECEE', fontSize: 13.5, fontWeight: mode === m ? 700 : 600, textTransform: 'capitalize', cursor: 'pointer' }}>{m}</button>
          ))}
        </div>
        <button onClick={() => setFlash(!flash)} aria-label={`Flash ${flash ? 'on' : 'off'}`} style={{ width: 44, height: 44, borderRadius: '50%', border: 'none', background: flash ? 'var(--gold)' : 'rgba(255,255,255,0.14)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={flash ? '#142027' : '#fff'} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 3 L5 13 H12 L11 21 L19 11 H12 Z" /></svg>
        </button>
      </div>

      {state === 'denied' && (
        <div style={{ position: 'absolute', left: 20, right: 20, top: 150, background: '#142027', borderRadius: 28, padding: '24px 20px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: 10, zIndex: 6 }}>
          <NeeluVideo anim="droop" size={150} />
          <h1 className="h1" style={{ color: '#fff' }}>Camera access needed</h1>
          <p style={{ margin: 0, fontSize: 17, color: '#C3D1D4' }}>Neelu needs to see your food.</p>
          <button className="btn gold" style={{ marginTop: 8 }} onClick={() => alert('Allow camera access in your browser site settings, then reopen the scanner.')}>Open settings</button>
          <button className="btn secondary" onClick={async () => {
            try { await navigator.mediaDevices.getUserMedia({ video: true }); setState('starting'); window.location.reload(); } catch { nav(`/search?slot=${slot}`); }
          }}>Try again</button>
          <Link to={`/search?slot=${slot}`} style={{ width: '100%', height: 48, borderRadius: 24, border: '1.5px solid #3E5560', color: '#fff', fontSize: 16, fontWeight: 600, textDecoration: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Search food instead</Link>
        </div>
      )}

      {state === 'starting' && (
        <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12, zIndex: 4 }}>
          <div className="spin" style={{ width: 44, height: 44, border: '4px solid rgba(255,255,255,0.2)', borderTopColor: 'var(--gold)', borderRadius: '50%' }} />
          <span style={{ color: '#C3D1D4' }}>Starting camera…</span>
        </div>
      )}

      {state === 'live' && (
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 170, background: '#0A0F12', display: 'flex', alignItems: 'center', justifyContent: 'space-around', padding: '0 24px 24px', zIndex: 5 }}>
          <button aria-label="Pick from gallery" onClick={() => fileRef.current?.click()}
            style={{ width: 52, height: 52, borderRadius: 14, background: '#2A3A40', border: '2px solid #fff', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
            <FoodIcon kind="dosa" size={40} />
          </button>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={pickFile} />
          <button aria-label="Take photo" onClick={capture} disabled={state !== 'live'}
            style={{ width: 84, height: 84, borderRadius: '50%', border: '4px solid #fff', background: 'none', cursor: 'pointer' }}>
            <span style={{ display: 'flex', width: 66, height: 66, borderRadius: '50%', background: 'var(--gold)', alignItems: 'center', justifyContent: 'center', margin: '2px' }}>
              <span style={{ display: 'flex', width: 50, height: 50, borderRadius: '50%', background: 'var(--pankh)', alignItems: 'center', justifyContent: 'center' }}>
                <span style={{ width: 30, height: 30, borderRadius: '50%', background: 'var(--jamun)' }} />
              </span>
            </span>
          </button>
          <Link to={`/search?slot=${slot}`} aria-label="Search instead" style={{ width: 52, height: 52, borderRadius: '50%', background: 'rgba(255,255,255,0.14)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.2" strokeLinecap="round"><circle cx="11" cy="11" r="6" /><path d="M16 16 L20 20" /></svg>
          </Link>
        </div>
      )}
    </div>
  );
}

const CHECKS = [
  { k: 'items', label: 'Reading dishes' },
  { k: 'portions', label: 'Estimating portions' },
  { k: 'oil', label: 'Counting oil' },
];

export function Scanning() {
  const nav = useNavigate();
  const { showSnack } = useStore();
  const [progress, setProgress] = useState(8);
  const [failed, setFailed] = useState(false);
  const startedRef = useRef(false);

  useEffect(() => {
    if (!pendingScan.image) { nav('/add', { replace: true }); return; }
    if (startedRef.current) return;
    startedRef.current = true;
    let done = false;
    const iv = setInterval(() => setProgress((p) => Math.min(92, p + Math.ceil(Math.random() * 7))), 420);
    (async () => {
      try {
        const r = await scanImage(pendingScan.image, 'image/jpeg', pendingScan.slot);
        done = true;
        clearInterval(iv);
        window.__lastScan = r;
        nav(`/result?scan_id=${r.scan_id}`, { replace: true });
      } catch (e) {
        done = true; clearInterval(iv); setFailed(true);
        showSnack(`Scan failed: ${e.message}`, 'err');
        setTimeout(() => nav('/camera', { replace: true }), 1400);
      }
    })();
    return () => clearInterval(iv);
  }, [nav, showSnack]);

  return (
    <div className="page" style={{ background: '#0A0F12', color: '#fff' }}>
      <div style={{ position: 'absolute', left: 90, top: 120, width: 180, height: 180, borderRadius: '50%', border: '3px solid var(--gold)', overflow: 'hidden' }}>
        {pendingScan.image
          ? <img src={`data:image/jpeg;base64,${pendingScan.image}`} alt="Your plate" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          : <div style={{ width: '100%', height: '100%', background: '#3B2B21' }} />}
        <div className="scanline" style={{ left: 0, right: 0 }} />
      </div>
      {pendingScan.image && <img src={`data:image/jpeg;base64,${pendingScan.image}`} alt="" style={{ position: 'absolute', inset: 0, width: '100%', height: 520, objectFit: 'cover', opacity: 0.5, filter: 'blur(2px)' }} />}
      <div style={{ position: 'absolute', left: 0, right: 0, top: 250, display: 'flex', justifyContent: 'center' }}>
        <Chip>Reading your plate…</Chip>
      </div>
      <NeeluVideo anim="think" size={120} style={{ position: 'absolute', left: 24, top: 560 }} />
      <div className="sheet" style={{ background: '#142027', color: '#fff' }}>
        <div className="grab" style={{ background: '#3E5560' }} />
        <h2 className="h2" style={{ color: '#fff' }}>Reading your plate…</h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, margin: '12px 0' }}>
          {CHECKS.map((c, i) => {
            const active = progress > i * 33;
            const running = progress > i * 33 && progress < (i + 1) * 33 + 10;
            return (
              <div key={c.k} style={{ display: 'flex', alignItems: 'center', gap: 10, opacity: active ? 1 : 0.4 }}>
                {running ? <div className="spin" style={{ width: 20, height: 20, border: '2.5px solid rgba(255,255,255,.25)', borderTopColor: 'var(--gold)', borderRadius: '50%' }} />
                  : <span style={{ width: 20, height: 20, borderRadius: '50%', background: active ? 'var(--panna)' : 'transparent', border: '2px solid var(--panna)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 800 }}>{active ? '✓' : ''}</span>}
                <span style={{ fontSize: 15.5, fontWeight: 600 }}>{c.label}{running ? '…' : ''}</span>
              </div>
            );
          })}
        </div>
        <div className="progressbar"><div style={{ width: `${progress}%`, background: 'var(--gold)' }} /></div>
        <button className="btn textlike" style={{ color: '#C3D1D4', marginTop: 10 }} onClick={() => nav('/camera', { replace: true })}>Cancel</button>
      </div>
      {failed && <div className="snackbar err">Couldn't read the plate. Trying again…</div>}
    </div>
  );
}

export function Result() {
  const [params] = useSearchParams();
  const scanId = params.get('scan_id');
  const [scan, setScan] = useState(window.__lastScan?.scan_id === Number(scanId) ? window.__lastScan : null);
  const [items, setItems] = useState(null);
  const [oil, setOil] = useState('normal');
  const nav = useNavigate();
  const { showSnack } = useStore();

  useEffect(() => {
    (async () => {
      if (!scan) {
        try { const s = await get(`/scans/${scanId}`); setScan(s); setItems(s.items); }
        catch { nav('/add', { replace: true }); return; }
      } else setItems(scan.items);
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scanId]);

  if (!scan) return <div className="page" style={{ background: '#0E1A1F', justifyContent: 'center', alignItems: 'center' }}><div className="spin" style={{ width: 40, height: 40, border: '4px solid rgba(255,255,255,.2)', borderTopColor: 'var(--gold)', borderRadius: '50%' }} /></div>;

  const failed = scan.status === 'failed' || !items?.length;
  const unsureRow = items?.findIndex((i) => i.unsure);

  const pickAlt = async (rowIdx, alt) => {
    try {
      const { items: matches } = await get(`/foods?q=${encodeURIComponent(alt)}`);
      const hit = matches.find((f) => f.name.toLowerCase() === alt.toLowerCase()) || matches[0];
      const next = items.map((it, i) => i === rowIdx ? {
        ...it, name: alt, unsure: false, alternatives: [],
        kcal: hit?.kcal ?? it.kcal, protein_g: hit?.protein_g ?? it.protein_g,
        carbs_g: hit?.carbs_g ?? it.carbs_g, fat_g: hit?.fat_g ?? it.fat_g,
      } : it);
      setItems(next);
    } catch { showSnack('Could not swap dish', 'err'); }
  };

  const logMeal = async () => {
    const clean = items.filter((i) => !i.unsure);
    const r = await post('/meals', {
      slot: pendingScan.slot, source: 'scan', scan_id: scan.scan_id, oil_level: oil,
      name: scan.meal_name || 'Scanned meal',
      items: clean.map((i) => ({ name: i.name, kcal: i.kcal, protein_g: i.protein_g, carbs_g: i.carbs_g, fat_g: i.fat_g, portion: i.portion })),
    });
    window.__lastLog = r;
    nav('/logged', { replace: true });
  };

  const total = items?.reduce((a, i) => a + i.kcal, 0) || 0;

  return (
    <div className="page" style={{ background: '#0E1A1F' }}>
      <div style={{ position: 'absolute', left: 90, top: 34, width: 180, height: 180, borderRadius: '50%', overflow: 'hidden', border: `3px solid ${failed ? 'var(--kumkum)' : 'var(--gold)'}`, background: '#3B2B21' }}>
        {scan.image_url
          ? <img src={`${API}${scan.image_url}`} alt="Your plate" style={{ width: '100%', height: '100%', objectFit: 'cover' }} onError={(e) => { e.currentTarget.style.display = 'none'; }} />
          : <FoodIcon kind="thali" size={154} style={{ margin: 10 }} />}
      </div>
      {!failed && items.map((it, i) => (
        <span key={i} style={{ position: 'absolute', left: [124, 172, 194, 128, 212][i % 5], top: [84, 52, 128, 130, 86][i % 5], width: 22, height: 22, borderRadius: '50%', background: it.unsure ? 'var(--gold)' : '#fff', color: '#142027', fontSize: 12, fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center', border: '2px solid #fff', zIndex: 3 }}>{it.idx || i + 1}</span>
      ))}
      <Link to="/camera" aria-label="Back to camera" style={{ position: 'absolute', left: 16, top: 18, width: 44, height: 44, borderRadius: '50%', background: 'rgba(255,255,255,0.14)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 5 }}>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.6" strokeLinecap="round"><path d="M6 6 L18 18 M18 6 L6 18" /></svg>
      </Link>

      {failed ? (
        <section className="sheet" style={{ top: 232, position: 'absolute', borderRadius: '28px 28px 0 0', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, textAlign: 'center', paddingBottom: 20 }}>
          <div style={{ marginTop: -70 }}><NeeluVideo anim="droop" size={150} /></div>
          <h1 className="h1" style={{ fontSize: 32 }}>Couldn't see the food</h1>
          <p className="muted" style={{ fontSize: 18 }}>{scan.status === 'failed' && items?.length === 0 ? 'No dish recognised. Try better light.' : 'Scan failed. Try again.'}</p>
          <div style={{ marginTop: 'auto', width: '100%', display: 'flex', flexDirection: 'column', gap: 8, paddingBottom: 10 }}>
            <Link to="/camera" className="btn" style={{ textDecoration: 'none' }}>Retake photo</Link>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              <Link to="/search" className="btn secondary" style={{ textDecoration: 'none' }}>Search food</Link>
              <Link to="/search?voice=1" className="btn secondary" style={{ textDecoration: 'none' }}>Speak</Link>
            </div>
          </div>
        </section>
      ) : (
        <section className="sheet" style={{ top: 232, position: 'absolute', borderRadius: '28px 28px 0 0', display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div className="grab" />
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ flex: 1 }}>
              <span className="small" style={{ color: 'var(--sub)', fontWeight: 600 }}>{(pendingScan.slot || 'lunch')[0].toUpperCase() + (pendingScan.slot || 'lunch').slice(1)} · Just now</span>
              <h1 className="h1" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>{scan.meal_name || 'Your meal'}
                <span className="pill ai"><FeatherEye size={14} /> AI estimate</span>
              </h1>
            </div>
            <button aria-label="Discard meal" onClick={() => nav('/add', { replace: true })} style={{ width: 40, height: 40, borderRadius: '50%', border: 'none', background: 'var(--disabled)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--sub)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M5 7 H19 M10 7 V4 H14 V7 M7 7 L8 20 H16 L17 7" /></svg>
            </button>
          </div>

          {unsureRow >= 0 && (
            <div style={{ background: '#FFF3DC', border: '1.5px solid #F2D9A6', borderRadius: 16, padding: '10px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ width: 22, height: 22, borderRadius: '50%', background: 'var(--gold)', color: '#142027', fontSize: 12, fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{items[unsureRow].idx}</span>
                <strong style={{ flex: 1, fontSize: 18, color: '#5C3A05' }}>{items[unsureRow].name}?</strong>
              </div>
              <div role="radiogroup" style={{ display: 'flex', gap: 8 }}>
                {items[unsureRow].alternatives.slice(0, 2).map((a) => (
                  <button key={a} className="btn sm" style={{ flex: 1, background: '#fff', color: '#142027', border: '1.5px solid #C98A1E', fontWeight: 700 }} onClick={() => pickAlt(unsureRow, a)}>{a}</button>
                ))}
                <button className="btn textlike" style={{ color: '#5C3A05' }} onClick={() => pickAlt(unsureRow, items[unsureRow].alternatives[0])}>Other</button>
              </div>
            </div>
          )}

          <div className="rowlist" style={{ flex: 1, overflowY: 'auto' }}>
            {items.map((it, i) => (
              <div className="rowitem" key={i}>
                <span style={{ position: 'relative', display: 'flex' }}>
                  <FoodIcon kind={kindForFood(it.name)} size={38} />
                  <span style={{ position: 'absolute', left: -4, top: -4, width: 17, height: 17, borderRadius: '50%', background: 'var(--ink)', color: '#fff', fontSize: 10, fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{it.idx || i + 1}</span>
                </span>
                <span className="grow">
                  <strong style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 4 }}>{it.name}
                    <svg width="12" height="12" viewBox="0 0 24 24" role="img" aria-label="AI estimate"><path d="M12 2 L13.8 9 L21 11 L13.8 13 L12 20 L10.2 13 L3 11 L10.2 9 Z" fill={it.unsure ? 'var(--genda)' : 'var(--jamun)'} /></svg>
                  </strong>
                  <span className="small" style={{ color: 'var(--teal)', fontWeight: 600 }}>{it.portion}{it.unsure ? ' · needs a check' : ''}</span>
                </span>
                <strong className="num" style={{ fontSize: 14.5, whiteSpace: 'nowrap' }}>{it.unsure ? '—' : it.kcal} kcal</strong>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="small" style={{ fontWeight: 600, color: 'var(--sub)', whiteSpace: 'nowrap' }}>Oil</span>
            <div style={{ flex: 1 }}><Segmented options={['less', 'normal', 'more']} value={oil} onChange={setOil} /></div>
          </div>

          <div style={{ display: 'flex', gap: 10, alignItems: 'center', paddingBottom: 4 }}>
            <Link to="/search" aria-label="Add another item" style={{ width: 56, height: 56, borderRadius: '50%', background: '#fff', border: '1.5px solid var(--line2)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--teal)" strokeWidth="2.6" strokeLinecap="round"><path d="M12 5 V19 M5 12 H19" /></svg>
            </Link>
            <button className="btn" style={{ position: 'relative', flex: 1 }} onClick={logMeal} disabled={unsureRow >= 0}>
              <span style={{ flex: 1 }}>Log {total} kcal</span>
              <span className="badge-eye"><span style={{ width: 30, height: 30, borderRadius: '50%', background: 'var(--pankh)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12 L10 17 L19 7" /></svg>
              </span></span>
            </button>
          </div>
        </section>
      )}
    </div>
  );
}

export function Logged() {
  const log = window.__lastLog;
  const { home, refreshHome } = useStore();
  const nav = useNavigate();
  useEffect(() => { refreshHome(); }, [refreshHome]);
  const kcal = log?.kcal || 0;
  const left = home ? Math.max(0, home.kcal_target - (home.kcal_eaten)) : 940;
  return (
    <Screen bg="linear-gradient(180deg,#F8F5EE 0%,#FDEBC8 100%)" style={{ alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
      <span className="pill gold" style={{ fontSize: 14, marginBottom: 6 }}>+{log?.coins_awarded || 10} coins</span>
      <NeeluVideo anim="hop" size={200} />
      <h1 className="h1" style={{ fontSize: 40, marginTop: 2 }}>Logged!</h1>
      <p className="muted" style={{ fontSize: 18, marginTop: 0 }}>{kcal} kcal · {left.toLocaleString()} kcal left today</p>
      <div style={{ display: 'flex', gap: 10, marginTop: 26, width: 'calc(100% - 48px)' }}>
        <button className="btn secondary" style={{ flex: 1 }} onClick={() => log && nav(`/meal/${log.id}`)}>See details</button>
        <button className="btn" style={{ flex: 1 }} onClick={() => nav('/home', { replace: true })}>Done</button>
      </div>
    </Screen>
  );
}
