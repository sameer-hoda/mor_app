import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { get, post } from '../api.js';
import { Navbar, TopBar, Screen, Segmented, NeeluImg, NeeluVideo, Coin, Diya, Btn, Sheet, Field } from '../components/ui.jsx';

export function Insights() {
  const [range, setRange] = useState('week');
  const [data, setData] = useState(null);
  const [weightSheet, setWeightSheet] = useState(false);
  const { home, refreshHome } = useStore();
  const loggedDays = data?.logged_days || 0;

  useEffect(() => { (async () => setData(await get('/insights')))().catch(() => {}); }, []);
  const goal = data?.kcal_target || 2000;

  return (
    <div className="page" style={{ background: 'var(--chandan)', paddingBottom: 96 }}>
      <TopBar title="Insights" />
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ width: 200 }}><Segmented options={['week', 'month']} value={range} onChange={setRange} /></div>

        {!data || loggedDays === 0 ? (
          <div style={{ background: '#fff', border: '1px solid var(--line)', borderRadius: 22, padding: '26px 18px', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
            <NeeluVideo anim="think" size={150} />
            <h2 className="h2">Log {3 - loggedDays || 3} days to unlock</h2>
            <p className="muted" style={{ margin: 0 }}>Your weekly view appears after 3 logged days. {loggedDays ? `${loggedDays} of 3 so far.` : ''}</p>
            <div style={{ display: 'flex', gap: 4, marginTop: 6 }}>
              {[0, 1, 2].map((i) => (
                <span key={i} style={{ width: 54, height: 8, borderRadius: 4, background: i < loggedDays ? 'var(--teal)' : 'var(--line2)' }} />
              ))}
            </div>
            <Link to="/add" className="btn" style={{ textDecoration: 'none', marginTop: 10 }}>Log a meal</Link>
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 4px' }}>
              <strong style={{ fontSize: 17 }}>{data.days[0].date.slice(8)} – {data.days[6].date.slice(8)} {data.days[6].date.slice(0, 4)}</strong>
              <span className="small muted">Week</span>
            </div>

            <div className="card" style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
              <div>
                <div className="small muted" style={{ fontWeight: 600 }}>Daily average</div>
                <div style={{ fontFamily: 'var(--display)', fontSize: 38, fontWeight: 800 }}>{data.avg.toLocaleString()}</div>
                <div className="small muted">kcal/day</div>
              </div>
              <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                <span className={`pill ${data.on_track_days >= 5 ? 'realistic' : 'ambitious'}`}>{data.on_track_days}/7 on track</span>
              </div>
            </div>

            <div className="card">
              <strong style={{ fontSize: 15 }}>This week vs your {goal.toLocaleString()} kcal goal</strong>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 130, marginTop: 14 }}>
                {data.days.map((d) => (
                  <div key={d.date} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, height: '100%', justifyContent: 'flex-end' }}>
                    <span className="num" style={{ fontSize: 10.5, color: 'var(--sub)' }}>{d.kcal ? d.kcal : '–'}</span>
                    <div style={{ width: '100%', borderRadius: 6, background: d.kcal ? (d.kcal <= goal ? 'var(--teal)' : 'var(--genda)') : 'var(--line2)', height: `${d.kcal ? Math.min(100, (d.kcal / (goal * 1.25)) * 100) : 4}%` }} />
                    <span className="small muted">{d.weekday[0]}</span>
                  </div>
                ))}
              </div>
              <div style={{ borderTop: '2px dashed var(--line2)', margin: '10px 0 0', position: 'relative' }}>
                <span className="small muted" style={{ position: 'absolute', right: 0, top: -20 }}>goal {goal.toLocaleString()}</span>
              </div>
            </div>

            <button className="card" style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', textAlign: 'left', fontFamily: 'inherit', border: '1px solid var(--line)' }} onClick={() => setWeightSheet(true)}>
              <span style={{ fontSize: 26 }}>⚖️</span>
              <div style={{ flex: 1 }}>
                <strong className="num" style={{ fontSize: 22 }}>{data.weights.length ? data.weights[data.weights.length - 1].kg : home?.weight_kg} kg</strong>
                <div className="small muted">{data.weight_trend < 0 ? `▼ ${Math.abs(data.weight_trend)} kg since first log` : data.weight_trend > 0 ? `▲ ${data.weight_trend} kg` : 'Log your weight'}</div>
              </div>
              <span style={{ color: 'var(--teal)', fontWeight: 700 }}>Log ›</span>
            </button>

            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong style={{ fontSize: 15 }}>Protein</strong>
                <span className="num">{data.protein_today}/{data.protein_goal} g</span>
              </div>
              <div className="macro-bar" style={{ marginTop: 10 }}>
                <span className="track"><span className="fill" style={{ width: `${Math.min(100, (data.protein_today / data.protein_goal) * 100)}%`, background: 'var(--neel)' }} /></span>
              </div>
              <div className="small muted" style={{ marginTop: 6 }}>Lowest at breakfast — add curd or sprouts.</div>
            </div>
          </>
        )}
      </div>
      <Navbar active="insights" />
      {weightSheet && <WeightSheet onClose={() => { setWeightSheet(false); (async () => { setData(await get('/insights')); refreshHome(); })(); }} />}
    </div>
  );
}

function WeightSheet({ onClose }) {
  const { home, refreshHome } = useStore();
  const [kg, setKg] = useState(home?.weight_kg || 72);
  const [delta, setDelta] = useState(null);
  const save = async () => {
    const r = await post('/weight', { kg });
    refreshHome();
    onClose(r.delta);
  };
  return (
    <>
      <div className="overlay" />
      <div className="sheet">
        <div className="grab" />
        <h2 className="h2">Today's weight</h2>
        <p className="small muted" style={{ margin: '2px 0 10px' }}>Last: {home?.weight_kg} kg</p>
        <div style={{ background: '#fff', border: '1px solid var(--line)', borderRadius: 20, padding: 16, textAlign: 'center' }}>
          <div style={{ fontFamily: 'var(--display)', fontSize: 44, fontWeight: 800 }}>{Number(kg).toFixed(1)}<span className="small"> kg</span></div>
          <input type="range" min="35" max="150" step="0.1" value={kg} style={{ width: '100%', accentColor: 'var(--teal)' }} onChange={(e) => setKg(Number(e.target.value))} />
        </div>
        {delta !== null && <div className="pill realistic" style={{ marginTop: 10 }}>{delta === 0 ? 'Same as last time' : delta < 0 ? `▼ ${Math.abs(delta)} kg since last time` : `▲ ${delta} kg since last time`}</div>}
        <Btn style={{ marginTop: 14 }} onClick={save}>Save</Btn>
      </div>
    </>
  );
}

export function Weight() {
  const nav = useNavigate();
  return <WeightSheetWrapper />;
}
function WeightSheetWrapper() {
  const { refreshHome } = useStore();
  return (
    <Screen bg="var(--chandan)">
      <TopBar title="Log weight" back={() => {}} />
      <WeightSheet onClose={() => { refreshHome(); window.history.back(); }} />
    </Screen>
  );
}

export function Streak() {
  const [data, setData] = useState(null);
  const nav = useNavigate();
  useEffect(() => { (async () => setData(await get('/streak')))(); }, []);
  const now = new Date();
  const monthName = now.toLocaleDateString('en', { month: 'long' });
  return (
    <Screen bg="linear-gradient(180deg,#FFF3DC 0%,#FDEBC8 100%)">
      <TopBar back={() => {}} />
      <div style={{ textAlign: 'center' }}>
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-end', gap: 8 }}>
          <Diya lit size={34} />
          <NeeluVideo anim="flex" size={130} />
          <Diya lit size={34} />
        </div>
        <h1 className="h1" style={{ fontSize: 38 }}>{data?.streak || 0} days streak!</h1>
        <p className="muted" style={{ marginTop: 0 }}>Every logged day lights a diya.</p>
      </div>
      <div className="card" style={{ margin: '12px 16px' }}>
        <strong style={{ fontSize: 15 }}>{monthName} {now.getFullYear()}</strong>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 6, marginTop: 10 }}>
          {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => <span key={i} className="small muted" style={{ textAlign: 'center', fontWeight: 700 }}>{d}</span>)}
          {Array.from({ length: data?.first_weekday || 0 }).map((_, i) => <span key={`e${i}`} />)}
          {(data?.days || []).map((d, i) => (
            <span key={i} style={{ aspectRatio: '1', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12.5, fontWeight: 700,
              background: d.lit ? 'var(--gold)' : 'transparent', color: d.lit ? '#7A4B05' : d.future ? 'var(--line2)' : 'var(--sub)',
              border: d.today ? '2px solid var(--teal)' : 'none', opacity: d.future ? 0.4 : 1 }}>{i + 1}</span>
          ))}
        </div>
        <div className="small muted" style={{ marginTop: 8, textAlign: 'center' }}>Best: {data?.best || 0} days</div>
      </div>
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="card" style={{ background: '#fff', display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 22 }}>❄️</span>
          <span style={{ flex: 1, fontSize: 14.5, fontWeight: 600 }}>Streak freeze ×{data?.freezes || 2} — saves a missed day</span>
        </div>
        <button className="btn gold" onClick={() => { try { navigator.share?.({ title: 'Mor', text: `I'm on a ${data?.streak || 1}-day eating streak with Mor! 🦚` }); } catch {} }}>Share</button>
        <Btn badge={false} onClick={() => nav('/home')}>Keep it going</Btn>
      </div>
    </Screen>
  );
}

export function Challenges() {
  const [data, setData] = useState(null);
  const [badge, setBadge] = useState(null);
  const { showSnack } = useStore();
  const load = () => get('/challenges').then(setData).catch(() => {});
  useEffect(() => { load(); }, []);
  if (!data) return <div className="page" style={{ justifyContent: 'center' }}><div className="spin" style={{ width: 36, height: 36, border: '3px solid var(--line2)', borderTopColor: 'var(--teal)', borderRadius: '50%' }} /></div>;

  const Group = ({ title, sub, list }) => (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', padding: '6px 4px 8px' }}>
        <strong style={{ fontSize: 16 }}>{title}</strong><span className="small muted">{sub}</span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {list.map((c) => (
          <div key={c.key} className="card" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ width: 40, height: 40, borderRadius: 12, background: c.done ? 'var(--green-bg)' : 'var(--mint2)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>{c.done ? '✅' : '🎯'}</span>
            <div style={{ flex: 1 }}>
              <strong style={{ fontSize: 15 }}>{c.title}</strong>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div className="progressbar" style={{ flex: 1, height: 6 }}><div style={{ width: `${Math.min(100, (c.progress / c.target) * 100)}%`, background: c.done ? 'var(--panna)' : 'var(--teal)' }} /></div>
                <span className="small num" style={{ width: 52 }}>{Math.min(c.progress, c.target)}/{c.target}</span>
              </div>
            </div>
            {c.done && !c.claimed ? (
              <button className="btn sm" style={{ width: 'auto', padding: '0 14px' }} onClick={async () => {
                const r = await post('/challenges/claim', { key: c.key });
                showSnack(`+${r.awarded} coins!`);
                load();
              }}>+{c.coins}</button>
            ) : c.claimed ? <span className="pill realistic">Done</span> : <span className="small num" style={{ display: 'flex', alignItems: 'center', gap: 4 }}><Coin size={15} />{c.coins}</span>}
          </div>
        ))}
      </div>
    </>
  );

  return (
    <div className="page" style={{ background: 'var(--chandan)', paddingBottom: 96 }}>
      <div style={{ background: 'var(--teal)', color: '#fff', padding: '18px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ flex: 1 }}>
          <strong style={{ fontFamily: 'var(--display)', fontSize: 24 }}>Challenges</strong>
          <div className="small" style={{ opacity: 0.85 }}>Small wins every day</div>
        </div>
        <span className="pill" style={{ background: 'var(--gold)', color: '#7A4B05', fontSize: 14 }}><Coin /> {data.coins} coins</span>
      </div>
      <div style={{ padding: '8px 16px', display: 'flex', flexDirection: 'column' }}>
        <Group title="Today" sub="2 of 4 left" list={data.challenges.filter((c) => c.scope === 'today')} />
        <Group title="This week" sub="resets Sunday" list={data.challenges.filter((c) => c.scope === 'week')} />
        <button className="card" style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', fontFamily: 'inherit', border: '1px solid var(--line)', textAlign: 'left' }} onClick={() => setBadge('protein_pro')}>
          <span style={{ fontSize: 26 }}>🏅</span>
          <div style={{ flex: 1 }}><strong style={{ fontSize: 15 }}>Badges</strong><div className="small muted">{data.badges_unlocked} unlocked</div></div>
          <span style={{ color: 'var(--teal)', fontWeight: 700 }}>View ›</span>
        </button>
      </div>
      <Navbar active="challenges" />
      {badge && <BadgeModal badgeKey={badge} onClose={() => setBadge(null)} />}
    </div>
  );
}

export function BadgeModal({ badgeKey = 'protein_pro', onClose, standalone = false }) {
  const { showSnack } = useStore();
  const [awarded, setAwarded] = useState(false);
  const names = { protein_pro: 'Protein Pro' };
  return (
    <>
      {!standalone && <div className="overlay" onClick={onClose} />}
      <div className="dialog" style={{ top: 200, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
        <NeeluVideo anim="dance" size={130} />
        <div style={{ width: 84, height: 84, background: 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center', clipPath: 'polygon(50% 0,93% 25%,93% 75%,50% 100%,7% 75%,7% 25%)' }}>
          <span style={{ fontSize: 34 }}>🏅</span>
        </div>
        <h2 className="h2">{names[badgeKey] || 'Protein Pro'}</h2>
        <p className="small muted" style={{ margin: 0 }}>Protein goal, 3 days in a row.</p>
        <span className="pill gold" style={{ fontSize: 14 }}>+50 coins</span>
        <div style={{ display: 'flex', gap: 10, marginTop: 8, width: '100%' }}>
          <button className="btn secondary" style={{ flex: 1 }} onClick={() => { try { navigator.share?.({ title: 'Mor', text: 'I earned the Protein Pro badge! 🦚🏅' }); } catch {} }}>Share</button>
          <button className="btn" style={{ flex: 1 }} disabled={awarded} onClick={async () => {
            const r = await post('/badges/award', { key: badgeKey });
            setAwarded(true); showSnack(`Badge collected · +${r.awarded} coins`);
            setTimeout(onClose, 700);
          }}>{awarded ? 'Collected' : 'Collect'}</button>
        </div>
      </div>
    </>
  );
}
