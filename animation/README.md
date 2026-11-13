# RepIo exercise animations

Looping 3D exercise clips for the RepIo app, rendered headless in Blender 5.2. Everything for
this work lives in this folder (branch `demo-animation`); nothing here is wired into the app yet.

## Look
"Sandstone" mannequin: warm clay skin `#C9BEAE`, espresso compression shorts `#2C2520`, the
target muscle in RepIo mint `#00BFA5`, warm-charcoal equipment, on RepIo's dark `#121212`.
The faceless head and the body come from Blender Studio's **Human Base Meshes** (CC0).

## Rebuild from scratch
Generated files (`*.blend`, renders, frames) are not in git; these commands recreate them.

```bash
cd animation
# 1. third-party sources (CC0 body/heads, MPFB add-on kept inside blender-user/)
mkdir -p third_party/downloads && cd third_party/downloads
curl -LO https://download.blender.org/demo/asset-bundles/human-base-meshes/human-base-meshes-bundle-v1.4.1.zip
cd .. && mkdir -p hbm && unzip -q downloads/human-base-meshes-bundle-v1.4.1.zip -d hbm && cd ..
python3 -m venv .venv && .venv/bin/pip install pillow numpy scikit-image

export BLENDER_USER_RESOURCES=$PWD/blender-user      # keeps add-ons/config inside this folder
# 2. character (~25 s) and equipment library (~4 min)
blender -b --python blender/build_character.py
blender -b --python blender/build_equipment.py
# 3. an exercise: frames + packaged clips
MODE=anim blender -b --python blender/ex_standing_dumbbell_curl.py
.venv/bin/python tools_package.py standing-dumbbell-curl
```

Renders run on the graphics card (OptiX) by default; `RENDER_DEVICE=CPU` only as a fallback.

## Layout
| Path | What |
|---|---|
| `blender/build_character.py` | mannequin: CC0 body, faceless head, skeleton from the mesh's joint seams, shorts, muscle masks |
| `blender/muscles.py` | anatomical highlights for the app's 18 muscle groups (`m_<group>` attributes) |
| `blender/pose.py` | posing helpers (local rotations, aim, finger grip) |
| `blender/equipment.py`, `build_equipment.py` | 70 equipment assets from one parts kit |
| `blender/ex_*.py` | one script per exercise (procedural motion, contact-fitted grip) |
| `blender/style.py` | palette, lights, camera, render settings |
| `tools_package.py` | frames -> webm (alpha) / mp4 / webp, contact sheet, loop-seam check |
| `output/` | deliverables and review sheets |
| `reference/` | RepDB study frames - local only, never committed |
