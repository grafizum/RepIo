#!/usr/bin/env python3
"""Build the self-contained theme report from its template.

`register_preview` serves exactly ONE file — the registered HTML — and 404s every
sibling, including a `shots/` subdirectory, so a page that points at PNG files shows
broken images. This inlines each capture as a downscaled JPEG data URI instead.

    python3 build_page.py        # page_template.html -> index.html

The full-resolution PNGs stay in `shots/` as the archival evidence; the page carries
420px-wide copies, which is all a ~260px phone frame can show.
"""
from __future__ import annotations

import base64
import re
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
TEMPLATE = HERE / "page_template.html"
OUTPUT = HERE / "index.html"
WIDTH = 420


def data_uri(png: Path) -> str:
    with tempfile.NamedTemporaryFile(suffix=".jpg") as tmp:
        subprocess.run(
            ["convert", str(png), "-resize", f"{WIDTH}x", "-quality", "80", tmp.name],
            check=True,
            capture_output=True,
        )
        raw = Path(tmp.name).read_bytes()
    return "data:image/jpeg;base64," + base64.b64encode(raw).decode("ascii")


def main() -> int:
    html = TEMPLATE.read_text(encoding="utf-8")
    seen: dict[str, str] = {}

    def swap(m: re.Match) -> str:
        name = m.group(1)
        if name not in seen:
            png = HERE / "shots" / name
            if not png.exists():
                raise SystemExit(f"capture missing: {png}")
            seen[name] = data_uri(png)
        return f'src="{seen[name]}"'

    out = re.sub(r'src="shots/([^"]+)"', swap, html)
    OUTPUT.write_text(out, encoding="utf-8")
    print(f"{OUTPUT.name}: {len(seen)} captures inlined, {OUTPUT.stat().st_size / 1e6:.2f} MB")
    for name in seen:
        print(f"   {name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
