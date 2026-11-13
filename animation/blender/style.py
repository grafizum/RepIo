"""RepDB look: one place for render settings, lights, camera and materials.

Every scene script imports this so the character and all equipment share the
exact same light rig and palette. Colours were sampled from the reference
frames (lit skin ~#C0C0C0, shorts ~#3A3A3A, equipment ~#4A4A4A, highlight orange).
"""
import math
import os
import bpy
from mathutils import Vector

# "Sandstone" (owner's pick 2026-11-12): warm clay skin, espresso shorts, RepIo mint highlight
SKIN = (0.578, 0.515, 0.423)     # #C9BEAE
SHORTS = (0.025, 0.018, 0.014)   # #2C2520 (RepIo warm-beige ink)
EQUIP = (0.042, 0.037, 0.034)    # #3A3634 warm charcoal
EQUIP_DARK = (0.025, 0.025, 0.026)
HIGHLIGHT = (0.0, 0.521, 0.376)  # #00BFA5 RepIo mint
NAVY = (0.0060, 0.0090, 0.0250)  # preview background only, never in the alpha renders


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def render_settings(res=1080, samples=96, engine="CYCLES"):
    sc = bpy.context.scene
    sc.render.engine = engine
    sc.render.resolution_x = sc.render.resolution_y = res
    sc.render.film_transparent = True
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGBA"
    # Standard keeps the highlight a saturated RepDB orange (AgX washed it to peach)
    sc.view_settings.view_transform = "Standard"
    sc.view_settings.look = "None"
    if engine == "CYCLES":
        cy = sc.cycles
        cy.samples = samples
        cy.use_denoising = True
        # Owner rule (2026-11-12): the graphics card renders. CPU only when explicitly asked
        # (RENDER_DEVICE=CPU), e.g. if another program has filled the card's memory.
        cy.device = os.environ.get("RENDER_DEVICE", "GPU")
        if cy.device == "GPU":
            cy.denoiser = "OPTIX"   # denoise on the card too (OIDN would run on the CPU)
        if cy.device == "CPU":
            sc.render.threads_mode = "AUTO"
            return _common(sc)
        prefs = bpy.context.preferences.addons["cycles"].preferences
        for kind in ("OPTIX", "CUDA"):
            try:
                prefs.compute_device_type = kind
                prefs.get_devices()
                for d in prefs.devices:
                    d.use = d.type != "CPU"   # GPU only, no hybrid CPU+GPU
                break
            except TypeError:
                continue
    return _common(sc)


def _common(sc):
    sc.render.fps = 30
    return sc


def world(strength=0.18):
    w = bpy.data.worlds.new("RepDBWorld")
    bpy.context.scene.world = w
    w.use_nodes = True
    bg = w.node_tree.nodes["Background"]
    bg.inputs[0].default_value = (0.8, 0.82, 0.86, 1)  # faint cool ambient, gives the soft grey falloff
    bg.inputs[1].default_value = strength


LIGHT_SCALE = float(os.environ.get("LIGHT_SCALE", "0.34"))  # calibrated to RepDB skin brightness


def _area(name, loc, size, energy, color=(1, 1, 1)):
    d = bpy.data.lights.new(name, "AREA")
    energy *= LIGHT_SCALE
    d.shape = "DISK"
    d.size = size
    d.energy = energy
    d.color = color
    o = bpy.data.objects.new(name, d)
    bpy.context.collection.objects.link(o)
    o.location = loc
    direction = Vector((0, 0, 1.0)) - Vector(loc)
    o.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    return o


def lights():
    """Big soft key front-left-high, gentle fill right, faint top/back rim. No hard shadows."""
    _area("Key", (-2.6, -3.4, 3.4), 4.5, 900)
    _area("Fill", (3.2, -2.6, 1.6), 5.0, 230)
    _area("Top", (0.0, 0.5, 5.0), 4.0, 260)
    _area("Rim", (1.5, 3.5, 2.6), 3.0, 220)


def camera(target=(0, 0, 0.95), azimuth=-32, elevation=8, distance=6.2, lens=60):
    """Static 3/4 camera. azimuth 0 = straight front (-Y), negative = from the figure's right."""
    cam = bpy.data.cameras.new("Cam")
    cam.lens = lens
    o = bpy.data.objects.new("Cam", cam)
    bpy.context.collection.objects.link(o)
    a, e = math.radians(azimuth), math.radians(elevation)
    t = Vector(target)
    o.location = t + Vector((math.sin(a) * math.cos(e), -math.cos(a) * math.cos(e), math.sin(e))) * distance
    o.rotation_euler = (t - o.location).to_track_quat("-Z", "Y").to_euler()
    bpy.context.scene.camera = o
    return o


def mat_principled(name, color, rough=0.6, sss=0.0, spec=0.35, coat=0.0):
    m = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*color, 1)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Specular IOR Level"].default_value = spec
    if sss:
        b.inputs["Subsurface Weight"].default_value = sss
        b.inputs["Subsurface Radius"].default_value = (0.25, 0.25, 0.25)
        b.inputs["Subsurface Scale"].default_value = 0.02
    if coat:
        b.inputs["Coat Weight"].default_value = coat
    return m


def skin_material(highlight_attr="hl"):
    """Clay skin, compression shorts from the `shorts` attribute, and on top of both the target
    muscle from `hl` - so a glute or quad still shows through the shorts."""
    m = mat_principled("Skin", SKIN, rough=0.55, sss=0.08, spec=0.3)
    nt = m.node_tree
    b = nt.nodes["Principled BSDF"]
    sh = nt.nodes.new("ShaderNodeAttribute")
    sh.attribute_name = "shorts"
    edge = nt.nodes.new("ShaderNodeMapRange")
    edge.inputs["From Min"].default_value, edge.inputs["From Max"].default_value = 0.47, 0.53
    nt.links.new(sh.outputs["Fac"], edge.inputs["Value"])
    cloth = nt.nodes.new("ShaderNodeMix")
    cloth.data_type = "RGBA"
    cloth.inputs["A"].default_value = (*SKIN, 1)
    cloth.inputs["B"].default_value = (*SHORTS, 1)
    nt.links.new(edge.outputs["Result"], cloth.inputs["Factor"])
    attr = nt.nodes.new("ShaderNodeAttribute")
    attr.attribute_name = highlight_attr
    mus = nt.nodes.new("ShaderNodeMix")
    mus.data_type = "RGBA"
    mus.inputs["B"].default_value = (*HIGHLIGHT, 1)
    nt.links.new(cloth.outputs["Result"], mus.inputs["A"])
    nt.links.new(attr.outputs["Fac"], mus.inputs["Factor"])
    nt.links.new(mus.outputs["Result"], b.inputs["Base Color"])
    rough = nt.nodes.new("ShaderNodeMix")
    rough.inputs["A"].default_value, rough.inputs["B"].default_value = 0.55, 0.75
    nt.links.new(edge.outputs["Result"], rough.inputs["Factor"])
    nt.links.new(rough.outputs["Result"], b.inputs["Roughness"])
    return m


def shorts_material():
    return mat_principled("Shorts", SHORTS, rough=0.7, spec=0.25)


def equipment_material(dark=False):
    return mat_principled("EquipDark" if dark else "Equip", EQUIP_DARK if dark else EQUIP, rough=0.45, spec=0.4)
