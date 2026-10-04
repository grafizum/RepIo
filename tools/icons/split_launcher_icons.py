#!/usr/bin/env python3
"""Split the launcher icons into separately-named light and dark resources.

THE PROBLEM THIS SOLVES
-----------------------
Android resolves `mipmap-night-anydpi-v26/` OVER `mipmap-anydpi-v26/` whenever
the SYSTEM is in dark mode, and it does this before the app gets a say. So a
user who picks a LIGHT app theme (Beige) on a phone that is in system dark
mode still gets Beige's DARK-plate icon - which is exactly the owner's report
("on light beige it shows orange dark and the rest also still shows the dark
icons not the light once").

Reverting the five dark plates in the light set (commit 4f2f1a7's damage) did
NOT fix this, and could not: the override is Android's resource resolution, not
a value in our files. As long as a light theme and its night variant share one
resource NAME, a dark system always picks the dark one.

THE FIX
-------
Give the two appearances DIFFERENT NAMES:

  ic_launcher_beige_light   -> light plate, light artwork, NO night variant
  ic_launcher_beige_dark    -> dark plate,  dark artwork,  NO night variant

Because `_light` has no `mipmap-night-*` counterpart, Android cannot override
it: a light app theme keeps its light icon on any system setting. And because
`_dark` is a real file rather than a night-qualified one, a dark theme keeps
its dark icon on a light system too. The theme choice now decides the icon,
which is what the owner asked for ("separate the icons").

These are COPIES, generated from the two existing sets, so both appearances
always derive from one source and cannot drift. Re-run this after changing any
source icon.

Run from the repo root:
    python tools/icons/split_launcher_icons.py          # write the resources
    python tools/icons/split_launcher_icons.py --check  # verify, no writes
"""
from __future__ import annotations

import argparse
import pathlib
import shutil
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "composeApp/src/main/res"

# Theme ownership, taken from WorkoutTrackerTheme's own isDarkThemeUsed branch
# in Theme.kt. That function is the app's definition of which scheme is dark;
# this table mirrors it, and the test `lightAndDarkIconsUseSeparateNames` in
# LauncherIconNightVariantTest asserts the two cannot silently diverge.
LIGHT_THEMES = ["beige", "emerald", "crimson", "royal_blue"]
DARK_THEMES = ["flame", "ocean", "rose", "purple", "retro"]

DENSITIES = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/{plate}" />
    <foreground android:drawable="@drawable/{foreground}" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""


def write(path: pathlib.Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def copy(src: pathlib.Path, dst: pathlib.Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)


def generate() -> list[str]:
    made = []

    # 1. Foreground ARTWORK under new names. The light set comes from the plain
    #    `drawable-<d>/` files; the dark set from `drawable-night-<d>/`. Both are
    #    written into the PLAIN drawable dir under a new name, because the new
    #    names must not be night-qualified - a night-qualified name is the exact
    #    thing being designed out.
    for density in DENSITIES:
        for theme in LIGHT_THEMES:
            src = RES / f"drawable-{density}/ic_launcher_fg_{theme}.png"
            dst = RES / f"drawable-{density}/ic_launcher_fg_{theme}_light.png"
            copy(src, dst)
            made.append(str(dst.relative_to(ROOT)))
        for theme in DARK_THEMES:
            src = RES / f"drawable-night-{density}/ic_launcher_fg_{theme}.png"
            dst = RES / f"drawable-{density}/ic_launcher_fg_{theme}_dark.png"
            copy(src, dst)
            made.append(str(dst.relative_to(ROOT)))

    # 2. Adaptive icons under new names, in the PLAIN anydpi dir. No
    #    `mipmap-night-anydpi-v26/ic_launcher_*_light.xml` is written, on
    #    purpose: that absence is what pins the light icon.
    for theme in LIGHT_THEMES:
        path = RES / f"mipmap-anydpi-v26/ic_launcher_{theme}_light.xml"
        write(path, ADAPTIVE_XML.format(
            plate="icon_bg_white", foreground=f"ic_launcher_fg_{theme}_light"))
        made.append(str(path.relative_to(ROOT)))
    for theme in DARK_THEMES:
        path = RES / f"mipmap-anydpi-v26/ic_launcher_{theme}_dark.xml"
        write(path, ADAPTIVE_XML.format(
            plate="icon_bg_dark", foreground=f"ic_launcher_fg_{theme}_dark"))
        made.append(str(path.relative_to(ROOT)))

    # 3. Legacy raster mipmaps under the new names. Some launchers and the
    #    pre-API-26 path use the flat PNG rather than the adaptive XML, and a
    #    night-qualified raster would reintroduce the same override.
    for density in DENSITIES:
        for theme in LIGHT_THEMES:
            src = RES / f"mipmap-{density}/ic_launcher_{theme}.png"
            dst = RES / f"mipmap-{density}/ic_launcher_{theme}_light.png"
            copy(src, dst)
            made.append(str(dst.relative_to(ROOT)))
        for theme in DARK_THEMES:
            src = RES / f"mipmap-night-{density}/ic_launcher_{theme}.png"
            dst = RES / f"mipmap-{density}/ic_launcher_{theme}_dark.png"
            copy(src, dst)
            made.append(str(dst.relative_to(ROOT)))

    return made


def check() -> int:
    """Every expected file must exist; nothing may be night-qualified."""
    problems = []

    def require(path: pathlib.Path, note: str) -> None:
        if not path.is_file():
            problems.append(f"missing {path.relative_to(ROOT)} ({note})")

    for density in DENSITIES:
        for theme in LIGHT_THEMES:
            require(RES / f"drawable-{density}/ic_launcher_fg_{theme}_light.png", "light artwork")
            require(RES / f"mipmap-{density}/ic_launcher_{theme}_light.png", "light raster")
        for theme in DARK_THEMES:
            require(RES / f"drawable-{density}/ic_launcher_fg_{theme}_dark.png", "dark artwork")
            require(RES / f"mipmap-{density}/ic_launcher_{theme}_dark.png", "dark raster")

    for theme in LIGHT_THEMES:
        require(RES / f"mipmap-anydpi-v26/ic_launcher_{theme}_light.xml", "light adaptive")
        # The whole point: no night-qualified twin of the light name.
        night = RES / f"mipmap-night-anydpi-v26/ic_launcher_{theme}_light.xml"
        if night.exists():
            problems.append(
                f"{night.relative_to(ROOT)} exists - a night-qualified _light icon "
                f"lets Android override the light theme again, which is the bug"
            )

    for theme in DARK_THEMES:
        require(RES / f"mipmap-anydpi-v26/ic_launcher_{theme}_dark.xml", "dark adaptive")

    # Every adaptive XML must name exactly the plate it claims.
    for theme in LIGHT_THEMES:
        path = RES / f"mipmap-anydpi-v26/ic_launcher_{theme}_light.xml"
        if path.is_file() and "icon_bg_dark" in path.read_text(encoding="utf-8"):
            problems.append(f"{theme}_light uses a dark plate")
    for theme in DARK_THEMES:
        path = RES / f"mipmap-anydpi-v26/ic_launcher_{theme}_dark.xml"
        if path.is_file() and "icon_bg_white" in path.read_text(encoding="utf-8"):
            problems.append(f"{theme}_dark uses a light plate")

    if problems:
        print(f"FAILED ({len(problems)}):")
        for p in problems:
            print(f"  - {p}")
        return 1
    print("OK: every light and dark icon exists under its own name, and no "
          "_light icon is night-qualified.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="verify only, write nothing")
    args = parser.parse_args()

    if args.check:
        return check()

    made = generate()
    print(f"wrote {len(made)} files")
    result = check()
    return result


if __name__ == "__main__":
    sys.exit(main())