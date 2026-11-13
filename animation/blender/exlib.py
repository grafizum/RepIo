"""Shared machinery for every exercise script.

- IK limbs: hands and feet are driven by target empties (two-bone IK + copied rotation), so feet
  stay planted and hands stay on the bar whatever the body does.
- Bar grips: a hand is placed on any cylinder (bar, handle) with the same palm/handle relation as
  the approved dumbbell curl grip, then every phalanx is contact-fitted to the cylinder.
- Production plan settings (PRODUCTION_PLAN.md): 20 fps, 2 s loops (40 frames); symmetric motions
  render only 21 unique frames and the packager mirrors them (ping-pong). GPU rendering.
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector

A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import build_character as bc  # noqa: E402  (landmarks only)
import muscles  # noqa: E402
import pose  # noqa: E402
import style  # noqa: E402

V = Vector
FPS, LOOP_FRAMES, UNIQUE = 20, 40, 21
pose.FINGER_SIGN = -1
FINGER_HALF = {1: 0.0095, 2: 0.0082, 3: 0.0070}
CLOSE_SPREAD = 0.75


# ---- scene ------------------------------------------------------------------------------------
def scene(res=1080, samples=16):
    style.reset()
    rig = pose.load_character()
    style.render_settings(res=res, samples=samples)
    sc = bpy.context.scene
    sc.cycles.use_adaptive_sampling = True
    sc.cycles.adaptive_threshold = 0.05
    sc.render.fps = FPS
    style.world()
    style.lights()
    return rig


def body():
    return bpy.data.objects["Mannequin"]


def highlight(groups):
    muscles.set_highlight(body().data, groups)


def library_object(aid):
    """Append one equipment asset from its .blend."""
    with bpy.data.libraries.load(f"{A}/assets/equipment/{aid}.blend") as (src, dst):
        dst.objects = [n for n in src.objects if n == aid]
    o = dst.objects[0]
    bpy.context.collection.objects.link(o)
    return o


def update():
    bpy.context.view_layer.update()


# ---- IK limbs ---------------------------------------------------------------------------------
def _empty(name, size=0.05):
    e = bpy.data.objects.new(name, None)
    e.empty_display_size = size
    bpy.context.collection.objects.link(e)
    return e


def add_ik(rig, arms=True, legs=True):
    """Targets: hand.L/R (wrist position + hand orientation), elbow.L/R poles,
    foot.L/R (ankle position + foot orientation), knee.L/R poles. Returns the target dict."""
    T = {}
    chains = []
    if arms:
        chains += [("forearm", "hand", "elbow", s) for s in "LR"]
    if legs:
        chains += [("shin", "foot", "knee", s) for s in "LR"]
    for lower, end, pole, s in chains:
        tgt, pl = _empty(f"T_{end}.{s}"), _empty(f"P_{pole}.{s}", 0.03)
        c = rig.pose.bones[f"{lower}.{s}"].constraints.new("IK")
        c.target, c.pole_target, c.chain_count = tgt, pl, 2
        c.use_tail = True
        r = rig.pose.bones[f"{end}.{s}"].constraints.new("COPY_ROTATION")
        r.target = tgt
        T[f"{end}.{s}"], T[f"{pole}.{s}"] = tgt, pl
    return T


def calibrate_poles(rig, T):
    """Find each IK constraint's pole angle so the elbow/knee really points at its pole target."""
    for lower, mid, pole in (("forearm", "upperarm", "elbow"), ("shin", "thigh", "knee")):
        for s in "LR":
            key = f"{pole}.{s}"
            if key not in T:
                continue
            c = rig.pose.bones[f"{lower}.{s}"].constraints["IK"]
            best = (-2.0, 0.0)
            for deg in range(-180, 180, 15):
                c.pole_angle = math.radians(deg)
                update()
                joint = rig.matrix_world @ rig.pose.bones[f"{mid}.{s}"].tail
                root = rig.matrix_world @ rig.pose.bones[f"{mid}.{s}"].head
                end = rig.matrix_world @ rig.pose.bones[f"{lower}.{s}"].tail
                axis = (end - root).normalized()
                want = (T[key].matrix_world.translation - root)
                want = (want - axis * want.dot(axis)).normalized()
                got = (joint - root)
                got = (got - axis * got.dot(axis)).normalized()
                best = max(best, (got.dot(want), deg))
            c.pole_angle = math.radians(best[1])


# ---- grips ------------------------------------------------------------------------------------
def _dumbbell_grip_matrix(side):
    """The approved curl grip: handle placement in armature REST space for this hand."""
    f = (lambda v: v) if side == "L" else bc.mirror
    ki, kp = f(bc.FINGERS["index"][0]), f(bc.FINGERS["pinky"][0])
    wrist = f(bc.J["wrist"])
    axis = (ki - kp).normalized()
    mid = (ki + kp) / 2
    down = (mid - wrist).normalized()
    palm = axis.cross(down).normalized()
    if palm.x * (1 if side == "L" else -1) > 0:
        palm = -palm
    centre = mid + palm * (0.0155 + 0.013) + down * 0.016
    y = down.cross(axis).normalized()
    m = Matrix((axis, y, axis.cross(y))).transposed().to_4x4()
    m.translation = centre
    return m


def grip_offset(rig, side):
    """G: handle frame expressed in the hand bone's REST frame (constant for every grip)."""
    hand_rest = rig.data.bones[f"hand.{side}"].matrix_local
    return hand_rest.inverted() @ _dumbbell_grip_matrix(side)


def hand_on_bar(rig, side, point, bar_axis, forearm_dir):
    """World matrix for hand.<side> holding a bar at `point`. bar_axis points toward the index
    finger side; the roll around the bar is chosen so the wrist stays straight along forearm_dir
    (elbow -> wrist)."""
    G = grip_offset(rig, side)
    Ginv = G.inverted()
    a = V(bar_axis).normalized()
    ref = V((0, 0, 1)) if abs(a.z) < 0.9 else V((0, 1, 0))
    y0 = (ref - a * ref.dot(a)).normalized()
    fd = V(forearm_dir).normalized()
    best = None
    for deg in range(0, 360, 3):
        q = Quaternion(a, math.radians(deg))
        y = q @ y0
        frame = Matrix((a, y, a.cross(y))).transposed().to_4x4()
        frame.translation = V(point)
        hm = rig.matrix_world @ Matrix.Identity(4)
        hm = frame @ Ginv                                    # hand bone matrix (world)
        score = hm.to_3x3().col[1].normalized().dot(fd)      # hand's long axis along the forearm
        if best is None or score > best[0]:
            best = (score, hm)
    return best[1]


def set_hand(T, side, hand_world):
    T[f"hand.{side}"].matrix_world = hand_world


def fit_fingers(rig, side, centre, axis, radius):
    """Contact-fit the fingers around a cylinder (rest of the pose must already be set).
    Returns {bone: [(axis, deg), ...]} to replay every frame."""
    out = {}
    sign = pose.FINGER_SIGN * (1 if side == "L" else -1)
    axis = V(axis).normalized()
    update()
    mid = rig.pose.bones[f"middle1.{side}"]
    mdir = (mid.tail - mid.head).normalized()
    spread = {}
    for f in ("index", "ring", "pinky"):
        pb = rig.pose.bones[f"{f}1.{side}"]
        ang = math.degrees((pb.tail - pb.head).normalized().angle(mdir)) * CLOSE_SPREAD
        best = (1e9, 0.0)
        for sg in (1, -1):
            pose.set_local(pb, ("Z", sg * ang))
            best = min(best, (math.degrees((pb.tail - pb.head).normalized().angle(mdir)), sg * ang))
        spread[f] = best[1]
        pose.set_local(pb, ("Z", best[1]))
    for f in ("index", "middle", "ring", "pinky", "thumb"):
        for j in (1, 2, 3):
            pb = rig.pose.bones[f"{f}{j}.{side}"]
            pre = [("Z", spread[f])] if j == 1 and f in spread else []
            target = radius + FINGER_HALF[j]
            best, err_best = 0, 1e9
            for a in range(0, (100 if f != "thumb" else 70) + 1, 2):
                pose.set_local(pb, *pre, ("X", sign * a))
                tip = rig.matrix_world @ pb.tail
                d = tip - V(centre)
                dist = (d - axis * d.dot(axis)).length
                if abs(dist - target) < err_best:
                    best, err_best = a, abs(dist - target)
                if dist <= target:
                    best = a
                    break
            pose.set_local(pb, *pre, ("X", sign * best))
            out[pb.name] = pre + [("X", sign * best)]
    return out


def apply_fingers(rig, grips):
    for name, rots in grips.items():
        pose.set_local(rig.pose.bones[name], *rots)


def joint(rig, name, end="head"):
    pb = rig.pose.bones[name]
    return rig.matrix_world @ (pb.head if end == "head" else pb.tail)


# ---- timing -----------------------------------------------------------------------------------
def ease(t):
    return 0.5 - 0.5 * math.cos(math.pi * max(0.0, min(1.0, t)))


def unique_phases():
    """21 renders from 0 to 1 (eased) - the packager plays them forward then back (40 frames)."""
    return [ease(i / (UNIQUE - 1)) for i in range(UNIQUE)]


def render_unique(slug, set_pose, first=1, last=UNIQUE):
    out = f"{A}/output/{slug}/frames"
    os.makedirs(out, exist_ok=True)
    sc = bpy.context.scene
    for i, a in enumerate(unique_phases(), start=1):
        if i < first or i > last:
            continue
        set_pose(a)
        sc.render.filepath = f"{out}/{i:04d}.png"
        bpy.ops.render.render(write_still=True)


def camera(target, azimuth, elevation, distance, lens=60):
    for o in [o for o in bpy.data.objects if o.type == "CAMERA"]:
        bpy.data.objects.remove(o)
    return style.camera(target=target, azimuth=azimuth, elevation=elevation, distance=distance, lens=lens)
