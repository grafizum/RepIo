"""Posing helpers shared by every exercise.

Convention from build_character.py: every hinge bone (forearm, shin, finger phalanges)
flexes about its LOCAL X axis, and Y runs along the bone (twist / pronation-supination).
"""
import bpy
from mathutils import Matrix, Quaternion, Vector


def load_character(path="/home/matrix/StudioProjects/RepIo/animation/assets/character/mannequin_male.blend"):
    with bpy.data.libraries.load(path) as (src, dst):
        dst.objects = list(src.objects)
    for o in dst.objects:
        bpy.context.collection.objects.link(o)
    rig = next(o for o in dst.objects if o.type == "ARMATURE")
    for pb in rig.pose.bones:
        pb.rotation_mode = "QUATERNION"
    return rig


def reset(rig):
    for pb in rig.pose.bones:
        pb.rotation_quaternion = (1, 0, 0, 0)
        pb.location = (0, 0, 0)
    bpy.context.view_layer.update()


def local_rot(pb, axis, deg):
    """Add a rotation about the bone's own axis ('X', 'Y' or 'Z'), in degrees."""
    import math
    v = {"X": (1, 0, 0), "Y": (0, 1, 0), "Z": (0, 0, 1)}[axis]
    pb.rotation_quaternion = pb.rotation_quaternion @ Quaternion(v, math.radians(deg))
    bpy.context.view_layer.update()


def aim(rig, name, direction, roll_deg=0.0):
    """Point bone `name` along an armature-space direction with the smallest swing."""
    pb = rig.pose.bones[name]
    bpy.context.view_layer.update()
    cur = pb.matrix.copy()
    y = cur.col[1].to_3d().normalized()
    q = y.rotation_difference(Vector(direction).normalized())
    rot = (q.to_matrix() @ cur.to_3x3())
    m = rot.to_4x4()
    m.translation = cur.translation
    pb.matrix = m
    bpy.context.view_layer.update()
    if roll_deg:
        local_rot(pb, "Y", roll_deg)


def set_local(pb, *rots):
    """Replace the bone's rotation with an ordered product of (axis, degrees) local rotations,
    e.g. set_local(forearm, ("X", flex), ("Y", twist)) = flex about the elbow hinge THEN twist."""
    import math
    q = Quaternion()
    for axis, deg in rots:
        v = {"X": (1, 0, 0), "Y": (0, 1, 0), "Z": (0, 0, 1)}[axis]
        q = q @ Quaternion(v, math.radians(deg))
    pb.rotation_quaternion = q
    bpy.context.view_layer.update()


def key(rig, frame, names=None):
    for pb in rig.pose.bones:
        if names is None or pb.name in names:
            pb.keyframe_insert("rotation_quaternion", frame=frame)
            pb.keyframe_insert("location", frame=frame)


FINGER_SIGN = 1.0  # set from the hand close-up test


def grip(rig, side, amount=1.0, thumb=1.0):
    """Close the fingers around a handle. amount 1 = full power grip (~ 3.5 cm bar)."""
    # finger hinge axes are mirrored copies, so the right hand closes with the opposite sign
    sign = FINGER_SIGN * (1 if side == "L" else -1)
    for f, angles in (("index", (62, 78, 45)), ("middle", (66, 80, 45)), ("ring", (70, 80, 45)),
                      ("pinky", (74, 80, 45))):
        for i, a in enumerate(angles):
            set_local(rig.pose.bones[f"{f}{i + 1}.{side}"], ("X", sign * a * amount))
    for i, a in enumerate((25, 30, 35)):
        set_local(rig.pose.bones[f"thumb{i + 1}.{side}"], ("X", sign * a * thumb))
