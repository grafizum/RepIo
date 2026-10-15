#!/usr/bin/env python3
"""Build the muscle glyphs as COMPUTED GEOMETRY, not hand-placed curves.

Owner, 2026-10-12: *"i dont want you to draw them i want you to make mathematical
lines to make it look realistic with the same design of the arm"* and *"draw it but
through coding."* The arm (ic_muscle_arms) is the style authority and stays as it is -
the other five are rebuilt here so every curve in them is a computed object:

  * arc3        - the exact circle through three designed points (circumcircle),
                  emitted as kappa cubics (k = 4/3*tan(theta/4), error < 0.03%).
  * arc_tan     - the circle through an end point tangent to a prescribed direction
                  at the other end (solved in closed form), used where two curves
                  must meet without a corner, e.g. the delt dome into its edges.
  * catmull     - uniform Catmull-Rom through designed points: C1 everywhere by
                  construction, reproduces straight runs of collinear points exactly.
  * mirror      - exact axial symmetry x -> 24-x, so the rectus abdominis is
                  symmetric to machine precision rather than by eye.

Everything lands in the lane [MuscleGlyphFamilyTest] measures: stroke 1.6, round
caps/joins, nothing filled, 24dp on the 24x24 viewport, ink inside the margins the
test requires (>= 2px at 192px render).

Usage:
    python3 math_glyphs.py --show [name|all]    ASCII render for judging
    python3 math_glyphs.py --write              emit the five drawables
"""
from __future__ import annotations

import math
import re
import sys
from pathlib import Path

TAU = 2.0 * math.pi
REPO = Path(__file__).resolve().parents[2]
DRAWABLES = REPO / "composeApp/src/main/res/drawable"

Vec = tuple[float, float]


# ------------------------------------------------------------------ primitives


def _norm(v: Vec) -> Vec:
    m = math.hypot(v[0], v[1])
    if m < 1e-12:
        raise ValueError("zero-length vector")
    return (v[0] / m, v[1] / m)


def _frac(a: float) -> float:
    return a % TAU


def arc_cubics(cx: float, cy: float, r: float, a0: float, a1: float) -> list:
    """Circle arc a0 -> a1 (radians) as cubic segments, each at most 90 degrees."""
    out = []
    total = a1 - a0
    n = max(1, math.ceil(abs(total) / (math.pi / 2) - 1e-9))
    step = total / n
    for i in range(n):
        s = a0 + i * step
        e = s + step
        k = 4.0 / 3.0 * math.tan((e - s) / 4.0)
        p0 = (cx + r * math.cos(s), cy + r * math.sin(s))
        p1 = (cx + r * math.cos(e), cy + r * math.sin(e))
        t0 = (-r * math.sin(s), r * math.cos(s))
        t1 = (-r * math.sin(e), r * math.cos(e))
        out.append(
            (
                "C",
                (p0[0] + k * t0[0], p0[1] + k * t0[1]),
                (p1[0] - k * t1[0], p1[1] - k * t1[1]),
                p1,
            )
        )
    return out


def circumcircle(p0: Vec, p1: Vec, p2: Vec) -> tuple[float, float, float]:
    (ax, ay), (bx, by), (cx, cy) = p0, p1, p2
    d = 2.0 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by))
    if abs(d) < 1e-9:
        raise ValueError("colinear points - use a line instead")
    s0, s1, s2 = ax * ax + ay * ay, bx * bx + by * by, cx * cx + cy * cy
    ux = (s0 * (by - cy) + s1 * (cy - ay) + s2 * (ay - by)) / d
    uy = (s0 * (cx - bx) + s1 * (ax - cx) + s2 * (bx - ax)) / d
    return ux, uy, math.hypot(ax - ux, ay - uy)


def arc3(p0: Vec, pm: Vec, p1: Vec) -> list:
    """Exact circle arc from p0 through pm to p1 (as cubic segments)."""
    try:
        cx, cy, r = circumcircle(p0, pm, p1)
    except ValueError:
        # Straight run: emit the line itself.
        return [("L", p1)]
    a0 = math.atan2(p0[1] - cy, p0[0] - cx)
    am = math.atan2(pm[1] - cy, pm[0] - cx)
    a1 = math.atan2(p1[1] - cy, p1[0] - cx)
    if _frac(am - a0) <= _frac(a1 - a0):
        a_end = a0 + _frac(a1 - a0)
    else:
        a_end = a0 - _frac(a0 - a1)
    return arc_cubics(cx, cy, r, a0, a_end)


def _circle_through_tangent(p: Vec, t: Vec, e: Vec) -> tuple[Vec, float]:
    """The circle through e that is tangent to direction t at p.

    Centre lies on the line through p perpendicular to t: C = p + r*n.
    |C - e| = r gives r = -|p-e|^2 / (2 n.(p-e)) in closed form; n is the
    perpendicular for which that r comes out positive.
    """
    tx, ty = _norm(t)
    d = (p[0] - e[0], p[1] - e[1])
    n = (ty, -tx)
    if n[0] * d[0] + n[1] * d[1] > 0:
        n = (-ty, tx)
    dd = d[0] * d[0] + d[1] * d[1]
    r = -dd / (2.0 * (n[0] * d[0] + n[1] * d[1]))
    return (p[0] + r * n[0], p[1] + r * n[1]), abs(r)


def arc_tan(p: Vec, t: Vec, e: Vec, *, at_start: bool = True) -> list:
    """Arc from p to e tangent to direction t at p (or at e when at_start=False)."""
    if at_start:
        (cx, cy), r = _circle_through_tangent(p, t, e)
        a_p = math.atan2(p[1] - cy, p[0] - cx)
        a_e = math.atan2(e[1] - cy, e[0] - cx)
        n = _norm((t[0], t[1]))
        u = ((p[0] - cx) / r, (p[1] - cy) / r)
        t_ccw = (-u[1], u[0])
        increasing = t_ccw[0] * n[0] + t_ccw[1] * n[1] >= 0
        if increasing:
            a_end = a_p + _frac(a_e - a_p)
        else:
            a_end = a_p - _frac(a_p - a_e)
        return arc_cubics(cx, cy, r, a_p, a_end)
    # arriving at e with direction t  ==  leaving e with direction -t
    rev = arc_tan(e, (-t[0], -t[1]), p, at_start=True)
    nodes: list[Vec] = [e]
    for s in rev:
        nodes.append(s[-1])
    out: list = []
    for i in range(len(rev) - 1, -1, -1):
        # seg i runs nodes[i] -> nodes[i+1]; walk it backwards with swapped controls
        _, c1, c2, _ = rev[i]
        out.append(("C", c2, c1, nodes[i]))
    return out


def catmull(points: list[Vec], *, closed: bool = False) -> list:
    """Uniform Catmull-Rom through points as cubic segments (C1 everywhere)."""
    n = len(points)

    def P(i: int) -> Vec:
        if closed:
            return points[i % n]
        return points[min(max(i, 0), n - 1)]

    out = []
    count = n if closed else n - 1
    for i in range(count):
        p0, p1, p2, p3 = P(i - 1), P(i), P(i + 1), P(i + 2)
        c1 = (p1[0] + (p2[0] - p0[0]) / 6.0, p1[1] + (p2[1] - p0[1]) / 6.0)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6.0, p2[1] - (p3[1] - p1[1]) / 6.0)
        out.append(("C", c1, c2, p2))
    return out


def mirror(points: list[Vec]) -> list[Vec]:
    """Exact axial symmetry about x = 12."""
    return [(24.0 - x, y) for x, y in points]


# ------------------------------------------------------------------ path emission


def _f(v: float) -> str:
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def pathdata(start: Vec, segs: list, close: bool = False) -> str:
    parts = [f"M{_f(start[0])},{_f(start[1])}"]
    for s in segs:
        if s[0] == "C":
            _, c1, c2, p = s
            parts.append(f"C{_f(c1[0])},{_f(c1[1])} {_f(c2[0])},{_f(c2[1])} {_f(p[0])},{_f(p[1])}")
        else:
            _, p = s
            parts.append(f"L{_f(p[0])},{_f(p[1])}")
    if close:
        parts.append("Z")
    return "".join(parts)


def points_of(start: Vec, segs: list) -> list[Vec]:
    pts = [start]
    for s in segs:
        pts.append(s[-1])
    return pts


# ------------------------------------------------------------------ the five glyphs


def g_chest() -> list[tuple[str, str]]:
    """ONE pectoral, redrawn from zero for legibility: a WEDGE slab - straight-ish
    sternum edge, clavicle rising to the shoulder, rounded cap, deep hanging
    under-curve - with two fibers fanning into the insertion."""
    s1 = (6.0, 8.6)    # sternum notch
    a1 = (16.4, 6.4)   # clavicle end
    a2 = (19.4, 11.8)  # armpit corner
    s2 = (6.4, 14.4)   # sternum bottom
    outline = (
        arc3(s1, (11.4, 6.8), a1)
        + arc3(a1, (19.7, 8.9), a2)
        + arc3(a2, (12.6, 17.2), s2)
        + arc3(s2, (5.2, 11.6), s1)
    )
    f_top = arc3((7.6, 9.5), (12.4, 9.2), (17.0, 9.6))
    f_bot = arc3((7.7, 13.6), (12.6, 13.4), (17.2, 11.4))
    return [
        ("outline", pathdata(s1, outline, close=True)),
        ("upper fiber", pathdata((7.6, 9.5), f_top)),
        ("lower fiber", pathdata((7.7, 13.6), f_bot)),
    ]


def g_shoulder() -> list[tuple[str, str]]:
    """ONE shoulder, redrawn from zero for legibility: the TOP OF AN ARM - a true
    half-circle delt cap on a short stump cut straight across, the trap line running
    off toward the neck, and two small muscle lines inside the cap (the concentric
    delt head arc and the insertion crease), the way the arm carries its bicep arc
    and crease."""
    cd = (11.4, 13.2)
    r = 5.4
    dome_l = (cd[0] - r, cd[1])
    dome_r = (cd[0] + r, cd[1])
    cut_f = (7.4, 19.6)
    cut_r = (15.4, 19.6)
    loop = (
        arc_tan(cut_f, (0.0, -1.0), dome_l, at_start=False)
        + arc_cubics(cd[0], cd[1], r, math.pi, 2 * math.pi)
        + arc_tan(dome_r, (0.0, 1.0), cut_r, at_start=True)
    )
    # trap: branches off the dome at 315 degrees, up toward the neck
    a_trap = math.radians(315)
    trap_p = (cd[0] + r * math.cos(a_trap), cd[1] + r * math.sin(a_trap))
    trap = arc3(trap_p, (17.0, 8.4), (18.9, 7.9))
    # head line: exact concentric offset of the dome (r - 2.6), over the top
    a_h0, a_h1 = math.radians(205), math.radians(335)
    head_start = (cd[0] + (r - 2.6) * math.cos(a_h0), cd[1] + (r - 2.6) * math.sin(a_h0))
    head = arc_cubics(cd[0], cd[1], r - 2.6, a_h0, a_h1)
    crease = arc3((8.6, 16.8), (11.4, 17.9), (14.2, 16.8))
    return [
        ("loop", pathdata(cut_f, loop, close=True)),
        ("trap", pathdata(trap_p, trap)),
        ("head line", pathdata(head_start, head)),
        ("crease", pathdata((8.6, 16.8), crease)),
    ]


def g_leg() -> list[tuple[str, str]]:
    """ONE leg in profile, redrawn from zero for legibility: the bulges are pushed
    until the S-curve is unmistakable - quad peaking hard off the front, calf off the
    back, a real notch at the knee - and three small muscle lines do the defining."""
    front = catmull(
        [
            (11.0, 3.6),
            (9.0, 6.2),
            (7.9, 9.6),
            (9.2, 13.6),
            (9.6, 17.2),
            (9.8, 19.0),
        ]
    )
    instep = arc3((9.8, 19.0), (7.4, 21.2), (5.7, 21.7))
    sole = [("L", (16.3, 21.7))]
    heel = arc3((16.3, 21.7), (17.6, 20.9), (17.7, 19.4))
    back = catmull([(17.7, 19.4), (18.2, 15.2), (15.3, 13.2), (17.2, 8.4), (16.8, 3.6)])
    outline = front + instep + sole + heel + back
    quad = arc3((9.2, 7.4), (11.4, 10.6), (13.8, 11.8))
    knee = arc3((9.9, 13.4), (11.1, 14.8), (12.5, 14.5))
    calf = arc3((14.0, 14.6), (15.2, 16.2), (15.0, 18.0))
    return [
        ("outline", pathdata((11.0, 3.6), outline)),
        ("quad sweep", pathdata((9.2, 7.4), quad)),
        ("kneecap", pathdata((9.9, 13.4), knee)),
        ("calf curve", pathdata((14.0, 14.6), calf)),
    ]


def g_back() -> list[tuple[str, str]]:
    """ONE latissimus, redrawn from zero for legibility: a HAND FAN - the spine-side
    edge is exactly straight (collinear ring points), the outer edge is one big
    circular bulge, and THREE fibers radiate toward it like fan ribs."""
    ring = [
        (18.2, 8.4),    # armpit corner
        (14.0, 6.8),
        (10.6, 6.6),
        (7.6, 8.0),
        (5.7, 10.8),
        (5.5, 14.0),
        (6.6, 17.6),
        (8.8, 20.0),
        (11.8, 21.2),
        (14.8, 20.9),
        (16.4, 19.9),
        (17.28, 15.73),  # straight run back up to the armpit: three collinear points
        (17.74, 12.07),
    ]
    f1 = arc3((16.2, 10.2), (11.6, 10.6), (7.6, 12.0))
    f2 = arc3((15.9, 12.4), (11.2, 13.8), (8.0, 15.8))
    f3 = arc3((15.4, 15.6), (11.6, 17.2), (8.6, 18.3))
    return [
        ("outline", pathdata(ring[0], catmull(ring, closed=True), close=True)),
        ("fiber 1", pathdata((16.2, 10.2), f1)),
        ("fiber 2", pathdata((15.9, 12.4), f2)),
        ("fiber 3", pathdata((15.4, 15.6), f3)),
    ]


def g_core() -> list[tuple[str, str]]:
    """THE rectus abdominis: rib arch, machine-symmetric flanks, notch, detail lines."""
    arch = arc3((7.2, 5.0), (12.0, 3.9), (16.8, 5.0))
    right = [
        (16.8, 5.0),
        (18.1, 8.6),
        (18.0, 11.8),
        (17.3, 14.8),
        (16.4, 17.7),
        (15.2, 20.1),
        (14.1, 21.1),
        (13.1, 21.2),
        (12.0, 19.7),
    ]
    left = mirror(right[-2::-1])  # from the notch back up the mirrored left side
    outline = arch + catmull(right + left)
    alba = [("L", (12.0, 18.6))]
    int1 = arc3((7.8, 9.6), (12.0, 10.3), (16.2, 9.6))
    int2 = arc3((7.7, 13.2), (12.0, 13.9), (16.1, 13.2))
    int3 = arc3((8.5, 16.7), (12.0, 17.4), (15.2, 16.7))
    return [
        ("outline", pathdata((7.2, 5.0), outline, close=True)),
        ("linea alba", pathdata((12.0, 6.9), alba)),
        ("intersection 1", pathdata((7.8, 9.6), int1)),
        ("intersection 2", pathdata((7.7, 13.2), int2)),
        ("intersection 3", pathdata((8.5, 16.7), int3)),
    ]


GLYPHS = {
    "chest": g_chest,
    "shoulders": g_shoulder,
    "legs": g_leg,
    "back": g_back,
    "core": g_core,
}


# ------------------------------------------------------------------ ASCII preview


def show(name: str, cols: int = 64, rows: int = 44) -> None:
    strokes = GLYPHS[name]()
    grid = [[" "] * cols for _ in range(rows)]
    xs, ys = [], []
    for _, d in strokes:
        # parse emitted pathData with a minimal walker
        toks = re.findall(r"[MLCZ]|-?\d+\.?\d*", d)
        i = 0
        cur = None
        pts: list[Vec] = []
        while i < len(toks):
            c = toks[i]
            if c == "M":
                cur = (float(toks[i + 1]), float(toks[i + 2]))
                i += 3
            elif c == "C":
                c1 = (float(toks[i + 1]), float(toks[i + 2]))
                c2 = (float(toks[i + 3]), float(toks[i + 4]))
                p = (float(toks[i + 5]), float(toks[i + 6]))
                i += 7
                p0 = cur
                for k in range(1, 25):
                    t = k / 24
                    mt = 1 - t
                    pts.append(
                        (
                            mt**3 * p0[0] + 3 * mt * mt * t * c1[0] + 3 * mt * t * t * c2[0] + t**3 * p[0],
                            mt**3 * p0[1] + 3 * mt * mt * t * c1[1] + 3 * mt * t * t * c2[1] + t**3 * p[1],
                        )
                    )
                cur = p
            elif c == "L":
                p = (float(toks[i + 1]), float(toks[i + 2]))
                i += 3
                a = cur
                n = max(2, int(max(abs(p[0] - a[0]), abs(p[1] - a[1])) * 8))
                for k in range(1, n + 1):
                    t = k / n
                    pts.append((a[0] + (p[0] - a[0]) * t, a[1] + (p[1] - a[1]) * t))
                cur = p
            else:  # Z - close back to the stroke start
                i += 1
                if cur is not None and cur != pts[0]:
                    a, b = cur, pts[0]
                    n = max(2, int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) * 8))
                    for k in range(1, n + 1):
                        t = k / n
                        pts.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t))
                    cur = b
        xs += [p[0] for p in pts]
        ys += [p[1] for p in pts]

        def put(x: float, y: float) -> None:
            cx, cy = int(x / 24 * cols), int(y / 24 * rows)
            if 0 <= cx < cols and 0 <= cy < rows:
                grid[cy][cx] = "#"

        for a, b in zip(pts, pts[1:]):
            n = max(2, int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) * 8))
            for k in range(n + 1):
                t = k / n
                put(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
    print(
        f"{name}: ink x {min(xs):.1f}..{max(xs):.1f} y {min(ys):.1f}..{max(ys):.1f} "
        f"(margins L{min(xs):.1f} R{24 - max(xs):.1f} T{min(ys):.1f} B{24 - max(ys):.1f})"
    )
    print("    " + "".join(str((i // 10) % 10) for i in range(cols)))
    for ry, row in enumerate(grid):
        print(f"{ry:3d} " + "".join(row))


# ------------------------------------------------------------------ XML emission

HEADERS = {
    "chest": """<!-- MUSCLE GLYPH - CHEST. ONE PECTORAL, built 2026-10-12 by CODE rather than by
     hand - owner: *"i dont want you to draw them i want you to make mathematical lines
     to make it look realistic with the same design of the arm"* and *"draw it but
     through     coding."* REMADE FROM ZERO for legibility (owner: *"your current drawings are not
     even nearly as understandable as the arm image"*): a WEDGE - straight-ish sternum
     edge, clavicle rising to the shoulder, rounded cap, deep hanging under-curve - with
     two fibers fanning into the insertion, wide at the sternum, close at the shoulder.
     Four circumcircle arcs + two fiber arcs, all computed.
     Generated by evidence/muscle-icons-2026-10-11/math_glyphs.py.

     Style: the reference arm (ic_muscle_arms) stays the style authority - stroke 1.6,
     round caps and joins, 24dp on the 24x24 viewport, nothing filled. -->""",
    "shoulders": """<!-- MUSCLE GLYPH - SHOULDERS. ONE SHOULDER, built 2026-10-12 by CODE rather than
     by hand - owner: *"i dont want you to draw them i want you to make mathematical
     lines to make it look realistic with the same design of the arm"* and *"draw it but
     through coding."* REMADE FROM ZERO for legibility: the TOP OF AN ARM - a true
     half-circle delt cap (r=5.4, 180 degrees exactly) on a stump solved tangent to it
     and cut straight across, the trap line off at 315 degrees, and two small muscle
     lines inside like the arm's: the head arc is an exact concentric offset (r-2.6) of
     the dome, the crease a circumcircle arc near the cut. Generated by
     evidence/muscle-icons-2026-10-11/math_glyphs.py.

     Style: the reference arm (ic_muscle_arms) stays the style authority - stroke 1.6,
     round caps and joins, 24dp on the 24x24 viewport, nothing filled. -->""",
    "legs": """<!-- MUSCLE GLYPH - LEGS. ONE LEG, built 2026-10-12 by CODE rather than by hand -
     owner: *"i dont want you to draw them i want you to make mathematical lines to make
     it look realistic with the same design of the arm"* and *"draw it but through
     coding."* REMADE FROM ZERO for legibility: the bulges are pushed until the S-curve
     is unmistakable - quad peaking hard off the front, calf off the back, a real notch
     at the knee. Front and back edges are Catmull-Rom runs (C1), the instep, toe and
     heel are circumcircle arcs, the sole is a straight line, and three small muscle
     lines do the defining (quad sweep, kneecap, calf curve). Generated by
     evidence/muscle-icons-2026-10-11/math_glyphs.py.

     Style: the reference arm (ic_muscle_arms) stays the style authority - stroke 1.6,
     round caps and joins, 24dp on the 24x24 viewport, nothing filled. -->""",
    "back": """<!-- MUSCLE GLYPH - BACK. ONE LATISSIMUS, built 2026-10-12 by CODE rather than by
     hand - owner: *"i dont want you to draw them i want you to make mathematical lines
     to make it look realistic with the same design of the arm"* and *"draw it but
     through coding."* REMADE FROM ZERO for legibility: a HAND FAN - the spine-side
     edge is exactly straight (three collinear ring points), the outer edge one big
     circular bulge, and THREE fibers radiate toward it like fan ribs. Closed
     Catmull-Rom ring + three circumcircle fiber arcs. Generated by
     evidence/muscle-icons-2026-10-11/math_glyphs.py.

     Style: the reference arm (ic_muscle_arms) stays the style authority - stroke 1.6,
     round caps and joins, 24dp on the 24x24 viewport, nothing filled. -->""",
    "core": """<!-- MUSCLE GLYPH - CORE. THE RECTUS ABDOMINIS, built 2026-10-12 by CODE rather
     than by hand - owner: *"i dont want you to draw them i want you to make
     mathematical lines to make it look realistic with the same design of the arm"* and
     *"draw it but through coding."* Built from zero the same way: the rib arch is one
     circumcircle arc whose apex is exactly x=12; the flank points of the right half are
     MIRRORED (x -> 24-x) to form the left half, so the muscle is symmetric to machine
     precision; the linea alba is the line x=12 and the three tendinous intersections
     are arcs with apices on it. Generated by evidence/muscle-icons-2026-10-11/math_glyphs.py.

     Style: the reference arm (ic_muscle_arms) stays the style authority - stroke 1.6,
     round caps and joins, 24dp on the 24x24 viewport, nothing filled. -->""",
}


def xml_for(name: str) -> str:
    strokes = GLYPHS[name]()
    body = []
    for label, d in strokes:
        body.append(
            "    <!-- {label} -->\n"
            "    <path\n"
            '        android:strokeColor="#FF000000"\n'
            '        android:strokeWidth="1.6"\n'
            '        android:strokeLineCap="round"\n'
            '        android:strokeLineJoin="round"\n'
            '        android:pathData="{d}" />'.format(label=label, d=d)
        )
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"{HEADERS[name]}\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp"\n'
        '    android:height="24dp"\n'
        '    android:viewportWidth="24"\n'
        '    android:viewportHeight="24">\n' + "\n".join(body) + "\n</vector>\n"
    )


def main() -> int:
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return 2
    if args[0] == "--show":
        which = args[1:] or ["all"]
        if which == ["all"]:
            which = list(GLYPHS)
        for name in which:
            show(name)
            print()
        return 0
    if args[0] == "--write":
        for name in GLYPHS:
            out = DRAWABLES / f"ic_muscle_{name}.xml"
            out.write_text(xml_for(name))
            print(f"wrote {out}")
        return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
