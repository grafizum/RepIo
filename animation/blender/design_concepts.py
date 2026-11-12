"""Three mannequin looks on the same body/rig/animation, to move away from RepDB's look.
Renders renders/designs/<design>_<view>.png ; only materials (and a tank-top region) change."""
import os, sys, bpy
A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
os.environ["MODE"] = "none"
src = open(f"{A}/blender/ex_standing_dumbbell_curl.py").read().replace("\nmain()\n", "\n")
g = {"__name__": "ex"}; exec(compile(src, "ex", "exec"), g)
import style


def srgb(h):
    c = [int(h[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    return tuple(x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c)


DESIGNS = {
    "A_sandstone": dict(skin="#C9BEAE", shorts="#2C2520", top=None, hl="#00BFA5", rough=0.6, coat=0.0, equip="#3A3634"),
    "B_graphite": dict(skin="#6E747D", shorts="#17191C", top=None, hl="#9E7AFF", rough=0.38, coat=0.25, equip="#2A2C30"),
    "C_athletic": dict(skin="#D7DDE4", shorts="#1E2235", top="#1E2235", hl="#00B8D4", rough=0.5, coat=0.1, equip="#3B4150"),
}


def tank_top(body, rig):
    """Point attribute `top` = 1 on the torso (tank top), from the rig weights + rest positions."""
    me = body.data
    gi = {gr.index: gr.name for gr in body.vertex_groups}
    vals = []
    for v in me.vertices:
        w = {gi[x.group]: x.weight for x in v.groups}
        torso = w.get("spine", 0) + w.get("chest", 0) + w.get("pelvis", 0) * (v.co.z > 0.97)
        arm = sum(val for k, val in w.items() if k.startswith(("upperarm", "clavicle")))
        p = v.co
        scoop = 1.36 - 0.07 * max(0.0, 1 - abs(p.x) / 0.08) * (p.y < 0)      # scoop neck in front
        inside = torso > 0.5 and arm < 0.35 and 0.97 < p.z < scoop and abs(p.x) < 0.17
        vals.append(1.0 if inside else 0.0)
    a = me.attributes.get("top") or me.attributes.new("top", "FLOAT", "POINT")
    a.data.foreach_set("value", vals)


def apply_design(d):
    m = bpy.data.materials["Skin"]
    nt = m.node_tree
    b = nt.nodes["Principled BSDF"]
    mixes = [n for n in nt.nodes if n.type == "MIX" and n.data_type == "RGBA"]
    hl_mix, shorts_mix = mixes[0], mixes[1]
    hl_mix.inputs["A"].default_value = (*srgb(d["skin"]), 1)
    hl_mix.inputs["B"].default_value = (*srgb(d["hl"]), 1)
    shorts_mix.inputs["B"].default_value = (*srgb(d["shorts"]), 1)
    b.inputs["Coat Weight"].default_value = d["coat"]
    if d["top"]:
        attr = nt.nodes.new("ShaderNodeAttribute"); attr.attribute_name = "top"
        mix3 = nt.nodes.new("ShaderNodeMix"); mix3.data_type = "RGBA"
        mix3.inputs["B"].default_value = (*srgb(d["top"]), 1)
        nt.links.new(attr.outputs["Fac"], mix3.inputs["Factor"])
        nt.links.new(shorts_mix.outputs["Result"], mix3.inputs["A"])
        nt.links.new(mix3.outputs["Result"], b.inputs["Base Color"])
    for mat in bpy.data.materials:
        if mat.name.startswith("Equip"):
            mat.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*srgb(d["equip"]), 1)


name = os.environ["DESIGN"]
d = DESIGNS[name]
rig = g["scene"](900, 48)
body = bpy.data.objects["Mannequin"]
if d["top"]:
    tank_top(body, rig)
apply_design(d)
for amt, view, az in ((1.0, "top", -28), (0.0, "front", -8)):
    g["set_pose"](rig, amt)
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]: bpy.data.objects.remove(o)
    style.camera(target=(0, -0.02, 0.98), azimuth=az, elevation=9, distance=3.75, lens=60)
    bpy.context.scene.render.filepath = f"{A}/renders/designs/{name}_{view}.png"
    bpy.ops.render.render(write_still=True)
