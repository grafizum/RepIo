"""Standing Dumbbell Curl - first test exercise.

Motion pattern "curl" (reused later for barbell/EZ/cable/kettlebell curls by swapping the
implement): elbows stay pinned at the sides, forearms flex ~130 deg with the palms supinated,
a small forward elbow drift and wrist curl at the top, slower lowering than lifting.

  MODE=test  -> bottom/top stills to check grip and pose
  MODE=anim  -> 60-frame seamless loop (2 s @ 30 fps), PNG RGBA frames
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import build_character as bc  # noqa: E402  (landmarks only)
import equipment  # noqa: E402
import muscles  # noqa: E402
import pose  # noqa: E402
import style  # noqa: E402

SLUG = "standing-dumbbell-curl"
OUT = f"{A}/output/{SLUG}"
FRAMES = 60
TARGET_MUSCLES = ["biceps"]   # app primary muscle: Biceps
HANDLE_R = 0.0155
FINGER_HALF = {1: 0.0095, 2: 0.0082, 3: 0.0070}   # finger half-thickness per phalanx (contact gap)
GRIP = {}                                         # fitted finger rotations, filled once by fit_grip()
CLOSE_SPREAD = float(os.environ.get("CLOSE_SPREAD", "0.75"))  # share of the rest finger spread to close
TWIST = float(os.environ.get("TWIST", "-80"))  # supinated (palms up); -80 verified on the grip close-ups
pose.FINGER_SIGN = -1


def ease(t):
    return t * t * (3 - 2 * t)


def curl_amount(phase):
    """0 = arms down, 1 = top. Up 0..0.42, squeeze to 0.50, lower (slower) to 0.95, settle."""
    if phase < 0.42:
        return ease(phase / 0.42)
    if phase < 0.50:
        return 1.0
    if phase < 0.95:
        return 1.0 - ease((phase - 0.50) / 0.45)
    return 0.0


def grip_matrix(side):
    """Dumbbell placement in armature rest space: handle across the palm, inside the curled fingers."""
    f = (lambda v: v) if side == "L" else bc.mirror
    ki, kp = f(bc.FINGERS["index"][0]), f(bc.FINGERS["pinky"][0])
    wrist = f(bc.J["wrist"])
    axis = (ki - kp).normalized()                      # handle runs index -> pinky knuckle
    mid = (ki + kp) / 2
    down = (mid - wrist).normalized()
    palm = axis.cross(down).normalized()
    if palm.x * (1 if side == "L" else -1) > 0:        # palm faces the body midline at rest
        palm = -palm
    # handle rests just off the palm, clear of the knuckle row, slightly toward the fingers
    centre = mid + palm * (HANDLE_R + 0.013) + down * 0.016
    y = down.cross(axis).normalized()
    rot = Matrix((axis, y, axis.cross(y))).transposed()
    m = rot.to_4x4()
    m.translation = centre
    return m


def _axis_dist(p, c, axis):
    d = p - c
    return (d - axis * d.dot(axis)).length


def fit_grip(rig, side, db):
    """Close every phalanx until it touches the handle: proximal, middle, then distal, each the
    smallest angle that brings its tip onto the handle surface. Done once in the rest pose with
    the body hidden (only the armature evaluates), then replayed every frame from GRIP."""
    c = db.matrix_world.translation.copy()
    axis = db.matrix_world.to_3x3().col[0].normalized()
    sign = pose.FINGER_SIGN * (1 if side == "L" else -1)
    # bring the fingers together first: turn index/ring/pinky toward the middle finger (about the
    # knuckle's palm-normal axis, local Z), closing ~75% of the rest-pose spread
    bpy.context.view_layer.update()
    mid = rig.pose.bones[f"middle1.{side}"]
    mdir = (mid.tail - mid.head).normalized()
    spread = {}
    for f in ("index", "ring", "pinky"):
        pb = rig.pose.bones[f"{f}1.{side}"]
        ang = math.degrees((pb.tail - pb.head).normalized().angle(mdir)) * CLOSE_SPREAD
        best = (1e9, 0.0)
        for sgn in (1, -1):
            pose.set_local(pb, ("Z", sgn * ang))
            d = math.degrees((pb.tail - pb.head).normalized().angle(mdir))
            best = min(best, (d, sgn * ang))
        spread[f] = best[1]
        pose.set_local(pb, ("Z", best[1]))
    for f in ("index", "middle", "ring", "pinky", "thumb"):
        for j in (1, 2, 3):
            pb = rig.pose.bones[f"{f}{j}.{side}"]
            pre = [("Z", spread[f])] if j == 1 and f in spread else []
            target = HANDLE_R + FINGER_HALF[j]
            best, best_err = 0.0, 1e9
            lim = 100 if f != "thumb" else 70
            for a in range(0, lim + 1, 2):
                pose.set_local(pb, *pre, ("X", sign * a))
                tip = rig.matrix_world @ pb.tail
                dist = _axis_dist(tip, c, axis)
                err = abs(dist - target)
                if err < best_err:
                    best, best_err = a, err
                if dist <= target:          # first contact: stop closing this joint
                    best = a
                    break
            pose.set_local(pb, *pre, ("X", sign * best))
            GRIP[pb.name] = pre + [("X", sign * best)]
    print("GRIP", side, "spread", {k: round(v, 1) for k, v in spread.items()})


def set_pose(rig, a):
    """a = curl amount 0..1."""
    pose.reset(rig)
    # torso: tiny lean back as the weight comes up (bracing), head steady
    pose.set_local(rig.pose.bones["spine"], ("X", 2.0 * a))
    pose.set_local(rig.pose.bones["neck"], ("X", -1.5 * a))
    for side, s in (("L", 1), ("R", -1)):
        # elbows pinned at the sides; drift ~10 deg forward at the top
        fwd = 0.02 + 0.17 * a
        pose.aim(rig, f"upperarm.{side}", (s * (0.15 - 0.08 * a), -fwd, -1))
        pose.set_local(rig.pose.bones[f"forearm.{side}"], ("X", -(10 + 120 * a)), ("Y", TWIST * s))
        pose.set_local(rig.pose.bones[f"hand.{side}"], ("X", -8 * a), ("Z", 4 * s))
        for name, ang in GRIP.items():
            if name.endswith(side):
                pose.set_local(rig.pose.bones[name], *ang)


def scene(res, samples):
    style.reset()
    rig = pose.load_character()
    style.render_settings(res=res, samples=samples)
    style.world()
    style.lights()
    for side in ("L", "R"):
        db = equipment.dumbbell(f"Dumbbell.{side}")
        db.parent = rig
        db.parent_type = "BONE"
        db.parent_bone = f"hand.{side}"
        bpy.context.view_layer.update()
        db.matrix_world = rig.matrix_world @ grip_matrix(side)
    body = bpy.data.objects["Mannequin"]
    for m in body.modifiers:
        m.show_viewport = False                    # armature-only evaluation while fitting
    for side in ("L", "R"):
        fit_grip(rig, side, bpy.data.objects[f"Dumbbell.{side}"])
    for m in body.modifiers:
        m.show_viewport = True
    muscles.set_highlight(bpy.data.objects["Mannequin"].data, TARGET_MUSCLES)
    style.camera(target=(0, -0.02, 0.98), azimuth=-28, elevation=9, distance=3.75, lens=60)
    return rig


def main():
    mode = os.environ.get("MODE", "test")
    os.makedirs(f"{OUT}/frames", exist_ok=True)
    if mode == "test":
        rig = scene(700, 32)
        for a, tag in ((0.0, "bottom"), (1.0, "top"), (0.5, "mid")):
            set_pose(rig, a)
            bpy.context.scene.render.filepath = f"{A}/renders/tests/curl_{tag}.png"
            bpy.ops.render.render(write_still=True)
        return
    rig = scene(int(os.environ.get("RES", 1080)), int(os.environ.get("SAMPLES", 64)))
    sc = bpy.context.scene
    sc.frame_start, sc.frame_end = 1, FRAMES
    first, last = int(os.environ.get("F0", 1)), int(os.environ.get("F1", FRAMES))
    for f in range(first, last + 1):
        set_pose(rig, curl_amount((f - 1) / FRAMES))
        sc.render.filepath = f"{OUT}/frames/{f:04d}.png"
        bpy.ops.render.render(write_still=True)


main()
