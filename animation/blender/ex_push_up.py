"""Push-Up (catalog id push-up, primary muscle Pectorals).

Archetype push_up: a rigid plank pivoting about the balls of the feet (toes tucked), hands planted
flat just outside the shoulders, fingers forward; elbows travel back at ~45 deg while the chest
lowers to just above the floor. a = 0 arms extended, a = 1 bottom.
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, "/home/matrix/StudioProjects/RepIo/animation/blender")
import build_character as bc  # noqa: E402
import exlib as X  # noqa: E402

V = Vector
SLUG = "push-up"
BALL_REST = V((0.0, -0.06, 0.015))           # balls of the feet in the rest pose (pivot)
BALL_WORLD = V((0.0, 0.80, 0.0))             # where the pivot sits on the floor
TILT_TOP, TILT_BOTTOM = 22.0, 6.5            # plank angle (deg) arms extended / at the bottom


def body_matrix(a):
    tilt = math.radians(TILT_TOP + (TILT_BOTTOM - TILT_TOP) * a)
    return (Matrix.Translation(BALL_WORLD) @ Matrix.Rotation(-tilt, 4, "X")
            @ Matrix.Rotation(math.radians(90), 4, "X") @ Matrix.Translation(-BALL_REST))


def flat_hand(rig, side, wrist):
    """Hand bone world matrix: palm on the floor, fingers pointing forward (-Y)."""
    f = (lambda v: v) if side == "L" else bc.mirror
    rest = rig.data.bones[f"hand.{side}"].matrix_local.to_3x3()
    y_rest = rest.col[1].normalized()
    ki, kp = f(bc.FINGERS["index"][0]), f(bc.FINGERS["pinky"][0])
    across = (ki - kp).normalized()
    n_rest = across.cross(y_rest).normalized()
    if n_rest.x * (1 if side == "L" else -1) > 0:   # palm normal faces the midline at rest
        n_rest = -n_rest
    y_new, n_new = V((0, -1, 0)), V((0, 0, -1))
    F_rest = Matrix((y_rest, n_rest, y_rest.cross(n_rest))).transposed()
    F_new = Matrix((y_new, n_new, y_new.cross(n_new))).transposed()
    m = (F_new @ F_rest.transposed() @ rest).to_4x4()
    m.translation = wrist
    return m


def build():
    rig = X.scene()
    T = X.add_ik(rig, arms=True, legs=False)
    X.highlight(["pectorals"])
    st = {"rig": rig, "T": T}
    rig.matrix_world = body_matrix(0.0)
    X.update()
    # hands: under and just outside the shoulders (measured at mid-rep), wrist ~4 cm off the floor
    rig.matrix_world = body_matrix(0.5)
    X.update()
    st["hands"] = {}
    for s, sx in (("L", 1), ("R", -1)):
        sh = X.joint(rig, f"upperarm.{s}")
        wrist = V((sh.x + sx * 0.10, sh.y + 0.02, 0.045))
        st["hands"][s] = flat_hand(rig, s, wrist)
        T[f"elbow.{s}"].location = V((sh.x + sx * 0.45, sh.y + 0.40, 0.75))   # elbows back ~45 deg
    set_pose(st, 0.5)
    X.calibrate_poles(rig, T)
    X.camera(target=(0, 0.05, 0.28), azimuth=-40, elevation=6, distance=3.4, lens=55)   # low, from the head end: the chest shows
    return st


def set_pose(st, a):
    rig, T = st["rig"], st["T"]
    rig.matrix_world = body_matrix(a)
    for pb in rig.pose.bones:
        pb.rotation_quaternion = (1, 0, 0, 0)
    for s in "LR":
        X.pose.set_local(rig.pose.bones[f"toe.{s}"], ("X", -55))     # toes tucked on the floor
        X.set_hand(T, s, st["hands"][s])
        for f in ("index", "middle", "ring", "pinky"):                 # relaxed flat fingers
            X.pose.set_local(rig.pose.bones[f"{f}1.{s}"], ("X", -6 * (1 if s == "L" else -1)))
    X.pose.set_local(rig.pose.bones["neck"], ("X", -8))                # head in line, eyes down-forward
    X.update()


def main():
    mode = os.environ.get("MODE", "test")
    st = build()
    if mode == "camtest":
        set_pose(st, 0.7)
        for az, el in ((-40, 6), (-55, 4)):
            X.camera(target=(0, 0.05, 0.28), azimuth=az, elevation=el, distance=3.4, lens=55)
            bpy.context.scene.render.filepath = f"{X.A}/renders/tests/pushcam_{az}_{el}.png"
            bpy.ops.render.render(write_still=True)
        return
    if mode == "test":
        for a, tag in ((0.0, "top"), (0.5, "mid"), (1.0, "bottom")):
            set_pose(st, a)
            bpy.context.scene.render.filepath = f"{X.A}/renders/tests/pushup_{tag}.png"
            bpy.ops.render.render(write_still=True)
        return
    X.render_unique(SLUG, lambda a: set_pose(st, a))


main()
