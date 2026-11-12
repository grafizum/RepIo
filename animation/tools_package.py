"""Package one exercise's PNG frames into the deliverables.

  python tools_package.py <slug> [reference_frame.png]

Writes into output/<slug>/:
  render.webm         VP9 with alpha, loops seamlessly (for apps/web)
  render.mp4          H.264 on RepDB navy (fallback, no alpha)
  render.webp         animated transparent WebP, 540 px (the format RepDB ships)
  contact-sheet.png   12 evenly spaced frames on navy
  compare.png         our frame next to the RepDB reference, same scale
  loop-check.txt      last->first frame difference vs the average step (seam test)
"""
import glob
import subprocess
import sys

import numpy as np
from PIL import Image

A = "/home/matrix/StudioProjects/RepIo/animation"
NAVY = (18, 18, 18)  # RepIo dark background (#121212)
slug = sys.argv[1]
ref = sys.argv[2] if len(sys.argv) > 2 else None
out = f"{A}/output/{slug}"
frames = sorted(glob.glob(f"{out}/frames/*.png"))
n = len(frames)
assert n >= 2, "no frames"

subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", "30", "-i", f"{out}/frames/%04d.png",
                "-c:v", "libvpx-vp9", "-pix_fmt", "yuva420p", "-b:v", "0", "-crf", "24", "-row-mt", "1",
                f"{out}/render.webm"], check=True)
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "color=c=0x121212:s=1080x1080:r=30",
                "-framerate", "30", "-i", f"{out}/frames/%04d.png",
                "-filter_complex", "[0][1]overlay=shortest=1,format=yuv420p",
                "-c:v", "libx264", "-crf", "18", "-movflags", "+faststart", f"{out}/render.mp4"], check=True)

imgs = [Image.open(f).convert("RGBA") for f in frames]
small = [im.resize((540, 540), Image.LANCZOS) for im in imgs]
small[0].save(f"{out}/render.webp", save_all=True, append_images=small[1:], duration=1000 // 30, loop=0,
              lossless=False, quality=88, method=5)

picks = [round(i * n / 12) for i in range(12)]
s = 300
sheet = Image.new("RGB", (6 * s, 2 * s), NAVY)
for k, i in enumerate(picks):
    t = imgs[i].resize((s, s), Image.LANCZOS)
    sheet.paste(t, ((k % 6) * s, (k // 6) * s), t)
sheet.save(f"{out}/contact-sheet.png")

if ref:
    h = 720
    pair = [imgs[picks[6]], Image.open(ref).convert("RGBA")]
    tiles = []
    for im in pair:
        im = im.crop(im.getbbox())
        im.thumbnail((h - 40, h - 40), Image.LANCZOS)
        tiles.append(im)
    cmp_ = Image.new("RGB", (2 * h, h), NAVY)
    for i, t in enumerate(tiles):
        cmp_.paste(t, (i * h + (h - t.width) // 2, (h - t.height) // 2), t)
    cmp_.save(f"{out}/compare.png")

arr = [np.asarray(im, dtype=np.float32) for im in imgs]
steps = [np.abs(arr[i + 1] - arr[i]).mean() for i in range(n - 1)]
seam = np.abs(arr[0] - arr[-1]).mean()
with open(f"{out}/loop-check.txt", "w") as f:
    f.write(f"frames: {n}\nmean step between neighbours: {np.mean(steps):.3f}\nmax step: {max(steps):.3f}\n"
            f"seam (last -> first): {seam:.3f}\nseam within normal step range: {seam <= max(steps) * 1.05}\n")
print(open(f"{out}/loop-check.txt").read())
