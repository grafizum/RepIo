import sys, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose
style.reset()
rig = pose.load_character()
style.render_settings(res=500, samples=16); style.world(); style.lights()
import os
flex, twist, fsign = (float(x) for x in os.environ["HT"].split())
pose.FINGER_SIGN = fsign
pose.aim(rig, "upperarm.L", (0.10, 0.02, -1))
pose.set_local(rig.pose.bones["forearm.L"], ("X", flex), ("Y", twist))
pose.grip(rig, "L", 0.7)
bpy.context.view_layer.update()
hp = rig.matrix_world @ rig.pose.bones["hand.L"].head
style.camera(target=tuple(hp), azimuth=-20, elevation=5, distance=0.7, lens=60)
bpy.context.scene.render.filepath = f"{A}/renders/tests/hand_f{int(flex)}_t{int(twist)}_s{int(fsign)}.png"
bpy.ops.render.render(write_still=True)
