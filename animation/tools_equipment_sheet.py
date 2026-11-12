"""Labelled contact sheet + markdown list of the equipment library.

  .venv/bin/python tools_equipment_sheet.py
Reads blender/equipment.py's LIBRARY (by parsing, no bpy) and output/equipment-scan.json.
"""
import ast
import json
import os

from PIL import Image, ImageDraw, ImageFont

A = "/home/matrix/StudioProjects/RepIo/animation"
BG = (18, 18, 18)
src = open(f"{A}/blender/equipment.py").read()
lib_node = [n for n in ast.parse(src).body if isinstance(n, ast.Assign) and getattr(n.targets[0], "id", "") == "LIBRARY"][0]
lib = [(e.elts[0].value, e.elts[1].value, e.elts[3].value) for e in lib_node.value.elts]
scan = json.load(open(f"{A}/output/equipment-scan.json"))["assets"]


def font(sz):
    for f in ("/usr/share/fonts/TTF/DejaVuSans.ttf", "/usr/share/fonts/noto/NotoSans-Regular.ttf"):
        if os.path.exists(f):
            return ImageFont.truetype(f, sz)
    return ImageFont.load_default()


cols, tile, lab = 8, 300, 46
items = [(aid, label) for aid, label, _ in lib if os.path.exists(f"{A}/renders/equipment/{aid}.png")]
rows = (len(items) + cols - 1) // cols
sheet = Image.new("RGB", (cols * tile, rows * (tile + lab)), BG)
d = ImageDraw.Draw(sheet)
f = font(19)
for i, (aid, label) in enumerate(items):
    im = Image.open(f"{A}/renders/equipment/{aid}.png").convert("RGBA")
    bb = im.getbbox()
    if bb:
        im = im.crop(bb)
    im.thumbnail((tile - 24, tile - 24), Image.LANCZOS)
    x, y = (i % cols) * tile, (i // cols) * (tile + lab)
    sheet.paste(im, (x + (tile - im.width) // 2, y + (tile - im.height) // 2), im)
    w = d.textlength(label, font=f)
    d.text((x + (tile - w) / 2, y + tile + 6), label, fill=(200, 200, 205), font=f)
sheet.save(f"{A}/output/equipment/equipment-sheet.png")

lines = ["# Equipment library (Sandstone)", "",
         "Every piece needed by the app's 601 exercises. Counts come from output/equipment-scan.json; "
         "cable attachments, calf-raise and plate-loaded machines share one scan group, so they show the "
         "group's count.", "",
         "| Asset id | Equipment | Exercises | Examples |", "|---|---|---:|---|"]
for aid, label, key in lib:
    ex = scan.get(key, [])
    lines.append(f"| `{aid}` | {label} | {len(ex)} | {', '.join(ex[:4])}{' …' if len(ex) > 4 else ''} |")
lines += ["", f"Total assets: {len(lib)}. Renders: renders/equipment/<id>.png; files: assets/equipment/<id>.blend; "
          "all in one: assets/equipment/library.blend."]
open(f"{A}/output/equipment/equipment-list.md", "w").write("\n".join(lines) + "\n")
print("sheet", len(items), "tiles; list", len(lib), "rows")
