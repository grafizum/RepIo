import sys, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import style
style.reset()
lib = f"{A}/third_party/hbm/human-base-meshes-bundle-v1.4.1/human_base_meshes_bundle.blend"
with bpy.data.libraries.load(lib) as (src, dst):
    dst.objects = ["GEO-body_male_realistic"]
objs = []
for i, lvl in enumerate((0, 1, 2, 3)):
    o = dst.objects[0].copy(); o.data = dst.objects[0].data.copy()
    bpy.context.collection.objects.link(o); o.location = (i * 0.35, 0, 0)
    for m in o.modifiers:
        if m.type == "MULTIRES": m.levels = m.render_levels = lvl
    if lvl == 0:
        s = o.modifiers.new("S", "SUBSURF"); s.levels = s.render_levels = 3
    o.data.materials.clear(); o.data.materials.append(style.mat_principled("Clay", (0.42, 0.37, 0.31), rough=0.6))
style.render_settings(res=1400, samples=32); style.world(); style.lights()
bpy.context.scene.render.resolution_y = 500
style.camera(target=(0.5, -0.05, 1.56), azimuth=-25, elevation=4, distance=1.9, lens=60)
bpy.context.scene.render.filepath = f"{A}/renders/tests/head_levels.png"; bpy.ops.render.render(write_still=True)
