"""Build every equipment asset, save it, and render a look-check still.

  cd animation && BLENDER_USER_RESOURCES=$PWD/blender-user blender -b --python blender/build_equipment.py
-> assets/equipment/<id>.blend, assets/equipment/library.blend (one collection per asset),
   renders/equipment/<id>.png
Optional: ONLY=id1,id2 to rebuild a subset.
"""
import math
import os
import sys
import time

import bpy
from mathutils import Vector

A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import equipment  # noqa: E402
import style  # noqa: E402

ONLY = set(filter(None, os.environ.get("ONLY", "").split(",")))
os.makedirs(f"{A}/assets/equipment", exist_ok=True)
os.makedirs(f"{A}/renders/equipment", exist_ok=True)


def frame_camera(o):
    cs = [o.matrix_world @ v.co for v in o.data.vertices]
    mn = Vector((min(c.x for c in cs), min(c.y for c in cs), min(c.z for c in cs)))
    mx = Vector((max(c.x for c in cs), max(c.y for c in cs), max(c.z for c in cs)))
    centre = (mn + mx) / 2
    radius = (mx - mn).length / 2
    lens = 50
    fov = 2 * math.atan(18 / lens)
    dist = radius / math.tan(fov / 2) * 1.08
    return style.camera(target=tuple(centre), azimuth=-35, elevation=18, distance=dist, lens=lens)


t0 = time.time()
done = []
for aid, label, build, _key in equipment.LIBRARY:
    if ONLY and aid not in ONLY:
        continue
    style.reset()
    o = build()
    o.name = aid
    bpy.ops.wm.save_as_mainfile(filepath=f"{A}/assets/equipment/{aid}.blend")
    style.render_settings(res=700, samples=48)
    style.world()
    style.lights()
    frame_camera(o)
    bpy.context.scene.render.filepath = f"{A}/renders/equipment/{aid}.png"
    bpy.ops.render.render(write_still=True)
    done.append(aid)
    print(f"BUILT {aid} {time.time() - t0:.0f}s", flush=True)

if not ONLY:
    style.reset()
    for aid, label, build, _key in equipment.LIBRARY:
        col = bpy.data.collections.new(aid)
        bpy.context.scene.collection.children.link(col)
        layer = bpy.context.view_layer.layer_collection.children[aid]
        bpy.context.view_layer.active_layer_collection = layer
        o = build()
        o.name = aid
        col.asset_mark()
    bpy.ops.wm.save_as_mainfile(filepath=f"{A}/assets/equipment/library.blend")
print(f"DONE {len(done)} assets in {time.time() - t0:.0f}s")
