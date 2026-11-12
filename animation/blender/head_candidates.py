"""Render CC0 head candidates (Blender Studio Human Base Meshes) + a low-passed version of our head."""
import sys, bpy
from mathutils import Vector
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style, pose
style.reset()
lib = f"{A}/third_party/hbm/human-base-meshes-bundle-v1.4.1/human_base_meshes_bundle.blend"
names = ["GEO-head_male_primitive_realistic", "Head - Generic Topology", "Head - Planar"]
with bpy.data.libraries.load(lib) as (src, dst):
    dst.objects = names
skin = style.mat_principled("Clay", style.SKIN, rough=0.55, sss=0.08, spec=0.3)
for i, o in enumerate(dst.objects):
    bpy.context.collection.objects.link(o)
    o.parent = None
    bpy.context.view_layer.update()
    # centre each head on its bbox and line them up along X
    bb = [o.matrix_world @ Vector(c) for c in o.bound_box]
    ctr = sum(bb, Vector()) / 8
    o.location -= ctr - Vector((i * 0.32, 0, 1.6))
    for m in o.modifiers:
        if m.type == "MULTIRES": m.levels = m.render_levels = m.total_levels
        if m.type == "SUBSURF": m.render_levels = 3
    o.data.materials.clear(); o.data.materials.append(skin)
    for p in o.data.polygons: p.use_smooth = True
# our head, low-passed: heavy volume-preserving smoothing on the head only
rig = pose.load_character()
body = bpy.data.objects["Mannequin"]
vg = body.vertex_groups.new(name="lp")
for v in body.data.vertices:
    if v.co.z > 1.44:
        vg.add([v.index], min(1.0, (v.co.z - 1.44) / 0.05), "REPLACE")
m = body.modifiers.new("LP", "LAPLACIANSMOOTH"); m.iterations, m.lambda_factor, m.vertex_group = 60, 3.0, "lp"
m.use_volume_preserve = True
bpy.context.view_layer.objects.active = body
with bpy.context.temp_override(object=body, active_object=body):
    bpy.ops.object.modifier_move_to_index(modifier="LP", index=0)
rig.location = (3 * 0.32, 0, 1.6 - 1.575)
style.render_settings(res=1500, samples=32); style.world(); style.lights()
bpy.context.scene.render.resolution_y = 420
for az, t in ((-30, "34"), (90, "side")):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    cam = style.camera(target=(0.48, 0, 1.6), azimuth=az, elevation=3, distance=3.2, lens=85)
    if t == "side":
        cam.data.type = "ORTHO"; cam.data.ortho_scale = 1.45
    bpy.context.scene.render.filepath = f"{A}/renders/tests/headcand_{t}.png"; bpy.ops.render.render(write_still=True)
