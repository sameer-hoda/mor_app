// Omni video clips for the Mor welcome journey. Usage: node scripts/journey-clips.mjs <NAME>...
// First+last frames per GENERATION.md §5: two image items (first, last), then text.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'

const root = new URL('..', import.meta.url).pathname
const env = readFileSync(root + 'backend/.env', 'utf8')
const KEY = env.match(/GEMINI_API_KEY=([^\s]+)/)[1]
const MODEL = 'gemini-omni-1.2-flash'
const KF = (n) => readFileSync(root + `design-ref/journey/kf/${n}.png`).toString('base64')

const VIDEO = `Single continuous shot, no scene cuts, no zoom pumping, no morphing architecture. Every architectural line stays rigid and stable from the first frame to the last. Real-world physics, real-time speed. No dialogue, no music.`

const PEACOCK = `The bird is the same adult male Indian peafowl (Pavo cristatus): iridescent cobalt-blue neck and breast, white facial stripes, a long bronze-green train with crisp eyespots. Photoreal, never stylised, cartoon or anthropomorphic. It is the only living creature in the shot.`

const EXACT = `Open exactly on the first frame and end exactly on the last frame.`

const CLIPS = {
  C0: {
    first: 'KF0', last: 'KF0', duration: '6s',
    prompt: `${EXACT} Locked-off camera, absolutely no camera movement. Only small ambient life: fine dust motes drift slowly through a soft shaft of daylight in front of the doors, and the thin line of light in the door seam breathes very gently brighter and dimmer. The doors stay closed and still. The end matches the start so it loops seamlessly. ${VIDEO}`,
  },
  C1: {
    first: 'KF0', last: 'KF2', duration: '4s',
    prompt: `${EXACT} [0-0.8s] The doors stay closed; the seam of light brightens and the doors shift very slightly as if unlatched. [0.8-2.6s] Both heavy doors swing inward together, slowly at first and then smoothly, hinged on their outer edges, revealing warm golden daylight that spills across the marble floor and plinths toward the camera; dust glitters in the light. [1.5-4s] The camera dollies slowly forward toward the doorway at walking pace. [3.2-4s] The light swells until the doorway is a bright warm glare, matching the last frame. ${VIDEO}`,
  },
  C2: {
    first: 'KF2', last: 'KF3', duration: '4s',
    prompt: `${EXACT} [0-1.4s] The bright glare softens and clears as the camera passes through the doorway, revealing the red arch and the peacock standing centred, train folded. [1.4-3.2s] The peacock turns to face the camera, lifts its train and spreads it in one fluid, natural display until it is fully fanned. [3.2-4s] The train settles with a faint shimmer; the camera settles to a stop. Matches the last frame exactly. ${PEACOCK} ${VIDEO}`,
  },
  C3: {
    first: 'KF3', last: 'KF3', duration: '6s',
    prompt: `${EXACT} Locked-off camera. The peacock holds the fanned display calmly: the train breathes with a very slight sway, the eyespots catch the light and shimmer iridescently, the crest feathers move in a light breeze, and the head turns a little to one side and back. A few dust motes drift in the warm backlight. The bird ends exactly in its starting pose so the clip loops seamlessly. ${PEACOCK} ${VIDEO}`,
  },
  C4: {
    first: 'KF3', last: 'KF3', ref: 'KF3B', duration: '3s',
    prompt: `${EXACT} Locked-off camera. [0-0.4s] The peacock stays still. [0.4-1.8s] It performs the real peacock train-rattle display: the fanned train shivers rapidly so the eyespots shimmer, rises a little higher and wider (as in <IMAGE_REF_0>), and the bird takes one small step and turns slightly side to side. [1.8-3s] It settles smoothly back into the exact starting pose. ${PEACOCK} ${VIDEO}`,
  },
}

const names = process.argv.slice(2)
if (!names.length) { console.error('usage: node scripts/journey-clips.mjs <C0|C1|C2|C3|C4>...'); process.exit(1) }
mkdirSync(root + 'design-ref/journey/raw', { recursive: true })

for (const name of names) {
  const def = CLIPS[name]
  if (!def) { console.error('unknown clip: ' + name); continue }
  const images = [
    { type: 'image', mime_type: 'image/png', data: KF(def.first) },
    { type: 'image', mime_type: 'image/png', data: KF(def.last) },
  ]
  if (def.ref) images.push({ type: 'image', mime_type: 'image/png', data: KF(def.ref) })
  const body = {
    model: MODEL,
    input: [...images, { type: 'text', text: def.prompt }],
    response_format: { type: 'video', aspect_ratio: '9:16', resolution: '1080p', duration: def.duration },
    generation_config: { video_config: { task: 'image_to_video' } },
  }
  process.stdout.write(`→ ${name} (${def.duration}) … `)
  const t0 = Date.now()
  try {
    const res = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
      method: 'POST',
      headers: { 'x-goog-api-key': KEY, 'content-type': 'application/json' },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(600000),
    })
    const j = await res.json()
    if (!res.ok || j.error) {
      console.log(`FAIL ${res.status} ${JSON.stringify(j.error || j).slice(0, 300)}`)
      // retry without the extra ref (C4 fallback per GENERATION.md §5)
      if (def.ref) {
        const body2 = {
          model: MODEL,
          input: [images[0], images[1], { type: 'text', text: def.prompt.replace(/ \(as in <IMAGE_REF_0>\)/, '') }],
          response_format: { type: 'video', aspect_ratio: '9:16', resolution: '1080p', duration: def.duration },
          generation_config: { video_config: { task: 'image_to_video' } },
        }
        process.stdout.write('  ↳ retry without IMAGE_REF … ')
        const res2 = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
          method: 'POST',
          headers: { 'x-goog-api-key': KEY, 'content-type': 'application/json' },
          body: JSON.stringify(body2),
          signal: AbortSignal.timeout(600000),
        })
        const j2 = await res2.json()
        if (!res2.ok || j2.error) { console.log(`FAIL ${res2.status} ${JSON.stringify(j2.error || j2).slice(0, 300)}`); continue }
        const parts2 = (j2.steps || []).flatMap((s) => s.outputs || s.content || [])
        const v2 = parts2.find((c) => c?.type === 'video')
        if (!v2?.data) { console.log('no video'); continue }
        const out = root + `design-ref/journey/raw/${name}.mp4`
        writeFileSync(out, Buffer.from(v2.data, 'base64'))
        console.log(`ok ${((Date.now() - t0) / 1000) | 0}s → ${out} (${(readFileSync(out).length / 1048576).toFixed(1)}MB)`)
      }
      continue
    }
    const parts = (j.steps || []).flatMap((s) => s.outputs || s.content || [])
    const video = parts.find((c) => c?.type === 'video')
    if (!video?.data) { console.log(`no video, parts: ${parts.map((p) => p?.type).join(',')}`); continue }
    const out = root + `design-ref/journey/raw/${name}.mp4`
    writeFileSync(out, Buffer.from(video.data, 'base64'))
    console.log(`ok ${((Date.now() - t0) / 1000) | 0}s → ${out} (${(readFileSync(out).length / 1048576).toFixed(1)}MB)`)
  } catch (e) {
    console.log('ERR ' + e.message)
  }
}
