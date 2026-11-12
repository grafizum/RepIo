import sys, bpy
sys.path.insert(0, "/home/matrix/StudioProjects/RepIo/animation/blender")
import style
A = "/home/matrix/StudioProjects/RepIo/animation"
style.reset()
from bl_ext.user_default.mpfb.services.humanservice import HumanService
from bl_ext.user_default.mpfb.services.targetservice import TargetService
macro = TargetService.get_default_macro_info_dict()
macro.update(gender=1.0, age=0.5, muscle=0.9, weight=0.42, proportions=1.0, height=0.62)
macro["race"] = {"asian": 0.0, "caucasian": 1.0, "african": 0.0}
body = HumanService.create_human(scale=0.1, macro_detail_dict=macro, feet_on_ground=True)
TargetService.bake_targets(body) if hasattr(TargetService, "bake_targets") else None
sub = body.modifiers.new("Subsurf", "SUBSURF"); sub.levels = sub.render_levels = 2
style.render_settings(res=900, samples=64)
style.world(); style.lights()
body.data.materials.clear(); body.data.materials.append(style.skin_material())
z = body.dimensions.z
print("DIMS", tuple(round(v, 3) for v in body.dimensions), len(body.data.vertices))
for az, tag in ((-30, "34"), (0, "front"), (180, "back")):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]:
        bpy.data.objects.remove(o)
    style.camera(target=(0, 0, z * 0.5), azimuth=az, elevation=6, distance=3.7, lens=60)
    bpy.context.scene.render.filepath = f"{A}/renders/candidates/mpfb_{tag}.png"
    bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=f"{A}/renders/candidates/mpfb_candidate.blend")
