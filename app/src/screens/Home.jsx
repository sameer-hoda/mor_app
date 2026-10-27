import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { Navbar, Ring, MacroBars, FeatherEye, Diya, Coin, NeeluImg, NeeluVideo, Segmented } from '../components/ui.jsx';
import FoodIcon, { kindForFood } from '../components/FoodIcon.jsx';
import { post, get } from '../api.js';

function Scene({ night = false, children }) {
  return (
    <div style={{ position: 'relative', height: 330, overflow: 'hidden' }}>
      <svg width="360" height="330" viewBox="0 0 360 330" style={{ position: 'absolute', inset: 0 }}>
        <defs>
          <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor={night ? '#1E2D4F' : '#CFE7E1'} />
            <stop offset="1" stopColor={night ? '#2A3B5E' : '#EAF5F1'} />
          </linearGradient>
        </defs>
        <rect width="360" height="330" fill="url(#sky)" />
        {night ? (
          <>
            <circle cx="290" cy="52" r="16" fill="#F5F0E4" />
            <circle cx="284" cy="48" r="14" fill="url(#sky)" opacity="0.9" />
            <circle cx="60" cy="60" r="2" fill="#F5F0E4" /><circle cx="120" cy="40" r="1.6" fill="#F5F0E4" />
            <circle cx="220" cy="70" r="1.6" fill="#F5F0E4" /><circle cx="330" cy="110" r="1.6" fill="#F5F0E4" />
          </>
        ) : (
          <>
            <circle cx="300" cy="56" r="22" fill="#F2B544" opacity="0.85" />
            <circle cx="300" cy="56" r="14" fill="#F5D67F" />
          </>
        )}
        <path d="M60 330 V150 q0 -14 14 -14 h212 q14 0 14 14 V330" fill={night ? '#2A3B5E' : '#EAF5F1'} stroke={night ? '#3B4F79' : '#BFD9CF'} strokeWidth="2" />
        <path d="M84 330 V168 q0 -8 8 -8 h176 q8 0 8 8 V330" fill="none" stroke={night ? '#3B4F79' : '#BFD9CF'} strokeWidth="1.5" />
        <path d="M52 136 Q60 118 90 116 h180 q30 2 38 20 l8 8 H44 z" fill={night ? '#3B4F79' : '#DDEDE6'} />
        <path d="M120 116 q4 -18 8 -22 h104 q4 4 8 22 z" fill={night ? '#3B4F79' : '#DDEDE6'} />
        <path d="M0 296 h360 v10 q-60 6 -120 2 t-120 0 -120 -2 z" fill={night ? '#3B2F46' : '#E3C394'} />
        <path d="M0 306 h360 v24 H0 z" fill={night ? '#2E2438' : '#D9B47F'} />
      </svg>
      {children}
    </div>
  );
}

const BANNER = {
  firstday: { bg: 'var(--mint2)', text: 'Log your first meal', actions: [{ label: 'Scan', to: '/camera' }, { label: 'Search', to: '/search' }, { label: 'Speak', to: '/search' }] },
  missed: { bg: 'var(--red-bg)', text: 'Missed lunch?', actions: [{ label: 'Log lunch', to: '/add' }, { label: 'I skipped it', to: null }] },
  over: { bg: 'var(--amber-bg)', text: "A bit over today. That's okay. Tomorrow is a fresh start.", actions: [] },
  goal: { bg: 'var(--green-bg)', text: 'Goal reached! +40 coins', actions: [{ label: 'See coins', to: '/challenges' }] },
  night: { bg: 'var(--mint2)', text: 'Day done. Well eaten.', actions: [{ label: 'See insights', to: '/insights' }] },
};

export function Home() {
  const { home, profile, refreshHome, refreshProfile, showSnack } = useStore();
  const [sheet, setSheet] = useState(false);
  const nav = useNavigate();
  if (!home || !profile) return <div className="page" style={{ justifyContent: 'center', alignItems: 'center' }}><div className="spin"><FeatherEye size={40} /></div></div>;

  const night = home.state === 'night';
  const banner = BANNER[home.state];
  const eatenPct = Math.min(1, home.kcal_eaten / home.kcal_target);
  const over = home.kcal_eaten > home.kcal_target;
  const mood = night ? 'sleepy' : home.state === 'goal' ? 'joy' : home.state === 'over' ? 'thinking' : home.state === 'missed' ? 'hungry' : 'happy';

  const addWater = async () => {
    const j = await post('/water', { delta: 1 });
    showSnack(`Water logged · ${j.glasses}/8`);
    refreshHome();
  };

  return (
    <div className="page" style={{ background: 'var(--chandan)', paddingBottom: 96 }}>
      <Scene night={night}>
        <div style={{ position: 'absolute', top: 12, left: 16, right: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <strong style={{ color: night ? '#fff' : 'var(--ink)', fontSize: 16 }}>{home.greeting}</strong>
          <Link to="/inbox" style={{ width: 40, height: 40, borderRadius: '50%', background: night ? 'rgba(255,255,255,.16)' : '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 8px rgba(0,0,0,.1)', position: 'relative' }}>
            🔔
            {home.unread > 0 && <span style={{ position: 'absolute', top: 6, right: 6, width: 9, height: 9, borderRadius: 5, background: 'var(--kumkum)', border: '1.5px solid #fff' }} />}
          </Link>
        </div>
        <div style={{ position: 'absolute', bottom: 12, left: 0, right: 0, display: 'flex', justifyContent: 'center' }}>
          <NeeluVideo key={mood} anim={night ? 'sleep' : home.state === 'over' ? 'shrug' : home.state === 'goal' ? 'dance' : home.state === 'missed' ? 'hungry' : 'wave'} size={215} />
        </div>
      </Scene>

      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12, marginTop: -18 }}>
        {banner && (
          <div style={{ background: banner.bg, borderRadius: 18, padding: '10px 14px', display: 'flex', alignItems: 'center', gap: 10, fontWeight: 600, fontSize: 14.5 }}>
            <span style={{ flex: 1 }}>{banner.text}</span>
            {banner.actions.map((a) => (
              <button key={a.label} className="btn sm" style={{ width: 'auto', padding: '0 14px', height: 36, fontSize: 13.5, background: '#fff', color: 'var(--teal)' }}
                onClick={() => a.to ? nav(a.to) : showSnack('Noted. Neelu will not nag.')}>{a.label}</button>
            ))}
          </div>
        )}

        <Link to="/streak" style={{ textDecoration: 'none', color: 'inherit' }}>
          <div style={{ background: '#FFF3DC', borderRadius: 20, padding: '10px 16px', display: 'flex', alignItems: 'center', gap: 10 }}>
            <Diya lit={home.streak > 0} size={26} />
            <strong style={{ fontFamily: 'var(--display)', fontSize: 24 }}>{home.streak}</strong>
            <span style={{ flex: 1, fontSize: 14, color: 'var(--amber)', fontWeight: 600 }}>day streak</span>
            <div style={{ display: 'flex', gap: 3 }}>
              {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
                <span key={i} style={{ width: 18, height: 18, borderRadius: '50%', fontSize: 9.5, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  background: i < Math.min(7, home.streak) ? 'var(--gold)' : 'rgba(242,169,59,0.25)', color: i < Math.min(7, home.streak) ? '#7A4B05' : '#C98A1E' }}>{d}</span>
              ))}
            </div>
          </div>
        </Link>

        <div className="card" style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
          <div style={{ flex: 1 }}>
            <div className="small" style={{ fontWeight: 700, color: 'var(--sub)' }}>{over ? 'KCAL OVER' : 'KCAL LEFT'}</div>
            <div style={{ fontFamily: 'var(--display)', fontSize: 44, fontWeight: 800, color: over ? 'var(--amber)' : 'var(--teal)', lineHeight: 1.05 }}>
              {(over ? home.kcal_eaten - home.kcal_target : home.kcal_left).toLocaleString()}
            </div>
            <div className="small muted" style={{ marginBottom: 10 }}>of {home.kcal_target.toLocaleString()} kcal · {home.kcal_eaten} eaten</div>
            <MacroBars eaten={home.macros.eaten} goal={home.macros.goal} />
          </div>
          <button onClick={() => setSheet(true)} aria-label="Log a meal" style={{ border: 'none', background: 'none', cursor: 'pointer', flexShrink: 0 }}>
            <Ring progress={eatenPct} size={100} color={over ? 'var(--genda)' : 'var(--teal)'}>
              <div style={{ width: 58, height: 58, borderRadius: '50%', background: 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <FeatherEye size={40} />
              </div>
            </Ring>
          </button>
        </div>

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '2px 4px 8px' }}>
            <strong style={{ fontSize: 16 }}>Today's meals</strong>
            <span className="small muted">{home.kcal_eaten} kcal today</span>
          </div>
          <div className="rowlist">
            {['breakfast', 'lunch', 'snack', 'dinner'].map((slot) => {
              const m = home.meals[slot];
              const cap = slot[0].toUpperCase() + slot.slice(1);
              if (m.logged) return (
                <Link key={slot} to={`/meal/${m.id}`} className="rowitem" style={{ textDecoration: 'none', color: 'inherit' }}>
                  <div style={{ display: 'flex' }}>
                    {(m.items || []).slice(0, 4).map((it, i) => (
                      <span key={i} style={{ marginLeft: i ? -16 : 0, borderRadius: '50%', border: '2px solid #fff', overflow: 'hidden', width: 42, height: 42, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#fff' }}>
                        <FoodIcon kind={kindForFood(it.name)} size={36} />
                      </span>
                    ))}
                  </div>
                  <span className="grow">
                    <strong style={{ fontSize: 15 }}>{cap}</strong>
                    <span className="small muted" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{(m.items || []).map((i) => i.name).join(', ')}</span>
                  </span>
                  <strong className="num" style={{ fontSize: 15 }}>{m.kcal} kcal</strong>
                </Link>
              );
              return (
                <button key={slot} className="rowitem" style={{ width: '100%', background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'inherit' }} onClick={() => nav('/add')}>
                  <span style={{ width: 42, height: 42, borderRadius: '50%', border: `2px dashed ${home.state === 'missed' && slot === 'lunch' ? 'var(--kumkum)' : 'var(--line2)'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--sub)' }}>+</span>
                  <span className="grow">
                    <strong style={{ fontSize: 15 }}>{cap}</strong>
                    <span className="small" style={{ color: home.state === 'missed' && slot === 'lunch' ? 'var(--red)' : 'var(--sub)' }}>
                      {home.state === 'missed' && slot === 'lunch' ? 'Not logged' : `Opens ${m.time}`}
                    </span>
                  </span>
                  <span style={{ width: 30, height: 30, borderRadius: '50%', background: 'var(--mint2)', color: 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800 }}>+</span>
                </button>
              );
            })}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 22 }}>💧</span>
            <div style={{ flex: 1 }}>
              <strong className="num">{home.water}/8</strong>
              <div className="small muted">glasses</div>
            </div>
            <button onClick={addWater} style={{ width: 34, height: 34, borderRadius: '50%', border: 'none', background: 'var(--mint2)', color: 'var(--teal)', fontSize: 18, fontWeight: 800, cursor: 'pointer' }}>+</button>
          </div>
          <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 22 }}>👟</span>
            <div>
              <strong className="num">{home.steps.toLocaleString()}</strong>
              <div className="small muted">steps today</div>
            </div>
          </div>
        </div>
      </div>

      <Navbar active="home" />
      {sheet && <AddSheet onClose={() => setSheet(false)} />}
    </div>
  );
}

export function AddSheet({ onClose }) {
  const { home, refreshHome, showSnack } = useStore();
  const [slot, setSlot] = useState(
    ['breakfast', 'lunch', 'snack', 'dinner'].find((s) => !home?.meals[s].logged) || 'snack'
  );
  const nav = useNavigate();
  const options = [
    { label: '📷  Scan meal', to: `/camera?slot=${slot}` },
    { label: '🔍  Search food', to: `/search?slot=${slot}` },
    { label: '🎙️  Speak', to: `/search?slot=${slot}&voice=1` },
    { label: '🖼️  Gallery', to: `/camera?slot=${slot}&gallery=1` },
  ];
  return (
    <>
      <div className="overlay" onClick={onClose} />
      <div className="sheet" style={{ paddingBottom: 26 }}>
        <div className="grab" />
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
          <NeeluVideo anim="peek" size={54} />
          <strong style={{ fontFamily: 'var(--display)', fontSize: 20 }}>Log a meal</strong>
        </div>
        <Segmented tall options={['breakfast', 'lunch', 'snack', 'dinner']} value={slot} onChange={setSlot} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 9, marginTop: 14 }}>
          {options.map((o) => (
            <button key={o.label} className="opt" style={{ minHeight: 56 }} onClick={() => nav(o.to)}>{o.label}</button>
          ))}
          <button className="opt" style={{ minHeight: 56 }} onClick={async () => {
            const r = await post('/meals', { slot, source: 'quick', name: 'Quick add', items: [{ name: 'Snack', kcal: 150, portion: '1 serving' }] });
            await refreshHome(); showSnack(`Logged 150 kcal · +${r.coins_awarded} coins`);
            onClose();
          }}>⚡  Quick add · 150 kcal</button>
        </div>
      </div>
    </>
  );
}
