import os, sys, bpy, numpy as np
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose, muscles
style.reset()
rig = pose.load_character()
body = bpy.data.objects["Mannequin"]
me = body.data
co, nor, ed = muscles.mesh_arrays(me)
cav = muscles.cavity(co, nor, ed, iterations=int(os.environ.get("IT", "40")))
m = muscles.MUSCLES["biceps"]
c, r = np.array(m["centre"]), np.array(m["radii"])
inside = (((co - c) / r) ** 2).sum(1) < 1
print("CAV inside prior: pct", [round(float(np.percentile(cav[inside], q)) * 1000, 3) for q in (5, 25, 50, 75, 90, 95, 99)], "mm")
mode = os.environ.get("VIS", "cav")
if mode == "cav":
    vals = np.clip(cav * float(os.environ.get("K", "4000")), 0, 1)
else:
    seed = int(np.argmin(((co - np.array(m["seed"])) ** 2).sum(1)))
    reg = muscles.grow(co, ed, cav, seed, inside, float(os.environ.get("G", "0.0003")))
    print("REGION verts", int(reg.sum()), "of prior", int(inside.sum()))
    vals = reg.astype(np.float32)
me.attributes["hl"].data.foreach_set("value", vals.astype(np.float32))
me.update()
style.render_settings(res=700, samples=24); style.world(); style.lights()
mid = muscles.SHOULDER.lerp(muscles.ELBOW, 0.5)
for az, t in ((0, "front"), (60, "side")):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    style.camera(target=tuple(mid), azimuth=az, elevation=5, distance=0.9, lens=60)
    bpy.context.scene.render.filepath = f"{A}/renders/tests/mus_{mode}_{t}.png"; bpy.ops.render.render(write_still=True)
