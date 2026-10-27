import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { Btn, Segmented, Field, StepDots, Screen, TopBar, FeatherEye, NeeluImg, NeeluVideo, Toggle, Sheet, VegMark } from '../components/ui.jsx';
import FoodIcon, { kindForFood } from '../components/FoodIcon.jsx';
import { post } from '../api.js';

const STEP_BG = 'linear-gradient(180deg, #F8F5EE 0%, #F1ECE1 100%)';

function Step({ step, title, sub, children, next, nextDisabled, onBack, action, actionLabel = 'Continue' }) {
  const nav = useNavigate();
  return (
    <Screen bg={STEP_BG}>
      <TopBar back={onBack !== false ? -1 : undefined} right={<StepDots step={step} />} />
      <div style={{ padding: '26px 20px 8px' }}>
        <div className="small" style={{ color: 'var(--sub)', fontWeight: 600, marginBottom: 4 }}>{sub}</div>
        <h1 className="h1">{title}</h1>
      </div>
      <div style={{ flex: 1, padding: '14px 20px', overflowY: 'auto' }}>{children}</div>
      <div style={{ padding: '10px 20px 26px' }}>
        <Btn disabled={nextDisabled} onClick={action || (() => nav(next))}>{actionLabel}</Btn>
      </div>
    </Screen>
  );
}

export function Splash() {
  const nav = useNavigate();
  return (
    <div className="page" style={{ background: 'var(--teal)', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} onClick={() => nav('/welcome')}>
      <div style={{ width: 150, height: 150, borderRadius: '50%', border: '5px solid var(--gold)', padding: 12, background: 'rgba(255,255,255,.08)' }}>
        <NeeluVideo anim="splash" size={124} />
      </div>
      <h1 style={{ margin: '18px 0 4px', fontFamily: 'var(--display)', fontWeight: 800, fontSize: 44, color: '#fff' }}>Mor</h1>
      <p style={{ color: '#CFE7E1', fontSize: 15, margin: 0 }}>Eat well, the Indian way.</p>
      <div style={{ marginTop: 40 }}><StepDots step={1} total={3} /></div>
    </div>
  );
}

export function Welcome() {
  const nav = useNavigate();
  return (
    <Screen>
      <div style={{ position: 'relative', height: 420, background: 'linear-gradient(180deg,#CFE7E1 0%,#EAF5F1 100%)' }}>
        <div style={{ position: 'absolute', left: 20, top: 40, padding: '10px 16px', background: '#fff', borderRadius: 18, boxShadow: '0 4px 14px rgba(0,0,0,.08)', fontSize: 14, fontWeight: 700, display: 'flex', gap: 8, alignItems: 'center' }}>
          <FoodIcon kind="thali" size={24} /> 590 kcal
        </div>
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, display: 'flex', justifyContent: 'center' }}>
          <NeeluVideo anim="wave" size={260} style={{ marginBottom: -10 }} />
        </div>
        <svg width="360" height="60" viewBox="0 0 360 60" style={{ position: 'absolute', bottom: -1 }}>
          <path d="M0 40 Q90 20 180 34 T360 30 V60 H0 Z" fill="var(--chandan)" />
        </svg>
      </div>
      <div style={{ padding: '6px 24px', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <h1 className="h1" style={{ fontSize: 32 }}>Snap your meal. Know your calories.</h1>
        <p className="muted" style={{ fontSize: 16, lineHeight: 1.5 }}>Point the camera at your thali. Neelu reads it in seconds — English-first, Indian food, minimal words.</p>
        <div style={{ marginTop: 'auto', paddingBottom: 26, textAlign: 'center' }}>
          <Btn onClick={() => nav('/meet-neelu')}>Get started</Btn>
          <div className="small muted" style={{ marginTop: 10 }}>No sign-up needed</div>
        </div>
      </div>
    </Screen>
  );
}

export function MeetNeelu() {
  const nav = useNavigate();
  return (
    <Screen>
      <TopBar back />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 24, textAlign: 'center' }}>
        <div style={{ width: 210, height: 210, borderRadius: '50%', background: 'var(--mint2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <NeeluVideo anim="wave" size={190} />
        </div>
        <h1 className="h1" style={{ marginTop: 22, fontSize: 34 }}>Hi, I'm Neelu.</h1>
        <p className="muted" style={{ fontSize: 17 }}>I'm happiest when you eat well.<br />I'll count, you enjoy.</p>
      </div>
      <div style={{ padding: '0 20px 26px' }}>
        <Btn onClick={() => nav('/goal')}>Let's go</Btn>
      </div>
    </Screen>
  );
}

export function Goal() {
  const { profile, saveProfile } = useStore();
  const [v, setV] = useState(profile?.goal || '');
  const nav = useNavigate();
  const opts = [
    { v: 'lose', label: 'Lose weight', sub: 'Steady, sustainable pace' },
    { v: 'maintain', label: 'Maintain weight', sub: 'Stay where you are' },
    { v: 'gain', label: 'Gain weight', sub: 'Build up gently' },
    { v: 'healthy', label: 'Eat healthier', sub: 'Better choices, same weight' },
  ];
  return (
    <Step step={1} title="What's your goal?" sub="STEP 1 OF 6"
      next="/diet" nextDisabled={!v}
      onBack={() => nav('/meet-neelu')}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {opts.map((o) => (
          <button key={o.v} className={`opt ${v === o.v ? 'on' : ''}`} onClick={() => { setV(o.v); saveProfile({ goal: o.v }); }}>
            <span className="radio">{v === o.v && <FeatherEye size={22} />}</span>
            <span style={{ flex: 1 }}>{o.label}<small>{o.sub}</small></span>
          </button>
        ))}
      </div>
    </Step>
  );
}

export function Diet() {
  const { profile, saveProfile } = useStore();
  const [v, setV] = useState(profile?.diet || '');
  const nav = useNavigate();
  const opts = [
    { v: 'veg', label: 'Vegetarian', mark: 'veg' },
    { v: 'egg', label: 'Eggetarian', mark: 'egg' },
    { v: 'non', label: 'Non-veg', mark: 'non' },
    { v: 'vegan', label: 'Vegan', mark: 'veg' },
  ];
  return (
    <Step step={2} title="What do you eat?" sub="STEP 2 OF 6"
      next="/body" nextDisabled={!v}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {opts.map((o) => (
          <button key={o.v} className={`opt ${v === o.v ? 'on' : ''}`} style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 8 }}
            onClick={() => { setV(o.v); saveProfile({ diet: o.v }); }}>
            <VegMark kind={o.mark} />
            <span>{o.label}</span>
          </button>
        ))}
      </div>
    </Step>
  );
}

export function Body() {
  const { profile, saveProfile } = useStore();
  const [sex, setSex] = useState(profile?.sex || 'female');
  const [age, setAge] = useState(profile?.age || '');
  const [heightCm, setHeightCm] = useState(profile?.height_cm || 170);
  const [weight, setWeight] = useState(profile?.weight_kg || 72);
  const [unit, setUnit] = useState('ft');
  const nav = useNavigate();
  const ageNum = Number(age);
  const invalid = !age || ageNum < 18 || ageNum > 100;
  const ft = Math.floor(heightCm / 30.48);
  const inch = Math.round((heightCm / 2.54 - ft * 12));

  const go = () => { saveProfile({ sex, age: ageNum, height_cm: heightCm, weight_kg: weight }); nav('/target'); };
  return (
    <Step step={3} title="About you" sub="STEP 3 OF 6" nextDisabled={invalid || !heightCm || !weight}
      next="/target" action={go}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Segmented options={['Female', 'Male', 'Other']} value={sex === 'male' ? 'Male' : sex === 'female' ? 'Female' : 'Other'} tall
          onChange={(s) => setSex(s.toLowerCase())} />
        <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <span className="small" style={{ fontWeight: 600, color: 'var(--sub)' }}>Age</span>
          <Field inputMode="numeric" placeholder="28" value={age} error={invalid && age !== ''} onChange={(e) => setAge(e.target.value)} />
          {invalid && age !== '' && <span className="small" style={{ color: 'var(--kumkum)', fontWeight: 600 }}>You must be 18 or older.</span>}
        </label>
        <div style={{ display: 'flex', gap: 10 }}>
          <label style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
            <span className="small" style={{ fontWeight: 600, color: 'var(--sub)' }}>Height · <button className="btn textlike" style={{ fontSize: 12 }} onClick={() => setUnit(unit === 'ft' ? 'cm' : 'ft')}>{unit === 'ft' ? 'Use cm' : 'Use ft/in'}</button></span>
            {unit === 'cm'
              ? <Field inputMode="numeric" value={heightCm} onChange={(e) => setHeightCm(Number(e.target.value) || 0)} />
              : <div className="field" style={{ display: 'flex', alignItems: 'center', fontWeight: 600 }}>{ft}′ {inch}″</div>}
          </label>
          <label style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
            <span className="small" style={{ fontWeight: 600, color: 'var(--sub)' }}>Weight (kg)</span>
            <Field inputMode="decimal" value={weight} onChange={(e) => setWeight(Number(e.target.value) || 0)} />
          </label>
        </div>
      </div>
    </Step>
  );
}

export function Target() {
  const { profile, saveProfile } = useStore();
  const w = profile?.weight_kg || 72;
  const [tgt, setTgt] = useState(profile?.target_weight_kg || 66);
  const nav = useNavigate();
  const loss = w - tgt;
  const ambitious = loss > w * 0.1;
  const weeks = Math.max(1, Math.ceil(Math.abs(loss) / 0.5));
  const date = new Date(Date.now() + weeks * 7 * 86400000).toLocaleDateString('en', { day: 'numeric', month: 'short', year: 'numeric' });
  const milestone = Math.round((w - w * 0.1) * 10) / 10;
  return (
    <Step step={4} title="Your target weight" sub="STEP 4 OF 6"
      nextDisabled={!tgt || tgt < 35}
      next="/activity"
      action={async () => { await saveProfile({ target_weight_kg: tgt }); nav('/activity'); }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ background: '#fff', border: '1px solid var(--line)', borderRadius: 22, padding: '18px 16px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'var(--display)', fontSize: 56, fontWeight: 800 }}>{tgt}<span style={{ fontSize: 22, color: 'var(--sub)' }}> kg</span></div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 6 }}>
            {loss !== 0 && <span className={`pill ${ambitious ? 'ambitious' : 'realistic'}`}>{loss > 0 ? '−' : '+'}{Math.abs(Math.round(loss * 10) / 10)} kg · {ambitious ? 'Ambitious' : 'Realistic'}</span>}
            {loss !== 0 && <span className="pill verified">By {date}</span>}
          </div>
          <input type="range" min="35" max={Math.max(80, w + 10)} step="0.5" value={tgt} style={{ width: '100%', marginTop: 14, accentColor: 'var(--teal)' }}
            onChange={(e) => setTgt(Number(e.target.value))} />
          <div className="small muted">Slide to your target · now {w} kg</div>
        </div>
        {ambitious && (
          <div style={{ background: 'var(--amber-bg)', borderRadius: 18, padding: '12px 14px', fontSize: 14.5, color: 'var(--amber)', fontWeight: 600, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <span>Big jump. Go in steps?</span>
            <button className="btn sm" style={{ background: '#fff', color: 'var(--amber)', width: 'auto', alignSelf: 'flex-start', padding: '0 16px' }}
              onClick={() => setTgt(milestone)}>Start with {milestone} kg</button>
          </div>
        )}
      </div>
    </Step>
  );
}

export function Activity() {
  const { profile, saveProfile } = useStore();
  const [v, setV] = useState(profile?.activity || 'light');
  const nav = useNavigate();
  const opts = [
    { v: 'sit', label: 'Mostly sitting', bars: 1, sub: 'Desk job, little movement' },
    { v: 'light', label: 'Lightly active', bars: 2, sub: 'Walks, chores some days' },
    { v: 'active', label: 'Active', bars: 3, sub: 'Exercise most days' },
    { v: 'very', label: 'Very active', bars: 4, sub: 'Physical work or daily training' },
  ];
  return (
    <Step step={5} title="How active are you?" sub="STEP 5 OF 6"
      nextDisabled={!v}
      next="/meal-times">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {opts.map((o) => (
          <button key={o.v} className={`opt ${v === o.v ? 'on' : ''}`}
            onClick={() => { setV(o.v); saveProfile({ activity: o.v }); }}>
            <span className="radio">{v === o.v && <FeatherEye size={22} />}</span>
            <span style={{ flex: 1 }}>
              {o.label}<small>{o.sub}</small>
            </span>
            <span style={{ display: 'flex', gap: 2 }}>
              {[1, 2, 3, 4].map((i) => <span key={i} style={{ width: 5, height: 6 + i * 4, borderRadius: 3, background: i <= o.bars ? 'var(--teal)' : 'var(--line2)' }} />)}
            </span>
          </button>
        ))}
      </div>
    </Step>
  );
}

const DEFAULT_MEAL_TIMES = { breakfast: '08:30', lunch: '13:30', snack: '17:00', dinner: '21:00' };
const MEAL_FOOD = { breakfast: 'paratha', lunch: 'thali', snack: 'chai', dinner: 'dal' };
const QUICK_TIMES = { breakfast: ['07:30', '08:00', '08:30', '09:00'], lunch: ['12:30', '13:00', '13:30', '14:00'], snack: ['16:00', '16:30', '17:00', '18:00'], dinner: ['20:00', '20:30', '21:00', '21:30'] };

function fmtTime(t) {
  const [h, m] = t.split(':').map(Number);
  const ap = h >= 12 ? 'PM' : 'AM';
  const hh = h % 12 === 0 ? 12 : h % 12;
  return `${hh}:${String(m).padStart(2, '0')} ${ap}`;
}

export function MealTimes() {
  const { profile, saveProfile } = useStore();
  const [times, setTimes] = useState({ ...DEFAULT_MEAL_TIMES, ...(profile?.meal_times || {}) });
  const [on, setOn] = useState({ breakfast: true, lunch: true, snack: true, dinner: true });
  const [edit, setEdit] = useState(null);
  const nav = useNavigate();
  return (
    <Step step={6} title="Your meal times" sub="STEP 6 OF 6" onBack={() => nav('/activity')}
      action={async () => {
        const keep = Object.fromEntries(Object.entries(times).filter(([k]) => on[k]));
        await saveProfile({ meal_times: keep, complete: true });
        nav('/building');
      }} actionLabel="Build my plan">
      <div className="rowlist">
        {['breakfast', 'lunch', 'snack', 'dinner'].map((s) => (
          <div className="rowitem" key={s}>
            <FoodIcon kind={MEAL_FOOD[s]} size={34} />
            <span className="grow"><strong style={{ fontSize: 15 }}>{s[0].toUpperCase() + s.slice(1)}</strong></span>
            <button className="chip" onClick={() => setEdit(s)}>{fmtTime(times[s])}</button>
            <Toggle on={on[s]} onChange={(v) => setOn({ ...on, [s]: v })} />
          </div>
        ))}
      </div>
      <p className="small muted" style={{ padding: '8px 4px' }}>Neelu reminds you at these times. Max 4 a day, never at night.</p>
      {edit && (
        <Sheet onClose={() => setEdit(null)}>
          <h2 className="h2" style={{ textTransform: 'capitalize' }}>Edit {edit}</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, margin: '12px 0' }}>
            {QUICK_TIMES[edit].map((t) => (
              <button key={t} className={`chip ${times[edit] === t ? 'on' : ''}`} onClick={() => setTimes({ ...times, [edit]: t })}>{fmtTime(t)}</button>
            ))}
          </div>
          <input type="time" className="field" value={times[edit]} step={900}
            onChange={(e) => e.target.value && setTimes({ ...times, [edit]: e.target.value })} />
          <Btn style={{ marginTop: 12 }} onClick={() => setEdit(null)}>Save</Btn>
        </Sheet>
      )}
    </Step>
  );
}

export function Building() {
  const { profile } = useStore();
  const nav = useNavigate();
  const [p, setP] = useState(4);
  useEffect(() => {
    const iv = setInterval(() => setP((x) => (x >= 100 ? 100 : x + Math.ceil(Math.random() * 14))), 160);
    return () => clearInterval(iv);
  }, []);
  useEffect(() => { if (p >= 100) { const t = setTimeout(() => nav('/plan'), 500); return () => clearTimeout(t); } }, [p, nav]);
  return (
    <Screen bg={STEP_BG} style={{ alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
      <NeeluVideo anim="think" size={190} />
      <h1 className="h1" style={{ marginTop: 18 }}>Building your plan…</h1>
      <p className="muted">Balancing your {profile?.diet === 'non' ? 'meals' : profile?.diet || 'meals'} and {profile?.goal || 'health'} goal.</p>
      <div style={{ width: 240, marginTop: 26 }}>
        <div className="progressbar"><div style={{ width: `${Math.min(100, p)}%` }} /></div>
        <div className="small muted" style={{ marginTop: 8 }}>{Math.min(100, p)}%</div>
      </div>
      <button className="btn textlike" style={{ marginTop: 30 }} onClick={() => nav('/plan')}>Skip</button>
    </Screen>
  );
}

export function Plan() {
  const { profile } = useStore();
  const plan = profile?.plan;
  const nav = useNavigate();
  const target = plan?.kcal_target || 1850;
  return (
    <Screen bg="linear-gradient(180deg,#F8F5EE 0%,#FDEBC8 100%)" style={{ alignItems: 'center' }}>
      <div style={{ height: 44 }} />
      <NeeluVideo anim="dance" size={200} />
      <h1 className="h1" style={{ fontSize: 32, marginTop: 4 }}>Your plan is ready!</h1>
      <div style={{ background: '#fff', border: '1px solid var(--line)', borderRadius: 24, padding: '18px 22px', margin: '18px 20px 0', width: 'calc(100% - 40px)', textAlign: 'center' }}>
        <div className="small" style={{ fontWeight: 700, color: 'var(--sub)' }}>DAILY TARGET</div>
        <div style={{ fontFamily: 'var(--display)', fontSize: 54, fontWeight: 800, color: 'var(--teal)' }}>{target.toLocaleString()} <span style={{ fontSize: 20 }}>kcal</span></div>
        <div className="macro-bars" style={{ marginTop: 10 }}>
          <div className="macro-bar"><span className="lbl">Protein</span><span className="track"><span className="fill" style={{ width: '78%', background: 'var(--neel)' }} /></span><span className="val num">{plan?.protein_g || 75} g</span></div>
          <div className="macro-bar"><span className="lbl">Carbs</span><span className="track"><span className="fill" style={{ width: '92%', background: 'var(--genda)' }} /></span><span className="val num">{plan?.carbs_g || 250} g</span></div>
          <div className="macro-bar"><span className="lbl">Fat</span><span className="track"><span className="fill" style={{ width: '48%', background: 'var(--pankh)' }} /></span><span className="val num">{plan?.fat_g || 60} g</span></div>
        </div>
        {plan?.goal_line && <div className="pill verified" style={{ marginTop: 12 }}>{plan.goal_line} · by {plan.target_date}</div>}
      </div>
      <div style={{ marginTop: 'auto', width: '100%', padding: '20px 20px 26px' }}>
        <Btn onClick={() => nav('/notify')}>Start</Btn>
      </div>
    </Screen>
  );
}

export function Notify() {
  const nav = useNavigate();
  const [sys, setSys] = useState(false);
  return (
    <Screen bg={STEP_BG} style={{ alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
      <div style={{ position: 'relative' }}>
        <NeeluVideo anim="wave" size={200} />
        <span style={{ position: 'absolute', right: 10, top: 4, width: 44, height: 44, borderRadius: '50%', background: 'var(--gold)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22 }}>🔔</span>
      </div>
      <h1 className="h1" style={{ fontSize: 30, marginTop: 10 }}>Remind you at mealtimes?</h1>
      <p className="muted" style={{ fontSize: 16 }}>Max 4 a day. Never at night.</p>
      <div style={{ width: 'calc(100% - 40px)', display: 'flex', flexDirection: 'column', gap: 10, marginTop: 24 }}>
        <Btn onClick={() => setSys(true)}>Yes, remind me</Btn>
        <button className="btn secondary" onClick={async () => { await post('/profile', { reminders_on: false }); nav('/home'); }}>Not now</button>
      </div>
      {sys && (
        <div className="dialog" style={{ textAlign: 'left' }}>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 10 }}>
            <span style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--teal)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'var(--display)', fontWeight: 800 }}>M</span>
            <div><strong>Allow Mor to send you notifications?</strong><div className="small muted">Reminders may include alerts and sounds.</div></div>
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 6 }}>
            <button className="btn sm" style={{ flex: 1, background: '#fff', color: 'var(--teal)' }} onClick={async () => { await post('/profile', { reminders_on: false }); nav('/home'); }}>Don't allow</button>
            <button className="btn sm" style={{ flex: 1 }} onClick={async () => { await post('/profile', { reminders_on: true }); nav('/home'); }}>Allow</button>
          </div>
        </div>
      )}
    </Screen>
  );
}
