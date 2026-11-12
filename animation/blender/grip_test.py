import os, sys, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
os.environ["MODE"] = "none"
src = open(f"{A}/blender/ex_standing_dumbbell_curl.py").read().replace("\nmain()\n", "\n")
g = {"__name__": "ex"}; exec(compile(src, "ex", "exec"), g)
import style
rig = g["scene"](600, 24)
a = float(os.environ.get("AMT", "0.5"))
g["set_pose"](rig, a)
for side in ("L", "R"):
    hp = rig.matrix_world @ rig.pose.bones[f"hand.{side}"].tail
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    for az, t in ((-28, "a"), (60 if side == "L" else -100, "b")):
        for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
        style.camera(target=tuple(hp), azimuth=az, elevation=10, distance=0.65, lens=60)
        bpy.context.scene.render.filepath = f"{A}/renders/tests/grip_{a}_{side}_{t}_tw{int(g["TWIST"])}.png"
        bpy.ops.render.render(write_still=True)
