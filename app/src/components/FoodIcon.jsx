import React from 'react';

const C = {
  thali: '#C98A1E', chai: '#8A5A2B', poha: '#F2A93B', idli: '#EFE9DC', dosa: '#E3C394',
  dal: '#E8A23C', roti: '#D9B48A', rice: '#F5F0E4', paneer: '#F5EDE0', samosa: '#E0A34C',
  fruit: '#E75A5A', curd: '#FDFBF5', biryani: '#E8B04C', paratha: '#DDB06A', sprouts: '#7CB35A', makhana: '#EDE4D2',
};

const SHAPES = {
  thali: (c) => <>
    <circle cx="12" cy="12" r="9.4" fill={c.thali} />
    <circle cx="9" cy="9.6" r="2.6" fill={c.dal} />
    <circle cx="15" cy="9.6" r="2.6" fill={c.rice} />
    <circle cx="12" cy="14.6" r="2.6" fill={c.roti} />
  </>,
  chai: (c) => <>
    <path d="M6 9h10v6a4 4 0 01-4 4H10a4 4 0 01-4-4z" fill={c.chai} />
    <path d="M16 10c3 0 3 4 0 4" stroke={c.chai} strokeWidth="1.8" fill="none" />
    <ellipse cx="11" cy="9" rx="5" ry="1.4" fill="#F5EDE0" />
    <path d="M9 5q2 2 0 4M13 5q2 2 0 4" stroke="#9B9B9B" strokeWidth="1" fill="none" opacity="0.6" />
  </>,
  poha: (c) => <>
    <path d="M3 13a9 5 0 0018 0l-1.5-2.5h-15z" fill={c.poha} />
    <path d="M4.5 10.5h15" stroke="#FFF3DC" strokeWidth="2.4" strokeLinecap="round" />
    <circle cx="10" cy="10" r="1" fill="#E75A5A" /><circle cx="14.5" cy="10.4" r="1" fill="#7CB35A" />
  </>,
  idli: (c) => <>
    <ellipse cx="12" cy="15" rx="8" ry="4" fill={c.curd} stroke="#D9D2C2" />
    <ellipse cx="9" cy="10" rx="3.6" ry="2.6" fill="#FDFBF5" stroke="#E3DCCB" />
    <ellipse cx="15" cy="10" rx="3.6" ry="2.6" fill="#FDFBF5" stroke="#E3DCCB" />
  </>,
  dosa: (c) => <>
    <path d="M3 16a9 6 0 0118 0l-2 1H5z" fill={c.dosa} />
    <path d="M6 15q6-3 12 0" stroke="#C79A5B" strokeWidth="1.6" fill="none" />
  </>,
  dal: (c) => <>
    <path d="M4 10h16v3a8 6 0 01-16 0z" fill={c.dal} />
    <ellipse cx="12" cy="10" rx="8" ry="2.6" fill="#EFC06A" />
    <circle cx="10" cy="9.4" r="0.9" fill="#C98A1E" /><circle cx="14" cy="10.2" r="0.9" fill="#C98A1E" />
  </>,
  roti: (c) => <>
    <circle cx="12" cy="12" r="8.6" fill={c.roti} />
    <circle cx="12" cy="12" r="7" fill="#E3C9A4" />
    <circle cx="9" cy="10" r="1" fill="#B98A55" /><circle cx="14" cy="13" r="1.2" fill="#B98A55" /><circle cx="12" cy="15" r="0.8" fill="#B98A55" />
  </>,
  rice: (c) => <>
    <path d="M5 12h14l-2 7a7 3 0 01-10 0z" fill="#C7CDD4" />
    <ellipse cx="12" cy="12" rx="7" ry="2.4" fill={c.rice} />
    <path d="M9 11l1-1.6M12 11l1-1.6M15 11l1-1.6" stroke="#E3DCCB" strokeWidth="1.2" strokeLinecap="round" />
  </>,
  paneer: (c) => <>
    <rect x="5" y="8" width="14" height="10" rx="2.5" fill="#E8B04C" />
    <rect x="7.4" y="10" width="4" height="3.4" rx="1" fill={c.paneer} />
    <rect x="12.6" y="12.4" width="4" height="3.4" rx="1" fill={c.paneer} />
    <circle cx="10" cy="15" r="0.9" fill="#7CB35A" /><circle cx="15" cy="10" r="0.9" fill="#E75A5A" />
  </>,
  samosa: (c) => <>
    <path d="M12 4L19 17H5z" fill={c.samosa} />
    <path d="M12 4l3.5 6.5M12 4L8.5 10.5M12 4v7" stroke="#C98A1E" strokeWidth="1.2" />
    <path d="M7.5 14.5h9" stroke="#C98A1E" strokeWidth="1.2" />
  </>,
  fruit: (c) => <>
    <circle cx="10" cy="13" r="6" fill={c.fruit} />
    <circle cx="16" cy="14" r="4.4" fill="#F2A93B" />
    <path d="M10 6.6q0-3 3-3.6" stroke="#7CB35A" strokeWidth="1.6" fill="none" />
  </>,
  curd: (c) => <>
    <path d="M6 8h12l-1.6 10a5 3 0 01-8.8 0z" fill={c.curd} stroke="#D9D2C2" />
    <ellipse cx="12" cy="8" rx="6" ry="1.8" fill="#F5F0E4" />
    <circle cx="10" cy="13" r="0.9" fill="#7CB35A" /><circle cx="14" cy="14" r="0.9" fill="#E75A5A" />
  </>,
  biryani: (c) => <>
    <path d="M4 11h16l-2.4 8a6 3 0 01-11.2 0z" fill="#B4661E" />
    <ellipse cx="12" cy="11" rx="8" ry="2.6" fill={c.biryani} />
    <circle cx="9" cy="10.6" r="1" fill="#7CB35A" /><circle cx="14.4" cy="10.4" r="1" fill="#E75A5A" />
  </>,
  paratha: (c) => <>
    <path d="M12 3.4c6 2 8.6 6.6 8.6 8.6 0 2-2.6 6.6-8.6 8.6-6-2-8.6-6.6-8.6-8.6 0-2 2.6-6.6 8.6-8.6z" fill={c.paratha} />
    <path d="M6 9q6-3 12 0M5.4 12.6q6.6-3.4 13.2 0M6.6 16q5.4-3 10.8 0" stroke="#B98A55" strokeWidth="1" fill="none" />
  </>,
  sprouts: (c) => <>
    <circle cx="9" cy="12" r="3.2" fill="#A8C686" />
    <circle cx="14" cy="10" r="3.2" fill={c.sprouts} />
    <circle cx="14.4" cy="15" r="3.2" fill="#8FBE6B" />
    <circle cx="9" cy="12" r="1.2" fill="#F5F0E4" /><circle cx="14" cy="10" r="1.2" fill="#F5F0E4" /><circle cx="14.4" cy="15" r="1.2" fill="#F5F0E4" />
  </>,
  makhana: (c) => <>
    <circle cx="9" cy="10" r="3" fill={c.makhana} stroke="#D9C9B4" />
    <circle cx="15" cy="9" r="3" fill={c.makhana} stroke="#D9C9B4" />
    <circle cx="12" cy="14.6" r="3" fill={c.makhana} stroke="#D9C9B4" />
    <circle cx="9" cy="10" r="1" fill="#fff" /><circle cx="15" cy="9" r="1" fill="#fff" /><circle cx="12" cy="14.6" r="1" fill="#fff" />
  </>,
};

export default function FoodIcon({ kind = 'thali', size = 38, style = {} }) {
  const c = C;
  const shape = SHAPES[kind] || SHAPES.thali;
  return (
    <span className="foodicon" style={{ width: size, height: size, ...style }}>
      <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">{shape(c)}</svg>
    </span>
  );
}

export function kindForFood(name) {
  const n = (name || '').toLowerCase();
  const map = [
    [/dal|rajma|chole|sambar|curry/, 'dal'], [/roti|phulka|chapati|naan/, 'roti'],
    [/rice|biryani|pulao|khichdi/, 'rice'], [/dosa/, 'dosa'], [/idli/, 'idli'],
    [/poha|upma/, 'poha'], [/chai|coffee|tea|lassi/, 'chai'], [/paratha/, 'paratha'],
    [/paneer/, 'paneer'], [/samosa/, 'samosa'], [/raita|curd|yogurt|dahi/, 'curd'],
    [/salad|fruit|chaat/, 'fruit'], [/sprouts/, 'sprouts'], [/makhana/, 'makhana'],
    [/thali|plate|meal/, 'thali'],
  ];
  for (const [re, kind] of map) if (re.test(n)) return kind;
  return 'thali';
}
