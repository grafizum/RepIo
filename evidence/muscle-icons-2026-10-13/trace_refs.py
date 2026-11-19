#!/usr/bin/env python3
"""The six muscle glyphs, as the owner's drawings: their INK, vectorised.

Owner, 2026-10-13, looking at the glyphs that were in the app: *"your ... images drawn are not
at all the same as the images that you are showing, remake the ... things based on the
images"*. Three rounds of hand-composed anatomy had already been rejected, so the shapes now
come from the files themselves.

TWO WAYS TO TRACE, AND WHY THIS IS THE SECOND ONE.

The first attempt walked the CENTRE LINE of each ink stroke (block-threshold the plate, thin
it to a skeleton, walk the skeleton into runs, smooth, stroke them). It is the obvious way and
it loses on both ends:

  * FIDELITY. A skeleton of a hand-drawn band is a staircase, and a staircase curves back out
    as wobble. Cleaned up enough to stop looking fuzzy, it also stops looking like the drawing:
    the plates came back as 14-41 strokes of 3-point wiggles, the chest and the back broke into
    loose fragments when the small ones were dropped to lighten them, and the abdomen plate
    came out as a bag with a stem.
  * WEIGHT. The drawing's own line measures 0.70-1.31 units in this 24-unit box (see
    measure_refs.py) while the filter lane's weight is 1.6, so a centre-line trace has to be
    stroked at twice the ink it was drawn with - and at 22dp, where the chip renders it, that
    1.6 in a 19-unit figure closes the 2-unit gaps of the abdomen grid into a solid mass.

So this traces the ink as OUTLINES instead: binarise the plate, thicken the ink to the weight
the filter lane wants, and hand back each connected region's contour as a filled closed path.
The geometry is then the drawing's own - every curve, corner and round end is where the artist
put it, with no skeleton reconstruction in between - and the weight is a single measurable
number (INK_TARGET) rather than an artefact of a stroke attribute.

WHAT IT KEEPS FROM THE HOUSE RULES (MuscleGlyphFamilyTest measures these):
  * 24dp on the 24x24 viewport, the same box as ic_eq_kettlebell;
  * ink inset from the edges, painting at least a quarter of the box;
  * ink WEIGHT inside the equipment set's own lane - and the lane is measured off the kit
    drawables there, not written down here, because the honest question is "is this the same
    hand as the kit icons", and that answer lives in those files;
  * six distinct parts, each resolving to its own glyph in the filter.

Usage:
    python3 trace_refs.py --stats             # what each trace produces
    python3 trace_refs.py --write             # emit the six drawables (backs up the old ones)
    python3 trace_refs.py --ascii core        # ASCII render of one traced glyph
    python3 trace_refs.py --weight            # measure the emitted ink, in units
"""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from measure_refs import decode_png  # noqa: E402  (same directory, shared decoder)

REPO = HERE.parents[1]
DRAWABLES = REPO / "composeApp/src/main/res/drawable"
BACKUP = HERE / "previous"
REF_DIR = Path("/home/matrix/Downloads/Muscles")

# group -> reference drawing, decided with the owner (2026-10-13): the flexed-arm plate is
# the arms, the abdomen plate is the core, the leg is the legs, the shoulder plate is the
# shoulders, and the two torso frames split chest (pec slab) / back (lat yoke).
SOURCES = {
    "arms": "muskel.png",
    "core": "abs.png",
    "legs": "bein.png",
    "shoulders": "schultern.png",
    "chest": "korperteil.png",
    "back": "body-part.png",
}

VIEWPORT = 24.0
MARGIN = 2.7           # units of air the ink keeps from the box edge

# THE SHOULDERS PLATE IS A FRAMED DIAGRAM, and the frame is not anatomy. schultern.png draws the
# delt sweep and a ruled box around it - the box's edges are dead straight and run most of the
# plate, and traced literally they turn the glyph into a filled block, which is what the first
# fill attempt on this plate produced. So for this one plate the box is removed first: a run of
# ink that is straight for FRAME_RUN of the plate and is barely wider than the drawing's own line
# is a ruled edge, not a body part. It is applied per plate, not everywhere: the abdomen plate's
# shoulder line is a long straight bar too, and there it IS the drawing.
FRAME_PLATES = {"shoulders"}
FRAME_RUN = 0.3        # of the plate's width (horizontal edges) / height (vertical edges)
FRAME_THIN = 2.2       # x the measured ink weight: wider than this is not a ruled line
DARK = 160             # luminance below this is ink (the plates are black strokes)
ALPHA = 64             # ... on a pixel at least this opaque
INK_TARGET = 1.25      # units of ink weight the trace aims for (the lane's light end is 1.2)
TOLERANCE = 1.1        # Douglas-Peucker on a contour, in source pixels
SMOOTH = 2             # cyclic moving-average passes before it is simplified
MIN_AREA = 60          # px^2; below this a contour is a speck from the antialiasing
EROSIONS = 60          # cap on the erode-to-empty measurement of ink weight

Vec = tuple[float, float]


# ------------------------------------------------------------------ ink

def ink_mask(path: Path, dark: int = DARK, alpha: int = ALPHA) -> np.ndarray:
    """The drawing's ink as a boolean mask at full plate resolution."""
    w, h, px = decode_png(path)
    flat = np.array(px, dtype=np.int16)
    lum = flat[:, :3].mean(axis=1)
    return ((flat[:, 3] > alpha) & (lum < dark)).reshape(h, w)


def thickness(mask: np.ndarray) -> float:
    """How thick the ink actually is, in pixels, without caring about its direction: erode
    the mask until nothing is left - a cross-shaped erosion takes a layer off every side - and
    half the number of layers is the thickness. A row-run measure gives the length of a
    horizontal stroke instead of its width; this gives the same answer either way."""
    current = mask
    layers = 0
    while current.any() and layers < EROSIONS:
        padded = np.pad(current, 1)
        current = (current & padded[:-2, 1:-1] & padded[2:, 1:-1]
                   & padded[1:-1, :-2] & padded[1:-1, 2:])
        layers += 1
    return 2.0 * layers


def _long_runs(line: np.ndarray, minimum: float) -> list[tuple[int, int]]:
    """The line's ink runs at least `minimum` long, as (start, end) pairs."""
    out: list[tuple[int, int]] = []
    start = None
    for j in range(len(line) + 1):
        filled = j < len(line) and bool(line[j])
        if filled and start is None:
            start = j
        elif not filled and start is not None:
            if j - start >= minimum:
                out.append((start, j))
            start = None
    return out


def strip_frame(mask: np.ndarray, weight: float) -> np.ndarray:
    """Drop the ruled edges of a framed plate (see FRAME_PLATES).

    A ruled edge is found by its sameness as much as by its length: scan the plate line by
    line across the edge's direction, and where the SAME long run persists for a few
    consecutive lines while staying no thicker than a drawn line, that is a ruled edge and
    not a drawing. Sameness is the part that matters - the first attempt at this removed
    every long thin run, which also removed the delt sweep's own flat middle and the steep
    diagonal in the box, and left nothing at all behind."""
    out = mask.copy()
    thin = max(4.0, weight * FRAME_THIN)
    for axis in (0, 1):
        moved = mask if axis == 0 else mask.T
        target = out if axis == 0 else out.T
        n, length = moved.shape
        minimum = FRAME_RUN * length
        stack: list[int] = []
        previous: list[tuple[int, int]] = []
        for i in range(n + 1):
            current = _long_runs(moved[i], minimum) if i < n else []
            same = (bool(current) and len(current) == len(previous) and all(
                abs(s - ps) <= 2 and abs(e - pe) <= 2
                for (s, e), (ps, pe) in zip(current, previous)))
            if same:
                stack.append(i)
            else:
                if 3 <= len(stack) <= thin:
                    for k in stack:
                        for s, e in _long_runs(moved[k], minimum):
                            target[k, s:e] = False
                stack = [i] if current else []
            previous = current
    return out


def thicken(mask: np.ndarray, radius: int) -> np.ndarray:
    """Binary dilation by a disk: the ink the artist drew, at the weight the filter lane
    wants. The plates' line is about 20px of 512 (0.74 units once fitted), so a few pixels
    of radius is the whole difference between a hairline and the kit's own weight - and
    because the structuring element is a disk, every end and corner it grows stays round."""
    if radius <= 0:
        return mask
    out = mask.copy()
    offsets = [(dx, dy) for dx in range(-radius, radius + 1)
               for dy in range(-radius, radius + 1) if dx * dx + dy * dy <= radius * radius]
    padded = np.zeros((mask.shape[0] + 2 * radius, mask.shape[1] + 2 * radius), dtype=bool)
    padded[radius:-radius, radius:-radius] = mask
    for dx, dy in offsets:
        out |= padded[radius + dy:radius + dy + mask.shape[0],
                      radius + dx:radius + dx + mask.shape[1]]
    return out


# ------------------------------------------------------------------ contours (marching squares)

# ------------------------------------------------------------------ contours (crack following)

DIRS = ((1, 0), (0, 1), (-1, 0), (0, -1))


def _sides(corner: tuple[int, int], d: tuple[int, int]) -> tuple[tuple[int, int], tuple[int, int]]:
    """The two pixels a crack separates, as (right of travel, left of travel). Travel with
    the ink on the right and a boundary that hugs it comes out wound consistently, holes
    opposed to outlines - which is what the fill needs."""
    x, y = corner
    dx, dy = d
    if d == (1, 0):
        return (x, y), (x, y - 1)
    if d == (0, 1):
        return (x - 1, y), (x, y)
    if d == (-1, 0):
        return (x - 1, y - 1), (x - 1, y)
    return (x, y - 1), (x - 1, y - 1)


def contours(mask: np.ndarray) -> list[list[Vec]]:
    """Every closed boundary of the ink - the outside of a stroke and the inside of a ring
    alike - as corner-to-corner loops, the ink held on the right.

    This is the third way the boundaries were found and the first one that is right. Two
    earlier passes collected boundary segments and joined them by shared endpoints, and both
    drew a long diagonal chord across the figure: at a crossing - and every plate has them,
    that is what a drawing is - four segment ends meet in one place, and greedy joining picks
    a partner that is not the continuation, so the loop walks the outside of a stroke and
    comes back along the inside. On the board that read as a solid wedge through the arm and
    the abdomen. A walk instead of a join has no such choice to get wrong: at every corner
    the next crack is the sharpest RIGHT turn that is still boundary, else straight on, else
    left, and following that rule can only ever thread the ink's actual edge."""
    h, w = mask.shape

    def ink(x: int, y: int) -> bool:
        return 0 <= x < w and 0 <= y < h and bool(mask[y, x])

    def boundary(corner: tuple[int, int], d: tuple[int, int]) -> bool:
        right, left = _sides(corner, d)
        return ink(*right) and not ink(*left)

    starts: list[tuple[int, int, tuple[int, int]]] = []
    for y, x in zip(*np.nonzero(mask)):
        for d in DIRS:
            # The crack that has this ink pixel on its right, whatever the direction.
            x0, y0 = x, y
            if d == (0, 1):
                x0 = x + 1
            elif d == (-1, 0):
                x0, y0 = x + 1, y + 1
            elif d == (0, -1):
                y0 = y + 1
            if boundary((x0, y0), d):
                starts.append((x0, y0, d))

    visited: set[tuple[int, int, tuple[int, int]]] = set()
    loops: list[list[Vec]] = []
    for start in starts:
        if start in visited:
            continue
        corner, d = start[:2], start[2]
        loop: list[Vec] = []
        while (corner[0], corner[1], d) not in visited:
            visited.add((corner[0], corner[1], d))
            loop.append((float(corner[0]), float(corner[1])))
            nxt = (corner[0] + d[0], corner[1] + d[1])
            for turn in ((-d[1], d[0]), d, (d[1], -d[0])):
                if boundary(nxt, turn):
                    corner, d = nxt, turn
                    break
            else:
                break
        if len(loop) >= 4:
            loops.append(loop)
    return loops


def _area(loop: list[Vec]) -> float:
    return abs(sum(x1 * y2 - x2 * y1 for (x1, y1), (x2, y2) in zip(loop, loop[1:]))) / 2.0


# ------------------------------------------------------------------ simplification

def smooth_cyclic(loop: list[Vec], passes: int = SMOOTH) -> list[Vec]:
    """Moving average around a closed loop. A marching-squares contour steps by half a pixel
    at a time; this is what takes that step off before the simplification sees it."""
    pts = list(loop)
    for _ in range(passes):
        if len(pts) < 5:
            break
        pts = [((pts[i - 1][0] + 2 * pts[i][0] + pts[(i + 1) % len(pts)][0]) / 4.0,
                (pts[i - 1][1] + 2 * pts[i][1] + pts[(i + 1) % len(pts)][1]) / 4.0)
               for i in range(len(pts))]
    return pts


def simplify(loop: list[Vec], tol: float = TOLERANCE) -> list[Vec]:
    """Douglas-Peucker around a closed loop: cut it in half by count, so each half is an open
    curve from loop[0] to loop[half] and from loop[half] back round to loop[0], and simplify
    the two independently. Douglas-Peucker needs two distinct endpoints to measure against, so
    a closed loop has to be cut before it can be simplified at all - and cutting it at its
    midpoint is what keeps the result in the loop's own order.

    The first version of this rotated the loop to start at a "far" point first, and that is
    where the arm's outline turned into a filled wedge: with the rotation in place the two
    halves no longer met, the emitted polyline ran one and a half times round the figure, and
    the overlap filled in as a solid triangle. Cut at the midpoint, nothing can overlap."""
    n = len(loop)
    if n < 6:
        return loop
    half = n // 2
    first = _rdp(loop[:half + 1], tol)
    second = _rdp(loop[half:] + loop[:1], tol)
    return first[:-1] + second[:-1]


def _rdp(points: list[Vec], tol: float) -> list[Vec]:
    if len(points) < 3:
        return points
    (x1, y1), (x2, y2) = points[0], points[-1]
    dx, dy = x2 - x1, y2 - y1
    norm = math.hypot(dx, dy)
    worst, index = -1.0, 0
    for i in range(1, len(points) - 1):
        x, y = points[i]
        d = abs(dy * (x - x1) - dx * (y - y1)) / norm if norm else math.hypot(x - x1, y - y1)
        if d > worst:
            worst, index = d, i
    if worst <= tol:
        return [points[0], points[-1]]
    return _rdp(points[:index + 1], tol)[:-1] + _rdp(points[index:], tol)


def closed_path(loop: list[Vec]) -> str:
    """The loop as a closed C1 cubic - Catmull-Rom with wraparound, so the contour of a curve
    stays a curve instead of becoming a polygon."""
    n = len(loop)
    if n < 3:
        return ""
    out = [f"M{loop[0][0]:.2f},{loop[0][1]:.2f}"]
    for i in range(n):
        p0, p1 = loop[i - 1], loop[i]
        p2, p3 = loop[(i + 1) % n], loop[(i + 2) % n]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6.0, p1[1] + (p2[1] - p0[1]) / 6.0)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6.0, p2[1] - (p3[1] - p1[1]) / 6.0)
        out.append(f"C{c1[0]:.2f},{c1[1]:.2f} {c2[0]:.2f},{c2[1]:.2f} {p2[0]:.2f},{p2[1]:.2f}")
    out.append("Z")
    return "".join(out)


# ------------------------------------------------------------------ the trace

class Trace:
    """One reference plate, as the six drawables need it: ink at the lane's weight, fitted
    into the 24-unit box, as closed filled contours."""

    def __init__(self, group: str, ref: str, radius: int | None = None):
        self.group = group
        self.ref = ref
        w, h, _ = decode_png(REF_DIR / ref)
        raw = ink_mask(REF_DIR / ref)
        self.frame_stripped = group in FRAME_PLATES
        if self.frame_stripped:
            raw = strip_frame(raw, thickness(raw))
        self.source = (w, h)

        # The ink's own line, measured: the plate's area over its skeleton length would be the
        # honest number, but the outline's area over its own bounding box is not - so the
        # radius that lands the ink on INK_TARGET is found by measurement below, not guessed.
        self.radius = radius if radius is not None else self.radius_for_weight(raw)

        mask = thicken(raw, self.radius)
        rows, cols = np.nonzero(mask)
        x0, x1, y0, y1 = cols.min(), cols.max(), rows.min(), rows.max()
        self.bbox = (int(x0), int(y0), int(x1), int(y1))

        raw_loops = contours(mask)
        span = max(x1 - x0 + 1, y1 - y0 + 1)
        scale = (VIEWPORT - 2 * MARGIN) / span
        cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0

        self.loops, self.specks, self.points = [], 0, 0
        for loop in raw_loops:
            if _area(loop) < MIN_AREA:
                self.specks += 1
                continue
            fitted = [((x - cx) * scale + VIEWPORT / 2, (y - cy) * scale + VIEWPORT / 2)
                      for x, y in simplify(smooth_cyclic(loop))]
            self.loops.append(fitted)
            self.points += len(fitted)
        self.scale = scale
        self.px_per_unit = px_per_unit = span / (VIEWPORT - 2 * MARGIN)
        self.ink_width = thickness(mask) / px_per_unit

    @staticmethod
    def radius_for_weight(mask: np.ndarray) -> int:
        """The dilation radius that lands the ink's weight on INK_TARGET. A stroke of weight
        t px grows by 2r under a dilation of radius r, and the plate is fitted into a box
        (VIEWPORT - 2*MARGIN) units wide, so the radius is arithmetic once t is measured:
        t comes from thickness(), which does not care whether a stroke runs across the plate
        or down it."""
        h, w = mask.shape
        px_per_unit = max(w, h) / (VIEWPORT - 2 * MARGIN)
        wanted = INK_TARGET * px_per_unit
        return max(0, int(round((wanted - thickness(mask)) / 2.0)))

    def xml(self) -> str:
        weight_note = (
            f"the ink was THICKENED by {self.radius}px to reach the filter lane's weight"
            if self.radius else
            "the ink is left at the weight it was drawn at, because that already measures\n"
            "     inside the filter lane"
        )
        head = (
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            f"<!-- MUSCLE GLYPH - {self.group.upper()}. The owner's own drawing `{self.ref}`\n"
            "     (512x512 line art, /home/matrix/Downloads/Muscles), vectorised by\n"
            "     evidence/muscle-icons-2026-10-13/trace_refs.py: the plate's ink is binarised,\n"
            f"     {weight_note}, and each connected\n"
            "     region's contour is emitted as a closed filled path. Owner, 2026-10-13: the\n"
            "     previous glyphs were \"not at all the same as the images\", so nothing here is\n"
            "     redrawn - every curve is the ink's own outline, and the only decisions are\n"
            "     weight, fit and which specks to drop.\n\n"
            f"     {self.points} points over {len(self.loops)} contours, measuring\n"
            f"     {self.ink_width:.2f} units of ink. Filled, not stroked: the fill IS the drawn\n"
            "     line, which is how the drawing's own weight and its round ends survive at all -\n"
            "     a centre-line trace has to invent both, and at twice the ink. The loops are\n"
            "     wound by the walk (ink on the right, so holes oppose outlines) and the fill type\n"
            "     is left at the default non-zero, which is what keeps a ring a band.\n\n"
            "     Regenerate with trace_refs.py in that same evidence directory. -->\n"
            "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            "    android:width=\"24dp\"\n"
            "    android:height=\"24dp\"\n"
            "    android:viewportWidth=\"24\"\n"
            "    android:viewportHeight=\"24\">\n"
        )
        # fillType is left at the default (non-zero) on purpose: the loops are wound by the
        # walk - ink on the right, so outlines and holes come out opposed - and non-zero
        # fills exactly what they wind around. evenOdd would find a hole wherever a loop
        # pinches against itself, which on a drawing of crossing strokes is everywhere.
        body = "".join(
            "    <path\n        android:fillColor=\"#FF000000\"\n"
            f"        android:pathData=\"{closed_path(loop)}\" />\n"
            for loop in self.loops
        )
        return head + body + "</vector>\n"

    def ascii(self, cols: int = 60) -> str:
        """A square ASCII plate of what will be DRAWN, sampled off the emitted contours."""
        rows = cols // 2
        grid = [["." for _ in range(cols)] for _ in range(rows)]
        for loop in self.loops:
            for (ax, ay), (bx, by) in zip(loop, loop[1:] + loop[:1]):
                steps = max(2, int(math.hypot(bx - ax, by - ay) * 8))
                for s in range(steps + 1):
                    t = s / steps
                    x, y = ax + (bx - ax) * t, ay + (by - ay) * t
                    gx, gy = int(x / VIEWPORT * (cols - 1)), int(y / VIEWPORT * (rows - 1))
                    if 0 <= gx < cols and 0 <= gy < rows:
                        grid[gy][gx] = "#"
        return "\n".join("".join(row) for row in grid)

    def weight(self) -> float:
        """The emitted ink's weight in viewport units. The contours ARE the mask's boundary,
        so the mask's own measured thickness is that number to within half a pixel - and it
        is the one MuscleGlyphFamilyTest compares against the lane, measured the same way."""
        return self.ink_width


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--write", action="store_true", help="emit the six drawables")
    ap.add_argument("--stats", action="store_true", help="print what each trace produced")
    ap.add_argument("--ascii", metavar="GROUP", help="ASCII render of one traced glyph")
    ap.add_argument("--only", metavar="GROUP", help="restrict --write to one group")
    ap.add_argument("--radius", type=int, help="override the dilation radius")
    ap.add_argument("--weight", action="store_true", help="measure the emitted ink")
    args = ap.parse_args()

    if args.ascii:
        t = Trace(args.ascii, SOURCES[args.ascii], args.radius)
        print(f"{args.ascii} <- {t.ref}: radius {t.radius}px, {len(t.loops)} contours, "
              f"{t.points} points, {t.specks} specks dropped")
        print(t.ascii())
        return

    if args.write:
        BACKUP.mkdir(exist_ok=True)
        for group, ref in SOURCES.items():
            if args.only and group != args.only:
                continue
            target = DRAWABLES / f"ic_muscle_{group}.xml"
            # The backup is the glyph as it was BEFORE this work began; re-tracing must never
            # overwrite it with a trace, or the board's "before" column becomes a picture of
            # itself.
            if target.exists() and not (BACKUP / target.name).exists():
                (BACKUP / target.name).write_text(target.read_text(encoding="utf-8"),
                                                  encoding="utf-8")
            t = Trace(group, ref, args.radius)
            target.write_text(t.xml(), encoding="utf-8")
            print(f"{target.name}: {ref} -> radius {t.radius}px, {len(t.loops)} contours, "
                  f"{t.points} points, {t.specks} specks"
                  + (", frame stripped" if t.frame_stripped else ""))
        return

    if args.weight:
        print(f"{'group':<11}{'source':<16}{'loops':>7}{'points':>8}{'specks':>8}"
              f"{'radius':>8}{'width(units)':>14}")
        for group, ref in SOURCES.items():
            t = Trace(group, ref, args.radius)
            print(f"{group:<11}{ref:<16}{len(t.loops):>7}{t.points:>8}{t.specks:>8}"
                  f"{t.radius:>8}{t.weight():>14.2f}")
        return

    print(f"{'group':<11}{'source':<16}{'loops':>7}{'points':>8}{'specks':>8}{'radius':>8}"
          f"{'ink bbox':>22}")
    for group, ref in SOURCES.items():
        t = Trace(group, ref, args.radius)
        print(f"{group:<11}{ref:<16}{len(t.loops):>7}{t.points:>8}{t.specks:>8}{t.radius:>8}"
              f"{str(t.bbox):>22}")


if __name__ == "__main__":
    main()
