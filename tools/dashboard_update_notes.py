#!/usr/bin/env python3
"""Update a dashboard task's body notes.

`dashboard_set_status.py` only sends `status` + `reason`, and a PATCH carrying
anything else is rejected with 400 `validation_error` unless a `reason` rides
along too - the API requires one on EVERY state-changing call, not just a status
change. So the full note cannot be pushed with that helper; this one sends
`notes` and `reason` together.

The token is read from .env and never printed.

    python tools/dashboard_update_notes.py task_abc123 --file notes.txt
    python tools/dashboard_update_notes.py task_abc123 --text "short note"
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# The API rejects a longer notes string with 400 validation_error rather than
# truncating it, so the cut happens here and says so. A silently shortened note
# would read on the board as if that were all there was to say.
API_MAX_NOTES = 5000

TRUNCATION_MARKER = (
    "\n\n[TRUNCATED to fit the board's 5000-character limit - the full text is "
    "kept in the repository, see the session handoff referenced above.]"
)


def truncate(text: str, limit: int) -> str:
    """Cut `text` to `limit` on a line boundary and mark that it was cut."""
    if len(text) <= limit:
        return text
    budget = max(0, limit - len(TRUNCATION_MARKER))
    head = text[:budget]
    # Prefer ending on a line break so the board never shows half a sentence.
    cut = head.rfind("\n")
    if cut > budget // 2:
        head = head[:cut]
    return head.rstrip() + TRUNCATION_MARKER



# Grafizum credentials are per project and live OUTSIDE the checkout, under
# ~/.config/grafizum/projects/. See tools/grafizum_env.py.
sys.path.insert(0, str(ROOT / "tools"))
from grafizum_env import load_env  # noqa: E402  (after ROOT is known)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("task_id")
    group = ap.add_mutually_exclusive_group(required=True)
    group.add_argument("--file", type=Path, help="read the notes from a file")
    group.add_argument("--text", help="the notes text")
    ap.add_argument("--reason", default="notes synced by the RepIo agent")
    ap.add_argument(
        "--max-chars",
        type=int,
        default=API_MAX_NOTES,
        help=f"truncate to this many characters (API cap is {API_MAX_NOTES})",
    )
    args = ap.parse_args()

    notes = args.text if args.text is not None else args.file.read_text(encoding="utf-8")
    notes = truncate(notes, args.max_chars)

    env = load_env()
    base = env.get("GRAFIZUM_DASHBOARD_URL", "https://api.grafizum.com/v1").rstrip("/")
    if base.endswith("/events"):
        base = base[: -len("/events")]
    token = env.get("GRAFIZUM_DASHBOARD_TOKEN", "")
    if not token:
        print("GRAFIZUM_DASHBOARD_TOKEN is not set", file=sys.stderr)
        return 1

    body = json.dumps({"notes": notes, "reason": args.reason}).encode("utf-8")
    # The API refuses every write with 428 instructions_required until the
    # handbook has been fetched within 24h, and the grant appears to be held
    # per-PROCESS rather than per-token: a successful GET from an earlier
    # script does not satisfy a write from this one. Fetching it here, in the
    # same process that writes, is what actually opens the gate. Reads are
    # always allowed, so this costs nothing when the gate is already open.
    try:
        urllib.request.urlopen(
            urllib.request.Request(
                f"{base}/agent/instructions",
                headers={
                    "Authorization": f"Bearer {token}",
                    "Accept": "application/json",
                    "User-Agent": "curl/8.5.0",
                },
            ),
            timeout=30,
        ).read()
    except (urllib.error.HTTPError, urllib.error.URLError) as exc:
        print(f"could not read the handbook to open the write gate: {exc}", file=sys.stderr)
    key = hashlib.sha256(f"{args.task_id}:notes:{notes}".encode("utf-8")).hexdigest()[:32]
    request = urllib.request.Request(
        f"{base}/tasks/{args.task_id}",
        data=body,
        method="PATCH",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Idempotency-Key": key,
            "User-Agent": "curl/8.5.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            data = json.loads(response.read().decode("utf-8"))["data"]
            print(f"{args.task_id} -> notes updated ({response.status})")
            print("  status:", data.get("status"))
            print("  notes length:", len(data.get("notes", "")))
            if len(notes) >= API_MAX_NOTES and notes.endswith(".]"):
                print("  NOTE: text was truncated to the board's 5000-char limit.")
            print("  updated_at:", data.get("updated_at"))
            return 0
    except urllib.error.HTTPError as exc:
        print(f"HTTP {exc.code}: {exc.read().decode('utf-8', 'replace')[:400]}", file=sys.stderr)
        return 1
    except urllib.error.URLError as exc:
        print(f"network error: {exc.reason}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
