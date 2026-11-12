import os, sys, time, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose, muscles
style.reset()
rig = pose.load_character()
me = bpy.data.objects["Mannequin"].data
t0 = time.time(); masks = muscles.write_masks(me); print("MASKS", round(time.time() - t0, 1), "s", {k: int((v > 0.5).sum()) for k, v in masks.items()})
groups = os.environ.get("GROUPS", "all")
muscles.set_highlight(me, list(masks) if groups == "all" else groups.split(","))
style.render_settings(res=int(os.environ.get("RES", "800")), samples=24); style.world(); style.lights()
views = [(-15, "front", (0, 0, 1.05), 3.4), (165, "back", (0, 0, 1.05), 3.4), (80, "side", (0, 0, 1.05), 3.4)]
if os.environ.get("ARM"):
    views = [(0, "armfront", (0.23, -0.02, 1.2), 1.0), (180, "armback", (0.23, 0, 1.2), 1.0)]
for az, t, tgt, dist in views:
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    style.camera(target=tgt, azimuth=az, elevation=4, distance=dist, lens=50)
    bpy.context.scene.render.filepath = f"{A}/renders/tests/atlas_{groups.replace(',', '+')[:40]}_{t}.png"; bpy.ops.render.render(write_still=True)
