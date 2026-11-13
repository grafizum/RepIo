"""Barbell Bench Press (catalog id bench-press, primary muscle Pectorals).

Archetype press_horizontal: supine on the flat bench, feet planted, overhand grip ~1.5x shoulder
width; the bar travels in a slight J from locked out over the shoulders to the lower chest, elbows
~45 deg from the torso. a = 0 lockout, a = 1 bar on the chest.

  MODE=test -> lockout / mid / chest stills      MODE=anim -> 21 unique frames (ping-pong loop)
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, "/home/matrix/StudioProjects/RepIo/animation/blender")
import exlib as X  # noqa: E402

V = Vector
SLUG = "barbell-bench-press"
PAD_TOP = 0.47                  # flat_bench pad top (0.43 centre + 0.04)
RIG_LOC = V((0.0, -1.10, PAD_TOP + 0.118))   # back of the ribcage rests on the pad
GRIP_HALF = 0.41                # hand centre distance from the bar centre


def bar_point(a):
    """Bar centre: over the shoulders at lockout, lower chest at the bottom (slight J)."""
    top = V((0.0, 0.245, 1.075))
    bottom = V((0.0, 0.095, 0.742))   # lower chest / sternum
    p = top.lerp(bottom, a)
    p.y += 0.02 * math.sin(math.pi * a)          # the J: drifts a touch toward the face mid-rep
    return p


def build():
    rig = X.scene()
    rig.rotation_euler = (math.radians(-90), 0, 0)   # supine, head toward +Y
    rig.location = RIG_LOC
    X.update()
    X.library_object("flat_bench")
    bar = X.library_object("barbell")
    T = X.add_ik(rig)
    # feet planted wide on the floor, toes pointing away from the bench
    rest_foot = {s: rig.data.bones[f"foot.{s}"].matrix_local.to_3x3() for s in "LR"}
    for s, sx in (("L", 1), ("R", -1)):
        toe_out = Matrix.Rotation(math.radians(-14 * sx), 3, "Z")
        m = (toe_out @ rest_foot[s]).to_4x4()
        m.translation = V((sx * 0.25, -0.74, 0.078))
        T[f"foot.{s}"].matrix_world = m
        T[f"knee.{s}"].location = V((sx * 0.30, -0.62, 1.30))
        T[f"elbow.{s}"].location = V((sx * 0.45, -0.30, 0.30))   # ~45 deg tuck
    X.highlight(["pectorals"])
    state = {"rig": rig, "T": T, "bar": bar}
    set_arms(state, 0.5)
    X.calibrate_poles(rig, T)
    set_arms(state, 0.5)
    X.update()
    pt = bar_point(0.5)
    state["grips"] = {}
    for s, sx in (("L", 1), ("R", -1)):
        state["grips"].update(X.fit_fingers(rig, s, pt + V((sx * GRIP_HALF, 0, 0)), (1, 0, 0), 0.01425))
    X.camera(target=(0, -0.10, 0.72), azimuth=-35, elevation=30, distance=3.7, lens=55)   # from the feet, a touch to the side
    return state


def set_arms(state, a):
    rig, T, bar = state["rig"], state["T"], state["bar"]
    pt = bar_point(a)
    bar.location = pt
    for s, sx in (("L", 1), ("R", -1)):
        hm = X.hand_on_bar(rig, s, pt + V((sx * GRIP_HALF, 0, 0)), (-sx, 0, 0), (0, 0.15, 1))
        X.set_hand(T, s, hm)
    X.update()


def set_pose(state, a):
    rig = state["rig"]
    for pb in rig.pose.bones:
        if pb.name.startswith(("index", "middle", "ring", "pinky", "thumb")):
            pb.rotation_quaternion = (1, 0, 0, 0)
    set_arms(state, a)
    X.apply_fingers(rig, state["grips"])
    X.update()


def main():
    mode = os.environ.get("MODE", "test")
    st = build()
    if mode == "camtest":
        set_pose(st, 0.6)
        for az, el in ((-60, 32), (-35, 30), (-90, 40)):
            X.camera(target=(0, -0.10, 0.70), azimuth=az, elevation=el, distance=3.8, lens=55)
            bpy.context.scene.render.filepath = f"{X.A}/renders/tests/benchcam_{az}_{el}.png"
            bpy.ops.render.render(write_still=True)
        return
    if mode == "test":
        for a, tag in ((0.0, "top"), (0.5, "mid"), (1.0, "bottom")):
            set_pose(st, a)
            bpy.context.scene.render.filepath = f"{X.A}/renders/tests/bench_{tag}.png"
            bpy.ops.render.render(write_still=True)
        return
    X.render_unique(SLUG, lambda a: set_pose(st, a))


main()
