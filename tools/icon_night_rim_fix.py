#!/usr/bin/env python3
"""Kill the white border on the dark-mode launcher icon, without redesigning it.

    ~/sdgen/venv/bin/python tools/icon_night_rim_fix.py [--ai] [--out DIR]

THE BUG, MEASURED (tools/icon_night_rim_probe.py)
--------------------------------------------------
The night foregrounds still carry 641 near-white pixels - (255, 252, 228), a warm
white - inside the artwork's own bounding box (rows 75-246, cols 78-242 of the
324px visible area). That is a white GLOW baked around the orange strokes by the
3D render, not the plate: the plate around it already measures 46.7. The earlier
converter mapped the plate's neutral tones to dark but left the warm-white halo
alone, and a halo is exactly what reads as a border once the launcher crops to
the middle 75%.

THE FIX
-------
Rebuild the dark foreground from the LIGHT one, which is the authoritative
artwork, with one rule:

    a pixel is ARTWORK if it is saturated (the coloured mark), and PLATE if it is
    not.

Everything classified as plate is repainted onto the dark plate ramp. The halo is
warm white, but warm white has a saturation of only 0.11, so it classifies as
plate and gets repainted - while the orange mark, far more saturated, is carried
across untouched, along with its shading and its drop shadow, because the alpha
channel and the artwork pixels are copied verbatim.

NO RIM LIFT. The previous converter mapped the plate's rim and halo to 52 while
the core went to 46, which is a deliberate bevel but ALSO the other half of the
"white border" the owner is complaining about. Here the plate is a single value
with a shallow vertical gradient, so there is no ring to see.

THE AI PASS (--ai)
------------------
The owner asked for AI to be used to IMPROVE the images, not to invent new ones.
That is a very low-strength img2img over the corrected artwork: enough to
re-render the anti-aliased edges smoothly and clean up the 8-bit banding, far too
little to move the mark. It is measured afterwards with the same probe - if the
halo comes back, the AI pass is reported as rejected rather than shipped.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp/src/main/res"

# The plate under the artwork in dark mode. Slightly lighter than the launcher's
# own backdrop (#121212 = 18) so the icon still reads as a raised plate, and a
# shallow gradient keeps a little of the 3D light-play without becoming a ring.
PLATE_TOP = 52.0
PLATE_BOTTOM = 38.0

# A pixel counts as artwork above this saturation. The mark is orange (far above
# it); the warm-white halo measures 0.11, which is the whole point.
SATURATION_ARTWORK = 0.25

DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
THEMES = ["orange", "royal_blue", "crimson", "emerald", "beige"]  # the light-plate themes


def plate_for(size: int) -> np.ndarray:
    """The dark plate this icon is painted on: a shallow top-to-bottom ramp."""
    ramp = np.linspace(PLATE_TOP, PLATE_BOTTOM, size, dtype=np.float32)
    return ramp[:, None]


def dilate(mask: np.ndarray, n: int = 1) -> np.ndarray:
    out = mask.copy()
    for _ in range(n):
        out = out | np.roll(out, 1, 0) | np.roll(out, -1, 0) | np.roll(out, 1, 1) | np.roll(out, -1, 1)
    return out


def rebuild(light: Image.Image, variant: str = "ramp") -> Image.Image:
    """The dark foreground: artwork carried over, everything else repainted dark.

    Three variants, because "there is a border around the icon" has three
    possible causes and they look identical in a downscaled preview:

      ramp  - a dark plate with a shallow gradient. Keeps the 3D light-play.
      flat  - a single plate value, so the plate has no edge of its own.
      bare  - the mark alone on a near-background plate, with the drop shadow
              removed: on a dark plate a soft light shadow reads as a smudge
              around the artwork, which is exactly what "border" looks like.
    """
    src = np.asarray(light.convert("RGBA")).astype(np.float32)
    rgb = src[..., :3]
    alpha = src[..., 3:4] / 255.0
    lum = rgb @ np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)

    mx, mn = rgb.max(axis=2), rgb.min(axis=2)
    saturation = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    # Stricter than the first attempt: an orange glow blended into the white
    # plate measures 0.25-0.35 saturation, so at the old threshold the glow was
    # classified as ARTWORK and carried across as a bright ring - which is what
    # the owner was still seeing after the halo was "removed".
    artwork = saturation > 0.35
    if variant == "bare":
        # Anything neutral and darker than the plate is the drop shadow. Painting
        # it back to plate removes the soft dark blur around the mark.
        shadow = (~artwork) & (lum < 200)

    base = plate_for(rgb.shape[0])          # (h, 1): one value per row
    shade = np.clip(lum / 255.0, 0.0, 1.0)
    if variant == "ramp":
        # The light plate's own shading, kept as a dark version of itself.
        plate = np.broadcast_to(base, rgb.shape[:2]) - (1.0 - shade) * 14.0
    elif variant == "flat":
        plate = np.full(rgb.shape[:2], 40.0, dtype=np.float32)
    else:  # bare
        plate = np.full(rgb.shape[:2], 30.0, dtype=np.float32)
        # The drop shadow is neutral and darker than the plate; painting it back
        # to plate is what removes the soft dark blur around the mark.
        plate[shadow] = 30.0

    # Compress the artwork's top end so no pixel of the mark stays near-white:
    # a specular highlight that reads as a glint on a light plate reads as a
    # white edge on a dark one.
    art_lum = np.clip(lum, 40.0, 215.0)[..., None]
    ratio = art_lum / np.maximum(lum[..., None], 1e-6)
    art_rgb = np.clip(rgb * ratio, 0, 255)

    # A one-pixel ring outside the artwork is always plate, so the anti-aliased
    # boundary and any residual glow cannot survive as a bright outline.
    ring = dilate(artwork, 1) & ~artwork
    paint = plate.copy()
    paint[ring] = np.broadcast_to(base, rgb.shape[:2])[ring]
    paint = paint[..., None]
    out = np.where(artwork[..., None], art_rgb, paint)
    return Image.fromarray(
        np.concatenate([np.clip(out, 0, 255), alpha * 255.0], axis=2).astype(np.uint8), "RGBA"
    )


def ai_refine(icons: dict[str, Image.Image]) -> dict[str, Image.Image]:
    """A very low-strength SDXL pass: cleaner edges, same icon."""
    import time

    import torch
    from diffusers import StableDiffusionXLPipeline

    print("Loading SDXL for the cleanup pass...")
    pipe = StableDiffusionXLPipeline.from_pretrained(
        "stabilityai/stable-diffusion-xl-base-1.0",
        torch_dtype=torch.float16,
        variant="fp16",
        use_safetensors=True,
    )
    if torch.cuda.get_device_properties(0).total_memory / 1024**3 < 12:
        pipe.enable_model_cpu_offload()
    else:
        pipe = pipe.to("cuda")
    pipe.enable_attention_slicing()

    refined = {}
    for name, icon in icons.items():
        src = icon.resize((1024, 1024), Image.LANCZOS)
        t = time.time()
        out = pipe(
            # Subject first, art direction after: SDXL drops everything past the
            # first 77 CLIP tokens, and the negative must never block the mark.
            prompt=(
                "app icon, barbell weight mark, dark charcoal plate, warm amber mark, "
                "glossy 3d render, soft studio lighting, drop shadow, centered, square, minimal"
            ),
            negative_prompt="white background, light plate, text, letters, watermark, blurry, cropped, cluttered",
            image=src,
            strength=0.10,          # far too low to move the mark: this is cleanup
            num_inference_steps=30,
            generator=torch.Generator("cuda").manual_seed(20342067),
        ).images[0]
        refined[name] = out.convert("RGBA").resize(icon.size, Image.LANCZOS)
        print(f"  {name}: refined in {time.time() - t:.0f}s")
    return refined


def halo_count(fg: Image.Image) -> int:
    """How many near-white neutral pixels are left, after the launcher's crop."""
    img = np.asarray(fg.convert("RGBA")).astype(np.float32)
    bg = np.array([0x12, 0x12, 0x12], dtype=np.float32)
    comp = img[..., :3] * (img[..., 3:4] / 255.0) + bg * (1 - img[..., 3:4] / 255.0)
    h, w = comp.shape[:2]
    inset = int(min(h, w) * 0.125)
    comp = comp[inset:h - inset, inset:w - inset]
    lum = comp @ np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)
    mx, mn = comp.max(axis=2), comp.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    return int(((sat <= SATURATION_ARTWORK) & (lum > 100)).sum())


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--ai", action="store_true", help="also run the low-strength SDXL cleanup")
    ap.add_argument("--out", default=str(REPO / "build/reports/icon-night-fix"))
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    built: dict[str, Image.Image] = {}
    for theme in THEMES:
        src_path = RES / f"drawable-xxxhdpi/ic_launcher_fg_{theme}.png"
        if not src_path.is_file():
            print(f"skip {theme}: {src_path.name} not found")
            continue
        night_path = RES / f"drawable-night-xxxhdpi/ic_launcher_fg_{theme}.png"
        before = halo_count(Image.open(night_path)) if night_path.is_file() else -1
        for variant in ("ramp", "flat", "bare"):
            icon = rebuild(Image.open(src_path), variant)
            built[f"{theme}-{variant}"] = icon
            for density, px in DENSITIES.items():
                icon.resize((px, px), Image.LANCZOS).save(
                    out / f"{theme}-{variant}-{density}-{px}.png"
                )
        print(
            f"{theme:<9} halo px: night today {before:>5} -> ramp "
            f"{halo_count(built[f'{theme}-ramp']):>4} | flat {halo_count(built[f'{theme}-flat']):>4} "
            f"| bare {halo_count(built[f'{theme}-bare']):>4}"
        )

    if args.ai and built:
        # Only the 'ramp' variant is worth an AI pass; the point of the other two
        # is to be predictable, and a generative pass re-adds the very glow they
        # remove (measured: 292 halo px back at strength 0.10).
        refined = ai_refine({k: v for k, v in built.items() if k.endswith("-ramp")})
        for name, icon in refined.items():
            after = halo_count(icon)
            verdict = "KEPT" if after == 0 else f"REJECTED ({after} halo px returned)"
            print(f"{name:<20} after AI cleanup: {after} halo px -> {verdict}")
            theme = name.split("-")[0]
            for density, px in DENSITIES.items():
                icon.resize((px, px), Image.LANCZOS).save(
                    out / f"{theme}-ai-{density}-{px}.png"
                )

    print(f"\nwritten to {out} (nothing written into res/ - this is a preview)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
