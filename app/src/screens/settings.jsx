import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { get, post } from '../api.js';
import { TopBar, Screen, Navbar, Toggle, NeeluImg, NeeluVideo, Coin, Diya, Btn } from '../components/ui.jsx';
import { BadgeModal } from './progress.jsx';

function timeAgo(iso) {
  const d = new Date(iso.replace(' ', 'T'));
  const mins = Math.max(1, Math.round((Date.now() - d) / 60000));
  if (mins < 60) return `${mins} min ago`;
  if (mins < 1440) return `${Math.round(mins / 60)} h ago`;
  return 'Yesterday';
}

export function Inbox() {
  const [items, setItems] = useState(null);
  const nav = useNavigate();
  const { refreshHome } = useStore();
  useEffect(() => { (async () => setItems((await get('/notifications')).items))(); }, []);
  if (!items) return <div className="page" style={{ justifyContent: 'center' }}><div className="spin" style={{ width: 36, height: 36, border: '3px solid var(--line2)', borderTopColor: 'var(--teal)', borderRadius: '50%' }} /></div>;
  const today = items.filter((n) => n.today);
  const yesterday = items.filter((n) => !n.today);

  const Item = ({ n }) => (
    <button className="rowitem" style={{ width: '100%', background: 'none', border: 'none', borderTop: '1px solid #F1ECE1', cursor: 'pointer', fontFamily: 'inherit', opacity: n.read ? 0.65 : 1 }}
      onClick={async () => { await post('/notifications/read', { id: n.id }); refreshHome(); nav(n.type === 'badge' ? '/challenges' : n.type === 'missed_meal' ? '/add' : '/home'); }}>
      <span style={{ width: 38, height: 38, borderRadius: 12, background: 'var(--mint2)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        {n.type === 'missed_meal' ? '🍽️' : n.type === 'badge' ? '🏅' : n.type === 'streak' ? <Diya size={20} /> : '💧'}
      </span>
      <span className="grow" style={{ textAlign: 'left' }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <strong style={{ fontSize: 15 }}>{n.title}</strong>
          {!n.read && <span style={{ width: 8, height: 8, borderRadius: 4, background: 'var(--kumkum)' }} />}
        </span>
        <span className="small muted" style={{ display: 'block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{n.body}</span>
      </span>
      <span className="small muted" style={{ flexShrink: 0 }}>{timeAgo(n.created_at)}</span>
    </button>
  );

  return (
    <div className="page" style={{ background: 'var(--chandan)' }}>
      <TopBar back="/home" title="Notifications" right={<button className="btn textlike" style={{ fontSize: 13 }} onClick={async () => { await post('/notifications/read'); setItems((it) => it.map((x) => ({ ...x, read: true }))); refreshHome(); }}>Mark all read</button>} />
      {items.length === 0 ? (
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10, textAlign: 'center', padding: 24 }}>
          <NeeluVideo anim="sleep" size={160} />
          <h2 className="h2">All caught up</h2>
          <p className="muted">No new reminders. Neelu is napping.</p>
          <Link to="/reminders" className="btn secondary" style={{ textDecoration: 'none', width: 220 }}>Reminder settings</Link>
        </div>
      ) : (
        <div style={{ padding: '0 16px 24px', display: 'flex', flexDirection: 'column', gap: 10 }}>
          {today.length > 0 && <>
            <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '2px 4px' }}>TODAY</div>
            <div className="rowlist">{today.map((n) => <Item key={n.id} n={n} />)}</div>
          </>}
          {yesterday.length > 0 && <>
            <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '2px 4px' }}>YESTERDAY</div>
            <div className="rowlist">{yesterday.map((n) => <Item key={n.id} n={n} />)}</div>
          </>}
        </div>
      )}
    </div>
  );
}

export function Profile() {
  const { profile, home, showSnack } = useStore();
  const nav = useNavigate();
  if (!profile || !home) return <div className="page" style={{ justifyContent: 'center' }}><div className="spin" style={{ width: 36, height: 36, border: '3px solid var(--line2)', borderTopColor: 'var(--teal)', borderRadius: '50%' }} /></div>;
  const plan = profile.plan;

  const exportData = async () => {
    const [p, i, n] = await Promise.all([get('/profile'), get('/insights'), get('/notifications')]);
    const blob = new Blob([JSON.stringify({ profile: p, insights: i, notifications: n }, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'mor-data-export.json';
    a.click();
    showSnack('Data exported');
  };

  const Row = ({ label, value, to, onClick }) => (
    <button className="rowitem" style={{ width: '100%', background: 'none', border: 'none', borderTop: '1px solid #F1ECE1', cursor: 'pointer', fontFamily: 'inherit' }} onClick={() => (onClick ? onClick() : to && nav(to))}>
      <span className="grow" style={{ textAlign: 'left' }}><span className="small muted">{label}</span></span>
      <strong style={{ fontSize: 15 }}>{value}</strong>
      {to && <span className="small muted">›</span>}
    </button>
  );

  return (
    <div className="page" style={{ background: 'var(--chandan)', paddingBottom: 96 }}>
      <div style={{ background: 'var(--teal)', color: '#fff', padding: '20px 16px' }}>
        <strong style={{ fontFamily: 'var(--display)', fontSize: 24 }}>{profile.name || 'Guest'}</strong>
        <div className="small" style={{ opacity: 0.85, display: 'flex', gap: 10, marginTop: 4 }}>
          <span>🔥 {home.streak}-day streak</span><span><Coin size={14} /> {home.coins} coins</span>
        </div>
      </div>
      <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div className="card" style={{ border: '1.5px dashed var(--line2)', display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 24 }}>☁️</span>
          <span style={{ flex: 1, fontSize: 13.5, fontWeight: 600 }}>Your data lives on your local Mor server — backed up in <code>backend/data/mor.db</code>.</span>
        </div>

        <div>
          <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '4px 4px 6px' }}>YOUR PLAN</div>
          <div className="rowlist">
            <Row label="Goal" value={plan ? `${profile.weight_kg} → ${profile.target_weight_kg} kg` : '—'} to="/target" />
            <Row label="Daily target" value={plan ? `${plan.kcal_target.toLocaleString()} kcal` : '—'} to="/plan" />
            <Row label="Diet" value={profile.diet || '—'} to="/diet" />
          </div>
        </div>

        <div>
          <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '4px 4px 6px' }}>APP</div>
          <div className="rowlist">
            <Row label="Reminders" value={profile.reminders_on ? `${Object.keys(profile.meal_times).length} on` : 'Off'} to="/reminders" />
            <Row label="Units" value={profile.units === 'kg-ft' ? 'kg · ft' : profile.units} />
          </div>
        </div>

        <div>
          <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '4px 4px 6px' }}>DATA</div>
          <div className="rowlist">
            <Row label="Export data" value="Download" onClick={exportData} />
            <button className="rowitem" style={{ width: '100%', background: 'none', border: 'none', borderTop: '1px solid #F1ECE1', cursor: 'pointer', fontFamily: 'inherit', color: 'var(--red)', fontWeight: 700, justifyContent: 'center' }}
              onClick={async () => { if (confirm('Delete ALL your data? This cannot be undone.')) { await fetch('/api/reset'); showSnack('Data cleared'); nav('/splash'); } }}>
              Delete all data
            </button>
          </div>
        </div>

        <div className="small muted" style={{ textAlign: 'center' }}>Mor v0.1 · local lab build</div>
      </div>
      <Navbar active="profile" />
    </div>
  );
}

export function Reminders() {
  const { profile, saveProfile, refreshHome } = useStore();
  const [times, setTimes] = useState(profile?.meal_times || {});
  const nav = useNavigate();
  const off = !profile?.reminders_on;
  const slots = ['breakfast', 'lunch', 'snack', 'dinner'];

  return (
    <Screen bg="var(--chandan)">
      <TopBar back={() => { refreshHome(); nav(-1); }} title="Reminders" />
      {off && (
        <div style={{ background: 'var(--red-bg)', color: 'var(--red)', padding: '10px 16px', fontWeight: 700, fontSize: 14.5, display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ flex: 1 }}>Notifications are off</span>
          <button className="btn sm" style={{ width: 'auto', padding: '0 14px' }} onClick={async () => { await saveProfile({ reminders_on: true }); }}>Turn on</button>
        </div>
      )}
      <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 12, opacity: off ? 0.55 : 1 }}>
        <div>
          <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '4px 4px 6px' }}>MEALS</div>
          <div className="rowlist">
            {slots.map((s) => (
              <div className="rowitem" key={s}>
                <span className="grow"><strong style={{ fontSize: 15 }}>{s[0].toUpperCase() + s.slice(1)}</strong></span>
                <span className="chip">{(times[s] || '—')}</span>
                <Toggle on={Boolean(times[s])} onChange={async (v) => {
                  const next = v
                    ? { ...times, [s]: s === 'breakfast' ? '08:30' : s === 'lunch' ? '13:30' : s === 'snack' ? '17:00' : '21:00' }
                    : Object.fromEntries(Object.entries(times).filter(([k]) => k !== s));
                  setTimes(next);
                  await saveProfile({ meal_times: next });
                }} />
              </div>
            ))}
          </div>
        </div>
        <div>
          <div className="small" style={{ fontWeight: 700, color: 'var(--sub)', padding: '4px 4px 6px' }}>NUDGES</div>
          <div className="rowlist">
            {[['missed', 'Missed meal', 'Once, 90 min later'], ['streak_saver', 'Streak saver', 'At 10:00 PM'], ['water', 'Water', 'Every 2 hours'], ['weekly', 'Weekly report', 'Sundays']].map(([k, label, when]) => (
              <div className="rowitem" key={k}>
                <span className="grow"><strong style={{ fontSize: 15 }}>{label}</strong><span className="small muted" style={{ display: 'block' }}>{when}</span></span>
                <Toggle on={true} onChange={() => {}} />
              </div>
            ))}
          </div>
        </div>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <NeeluImg mood="sleepy" size={44} />
          <span className="small" style={{ flex: 1, fontWeight: 600 }}>Quiet hours: 10:30 PM – 7:00 AM</span>
          <span className="pill verified">Max 4/day</span>
        </div>
        {off && (
          <div style={{ display: 'flex', justifyContent: 'center' }}><NeeluVideo anim="hungry" size={140} /></div>
        )}
      </div>
    </Screen>
  );
}
