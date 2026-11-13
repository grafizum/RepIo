"""Lat Pulldown (catalog id lat-pulldown, primary muscle Lats).

Archetype pulldown: seated facing the stack, thighs locked under the knee roller, wide overhand grip
on the lat bar. The bar travels from arms-extended overhead to the upper chest with a slight lean
back; the cable runs over both pulleys and the selected plates rise exactly as far as the bar
comes down (1:1 cable), while the plates below the pin stay on the stack.
a = 0 arms extended, a = 1 bar at the upper chest. Camera behind/side so the lats are visible.
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, "/home/matrix/StudioProjects/RepIo/animation/blender")
import equipment as EQ  # noqa: E402
import exlib as X  # noqa: E402

V = Vector
SLUG = "lat-pulldown"
TOWER = V((0.0, -0.72, 0.0))                 # stack in front of the user (user faces -Y)
SEAT = V((0.0, 0.15, 0.47))
FRONT_PULLEY = V((0.0, 0.02, 2.30))
ROLLER = V((0.0, -0.03, 0.655))
PIN_PLATE = 5                                 # plates 5..13 are selected (lift), 0..4 stay
GRIP_X = 0.50                                 # hands on the angled ends of the bar


def build_machine():
    """Static frame + moving groups (bar, selected plates) built from the equipment kit."""
    parts = EQ.stack_tower(TOWER.x, TOWER.y, 2.2, 0.36, 0.22)
    pin_z = 0.05 + 0.05 / 2 + PIN_PLATE * 0.05
    moving, static = [], []
    for o in parts:
        cs = [o.matrix_world @ v.co for v in o.data.vertices]
        zmin, zmax = min(c.z for c in cs), max(c.z for c in cs)
        tall = zmax - zmin > 0.2
        near_stack = all(abs(c.x - TOWER.x) < 0.3 and abs(c.y - TOWER.y) < 0.25 for c in cs)
        if not tall and near_stack and zmin >= pin_z - 0.03 and zmax < 1.4:
            moving.append(o)              # selected plates, their pin holes, top plate, pin + knob
        elif tall and zmax - zmin < 2.0 and abs(zmin - 0.82) < 0.06:
            bpy.data.objects.remove(o)    # the old lift cable: rebuilt every frame
        else:
            static.append(o)
    tower_pulley_z = 2.2 - 0.09
    static += [
        EQ.beam((0, TOWER.y, 2.2), (0, FRONT_PULLEY.y, 2.36), 0.08),               # boom
        EQ.pulley(FRONT_PULLEY, (1, 0, 0), 0.05),
        EQ.beam((0, TOWER.y, 0.05), (0, SEAT.y + 0.2, 0.05), 0.08),                  # base rail
        EQ.beam((-0.25, SEAT.y + 0.2, 0.04), (0.25, SEAT.y + 0.2, 0.04), 0.07, 0.06),
        EQ.beam((0, SEAT.y, 0.05), (0, SEAT.y, 0.43), 0.07),                         # seat post
        EQ.seat(SEAT, 0.40, 0.36),
        EQ.beam((0, ROLLER.y, 0.05), (0, ROLLER.y, ROLLER.z - 0.04), 0.05),          # roller post
        EQ.tube(ROLLER + V((-0.21, 0, 0)), ROLLER + V((0.21, 0, 0)), 0.05, "pad"),   # knee roller
        EQ.tube((0, TOWER.y + 0.05, tower_pulley_z + 0.05), FRONT_PULLEY + V((0, -0.05, 0.05)), 0.004,
                "steel"),                                                            # cable along the boom
    ]
    frame = EQ.assemble("lat_pulldown_frame", static, origin="keep")
    stack = EQ.assemble("lat_pulldown_stack", moving, origin="keep")
    bar = EQ.att_lat_bar("lat_bar")              # origin = bar centre
    return frame, stack, bar, tower_pulley_z


def bar_point(a):
    top = V((0.0, 0.10, 1.66))                   # arms extended, a little in front of the face
    bottom = V((0.0, 0.03, 1.13))                # upper chest
    return top.lerp(bottom, a)


def build():
    rig = X.scene()
    frame, stack, bar, tpz = build_machine()
    T = X.add_ik(rig)
    rest_foot = {s: rig.data.bones[f"foot.{s}"].matrix_local.to_3x3() for s in "LR"}
    for s, sx in (("L", 1), ("R", -1)):
        m = (Matrix.Rotation(math.radians(10 * sx), 3, "Z") @ rest_foot[s]).to_4x4()
        m.translation = V((sx * 0.17, -0.27, 0.078))
        T[f"foot.{s}"].matrix_world = m
        T[f"knee.{s}"].location = V((sx * 0.22, -0.80, 0.60))
        T[f"elbow.{s}"].location = V((sx * 0.65, 0.35, 0.80))   # elbows out, down and back
    X.highlight(["lats"])
    st = {"rig": rig, "T": T, "bar": bar, "stack": stack, "tpz": tpz, "cables": [],
          "pelvis_rest": rig.data.bones["pelvis"].matrix_local.copy()}
    set_body(st, 0.5)
    X.calibrate_poles(rig, T)
    set_body(st, 0.5)
    st["grips"] = {}
    for s, sx in (("L", 1), ("R", -1)):
        p, axis = grip_on_bar(bar_point(0.5), sx)
        st["grips"].update(X.fit_fingers(rig, s, p, axis, 0.017))
    X.camera(target=(0, -0.10, 1.25), azimuth=148, elevation=12, distance=4.3, lens=55)
    return st


def grip_on_bar(centre, sx):
    """Point and direction of the bar's angled end where the hand sits (bar shape from att_lat_bar)."""
    p0, p1 = V((sx * 0.45, 0, -0.01)), V((sx * 0.6, 0, -0.06))
    t = (GRIP_X - 0.45) / 0.15
    return centre + p0.lerp(p1, t), (p1 - p0).normalized()


def set_body(st, a):
    rig, T = st["rig"], st["T"]
    for pb in rig.pose.bones:
        if not pb.name.startswith(("index", "middle", "ring", "pinky", "thumb")):
            pb.rotation_quaternion = (1, 0, 0, 0)
    pr = st["pelvis_rest"]
    lean = math.radians(-(6 + 10 * a))                              # lean back while pulling
    m = Matrix.Rotation(lean, 4, "X") @ pr.to_3x3().to_4x4()
    m.translation = pr.translation + V((0, 0.175, -0.285))           # seated: hips on the seat
    rig.pose.bones["pelvis"].matrix = m
    X.update()
    X.pose.set_local(rig.pose.bones["chest"], ("X", -4 * a))         # chest up into the bar
    X.pose.set_local(rig.pose.bones["neck"], ("X", 10 + 6 * a))       # head back out of the bar's path
    X.update()
    c = bar_point(a)
    st["bar"].location = c
    for s, sx in (("L", 1), ("R", -1)):
        p, axis = grip_on_bar(c, sx)
        hm = X.hand_on_bar(rig, s, p, -axis * 1.0 if sx > 0 else -axis, (0, 0.05, 1))
        X.set_hand(T, s, hm)
    # physics: the cable is inextensible - whatever the bar comes down, the selected plates go up
    lift = (bar_point(0.0) - c).length
    st["stack"].location = (0, 0, lift)
    for o in st["cables"]:
        bpy.data.objects.remove(o)
    top_plate_z = 0.05 + 14 * 0.05 + 0.03 + lift
    st["cables"] = [
        EQ.tube((0, TOWER.y, top_plate_z), (0, TOWER.y, st["tpz"] - 0.045), 0.0035, "steel"),  # stack -> pulley
        EQ.tube(FRONT_PULLEY + V((0, 0.0, -0.05)), c + V((0, 0, 0.05)), 0.004, "steel"),     # pulley -> bar
    ]
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
            bpy.context.scene.render.filepath = f"{X.A}/renders/tests/pulldown_{tag}.png"
            bpy.ops.render.render(write_still=True)
        return
    X.render_unique(SLUG, lambda a: set_pose(st, a))


main()
