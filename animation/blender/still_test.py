import sys, os, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose
style.reset()
rig = pose.load_character()
pose.FINGER_SIGN = -1
style.render_settings(res=800, samples=32); style.world(); style.lights()
for side, s in (("L", 1), ("R", -1)):
    pose.aim(rig, f"upperarm.{side}", (s * 0.08, 0.03, -1))
    pose.set_local(rig.pose.bones[f"forearm.{side}"], ("X", -12), ("Y", 80 * s))
    pose.grip(rig, side, 0.85)
style.camera(target=(0, 0, 0.92), azimuth=float(os.environ.get("AZ", "-30")), elevation=6, distance=3.5, lens=60)
bpy.context.scene.render.filepath = f"{A}/renders/tests/still_{os.environ.get('TAG','x')}.png"
bpy.ops.render.render(write_still=True)
