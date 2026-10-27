import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

export function Btn({ children, className = '', badge = true, ...p }) {
  return (
    <button className={`btn ${className}`} {...p}>
      <span style={{ flex: 1 }}>{children}</span>
      {badge && !p.disabled && (
        <span className="badge-eye">
          <FeatherEye size={28} arrow />
        </span>
      )}
    </button>
  );
}

export function FeatherEye({ size = 20, arrow = false }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="11" fill="#F2B544" />
      <circle cx="12" cy="12" r="7.5" fill="#19A39A" />
      <circle cx="12" cy="12" r="4.5" fill="#2D2A6E" />
      {arrow && <path d="M9.6 12h4.8M12.4 9.6L14.8 12l-2.4 2.4" stroke="#fff" strokeWidth="1.6" fill="none" strokeLinecap="round" strokeLinejoin="round" />}
    </svg>
  );
}

export function Segmented({ options, value, onChange, tall = false }) {
  return (
    <div className={`seg ${tall ? 'tall' : ''}`}>
      {options.map((o) => (
        <button key={o.value || o} type="button" className={String(o.value || o) === String(value) ? 'on' : ''}
          onClick={() => onChange(o.value || o)}>{o.label || o}</button>
      ))}
    </div>
  );
}

export function Toggle({ on, onChange }) {
  return <button type="button" aria-pressed={on} className={`toggle ${on ? 'on' : ''}`} onClick={() => onChange(!on)} />;
}

export function VegMark({ kind = 'veg' }) {
  return <span className={`vegmark ${kind}`} aria-label={kind}><i /></span>;
}

export function Sheet({ children, onClose, dim = true }) {
  return (
    <>
      {dim && <div className="overlay" onClick={onClose} />}
      <div className="sheet">
        <div className="grab" />
        {children}
      </div>
    </>
  );
}

export function Ring({ progress, size = 100, stroke = 10, color = 'var(--teal)', track = '#EFE9DC', children }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const dash = Math.max(0, Math.min(1, progress)) * c;
  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      <svg width={size} height={size}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke={track} strokeWidth={stroke} fill="none" />
        <circle cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth={stroke} fill="none"
          strokeDasharray={`${dash} ${c}`} strokeLinecap="round" transform={`rotate(-90 ${size / 2} ${size / 2})`} />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{children}</div>
    </div>
  );
}

export function MacroBars({ eaten, goal }) {
  const rows = [
    { k: 'Carbs', c: 'var(--genda)', e: eaten.carbs_g, g: goal.carbs_g, unit: 'g' },
    { k: 'Protein', c: 'var(--neel)', e: eaten.protein_g, g: goal.protein_g, unit: 'g' },
    { k: 'Fat', c: 'var(--pankh)', e: eaten.fat_g, g: goal.fat_g, unit: 'g' },
  ];
  return (
    <div className="macro-bars">
      {rows.map((r) => (
        <div className="macro-bar" key={r.k}>
          <span className="lbl">{r.k}</span>
          <span className="track"><span className="fill" style={{ width: `${Math.min(100, (r.e / Math.max(1, r.g)) * 100)}%`, background: r.c }} /></span>
          <span className="val num">{r.e}/{r.g}{r.unit}</span>
        </div>
      ))}
    </div>
  );
}

export function Field({ error, ...p }) {
  return <input className={`field ${error ? 'error' : ''}`} {...p} />;
}

export function StepDots({ step, total = 6 }) {
  return (
    <div className="step-dots">
      {Array.from({ length: total }, (_, i) => <span key={i} className={i < step ? 'on' : ''} />)}
    </div>
  );
}

export function Screen({ bg = 'var(--chandan)', dark = false, children, style = {} }) {
  return <div className="page fade-in" style={{ background: bg, ...style }}>{children}</div>;
}

export function TopBar({ title, back, right, dark = false }) {
  const nav = useNavigate();
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 16px 8px' }}>
      {back ? (
        <button onClick={() => nav(back)} aria-label="Back" style={iconBtn(dark)}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={dark ? '#fff' : 'var(--ink)'} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        </button>
      ) : <span style={{ width: 44 }} />}
      {title && <strong style={{ fontSize: 17, color: dark ? '#fff' : 'var(--ink)' }}>{title}</strong>}
      {right || <span style={{ width: 44 }} />}
    </div>
  );
}

function iconBtn(dark) {
  return {
    width: 44, height: 44, borderRadius: '50%', border: 'none', cursor: 'pointer',
    background: dark ? 'rgba(255,255,255,0.14)' : '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
  };
}

export function Navbar({ active }) {
  const Icon = ({ d, stroke = 1.9 }) => <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"><path d={d} /></svg>;
  const tabs = [
    { k: 'home', label: 'Home', to: '/home', icon: <Icon d="M3 10.5L12 3l9 7.5V21H3z" /> },
    { k: 'insights', label: 'Insights', to: '/insights', icon: <Icon d="M4 20V10M10 20V4M16 20v-8M22 20H2" /> },
    { k: 'log', label: 'Log', to: '/add', icon: null },
    { k: 'challenges', label: 'Coins', to: '/challenges', icon: <Icon d="M12 2v20M2 12h20" /> },
    { k: 'profile', label: 'Profile', to: '/profile', icon: <Icon d="M12 12a4 4 0 100-8 4 4 0 000 8zM4 21c0-4 4-6 8-6s8 2 8 6" /> },
  ];
  return (
    <nav className="navbar">
      {tabs.map((t) => t.k === 'log' ? (
        <Link key={t.k} to={t.to} aria-label="Log a meal" className="tab">
          <span className="fab"><FeatherEye size={36} arrow /></span>
        </Link>
      ) : (
        <Link key={t.k} to={t.to} className={`tab ${active === t.k ? 'on' : ''}`}>
          <span className="icon-wrap">{t.icon}</span>{t.label}
        </Link>
      ))}
    </nav>
  );
}

export function NeeluImg({ mood = 'happy', size = 160, anim, style = {}, className = '', alt = '' }) {
  const [err, setErr] = React.useState(false);
  const src = `/neelu/neelu-${mood}.png`;
  if (err) return <NeeluSvg size={size} mood={mood} style={style} className={className} />;
  return (
    <img src={src} alt={alt || `Neelu ${mood}`} width={size} height={size}
      className={`neelu ${anim || ''} ${className}`} style={style}
      onError={() => setErr(true)} draggable={false} />
  );
}

export function NeeluSvg({ size = 160, mood = 'happy', style = {}, className = '' }) {
  const eyes = { happy: [7, 7], cool: [6, 6], joy: [2, 2], love: [6, 6], hungry: [7, 7], thinking: [7, 7], sleepy: [0, 0] }[mood] || [7, 7];
  return (
    <svg width={size} height={size} viewBox="0 0 200 200" className={`neelu ${className}`} style={style} aria-hidden="true">
      <g transform="translate(0 6)">
        <path d="M40 150 Q20 130 26 96" stroke="#2B8A57" strokeWidth="10" fill="none" strokeLinecap="round" />
        <path d="M160 150 Q180 130 174 96" stroke="#2B8A57" strokeWidth="10" fill="none" strokeLinecap="round" />
        <ellipse cx="100" cy="140" rx="46" ry="38" fill="#1F4FB5" />
        <ellipse cx="100" cy="146" rx="28" ry="24" fill="#2A5FCB" />
        <path d="M78 176 L78 184 M100 176 L100 184 M122 176 L122 184" stroke="#E8843C" strokeWidth="7" strokeLinecap="round" />
        <circle cx="100" cy="78" r="52" fill="#2463C9" />
        <path d="M88 30 L84 14 M100 28 L100 10 M112 30 L116 14" stroke="#19A39A" strokeWidth="3.5" strokeLinecap="round" fill="none" />
        <circle cx="84" cy="12" r="5" fill="#19A39A" /><circle cx="84" cy="12" r="2" fill="#F2B544" />
        <circle cx="100" cy="8" r="5" fill="#19A39A" /><circle cx="100" cy="8" r="2" fill="#F2B544" />
        <circle cx="116" cy="12" r="5" fill="#19A39A" /><circle cx="116" cy="12" r="2" fill="#F2B544" />
        {mood === 'cool' ? (
          <rect x="66" y="66" width="68" height="18" rx="9" fill="#111" stroke="#F2B544" strokeWidth="2" />
        ) : (
          <>
            <ellipse cx="82" cy="76" rx={eyes[0]} ry={eyes[0] === 2 ? 1.5 : 9} fill="#111" />
            <ellipse cx="118" cy="76" rx={eyes[1]} ry={eyes[1] === 2 ? 1.5 : 9} fill="#111" />
            {mood === 'love' && <>
              <path d="M76 70 c-3-5 2-9 6-5 c4-4 9 0 6 5 l-6 5 z" fill="#E75A9C" transform="translate(-2 2)" />
              <path d="M112 70 c-3-5 2-9 6-5 c4-4 9 0 6 5 l-6 5 z" fill="#E75A9C" transform="translate(-2 2)" />
            </>}
            {eyes[0] > 0 && <>
              <circle cx="79" cy="72" r="2.6" fill="#fff" /><circle cx="86" cy="79" r="1.6" fill="#fff" />
              <circle cx="115" cy="72" r="2.6" fill="#fff" /><circle cx="122" cy="79" r="1.6" fill="#fff" />
            </>}
            <path d="M72 92 q10 4 20 0" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" opacity="0.85" />
            <path d="M108 92 q10 4 20 0" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" opacity="0.85" />
          </>
        )}
        <ellipse cx="72" cy="96" rx="7" ry="5" fill="#F3B7C2" opacity="0.8" />
        <ellipse cx="128" cy="96" rx="7" ry="5" fill="#F3B7C2" opacity="0.8" />
        <path d="M92 102 q8 7 16 0" stroke="#F5B83D" strokeWidth="0" fill="none" />
        <path d="M94 104 L106 104 L100 112 Z" fill="#F5B83D" />
        {mood === 'sleepy' && <>
          <text x="132" y="40" fontSize="20" fontWeight="800" fill="#1F4FB5" fontFamily="Baloo 2">Z</text>
          <text x="146" y="26" fontSize="14" fontWeight="800" fill="#1F4FB5" fontFamily="Baloo 2">Z</text>
        </>}
        {mood === 'joy' && <>
          <circle cx="150" cy="52" r="3" fill="#F2B544" /><circle cx="46" cy="60" r="2.5" fill="#F2B544" />
        </>}
      </g>
    </svg>
  );
}

const VIDEO_MOOD = { splash: 'cool', flex: 'cool', wave: 'happy', peek: 'happy', hop: 'love', dance: 'joy', think: 'thinking', shrug: 'thinking', hungry: 'hungry', droop: 'hungry', sleep: 'sleepy' };
const ONE_SHOTS = ['wave', 'peek', 'hop', 'dance', 'shrug', 'splash', 'flex'];

export function NeeluVideo({ anim = 'wave', size = 160, className = '', style = {}, alt = '' }) {
  const ref = React.useRef(null);
  const plays = React.useRef(0);
  const [failed, setFailed] = React.useState(() => {
    const ua = navigator.userAgent || '';
    const isSafariOnly = /Safari/.test(ua) && !/Chrome|Chromium|Edg|Firefox/.test(ua);
    return isSafariOnly;
  });
  const loops = !ONE_SHOTS.includes(anim);
  const onEnded = () => {
    plays.current += 1;
    if (loops && plays.current < 3 && ref.current) ref.current.play().catch(() => {});
  };
  if (failed) {
    return <NeeluImg mood={VIDEO_MOOD[anim] || 'happy'} size={size} anim={loops ? 'breathe' : 'bob'} style={style} className={className} alt={alt} />;
  }
  return (
    <video ref={ref} className={`neelu ${className}`} style={{ width: size, height: size, ...style }}
      src={`/neelu-video/neelu-${anim}.webm`} poster={`/neelu/neelu-${VIDEO_MOOD[anim] || 'happy'}.png`}
      autoPlay muted playsInline onEnded={onEnded} onError={() => setFailed(true)}
      aria-label={alt || `Neelu ${anim}`} draggable={false} />
  );
}

export function Coin({ size = 18 }) {  return (
    <svg width={size} height={size} viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="10" fill="#F2A93B" />
      <circle cx="12" cy="12" r="7" fill="#F2B544" />
      <path d="M12 8v8M9.5 10.5h5M9.5 13.5h5" stroke="#7A4B05" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

export function Diya({ lit = true, size = 20 }) {
  return (
    <svg width={size} height={size * 0.8} viewBox="0 0 24 20">
      <path d="M3 12 q9 8 18 0 l-2 6 q-7 3 -14 0 z" fill="#C98A1E" />
      <path d="M3 12 q9 5 18 0 l-1 2 q-8 4 -16 0 z" fill="#F2B544" />
      {lit && <>
        <ellipse cx="12" cy="7" rx="3" ry="5" fill="#F2A93B" />
        <ellipse cx="12" cy="7.5" rx="1.4" ry="3" fill="#FFF3DC" />
      </>}
    </svg>
  );
}
