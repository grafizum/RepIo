"""One-off: rebuild the dark-theme launcher icon variants (v4.1 — plate swap).

Task: 'The icon change on dark theme needs to have a dark / blackish background
but make sure that the shadow/light play is still visible to give it the 3d
effect like on the light theme.' — and after v1: 'make it dark completely,
not a cheap photoshop'.

The light icon's structure, measured on the xxxhdpi sources (alpha masks are
identical across variants):

  plate fill   opaque neutral, flat lum ~253
  rim ring     opaque neutral lum 255, at 2-5 px depth (the light-play line)
  gaps         enclosed lum-255 whites BETWEEN artwork strokes — the plate
               showing through the line-art glyph (negative space, not paint)
  shading      interior opaque neutrals lum 200-248 (soft inner shadows)
  bevel AA     outer semi-neutral, alpha >= 160, stored lum ~231
  transition   outer semi-neutral, alpha 80..160, stored lum ~170
  drop shadow  outer semi-neutral, alpha < 80, stored lum ~131
  artwork      colored strokes (channel spread >= 25); retro is grayscale

So EVERY neutral pixel is plate surface; the artwork is exactly the saturated
pixels. That makes the dark rendition an exact, linear per-role mapping —
no region growing, no flood fill, nothing heuristic:

  fill 253 -> 46 (#2E2E2E, reads against the #121212 ground)
  rim/gaps 255 -> 52        (light-play line and stroke gaps survive,
                             keeping their brighter-than-fill relationship)
  shading   -> 46*lum/253   (linear: interior soft shadows keep their depth)
  bevel AA  -> 26*lum/231   (crease direction preserved)
  transition-> 20*lum/170
  shadow    -> min(lum, 30) (only ever darkens; alpha kept)
  artwork   -> untouched    (strokes keep every pixel, incl. their AA via
                             un-mixing the white plate out of semi pixels)

Because the mapping is linear in lum, blends map exactly: an AA pixel between
two plate tones maps to the same blend of the mapped tones.

Retro's artwork is DARK neutral lines: on a dark plate they would vanish, so
inside the glyph (beyond the outer edge band) lines invert — the classic
dark-mode treatment — with the same relative contrast:
  line lum -> 46*(2 - lum/253), clamped to 12..92.

Outer-edge semi neutrals (the bevel/transition/shadow bands) are recognised
GEOMETRICALLY: within 10 px (Chebyshev) of the outermost alpha boundary.
Alpha is never modified, so every edge stays exactly as smooth as the
original. Composited over #121212, nothing neutral exceeds ~52 (retro: 92),
so no residue can remain by construction.

DARK_VARIANTS is the source of truth: the five themes Theme.kt reports as
dark. Light variants (beige, emerald, crimson, royal_blue) are never touched.

Run from the repo root:  python tools/icon-dark-variants.py [--write]
Without --write it prints reports and previews only (dry run).
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp" / "src" / "main" / "res"

# Theme.kt: BEIGE/EMERALD/CRIMSON/ROYAL_BLUE -> light; these five -> dark.
DARK_COLORED = ["flame", "ocean", "rose", "purple"]
DARK_INVERTED = ["retro"]  # grayscale line art: dark lines must become light
DARK_VARIANTS = DARK_COLORED + DARK_INVERTED

GROUND = 18.0  # the in-app dark ground (#121212), used in previews/metrics

# Dark plate design (measured light structure -> dark counterpart).
PLATE_CORE = 46.0   # plate fill 253 -> 46
PLATE_RIM = 52.0    # rim ring + stroke gaps 255 -> 52
CREASE = 26.0       # bevel AA (stored ~231) -> ~26, scaled with stored lum
TRANSITION = 20.0   # alpha 80..160 band -> ~20
SHADOW_MAX = 30.0   # drop shadow: stored lum clamped to this, alpha kept

LIGHT_FILL = 253.0  # measured plate fill of the light icons
LIGHT_RIM = 254.0   # >= this is rim/gap white
SAT_ART = 25.0      # >= this channel spread means colored artwork
EDGE_BAND = 10.0    # px (Chebyshev) from the outer alpha boundary = outer band
RETRO_LINE_MAX = 200.0  # retro: darker than this = artwork line, invert


def _shifts3(a):
    h, w = a.shape
    ap = np.pad(a, 1, mode="edge")
    return np.stack([
        ap[dy:dy + h, dx:dx + w]
        for dy in (0, 1, 2) for dx in (0, 1, 2)
    ])


def distance_to_edge(alpha):
    """Chebyshev distance (in px) from every pixel to the nearest transparent
    one (alpha < 128). The outer bevel/shadow lives within ~10 px of this."""
    dist = np.where(alpha < 128, 0.0, 1e9)
    for _ in range(512):
        nxt = np.minimum(dist, _shifts3(dist).min(0) + 1.0)
        if np.array_equal(nxt, dist):
            return dist
        dist = nxt
    return dist


def plate_map(lum, inverted):
    """Dark counterpart of a plate-surface luminance (opaque semantics).
    Linear in lum, so blends of plate tones map to blends of mapped tones."""
    if inverted:
        # Rim/gap whites stay plate; soft shading scales; dark lines invert
        # with the same relative contrast they had against the light plate.
        line = lum < RETRO_LINE_MAX
        soft = (lum >= RETRO_LINE_MAX) & (lum < LIGHT_RIM)
        rim = lum >= LIGHT_RIM
        out = np.where(line, np.clip(PLATE_CORE * (2.0 - lum / LIGHT_FILL),
                                     12.0, 92.0),
                       np.where(soft, PLATE_CORE * lum / LIGHT_FILL,
                                PLATE_RIM))
        return out, line
    out = np.where(lum >= LIGHT_RIM, PLATE_RIM,
                   np.maximum(12.0, PLATE_CORE * lum / LIGHT_FILL))
    return out, np.zeros_like(lum, dtype=bool)


def convert(img, inverted):
    """Swap the white plate for the dark one, role by role. Alpha untouched."""
    arr = np.asarray(img.convert("RGBA"), dtype=np.float32)
    r, g, b, al = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]
    lum = (r + g + b) / 3.0
    sat = arr[..., :3].max(-1) - arr[..., :3].min(-1)
    neutral = sat < SAT_ART

    out = arr.copy()

    # 1) Colored-art anti-aliasing: stored = a*art + (1-a)*white_plate, so
    #    un-mix the plate out and keep the same alpha over the dark plate.
    colsemi = (~neutral) & (al > 0) & (al < 250)
    a = np.clip(al / 255.0, 0.02, 1.0)[..., None]
    unmixed = np.clip((arr[..., :3] - (1.0 - a) * 255.0) / a, 0.0, 255.0)
    for c in range(3):
        out[..., c] = np.where(colsemi, unmixed[..., c], out[..., c])

    # 2) Neutral opaque: fill / rim / gaps / shading (retro: lines invert).
    n_opq = neutral & (al >= 250)
    mapped, _ = plate_map(lum, inverted)
    for c in range(3):
        out[..., c] = np.where(n_opq, mapped, out[..., c])

    # 3) Neutral semi, split geometrically:
    #    outer band (within EDGE_BAND of the alpha boundary) = the icon's
    #    bevel crease, transition and drop shadow -> dark band mapping;
    #    deeper = interior plate-tone blends -> the same linear map as opaque.
    n_semi = neutral & (al > 0) & (al < 250)
    dist = distance_to_edge(al)
    outer = n_semi & (dist <= EDGE_BAND)
    inner = n_semi & (dist > EDGE_BAND)
    inner_mapped, _ = plate_map(lum, inverted)
    bevel = outer & (al >= 160)
    trans = outer & (al >= 80) & (al < 160)
    shadow = outer & (al < 80)
    bevel_lum = np.clip(CREASE * lum / 231.0, 0.0, 40.0)
    trans_lum = np.clip(TRANSITION * lum / 170.0, 0.0, 30.0)
    shadow_lum = np.minimum(lum, SHADOW_MAX)
    band_lum = np.where(al >= 160, bevel_lum,
                        np.where(al >= 80, trans_lum, shadow_lum))
    for c in range(3):
        out[..., c] = np.where(inner, inner_mapped, out[..., c])
        out[..., c] = np.where(outer, band_lum, out[..., c])

    out[..., 3] = al
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def metrics(original, converted):
    """(edge_max, opaque_neutral_max, art_diff_max) with the result composited
    over #121212.

    edge_max            max luminance over pixels whose ORIGINAL alpha was a
                        semi-neutral pixel — the residue metric; must be ~<=30
    opaque_neutral_max  max luminance over opaque neutral output pixels; the
                        plate maps to <= 52 (retro lines <= 92), so anything
                        far above that would be surviving white — must be small
    art_diff_max        max |rgb| change over originally colored OPAQUE pixels
                        — must be 0 (artwork untouched)
    """
    o = np.asarray(original.convert("RGBA"), dtype=np.float32)
    c = np.asarray(converted.convert("RGBA"), dtype=np.float32)
    sat_o = o[..., :3].max(-1) - o[..., :3].min(-1)
    al_o = o[..., 3]

    a = (c[..., 3] / 255.0)[..., None]
    comp = c[..., :3] * a + GROUND * (1.0 - a)
    lum_c = comp.mean(-1)
    sat_c = comp.max(-1) - comp.min(-1)

    semi_o = (sat_o < SAT_ART) & (al_o > 0) & (al_o < 250)
    edge_max = float(lum_c[semi_o].max()) if semi_o.any() else 0.0

    opq_neutral_out = (c[..., 3] >= 250) & (sat_c < SAT_ART)
    opaque_neutral_max = (
        float(lum_c[opq_neutral_out].max()) if opq_neutral_out.any() else 0.0
    )

    col_opq = (sat_o >= SAT_ART) & (al_o >= 250)
    art_diff = (
        float(np.abs(c[..., :3][col_opq] - o[..., :3][col_opq]).max())
        if col_opq.any() else 0.0
    )
    return edge_max, opaque_neutral_max, art_diff


def ascii_preview(img):
    """Composite over #121212 and render a coarse ASCII map."""
    bg = Image.new("RGBA", img.size, (int(GROUND),) * 3 + (255,))
    comp = Image.alpha_composite(bg, img.convert("RGBA"))
    arr = np.asarray(comp, dtype=np.float32)
    lum = arr[..., :3].mean(-1)
    sat = arr[..., :3].max(-1) - arr[..., :3].min(-1)
    step = max(1, img.size[0] // 48)
    lines = []
    for y in range(0, img.size[1], step):
        row = ""
        for x in range(0, img.size[0], step):
            if sat[y, x] >= SAT_ART:
                row += "A"
            elif lum[y, x] >= 40:
                row += "#"
            elif lum[y, x] >= 24:
                row += "+"
            elif lum[y, x] >= 19:
                row += "."
            else:
                row += " "
        lines.append(row)
    return "\n".join(lines)


def save_preview(results, path):
    """results: {variant: (original, dark)} at the same size — a contact sheet:
    light originals on white, dark rebuilds on #121212."""
    cell, pad = 192, 12
    W = pad + 2 * (cell + pad)
    H = pad + len(results) * (cell + pad)
    canvas = Image.new("RGB", (W, H), (90, 90, 90))
    for i, (variant, (orig, dark)) in enumerate(results.items()):
        y = pad + i * (cell + pad)
        light_cell = Image.new("RGBA", (cell, cell), (255, 255, 255, 255))
        light_cell.alpha_composite(orig.convert("RGBA"))
        dark_cell = Image.new("RGBA", (cell, cell), (int(GROUND),) * 3 + (255,))
        dark_cell.alpha_composite(dark.convert("RGBA"))
        canvas.paste(light_cell.convert("RGB"), (pad, y))
        canvas.paste(dark_cell.convert("RGB"), (pad + cell + pad, y))
    canvas.save(path, "PNG")
    print(f"preview sheet: {path}")


def main(write):
    densities = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]

    # Adaptive-icon backgrounds: white -> dark (idempotent).
    for variant in DARK_VARIANTS:
        xml = RES / "mipmap-anydpi-v26" / f"ic_launcher_{variant}.xml"
        text = xml.read_text(encoding="utf-8")
        if "icon_bg_white" in text:
            print(f"adaptive bg {xml.name}: icon_bg_white -> icon_bg_dark")
            if write:
                xml.write_text(
                    text.replace("@color/icon_bg_white", "@color/icon_bg_dark"),
                    encoding="utf-8",
                )
        else:
            print(f"adaptive bg {xml.name}: already dark")
    if write:
        colors = RES / "values" / "colors.xml"
        ctext = colors.read_text(encoding="utf-8")
        if "icon_bg_dark" not in ctext:
            ctext = ctext.replace(
                '<color name="icon_bg_white">#FFFFFF</color>',
                '<color name="icon_bg_white">#FFFFFF</color>\n'
                '    <color name="icon_bg_dark">#121212</color>',
            )
            colors.write_text(ctext, encoding="utf-8")
            print("colors.xml: added icon_bg_dark (#121212)")

    preview = {}
    for variant in DARK_VARIANTS:
        inverted = variant in DARK_INVERTED
        for sub, prefix in (
            [(f"drawable-{d}", "ic_launcher_fg_") for d in densities]
            + [(f"mipmap-{d}", "ic_launcher_") for d in densities]
        ):
            path = RES / sub / f"{prefix}{variant}.png"
            if not path.exists():
                print(f"MISSING: {path}")
                continue
            original = Image.open(path)
            out = convert(original, inverted)
            edge_max, on_max, art_diff = metrics(original, out)
            print(
                f"{path.relative_to(REPO)}  "
                f"edge-band max {edge_max:5.1f} | "
                f"opaque-neutral max {on_max:5.1f} | "
                f"art diff {art_diff:.0f}"
            )
            if variant == "flame" and sub == "mipmap-xxxhdpi":
                print("--- flame xxxhdpi on #121212 (light original -> dark) ---")
                print(ascii_preview(original))
                print("...")
                print(ascii_preview(out))
            if variant in ("flame", "retro") and sub == "mipmap-xxxhdpi":
                preview[variant] = (original, out)
            if write:
                out.save(path, "PNG")

    if preview:
        save_preview(preview, REPO / "tools" / "icon-dark-preview.png")


if __name__ == "__main__":
    main("--write" in sys.argv)
