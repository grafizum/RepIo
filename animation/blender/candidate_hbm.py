import sys, bpy
sys.path.insert(0, "/home/matrix/StudioProjects/RepIo/animation/blender")
import style
A = "/home/matrix/StudioProjects/RepIo/animation"
style.reset()
lib = f"{A}/third_party/hbm/human-base-meshes-bundle-v1.4.1/human_base_meshes_bundle.blend"
with bpy.data.libraries.load(lib) as (src, dst):
    dst.objects = [n for n in src.objects if n == "GEO-body_male_realistic"]
body = dst.objects[0]
bpy.context.collection.objects.link(body)
body.data = body.data.copy()
body.location = (0, 0, 0)
style.render_settings(res=900, samples=64)
style.world(); style.lights()
body.data.materials.clear(); body.data.materials.append(style.skin_material())
z = body.dimensions.z
for az, tag in ((-30, "34"), (0, "front"), (180, "back")):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]:
        bpy.data.objects.remove(o)
    style.camera(target=(0, 0, z * 0.52), azimuth=az, elevation=6, distance=3.7, lens=60)
    bpy.context.scene.render.filepath = f"{A}/renders/candidates/hbm_{tag}.png"
    bpy.ops.render.render(write_still=True)
