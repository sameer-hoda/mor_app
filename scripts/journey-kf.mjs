// Keyframe generation for the Mor welcome journey (Nano Banana Pro).
// Usage: node scripts/journey-kf.mjs <outPrefix> <nCandidates> -- <prompt-file>
// References are attached per-prompt from a manifest embedded below.
import { readFileSync, writeFileSync } from 'node:fs'

const root = new URL('..', import.meta.url).pathname
const env = readFileSync(root + 'backend/.env', 'utf8')
const KEY = env.match(/GEMINI_API_KEY=([^\s]+)/)[1]

const REFS = {
  MB1: 'design-ref/journey/refs/MB1_lotus_gate.jpg',
  MB2: 'design-ref/journey/refs/MB2_red_arch_peacock.jpg',
  MB3: 'design-ref/journey/refs/MB3_peacock_ledge.jpg',
  MB4: 'design-ref/journey/refs/MB4_gold_gate.jpg',
}

const STYLE = `Photoreal architectural photography of a Rajput palace in Jaipur. Materials: sindoor-red and rose lime plaster with gentle weathering, carved pink sandstone, white marble, painted lotus and peacock-feather fresco, aged brass and gold leaf. Light: soft natural daylight with warm highlights and open shadows, no harsh HDR. Lens: 35 mm full-frame look at eye height, vertical lines perfectly vertical, symmetrical one-point composition. Colour: saturated sindoor red, ivory, sandstone and brass gold; peacock blue and emerald are the only cool accents. Editorial detail, natural film-like grain. No people, no text, no signage, no logos, no modern objects, no lens flare, no vignette, no watermark.`

const PEACOCK = `The same adult male Indian peafowl (Pavo cristatus) in every shot: iridescent cobalt-blue neck and breast, white facial stripes, a crest of fan-tipped feathers, a long bronze-green train with crisp eyespots, barred brown wings, grey legs. Exactly two legs, natural proportions and natural behaviour. Photoreal, never stylised, cartoon or anthropomorphic.`

// KF0 — gate closed. Refs: MB1, MB4
const KF0 = `A frontal, perfectly symmetrical palace gateway. Tall closed double doors of red-lacquered carved wood: five panels per leaf, rows of brass studs, two brass ring knockers at the meeting edge, and a cusped arched top. White marble door frame, with low white marble plinths either side. Above the doors, a large semicircular fan arch densely painted with rows of lotus-petal and peacock-feather eyespots in sindoor red, ivory, emerald and teal, radiating from a small gilded medallion, framed by painted floral panels on pale green. A thin line of warm daylight glows through the seam between the doors and under them. Soft, even morning light. Doors at x 25-75%, y 48-92%; fan arch at y 15-48%.`

// KF1 — doors half open. Parent: KF0
const KF1 = `Use this exact image. Change only the doors: both leaves have swung inward about 45 degrees, hinged on their outer edges, and warm golden daylight streams through the opening onto the marble floor and the plinths. Through the gap, overexposed, a sunlit red courtyard is faintly visible. Everything else is identical, pixel for pixel.`

// KF2 — doorway glare. Parent: KF1
const KF2 = `Use this exact image. The doors are now fully open against the walls. The doorway is filled with bright, almost white warm daylight that blooms out over the frame edges and the floor. The far courtyard is only a pale hint. Same camera, a little closer (about 8%).`

// KF3 — hero Neelu. Refs: MB2, MB3 + parent KF2
const KF3 = `${PEACOCK} Neelu stands centred in a deep sindoor-red cusped arch of weathered lime plaster (hairline cracks, small flaking patches of white plaster), facing the camera with its train fully fanned in a wide, even semicircle filling the arch opening. Behind it, a receding sequence of smaller arches ends in a sunlit white-marble arch. Low sandstone step in the foreground. Soft warm daylight spilling from behind the bird gives the train a gentle rim light. Train spans y 45-85%, head at y 62%, feet at y 88%, centred.`

// KF3a — train folded. Parent: KF3
const KF3A = `Use this exact image. Change only the peacock: its train is folded and trails behind it to the floor, and its head is turned slightly to its left. Same bird, position and light. Everything else identical.`

// KF3b — train lifted high. Parent: KF3
const KF3B = `Use this exact image. Change only the peacock: its train is raised a little higher and spread a little wider, slightly forward, feathers trembling (a hint of motion blur on the outer eyespots), wings drooped. Same bird and position. Everything else identical.`

const PROMPTS = {
  KF0: { refs: ['MB1', 'MB4'], text: `${STYLE}\n\n${KF0}` },
  KF1: { refs: [], parent: 'KF0', text: `${STYLE}\n\n${KF1}` },
  KF2: { refs: [], parent: 'KF1', text: `${STYLE}\n\n${KF2}` },
  KF3: { refs: ['MB2', 'MB3'], parent: 'KF2', text: `${STYLE}\n\n${KF3}` },
  KF3A: { refs: [], parent: 'KF3', text: `${STYLE}\n\n${KF3A}` },
  KF3B: { refs: [], parent: 'KF3', text: `${STYLE}\n\n${KF3B}` },
}

const [name, nStr] = process.argv.slice(2)
const n = Number(nStr || 4)
const def = PROMPTS[name]
if (!def) { console.error('unknown keyframe: ' + name); process.exit(1) }

const parts = []
for (const r of def.refs) parts.push({ inline_data: { mime_type: 'image/jpeg', data: readFileSync(root + REFS[r]).toString('base64') } })
if (def.parent) {
  // parent must already exist as design-ref/journey/kf/<PARENT>.png
  parts.push({ inline_data: { mime_type: 'image/png', data: readFileSync(root + `design-ref/journey/kf/${def.parent}.png`).toString('base64') } })
}
parts.push({ text: def.text })

for (let i = 1; i <= n; i++) {
  const out = root + `design-ref/journey/kf/${name}_c${i}.png`
  process.stdout.write(`→ ${name}_c${i} … `)
  const t0 = Date.now()
  try {
    const res = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/nano-banana-pro-preview:generateContent`,
      {
        method: 'POST',
        headers: { 'content-type': 'application/json', 'x-goog-api-key': KEY },
        body: JSON.stringify({
          contents: [{ role: 'user', parts }],
          generationConfig: { responseModalities: ['IMAGE'], imageConfig: { aspectRatio: '9:16' } },
        }),
      },
    )
    const j = await res.json()
    const p = j.candidates?.[0]?.content?.parts?.find((p) => p.inlineData || p.inline_data)
    const b64 = p?.inlineData?.data || p?.inline_data?.data
    if (!b64) {
      console.log(`FAIL ${res.status} ${JSON.stringify(j.error || j.candidates?.[0]?.finishReason || j).slice(0, 300)}`)
      continue
    }
    writeFileSync(out, Buffer.from(b64, 'base64'))
    console.log(`ok ${(Date.now() - t0) / 1000 | 0}s → ${out}`)
  } catch (e) {
    console.log('ERR ' + e.message)
  }
}
