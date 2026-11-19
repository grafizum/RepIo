#!/usr/bin/env python3
"""Drive the welcome screen (guided build) on a real device and read what it generates.

Owner, 2026-10-08: "test each filter and day and day amounts etc.. inside the welcome
screen and check if the system that outputs the workout for each day is truly a
professional workout".

The welcome screen is `FirstTimeSetupScreen`'s guided tab. Every control on it feeds the
SAME generator the app uses for the real week, so the honest way to check the generator is
to change one control at a time on the device and read back what the option cards say they
will build (`Ganzkörper · Oben · Unten`, `Beintage: 2`) plus whatever warning the screen
prints.

Coordinates are never hard-coded: every tap resolves the label to the clickable node it
lives in, right now. A layout change is then a rerun, not a rewrite.

Usage:
  python3 tools/welcome_sweep.py <serial> days     # 2..6 days, prefix + spread patterns
  python3 tools/welcome_sweep.py <serial> filters  # equipment / goal / duration / intensity
  python3 tools/welcome_sweep.py <serial> save     # commit a config and read the real week
"""
from __future__ import annotations

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

EVIDENCE = "evidence/pixel-2026-10-08"
LOG = []
LOGNAME = "welcome-sweep.txt"


def sh(serial: str, *args: str) -> str:
    return subprocess.run(
        ["adb", "-s", serial, *args], check=True, capture_output=True, text=True
    ).stdout


def dump(serial: str, attempts: int = 5) -> ET.Element:
    """The current screen as a node tree.

    `uiautomator dump` is an ordinary process on the device and the system kills it
    (exit 137) often enough - under memory pressure, or when anything else is dumping at
    the same time - that a sweep without a retry dies on a phone that is fine. Retrying
    is part of reading the screen, not a workaround for a broken device.
    """
    for i in range(attempts):
        try:
            sh(serial, "shell", "uiautomator", "dump", "--compressed", "/sdcard/ui.xml")
            xml = sh(serial, "exec-out", "cat", "/sdcard/ui.xml")
            return ET.fromstring(xml)
        except (subprocess.CalledProcessError, ET.ParseError) as exc:
            log(f"  .. dump attempt {i + 1} failed ({(exc.returncode if hasattr(exc, 'returncode') else 'parse')}), retrying")
            time.sleep(1.5)
    raise SystemExit("uiautomator dump kept failing; nothing read this screen")


def nodes(root):
    return list(root.iter("node"))


def text_of(node) -> str:
    return (node.attrib.get("text") or "").strip()


def desc_of(node) -> str:
    return (node.attrib.get("content-desc") or "").strip()


def bounds(node):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib["bounds"])
    return tuple(int(g) for g in m.groups())


def parent_map(root):
    out = {}
    for parent in root.iter("node"):
        for child in parent:
            out[child] = parent
    return out


def tap(serial: str, x: int, y: int):
    sh(serial, "shell", "input", "tap", str(x), str(y))


def label_nodes(root, label: str, contains: bool = False):
    hits = []
    for n in nodes(root):
        t = text_of(n)
        d = desc_of(n)
        if (contains and (label in t or label in d)) or (not contains and (t == label or d == label)):
            hits.append(n)
    return hits


def tap_label(serial: str, label: str, nth: int = 0, contains: bool = False, root=None) -> bool:
    root = root if root is not None else dump(serial)
    hits = label_nodes(root, label, contains)
    if len(hits) <= nth:
        log(f"  !! label not found: {label!r} (hits={len(hits)})")
        return False
    node = hits[nth]
    parents = parent_map(root)
    target = node
    while target in parents and parents[target].attrib.get("clickable") != "true":
        target = parents[target]
    l, t, r, b = bounds(target)
    x, y = (l + r) // 2, (t + b) // 2
    tap(serial, x, y)
    return True


def log(line: str = ""):
    LOG.append(line)
    print(line, flush=True)


def flush_log(name: str):
    with open(f"{EVIDENCE}/{name}", "w") as fh:
        fh.write("\n".join(LOG) + "\n")


def shot(serial: str, name: str):
    data = subprocess.run(
        ["adb", "-s", serial, "exec-out", "screencap", "-p"], check=True, capture_output=True
    ).stdout
    with open(f"{EVIDENCE}/{name}", "wb") as fh:
        fh.write(data)


def save_xml(serial: str, name: str):
    """Keep the raw node tree of a screen, so a later reader sees what this one saw."""
    ET.ElementTree(dump(serial)).write(f"{EVIDENCE}/{name}", encoding="utf-8")


def option_cards(root) -> list[str]:
    """The three option cards as `Header | preview | best-for` lines."""
    out = []
    for n in nodes(root):
        t = text_of(n)
        if re.match(r"^Option [ABC] —", t):
            out.append(t)
    return out


def card_details(root) -> list[dict]:
    """Group each card's own texts, so a card can be read without a screenshot."""
    cards = []
    current = None
    for n in nodes(root):
        t = text_of(n)
        if re.match(r"^Option [ABC] —", t):
            current = {"header": t, "lines": []}
            cards.append(current)
        elif current is not None and t and not t.startswith("Option "):
            if re.match(r"^(Speichern|Geführter|Eigener|Trainingstage|Wie du)", t):
                current = None
                continue
            current["lines"].append(t)
    return cards


def warnings(root) -> list[str]:
    pats = ("Risiko", "Warnung", "nicht genug", "fehlt", "Übertraining", "unvollständig")
    return [text_of(n) for n in nodes(root) if any(p in text_of(n) for p in pats)]


def report_state(serial: str, tag: str, notes: str = ""):
    root = dump(serial)
    cards = card_details(root)
    log(f"--- {tag}{(' | ' + notes) if notes else ''}")
    for c in cards:
        log(f"    {c['header']}")
        for line in c["lines"]:
            log(f"        {line}")
    w = warnings(root)
    if w:
        log(f"    WARNINGS: {w}")
    shot(serial, f"welcome-{tag}.png")
    flush_log(LOGNAME)
    return root


def day_count_from(root) -> int | None:
    """How many training days the selected option actually plans.

    Read from the option cards, which are the screen's own statement of what it will
    build. A one-day week has no option card at all, so `None` there means "not a week
    this screen will describe", not "zero".
    """
    for c in card_details(root):
        for line in c["lines"]:
            m = re.match(r"^(\d+) Tage pro Woche", line)
            if m:
                return int(m.group(1))
            if "·" in line and not line.startswith("Am besten"):
                return len([p for p in line.split("·") if p.strip()])
    return None


DAYS = ["Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"]
MODEL = {"on": None}


def probe_selected(serial: str) -> list[str]:
    """Which weekday chips are ON right now.

    Compose reports no `selected` flag for these chips, so the state has to be read
    BEHAVIOURALLY: tap the chip, and the option cards' day count moves by exactly one in
    the direction that says which way it went. Every probe is undone immediately, so the
    screen is left as it was found and the sweep never relies on a guessed default.
    """
    base = day_count_from(dump(serial))
    if base is None or base < 3:
        log(
            f"  !! cannot probe from {base} days: a probe has to dip one day and the two-day "
            "screen is the lowest one with readable cards. Relaunch the app with cleared data."
        )
        return []
    on = []
    for d in DAYS:
        if base <= 1:
            log(f"  !! probe {d}: only {base} day selected, refusing to go lower")
            break
        if not tap_label(serial, d):
            continue
        time.sleep(0.9)
        n = day_count_from(dump(serial))
        if n == base - 1:
            log(f"  probe {d}: ON")
            on.append(d)
        elif n == base + 1:
            log(f"  probe {d}: off")
        else:
            log(f"  !! probe {d}: count {base} -> {n}, expected +/-1")
        tap_label(serial, d)  # restore, so each probe starts from the same screen
        time.sleep(0.9)
        restored = day_count_from(dump(serial))
        if restored != base:
            log(f"  !! probe {d}: restore failed ({base} -> {restored})")
            base = restored
    log(f"  selected days = {sorted(on)} (cards plan {base})")
    return on


def set_days(serial: str, wanted: list[str]):
    """Tap chips until the selected set is exactly `wanted` (a tap toggles one day).

    ADDS before it REMOVES. Toggling down to a single day leaves the screen with no
    option card to read, and a sweep that cannot read the count cannot verify itself, so
    the path between two multi-day states must never dip below two.

    The selected set is probed ONCE, from the default state, and then tracked by hand:
    re-probing from a two-day state would have to dip to one day to learn anything, and
    that state has no card to read.
    """
    on = MODEL["on"]
    if on is None:
        on = probe_selected(serial)
        if not on:
            # Fall back to the state a cleared install starts in (Mo/Mi/Fr, asserted on
            # device 2026-10-08) rather than skipping: a wrong assumption shows up
            # immediately as a count mismatch in the log below.
            log("  !! assuming the cleared-install default Mo+Mi+Fr")
            on = ["Mo", "Mi", "Fr"]
        MODEL["on"] = list(on)

    def flip(d: str, want: bool) -> None:
        expected = len(on) + (1 if want else -1)
        if not tap_label(serial, d):
            return
        time.sleep(0.9)
        n = day_count_from(dump(serial))
        log(f"  day {d} -> {'on' if want else 'off'} (cards now plan {n} days)")
        if n is not None and n != expected:
            log(f"  !! after flipping {d} the cards should plan {expected} days, they say {n}")

    for want in (True, False):
        for d in DAYS:
            if (d in wanted) == want and (d in on) != want:
                flip(d, want)
                if want:
                    on.append(d)
                else:
                    on.remove(d)
                MODEL["on"] = list(on)
    MODEL["on"] = list(on)
    return on


def sweep_days(serial: str):
    log("== day amounts (welcome screen, guided build)")
    patterns = {
        2: [["Mo", "Do"], ["Mo", "Fr"]],
        3: [["Mo", "Mi", "Fr"], ["Mo", "Di", "Do"]],
        4: [["Mo", "Di", "Do", "Fr"], ["Mo", "Di", "Fr", "So"]],
        5: [["Mo", "Di", "Mi", "Do", "Fr"], ["Mo", "Mi", "Do", "Fr", "So"]],
        6: [["Mo", "Di", "Mi", "Do", "Fr", "Sa"]],
    }
    for count, variants in patterns.items():
        for i, want in enumerate(variants):
            set_days(serial, want)
            report_state(serial, f"days{count}{'b' if i else 'a'}", f"chips={'+'.join(want)}")
    for count in (3, 4, 5, 6):  # prove the count is what matters, not the particular days
        set_days(serial, DAYS[:count])
        report_state(serial, f"days{count}-prefix", f"chips={'+'.join(DAYS[:count])}")


def scroll_to(serial: str, label: str, tries: int = 8) -> bool:
    for _ in range(tries):
        root = dump(serial)
        if label_nodes(root, label, contains=True):
            return True
        sh(serial, "shell", "input", "swipe", "540", "1900", "540", "900", "250")
        time.sleep(0.8)
    return False


def sweep_filters(serial: str):
    """Tap every control below the day picker one at a time and record the result."""
    log("== filters (equipment / goal / duration / intensity)")
    save_xml(serial, "filters-start.xml")
    report_state(serial, "filters-start")
    seen = list(DAYS) + [
        # Not filters: the tab bar, the save button and the sheet's close X all navigate
        # away from the screen being walked.
        "Speichern", "Geführter Aufbau", "Eigener Aufbau", "Schließen", "Zurück",
    ]
    for _ in range(14):
        root = dump(serial)
        fresh = []
        for n in nodes(root):
            if n.attrib.get("clickable") != "true":
                continue
            t = text_of(n) or desc_of(n)
            if not t or t in seen or t.startswith("Option "):
                continue
            fresh.append((t, n))
        if not fresh:
            sh(serial, "shell", "input", "swipe", "540", "1900", "540", "950", "250")
            time.sleep(0.9)
            continue
        label, _ = fresh[0]
        seen.append(label)
        log(f"\n>>> tap filter {label!r}")
        tap_label(serial, label)
        time.sleep(1.6)
        report_state(serial, f"filter-{re.sub('[^A-Za-z0-9]+', '-', label)[:32]}")


def save_and_read(serial: str):
    log("== save a configuration and read the generated week back")
    tap_label(serial, "Speichern", contains=True)
    time.sleep(4)
    save_xml(serial, "after-save.xml")
    root = dump(serial)
    for n in nodes(root):
        t = text_of(n)
        if t:
            log(f"    {bounds(n)} {t[:110]}")


def find_tap(serial: str, label: str) -> bool:
    """Tap a control by label, scrolling the page if it is not on screen yet."""
    if label_nodes(dump(serial), label, contains=True):
        return tap_label(serial, label, contains=True)
    for _ in range(6):
        sh(serial, "shell", "input", "swipe", "540", "1900", "540", "950", "250")
        time.sleep(0.7)
        if label_nodes(dump(serial), label, contains=True):
            return tap_label(serial, label, contains=True)
    for _ in range(12):
        sh(serial, "shell", "input", "swipe", "540", "950", "540", "1900", "250")
        time.sleep(0.7)
        if label_nodes(dump(serial), label, contains=True):
            return tap_label(serial, label, contains=True)
    log(f"  !! never found {label!r}")
    return False


def tap_sequence(serial: str, labels: list[str]):
    """Tap a list of labels in order, recording the screen after each one.

    The label IS the assertion on device: a control that has been tapped shows up as
    selected in the next dump, and a control that silently did nothing does not.
    """
    for label in labels:
        log(f"\n>>> tap {label!r}")
        if not find_tap(serial, label):
            continue
        time.sleep(1.5)
        tag = re.sub("[^A-Za-z0-9]+", "-", label)[:28].strip("-")
        report_state(serial, f"tap-{tag}")


def inventory(serial: str, steps: int = 6):
    """Walk the whole welcome page top to bottom, keeping every screen it passes.

    The option-card recorder only sees whatever is on screen; the filters (equipment,
    goal, session length, difficulty and the extra-preferences block) live below the
    fold, so the page has to be slid through once to know what is actually there.
    """
    log("== welcome page inventory")
    for _ in range(10):
        sh(serial, "shell", "input", "swipe", "540", "950", "540", "1900", "200")
        time.sleep(0.4)
    tap_label(serial, "Geführter Aufbau", contains=True)
    time.sleep(1.0)
    for _ in range(4):
        sh(serial, "shell", "input", "swipe", "540", "950", "540", "1900", "200")
        time.sleep(0.4)
    for i in range(steps):
        root = dump(serial)
        save_xml(serial, f"welcome-slide-{i}.xml")
        shot(serial, f"welcome-slide-{i}.png")
        short = [
            text_of(n) for n in nodes(root)
            if text_of(n) and len(text_of(n)) <= 34 and text_of(n) not in DAYS
        ]
        log(f"--- slide {i}: {' | '.join(short)}")
        flush_log(LOGNAME)
        sh(serial, "shell", "input", "swipe", "540", "1900", "540", "900", "250")
        time.sleep(0.8)


def main() -> int:
    serial = sys.argv[1]
    what = sys.argv[2] if len(sys.argv) > 2 else "days"
    global LOGNAME
    if what in ("inventory", "days", "filters", "week"):
        LOGNAME = f"welcome-{what}.txt"
    log(f"# welcome sweep on {serial} — {what} — {time.strftime('%Y-%m-%d %H:%M')}")
    if what == "tap":
        LOGNAME = "welcome-taps.txt"
        tap_sequence(serial, sys.argv[3:])
        flush_log(LOGNAME)
        return 0
    if what == "inventory":
        inventory(serial)
    if what == "days":
        sweep_days(serial)
    elif what == "filters":
        sweep_filters(serial)
    elif what == "save":
        save_and_read(serial)
    elif what == "state":
        report_state(serial, sys.argv[3] if len(sys.argv) > 3 else "state")
    flush_log(LOGNAME)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
