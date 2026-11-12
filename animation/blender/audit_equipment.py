"""Physics / real-world plausibility audit of every equipment asset.

  cd animation && BLENDER_USER_RESOURCES=$PWD/blender-user blender -b --python blender/audit_equipment.py
  (ONLY=id1,id2 to audit a subset)
-> prints a table, writes output/equipment/physics-audit.md

Checks per asset (mesh is in world space, origin = asset origin):
  ground   floor items rest on z=0 (min z within 1 mm); hand-held items have their origin at the
           grip/centre; ceiling-mounted items are exempt (their top beam is the mount)
  connect  every loose part touches/overlaps another part (gap <= 2 mm) or the floor, and the
           whole asset is one connected structure (anchored to the floor for floor items)
  stable   centre of mass (by volume) lies inside the convex footprint of the floor contacts
  dims     key dimensions within 10% of standard gym specs (REF table below)
  drape    flexible parts (vertex attribute role==1: ropes, cables, straps, bands) are either taut
           and straight, hang vertically from an anchor, or lie flat on the floor
  stack    weight-stack plates (role==2) sit flush on each other, a selector pin (role==3) is
           in the stack and a lift cable rises straight from the top plate to the pulley
"""
import math
import os
import sys
from collections import defaultdict

import bpy
import numpy as np
from mathutils import Vector
from mathutils.bvhtree import BVHTree

A = "/home/matrix/StudioProjects/RepIo/animation"
sys.path.insert(0, f"{A}/blender")
import equipment  # noqa: E402

TOL = 0.002          # max joint gap
FLOOR_TOL = 0.001

HAND = {"dumbbell", "barbell", "ez_bar", "hex_bar", "weight_plate", "kettlebell", "medicine_ball",
        "stability_ball", "ab_wheel", "wrist_roller", "dip_belt", "jump_rope", "battle_ropes",
        "att_straight_bar", "att_v_bar", "att_rope", "att_d_handle", "att_lat_bar", "att_ankle_strap",
        "resistance_band", "mini_band", "yoga_mat"}
MOUNTED = {"gymnastic_rings", "suspension_trainer", "climbing_rope"}
# where the origin of a hand-held item must sit: on the handle/bar ('grip') or at the centre ('centre')
ORIGIN = {"dumbbell": "grip", "barbell": "grip", "ez_bar": "grip", "hex_bar": "centre", "weight_plate": "centre",
          "kettlebell": "grip", "medicine_ball": "centre", "stability_ball": "centre", "ab_wheel": "grip",
          "wrist_roller": "grip", "dip_belt": "centre", "jump_rope": "centre", "battle_ropes": "centre",
          "att_straight_bar": "grip", "att_v_bar": "grip", "att_rope": "grip", "att_d_handle": "grip",
          "att_lat_bar": "grip", "att_ankle_strap": "centre", "resistance_band": "centre",
          "mini_band": "centre", "yoga_mat": "centre"}


# ---- reference dimensions (standard commercial gym specs) -------------------------------------
# each entry: (what, measure(ctx) -> metres or degrees, expected value); pass if within +-10 %
def bbox(ax):
    return lambda c: c["size"][ax]


def top(mat):
    return lambda c: max((p["max"][2] for p in c["parts"] if p["mat"] == mat), default=0.0)


def part_dim(sel, ax, agg=max):
    return lambda c: agg([p["size"][ax] for p in c["parts"] if sel(p)] or [0.0])


def plate_diam(c):
    return max((max(p["size"][1], p["size"][2]) for p in c["parts"] if p["mat"] == "EQ_plate"
                and min(p["size"]) < 0.08), default=0.0)


def steel_min_diam(c):
    """Thinnest long steel part across its section (bar shaft)."""
    ds = [sorted(p["size"])[0] for p in c["parts"] if p["mat"] == "EQ_steel" and max(p["size"]) > 0.5]
    return min(ds, default=0.0)


def sleeve_diam(c):
    ds = [sorted(p["size"])[1] for p in c["parts"] if p["mat"] == "EQ_steel" and 0.3 < max(p["size"]) < 0.5]
    return max(ds, default=0.0)


def max_beam_angle(c, mat="EQ_frame", lo=20, hi=70):
    """Angle from horizontal of inclined frame members (leg press / hack squat rails)."""
    out = []
    for p in c["parts"]:
        if p["mat"] != mat or p["len"] < 0.8:
            continue
        a = math.degrees(math.asin(min(1.0, abs(p["axis"][2]))))
        if lo < a < hi:
            out.append(a)
    return max(out, default=0.0)


REF = {
    "dumbbell": [("length", bbox(0), 0.236)],
    "barbell": [("bar length", bbox(0), 2.20), ("shaft diameter", steel_min_diam, 0.0285),
                ("sleeve diameter", sleeve_diam, 0.050), ("20 kg plate diameter", plate_diam, 0.450)],
    "ez_bar": [("length", bbox(0), 1.24)],
    "hex_bar": [("length", bbox(0), 1.50), ("plate diameter", plate_diam, 0.450)],
    "weight_plate": [("diameter", lambda c: max(c["size"][0], c["size"][2]), 0.41)],
    "kettlebell": [("height (16 kg)", bbox(2), 0.28), ("width", bbox(0), 0.21)],
    "medicine_ball": [("diameter", bbox(2), 0.34)],
    "stability_ball": [("diameter", bbox(2), 0.65)],
    "ab_wheel": [("wheel diameter", bbox(2), 0.20)],
    "jump_rope": [("handle length", lambda c: max((p["len"] for p in c["parts"] if p["mat"] == "EQ_rubber" and p["len"] < 0.3), default=0), 0.16)],
    "flat_bench": [("pad height", top("EQ_pad"), 0.44), ("pad length", part_dim(lambda p: p["mat"] == "EQ_pad", 1), 1.18),
                   ("pad width", part_dim(lambda p: p["mat"] == "EQ_pad", 0), 0.30)],
    "adjustable_bench": [("seat height", lambda c: min((p["max"][2] for p in c["parts"] if p["mat"] == "EQ_pad"), default=0), 0.45)],
    "preacher_bench": [("seat height", lambda c: min((p["max"][2] for p in c["parts"] if p["mat"] == "EQ_pad"), default=0), 0.62)],
    "power_rack": [("height", bbox(2), 2.25),
                   ("frame width", lambda c: max(p["max"][0] for p in c["parts"] if p["mat"] == "EQ_frame")
                    - min(p["min"][0] for p in c["parts"] if p["mat"] == "EQ_frame"), 1.26)],
    "smith_machine": [("height", bbox(2), 2.20)],
    "plyo_box": [("height", bbox(2), 0.53), ("width", bbox(0), 0.75)],
    "pull_up_bar": [("bar height", bbox(2), 2.30)],
    "dip_bars": [("bar height", bbox(2), 1.20), ("bar spacing", lambda c: c["size"][0] - 0.06, 0.52)],
    "treadmill": [("length", bbox(1), 2.13), ("deck width", bbox(0), 0.85)],
    "rowing_machine": [("length", bbox(1), 2.44)],
    "leg_press_45": [("sled angle (deg)", max_beam_angle, 45.0)],
    "hack_squat": [("sled angle (deg)", max_beam_angle, 45.0)],
    "yoga_mat": [("length", bbox(1), 1.83), ("width", bbox(0), 0.61)],
    "lat_pulldown": [("height", bbox(2), 2.30)],
    "cable_station": [("height", bbox(2), 2.10)],
    "stationary_bike": [("saddle height", top("EQ_pad"), 0.95)],
    "gymnastic_rings": [("ring diameter", part_dim(lambda p: p["mat"] == "EQ_frame" and p["len"] < 0.4, 2), 0.25)],
    "climbing_rope": [("rope length", part_dim(lambda p: p["role"] == 1, 2), 4.0)],
    "human_flag_pole": [("height", bbox(2), 3.2)],
    "wall": [("height", bbox(2), 2.5)],
    "landmine": [("bar length (shaft+sleeve)", lambda c: sum(p["len"] for p in c["parts"] if p["mat"] == "EQ_steel"
                                                             and p["len"] > 0.3) - 0.02, 2.20)],
}


# ---- mesh -> loose parts -----------------------------------------------------------------------
def loose_parts(o):
    me = o.data
    n = len(me.vertices)
    co = np.empty(n * 3, dtype=np.float64)
    me.vertices.foreach_get("co", co)
    co = co.reshape(-1, 3)
    parent = np.arange(n)

    def find(a):
        root = a
        while parent[root] != root:
            root = parent[root]
        while parent[a] != root:
            parent[a], a = root, parent[a]
        return root

    ev = np.empty(len(me.edges) * 2, dtype=np.int64)
    me.edges.foreach_get("vertices", ev)
    for a, b in ev.reshape(-1, 2):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb
    roots = np.array([find(i) for i in range(n)])
    role = np.zeros(n, dtype=np.int64)
    if "role" in me.attributes:
        me.attributes["role"].data.foreach_get("value", role)
    mats = [m.name if m else "" for m in me.materials]
    polys_by = defaultdict(list)
    pmat = defaultdict(lambda: defaultdict(float))
    for p in me.polygons:
        r = roots[p.vertices[0]]
        polys_by[r].append(list(p.vertices))
        pmat[r][mats[p.material_index] if mats else ""] += p.area
    parts = []
    for r, polys in polys_by.items():
        idx = np.nonzero(roots == r)[0]
        pts = co[idx]
        remap = {int(g): i for i, g in enumerate(idx)}
        tree = BVHTree.FromPolygons([tuple(v) for v in pts], [[remap[v] for v in poly] for poly in polys])
        mn, mx = pts.min(0), pts.max(0)
        c0 = pts.mean(0)
        u, s, vt = np.linalg.svd(pts - c0, full_matrices=False)
        axis = vt[0]
        proj = (pts - c0) @ axis
        resid = np.linalg.norm((pts - c0) - np.outer(proj, axis), axis=1).max()
        vol, cen = 0.0, np.zeros(3)
        for poly in polys:                     # signed tetra volume (closed meshes)
            a = co[poly[0]]
            for k in range(1, len(poly) - 1):
                b, cc = co[poly[k]], co[poly[k + 1]]
                v6 = np.dot(a, np.cross(b, cc)) / 6.0
                vol += v6
                cen += v6 * (a + b + cc) / 4.0
        cen = cen / vol if abs(vol) > 1e-12 else c0
        parts.append(dict(idx=idx, pts=pts, tree=tree, min=mn, max=mx, size=mx - mn,
                          mat=max(pmat[r], key=pmat[r].get), role=int(role[idx].max()),
                          axis=axis, len=float(proj.max() - proj.min()), resid=float(resid),
                          vol=abs(vol), com=cen))
    return parts


def touching(a, b):
    if np.any(a["min"] - TOL > b["max"]) or np.any(b["min"] - TOL > a["max"]):
        return False, 1.0
    if a["tree"].overlap(b["tree"]):
        return True, 0.0
    best = 1.0
    for p, q in ((a, b), (b, a)):
        pts = p["pts"]
        step = max(1, len(pts) // 600)
        for v in pts[::step]:
            loc, nrm, _, d = q["tree"].find_nearest(Vector(v))
            if loc is None:
                continue
            if d <= TOL or (Vector(v) - loc).dot(nrm) < 0:      # within tolerance or inside the other part
                return True, d
            best = min(best, d)
    return False, best


def hull2d(pts):
    pts = sorted(set(map(tuple, np.round(pts, 5))))
    if len(pts) < 3:
        return pts
    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lower, upper = [], []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)
    return lower[:-1] + upper[:-1]


def inside_hull(h, p):
    if len(h) < 3:
        return False
    sign = None
    for i in range(len(h)):
        a, b = h[i], h[(i + 1) % len(h)]
        c = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
        if abs(c) < 1e-9:
            continue
        s = c > 0
        if sign is None:
            sign = s
        elif s != sign:
            return False
    return True


def audit(aid):
    o = bpy.data.objects[aid]
    parts = loose_parts(o)
    allpts = np.vstack([p["pts"] for p in parts])
    mn, mx = allpts.min(0), allpts.max(0)
    ctx = dict(parts=parts, size=mx - mn, min=mn, max=mx)
    res = {}
    cls = "hand" if aid in HAND else "mounted" if aid in MOUNTED else "floor"

    # ground
    if cls == "floor":
        ok = abs(mn[2]) <= FLOOR_TOL
        res["ground"] = (ok, f"min z {mn[2] * 1000:+.1f} mm")
    elif cls == "hand":
        mode = ORIGIN.get(aid, "centre")
        if mode == "grip":
            d = min(p["tree"].find_nearest(Vector((0, 0, 0)))[3] for p in parts)
            # origin inside a handle/bar: within that part's box and within ~a grip radius of its surface
            inside = any(np.all(p["min"] - 0.002 <= 0) and np.all(p["max"] + 0.002 >= 0)
                         and p["tree"].find_nearest(Vector((0, 0, 0)))[3] < 0.03 for p in parts)
            ok = inside or d < 0.005
            res["ground"] = (ok, f"origin {'on' if ok else 'off'} grip ({d * 1000:.0f} mm)")
        else:
            c = (mn + mx) / 2
            off = np.linalg.norm(c)
            tol = 0.02 if aid == "att_ankle_strap" else 0.01      # D-ring offsets the cuff's box
            ok = off < tol or (aid in ("jump_rope", "battle_ropes", "resistance_band", "mini_band", "yoga_mat")
                                and abs(mn[2]) <= FLOOR_TOL and np.linalg.norm(c[:2]) < 0.01)
            res["ground"] = (ok, f"origin {off * 1000:.0f} mm from centre")
    else:
        res["ground"] = (True, "ceiling-mounted (exempt)")

    # connect
    n = len(parts)
    adj = defaultdict(set)
    FLOOR = -1
    for i in range(n):
        if cls != "hand" and parts[i]["min"][2] <= FLOOR_TOL:
            adj[i].add(FLOOR)
            adj[FLOOR].add(i)
        for j in range(i + 1, n):
            t, _ = touching(parts[i], parts[j])
            if t:
                adj[i].add(j)
                adj[j].add(i)
    start = FLOOR if cls == "floor" else max(range(n), key=lambda k: parts[k]["vol"])
    seen, stack = {start}, [start]
    while stack:
        k = stack.pop()
        for m in adj[k]:
            if m not in seen:
                seen.add(m)
                stack.append(m)
    loose = [i for i in range(n) if i not in seen]
    if loose:
        desc = []
        for i in loose[:4]:
            gap = min((touching(parts[i], parts[j])[1] for j in seen if j != FLOOR), default=1.0)
            c = (parts[i]["min"] + parts[i]["max"]) / 2
            desc.append(f"{parts[i]['mat'][3:]}@({c[0]:.2f},{c[1]:.2f},{c[2]:.2f}) gap {gap * 1000:.0f}mm")
        res["connect"] = (False, f"{len(loose)}/{n} parts float: " + "; ".join(desc))
    else:
        res["connect"] = (True, f"{n} parts, all joined")

    # stable
    if cls == "floor":
        foot = allpts[allpts[:, 2] <= FLOOR_TOL + 0.001][:, :2]
        h = hull2d(foot)
        vol = sum(p["vol"] for p in parts)
        com = sum(p["vol"] * p["com"] for p in parts) / vol
        ok = inside_hull(h, com[:2])
        res["stable"] = (ok, f"COM ({com[0]:.2f},{com[1]:.2f}) {'inside' if ok else 'OUTSIDE'} footprint")
    else:
        res["stable"] = (True, "n/a")

    # dims
    bad = []
    for what, fn, exp in REF.get(aid, []):
        v = fn(ctx)
        if abs(v - exp) > 0.10 * exp:
            bad.append(f"{what} {v:.3f} vs {exp:.3f}")
    res["dims"] = (not bad, "; ".join(bad) if bad else (f"{len(REF.get(aid, []))} checked" if aid in REF else "no ref"))

    # drape
    bad = []
    nflex = sum(1 for p in parts if p["role"] == 1)
    for p in parts:
        if p["role"] != 1:
            continue
        thick = sorted(p["size"])[0]
        width = sorted(p["size"])[1]                       # a flat strap's width sets its half-diagonal
        straight = p["resid"] <= max(0.012, width * 0.75) + 0.002
        lying = p["min"][2] <= FLOOR_TOL + 0.0005 and p["size"][2] <= max(0.05, thick * 1.2) + 0.003
        vertical = abs(p["axis"][2]) > math.cos(math.radians(3))
        on_floor = p["min"][2] <= FLOOR_TOL
        # a straight part with nothing below it must hang plumb unless it runs between two supports
        supports = sum(1 for q in parts if q is not p and touching(p, q)[0])
        if lying:
            continue
        if straight and (vertical or supports >= 2 or on_floor):
            continue
        c = (p["min"] + p["max"]) / 2
        why = "sags/bends unsupported" if not straight else "not plumb under a single anchor"
        bad.append(f"{p['mat'][3:]}@({c[0]:.2f},{c[1]:.2f},{c[2]:.2f}) {why}")
    res["drape"] = (not bad, "; ".join(bad) if bad else (f"{nflex} flexible part(s) hang/run/lie correctly" if nflex else "n/a"))

    # stack
    plates = sorted([p for p in parts if p["role"] == 2], key=lambda p: p["min"][2])
    if plates:
        bad = []
        cols = defaultdict(list)
        for p in plates:                                   # group into columns by xy distance, not rounding
            cxy = ((p["min"][0] + p["max"][0]) / 2, (p["min"][1] + p["max"][1]) / 2)
            key = next((k for k in cols if math.hypot(k[0] - cxy[0], k[1] - cxy[1]) < 0.05), None)
            cols[key or (round(float(cxy[0]), 2), round(float(cxy[1]), 2))].append(p)
        for key, ps in cols.items():
            gaps = [ps[k + 1]["min"][2] - ps[k]["max"][2] for k in range(len(ps) - 1)]
            if any(abs(gp) > 0.0006 for gp in gaps):
                bad.append(f"stack {key} gaps up to {max(map(abs, gaps)) * 1000:.1f} mm")
            pins = [q for q in parts if q["role"] == 3 and touching(q, ps[0])[0] or q["role"] == 3 and any(touching(q, s)[0] for s in ps)]
            if not pins:
                bad.append(f"stack {key} no selector pin")
            topp = ps[-1]
            lift = [q for q in parts if q["role"] == 1 and touching(q, topp)[0] and abs(q["axis"][2]) > 0.995]
            if not lift:
                bad.append(f"stack {key} no straight lift cable from top plate")
        res["stack"] = (not bad, "; ".join(bad) if bad else f"{len(plates)} plates flush, pin + cable ok")
    else:
        res["stack"] = (True, "n/a")
    return res


def main():
    only = set(filter(None, os.environ.get("ONLY", "").split(",")))
    rows = []
    for aid, label, _b, _k in equipment.LIBRARY:
        if only and aid not in only:
            continue
        bpy.ops.wm.open_mainfile(filepath=f"{A}/assets/equipment/{aid}.blend")
        r = audit(aid)
        rows.append((aid, r))
        flags = " ".join(("ok  " if r[k][0] else "FAIL") for k in ("ground", "connect", "stable", "dims", "drape", "stack"))
        print(f"AUDIT {aid:26s} {flags}  " + " | ".join(f"{k}: {v[1]}" for k, v in r.items() if not v[0]), flush=True)
    keys = ("ground", "connect", "stable", "dims", "drape", "stack")
    fails = sum(1 for _, r in rows if not all(r[k][0] for k in keys))
    lines = ["# Equipment physics audit", "",
             "Checks: **ground** (floor items on z=0, hand-held origin at grip/centre), **connect** (no floating "
             "parts, joints <= 2 mm), **stable** (centre of mass inside the floor footprint), **dims** (within 10 % "
             "of standard gym specs), **drape** (ropes/cables/straps/bands hang plumb, run taut and straight, or "
             "lie on the floor), **stack** (weight-stack plates flush, selector pin, straight lift cable).", "",
             f"Result: {len(rows) - fails}/{len(rows)} assets pass every check.", "",
             "| Asset | ground | connect | stable | dims | drape | stack | notes |", "|---|---|---|---|---|---|---|---|"]
    for aid, r in rows:
        notes = "; ".join(f"{k}: {r[k][1]}" for k in keys if not r[k][0]) or "; ".join(
            f"{k}: {r[k][1]}" for k in ("dims", "drape", "stack") if r[k][1] not in ("n/a", "no ref"))
        lines.append(f"| `{aid}` | " + " | ".join("✅" if r[k][0] else "❌" for k in keys) + f" | {notes} |")
    lines += ["", "## Reference dimensions used", "", "| Asset | Dimension | Spec |", "|---|---|---:|"]
    for aid, refs in REF.items():
        for what, _fn, exp in refs:
            lines.append(f"| `{aid}` | {what} | {exp:g}{'°' if 'deg' in what else ' m'} |")
    if not only:
        open(f"{A}/output/equipment/physics-audit.md", "w").write("\n".join(lines) + "\n")
    print(f"AUDIT RESULT {len(rows) - fails}/{len(rows)} clean")


main()
