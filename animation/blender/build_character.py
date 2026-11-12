"""Build the reusable RepDB-style male mannequin -> assets/character/mannequin_male.blend

Source body: Blender Studio "Human Base Meshes" realistic male (CC0), full multires
sculpt detail. Joints come from the mesh's own face-set seams (every limb segment and
finger phalanx is its own face set), so the skeleton sits exactly where the sculpt bends.

Run:  BLENDER_USER_RESOURCES=animation/blender-user blender -b --python animation/blender/build_character.py
"""
import math
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector

A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import muscles  # noqa: E402
import style  # noqa: E402

LIB = f"{A}/third_party/hbm/human-base-meshes-bundle-v1.4.1/human_base_meshes_bundle.blend"
OUT = f"{A}/assets/character/mannequin_male.blend"
V = Vector

# ---- joint landmarks (object space, +X = character's LEFT, -Y = front) --------------------
# Measured from face-set boundary centroids (output/facesets.json); a few are nudged from the
# skin seam into the joint centre (shoulder, hip, ankle) where the seam sits off-centre.
J = {
    "pelvis": V((0, 0.0, 0.92)), "spine": V((0, -0.005, 1.02)), "chest": V((0, -0.01, 1.13)),
    "neck": V((0, -0.012, 1.405)), "head": V((0, -0.03, 1.50)), "head_end": V((0, -0.03, 1.69)),
    "clav": V((0.025, -0.035, 1.395)), "shoulder": V((0.172, 0.006, 1.352)),
    "elbow": V((0.2941, 0.0, 1.0905)), "wrist": V((0.3758, -0.0635, 0.8816)),
    "hand_end": V((0.4192, -0.1106, 0.7956)),
    "hip": V((0.093, -0.005, 0.885)), "knee": V((0.1389, 0.0173, 0.4478)),
    "ankle": V((0.170, 0.038, 0.078)), "ball": V((0.2268, -0.0526, 0.0148)), "toe_end": V((0.248, -0.135, 0.014)),
}
# finger chains: knuckle -> mid -> distal -> (extrapolated tip)
FINGERS = {
    "thumb": [V((0.3699, -0.117, 0.8427)), V((0.3673, -0.1341, 0.8413)), V((0.3697, -0.1581, 0.8175))],
    "index": [V((0.4104, -0.13, 0.805)), V((0.4194, -0.1476, 0.7833)), V((0.42, -0.1605, 0.7598))],
    "middle": [V((0.4192, -0.1106, 0.7956)), V((0.4296, -0.1235, 0.7688)), V((0.4322, -0.1362, 0.7394))],
    "ring": [V((0.4188, -0.0884, 0.7885)), V((0.4302, -0.0983, 0.7593)), V((0.4279, -0.1063, 0.7332))],
    "pinky": [V((0.4107, -0.0661, 0.7858)), V((0.4213, -0.0631, 0.7579)), V((0.4236, -0.0626, 0.7395))],
}
FRONT = V((0, -1, 0))


def mirror(v):
    return V((-v.x, v.y, v.z))


def load_body():
    with bpy.data.libraries.load(LIB) as (src, dst):
        dst.objects = ["GEO-body_male_realistic"]
    o = dst.objects[0]
    bpy.context.collection.objects.link(o)
    o.data = o.data.copy()
    o.location = (0, 0, 0)
    o.name = o.data.name = "Mannequin"
    return o


def apply_mod(o, name):
    with bpy.context.temp_override(object=o, active_object=o, selected_objects=[o]):
        bpy.ops.object.modifier_apply(modifier=name)


def fix_weights(o, rig):
    """Every vertex must follow the skeleton: fold duplicate groups ('head.001') into their bone's
    group and normalise, otherwise those vertices stay behind (the crown 'popped' in the curl)."""
    bones = {b.name for b in rig.data.bones if b.use_deform}
    me = o.data
    for g in list(o.vertex_groups):
        base = g.name.rsplit(".", 1)[0] if g.name not in bones else None
        if base and base in bones:
            dst = o.vertex_groups[base]
            for v in me.vertices:
                for e in v.groups:
                    if e.group == g.index and e.weight > 0:
                        dst.add([v.index], e.weight, "ADD")
            o.vertex_groups.remove(g)
    with bpy.context.temp_override(object=o, active_object=o):
        bpy.ops.object.vertex_group_normalize_all(lock_active=False)
    left = sum(1 for v in me.vertices if sum(e.weight for e in v.groups) < 0.99)
    print("WEIGHTS unweighted after fix:", left)


# ---- skeleton ---------------------------------------------------------------------------------
def build_armature():
    arm = bpy.data.armatures.new("Rig")
    rig = bpy.data.objects.new("Rig", arm)
    bpy.context.collection.objects.link(rig)
    bpy.context.view_layer.objects.active = rig
    bpy.ops.object.mode_set(mode="EDIT")
    eb = arm.edit_bones

    def bone(name, head, tail, parent=None, roll_z=None, connect=False):
        b = eb.new(name)
        b.head, b.tail = head, tail
        if roll_z is not None:
            b.align_roll(roll_z)
        if parent:
            b.parent = eb[parent]
            b.use_connect = connect
        return b

    def hinge_z(head, tail, axis):
        """Z axis that makes local X == `axis` (so flexion is always a local-X rotation)."""
        y = (tail - head).normalized()
        x = (axis - y * axis.dot(y)).normalized()
        return x.cross(y)

    bone("root", V((0, 0, 0)), V((0, 0.25, 0)))
    bone("pelvis", J["pelvis"], J["spine"], "root", roll_z=FRONT)
    bone("spine", J["spine"], J["chest"], "pelvis", roll_z=FRONT, connect=True)
    bone("chest", J["chest"], J["neck"], "spine", roll_z=FRONT, connect=True)
    bone("neck", J["neck"], J["head"], "chest", roll_z=FRONT, connect=True)
    bone("head", J["head"], J["head_end"], "neck", roll_z=FRONT, connect=True)
    lat = V((1, 0, 0))
    for side, f in (("L", lambda v: v), ("R", mirror)):
        s = 1 if side == "L" else -1
        lx = V((s, 0, 0))
        bone(f"clavicle.{side}", f(J["clav"]), f(J["shoulder"]), "chest", roll_z=FRONT)
        # elbow/knee flex about the lateral axis; keep it identical on both sides so +X rotation
        # always means "flex" for arms (forearm toward the front) and legs.
        bone(f"upperarm.{side}", f(J["shoulder"]), f(J["elbow"]), f"clavicle.{side}",
             roll_z=hinge_z(f(J["shoulder"]), f(J["elbow"]), lat))
        bone(f"forearm.{side}", f(J["elbow"]), f(J["wrist"]), f"upperarm.{side}",
             roll_z=hinge_z(f(J["elbow"]), f(J["wrist"]), lat), connect=True)
        bone(f"hand.{side}", f(J["wrist"]), f(J["hand_end"]), f"forearm.{side}",
             roll_z=hinge_z(f(J["wrist"]), f(J["hand_end"]), lat), connect=True)
        # palm normal points to the body midline in the A-pose; fingers curl toward it
        k_index, k_pinky = f(FINGERS["index"][0]), f(FINGERS["pinky"][0])
        across = (k_index - k_pinky).normalized()
        for fname, pts in FINGERS.items():
            pts = [f(p) for p in pts]
            tip = pts[2] + (pts[2] - pts[1]) * 0.9
            chain = pts + [tip]
            axis = across if fname != "thumb" else (f(J["hand_end"]) - f(J["wrist"])).normalized()
            parent = f"hand.{side}"
            for i in range(3):
                n = f"{fname}{i + 1}.{side}"
                bone(n, chain[i], chain[i + 1], parent, roll_z=hinge_z(chain[i], chain[i + 1], axis),
                     connect=(i > 0))
                parent = n
        bone(f"thigh.{side}", f(J["hip"]), f(J["knee"]), "pelvis", roll_z=hinge_z(f(J["hip"]), f(J["knee"]), lat))
        bone(f"shin.{side}", f(J["knee"]), f(J["ankle"]), f"thigh.{side}",
             roll_z=hinge_z(f(J["knee"]), f(J["ankle"]), lat), connect=True)
        bone(f"foot.{side}", f(J["ankle"]), f(J["ball"]), f"shin.{side}",
             roll_z=hinge_z(f(J["ankle"]), f(J["ball"]), lat), connect=True)
        bone(f"toe.{side}", f(J["ball"]), f(J["toe_end"]), f"foot.{side}",
             roll_z=hinge_z(f(J["ball"]), f(J["toe_end"]), lat), connect=True)
    eb["root"].use_deform = False
    bpy.ops.object.mode_set(mode="OBJECT")
    return rig


# ---- mannequin head: the sculpt's own natural head, stylised like a designer mannequin ----------
EYES = [V((sx * 0.0329, -0.1222, 1.5737)) for sx in (1, -1)]   # eyeball centres from the source rig
LID_R = 0.0150   # closed lids sit level with the lid rims (eyeball 12 mm + ~3 mm lid)


def _smooth_region(o, name, weight_fn, iterations, factor=0.8):
    # temp groups get a prefix: a temp group called "head" once shadowed and then deleted the
    # head bone's weights, leaving the crown behind when the character moved
    name = f"_tmp_{name}"
    vg = o.vertex_groups.new(name=name)
    for v in o.data.vertices:
        w = weight_fn(v.co)
        if w > 0:
            vg.add([v.index], min(1.0, w), "REPLACE")
    m = o.modifiers.new(name, "SMOOTH")
    m.factor, m.iterations, m.vertex_group = factor, iterations, name
    apply_mod(o, name)
    o.vertex_groups.remove(o.vertex_groups[name])


def _ramp(x, a, b):
    return min(1.0, max(0.0, (x - a) / (b - a)))


def close_eyes(o):
    """Cut out the open eye (socket shell + inner lid folds) and cap it with a fresh dome at lid
    level: the closed, calm eyelids of a designer mannequin. Pushing the old folds outward left
    back-facing sheets on the dome that rendered as a dark almond."""
    bm = bmesh.new()
    bm.from_mesh(o.data)
    deform = bm.verts.layers.deform.active
    capl = bm.faces.layers.int.new("eye_cap")   # copied to child faces by subdivide
    head_gi = o.vertex_groups["head"].index
    for c in EYES:
        cut = [v for v in bm.verts if (v.co - c).length < 0.0148 and (v.co - c).y < 0.002]
        bmesh.ops.delete(bm, geom=cut, context="VERTS")
        rim = [e for e in bm.edges if e.is_boundary and (e.verts[0].co - c).length < 0.03]
        cap = bmesh.ops.holes_fill(bm, edges=rim, sides=0)["faces"]
        tri = bmesh.ops.triangulate(bm, faces=cap)["faces"]
        for f in tri:
            f[capl] = 1
        for _ in range(3):   # densify only the new cap so it bends as smoothly as the sculpt
            inner = [e for e in {e for f in tri for e in f.edges} if all(lf[capl] for lf in e.link_faces)]
            bmesh.ops.subdivide_edges(bm, edges=inner, cuts=1, use_grid_fill=True)
            tri = [f for f in bm.faces if f[capl]]
        for v in bm.verts:
            d = v.co - c
            if d.length < 0.0152 and d.y < 0.004:      # new cap verts onto the lid dome
                v.co = c + d.normalized() * LID_R
            if d.length < 0.02:                         # eyes move with the head bone only
                v[deform].clear()
                v[deform][head_gi] = 1.0
    bm.faces.layers.int.remove(capl)
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 4])
    bm.to_mesh(o.data)
    bm.free()


def _landmarks(pts):
    """top, chin, nose tip, back of skull and half-width at the temples (no ears) of a head."""
    top = max(p.z for p in pts)
    mid = [p for p in pts if abs(p.x) < 0.006]
    nose = min(p.y for p in mid if top - 0.17 < p.z < top - 0.10)
    chin = min(p.z for p in mid if p.y < nose + 0.045)
    back = max(p.y for p in pts if p.z > top - 0.10)
    temple = max(abs(p.x) for p in pts if abs(p.z - (top - 0.06)) < 0.006)
    return top, chin, nose, back, temple


def close_mouth(o):
    """Remove the inside of the mouth (face set 8 of the source sculpt: the folded inner lips) and
    cap it, so the lips cannot crumple into a mark when the face is projected and deformed."""
    bm = bmesh.new()
    bm.from_mesh(o.data)
    fs = bm.faces.layers.int.get(".sculpt_face_set")
    deform = bm.verts.layers.deform.active
    head_gi = o.vertex_groups["head"].index
    inner = [f for f in bm.faces if f[fs] == 8]
    print("MOUTH faces removed", len(inner))
    bmesh.ops.delete(bm, geom=inner, context="FACES_ONLY")
    bmesh.ops.delete(bm, geom=[v for v in bm.verts if not v.link_faces], context="VERTS")
    rim = [e for e in bm.edges if e.is_boundary and abs(e.verts[0].co.x) < 0.05 and 1.44 < e.verts[0].co.z < 1.54]
    cap = bmesh.ops.holes_fill(bm, edges=rim, sides=0)["faces"]
    tri = bmesh.ops.triangulate(bm, faces=cap)["faces"]
    bmesh.ops.subdivide_edges(bm, edges=list({e for f in tri for e in f.edges if all(lf in tri for lf in e.link_faces)}),
                              cuts=2, use_grid_fill=True)
    for v in bm.verts:
        if abs(v.co.x) < 0.035 and 1.47 < v.co.z < 1.515 and v.co.y < -0.08 and not v[deform]:
            v[deform][head_gi] = 1.0
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 4])
    bm.to_mesh(o.data)
    bm.free()


def close_ears(o):
    """Cut the ears off at the root and cap the openings (their folds would otherwise collapse
    into dark slivers when the head is projected); the cap is then projected like the rest."""
    bm = bmesh.new()
    bm.from_mesh(o.data)
    deform = bm.verts.layers.deform.active
    capl = bm.faces.layers.int.new("ear_cap")
    head_gi = o.vertex_groups["head"].index
    for sx in (1, -1):
        cut = [v for v in bm.verts if v.co.x * sx > 0.071 and 1.50 < v.co.z < 1.63 and -0.045 < v.co.y < 0.045]
        bmesh.ops.delete(bm, geom=cut, context="VERTS")
        rim = [e for e in bm.edges if e.is_boundary and e.verts[0].co.x * sx > 0.05 and e.verts[0].co.z > 1.45]
        tri = bmesh.ops.triangulate(bm, faces=bmesh.ops.holes_fill(bm, edges=rim, sides=0)["faces"])["faces"]
        for f in tri:
            f[capl] = 1
        for _ in range(3):
            inner = [e for e in {e for f in tri for e in f.edges} if all(lf[capl] for lf in e.link_faces)]
            bmesh.ops.subdivide_edges(bm, edges=inner, cuts=1, use_grid_fill=True)
            tri = [f for f in bm.faces if f[capl]]
        for f in tri:
            for v in f.verts:
                v[deform].clear()
                v[deform][head_gi] = 1.0
        for f in tri:
            f[capl] = 0
    bm.faces.layers.int.remove(capl)
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 4])
    bm.to_mesh(o.data)
    bm.free()


def mannequin_head_target(body):
    """CC0 'Head - Generic Topology' (Blender Studio) with its eye and mouth lines smoothed away,
    scaled onto this body's head: the faceless designer head the body is projected onto."""
    with bpy.data.libraries.load(LIB) as (src, dst):
        dst.objects = ["Head - Generic Topology"]
    h = dst.objects[0]
    bpy.context.collection.objects.link(h)
    h.parent = None
    h.matrix_world = Matrix.Identity(4)
    h.data = h.data.copy()
    h.shape_key_clear()
    for m in h.modifiers:
        if m.type == "SUBSURF":
            m.levels = 3
            apply_mod(h, m.name)
    hp = [v.co for v in h.data.vertices]
    ht, hc, hn, hb, hw = _landmarks(hp)
    bp = [v.co for v in body.data.vertices if v.co.z > 1.40 and abs(v.co.x) < 0.12]
    bt, bc, bn, bb, bw = _landmarks(bp)
    sx, sy, sz = bw / hw, (bb - bn) / (hb - hn), (bt - bc) / (ht - hc)
    for v in h.data.vertices:
        c = v.co
        v.co = V((c.x * sx, bn + (c.y - hn) * sy, bc + (c.z - hc) * sz))
    # featureless: smooth the eye band and the mouth on the target, keep skull, brow, nose, jaw
    _smooth_region(h, "eyes", lambda p: (1 - _ramp(abs(p.z - (bc + 0.38 * (bt - bc))), 0.012, 0.022))
                   * _ramp(abs(p.x), 0.008, 0.016) * (1 - _ramp(abs(p.x), 0.05, 0.065)) * _ramp(-p.y, 0.04, 0.07), 40)
    _smooth_region(h, "mouth", lambda p: (1 - _ramp(abs(p.z - (bc + 0.17 * (bt - bc))), 0.016, 0.028))
                   * (1 - _ramp(abs(p.x), 0.03, 0.045)) * _ramp(-p.y, 0.04, 0.07), 80, 1.0)
    # no ears: whatever sticks out past the temple width is the ear; fold it into the skull
    ear = [v.co.copy() for v in h.data.vertices if abs(v.co.x) > bw * 1.03]
    ey = sum(q.y for q in ear) / len(ear)
    ez = sum(q.z for q in ear) / len(ear)
    ear_w = lambda q: (1 - _ramp(abs(q.z - ez), 0.035, 0.05)) * (1 - _ramp(abs(q.y - ey), 0.028, 0.042))
    for v in h.data.vertices:
        q = v.co
        k = ear_w(q)
        if k > 0 and abs(q.x) > bw * 1.0:
            q.x = math.copysign(abs(q.x) + (bw * 1.0 - abs(q.x)) * k, q.x)
    _smooth_region(h, "ears", lambda q: ear_w(q) * 1.3 * _ramp(abs(q.x), 0.045, 0.06), 40, 1.0)
    print("HEAD TARGET scale", round(sx, 3), round(sy, 3), round(sz, 3))
    return h


def head_mask(p):
    """1 above the neck line (under the chin -> nape), 0 below, 4.5 cm soft band."""
    d = (p.z - 1.485) - 0.5 * (p.y + 0.01)
    return min(1.0, max(0.0, (d + 0.006) / 0.045))


def stylize_head(o):
    """Replace the portrait head with the faceless designer head: cut the open eyes (so no folded
    lid sheets survive), then project everything above the neck line radially onto the target."""
    from mathutils.bvhtree import BVHTree
    close_eyes(o)
    close_mouth(o)
    close_ears(o)
    target = mannequin_head_target(o)
    tree = BVHTree.FromObject(target, bpy.context.evaluated_depsgraph_get())
    c = V((0.0, -0.012, 1.565))
    for v in o.data.vertices:
        p = v.co
        if p.z < 1.40 or abs(p.x) > 0.14:
            continue
        k = head_mask(p)
        if k <= 0:
            continue
        hit = tree.ray_cast(c, (p - c).normalized(), 1.0)[0]
        if hit is not None:
            v.co = p.lerp(hit, k)
    bpy.data.objects.remove(target)
    # last trace of a mouth line: smooth the lower face between nose and chin
    _smooth_region(o, "mouth", lambda p: (1 - _ramp(abs(p.x), 0.03, 0.045)) * (1 - _ramp(abs(p.z - 1.490), 0.016, 0.026))
                   * _ramp(-p.y, 0.07, 0.09), 60, 0.9)
    _smooth_region(o, "seam", lambda p: head_mask(p + V((0, 0, 0.03))) * (1 - head_mask(p - V((0, 0, 0.02)))), 12, 0.7)
    _smooth_region(o, "head", lambda p: _ramp(p.z, 1.44, 1.48) * (1 - _ramp(abs(p.x), 0.11, 0.13)), 2, 0.5)


def athletic_shape(o):
    """RepDB's figure is leaner: narrower waist/hips, fuller shoulders."""
    for v in o.data.vertices:
        p = v.co
        if abs(p.x) > 0.24 and p.z < 1.2:     # leave hanging arms alone
            continue
        # waist taper peaking at the navel
        t = math.exp(-((p.z - 1.02) / 0.11) ** 2)
        k = 1 - 0.06 * t
        p.x *= k
        p.y = p.y * (1 - 0.05 * t)
        # shoulders/deltoids slightly broader
        sh = math.exp(-((p.z - 1.33) / 0.07) ** 2) * min(1.0, max(0.0, (abs(p.x) - 0.10) / 0.08))
        p.x *= 1 + 0.035 * sh


# ---- shorts: a shell lifted off the pelvis/thigh skin -------------------------------------
def make_shorts(o):
    """Compression shorts as a region of the body itself (attribute `shorts`), so they fit like
    RepDB's: skin-tight, a clean hem, a faint 2-3 mm lip at the edges, fabric-smooth inside."""
    me = o.data
    waist_front, waist_back = 0.985, 1.005

    def depth(p):
        """Signed distance (m) inside the shorts; > 0 inside."""
        if abs(p.x) > 0.27:
            return -1.0
        waist = waist_front + (waist_back - waist_front) * min(1, max(0, (p.y + 0.12) / 0.24))
        d_waist = waist - p.z
        hip = J["hip"] if p.x >= 0 else mirror(J["hip"])
        knee = J["knee"] if p.x >= 0 else mirror(J["knee"])
        axis = knee - hip
        s = (p - hip).dot(axis) / axis.length_squared
        hem = 0.56 + 0.03 * max(0.0, min(1.0, (abs(p.x) - 0.12) / 0.06))
        d_hem = (hem - s) * axis.length
        return min(d_waist, d_hem)

    dvals = [depth(v.co) for v in me.vertices]
    attr = me.attributes.get("shorts") or me.attributes.new("shorts", "FLOAT", "POINT")
    # 0.5 sits exactly on the boundary; the shader's 0.47..0.53 ramp gives a crisp, anti-aliased hem
    attr.data.foreach_set("value", [min(1.0, max(0.0, 0.5 + d / 0.004)) for d in dvals])
    inner = o.vertex_groups.new(name="shorts_inner")
    lift = o.vertex_groups.new(name="shorts_lift")
    for i, d in enumerate(dvals):
        if d > 0.008:
            inner.add([i], min(1.0, (d - 0.008) / 0.02), "REPLACE")
        if d > -0.0015:
            lift.add([i], min(1.0, (d + 0.0015) / 0.004), "REPLACE")
    # fabric smooths over the sculpt (crotch, buttock crease, quad striations), then lifts 3.5 mm
    m = o.modifiers.new("Fabric", "SMOOTH")
    m.factor, m.iterations, m.vertex_group = 0.8, 14, "shorts_inner"
    apply_mod(o, "Fabric")
    m = o.modifiers.new("Lift", "DISPLACE")
    m.strength, m.mid_level, m.direction, m.vertex_group = 0.0035, 0.0, "NORMAL", "shorts_lift"
    apply_mod(o, "Lift")
    o.vertex_groups.remove(o.vertex_groups["shorts_inner"])
    o.vertex_groups.remove(o.vertex_groups["shorts_lift"])


# ---- target-muscle highlight attribute ------------------------------------------------------
def muscle_attr(o):
    """Point attribute `hl` (0..1) per muscle preset; the skin shader mixes in the orange."""
    me = o.data
    hl = me.attributes.get("hl") or me.attributes.new("hl", "FLOAT", "POINT")
    vals = [0.0] * len(me.vertices)
    for side, f in (("L", lambda v: v), ("R", mirror)):
        a, b = f(J["shoulder"]), f(J["elbow"])
        axis = b - a
        L2 = axis.length_squared
        an = axis.normalized()
        fwd = (FRONT - an * FRONT.dot(an)).normalized()
        for v in me.vertices:
            p = v.co
            s = (p - a).dot(axis) / L2
            if not (0.1 < s < 1.0):
                continue
            radial = (p - a) - an * (p - a).dot(an)
            if radial.length > 0.075:
                continue
            ang = math.degrees(math.acos(max(-1, min(1, radial.normalized().dot(fwd)))))
            e = ((s - 0.60) / 0.30) ** 2 + (ang / 52.0) ** 2
            if e < 1.0:
                vals[v.index] = max(vals[v.index], min(1.0, (1.0 - e) / 0.12))
    hl.data.foreach_set("value", vals)
    me.update()


def main():
    import time
    t0 = time.time()
    step = lambda n: print(f"STEP {n} {time.time() - t0:.1f}s", flush=True)
    style.reset()
    body = load_body()
    # weights are solved on the clean low-res cage, then carried to the sculpt
    cage = body.copy()
    cage.data = body.data.copy()
    cage.modifiers.clear()
    bpy.context.collection.objects.link(cage)
    for m in body.modifiers:
        if m.type == "MULTIRES":
            m.levels = m.render_levels = m.sculpt_levels = 3
            apply_mod(body, m.name)
    rig = build_armature()
    step('rig = build_armature')
    bpy.ops.object.select_all(action="DESELECT")
    cage.select_set(True)
    rig.select_set(True)
    bpy.context.view_layer.objects.active = rig
    bpy.ops.object.parent_set(type="ARMATURE_AUTO")
    step('bpy.ops.object.parent_set')
    dt = body.modifiers.new("Weights", "DATA_TRANSFER")
    dt.object = cage
    dt.use_vert_data = True
    dt.data_types_verts = {"VGROUP_WEIGHTS"}
    dt.vert_mapping = "POLYINTERP_NEAREST"
    with bpy.context.temp_override(object=body, active_object=body):
        bpy.ops.object.datalayout_transfer(modifier="Weights")
    apply_mod(body, "Weights")
    fix_weights(body, rig)
    step('apply_mod')
    # neck: a little fuller (sternomastoid + traps) so the head sits on an athletic neck
    nb = body.vertex_groups.new(name="neck_bulk")
    gi0 = {g.index: g.name for g in body.vertex_groups}
    for v in body.data.vertices:
        w = sum(g.weight for g in v.groups if gi0.get(g.group) == "neck")
        if w > 0.01:
            nb.add([v.index], min(1.0, w), "REPLACE")
    m = body.modifiers.new("NeckBulk", "DISPLACE")
    m.strength, m.mid_level, m.direction, m.vertex_group = 0.003, 0.0, "NORMAL", "neck_bulk"
    apply_mod(body, "NeckBulk")
    body.vertex_groups.remove(body.vertex_groups["neck_bulk"])
    bulk = body.vertex_groups.new(name="arm_bulk")
    me = body.data
    gi = {g.index: g.name for g in body.vertex_groups}
    for v in me.vertices:
        w = sum(g.weight for g in v.groups if gi[g.group].startswith(("upperarm", "forearm")))
        if w > 0.01:
            bulk.add([v.index], min(1.0, w), "REPLACE")
    m = body.modifiers.new("ArmBulk", "DISPLACE")
    m.strength, m.mid_level, m.direction, m.vertex_group = 0.0045, 0.0, "NORMAL", "arm_bulk"
    apply_mod(body, "ArmBulk")
    step('apply_mod')
    body.vertex_groups.remove(body.vertex_groups["arm_bulk"])
    stylize_head(body)
    step('stylize_head')
    athletic_shape(body)
    step('athletic_shape')
    make_shorts(body)
    step('make_shorts')
    bpy.data.objects.remove(cage)
    muscles.write_masks(body.data)
    body.data.attributes.new("hl", "FLOAT", "POINT")
    step('muscle_attr')
    body.data.materials.clear()
    body.data.materials.append(style.skin_material())
    for p in body.data.polygons:
        p.use_smooth = True
    body.parent = rig
    body.modifiers.new("Armature", "ARMATURE").object = rig
    rig.data.display_type = "STICK"
    bpy.ops.wm.save_as_mainfile(filepath=OUT)
    print("SAVED", OUT, len(body.data.vertices), "verts")


if __name__ == "__main__":
    main()
