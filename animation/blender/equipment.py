"""Equipment builders in the RepDB look (dark matte grey, soft bevels, clean silhouettes).

Each builder returns one object with its origin at the natural grip/pivot point, so scenes
can attach it to a hand bone (dumbbell, kettlebell) or place it on the floor (bench, rack).
"""
import math

import bpy
from mathutils import Matrix, Vector

import style


def _lathe(name, profile, steps=96):
    """Revolve a (radius, x) profile around the object's X axis."""
    me = bpy.data.meshes.new(name)
    verts = []
    for i in range(steps):
        a = 2 * math.pi * i / steps
        ca, sa = math.cos(a), math.sin(a)
        for r, x in profile:
            verts.append((x, r * ca, r * sa))
    n = len(profile)
    faces = []
    for i in range(steps):
        j = (i + 1) % steps
        for k in range(n - 1):
            faces.append((i * n + k, i * n + k + 1, j * n + k + 1, j * n + k))
    me.from_pydata(verts, [], faces)
    me.validate()
    o = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(o)
    bpy.ops.object.select_all(action="DESELECT")
    o.select_set(True)
    bpy.context.view_layer.objects.active = o
    m = o.modifiers.new("Merge", "WELD")
    m.merge_threshold = 1e-5
    with bpy.context.temp_override(object=o, active_object=o, selected_objects=[o]):
        bpy.ops.object.modifier_apply(modifier="Merge")
        bpy.ops.object.shade_smooth_by_angle(angle=math.radians(35))
    return o


def _corner(p0, p1, p2, r=0.0025, n=3):
    """Points that round the corner at p1 (between p0 and p1 -> p2) with radius ~r."""
    a, b, c = Vector(p0), Vector(p1), Vector(p2)
    u, w = (a - b).normalized(), (c - b).normalized()
    s, e = b + u * r, b + w * r
    pts = []
    for i in range(n + 1):
        t = i / n
        q = s.lerp(b, t).lerp(b.lerp(e, t), t)   # quadratic bezier
        pts.append((q.x, q.y))
    return pts


def _rounded(points, r=0.0025):
    out = [points[0]]
    for i in range(1, len(points) - 1):
        out += _corner(points[i - 1], points[i], points[i + 1], r)
    out.append(points[-1])
    return out


def dumbbell(name="Dumbbell", plate_r=0.064, plate_w=0.034, handle_len=0.132, handle_r=0.0155):
    """RepDB's dumbbell: one thick round plate per side with a rounded rim, a raised hub and
    an end cap, on a plain handle. Origin = grip centre, long axis = local X."""
    h = handle_len / 2
    collar_r, hub_r = 0.024, 0.027
    x0 = h                       # inner collar starts
    x1 = x0 + 0.010              # plate inner face
    x2 = x1 + plate_w            # plate outer face
    x3 = x2 + 0.007              # hub / end cap
    half = [
        (0.0005, x3), (hub_r * 0.35, x3), (hub_r * 0.35, x3 - 0.0012),  # tiny dimple in the cap
        (hub_r, x3 - 0.0012), (hub_r, x2), (plate_r - 0.006, x2),
        (plate_r, x2 - 0.006), (plate_r, x1 + 0.006), (plate_r - 0.006, x1),
        (collar_r, x1), (collar_r, x0), (handle_r, x0), (handle_r, 0.0),
    ]
    rounded = _rounded([(x, r) for r, x in half], r=0.0022)
    prof = [(r, x) for x, r in rounded]
    full = prof + [(r, -x) for r, x in reversed(prof[:-1])]
    o = _lathe(name, full)
    o.data.materials.append(style.equipment_material())
    return o


# =====================================================================================
# Shared kit: everything below is built with bmesh / plain mesh data and baked into world
# space, then joined into ONE object per asset (no modifier stacks left behind).
# Conventions: metres, Z up, the user faces -Y, floor items have their origin at floor centre.
# =====================================================================================
import bmesh  # noqa: E402

PAD = (0.018, 0.012, 0.010)      # #241E1A darker espresso upholstery
STEEL = (0.262, 0.242, 0.222)    # #8C8780 brushed steel bars / handles
RUBBER = (0.012, 0.011, 0.010)   # near-black rubber
BALL = (0.100, 0.088, 0.075)     # warm taupe for exercise balls / mats
WALL = (0.120, 0.110, 0.100)     # warm grey wall panel


def _mat(kind):
    name = f"EQ_{kind}"
    m = bpy.data.materials.get(name)
    if m:
        return m
    if kind == "frame":
        m = style.mat_principled(name, style.EQUIP, rough=0.45, spec=0.4)
    elif kind == "pad":
        m = style.mat_principled(name, PAD, rough=0.75, spec=0.25)
    elif kind == "steel":
        m = style.mat_principled(name, STEEL, rough=0.35, spec=0.5)
        m.node_tree.nodes["Principled BSDF"].inputs["Metallic"].default_value = 0.6
    elif kind == "rubber":
        m = style.mat_principled(name, RUBBER, rough=0.8, spec=0.2)
    elif kind == "plate":
        m = style.mat_principled(name, style.EQUIP, rough=0.55, spec=0.3)
    elif kind == "ball":
        m = style.mat_principled(name, BALL, rough=0.6, spec=0.3)
    elif kind == "wall":
        m = style.mat_principled(name, WALL, rough=0.9, spec=0.2)
    return m


def _from_bm(bm, mat, name="part", smooth_angle=35):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    me.materials.append(_mat(mat))
    for p in me.polygons:
        p.use_smooth = True
    me.set_sharp_from_angle(angle=math.radians(smooth_angle))
    o = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(o)
    return o


def _align(p1, p2):
    """Matrix taking a unit-length Z-aligned part centred on the origin onto segment p1->p2.
    Local Y is kept as close to world up as possible, so a beam's `h` is always its height
    (a rotation_difference here once turned cross-beams' width into height: 5 mm floor gaps)."""
    p1, p2 = Vector(p1), Vector(p2)
    d = p2 - p1
    q = d.normalized().to_track_quat("Z", "Y") if abs(d.normalized().z) < 0.999 else \
        Vector((0, 0, 1)).rotation_difference(d.normalized())
    return Matrix.Translation((p1 + p2) / 2) @ q.to_matrix().to_4x4(), d.length


ROLE_FLEX, ROLE_STACK, ROLE_PIN = 1, 2, 3


def tag(o, role):
    """Mark a part for the physics audit: 1 flexible (rope/cable/strap/band), 2 stack plate, 3 pin."""
    me = o.data
    a = me.attributes.get("role") or me.attributes.new("role", "INT", "POINT")
    a.data.foreach_set("value", [role] * len(me.vertices))
    return o


def box(size, loc, rot=(0, 0, 0), bevel=0.008, mat="frame", segs=3, role=0):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    bmesh.ops.scale(bm, vec=Vector(size), verts=bm.verts)
    if bevel > 0:
        bmesh.ops.bevel(bm, geom=list(bm.edges), offset=min(bevel, min(size) * 0.45), segments=segs,
                        profile=0.5, affect="EDGES", clamp_overlap=True)
    from mathutils import Euler
    bmesh.ops.transform(bm, matrix=Matrix.Translation(Vector(loc)) @ Euler(rot).to_matrix().to_4x4(), verts=bm.verts)
    o = _from_bm(bm, mat)
    return tag(o, role) if role else o


def beam(p1, p2, w=0.06, h=None, mat="frame", bevel=0.008, roll=0.0, role=0):
    """Rounded square tube between two points."""
    m, L = _align(p1, p2)
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    bmesh.ops.scale(bm, vec=Vector((w, h or w, L)), verts=bm.verts)
    bmesh.ops.bevel(bm, geom=list(bm.edges), offset=min(bevel, w * 0.4), segments=3, profile=0.5,
                    affect="EDGES", clamp_overlap=True)
    bmesh.ops.transform(bm, matrix=m @ Matrix.Rotation(roll, 4, "Z"), verts=bm.verts)
    o = _from_bm(bm, mat)
    return tag(o, role) if role else o


def tube(p1, p2, r=0.015, mat="steel", segs=24, cap_bevel=True, role=0):
    """Round tube between two points."""
    m, L = _align(p1, p2)
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, cap_tris=False, segments=segs, radius1=r, radius2=r, depth=L)
    if cap_bevel:
        rim = [e for e in bm.edges if all(abs(abs(v.co.z) - L / 2) < 1e-6 for v in e.verts)]
        bmesh.ops.bevel(bm, geom=rim, offset=min(r * 0.25, L * 0.2), segments=2, profile=0.5, affect="EDGES",
                        clamp_overlap=True)
    bmesh.ops.transform(bm, matrix=m, verts=bm.verts)
    o = _from_bm(bm, mat)
    return tag(o, role) if role else o


def pad(size, loc, rot=(0, 0, 0), r=0.025):
    """Upholstered pad: a well-rounded box in the pad material."""
    return box(size, loc, rot, bevel=r, mat="pad", segs=5)


def sphere(c, r, mat="ball", segs=48):
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=segs, v_segments=segs // 2, radius=r)
    bmesh.ops.translate(bm, vec=Vector(c), verts=bm.verts)
    return _from_bm(bm, mat)


def torus(c, R, r, axis=(0, 0, 1), mat="steel", seg=48, sseg=16, role=0, scale=(1, 1, 1)):
    verts, faces = [], []
    for i in range(seg):
        a = 2 * math.pi * i / seg
        for j in range(sseg):
            b = 2 * math.pi * j / sseg
            verts.append(((R + r * math.cos(b)) * math.cos(a), (R + r * math.cos(b)) * math.sin(a), r * math.sin(b)))
    for i in range(seg):
        for j in range(sseg):
            i2, j2 = (i + 1) % seg, (j + 1) % sseg
            faces.append((i * sseg + j, i2 * sseg + j, i2 * sseg + j2, i * sseg + j2))
    bm = bmesh.new()
    vs = [bm.verts.new(v) for v in verts]
    for f in faces:
        bm.faces.new([vs[k] for k in f])
    q = Vector((0, 0, 1)).rotation_difference(Vector(axis).normalized())
    bmesh.ops.transform(bm, matrix=Matrix.Translation(Vector(c)) @ q.to_matrix().to_4x4() @ Matrix.Diagonal((*scale, 1)),
                        verts=bm.verts)
    o = _from_bm(bm, mat, smooth_angle=180)
    return tag(o, role) if role else o


def lathe(profile, c=(0, 0, 0), axis=(1, 0, 0), mat="plate", steps=64):
    """Revolve a closed or open (radius, x) profile about X, then orient X onto `axis` at `c`."""
    o = _lathe("lathe", profile, steps)
    q = Vector((1, 0, 0)).rotation_difference(Vector(axis).normalized())
    o.data.transform(Matrix.Translation(Vector(c)) @ q.to_matrix().to_4x4())
    o.data.materials.clear()
    o.data.materials.append(_mat(mat))
    return o


def plate(c, axis=(1, 0, 0), R=0.225, w=0.05, mat="plate"):
    """Olympic/bumper plate: rounded rim, recessed face, steel-lined hub."""
    h = w / 2
    pts = [(0.026, -h * 0.7), (0.06, -h * 0.7), (0.075, -h), (R - 0.008, -h), (R, -h + 0.008), (R, h - 0.008),
           (R - 0.008, h), (0.075, h), (0.06, h * 0.7), (0.026, h * 0.7), (0.026, -h * 0.7)]
    return lathe(_rounded_closed(pts, 0.003), c, axis, mat)


def _rounded_closed(pts, r):
    xy = [(x, rr) for rr, x in pts]
    out = _rounded(xy, r)
    return [(rr, x) for x, rr in out]


def path_tube(points, r=0.006, mat="rubber", closed=False, res=12, flat=None, role=0):
    """Rope / cable / strap along a smooth path (curve -> mesh). flat=(w, t) makes a strap."""
    cu = bpy.data.curves.new("path", "CURVE")
    cu.dimensions = "3D"
    sp = cu.splines.new("NURBS" if not closed else "NURBS")
    sp.points.add(len(points) - 1)
    for p, q in zip(sp.points, points):
        p.co = (*q, 1.0)
    sp.use_endpoint_u = not closed
    sp.use_cyclic_u = closed
    sp.order_u = min(4, len(points))
    sp.resolution_u = res
    if flat:
        w, t = flat
        prof = bpy.data.curves.new("prof", "CURVE")
        ps = prof.splines.new("POLY")
        ps.points.add(3)
        for p, q in zip(ps.points, [(-w / 2, -t / 2), (w / 2, -t / 2), (w / 2, t / 2), (-w / 2, t / 2)]):
            p.co = (*q, 0, 1)
        ps.use_cyclic_u = True
        po = bpy.data.objects.new("prof", prof)
        bpy.context.collection.objects.link(po)
        cu.bevel_mode = "OBJECT"
        cu.bevel_object = po
    else:
        cu.bevel_depth = r
        cu.bevel_resolution = 4
    cu.use_fill_caps = True
    co = bpy.data.objects.new("path", cu)
    bpy.context.collection.objects.link(co)
    dg = bpy.context.evaluated_depsgraph_get()
    me = bpy.data.meshes.new_from_object(co.evaluated_get(dg))
    bpy.data.objects.remove(co)
    if flat:
        bpy.data.objects.remove(po)
    me.materials.clear()
    me.materials.append(_mat(mat))
    for p in me.polygons:
        p.use_smooth = True
    o = bpy.data.objects.new("path", me)
    bpy.context.collection.objects.link(o)
    return tag(o, role) if role else o


def pulley(c, axis=(1, 0, 0), r=0.05):
    pts = [(0.008, -0.012), (r * 0.8, -0.012), (r, -0.008), (r * 0.82, 0.0), (r, 0.008), (r * 0.8, 0.012),
           (0.008, 0.012)]
    return lathe(pts, c, axis, "steel", 48)


def assemble(name, parts, origin="floor"):
    """Join parts into one object. origin: 'floor' (floor centre), 'keep' (world origin is already
    the grip), 'bbox' (bounding-box centre) or a point (that point becomes the origin: the grip)."""
    parts = [p for p in parts if p is not None]
    for p in parts:
        p.select_set(False)
    with bpy.context.temp_override(active_object=parts[0], object=parts[0], selected_objects=parts,
                                   selected_editable_objects=parts):
        bpy.ops.object.join()
    o = parts[0]
    o.name = o.data.name = name
    cs = [v.co for v in o.data.vertices]
    mn = Vector((min(c.x for c in cs), min(c.y for c in cs), min(c.z for c in cs)))
    mx = Vector((max(c.x for c in cs), max(c.y for c in cs), max(c.z for c in cs)))
    if origin == "floor":
        o.data.transform(Matrix.Translation(Vector((-(mn.x + mx.x) / 2, -(mn.y + mx.y) / 2, -mn.z))))
    elif origin == "bbox":
        o.data.transform(Matrix.Translation(-(mn + mx) / 2))
    elif origin != "keep":
        o.data.transform(Matrix.Translation(-Vector(origin)))
    return o


# ---- reusable sub-assemblies -----------------------------------------------------------------
def stack_tower(x, y, h=1.95, w=0.36, d=0.22, plates=14):
    """Selectorised weight stack: two uprights, top cap, guide rods, plate stack, top pulley."""
    p = []
    for sx in (-1, 1):
        p.append(beam((x + sx * w / 2, y, 0), (x + sx * w / 2, y, h), 0.07, 0.09))
    p.append(beam((x - w / 2 - 0.035, y, h), (x + w / 2 + 0.035, y, h), 0.09, 0.07))
    p.append(box((w + 0.12, d + 0.12, 0.05), (x, y, 0.025), bevel=0.01))
    for sx in (-0.1, 0.1):
        p.append(tube((x + sx, y, 0.05), (x + sx, y, h - 0.04), 0.008))
    z0, ph = 0.05, 0.05                       # plates sit flush on the base, 50 mm each
    for i in range(plates):
        p.append(box((w - 0.08, d - 0.04, ph), (x, y, z0 + ph / 2 + i * ph), bevel=0.006, mat="plate", role=ROLE_STACK))
        p.append(box((0.03, 0.006, 0.014), (x, y - (d - 0.04) / 2 - 0.001, z0 + ph / 2 + i * ph), bevel=0.002, mat="rubber"))  # pin hole
    ztop = z0 + plates * ph
    p.append(box((w - 0.04, d * 0.8, 0.03), (x, y, ztop + 0.015), bevel=0.006, mat="steel", role=ROLE_STACK))  # top plate
    pin_z = z0 + ph / 2 + 5 * ph
    p.append(tube((x, y - (d - 0.04) / 2 + 0.03, pin_z), (x, y - (d - 0.04) / 2 - 0.05, pin_z), 0.006, "steel", role=ROLE_PIN))
    p.append(sphere((x, y - (d - 0.04) / 2 - 0.055, pin_z), 0.013, "rubber", 16))                    # pin knob
    p.append(pulley((x, y, h - 0.09), (0, 1, 0), 0.055))
    p.append(tube((x, y, ztop + 0.03), (x, y, h - 0.09 - 0.045), 0.0035, "steel", role=ROLE_FLEX))  # lift cable
    if abs(x) > 0.05:                         # tie the tower into the machine's base frame
        p.append(beam((x, y, 0.04), (0, y, 0.04), 0.08, 0.08))
    return p


def seat(c, w=0.42, d=0.40, t=0.08, tilt=0.0):
    return pad((w, d, t), c, (tilt, 0, 0))


def backrest(c, w=0.40, h=0.62, t=0.08, lean=0.18):
    return pad((w, t, h), c, (-lean, 0, 0))


def base_rail(y0, y1, x=0.0, w=0.08):
    return beam((x, y0, 0.04), (x, y1, 0.04), w, 0.08)


def handle_grip(p1, p2, r=0.016):
    return [tube(p1, p2, r, "rubber")]


# ---- free weights ---------------------------------------------------------------------------
def barbell(name="barbell"):
    L, grip = 2.2, 1.31
    p = [tube((-grip / 2, 0, 0), (grip / 2, 0, 0), 0.014, "steel")]
    for sx in (-1, 1):
        p.append(tube((sx * grip / 2, 0, 0), (sx * (grip / 2 + 0.05), 0, 0), 0.026, "steel"))      # collar
        p.append(tube((sx * (grip / 2 + 0.05), 0, 0), (sx * L / 2, 0, 0), 0.025, "steel"))        # sleeve
        p.append(plate((sx * (grip / 2 + 0.085), 0, 0), (1, 0, 0), 0.225, 0.055))
        p.append(plate((sx * (grip / 2 + 0.14), 0, 0), (1, 0, 0), 0.165, 0.035))
    return assemble(name, p, origin="keep")          # origin = bar centre (grip)


def ez_bar(name="ez_bar"):
    pts, z = [], 0.0
    xs = [-0.62, -0.30, -0.24, -0.15, -0.08, 0.0, 0.08, 0.15, 0.24, 0.30, 0.62]
    ys = [0, 0, 0.035, -0.035, 0.0, 0.0, 0.0, -0.035, 0.035, 0, 0]
    p = [path_tube([(x, y, 0) for x, y in zip(xs, ys)], 0.014, "steel", res=16)]
    for sx in (-1, 1):
        p.append(tube((sx * 0.30, 0, 0), (sx * 0.34, 0, 0), 0.026, "steel"))
        p.append(tube((sx * 0.34, 0, 0), (sx * 0.62, 0, 0), 0.025, "steel"))
        p.append(plate((sx * 0.385, 0, 0), (1, 0, 0), 0.16, 0.04))
    return assemble(name, p, origin="keep")


def hex_bar(name="hex_bar"):
    r = 0.33
    hexpts = [(r * math.cos(math.radians(30 + 60 * i)), r * math.sin(math.radians(30 + 60 * i)), 0.0) for i in range(7)]
    p = [tube(hexpts[i], hexpts[i + 1], 0.016, "steel") for i in range(6)]
    for sx in (-1, 1):
        p.append(tube((sx * 0.14, -0.12, 0), (sx * 0.14, 0.12, 0), 0.017, "rubber"))       # handles
        p.append(tube((sx * 0.14, -0.12, 0), (sx * r * 0.87, -0.12, 0), 0.015, "steel"))
        p.append(tube((sx * 0.14, 0.12, 0), (sx * r * 0.87, 0.12, 0), 0.015, "steel"))
        p.append(tube((sx * r * 0.87, 0, 0), (sx * 0.75, 0, 0), 0.025, "steel"))
        p.append(plate((sx * 0.45, 0, 0), (1, 0, 0), 0.225, 0.055))
    return assemble(name, p, origin="keep")


def weight_plate(name="weight_plate"):
    pts = [(0.03, -0.012), (0.13, -0.012), (0.14, -0.018), (0.2, -0.018), (0.205, -0.012), (0.205, 0.012),
           (0.2, 0.018), (0.14, 0.018), (0.13, 0.012), (0.03, 0.012), (0.03, -0.012)]
    p = [lathe(_rounded_closed(pts, 0.003), (0, 0, 0.205), (0, 1, 0), "plate")]
    for sx in (-1, 1):                       # grip cut-outs read as dark ovals
        p.append(box((0.07, 0.04, 0.03), (sx * 0.15, 0, 0.205), bevel=0.012, mat="rubber"))
    return assemble(name, p, origin=(0, 0, 0.205))


def kettlebell(name="kettlebell", r=0.097):
    # round bell with a flat foot: a circle profile from the foot (-66°) over the top (90°)
    body = [(0.0005, -r * 0.92)]
    for deg in range(-66, 91, 6):
        a = math.radians(deg)
        body.append((max(0.0005, r * math.cos(a)), r * math.sin(a)))
    p = [lathe(body, (0, 0, r * 0.92), (0, 0, 1), "plate")]
    hz = r * 0.92 + r * 0.6
    hp = [(-r * 0.62, 0, hz - 0.01), (-r * 0.66, 0, hz + 0.06), (-r * 0.45, 0, hz + 0.105), (0, 0, hz + 0.115),
          (r * 0.45, 0, hz + 0.105), (r * 0.66, 0, hz + 0.06), (r * 0.62, 0, hz - 0.01)]
    p.append(path_tube(hp, 0.017, "plate", res=16))
    return assemble(name, p, origin=(0, 0, hz + 0.095))


def medicine_ball(name="medicine_ball"):
    p = [sphere((0, 0, 0.17), 0.17, "rubber")]
    for ax in ((1, 0, 0), (0, 1, 0)):
        p.append(torus((0, 0, 0.17), 0.171, 0.004, ax, "plate", 64, 8))
    return assemble(name, p, origin=(0, 0, 0.17))


def stability_ball(name="stability_ball"):
    return assemble(name, [sphere((0, 0, 0.325), 0.325, "ball", 64)], origin=(0, 0, 0.325))


def ab_wheel(name="ab_wheel"):
    tyre = [(0.03, -0.03), (0.08, -0.03), (0.095, -0.025), (0.1, 0.0), (0.095, 0.025), (0.08, 0.03), (0.03, 0.03)]
    p = [lathe(tyre, (0, 0, 0.1), (1, 0, 0), "rubber")]
    p.append(tube((-0.03, 0, 0.1), (0.03, 0, 0.1), 0.03, "plate"))
    p.append(tube((-0.18, 0, 0.1), (0.18, 0, 0.1), 0.012, "steel"))
    for sx in (-1, 1):
        p.append(tube((sx * 0.06, 0, 0.1), (sx * 0.17, 0, 0.1), 0.018, "rubber"))
    return assemble(name, p, origin=(0, 0, 0.1))


def wrist_roller(name="wrist_roller"):
    p = [tube((-0.22, 0, 1.0), (0.22, 0, 1.0), 0.02, "plate")]
    for sx in (-1, 1):
        p.append(tube((sx * 0.06, 0, 1.0), (sx * 0.22, 0, 1.0), 0.022, "rubber"))
    p.append(tube((0, 0, 0.985), (0, 0, 0.16), 0.003, "steel", role=ROLE_FLEX))
    p.append(plate((0, 0, 0.16), (0, 0, 1), 0.11, 0.03))
    return assemble(name, p, origin=(0, 0, 1.0))


def dip_belt(name="dip_belt"):
    b = torus((0, 0, 1.0), 0.15, 0.02, (0, 0, 1), "pad", 64, 12)   # oval belt, flattened into a strap
    b.data.transform(Matrix.Translation((0, 0, 1.0)) @ Matrix.Diagonal((1.0, 0.72, 2.2, 1.0)) @ Matrix.Translation((0, 0, -1.0)))
    p = [b]
    for sx in (-1, 1):
        p.append(tube((sx * 0.09, -0.085, 0.99), (0, -0.115, 0.45), 0.006, "steel", role=ROLE_FLEX))
    p.append(plate((0, -0.115, 0.45), (0, 1, 0), 0.165, 0.035))
    return assemble(name, p, origin="bbox")


def jump_rope(name="jump_rope"):
    p = []
    for sx in (-1, 1):                                   # handles lying on the floor
        p.append(tube((sx * 0.12, 0.30, 0.016), (sx * 0.12, 0.14, 0.016), 0.016, "rubber"))
    loop = [(-0.12, 0.14, 0.004), (-0.30, -0.05, 0.004), (-0.42, -0.45, 0.004), (-0.25, -0.85, 0.004), (0.0, -0.95, 0.004),
            (0.25, -0.85, 0.004), (0.42, -0.45, 0.004), (0.30, -0.05, 0.004), (0.12, 0.14, 0.004)]
    p.append(path_tube(loop, 0.004, "rubber", res=20, role=ROLE_FLEX))
    return assemble(name, p)


def battle_ropes(name="battle_ropes"):
    p = [box((0.16, 0.16, 0.3), (0, 2.4, 0.15), bevel=0.02)]                       # anchor post
    for sx in (-1, 1):
        pts = [(sx * 0.04, 2.34, 0.019)] + [(sx * (0.12 + 0.18 * (i / 6)) + 0.05 * math.sin(i * 1.8),
                                             2.2 - i * 0.36, 0.019) for i in range(7)]
        p.append(path_tube(pts, 0.019, "rubber", res=16, role=ROLE_FLEX))
        end = Vector(pts[-1])
        d = (end - Vector(pts[-2])).normalized()
        p.append(tube(end - d * 0.02 + Vector((0, 0, 0.005)), end + d * 0.18 + Vector((0, 0, 0.005)), 0.024, "pad"))
    return assemble(name, p)


# ---- benches & racks ------------------------------------------------------------------------
def flat_bench(name="flat_bench"):
    p = [pad((0.30, 1.18, 0.08), (0, 0, 0.43))]
    p.append(beam((0, -0.48, 0.36), (0, 0.48, 0.36), 0.08))
    for y in (-0.48, 0.48):
        p.append(beam((0, y, 0.36), (0, y, 0.04), 0.07))
        p.append(beam((-0.24, y, 0.035), (0.24, y, 0.035), 0.07, 0.06))
        for sx in (-1, 1):
            p.append(box((0.06, 0.06, 0.012), (sx * 0.24, y, 0.006), bevel=0.004, mat="rubber"))
    return assemble(name, p)


def adjustable_bench(name="adjustable_bench", incline=40):
    a = math.radians(incline)
    p = [pad((0.32, 0.38, 0.08), (0, -0.30, 0.45))]
    bl = 0.86
    bc = Vector((0, -0.08 + math.cos(a) * bl / 2, 0.46 + math.sin(a) * bl / 2))
    p.append(pad((0.30, bl, 0.08), bc, (a, 0, 0)))
    p.append(beam((0, -0.62, 0.05), (0, 0.62, 0.05), 0.08))
    p.append(beam((0, -0.30, 0.05), (0, -0.30, 0.43), 0.07))
    p.append(beam((0, 0.10, 0.05), (0, bc.y, bc.z - 0.03), 0.06))                     # support strut
    p.append(beam((0, 0.28, 0.05), (0, 0.15, 0.30), 0.05))
    for y in (-0.62, 0.62):
        p.append(beam((-0.25, y, 0.035), (0.25, y, 0.035), 0.07, 0.06))
    p.append(tube((-0.12, -0.66, 0.06), (0.12, -0.66, 0.06), 0.035, "rubber"))     # transport wheels hint
    return assemble(name, p)


def decline_situp_bench(name="decline_situp_bench"):
    a = math.radians(-22)
    p = [pad((0.32, 1.2, 0.07), (0, 0.05, 0.55), (a, 0, 0))]
    p.append(beam((0, -0.55, 0.05), (0, 0.6, 0.05), 0.08))
    p.append(beam((0, -0.45, 0.05), (0, -0.48, 0.74), 0.07))
    p.append(beam((0, 0.55, 0.05), (0, 0.55, 0.32), 0.07))
    for z in (0.92, 0.78):                                                         # foot rollers
        p.append(tube((-0.18, -0.55, z), (0.18, -0.55, z), 0.045, "pad"))
    p.append(beam((0, -0.48, 0.74), (0, -0.55, 0.95), 0.05))
    for y in (-0.5, 0.6):
        p.append(beam((-0.25, y, 0.035), (0.25, y, 0.035), 0.07, 0.06))
    return assemble(name, p)


def preacher_bench(name="preacher_bench"):
    p = [pad((0.36, 0.34, 0.08), (0, 0.28, 0.58))]
    p.append(pad((0.52, 0.10, 0.36), (0, -0.05, 0.98), (math.radians(-40), 0, 0)))
    p.append(beam((0, 0.28, 0.05), (0, 0.28, 0.555), 0.07))
    p.append(beam((0, 0.0, 0.05), (0, 0.0, 0.95), 0.08))
    p.append(beam((0, -0.4, 0.05), (0, 0.55, 0.05), 0.08))
    p.append(beam((-0.27, 0.0, 0.86), (0.27, 0.0, 0.86), 0.04))
    for sx in (-1, 1):
        p.append(beam((sx * 0.25, 0.0, 0.86), (sx * 0.30, -0.22, 0.96), 0.04))     # bar catches
    for y in (-0.4, 0.55):
        p.append(beam((-0.28, y, 0.035), (0.28, y, 0.035), 0.07, 0.06))
    return assemble(name, p)


def back_extension_bench(name="back_extension_bench"):
    a = math.radians(45)
    p = [beam((0, -0.7, 0.05), (0, 0.55, 0.05), 0.08)]
    p.append(beam((0, 0.45, 0.05), (0, -0.30, 0.82), 0.08))                         # main 45° spine
    p.append(pad((0.36, 0.12, 0.30), (0, -0.30, 0.86), (-a, 0, 0)))                 # hip pad
    p.append(box((0.4, 0.3, 0.03), (0, 0.42, 0.2), (a * 0.7, 0, 0), bevel=0.01, mat="rubber"))  # foot plate
    for z in (0.33,):
        p.append(tube((-0.16, 0.30, z), (0.16, 0.30, z), 0.045, "pad"))             # ankle roller
    p.append(beam((0, 0.3, 0.05), (0, 0.3, 0.33), 0.05))
    for y in (-0.7, 0.55):
        p.append(beam((-0.28, y, 0.035), (0.28, y, 0.035), 0.07, 0.06))
    return assemble(name, p)


def power_rack(name="power_rack"):
    w, d, h = 1.2, 1.2, 2.3
    p = []
    for sx in (-1, 1):
        for sy in (-1, 1):
            p.append(beam((sx * w / 2, sy * d / 2, 0), (sx * w / 2, sy * d / 2, h), 0.075))
            # hole pattern hint: small dark plates on the front uprights
        p.append(beam((sx * w / 2, -d / 2, h), (sx * w / 2, d / 2, h), 0.075))
        p.append(beam((sx * w / 2, -d / 2, 0.04), (sx * w / 2, d / 2, 0.04), 0.075))
        p.append(beam((sx * w / 2, -d / 2, 0.9), (sx * w / 2, d / 2, 0.9), 0.05))         # safety arm
        p.append(box((0.06, 0.12, 0.05), (sx * w / 2, -d / 2 - 0.06, 1.38), bevel=0.01, mat="steel"))  # J-hook
    for sy in (-1, 1):
        p.append(beam((-w / 2, sy * d / 2, h), (w / 2, sy * d / 2, h), 0.075))
    p.append(tube((-w / 2, d / 2 + 0.045, h - 0.04), (w / 2, d / 2 + 0.045, h - 0.04), 0.016, "steel"))   # pull-up bar
    bb = barbell("rack_bar")                          # resting in the J-hooks (hook top 1.405 m)
    bb.data.transform(Matrix.Translation((0, -d / 2 - 0.08, 1.405 + 0.0135)))
    p.append(bb)
    return assemble(name, p)


def landmine(name="landmine"):
    p = [box((0.5, 0.5, 0.03), (0, 0.9, 0.015), bevel=0.01)]
    p.append(tube((0, 0.9, 0.03), (0, 0.9, 0.12), 0.04, "frame"))
    piv = Vector((0, 0.9, 0.10))
    a = math.asin((0.225 - piv.z) / 1.88)               # plate rests on the floor 1.88 m out
    dirv = Vector((0, -math.cos(a), math.sin(a)))
    p.append(tube(piv, piv + dirv * 1.78, 0.014, "steel"))
    p.append(tube(piv + dirv * 1.76, piv + dirv * 2.2, 0.025, "steel"))
    p.append(plate(piv + dirv * 1.88, dirv, 0.225, 0.05))
    return assemble(name, p)


def plyo_box(name="plyo_box"):
    p = [box((0.75, 0.6, 0.5), (0, 0, 0.25), bevel=0.02)]
    p.append(pad((0.74, 0.59, 0.03), (0, 0, 0.51), r=0.012))
    return assemble(name, p)


def smith_machine(name="smith_machine"):
    w, h = 1.6, 2.2
    p = []
    for sx in (-1, 1):
        for y in (-0.35, 0.35):
            p.append(beam((sx * w / 2, y, 0), (sx * w / 2, y, h), 0.08))
        p.append(beam((sx * w / 2, -0.35, h), (sx * w / 2, 0.35, h), 0.08))
        p.append(beam((sx * w / 2, -0.45, 0.04), (sx * w / 2, 0.45, 0.04), 0.08))
        p.append(tube((sx * (w / 2 - 0.12), -0.05, 0.05), (sx * (w / 2 - 0.12), -0.05, h - 0.04), 0.016))  # guide rail
        for zz in (h - 0.04, 0.04):
            p.append(beam((sx * w / 2, -0.05, zz), (sx * (w / 2 - 0.14), -0.05, zz), 0.05))
        p.append(box((0.07, 0.07, 0.14), (sx * (w / 2 - 0.12), -0.05, 1.25), bevel=0.01, mat="frame"))   # carriage
        p.append(tube((sx * (w / 2 - 0.11), -0.05, 1.25), (sx * (w / 2 + 0.25), -0.05, 1.25), 0.025))
        p.append(plate((sx * (w / 2 + 0.1), -0.05, 1.25), (1, 0, 0), 0.225, 0.05))
    p.append(beam((-w / 2, 0.35, h), (w / 2, 0.35, h), 0.08))
    p.append(beam((-w / 2, -0.35, h), (w / 2, -0.35, h), 0.08))
    p.append(tube((-(w / 2 - 0.12), -0.05, 1.25), (w / 2 - 0.12, -0.05, 1.25), 0.014))
    return assemble(name, p)


# ---- cable ------------------------------------------------------------------------------------
def cable_station(name="cable_station"):
    p = stack_tower(0, 0.25, 2.1, 0.40, 0.24)
    for sx in (-1, 1):                                                      # adjustable column track
        p.append(beam((sx * 0.06, -0.05, 0.03), (sx * 0.06, -0.05, 2.08), 0.035, 0.05))
    p.append(beam((0, 0.25, 2.08), (0, -0.07, 2.08), 0.06))
    p.append(box((0.2, 0.12, 0.16), (0, -0.1, 1.2), bevel=0.015))                 # pulley carriage
    p.append(pulley((0, -0.2, 1.2), (1, 0, 0), 0.05))
    p.append(tube((0, -0.25, 1.205), (0, -0.25, 1.02), 0.004, "steel", role=ROLE_FLEX))
    p += att_d_handle_parts((0, -0.25, 0.905))
    p.append(box((0.8, 0.9, 0.04), (0, 0.15, 0.02), bevel=0.01))
    return assemble(name, p)


def lat_pulldown(name="lat_pulldown"):
    p = stack_tower(0, 0.55, 2.2, 0.36, 0.22)
    p.append(beam((0, 0.55, 2.2), (0, -0.15, 2.3), 0.08))                          # boom
    p.append(pulley((0, -0.15, 2.22), (1, 0, 0), 0.05))
    p.append(tube((0, -0.15, 2.18), (0, -0.15, 1.78), 0.004, "steel", role=ROLE_FLEX))
    p.append(att_lat_bar(origin_only=False, at=(0, -0.15, 1.76)))
    p.append(beam((0, 0.55, 0.05), (0, -0.3, 0.05), 0.08))
    p.append(beam((0, -0.15, 0.05), (0, -0.15, 0.45), 0.07))
    p.append(seat((0, -0.15, 0.47), 0.40, 0.36))
    p.append(beam((0, -0.15, 0.45), (0, -0.30, 0.68), 0.05))
    p.append(tube((-0.2, -0.32, 0.68), (0.2, -0.32, 0.68), 0.05, "pad"))          # thigh roller
    return assemble(name, p)


def seated_row(name="seated_row"):
    p = stack_tower(0, -0.95, 1.6, 0.36, 0.22)
    p.append(beam((0, -0.85, 0.12), (0, 1.0, 0.12), 0.09))
    for y in (-0.85, 1.0):
        p.append(beam((0, y, 0.0), (0, y, 0.12), 0.08))
    p.append(seat((0, 0.6, 0.38), 0.38, 0.45))
    p.append(beam((0, 0.6, 0.12), (0, 0.6, 0.36), 0.07))
    p.append(box((0.5, 0.06, 0.32), (0, -0.4, 0.3), (math.radians(-20), 0, 0), bevel=0.015, mat="rubber"))  # foot plate
    p.append(pulley((0, -0.72, 0.45), (1, 0, 0), 0.05))
    p.append(tube((0, -0.72, 0.40), (0, -0.28, 0.40), 0.004, "steel", role=ROLE_FLEX))
    p += att_v_bar_parts((0, -0.255, 0.40))
    for y in (-0.85, 1.0):
        p.append(beam((-0.3, y, 0.035), (0.3, y, 0.035), 0.07, 0.06))
    return assemble(name, p)


def _carabiner(c):
    return torus(c, 0.022, 0.004, (1, 0, 0), "steel", 24, 8)


def att_straight_bar(name="att_straight_bar"):
    p = [tube((-0.25, 0, 0), (0.25, 0, 0), 0.015, "steel")]
    for sx in (-1, 1):
        p.append(tube((sx * 0.08, 0, 0), (sx * 0.24, 0, 0), 0.017, "rubber"))
    p.append(_carabiner((0, 0, 0.035)))
    return assemble(name, p, origin="keep")


def att_v_bar_parts(c):
    c = Vector(c)
    p = [path_tube([c + Vector(q) for q in ((-0.06, 0.12, 0), (-0.03, 0.03, 0), (0, 0, 0), (0.03, 0.03, 0), (0.06, 0.12, 0))],
                   0.012, "steel", res=16)]
    for sx in (-1, 1):
        p.append(tube(c + Vector((sx * 0.06, 0.08, 0)), c + Vector((sx * 0.065, 0.16, 0)), 0.016, "rubber"))
    p.append(tube(c + Vector((0, 0.012, 0)), c + Vector((0, -0.026, 0)), 0.008, "steel"))   # swivel stem
    p.append(_carabiner(c + Vector((0, -0.025, 0))))
    return p


def att_v_bar(name="att_v_bar"):
    return assemble(name, att_v_bar_parts((0, 0, 0)), origin="keep")


def att_rope(name="att_rope"):
    p = [_carabiner((0, 0, 0.03))]
    for sx in (-1, 1):
        p.append(tube((sx * 0.011, 0, 0.012), (sx * 0.011, 0, -0.30), 0.012, "rubber", role=ROLE_FLEX))
        p.append(sphere((sx * 0.011, 0, -0.32), 0.028, "rubber", 24))
    return assemble(name, p, origin="keep")


def att_d_handle_parts(c):
    c = Vector(c)
    p = [tube(c + Vector((-0.06, 0, 0)), c + Vector((0.06, 0, 0)), 0.017, "rubber")]
    p.append(path_tube([c + Vector(q) for q in ((-0.065, 0, 0), (-0.06, 0, 0.07), (0, 0, 0.1), (0.06, 0, 0.07), (0.065, 0, 0))],
                       0.008, "steel", res=16))
    p.append(_carabiner(c + Vector((0, 0, 0.12))))
    return p


def att_d_handle(name="att_d_handle"):
    return assemble(name, att_d_handle_parts((0, 0, 0)), origin="keep")


def att_lat_bar(name="att_lat_bar", origin_only=True, at=(0, 0, 0)):
    c = Vector(at)
    pts = [(-0.6, 0, -0.06), (-0.45, 0, -0.01), (-0.2, 0, 0.0), (0.2, 0, 0.0), (0.45, 0, -0.01), (0.6, 0, -0.06)]
    p = [path_tube([c + Vector(q) for q in pts], 0.014, "steel", res=16)]
    for sx in (-1, 1):
        p.append(tube(c + Vector((sx * 0.42, 0, -0.005)), c + Vector((sx * 0.6, 0, -0.06)), 0.017, "rubber"))
    p.append(_carabiner(c + Vector((0, 0, 0.035))))
    return assemble(name, p, origin="keep")


def att_ankle_strap(name="att_ankle_strap"):
    loop = [(0.06 * math.cos(math.radians(a)), 0.045 * math.sin(math.radians(a)), 0) for a in range(0, 361, 30)]
    p = [path_tube(loop, 0, "pad", flat=(0.012, 0.07))]
    p.append(torus((0.075, 0, 0), 0.016, 0.004, (0, 0, 1), "steel", 24, 8))
    return assemble(name, p, origin="keep")


# ---- machines ---------------------------------------------------------------------------------
def _frame_base(y0, y1, w=0.6):
    p = [beam((0, y0, 0.04), (0, y1, 0.04), 0.09, 0.08)]
    for y in (y0, y1):
        p.append(beam((-w / 2, y, 0.035), (w / 2, y, 0.035), 0.08, 0.07))
    return p


def _seat_unit(y=0.0, h=0.5, back=True, lean=0.15):
    p = [beam((0, y, 0.05), (0, y, h - 0.03), 0.08), seat((0, y, h))]
    if back:
        p.append(beam((0, y + 0.25, 0.05), (0, y + 0.3, h + 0.35), 0.07))
        p.append(backrest((0, y + 0.24, h + 0.36), lean=lean))
    return p


def chest_press(name="chest_press"):
    p = stack_tower(0.55, 0.45, 1.9) + _frame_base(-0.4, 0.55) + _seat_unit(0.05, 0.48)
    for sx in (-1, 1):
        p.append(beam((sx * 0.30, 0.35, 1.25), (sx * 0.30, -0.25, 1.05), 0.05))
        p.append(tube((sx * 0.30, -0.25, 1.05), (sx * 0.30, -0.25, 1.22), 0.018, "rubber"))
    p.append(beam((-0.3, 0.35, 1.25), (0.3, 0.35, 1.25), 0.06))
    p.append(beam((0, 0.35, 0.05), (0, 0.35, 1.25), 0.08))
    return assemble(name, p)


def pec_deck(name="pec_deck"):
    p = stack_tower(0, 0.75, 1.95) + _frame_base(-0.3, 0.75) + _seat_unit(0.0, 0.48, lean=0.05)
    p.append(beam((0, 0.4, 0.05), (0, 0.4, 1.75), 0.08))
    p.append(beam((-0.25, 0.4, 1.75), (0.25, 0.4, 1.75), 0.06))
    for sx in (-1, 1):
        p.append(beam((sx * 0.25, 0.4, 1.75), (sx * 0.55, 0.05, 1.68), 0.045))
        p.append(beam((sx * 0.55, 0.05, 1.68), (sx * 0.55, 0.05, 1.05), 0.04))
        p.append(pad((0.06, 0.16, 0.42), (sx * 0.52, -0.02, 1.2), r=0.02))
    return assemble(name, p)


def shoulder_press_machine(name="shoulder_press_machine"):
    p = stack_tower(0.55, 0.45, 1.95) + _frame_base(-0.3, 0.55) + _seat_unit(0.0, 0.48, lean=0.08)
    p.append(beam((0, 0.38, 0.05), (0, 0.38, 1.5), 0.08))
    for sx in (-1, 1):
        p.append(beam((sx * 0.05, 0.38, 1.5), (sx * 0.30, 0.02, 1.45), 0.05))
        p.append(tube((sx * 0.30, 0.02, 1.45), (sx * 0.30, -0.12, 1.45), 0.018, "rubber"))
    return assemble(name, p)


def biceps_curl_machine(name="biceps_curl_machine"):
    p = stack_tower(0.6, 0.4, 1.7) + _frame_base(-0.3, 0.45) + _seat_unit(0.3, 0.55, back=False)
    p.append(beam((0, 0.0, 0.05), (0, -0.04, 0.98), 0.08))
    p.append(pad((0.6, 0.10, 0.30), (0, -0.05, 1.0), (math.radians(-45), 0, 0)))     # arm pad
    p.append(tube((-0.34, -0.05, 1.0), (0.34, -0.05, 1.0), 0.02, "steel"))           # pivot
    p.append(beam((-0.32, -0.05, 1.0), (-0.32, -0.32, 1.12), 0.04))
    p.append(beam((0.32, -0.05, 1.0), (0.32, -0.32, 1.12), 0.04))
    p.append(tube((-0.32, -0.32, 1.12), (0.32, -0.32, 1.12), 0.017, "rubber"))
    return assemble(name, p)


def triceps_extension_machine(name="triceps_extension_machine"):
    p = stack_tower(0.6, 0.45, 1.7) + _frame_base(-0.3, 0.5) + _seat_unit(0.3, 0.5, lean=0.1)
    p.append(beam((0, -0.05, 0.05), (0, -0.05, 0.95), 0.08))
    p.append(pad((0.55, 0.10, 0.26), (0, -0.05, 1.05), (math.radians(-35), 0, 0)))
    p.append(tube((-0.32, -0.05, 1.05), (0.32, -0.05, 1.05), 0.02, "steel"))         # pivot
    for sx in (-1, 1):
        p.append(beam((sx * 0.3, -0.05, 1.05), (sx * 0.3, -0.22, 1.25), 0.04))
        p.append(pad((0.08, 0.06, 0.16), (sx * 0.3, -0.24, 1.3), r=0.02))
    return assemble(name, p)


def leg_extension(name="leg_extension"):
    p = stack_tower(0.6, 0.35, 1.6) + _frame_base(-0.4, 0.45) + _seat_unit(0.15, 0.5, lean=0.1)
    p.append(beam((0, -0.15, 0.05), (0, -0.15, 0.5), 0.07))
    p.append(tube((0.0, -0.15, 0.48), (0.27, -0.15, 0.48), 0.02, "steel"))           # pivot
    p.append(beam((0.25, -0.15, 0.48), (0.25, -0.32, 0.18), 0.045))                     # lever
    p.append(tube((-0.27, -0.34, 0.2), (0.27, -0.34, 0.2), 0.05, "pad"))             # shin roller
    for sx in (-1, 1):
        p.append(tube((sx * 0.225, 0.25, 0.5), (sx * 0.225, 0.05, 0.5), 0.018, "rubber"))
    return assemble(name, p)


def seated_leg_curl(name="seated_leg_curl"):
    p = stack_tower(0.6, 0.35, 1.6) + _frame_base(-0.5, 0.45) + _seat_unit(0.15, 0.5, lean=0.15)
    p.append(beam((0, -0.15, 0.05), (0, -0.15, 0.5), 0.07))
    p.append(beam((0.22, 0.0, 0.5), (0.22, -0.08, 0.68), 0.04))                       # hold-down bracket
    p.append(tube((-0.24, -0.08, 0.68), (0.24, -0.08, 0.68), 0.045, "pad"))          # thigh hold-down
    p.append(tube((0.0, -0.15, 0.48), (0.27, -0.15, 0.48), 0.02, "steel"))           # pivot
    p.append(beam((0.25, -0.15, 0.48), (0.25, -0.45, 0.5), 0.045))
    p.append(tube((-0.27, -0.47, 0.48), (0.27, -0.47, 0.48), 0.05, "pad"))           # calf roller
    return assemble(name, p)


def lying_leg_curl(name="lying_leg_curl"):
    p = stack_tower(0.6, 0.6, 1.4) + _frame_base(-0.75, 0.7)
    p.append(pad((0.34, 1.05, 0.08), (0, 0.1, 0.62), (math.radians(-6), 0, 0)))
    for y in (-0.3, 0.45):
        p.append(beam((0, y, 0.05), (0, y, 0.58), 0.07))
    p.append(tube((0.0, -0.3, 0.55), (0.27, -0.3, 0.55), 0.02, "steel"))             # pivot
    p.append(beam((0.25, -0.3, 0.55), (0.25, -0.72, 0.68), 0.045))
    p.append(tube((-0.27, -0.72, 0.68), (0.27, -0.72, 0.68), 0.05, "pad"))
    p.append(beam((-0.2, 0.45, 0.5), (0.2, 0.45, 0.5), 0.05))
    for sx in (-1, 1):
        p.append(tube((sx * 0.2, 0.45, 0.5), (sx * 0.2, 0.45, 0.6), 0.018, "rubber"))
    return assemble(name, p)


def hip_abduction_adduction(name="hip_abduction_adduction"):
    p = stack_tower(0, 0.75, 1.6) + _frame_base(-0.5, 0.75) + _seat_unit(0.1, 0.5, lean=0.3)
    for sx in (-1, 1):
        p.append(beam((sx * 0.02, 0.05, 0.45), (sx * 0.2, -0.45, 0.5), 0.045))
        p.append(pad((0.07, 0.30, 0.16), (sx * 0.17, -0.3, 0.56), r=0.025))         # thigh pads
        p.append(beam((sx * 0.2, -0.45, 0.5), (sx * 0.25, -0.5, 0.31), 0.035))
        p.append(box((0.12, 0.2, 0.03), (sx * 0.25, -0.5, 0.3), bevel=0.008, mat="rubber"))  # foot pegs
    return assemble(name, p)


def ab_crunch_machine(name="ab_crunch_machine"):
    p = stack_tower(0.6, 0.45, 1.7) + _frame_base(-0.3, 0.5) + _seat_unit(0.15, 0.5, lean=0.05)
    p.append(beam((0, 0.45, 0.82), (0, 0.42, 1.2), 0.06))
    p.append(beam((0, 0.42, 1.2), (0, 0.05, 1.42), 0.06))
    p.append(beam((-0.15, 0.05, 1.42), (0.15, 0.05, 1.42), 0.05))
    for sx in (-1, 1):
        p.append(pad((0.09, 0.08, 0.22), (sx * 0.13, 0.0, 1.32), r=0.02))           # chest/shoulder pads
        p.append(tube((sx * 0.13, 0.0, 1.44), (sx * 0.2, -0.07, 1.44), 0.016, "rubber"))
    p.append(beam((0, 0.15, 0.3), (0, -0.25, 0.28), 0.05))
    p.append(tube((-0.18, -0.25, 0.28), (0.18, -0.25, 0.28), 0.045, "pad"))
    return assemble(name, p)


def back_extension_machine(name="back_extension_machine"):
    p = stack_tower(0.6, 0.55, 1.6) + _frame_base(-0.4, 0.6) + _seat_unit(0.0, 0.5, back=False)
    p.append(beam((0, 0.35, 0.05), (0, 0.35, 0.95), 0.08))
    p.append(beam((0, 0.35, 0.95), (0.2, 0.35, 0.95), 0.05))
    p.append(beam((0.2, 0.35, 0.95), (0.2, 0.3, 1.2), 0.045))
    p.append(tube((-0.2, 0.3, 1.18), (0.2, 0.3, 1.18), 0.06, "pad"))                 # back roller
    p.append(beam((0, 0.0, 0.25), (0, -0.35, 0.25), 0.05))
    p.append(box((0.4, 0.25, 0.03), (0, -0.35, 0.25), bevel=0.008, mat="rubber"))
    return assemble(name, p)


def calf_raise_standing(name="calf_raise_standing"):
    p = stack_tower(0, 0.5, 2.0) + _frame_base(-0.4, 0.5)
    p.append(beam((0, 0.33, 0.0), (0, 0.33, 1.55), 0.07))
    p.append(beam((0, 0.33, 1.55), (0, -0.02, 1.55), 0.07))
    p.append(beam((-0.2, -0.02, 1.55), (0.2, -0.02, 1.55), 0.05))
    for sx in (-1, 1):
        p.append(pad((0.12, 0.26, 0.10), (sx * 0.17, -0.02, 1.48), r=0.03))          # shoulder pads
        p.append(tube((sx * 0.2, -0.02, 1.57), (sx * 0.2, -0.02, 1.72), 0.016, "rubber"))
    p.append(box((0.5, 0.18, 0.09), (0, -0.05, 0.08), bevel=0.02, mat="rubber"))      # toe block
    return assemble(name, p)


def calf_raise_seated(name="calf_raise_seated"):
    p = _frame_base(-0.5, 0.45) + [beam((0, 0.25, 0.05), (0, 0.25, 0.45), 0.07), seat((0, 0.25, 0.47), 0.36, 0.36)]
    p.append(beam((0, -0.2, 0.05), (0, -0.2, 0.55), 0.07))
    p.append(beam((0, -0.2, 0.55), (0, 0.15, 0.64), 0.06))                          # lever
    p.append(pad((0.36, 0.14, 0.07), (0, 0.08, 0.62), r=0.025))                       # knee pad
    p.append(tube((0, -0.2, 0.58), (0, -0.2, 0.85), 0.025, "steel"))                  # plate horn
    p.append(plate((0, -0.2, 0.62), (0, 0, 1), 0.225, 0.05))
    p.append(box((0.4, 0.14, 0.08), (0, -0.05, 0.06), bevel=0.02, mat="rubber"))
    return assemble(name, p)


def calf_raise_donkey(name="calf_raise_donkey"):
    p = _frame_base(-0.4, 0.9)
    p.append(beam((0, -0.3, 0.05), (0, -0.3, 1.1), 0.08))
    p.append(pad((0.4, 0.25, 0.06), (0, -0.25, 1.12), r=0.02))                        # forearm pad
    p.append(beam((0, 0.8, 0.05), (0, 0.8, 0.85), 0.08))
    p.append(beam((0, 0.8, 0.85), (0, 0.25, 0.95), 0.07))                            # lever
    p.append(pad((0.36, 0.30, 0.10), (0, 0.32, 0.9), r=0.03))                          # hip pad
    p.append(tube((0.0, 0.55, 0.9), (0.45, 0.55, 0.9), 0.025, "steel"))
    p.append(plate((0.3, 0.55, 0.9), (1, 0, 0), 0.225, 0.05))
    p.append(box((0.45, 0.16, 0.09), (0, 0.4, 0.06), bevel=0.02, mat="rubber"))
    return assemble(name, p)


def assisted_pullup_dip(name="assisted_pullup_dip"):
    p = stack_tower(0, 0.55, 2.35) + _frame_base(-0.5, 0.6, 0.9)
    for sx in (-1, 1):
        p.append(beam((sx * 0.4, 0.2, 0.0), (sx * 0.4, 0.2, 2.35), 0.08))
        p.append(beam((sx * 0.4, 0.2, 2.35), (sx * 0.4, -0.25, 2.3), 0.06))
        p.append(tube((sx * 0.4, -0.25, 2.3), (sx * 0.2, -0.3, 2.3), 0.017, "rubber"))   # pull-up grips
        p.append(beam((sx * 0.4, 0.2, 1.3), (sx * 0.28, -0.3, 1.3), 0.045))
        p.append(tube((sx * 0.28, -0.12, 1.3), (sx * 0.28, -0.42, 1.3), 0.018, "rubber"))  # dip handles
    p.append(beam((-0.4, 0.2, 2.35), (0.4, 0.2, 2.35), 0.07))
    p.append(beam((-0.4, 0.2, 0.6), (0.4, 0.2, 0.6), 0.06))
    p.append(beam((0, 0.2, 0.6), (0, -0.25, 0.85), 0.06))
    p.append(pad((0.36, 0.30, 0.07), (0, -0.3, 0.86), r=0.025))                         # knee platform
    return assemble(name, p)


def glute_drive(name="glute_drive"):
    p = _frame_base(-0.6, 0.6, 0.8)
    p.append(pad((0.5, 0.35, 0.08), (0, 0.38, 0.42), (math.radians(10), 0, 0)))       # back support
    p.append(beam((0, 0.38, 0.05), (0, 0.38, 0.40), 0.07))
    p.append(pad((0.5, 0.40, 0.06), (0, -0.05, 0.08), r=0.02))                           # floor seat
    for sx in (-1, 1):
        p.append(beam((sx * 0.35, 0.45, 0.0), (sx * 0.35, 0.45, 0.55), 0.07))
        p.append(beam((sx * 0.35, 0.45, 0.55), (sx * 0.35, -0.1, 0.42), 0.06))           # lever arms
        p.append(tube((sx * 0.35, -0.1, 0.42), (sx * 0.6, -0.1, 0.42), 0.025, "steel"))
        p.append(plate((sx * 0.48, -0.1, 0.42), (1, 0, 0), 0.225, 0.05))
    p.append(beam((-0.35, -0.1, 0.42), (0.35, -0.1, 0.42), 0.05))
    p.append(pad((0.6, 0.12, 0.07), (0, -0.1, 0.37), r=0.03))                            # hip belt pad
    return assemble(name, p)


def lateral_raise_machine(name="lateral_raise_machine"):
    p = _frame_base(-0.3, 0.55) + [beam((0, 0.0, 0.05), (0, 0.0, 0.47), 0.07), seat((0, 0.0, 0.5))]
    p.append(beam((0, 0.3, 0.05), (0, 0.3, 1.25), 0.08))
    p.append(pad((0.38, 0.08, 0.5), (0, 0.24, 0.95)))                                     # chest pad
    for sx in (-1, 1):
        p.append(beam((0.0, 0.3, 1.25), (sx * 0.42, 0.1, 1.2), 0.045))
        p.append(pad((0.07, 0.18, 0.08), (sx * 0.42, 0.05, 1.15), r=0.025))              # elbow pads
        p.append(tube((sx * 0.42, 0.1, 1.2), (sx * 0.65, 0.1, 1.2), 0.025, "steel"))
        p.append(plate((sx * 0.53, 0.1, 1.2), (1, 0, 0), 0.165, 0.035))
    return assemble(name, p)


def shrug_machine(name="shrug_machine"):
    p = _frame_base(-0.5, 0.5, 0.9)
    p.append(box((0.6, 0.5, 0.05), (0, -0.05, 0.03), bevel=0.01, mat="rubber"))         # platform
    for sx in (-1, 1):
        p.append(beam((sx * 0.42, 0.3, 0.0), (sx * 0.42, 0.3, 0.9), 0.08))
        p.append(beam((sx * 0.42, 0.3, 0.9), (sx * 0.30, -0.1, 0.72), 0.06))
        p.append(tube((sx * 0.30, -0.1, 0.72), (sx * 0.30, -0.1, 0.6), 0.018, "rubber"))  # handles
        p.append(tube((sx * 0.42, 0.3, 0.9), (sx * 0.62, 0.3, 0.9), 0.025, "steel"))
        p.append(plate((sx * 0.52, 0.3, 0.9), (1, 0, 0), 0.225, 0.05))
    return assemble(name, p)


def leg_press_45(name="leg_press_45"):
    a = math.radians(45)
    p = [beam((sx * 0.35, -0.9, 0.05), (sx * 0.35, 1.0, 0.05), 0.09, 0.08) for sx in (-1, 1)]
    for y in (-0.9, 1.0):
        p.append(beam((-0.4, y, 0.04), (0.4, y, 0.04), 0.08))
    for sx in (-1, 1):                                                                    # 45° sled rails
        p.append(beam((sx * 0.3, -0.3, 0.05), (sx * 0.3, -1.25, 1.0), 0.07))
    p.append(beam((0, -0.9, 0.05), (0, -1.15, 0.95), 0.08))
    p.append(box((0.75, 0.06, 0.55), (0, -0.95, 0.7), (a, 0, 0), bevel=0.015, mat="frame"))   # foot plate
    for sx in (-1, 1):
        p.append(tube((sx * 0.33, -0.95, 0.7), (sx * 0.62, -0.95, 0.7), 0.025, "steel"))
        p.append(plate((sx * 0.5, -0.95, 0.7), (1, 0, 0), 0.225, 0.05))
    p.append(seat((0, 0.35, 0.45), 0.46, 0.42, tilt=math.radians(-10)))
    p.append(backrest((0, 0.72, 0.72), 0.46, 0.75, lean=-0.75))
    p.append(beam((0, 0.35, 0.0), (0, 0.35, 0.43), 0.08))
    p.append(beam((0, 0.72, 0.0), (0, 0.72, 0.72), 0.08))                                   # through the backrest
    for sx in (-1, 1):
        p.append(tube((sx * 0.245, 0.35, 0.47), (sx * 0.245, 0.15, 0.47), 0.018, "rubber"))   # seat-side grips
    return assemble(name, p)


def horizontal_leg_press(name="horizontal_leg_press"):
    p = stack_tower(0.6, 0.6, 1.5) + _frame_base(-0.7, 0.75)
    p.append(seat((0, 0.25, 0.5), 0.44, 0.4))
    p.append(backrest((0, 0.55, 0.85), 0.44, 0.65, lean=0.5))
    p.append(beam((0, 0.3, 0.05), (0, 0.3, 0.47), 0.08))
    p.append(beam((0, 0.55, 0.05), (0, 0.55, 0.85), 0.08))                                  # through the backrest
    p.append(beam((0, -0.45, 0.05), (0, -0.45, 0.65), 0.08))                                # foot-plate post
    p.append(box((0.6, 0.06, 0.5), (0, -0.45, 0.65), (math.radians(8), 0, 0), bevel=0.015))
    p.append(beam((-0.28, 0.25, 0.5), (0.28, 0.25, 0.5), 0.05))
    for sx in (-1, 1):
        p.append(tube((sx * 0.28, 0.25, 0.5), (sx * 0.28, 0.05, 0.5), 0.018, "rubber"))
    return assemble(name, p)


def hack_squat(name="hack_squat"):
    a = math.radians(45)
    p = [beam((sx * 0.4, -0.6, 0.04), (sx * 0.4, 1.65, 0.04), 0.09, 0.08) for sx in (-1, 1)]
    for y in (-0.6, 1.65):
        p.append(beam((-0.45, y, 0.04), (0.45, y, 0.04), 0.08))
    rail = lambda y: Vector((0, y, 0.05 + (y + 0.2)))                                 # 45° rail line
    for sx in (-1, 1):
        p.append(beam((sx * 0.25, -0.2, 0.05), (sx * 0.25, 1.6, 1.85), 0.07))          # 45° rails
        p.append(beam((sx * 0.25, 1.6, 0.0), (sx * 0.25, 1.6, 1.85), 0.07))            # rear posts
        h = rail(0.75)
        p.append(tube((sx * 0.22, h.y, h.z), (sx * 0.62, h.y, h.z), 0.025, "steel"))   # horns on the sled
        p.append(plate((sx * 0.5, h.y, h.z), (1, 0, 0), 0.225, 0.05))
    n = Vector((0, -math.sin(a), math.cos(a)))                                          # rail normal
    c = rail(0.55) + n * 0.07
    p.append(pad((0.46, 0.08, 0.85), c, (-a, 0, 0)))                                    # back pad on the sled
    s_ = rail(0.8) + n * 0.16
    for sx in (-1, 1):
        p.append(pad((0.12, 0.22, 0.10), (sx * 0.16, s_.y, s_.z), (-a, 0, 0), r=0.03))   # shoulder pads
        b0 = rail(0.8) + n * 0.07
        p.append(beam((sx * 0.16, b0.y, b0.z), (sx * 0.16, s_.y, s_.z), 0.05))
    p.append(box((0.78, 0.5, 0.05), (0, -0.3, 0.1), (math.radians(-18), 0, 0), bevel=0.012, mat="rubber"))
    return assemble(name, p)


def captain_chair(name="captain_chair"):
    p = _frame_base(-0.2, 0.45, 0.8)
    for sx in (-1, 1):
        p.append(beam((sx * 0.32, 0.35, 0.0), (sx * 0.32, 0.35, 1.55), 0.08))
        p.append(beam((sx * 0.32, 0.35, 1.25), (sx * 0.32, -0.15, 1.25), 0.06))
        p.append(pad((0.1, 0.42, 0.07), (sx * 0.26, 0.12, 1.3), r=0.025))                # forearm pads
        p.append(tube((sx * 0.3, -0.12, 1.22), (sx * 0.3, -0.12, 1.38), 0.018, "rubber"))
        p.append(beam((sx * 0.32, 0.35, 0.3), (sx * 0.2, 0.2, 0.3), 0.05))
    p.append(beam((-0.32, 0.35, 1.55), (0.32, 0.35, 1.55), 0.07))
    for zz in (0.75, 1.15):
        p.append(beam((-0.32, 0.38, zz), (0.32, 0.38, zz), 0.05))
    p.append(pad((0.4, 0.08, 0.6), (0, 0.35, 0.95)))                                     # back pad
    p.append(box((0.4, 0.22, 0.03), (0, 0.2, 0.32), bevel=0.01, mat="rubber"))           # foot step
    return assemble(name, p)


def sled(name="sled"):
    p = []
    for sx in (-1, 1):
        p.append(beam((sx * 0.3, -0.45, 0.025), (sx * 0.3, 0.45, 0.025), 0.06, 0.05))
        p.append(beam((sx * 0.3, -0.45, 0.025), (sx * 0.3, -0.55, 0.09), 0.06, 0.05))
        p.append(tube((sx * 0.22, 0.35, 0.05), (sx * 0.22, 0.35, 1.05), 0.022, "frame"))  # push posts
    p.append(box((0.6, 0.9, 0.04), (0, 0, 0.07), bevel=0.01))
    p.append(tube((0, 0, 0.09), (0, 0, 0.45), 0.025, "steel"))
    p.append(plate((0, 0, 0.13), (0, 0, 1), 0.225, 0.05))
    p.append(plate((0, 0, 0.19), (0, 0, 1), 0.225, 0.05))
    p.append(torus((0, -0.452, 0.075), 0.03, 0.006, (0, 1, 0), "steel", 24, 8))
    return assemble(name, p)


# ---- bodyweight stations --------------------------------------------------------------------
def pull_up_bar(name="pull_up_bar"):
    h = 2.3
    p = []
    for sx in (-1, 1):
        p.append(beam((sx * 0.6, 0, 0.05), (sx * 0.6, 0, h), 0.08))
        p.append(beam((sx * 0.6, -0.5, 0.04), (sx * 0.6, 0.5, 0.04), 0.08, 0.07))
        p.append(beam((sx * 0.6, 0.45, 0.05), (sx * 0.6, 0.0, 0.9), 0.05))
    p.append(tube((-0.64, 0, h - 0.04), (0.64, 0, h - 0.04), 0.016, "steel"))
    return assemble(name, p)


def dip_bars(name="dip_bars"):
    p = []
    for sx in (-1, 1):
        for y in (-0.35, 0.35):
            p.append(beam((sx * 0.27, y, 0.05), (sx * 0.27, y, 1.15), 0.06))
        p.append(tube((sx * 0.27, -0.5, 1.168), (sx * 0.27, 0.5, 1.168), 0.022, "rubber"))
        p.append(beam((sx * 0.27, -0.45, 0.04), (sx * 0.27, 0.45, 0.04), 0.08, 0.06))
    for y in (-0.35, 0.35):
        p.append(beam((-0.27, y, 0.3), (0.27, y, 0.3), 0.05))
    return assemble(name, p)


def gymnastic_rings(name="gymnastic_rings"):
    p = [tube((-0.4, 0, 2.6), (0.4, 0, 2.6), 0.03, "frame")]
    for sx in (-1, 1):
        p.append(beam((sx * 0.25, 0, 2.6), (sx * 0.25, 0, 1.505), 0.035, 0.004, "pad", 0.001, role=ROLE_FLEX))
        p.append(torus((sx * 0.25, 0, 1.38), 0.12, 0.014, (1, 0, 0), "frame", 48, 12))
    return assemble(name, p)


def suspension_trainer(name="suspension_trainer"):
    """Unloaded, both straps hang plumb from the single anchor; handles hang in their strap loops."""
    p = [tube((-0.3, 0, 2.5), (0.3, 0, 2.5), 0.03, "frame"), _carabiner((0, 0, 2.452))]
    p.append(tube((-0.045, 0, 2.432), (0.045, 0, 2.432), 0.005, "steel"))               # anchor link bar
    for sx in (-1, 1):
        x = sx * 0.02
        p.append(beam((x, 0, 2.436), (x, 0, 1.30), 0.035, 0.004, "pad", 0.001, role=ROLE_FLEX))
        p.append(beam((x, 0, 1.31), (x - 0.06, 0, 1.17), 0.035, 0.004, "pad", 0.001))   # handle loop (rigid triangle)
        p.append(beam((x, 0, 1.31), (x + 0.06, 0, 1.17), 0.035, 0.004, "pad", 0.001))
        p.append(tube((x - 0.065, 0, 1.17), (x + 0.065, 0, 1.17), 0.017, "rubber"))
    return assemble(name, p)


def climbing_rope(name="climbing_rope"):
    p = [beam((-0.4, 0, 4.5), (0.4, 0, 4.5), 0.1), torus((0, 0, 4.42), 0.04, 0.008, (1, 0, 0), "steel", 24, 8)]
    p.append(tube((0, 0, 4.43), (0, 0, 0.3), 0.019, "ball", 24, role=ROLE_FLEX))       # hangs plumb
    p.append(tube((0, 0, 0.3), (0, 0, 0.18), 0.026, "rubber"))
    return assemble(name, p)


def human_flag_pole(name="human_flag_pole"):
    p = [tube((0, 0, 0.02), (0, 0, 3.2), 0.024, "steel"), lathe([(0.0005, 0.0), (0.18, 0.0), (0.18, 0.015), (0.05, 0.03), (0.0005, 0.03)], (0, 0, 0), (0, 0, 1), "frame")]
    p.append(lathe([(0.0005, 0.0), (0.12, 0.0), (0.12, 0.012), (0.04, 0.025), (0.0005, 0.025)], (0, 0, 3.2), (0, 0, -1), "frame"))
    return assemble(name, p)


def wall(name="wall"):
    return assemble(name, [box((2.0, 0.15, 2.5), (0, 0, 1.25), bevel=0.01, mat="wall")])


# ---- cardio -----------------------------------------------------------------------------------
def treadmill(name="treadmill"):
    p = [box((0.85, 1.9, 0.18), (0, 0.1, 0.12), bevel=0.04)]
    p.append(box((0.55, 1.6, 0.02), (0, 0.15, 0.22), bevel=0.005, mat="rubber"))           # belt
    p.append(box((0.85, 0.3, 0.2), (0, -0.85, 0.14), bevel=0.05))                           # motor hood
    for sx in (-1, 1):
        p.append(beam((sx * 0.38, -0.8, 0.2), (sx * 0.38, -0.95, 1.25), 0.07))
        p.append(tube((sx * 0.36, -0.9, 1.05), (sx * 0.36, -0.3, 1.0), 0.02, "rubber"))   # side rails
    p.append(box((0.85, 0.2, 0.32), (0, -1.0, 1.35), (math.radians(-25), 0, 0), bevel=0.04))   # console
    p.append(box((0.5, 0.02, 0.22), (0, -0.92, 1.38), (math.radians(-25), 0, 0), bevel=0.01, mat="rubber"))
    return assemble(name, p)


def stationary_bike(name="stationary_bike"):
    p = [beam((0, -0.45, 0.04), (0, 0.55, 0.04), 0.07)]
    for y in (-0.45, 0.55):
        p.append(beam((-0.25, y, 0.035), (0.25, y, 0.035), 0.07, 0.06))
    p.append(lathe([(0.0005, -0.03), (0.24, -0.03), (0.25, 0.0), (0.24, 0.03), (0.0005, 0.03)], (0, -0.35, 0.3), (1, 0, 0), "steel"))
    p.append(beam((0, -0.35, 0.3), (0, 0.05, 0.4), 0.08))
    p.append(beam((0, 0.05, 0.4), (0, 0.17, 0.89), 0.07))                                  # seat post
    p.append(beam((0, -0.35, 0.3), (0, -0.48, 1.08), 0.07))                                # head tube
    p.append(pad((0.2, 0.28, 0.07), (0, 0.17, 0.9), r=0.03))                                  # saddle
    p.append(tube((-0.25, -0.48, 1.08), (0.25, -0.48, 1.08), 0.016, "rubber"))             # handlebar
    p.append(tube((-0.13, -0.1, 0.35), (0.13, -0.1, 0.35), 0.01, "steel"))                 # crank axle in the frame
    for sx in (-1, 1):
        end = Vector((sx * 0.15, -0.1 + sx * 0.12, 0.35 - sx * 0.12))
        p.append(beam((sx * 0.13, -0.1, 0.35), end, 0.025))                                # crank arm
        p.append(box((0.1, 0.12, 0.025), end + Vector((sx * 0.04, 0, 0)), bevel=0.006, mat="rubber"))
    return assemble(name, p)


def air_bike(name="air_bike"):
    p = [beam((0, -0.55, 0.04), (0, 0.55, 0.04), 0.07)]
    for y in (-0.55, 0.55):
        p.append(beam((-0.28, y, 0.035), (0.28, y, 0.035), 0.07, 0.06))
    fan = [(0.0005, -0.09), (0.33, -0.09), (0.35, -0.06), (0.35, 0.06), (0.33, 0.09), (0.0005, 0.09)]
    p.append(lathe(fan, (0, -0.42, 0.42), (1, 0, 0), "frame"))
    p.append(torus((0, -0.42, 0.42), 0.335, 0.012, (1, 0, 0), "steel", 64, 8))
    p.append(beam((0, -0.3, 0.3), (0, 0.15, 0.45), 0.08))
    p.append(beam((0, 0.15, 0.45), (0, 0.24, 0.88), 0.07))
    p.append(pad((0.22, 0.28, 0.08), (0, 0.24, 0.9), r=0.03))
    p.append(tube((-0.26, -0.42, 0.42), (0.26, -0.42, 0.42), 0.012, "steel"))              # fan axle
    for sx in (-1, 1):
        p.append(beam((sx * 0.24, -0.42, 0.42), (sx * 0.3, -0.05, 1.25), 0.04))             # moving arms
        p.append(tube((sx * 0.3, -0.05, 1.2), (sx * 0.3, -0.05, 1.38), 0.018, "rubber"))
    return assemble(name, p)


def elliptical(name="elliptical"):
    p = [beam((0, -0.7, 0.05), (0, 0.9, 0.05), 0.1, 0.08)]
    p.append(lathe([(0.0005, -0.1), (0.26, -0.1), (0.28, -0.06), (0.28, 0.06), (0.26, 0.1), (0.0005, 0.1)], (0, 0.75, 0.32), (1, 0, 0), "frame"))
    p.append(beam((0, -0.6, 0.05), (0, -0.7, 1.4), 0.08))
    p.append(box((0.4, 0.15, 0.25), (0, -0.75, 1.42), (math.radians(-20), 0, 0), bevel=0.03))
    for sx in (-1, 1):
        p.append(beam((sx * 0.12, 0.65, 0.32), (sx * 0.12, -0.2, 0.32), 0.05))              # pedal arms
        p.append(box((0.14, 0.34, 0.04), (sx * 0.12, -0.05, 0.36), bevel=0.01, mat="rubber"))
        p.append(beam((sx * 0.02, -0.63, 0.55), (sx * 0.28, -0.45, 1.6), 0.04))           # handle poles
        p.append(tube((sx * 0.28, -0.45, 1.45), (sx * 0.28, -0.45, 1.62), 0.018, "rubber"))
    return assemble(name, p)


def stair_climber(name="stair_climber"):
    p = [box((0.8, 1.2, 0.12), (0, 0.1, 0.06), bevel=0.03)]
    for i in range(6):                                                                   # visible stair treads
        p.append(box((0.6, 0.3, 0.035), (0, 0.4 - i * 0.18, 0.2 + i * 0.2), bevel=0.01, mat="rubber"))
    for sx in (-1, 1):
        p.append(beam((sx * 0.33, 0.55, 0.1), (sx * 0.33, -0.55, 1.35), 0.06, 0.16))   # side stringers
        p.append(beam((sx * 0.38, 0.2, 0.1), (sx * 0.38, -0.55, 1.6), 0.06))
        p.append(tube((sx * 0.36, 0.15, 1.15), (sx * 0.36, -0.5, 1.45), 0.02, "rubber"))
    p.append(box((0.8, 0.15, 0.25), (0, -0.55, 1.62), (math.radians(-30), 0, 0), bevel=0.03))
    return assemble(name, p)


def rowing_machine(name="rowing_machine"):
    p = [beam((0, -0.9, 0.32), (0, 1.3, 0.32), 0.1, 0.06)]
    p.append(beam((0, 1.3, 0.32), (0, 1.3, 0.04), 0.06))
    p.append(beam((-0.3, 1.3, 0.035), (0.3, 1.3, 0.035), 0.07, 0.06))
    p.append(lathe([(0.0005, -0.12), (0.24, -0.12), (0.26, -0.08), (0.26, 0.08), (0.24, 0.12), (0.0005, 0.12)], (0, -1.05, 0.42), (1, 0, 0), "frame"))
    p.append(beam((0, -0.9, 0.32), (0, -1.0, 0.04), 0.08))
    p.append(beam((-0.3, -1.0, 0.035), (0.3, -1.0, 0.035), 0.07, 0.06))
    p.append(pad((0.3, 0.28, 0.06), (0, 0.25, 0.375), r=0.025))                              # seat on the rail
    for sx in (-1, 1):
        p.append(box((0.12, 0.25, 0.03), (sx * 0.1, -0.65, 0.42), (math.radians(-40), 0, 0), bevel=0.008, mat="rubber"))
    p.append(tube((-0.25, -0.72, 0.55), (0.25, -0.72, 0.55), 0.016, "rubber"))               # handle
    p.append(tube((0, -0.72, 0.55), (0, -1.0, 0.47), 0.004, "steel", role=ROLE_FLEX))     # chain, taut
    return assemble(name, p)


# ---- floor accessories -----------------------------------------------------------------------
def yoga_mat(name="yoga_mat"):
    p = [box((0.61, 1.6, 0.006), (0, -0.1, 0.003), bevel=0.003, mat="pad")]
    p.append(tube((-0.305, 0.70, 0.06), (0.305, 0.70, 0.06), 0.06, "pad"))                 # rolled end
    return assemble(name, p)


def resistance_band(name="resistance_band"):
    # at rest a long loop band lies flat on the floor
    return assemble(name, [torus((0, 0, 0.002), 0.5, 0.015, (0, 0, 1), "ball", 96, 12, role=ROLE_FLEX,
                                 scale=(0.12, 1.0, 0.13))])


def mini_band(name="mini_band"):
    return assemble(name, [torus((0, 0, 0.002), 0.15, 0.025, (0, 0, 1), "ball", 64, 12, role=ROLE_FLEX,
                                 scale=(1.0, 1.0, 0.08))])


# ---- registry: id -> (display name, builder, scan key the exercises come from) ------------------
LIBRARY = [
    # free weights
    ("dumbbell", "Dumbbell", lambda: assemble("dumbbell", [dumbbell("dumbbell")], origin="keep"), "Dumbbell"),
    ("barbell", "Barbell + plates", barbell, "Barbell + plates"),
    ("ez_bar", "EZ curl bar", ez_bar, "EZ curl bar"),
    ("hex_bar", "Hex / trap bar", hex_bar, "Hex / trap bar"),
    ("weight_plate", "Weight plate", weight_plate, "Weight plate (handheld)"),
    ("kettlebell", "Kettlebell", kettlebell, "Kettlebell"),
    ("medicine_ball", "Medicine ball", medicine_ball, "Medicine ball"),
    ("stability_ball", "Stability ball", stability_ball, "Stability / exercise ball"),
    ("ab_wheel", "Ab wheel", ab_wheel, "Ab wheel"),
    ("wrist_roller", "Wrist roller", wrist_roller, "Wrist roller"),
    ("dip_belt", "Dip belt", dip_belt, "Dip belt"),
    ("jump_rope", "Jump rope", jump_rope, "Jump rope"),
    ("battle_ropes", "Battle ropes", battle_ropes, "Battle ropes"),
    # benches & racks
    ("flat_bench", "Flat bench", flat_bench, "Flat bench"),
    ("adjustable_bench", "Adjustable bench", adjustable_bench, "Adjustable bench (incline/decline)"),
    ("decline_situp_bench", "Decline sit-up bench", decline_situp_bench, "Decline sit-up bench"),
    ("preacher_bench", "Preacher bench", preacher_bench, "Preacher bench"),
    ("back_extension_bench", "Back extension bench (45°)", back_extension_bench, "Back extension bench (45° / roman chair)"),
    ("power_rack", "Squat / power rack", power_rack, "Squat / power rack"),
    ("smith_machine", "Smith machine", smith_machine, "Smith machine"),
    ("landmine", "Landmine pivot", landmine, "Landmine pivot"),
    ("plyo_box", "Plyo box", plyo_box, "Plyo box"),
    # cable
    ("cable_station", "Cable station", cable_station, "Cable station (adjustable pulley)"),
    ("lat_pulldown", "Lat pulldown machine", lat_pulldown, "Lat pulldown machine"),
    ("seated_row", "Seated cable row", seated_row, "Seated cable row station"),
    ("att_straight_bar", "Cable straight bar", att_straight_bar, "Cable attachments (straight bar / V-bar / rope / D-handle / ankle strap)"),
    ("att_v_bar", "Cable V-bar", att_v_bar, "Cable attachments (straight bar / V-bar / rope / D-handle / ankle strap)"),
    ("att_rope", "Cable rope", att_rope, "Cable attachments (straight bar / V-bar / rope / D-handle / ankle strap)"),
    ("att_d_handle", "Cable D-handle", att_d_handle, "Cable attachments (straight bar / V-bar / rope / D-handle / ankle strap)"),
    ("att_lat_bar", "Cable lat bar", att_lat_bar, "Lat pulldown machine"),
    ("att_ankle_strap", "Cable ankle strap", att_ankle_strap, "Cable attachments (straight bar / V-bar / rope / D-handle / ankle strap)"),
    # machines
    ("leg_press_45", "Leg press (45°)", leg_press_45, "Leg press (45°)"),
    ("horizontal_leg_press", "Horizontal leg press", horizontal_leg_press, "Horizontal leg press"),
    ("hack_squat", "Hack squat", hack_squat, "Hack squat machine"),
    ("leg_extension", "Leg extension", leg_extension, "Leg extension machine"),
    ("lying_leg_curl", "Lying leg curl", lying_leg_curl, "Lying leg curl machine"),
    ("seated_leg_curl", "Seated leg curl", seated_leg_curl, "Seated leg curl machine"),
    ("hip_abduction_adduction", "Hip abduction / adduction", hip_abduction_adduction, "Hip adduction/abduction machine"),
    ("chest_press", "Chest press", chest_press, "Chest press machine"),
    ("pec_deck", "Pec deck / chest fly", pec_deck, "Pec deck / chest fly machine"),
    ("shoulder_press_machine", "Shoulder press machine", shoulder_press_machine, "Shoulder press machine"),
    ("biceps_curl_machine", "Biceps curl machine", biceps_curl_machine, "Biceps curl machine"),
    ("triceps_extension_machine", "Triceps extension machine", triceps_extension_machine, "Triceps extension machine"),
    ("calf_raise_standing", "Standing calf raise", calf_raise_standing, "Calf raise machines (standing / seated / donkey)"),
    ("calf_raise_seated", "Seated calf raise", calf_raise_seated, "Calf raise machines (standing / seated / donkey)"),
    ("calf_raise_donkey", "Donkey calf raise", calf_raise_donkey, "Calf raise machines (standing / seated / donkey)"),
    ("assisted_pullup_dip", "Assisted pull-up / dip", assisted_pullup_dip, "Assisted pull-up / dip machine"),
    ("back_extension_machine", "Back extension machine", back_extension_machine, "Machine back extension"),
    ("ab_crunch_machine", "Ab crunch machine", ab_crunch_machine, "Ab crunch machine"),
    ("glute_drive", "Glute drive (plate-loaded)", glute_drive, "Plate-loaded machines (glute drive / lateral raise / shrug)"),
    ("lateral_raise_machine", "Lateral raise (plate-loaded)", lateral_raise_machine, "Plate-loaded machines (glute drive / lateral raise / shrug)"),
    ("shrug_machine", "Shrug (plate-loaded)", shrug_machine, "Plate-loaded machines (glute drive / lateral raise / shrug)"),
    ("captain_chair", "Captain's chair", captain_chair, "Captain's chair (knee-raise station)"),
    ("sled", "Sled", sled, "Sled"),
    # bodyweight stations
    ("pull_up_bar", "Pull-up bar", pull_up_bar, "Pull-up bar"),
    ("dip_bars", "Dip bars", dip_bars, "Dip bars / parallel bars"),
    ("gymnastic_rings", "Gymnastic rings", gymnastic_rings, "Gymnastic rings"),
    ("suspension_trainer", "Suspension trainer", suspension_trainer, "Suspension trainer (TRX)"),
    ("climbing_rope", "Climbing rope", climbing_rope, "Climbing rope"),
    ("human_flag_pole", "Vertical pole", human_flag_pole, "Vertical pole (human flag)"),
    ("wall", "Wall", wall, "Wall"),
    # cardio
    ("treadmill", "Treadmill", treadmill, "Treadmill"),
    ("stationary_bike", "Stationary bike", stationary_bike, "Stationary bike"),
    ("air_bike", "Air bike", air_bike, "Air bike"),
    ("elliptical", "Elliptical", elliptical, "Elliptical trainer"),
    ("stair_climber", "Stair climber", stair_climber, "Stair climber"),
    ("rowing_machine", "Rowing machine", rowing_machine, "Rowing machine"),
    # other
    ("yoga_mat", "Yoga mat", yoga_mat, "Yoga mat"),
    ("resistance_band", "Resistance band", resistance_band, "Resistance band (long loop)"),
    ("mini_band", "Mini band", mini_band, "Mini band (short loop)"),
]
