const ACTIVITY = { sit: 1.2, light: 1.375, active: 1.55, very: 1.725 };

export function bmr({ sex, age, height_cm, weight_kg }) {
  const base = 10 * weight_kg + 6.25 * height_cm - 5 * age;
  if (sex === 'male') return base + 5;
  if (sex === 'female') return base - 161;
  return base - 78;
}

export function tdee(profile) {
  const mult = ACTIVITY[profile.activity] ?? 1.375;
  return bmr(profile) * mult;
}

export function targetDate(weight, target) {
  const weeks = Math.max(1, Math.abs(weight - target) / 0.5);
  const d = new Date(Date.now() + Math.ceil(weeks) * 7 * 86400000);
  return d.toISOString().slice(0, 10);
}

export function isAmbitious(weight, target) {
  const loss = weight - target;
  return loss > 0 && loss > weight * 0.1;
}

export function milestone(weight, target) {
  const loss = weight - target;
  if (loss <= weight * 0.1) return target;
  return Math.round((weight - weight * 0.1) * 10) / 10;
}

export function computePlan(profile) {
  const { weight_kg, target_weight_kg } = profile;
  // v4 #5: the target decides the intent, not the goal word. Below current is
  // treated as loss for calories, above current as gain, equal as maintenance.
  const tgt = target_weight_kg || weight_kg;
  const delta = tgt - weight_kg;
  const intent = delta < -0.25 ? 'lose' : delta > 0.25 ? 'gain' : 'maintain';
  let kcal = tdee(profile);
  if (intent === 'lose') kcal -= 500;
  if (intent === 'gain') kcal += 300;
  const min = profile.sex === 'male' ? 1500 : 1200;
  kcal = Math.max(min, Math.round(kcal / 10) * 10);

  const protein_g = Math.min(120, Math.max(40, Math.round(1.15 * tgt)));
  const fat_g = Math.min(90, Math.max(35, Math.round(0.9 * tgt)));
  const carbs_g = Math.max(100, Math.round((kcal - protein_g * 4 - fat_g * 9) / 4));

  return {
    kcal_target: kcal,
    protein_g,
    carbs_g,
    fat_g,
    target_date: targetDate(weight_kg, tgt),
    goal_line: `${weight_kg} → ${tgt} kg`,
    ambitious: isAmbitious(weight_kg, tgt),
    milestone: milestone(weight_kg, tgt),
  };
}
