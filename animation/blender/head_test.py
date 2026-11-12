import sys, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose
style.reset(); rig = pose.load_character()
style.render_settings(res=600, samples=32); style.world(); style.lights()
for az, t in ((-30, "34"), (0, "front"), (90, "side")):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    style.camera(target=(0, -0.02, 1.52), azimuth=az, elevation=4, distance=1.1, lens=60)
    bpy.context.scene.render.filepath = f"{A}/renders/tests/head_{t}.png"; bpy.ops.render.render(write_still=True)
