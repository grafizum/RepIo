"""Side-by-side sheet on RepDB's navy so a render can be judged against the reference by eye."""
import sys
from PIL import Image
out, *paths = sys.argv[1:]
h = int(__import__("os").environ.get("TILE", 640))
tiles = []
for p in paths:
    im = Image.open(p).convert("RGBA")
    bb = im.getbbox() or (0, 0, *im.size)
    im = im.crop(bb)
    im.thumbnail((h, h - 20), Image.LANCZOS)
    tiles.append(im)
sheet = Image.new("RGB", (h * len(tiles), h), (18, 18, 18))
for i, t in enumerate(tiles):
    sheet.paste(t, (i * h + (h - t.width) // 2, (h - t.height) // 2), t)
sheet.save(out)
