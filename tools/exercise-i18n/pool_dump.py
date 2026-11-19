"""Dump the generated exercise catalog into JSON.

The Kotlin catalog is the single source of truth for the pool, but the i18n
tooling still expects the pre-CSV layout. This reads the generated
`ExerciseCatalog.ENTRIES` list directly so translations, the equipment gate
and the muscle tables can be worked on without a Gradle round-trip.

    python tools/exercise-i18n/pool_dump.py            -> tools/exercise-i18n/out/pool.json
"""

import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
CATALOG = ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseCatalog.kt"
OUT = ROOT / "tools/exercise-i18n/out/pool.json"

ENTRY = re.compile(
    r'ExerciseCatalogEntry\("(?P<id>[^"]+)", "(?P<name>(?:[^"\\]|\\.)*)", '
    r'"(?P<muscle>[^"]*)", "(?P<equipment>[^"]*)", "(?P<type>[^"]*)", '
    r'"(?P<level>[^"]*)", "(?P<description>(?:[^"\\]|\\.)*)"\)'
)


def slugify(name: str) -> str:
    """Mirror of ExerciseLocalization.slugify: ASCII only, runs collapse to '_'."""
    out = []
    for ch in name.lower():
        if ch.isascii() and (ch.isalpha() or ch.isdigit()):
            out.append(ch)
        elif out and out[-1] != "_":
            out.append("_")
    return "".join(out).rstrip("_")


def main() -> int:
    src = CATALOG.read_text(encoding="utf-8")
    body = src[src.index("val ENTRIES = listOf("):]
    rows = []
    for m in ENTRY.finditer(body):
        d = m.groupdict()
        rows.append(
            {
                "id": d["id"],
                "name": d["name"],
                "slug": slugify(d["name"]),
                "muscle": d["muscle"],
                "equipment": d["equipment"],
                "description": d["description"],
            }
        )
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(rows, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(rows)} pool exercises -> {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
