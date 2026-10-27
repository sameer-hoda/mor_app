#!/usr/bin/env python3
"""Key generated mascot videos: magenta border flood-fill -> alpha, encode WebM VP9 with alpha."""
import os
import subprocess
import sys
from collections import deque

from PIL import Image, ImageFilter

RAW = "/tmp/omni/raw"
FRAMES = "/tmp/omni/frames"
KEYED = "/tmp/omni/keyed"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "public", "neelu-video")
os.makedirs(FRAMES, exist_ok=True)
os.makedirs(KEYED, exist_ok=True)
os.makedirs(OUT, exist_ok=True)

TARGET = (255, 0, 255)
NEAR = 105
FEATHER = 150
FPS = 24


def dist(c, t):
    return ((c[0] - t[0]) ** 2 + (c[1] - t[1]) ** 2 + (c[2] - t) ** 2) ** 0.5


def extract(src, dst_dir):
    for f in os.listdir(dst_dir):
        os.remove(os.path.join(dst_dir, f))
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-i", src,
        "-vf", f"fps={FPS},scale=512:512:flags=lanczos",
        os.path.join(dst_dir, "f%04d.png"),
    ], check=True)


def key_frame(path):
    img = Image.open(path).convert("RGBA")
    w, h = img.size
    px = img.load()

    def d(c):
        return ((c[0] - 255) ** 2 + (c[1]) ** 2 + (c[2] - 255) ** 2) ** 0.5

    near = [[d(px[x, y][:3]) <= NEAR for x in range(w)] for y in range(h)]
    bg = [[False] * w for _ in range(h)]
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if near[y][x]:
                bg[y][x] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if near[y][x] and not bg[y][x]:
                bg[y][x] = True
                q.append((x, y))
    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and near[ny][nx] and not bg[ny][nx]:
                bg[ny][nx] = True
                q.append((nx, ny))
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if bg[y][x]:
                px[x, y] = (r, g, b, 0)
                continue
            touch = any(
                bg[ny][nx]
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1))
                for nx, ny in ((x + dx, y + dy),)
                if 0 <= nx < w and 0 <= ny < h
            )
            af = a / 255.0
            if touch:
                # edge band: fully neutralize any magenta cast
                m = min(r, b) - g
                if m > 8:
                    r = max(0, int(r - m * 0.85))
                    b = max(0, int(b - m * 0.85))
                if 0.05 < af < 0.995:
                    # blend of mascot over magenta: unpremultiply
                    ur = (r - (1 - af) * 255) / af
                    ug = g / af
                    ub = (b - (1 - af) * 255) / af
                    if 0 <= ur <= 255 and 0 <= ub <= 255 and ug <= 255:
                        r, g, b = int(ur), int(ug), int(ub)
            elif a > 0 and min(r, b) > g + 25:
                # interior: gentle despill only
                excess = (min(r, b) - g) * 0.8
                r = max(0, min(255, int(r - excess)))
                b = max(0, min(255, int(b - excess)))
                px[x, y] = (r, g, b, a)
            dd = d((r, g, b))
            if dd < FEATHER:
                alpha = int(min(255, max(0, (dd - NEAR) / (FEATHER - NEAR) * 255)))
                px[x, y] = (r, g, b, min(a, alpha))
    alpha_ch = img.getchannel("A").filter(ImageFilter.MinFilter(3))
    img.putalpha(alpha_ch)
    img.save(path)


def encode(frames_dir, dst, loop):
    args = ["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS), "-i", os.path.join(frames_dir, "f%04d.png")]
    if loop:
        args += ["-loop", "0"]
    args += [
        "-c:v", "libvpx-vp9", "-pix_fmt", "yuva420p", "-auto-alt-ref", "0",
        "-b:v", "900k", "-cpu-used", "5", "-an", dst,
    ]
    subprocess.run(args, check=True)


MAP = {
    "wave": ("happy-lifts_one_wing_and.mp4", False),
    "peek": ("happy-rises_up_from_belo.mp4", False),
    "hop": ("love-two_little_hops_s.mp4", False),
    "dance": ("joy-shimmies_the_fanne.mp4", False),
    "think": ("thinking-taps_beak_with_a_w.mp4", True),
    "hungry": ("hungry-rubs_belly_with_on.mp4", True),
    "sleep": ("sleepy-dozes_with_slow_br.mp4", True),
    "droop": ("hungry-crest_stems_droop_.mp4", True),
    "shrug": ("thinking-shrugs_both_wings_.mp4", False),
    "splash": ("cool-tail_feathers_shim.mp4", False),
    "flex": ("cool-bounces_once_with_.mp4", False),
}


def process(name):
    if name not in MAP:
        print(f"UNKNOWN slot {name}")
        return False
    src, loop = MAP[name]
    path = os.path.join(RAW, src)
    if not os.path.exists(path):
        print(f"NO RAW for {name}: {src}")
        return False
    fdir = os.path.join(FRAMES, name)
    os.makedirs(fdir, exist_ok=True)
    extract(path, fdir)
    for f in sorted(os.listdir(fdir)):
        key_frame(os.path.join(fdir, f))
    dst = os.path.join(OUT, f"neelu-{name}.webm")
    encode(fdir, dst, loop)
    kb = os.path.getsize(dst) // 1024
    n = len(os.listdir(fdir))
    print(f"OK {name}: {n} frames -> {os.path.basename(dst)} ({kb} KB, loop={loop})")
    return True


if __name__ == "__main__":
    args = sys.argv[1:] or list(MAP)
    for arg in args:
        process(arg)
    print("VIDEO KEYING DONE")
