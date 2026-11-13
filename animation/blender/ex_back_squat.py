"""Barbell Back Squat (catalog id squat, primary muscle Quadriceps).

Archetype squat: feet planted shoulder-width, toes out; high bar across the upper traps, hands just
outside the shoulders; the hips sit down and back to parallel while the torso leans forward with a
neutral spine and the knees track over the toes. a = 0 standing, a = 1 bottom.
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, "/home/matrix/StudioProjects/RepIo/animation/blender")
import exlib as X  # noqa: E402

V = Vector
SLUG = "barbell-back-squat"
GRIP_HALF = 0.36
DROP, BACK, LEAN = 0.40, 0.24, 42.0          # bottom: pelvis down/back (m), torso lean (deg)


def build():
    rig = X.scene()
    X.update()
    bar = X.library_object("barbell")
    T = X.add_ik(rig)
    rest_foot = {s: rig.data.bones[f"foot.{s}"].matrix_local.to_3x3() for s in "LR"}
    for s, sx in (("L", 1), ("R", -1)):
        m = (Matrix.Rotation(math.radians(18 * sx), 3, "Z") @ rest_foot[s]).to_4x4()
        m.translation = V((sx * 0.19, 0.035, 0.078))
        T[f"foot.{s}"].matrix_world = m
        T[f"knee.{s}"].location = V((sx * 0.40, -0.70, 0.50))     # knees out over the toes
        T[f"elbow.{s}"].location = V((sx * 0.45, 0.45, 0.95))     # elbows down and back
    X.highlight(["quadriceps"])
    st = {"rig": rig, "T": T, "bar": bar,
          "pelvis_rest": rig.data.bones["pelvis"].matrix_local.copy()}
    set_body(st, 0.5)
    X.calibrate_poles(rig, T)
    set_body(st, 0.5)
    pt = bar_point(st)
    st["grips"] = {}
    for s, sx in (("L", 1), ("R", -1)):
        st["grips"].update(X.fit_fingers(rig, s, pt + V((sx * GRIP_HALF, 0, 0)), (1, 0, 0), 0.01425))
    X.camera(target=(0, 0.0, 0.92), azimuth=-38, elevation=8, distance=3.9, lens=55)
    return st


def bar_point(st):
    """High bar: on the traps, a little below and behind the base of the neck."""
    rig = st["rig"]
    ch = rig.matrix_world @ rig.pose.bones["chest"].matrix
    neck = X.joint(rig, "neck")
    back = -ch.to_3x3().col[2].normalized()          # bone Z points to the front
    up = ch.to_3x3().col[1].normalized()
    return neck + back * 0.085 - up * 0.035


def set_body(st, a):
    rig, T, bar = st["rig"], st["T"], st["bar"]
    for pb in rig.pose.bones:
        if not pb.name.startswith(("index", "middle", "ring", "pinky", "thumb")):
            pb.rotation_quaternion = (1, 0, 0, 0)
    pr = st["pelvis_rest"]
    lean = math.radians(LEAN * a)
    m = Matrix.Rotation(lean, 4, "X") @ pr.to_3x3().to_4x4()
    m.translation = pr.translation + V((0, BACK * a, -DROP * a))
    rig.pose.bones["pelvis"].matrix = m
    X.update()
    # neutral spine, a little extra chest-up, gaze stays forward
    for name, k in (("spine", -0.10), ("chest", -0.06)):
        X.pose.set_local(rig.pose.bones[name], ("X", -LEAN * k * a))
    X.pose.set_local(rig.pose.bones["neck"], ("X", -LEAN * 0.55 * a))
    X.update()
    pt = bar_point(st)
    bar.location = pt
    for s, sx in (("L", 1), ("R", -1)):
        hm = X.hand_on_bar(rig, s, pt + V((sx * GRIP_HALF, 0, 0)), (-sx, 0, 0), (0, -0.25, 1))
        X.set_hand(T, s, hm)
    X.update()


def set_pose(st, a):
    set_body(st, a)
    X.apply_fingers(st["rig"], st["grips"])
    X.update()


def main():
    mode = os.environ.get("MODE", "test")
    st = build()
    if mode == "test":
        for a, tag in ((0.0, "top"), (0.5, "mid"), (1.0, "bottom")):
            set_pose(st, a)
            bpy.context.scene.render.filepath = f"{X.A}/renders/tests/squat_{tag}.png"
            bpy.ops.render.render(write_still=True)
        return
    X.render_unique(SLUG, lambda a: set_pose(st, a))


main()
