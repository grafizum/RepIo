#!/usr/bin/env python3
"""Remake the DARK-MODE launcher icon with AI, and show it next to what exists.

    ~/sdgen/venv/bin/python tools/icon_ai_night.py [--seeds N] [--out DIR]

WHY IMG2IMG AND NOT A FRESH ICON
---------------------------------
The owner asked for the dark icon to be remade with AI, but also to be the SAME
icon at the SAME sizes with only the colours changed. Those two requirements rule
out generating a new icon from scratch: SDXL would invent a different mark, and
"the same icon" would be a lie. So this uses the real artwork as the img2img
init image at a strength low enough to keep the mark, its bevel and its shadow,
and lets the model push the palette to dark. That is the only honest way to get
an AI result that is still this app's icon.

THE PROMPT RULES ARE THE ONES THE IMAGE PIPELINE ALREADY LEARNED
----------------------------------------------------------------
Written into the sdgen script after six passes fought each other:
  * The subject goes FIRST and art direction after. SDXL reads only the first 77
    CLIP tokens of each encoder and silently drops the rest, so whatever is last
    is what gets thrown away - which is how 8 squat renders once came out
    byte-identical.
  * The NEGATIVE never blocks the subject. No "robot, metal, android", no
    "barbell" - the mark IS a barbell. It blocks defects and the light-mode look
    instead.
  * Over-describing the subject blocks it as effectively as forbidding it, so the
    palette words are terse and the shadow is described once, not four times.

THE OUTPUT IS A PREVIEW, NOT A DROP-IN
---------------------------------------
Nothing here is written into `res/`. The generated PNGs land in build/reports/
and the sheet is assembled at the real launcher sizes (48/72/96/144/192) with
the adaptive icon composited the way Android composites it: flat background
colour, foreground on top, masked to the middle 75%.
"""
from __future__ import annotations

import argparse
import re
import sys
import time
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp/src/main/res"

# The launcher sizes the real icons ship at, per density folder.
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
# The foreground drawable is 2.25x the launcher size: the adaptive icon is a
# 108dp canvas of which only the middle 72dp is ever visible.
FG_SCALE = 2.25

DARK_BG = (0x12, 0x12, 0x12, 255)
LIGHT_BG = (0xFF, 0xFF, 0xFF, 255)

# Subject first, palette and finish after - see the header.
PROMPT = (
    "app icon, barbell weight mark, dark charcoal plate, warm amber orange "
    "mark, glossy 3d render, soft studio lighting, drop shadow, centered, "
    "square, minimal, high contrast"
)
NEGATIVE = (
    "white background, light plate, photo, photograph, text, letters, watermark, "
    "blurry, low quality, cropped, cluttered, multiple objects, hands, gym interior"
)


def load_font(px: int, bold: bool = False) -> Image.Image.Image:
    names = (
        ["DejaVuSans-Bold.ttf", "LiberationSans-Bold.ttf"] if bold
        else ["DejaVuSans.ttf", "LiberationSans-Regular.ttf"]
    )
    for name in names:
        for base in ("/usr/share/fonts/truetype/dejavu/", "/usr/share/fonts/truetype/liberation/"):
            p = Path(base) / name
            if p.exists():
                return ImageFont.truetype(str(p), px)
    return ImageFont.load_default()


def read_colors() -> dict[str, tuple[int, int, int, int]]:
    xml = (RES / "values/colors.xml").read_text()
    return {
        m.group(1): tuple(int(m.group(2)[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
        for m in re.finditer(r'<color name="([^"]+)">#([0-9A-Fa-f]{6})</color>', xml)
    }


def launcher_composite(fg: Image.Image, bg: tuple[int, int, int, int], size: int) -> Image.Image:
    """Draw the adaptive icon the way Android does: flat bg, foreground, 75% crop."""
    canvas = Image.new("RGBA", (size, size), bg)
    scaled = fg.resize((size, size), Image.LANCZOS)
    canvas.alpha_composite(scaled)
    # The visible 72dp of a 108dp canvas, centred: crop the outer 18% total.
    inset = int(size * 0.125)
    return canvas.crop((inset, inset, size - inset, size - inset))


def generate(out: Path, seeds: int) -> list[Path]:
    import torch
    from diffusers import StableDiffusionXLPipeline

    print("Loading SDXL base (cached in ~/.cache/huggingface)...")
    t0 = time.time()
    pipe = StableDiffusionXLPipeline.from_pretrained(
        "stabilityai/stable-diffusion-xl-base-1.0",
        torch_dtype=torch.float16,
        variant="fp16",
        use_safetensors=True,
    )
    vram_gb = torch.cuda.get_device_properties(0).total_memory / 1024**3
    if vram_gb < 12:
        print(f"{vram_gb:.1f} GB VRAM: CPU offload")
        pipe.enable_model_cpu_offload()
    else:
        pipe = pipe.to("cuda")
    pipe.enable_attention_slicing()
    print(f"loaded in {time.time() - t0:.0f}s")

    src = Image.open(RES / "drawable-xxxhdpi/ic_launcher_fg_orange.png").convert("RGBA")
    src = src.resize((1024, 1024), Image.LANCZOS)

    written: list[Path] = []
    for i in range(seeds):
        # Low strength on purpose: the mark and its shadow must survive. High
        # strength drifts into a different icon, which is the one thing the owner
        # explicitly does not want.
        strength = 0.32 + 0.12 * i
        t = time.time()
        result = pipe(
            prompt=PROMPT,
            negative_prompt=NEGATIVE,
            image=src,
            strength=strength,
            num_inference_steps=40,
            generator=torch.Generator("cuda").manual_seed(20342067 + i),
        ).images[0]
        path = out / f"ai-night-orange-seed{i}-s{strength:.2f}.png"
        result.convert("RGBA").save(path)
        print(f"  seed {i} strength {strength:.2f} -> {path.name} ({time.time() - t:.0f}s)")
        written.append(path)
    return written


def sheet(out: Path, ai_paths: list[Path]) -> Path:
    colors = read_colors()
    fg_light = Image.open(RES / "drawable-xxxhdpi/ic_launcher_fg_orange.png").convert("RGBA")
    fg_dark = Image.open(RES / "drawable-night-xxxhdpi/ic_launcher_fg_orange.png").convert("RGBA")

    rows = [
        ("Light mode today (white plate)", launcher_composite(fg_light, LIGHT_BG, 192)),
        ("Dark mode today (what you called horrible)", launcher_composite(fg_dark, DARK_BG, 192)),
    ]
    ai_icons = []
    for p in ai_paths:
        icon = launcher_composite(Image.open(p).convert("RGBA"), DARK_BG, 192)
        rows.append((f"AI remake - {p.stem.split('-')[-1]}", icon))
        ai_icons.append(icon)

    W = 980
    row_h = 250
    H = 70 + row_h * len(rows) + 150
    img = Image.new("RGBA", (W, H), (0x14, 0x14, 0x18, 255))
    d = ImageDraw.Draw(img)
    d.text((28, 18), "RepIo launcher icon - dark mode", font=load_font(24, True), fill=(255, 255, 255))
    d.text((28, 50),
           "Each row shown at the real 192px launcher size, composited and masked the way "
           "Android composites an adaptive icon.",
           font=load_font(13), fill=(0x9A, 0x9A, 0xA5))

    for i, (label, icon) in enumerate(rows):
        y = 80 + i * row_h
        d.text((28, y + 80), label, font=load_font(15, True), fill=(0xE8, 0xE8, 0xEE))
        # Big preview plus the five real densities, so "does it read at launcher
        # size" is answerable from the sheet itself.
        img.alpha_composite(icon, (30, y))
        x = 250
        for density, px in DENSITIES.items():
            small = icon.resize((px, px), Image.LANCZOS)
            img.alpha_composite(small, (x, y + (192 - px) // 2))
            d.text((x, y + 198), f"{density} {px}px", font=load_font(11), fill=(0x88, 0x88, 0x92))
            x += px + 26

    y = 80 + len(rows) * row_h
    d.text((28, y + 8), "The generated files are in build/reports - nothing has been written into res/ yet.",
           font=load_font(13), fill=(0x9A, 0x9A, 0xA5))
    d.text((28, y + 30), f"plate dark {DARK_BG[:3]} (icon_bg_dark)   plate light {LIGHT_BG[:3]} (icon_bg_white)",
           font=load_font(12), fill=(0x77, 0x77, 0x80))

    path = out / "icon-night-remake-preview.png"
    img.convert("RGB").save(path, "PNG")
    return path


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--seeds", type=int, default=3)
    ap.add_argument("--out", default=str(REPO / "build/reports"))
    args = ap.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    ai = generate(out, args.seeds)
    path = sheet(out, ai)
    print(f"preview: {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
