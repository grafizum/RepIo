"""Package one exercise's rendered frames into the app deliverables (PRODUCTION_PLAN.md A.5).

  python tools_package.py <slug>

21 frames = a symmetric loop rendered once (ping-pong: 1..21 then 20..2 -> 40 frames @ 20 fps);
any other count is played as-is. Writes into output/<slug>/:
  clip.mp4          app clip: stacked alpha H.264 - left colour (premultiplied on black), right
                    alpha as luma; the figure crop scaled to 512 px tall, 20 fps, one I-frame
  clip.webp         fallback: animated WebP with alpha, same crop/size
  poster.webp       still of the first frame (placeholder while the clip decodes)
  preview.mp4       review copy on RepIo dark #121212 (not shipped)
  contact-sheet.png 12 frames on #121212
  loop-check.txt    seam vs. neighbour steps, sizes
"""
import glob
import os
import shutil
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

A = "/home/matrix/StudioProjects/RepIo/animation"
BG = (18, 18, 18)
HEIGHT, FPS = 512, int(os.environ.get("FPS", "20"))
slug = sys.argv[1]
out = f"{A}/output/{slug}"
src = sorted(glob.glob(f"{out}/frames/*.png"))
assert len(src) >= 2, "no frames"
seq = src + src[-2:0:-1] if len(src) == 21 else src

imgs = [Image.open(f).convert("RGBA") for f in seq]
# union bounding box of everything visible, + margin, then scale to HEIGHT (even width)
boxes = [im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox() for im in imgs]
x0 = min(b[0] for b in boxes); y0 = min(b[1] for b in boxes)
x1 = max(b[2] for b in boxes); y1 = max(b[3] for b in boxes)
m = int(0.04 * max(x1 - x0, y1 - y0))
W, H = imgs[0].size
box = (max(0, x0 - m), max(0, y0 - m), min(W, x1 + m), min(H, y1 + m))
bw, bh = box[2] - box[0], box[3] - box[1]
tw = max(2, int(round(bw * HEIGHT / bh / 2)) * 2)
crops = [im.crop(box).resize((tw, HEIGHT), Image.LANCZOS) for im in imgs]

tmp = tempfile.mkdtemp(prefix="pkg_", dir=f"{A}/renders")
for i, im in enumerate(crops):
    a = np.asarray(im, dtype=np.float32) / 255.0
    rgb = a[..., :3] * a[..., 3:4]                                   # premultiplied on black
    alpha = np.repeat(a[..., 3:4], 3, axis=2)
    stacked = np.concatenate([rgb, alpha], axis=1)
    Image.fromarray((stacked * 255 + 0.5).astype(np.uint8)).save(f"{tmp}/s{i:04d}.png")
    bg = Image.new("RGBA", im.size, BG + (255,))
    bg.alpha_composite(im)
    bg.convert("RGB").save(f"{tmp}/p{i:04d}.png")

x264 = ["-c:v", "libx264", "-preset", "veryslow", "-crf", "24", "-g", "999", "-pix_fmt", "yuv420p",
        "-movflags", "+faststart", "-an"]
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS), "-i", f"{tmp}/s%04d.png", *x264,
                f"{out}/clip.mp4"], check=True)
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS), "-i", f"{tmp}/p%04d.png",
                "-c:v", "libx264", "-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p", "-movflags", "+faststart",
                f"{out}/preview.mp4"], check=True)
crops[0].save(f"{out}/clip.webp", save_all=True, append_images=crops[1:], duration=1000 // FPS, loop=0,
              quality=70, alpha_quality=65, method=6, minimize_size=True)
crops[0].save(f"{out}/poster.webp", quality=80, alpha_quality=70, method=6)
shutil.rmtree(tmp)

s = 300
sheet = Image.new("RGB", (6 * s, 2 * s), BG)
for k in range(12):
    t = crops[round(k * len(crops) / 12) % len(crops)].copy()
    t.thumbnail((s, s), Image.LANCZOS)
    sheet.paste(t, ((k % 6) * s + (s - t.width) // 2, (k // 6) * s + (s - t.height) // 2), t)
sheet.save(f"{out}/contact-sheet.png")

arr = [np.asarray(im, dtype=np.float32) for im in crops]
steps = [np.abs(arr[i + 1] - arr[i]).mean() for i in range(len(arr) - 1)]
seam = np.abs(arr[0] - arr[-1]).mean()
kb = lambda p: os.path.getsize(p) / 1024
rep = (f"frames delivered: {len(crops)} ({len(src)} rendered) @ {FPS} fps, {tw}x{HEIGHT}\n"
       f"mean step: {np.mean(steps):.3f}  max step: {max(steps):.3f}  seam: {seam:.3f}  "
       f"seam ok: {seam <= max(steps) * 1.05}\n"
       f"clip.mp4 {kb(f'{out}/clip.mp4'):.1f} KB  clip.webp {kb(f'{out}/clip.webp'):.1f} KB  "
       f"poster.webp {kb(f'{out}/poster.webp'):.1f} KB  preview.mp4 {kb(f'{out}/preview.mp4'):.1f} KB\n")
open(f"{out}/loop-check.txt", "w").write(rep)
print(rep)
