#!/usr/bin/env python3
import os
from collections import deque
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "assets-raw")
OUT = os.path.join(ROOT, "app", "src", "assets", "neelu")
os.makedirs(OUT, exist_ok=True)

TARGET = (255, 0, 255)
NEAR = 105
FEATHER = 150

def dist(c, t):
    return ((c[0]-t[0])**2 + (c[1]-t[1])**2 + (c[2]-t[2])**2) ** 0.5

def key(src, dst, max_size=640):
    img = Image.open(src).convert("RGBA")
    img.thumbnail((max_size, max_size), Image.LANCZOS)
    w, h = img.size
    px = img.load()

    near = [[dist(px[x, y], TARGET) <= NEAR for x in range(w)] for y in range(h)]
    bg = [[False]*w for _ in range(h)]
    q = deque()
    for x in range(w):
        for y in (0, h-1):
            if near[y][x] and not bg[y][x]:
                bg[y][x] = True; q.append((x, y))
    for y in range(h):
        for x in (0, w-1):
            if near[y][x] and not bg[y][x]:
                bg[y][x] = True; q.append((x, y))
    while q:
        x, y = q.popleft()
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            nx, ny = x+dx, y+dy
            if 0 <= nx < w and 0 <= ny < h and near[ny][nx] and not bg[ny][nx]:
                bg[ny][nx] = True; q.append((nx, ny))

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if bg[y][x]:
                px[x, y] = (r, g, b, 0)
            else:
                d = dist((r, g, b), TARGET)
                if d < FEATHER:
                    touch = any(bg[ny][nx] for dx, dy in ((1,0),(-1,0),(0,1),(0,-1),(1,1),(-1,-1),(1,-1),(-1,1))
                                for nx, ny in ((x+dx, y+dy),) if 0 <= nx < w and 0 <= ny < h)
                    if touch:
                        alpha = int(min(255, max(0, (d - NEAR) / (FEATHER - NEAR) * 255)))
                        px[x, y] = (r, g, b, min(a, alpha))
    img.save(dst, "PNG", optimize=True)
    print(f"{os.path.basename(dst)}: {img.size} ({os.path.getsize(dst)//1024} KB)")

for f in sorted(os.listdir(RAW)):
    if f.startswith("neelu-") or f == "icon.png":
        key(os.path.join(RAW, f), os.path.join(OUT, f))
print("KEYING DONE")
