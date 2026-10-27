import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, Link, useSearchParams, useParams } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { get, post, del, API } from '../api.js';
import { VegMark, Segmented, NeeluImg, NeeluVideo, FeatherEye } from '../components/ui.jsx';
import FoodIcon, { kindForFood } from '../components/FoodIcon.jsx';

const OIL = { less: 0.85, normal: 1, more: 1.15 };
const TABS = ['Recent', 'Saved', 'Restaurant'];

export function Search() {
  const [params] = useSearchParams();
  const initialQ = params.get('q') || '';
  const voiceStart = params.get('voice') === '1';
  const [q, setQ] = useState(initialQ);
  const [rows, setRows] = useState([]);
  const [tab, setTab] = useState('Recent');
  const [cart, setCart] = useState([]);
  const [searched, setSearched] = useState(Boolean(initialQ));
  const [voice, setVoice] = useState(voiceStart);
  const [estimating, setEstimating] = useState(false);
  const nav = useNavigate();
  const { showSnack } = useStore();

  useEffect(() => {
    (async () => {
      const j = await get(`/foods${q ? `?q=${encodeURIComponent(q)}` : ''}`);
      setRows(j.items);
      setSearched(true);
    })().catch(() => {});
  }, [q]);

  const toggle = (f) => {
    setCart((c) => c.find((x) => x.name === f.name) ? c.filter((x) => x.name !== f.name) : [...c, { ...f, qty: 1 }]);
  };

  const estimate = async () => {
    setEstimating(true);
    try {
      const r = await post('/foods/estimate', { name: q });
      setRows((cur) => [r.item, ...cur]);
      setCart((c) => [...c, { ...r.item, qty: 1 }]);
      showSnack(`Neelu estimates ${r.item.kcal} kcal`, 'ok');
    } catch (e) { showSnack('Estimate failed', 'err'); }
    setEstimating(false);
  };

  const logCart = async () => {
    const slot = params.get('slot') || 'dinner';
    const r = await post('/meals', {
      slot, source: 'search', name: cart.length === 1 ? cart[0].name : 'Mixed meal',
      items: cart.map((i) => ({ name: i.name, kcal: Math.round(i.kcal * (i.qty || 1)), protein_g: i.protein_g, carbs_g: i.carbs_g, fat_g: i.fat_g, portion: `${i.portion} ×${i.qty || 1}` })),
    });
    window.__lastLog = r;
    nav('/logged', { replace: true });
  };

  const cartTotal = cart.reduce((a, i) => a + i.kcal * (i.qty || 1), 0);

  return (
    <div className="page" style={{ background: 'var(--chandan)' }}>
      <div style={{ display: 'flex', gap: 10, padding: '14px 16px 8px', alignItems: 'center' }}>
        <Link to="/add" aria-label="Close" style={{ width: 40, height: 40, borderRadius: '50%', background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid var(--line)', flexShrink: 0 }}>
          <svg width="16" height="16" viewBox="0 0 24 24" stroke="var(--ink)" strokeWidth="2.6" fill="none" strokeLinecap="round"><path d="M6 6 L18 18 M18 6 L6 18" /></svg>
        </Link>
        <div style={{ flex: 1, display: 'flex', gap: 6 }}>
          <input className="field" style={{ flex: 1 }} autoFocus placeholder="Search dal, paneer, dosa…" value={q} onChange={(e) => setQ(e.target.value)} />
          <button onClick={() => setVoice(true)} aria-label="Voice search" style={{ width: 52, height: 52, borderRadius: 14, border: 'none', background: 'var(--teal)', color: '#fff', cursor: 'pointer', fontSize: 20 }}>🎙️</button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, padding: '4px 16px 10px' }}>
        {TABS.map((t) => <button key={t} className={`chip ${tab === t ? 'on' : ''}`} onClick={() => setTab(t)}>{t}</button>)}
      </div>

      {voice ? <VoicePanel transcript={q} setTranscript={setQ} onClose={() => setVoice(false)} onParse={async (t) => {
        const r = await post('/voice-log', { transcript: t });
        setCart(r.items.map((i) => ({ name: i.name, kcal: i.kcal, protein_g: i.protein_g, carbs_g: i.carbs_g, fat_g: i.fat_g, portion: i.portion, qty: 1 })));
        setVoice(false);
      }} /> : (
        <div style={{ flex: 1, padding: '0 16px', overflowY: 'auto', paddingBottom: cart.length ? 150 : 20 }}>
          {rows.length === 0 && searched ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: 10, paddingTop: 30 }}>
              <NeeluVideo anim="think" size={140} />
              <h2 className="h2">No match for “{q}”</h2>
              <p className="muted" style={{ margin: 0 }}>Neelu can estimate it from its name.</p>
              <button className="btn" disabled={estimating || !q} onClick={estimate}>{estimating ? 'Estimating…' : 'Estimate with Neelu'}</button>
              <button className="btn secondary" onClick={() => { setCart([{ name: q, kcal: 200, protein_g: 4, carbs_g: 20, fat_g: 8, portion: '1 serving', veg: 'veg', qty: 1 }]); }}>Create custom food</button>
              <Link to="/camera" className="btn secondary" style={{ textDecoration: 'none' }}>Scan instead</Link>
            </div>
          ) : (
            <div className="rowlist">
              {rows.map((f) => {
                const inCart = cart.some((x) => x.name === f.name);
                return (
                  <button key={f.name} className="rowitem" style={{ width: '100%', background: 'none', border: 'none', borderTop: '1px solid #F1ECE1', cursor: 'pointer', fontFamily: 'inherit' }}
                    onClick={() => nav(`/food?name=${encodeURIComponent(f.name)}&slot=${params.get('slot') || 'dinner'}`)}>
                    <FoodIcon kind={kindForFood(f.name)} size={40} />
                    <span className="grow" style={{ textAlign: 'left' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <VegMark kind={f.veg} />
                        <strong style={{ fontSize: 15 }}>{f.name}</strong>
                        {f.verified ? <span className="pill verified">Verified</span> : <span className="pill ai">AI</span>}
                      </span>
                      <span className="small muted">{f.portion} · {f.kcal} kcal</span>
                    </span>
                    <span onClick={(e) => { e.stopPropagation(); toggle(f); }} role="button" aria-label={`Add ${f.name}`}
                      style={{ width: 34, height: 34, borderRadius: '50%', border: inCart ? 'none' : '1.5px solid var(--teal)', background: inCart ? 'var(--teal)' : '#fff', color: inCart ? '#fff' : 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 16 }}>
                      {inCart ? '✓' : '+'}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}

      {cart.length > 0 && !voice && (
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, background: '#fff', borderTop: '1px solid var(--line)', padding: '10px 16px 18px', display: 'flex', alignItems: 'center', gap: 10, zIndex: 10 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="small" style={{ fontWeight: 700 }}>{(params.get('slot') || 'dinner')[0].toUpperCase() + (params.get('slot') || 'dinner').slice(1)} · {cart.length} item{cart.length > 1 ? 's' : ''}</div>
            <div className="small muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{cart.map((c) => `${c.name} ×${c.qty || 1}`).join(' · ')}</div>
          </div>
          <button className="btn" style={{ width: 160 }} onClick={logCart}>Log {cartTotal} kcal</button>
        </div>
      )}
    </div>
  );
}

function VoicePanel({ transcript, setTranscript, onClose, onParse }) {
  const recRef = useRef(null);
  const [listening, setListening] = useState(false);
  const [parsing, setParsing] = useState(false);

  useEffect(() => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) return;
    const rec = new SR();
    rec.lang = 'en-IN'; rec.continuous = true; rec.interimResults = true;
    rec.onresult = (e) => {
      let t = '';
      for (const r of e.results) t += r[0].transcript;
      setTranscript(t);
    };
    rec.onend = () => setListening(false);
    rec.onerror = () => setListening(false);
    recRef.current = rec;
    return () => { try { rec.stop(); } catch {} };
  }, [setTranscript]);

  const chips = useMemo(() => transcript.split(/(?:,| and | with )/i).map((s) => s.trim()).filter(Boolean).slice(0, 4), [transcript]);

  return (
    <div className="dialog" style={{ top: 130, bottom: 'auto', left: 16, right: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span className="chip on">English</span>
        <button onClick={onClose} style={{ border: 'none', background: 'none', fontSize: 18, cursor: 'pointer' }}>✕</button>
      </div>
      <div style={{ display: 'flex', justifyContent: 'center', margin: '12px 0' }}>
        <NeeluVideo anim="wave" size={120} />
      </div>
      <p style={{ minHeight: 48, fontSize: 17, fontWeight: 600, margin: '0 0 10px' }}>{transcript || 'Say: “Two rotis, a bowl of dal and some bhindi…”'}</p>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
        {chips.map((c, i) => <span key={i} className="chip on" style={{ height: 32 }}>{c}</span>)}
      </div>
      <div style={{ display: 'flex', gap: 10 }}>
        <button className="btn secondary" style={{ flex: 1 }} onClick={() => listening ? recRef.current?.stop() : recRef.current?.start()}>{listening ? '■ Stop' : '🎙️ Listen'}</button>
        <button className="btn" style={{ flex: 1 }} disabled={!transcript || parsing} onClick={async () => { setParsing(true); await onParse(transcript); setParsing(false); }}>{parsing ? 'Reading…' : 'Add items'}</button>
      </div>
    </div>
  );
}

export function FoodItem() {
  const [params] = useSearchParams();
  const name = params.get('name') || 'Paneer tikka';
  const slot = params.get('slot') || 'dinner';
  const [food, setFood] = useState(null);
  const [unit, setUnit] = useState('Bowl');
  const [qty, setQty] = useState(1);
  const [oil, setOil] = useState('normal');
  const nav = useNavigate();
  const { showSnack, refreshHome } = useStore();

  useEffect(() => {
    (async () => {
      const { items } = await get(`/foods?q=${encodeURIComponent(name)}`);
      setFood(items[0] || null);
    })();
  }, [name]);

  if (!food) return <div className="page" style={{ justifyContent: 'center' }}><div className="spin" style={{ width: 36, height: 36, border: '3px solid var(--line2)', borderTopColor: 'var(--teal)', borderRadius: '50%' }} /></div>;

  const oilMult = OIL[oil];
  const kcal = Math.round(food.kcal * qty * oilMult);
  const base = { kcal: Math.round(food.kcal * qty * oilMult), protein_g: food.protein_g * qty, carbs_g: food.carbs_g * qty, fat_g: food.fat_g * qty * (oil === 'more' ? 1.25 : oil === 'less' ? 0.75 : 1) };

  const add = async () => {
    const r = await post('/meals', {
      slot, source: 'search', name: food.name,
      items: [{ name: food.name, kcal: base.kcal, protein_g: Math.round(base.protein_g), carbs_g: Math.round(base.carbs_g), fat_g: Math.round(base.fat_g), portion: `${qty} ${unit.toLowerCase()}${qty > 1 ? 's' : ''}` }],
    });
    window.__lastLog = r;
    refreshHome();
    nav('/logged', { replace: true });
  };

  return (
    <div className="page" style={{ background: 'var(--chandan)' }}>
      <div style={{ background: 'var(--teal)', color: '#fff', padding: '14px 16px 18px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <Link to="/search" aria-label="Back" style={{ width: 40, height: 40, borderRadius: '50%', background: 'rgba(255,255,255,.14)', display: 'flex', alignItems: 'center', justifyContent: 'center', textDecoration: 'none' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.6" strokeLinecap="round"><path d="M15 18l-6-6 6-6" /></svg>
        </Link>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}><VegMark kind={food.veg} /><strong style={{ fontSize: 18 }}>{food.name}</strong></div>
          <span className="small" style={{ opacity: 0.8 }}>{food.verified ? 'Verified · home-style portion' : 'AI estimate'}</span>
        </div>
        <FoodIcon kind={kindForFood(food.name)} size={44} />
      </div>

      <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div className="card">
          <span className="small" style={{ fontWeight: 700, color: 'var(--sub)' }}>UNIT</span>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            {['Bowl', 'Plate', 'Spoon', 'Grams'].map((u) => (
              <button key={u} className={`chip ${unit === u ? 'on' : ''}`} onClick={() => setUnit(u)}>{u}</button>
            ))}
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button onClick={() => setQty(Math.max(0.5, Math.round((qty - 0.5) * 2) / 2))} style={stepperBtn}>−</button>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <strong style={{ fontFamily: 'var(--display)', fontSize: 30 }}>{qty} {unit.toLowerCase()}{qty > 1 ? 's' : ''}</strong>
            {unit === 'Bowl' && <div className="small muted">≈ {Math.round(150 * qty)} g</div>}
          </div>
          <button onClick={() => setQty(Math.min(5, qty + 0.5))} style={stepperBtn}>+</button>
        </div>

        <div className="card">
          <span className="small" style={{ fontWeight: 700, color: 'var(--sub)' }}>OIL LEVEL</span>
          <div style={{ marginTop: 8 }}><Segmented options={[{ value: 'less', label: 'Less' }, { value: 'normal', label: 'Normal' }, { value: 'more', label: 'More' }]} value={oil} onChange={setOil} /></div>
        </div>

        <div style={{ background: 'var(--mint2)', borderRadius: 20, padding: '12px 16px', display: 'flex', alignItems: 'center' }}>
          <div style={{ flex: 1 }}>
            <strong style={{ fontSize: 16 }}>{food.name}</strong>
            <div className="small muted">{Math.round(base.protein_g)} g protein · {Math.round(base.carbs_g)} g carbs · {Math.round(base.fat_g)} g fat</div>
          </div>
          <strong style={{ fontFamily: 'var(--display)', fontSize: 26, color: 'var(--teal)' }}>{kcal} kcal</strong>
        </div>
      </div>

      <div style={{ marginTop: 'auto', padding: '0 16px 22px', display: 'flex', gap: 10, alignItems: 'center' }}>
        <button className="btn secondary" style={{ width: 120 }} onClick={() => showSnack(`Saved ${food.name} to Meri thali`)}>♡ Save</button>
        <button className="btn" style={{ flex: 1 }} onClick={add}>Add to {slot} · {kcal} kcal</button>
      </div>
    </div>
  );
}

const stepperBtn = { width: 48, height: 48, borderRadius: '50%', border: 'none', background: 'var(--mint2)', color: 'var(--teal)', fontSize: 22, fontWeight: 800, cursor: 'pointer' };

function healthScore(meal) {
  const fat = meal.fat_g || 0;
  let score = 92 - Math.max(0, fat - 18) * 1.1 - (meal.kcal > 650 ? 12 : 0) + Math.max(0, (meal.protein_g || 0) - 12) * 0.5;
  score = Math.max(40, Math.min(96, Math.round(score)));
  return score;
}

export function MealDetail() {
  const { id } = useParams();
  const [meal, setMeal] = useState(null);
  const nav = useNavigate();
  const { refreshHome, showSnack } = useStore();

  useEffect(() => { (async () => { try { setMeal(await get(`/meals/${id}`)); } catch { nav('/home'); } })(); }, [id, nav]);

  if (!meal) return <div className="page" style={{ justifyContent: 'center' }}><div className="spin" style={{ width: 36, height: 36, border: '3px solid var(--line2)', borderTopColor: 'var(--teal)', borderRadius: '50%' }} /></div>;
  const score = healthScore(meal);
  const scoreColor = score >= 70 ? 'var(--green)' : 'var(--amber)';

  return (
    <div className="page" style={{ background: 'var(--chandan)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 16px' }}>
        <Link to="/home" aria-label="Back" style={{ width: 40, height: 40, borderRadius: '50%', background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid var(--line)' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--ink)" strokeWidth="2.6" strokeLinecap="round"><path d="M15 18l-6-6 6-6" /></svg>
        </Link>
        <strong style={{ fontSize: 16 }}>{meal.slot[0].toUpperCase() + meal.slot.slice(1)} · {meal.time}</strong>
        <button aria-label="Delete meal" onClick={async () => { await del(`/meals/${meal.id}`); refreshHome(); showSnack('Meal removed'); nav('/home', { replace: true }); }}
          style={{ width: 40, height: 40, borderRadius: '50%', background: '#fff', border: '1px solid var(--line)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="var(--red)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M5 7 H19 M10 7 V4 H14 V7 M7 7 L8 20 H16 L17 7" /></svg>
        </button>
      </div>

      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12, paddingBottom: 24 }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ width: 110, height: 110, borderRadius: '50%', overflow: 'hidden', background: '#3B2B21', border: '3px solid var(--gold)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
            {meal.scan_id ? (
              <img src={meal.scan?.image_path ? `${API}/uploads/${meal.scan.image_path}` : undefined} alt="Meal" style={{ width: '100%', height: '100%', objectFit: 'cover' }} onError={(e) => { e.currentTarget.outerHTML = '<div style=\'width:80px\'></div>'; }} />
            ) : <FoodIcon kind={kindForFood(meal.name)} size={90} />}
          </div>
          <div style={{ flex: 1 }}>
            <strong style={{ fontSize: 19 }}>{meal.name}</strong>
            <div style={{ fontFamily: 'var(--display)', fontSize: 40, fontWeight: 800, color: 'var(--teal)', lineHeight: 1.1 }}>{meal.kcal} kcal</div>
            <span className="pill" style={{ background: '#fff', border: `1.5px solid ${scoreColor}`, color: scoreColor }}>{score} · {score >= 78 ? 'Good' : 'Okay'}</span>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', gap: 6 }}>
          {[['Carbs', meal.carbs_g, 'var(--genda)'], ['Protein', meal.protein_g, 'var(--neel)'], ['Fat', meal.fat_g, 'var(--pankh)']].map(([k, v, c]) => (
            <div key={k} style={{ flex: 1, textAlign: 'center' }}>
              <div style={{ width: 10, height: 34, borderRadius: 5, background: c, margin: '0 auto 6px', opacity: 0.85 }} />
              <strong className="num">{Math.round(v)} g</strong>
              <div className="small muted">{k}</div>
            </div>
          ))}
        </div>

        <div className="rowlist">
          {meal.items.map((it, i) => (
            <button key={i} className="rowitem" style={{ width: '100%', background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'inherit', borderTop: i ? '1px solid #F1ECE1' : 'none' }}
              onClick={() => nav(`/food?name=${encodeURIComponent(it.name)}&slot=${meal.slot}`)}>
              <FoodIcon kind={kindForFood(it.name)} size={38} />
              <span className="grow" style={{ textAlign: 'left' }}>
                <strong style={{ fontSize: 15 }}>{it.name}</strong>
                <span className="small muted">{it.portion}</span>
              </span>
              <strong className="num" style={{ fontSize: 14.5 }}>{it.kcal} kcal</strong>
            </button>
          ))}
        </div>

        <div style={{ background: 'var(--mint)', borderRadius: 18, padding: '12px 14px', display: 'flex', gap: 10, alignItems: 'center' }}>
          <NeeluImg mood="happy" size={48} />
          <span style={{ flex: 1, fontSize: 14.5, fontWeight: 600 }}>Tip: add a bowl of curd for +6 g protein.</span>
        </div>

        <button className="btn" onClick={() => { refreshHome(); nav('/home', { replace: true }); }}>Done</button>
      </div>
    </div>
  );
}
