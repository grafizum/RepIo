"""Anatomical muscle highlights: each muscle is placed where it really lies, not painted on.

A muscle is a set of fibre strands. Each strand runs from the muscle's origin to its insertion
(anatomical landmarks on this body, left side; the right side is mirrored). Its control points are
dropped onto the skin facing the muscle's side, and the strand gets a belly profile: narrow at the
tendons, full at the belly (or a fan, wide at the origin, for pecs/lats/glutes). Skin within the
strand's width (and within a shallow depth, so the far side of a limb never lights up) belongs to
the muscle. The result is written as a point attribute `m_<group>` (0..1 with a soft edge) for each
of the 18 muscle groups the app uses; an exercise mixes its groups into `hl` for the shader.
"""
import math

import numpy as np
from mathutils import Vector
from mathutils.kdtree import KDTree

V = Vector

# ---- rest-pose landmarks (A-pose, left side) -------------------------------------------------
S, E, W = V((0.172, 0.006, 1.352)), V((0.2941, 0.0, 1.0905)), V((0.3758, -0.0635, 0.8816))
H, K, A = V((0.093, -0.005, 0.885)), V((0.1389, 0.0173, 0.4478)), V((0.170, 0.038, 0.078))


def on(a, b, t, dx=0.0, dy=0.0, dz=0.0):
    return a.lerp(b, t) + V((dx, dy, dz))


def fan(o0, o1, ins, facing, half_w, bulge=(0, 0, 0), n=8, kind="fan"):
    """Fan-shaped muscle: n strands from points spread along the origin line o0->o1 to one
    insertion, each bowed outward by `bulge` at its middle, overlapping into one smooth sheet."""
    out = []
    for i in range(n):
        o = o0.lerp(o1, i / (n - 1))
        mid = o.lerp(ins, 0.5) + V(bulge)
        out.append(([o, mid, ins], facing, half_w, kind))
    return out


FUSI = "fusiform"   # tendon - belly - tendon
FAN = "fan"         # wide at the origin, narrowing to the insertion
STRIP = "strip"     # even width, rounded ends

# group: [(points, facing, max half-width, profile)]   points run origin -> insertion
MUSCLES = {
    "biceps": [([on(S, E, 0.24, dy=-0.05), on(S, E, 0.58, dy=-0.06), on(S, E, 0.93, dy=-0.045)], (0, -1, 0), 0.032, FUSI)],
    "triceps": [([on(S, E, 0.12, dy=0.05), on(S, E, 0.5, dy=0.06), on(S, E, 0.95, dy=0.04)], (0.2, 1, 0), 0.030, FUSI),
                ([on(S, E, 0.15, dx=0.03, dy=0.035), on(S, E, 0.55, dx=0.045, dy=0.03), on(S, E, 0.9, dx=0.02, dy=0.03)], (1, 0.6, 0), 0.020, FUSI)],
    "front_delts": [([V((0.125, -0.06, 1.405)), V((0.18, -0.06, 1.33)), on(S, E, 0.40, dx=0.02, dy=-0.025)], (0.4, -1, 0.3), 0.024, FUSI)],
    "lateral_delts": [([V((0.185, 0.0, 1.43)), V((0.235, -0.005, 1.36)), on(S, E, 0.40, dx=0.04)], (1, 0, 0.35), 0.028, FUSI)],
    "rear_delts": [([V((0.12, 0.075, 1.405)), V((0.19, 0.06, 1.35)), on(S, E, 0.40, dx=0.02, dy=0.03)], (0.4, 1, 0.3), 0.024, FUSI)],
    "forearms": [([on(E, W, 0.05, dx=-0.02, dy=-0.03), on(E, W, 0.45, dx=-0.01, dy=-0.035), on(E, W, 0.95, dy=-0.02)], (-0.5, -1, 0), 0.022, FUSI),
                 ([on(E, W, 0.05, dx=0.03, dy=0.02), on(E, W, 0.45, dx=0.03, dy=0.02), on(E, W, 0.95, dx=0.02, dy=0.01)], (1, 0.6, 0), 0.020, FUSI)],
    "pectorals": fan(V((0.012, -0.12, 1.14)), V((0.012, -0.12, 1.36)), V((0.155, -0.06, 1.305)), (0, -1, 0.1), 0.030,
                     bulge=(0.0, -0.025, 0.0)) + fan(V((0.03, -0.11, 1.385)), V((0.11, -0.09, 1.395)), V((0.158, -0.06, 1.315)),
                                                   (0, -1, 0.3), 0.020, n=4, kind="fusiform"),
    "abs": [([V((0.035, -0.125, 0.94)), V((0.04, -0.135, 1.08)), V((0.05, -0.13, 1.22))], (0, -1, 0), 0.036, STRIP)],
    "obliques": [([V((0.125, -0.07, 1.17)), V((0.13, -0.07, 1.08)), V((0.115, -0.08, 0.99))], (1, -0.7, 0), 0.034, FUSI),
                 ([V((0.15, -0.02, 1.15)), V((0.15, -0.01, 1.07)), V((0.14, 0.0, 0.99))], (1, -0.1, 0), 0.030, FUSI)],
    "lats": fan(V((0.025, 0.11, 0.99)), V((0.04, 0.105, 1.22)), V((0.15, 0.045, 1.27)), (0.5, 1, 0), 0.034,
                bulge=(0.04, 0.0, 0.0)) + fan(V((0.06, 0.105, 0.98)), V((0.11, 0.08, 0.99)), V((0.15, 0.045, 1.27)),
                                              (0.6, 1, 0), 0.030, n=4, bulge=(0.03, 0.0, 0.0)),
    "upper_traps": fan(V((0.015, 0.06, 1.465)), V((0.015, 0.085, 1.36)), V((0.165, 0.03, 1.415)), (0.3, 0.7, 0.8), 0.024,
                       bulge=(0, 0.01, 0.02), n=6),
    "spinal_erectors": [([V((0.03, 0.11, 0.96)), V((0.035, 0.115, 1.12)), V((0.035, 0.10, 1.30))], (0, 1, 0), 0.022, STRIP)],
    "glutes": fan(V((0.012, 0.12, 0.90)), V((0.13, 0.10, 1.0)), V((0.165, 0.06, 0.80)), (0.3, 1, 0), 0.058,
                  bulge=(0, 0.04, 0), n=12, kind="fusiform"),
    "hip_abductors": [([V((0.15, 0.02, 0.99)), V((0.17, 0.01, 0.92)), V((0.175, 0.0, 0.82))], (1, 0, 0), 0.035, FUSI)],
    "quadriceps": [([on(H, K, 0.08, dy=-0.07), on(H, K, 0.5, dy=-0.075), on(H, K, 0.92, dy=-0.06)], (0, -1, 0), 0.030, FUSI),
                   ([on(H, K, 0.15, dx=0.06, dy=-0.03), on(H, K, 0.55, dx=0.065, dy=-0.03), on(H, K, 0.88, dx=0.04, dy=-0.04)], (1, -0.5, 0), 0.030, FUSI),
                   ([on(H, K, 0.55, dx=-0.04, dy=-0.05), on(H, K, 0.78, dx=-0.045, dy=-0.05), on(H, K, 0.93, dx=-0.03, dy=-0.05)], (-0.6, -1, 0), 0.026, FUSI)],
    "hip_adductors": [([V((0.025, -0.04, 0.83)), on(H, K, 0.35, dx=-0.06, dy=-0.01), on(H, K, 0.72, dx=-0.05)], (-1, -0.3, 0), 0.032, FAN)],
    "hamstrings": [([on(H, K, 0.12, dx=-0.02, dy=0.08), on(H, K, 0.5, dx=-0.02, dy=0.075), on(H, K, 0.93, dx=-0.025, dy=0.05)], (0, 1, 0), 0.026, FUSI),
                   ([on(H, K, 0.12, dx=0.015, dy=0.08), on(H, K, 0.5, dx=0.03, dy=0.07), on(H, K, 0.93, dx=0.035, dy=0.045)], (0.3, 1, 0), 0.026, FUSI)],
    "calves": [([on(K, A, 0.06, dx=-0.02, dy=0.05), on(K, A, 0.30, dx=-0.022, dy=0.065), on(K, A, 0.62, dx=-0.01, dy=0.045)], (-0.2, 1, 0), 0.026, FUSI),
               ([on(K, A, 0.06, dx=0.02, dy=0.05), on(K, A, 0.28, dx=0.022, dy=0.06), on(K, A, 0.55, dx=0.01, dy=0.045)], (0.3, 1, 0), 0.022, FUSI)],
}

# app primary-muscle names -> groups
APP_MUSCLE = {
    "Quadriceps": "quadriceps", "Lats": "lats", "Pectorals": "pectorals", "Glutes": "glutes", "Abs": "abs",
    "Front Deltoids": "front_delts", "Hamstrings": "hamstrings", "Biceps": "biceps", "Triceps": "triceps",
    "Spinal Erectors": "spinal_erectors", "Calves": "calves", "Forearms": "forearms", "Lateral Deltoids": "lateral_delts",
    "Hip Adductors": "hip_adductors", "Rear Deltoids": "rear_delts", "Obliques": "obliques", "Upper Traps": "upper_traps",
    "Hip Abductors": "hip_abductors",
}


def _profile(kind, t):
    if kind == FUSI:
        return max(0.0, math.sin(math.pi * t)) ** 0.6
    if kind == FAN:
        return max(0.0, 1.0 - 0.75 * t) * min(1.0, t / 0.08 + 0.4) * min(1.0, (1 - t) / 0.06 + 0.2)
    return min(1.0, t / 0.08, (1 - t) / 0.08) if 0 < t < 1 else 0.0


def _catmull(pts, n=40):
    p = [pts[0] * 2 - pts[1]] + list(pts) + [pts[-1] * 2 - pts[-2]]
    out = []
    for i in range(1, len(p) - 2):
        for k in range(n):
            t = k / n
            a, b, c, d = p[i - 1], p[i], p[i + 1], p[i + 2]
            out.append(0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t
                              + (-a + 3 * b - 3 * c + d) * t ** 3))
    out.append(pts[-1].copy())
    return out


def build_masks(me, depth=0.022, soft=0.18):
    """Return {group: float32 array} for the rest-pose mesh `me`."""
    n = len(me.vertices)
    co = np.empty(n * 3, np.float32)
    me.vertices.foreach_get("co", co)
    co = co.reshape(n, 3)
    nor = np.empty(n * 3, np.float32)
    me.vertex_normals.foreach_get("vector", nor)
    nor = nor.reshape(n, 3)
    kd = KDTree(n)
    for i, p in enumerate(co):
        kd.insert(p, i)
    kd.balance()

    def drop(p, facing):
        """Nearest skin point to p whose normal faces the muscle's side."""
        f = V(facing).normalized()
        for _, i, _ in kd.find_n(p, 600):
            if V(nor[i]).dot(f) > 0.25:
                return V(co[i]), V(nor[i])
        _, i, _ = kd.find(p)
        return V(co[i]), V(nor[i])

    masks = {}
    for group, strands in MUSCLES.items():
        m = np.zeros(n, np.float32)
        for side in (1, -1):
            for pts, facing, half_w, kind in strands:
                pts = [V((p.x * side, p.y, p.z)) for p in pts]
                fac = (facing[0] * side, facing[1], facing[2])
                dropped = [drop(p, fac) for p in pts]
                spine = _catmull([d[0] for d in dropped])
                sp = np.array(spine, np.float32)
                ns = len(sp)
                # skin normal along the spine (from the nearest skin vertex of each sample)
                snor = np.array([nor[kd.find(V(q))[1]] for q in sp], np.float32)
                tang = np.gradient(sp, axis=0)
                tang /= np.linalg.norm(tang, axis=1, keepdims=True) + 1e-9
                lo, hi = sp.min(0) - half_w - depth, sp.max(0) + half_w + depth
                cand = np.where(np.all((co > lo) & (co < hi), axis=1))[0]
                if not len(cand):
                    continue
                c = co[cand]
                d2 = ((c[:, None, :] - sp[None, :, :]) ** 2).sum(-1)
                j = d2.argmin(1)
                off = c - sp[j]
                along = (off * tang[j]).sum(1)
                dep = (off * snor[j]).sum(1)
                wid = off - tang[j] * along[:, None] - snor[j] * dep[:, None]
                wn = np.linalg.norm(wid, axis=1)
                t = j / (ns - 1)
                prof = np.array([_profile(kind, x) for x in t], np.float32)
                end = ((j == 0) & (along < -0.002)) | ((j == ns - 1) & (along > 0.002))
                e = (wn / np.maximum(half_w * prof, 1e-4)) ** 2 + (dep / depth) ** 2
                val = np.clip((1.0 - e) / soft, 0, 1)
                val[end | (prof < 0.05)] = 0
                m[cand] = np.maximum(m[cand], val)
        masks[group] = m
    return masks


def write_masks(me):
    masks = build_masks(me)
    for g, m in masks.items():
        a = me.attributes.get(f"m_{g}") or me.attributes.new(f"m_{g}", "FLOAT", "POINT")
        a.data.foreach_set("value", m)
    me.update()
    return masks


def set_highlight(me, groups):
    """Mix the chosen groups into the shader's `hl` attribute."""
    n = len(me.vertices)
    out = np.zeros(n, np.float32)
    buf = np.empty(n, np.float32)
    for g in groups:
        me.attributes[f"m_{g}"].data.foreach_get("value", buf)
        np.maximum(out, buf, out=out)
    me.attributes["hl"].data.foreach_set("value", out)
    me.update()
