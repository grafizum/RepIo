import sys, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose
style.reset()
rig = pose.load_character()
style.render_settings(res=700, samples=24); style.world(); style.lights()
cam = style.camera(target=(0, 0, 0.9), azimuth=-30, elevation=6, distance=3.6, lens=60)
out = f"{A}/renders/tests"
def shot(tag, az=-30):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    style.camera(target=(0, 0, 0.9), azimuth=az, elevation=6, distance=3.6, lens=60)
    bpy.context.scene.render.filepath = f"{out}/{tag}.png"; bpy.ops.render.render(write_still=True)
mode = sys.argv[-1]
if mode == "apose":
    for az, t in ((-30, "34"), (0, "front"), (90, "side"), (180, "back")): shot(f"apose_{t}", az)
else:
    for side, s in (("L", 1), ("R", -1)):
        pose.aim(rig, f"upperarm.{side}", (s * 0.10, 0.02, -1))
        pose.aim(rig, f"forearm.{side}", (s * 0.10, -0.02, -1))
        pose.local_rot(rig.pose.bones[f"forearm.{side}"], "Y", float(mode.split(":")[1]) * s)
        pose.grip(rig, side, 0.6)
    shot("down_" + mode.replace(":", "_"), 0)
    for side in ("L", "R"):
        pose.local_rot(rig.pose.bones[f"forearm.{side}"], "X", 125)
    shot("curl_" + mode.replace(":", "_"), -30)
