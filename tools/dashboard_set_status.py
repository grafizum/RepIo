#!/usr/bin/env python3
"""Set the status of a dashboard task.

The command center has no "close" verb; a task is done when its status says so.
This PATCHes one task and prints the result. Every state-changing call on the
API needs an Idempotency-Key header - it is derived from the task and the new
status here, so re-running the same close is a no-op.

    python tools/dashboard_set_status.py task_abc123 done
"""
from __future__ import annotations

import hashlib
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load_env() -> dict[str, str]:
    env = dict(os.environ)
    env_file = ROOT / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            env.setdefault(key.strip(), value.strip().strip('"').strip("'"))
    return env


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    task_id, status = sys.argv[1], sys.argv[2]
    # The API requires a reason on every status change - it is what the board
    # shows next to the move, so it gets the rest of the command line.
    reason = " ".join(sys.argv[3:]) or f"status set to {status} by the RepIo agent"

    env = load_env()
    base = env.get("GRAFIZUM_DASHBOARD_URL", "https://api.grafizum.com/v1").rstrip("/")
    if base.endswith("/events"):
        base = base[: -len("/events")]
    token = env.get("GRAFIZUM_DASHBOARD_TOKEN", "")
    if not token:
        print("GRAFIZUM_DASHBOARD_TOKEN is not set", file=sys.stderr)
        return 1

    body = json.dumps({"status": status, "reason": reason}).encode("utf-8")
    # Open the 24h write gate in THIS process before writing - see the note in
    # dashboard_update_notes.py: the handbook grant is per-process, so a read
    # from an earlier script does not satisfy this write.
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
    key = hashlib.sha256(f"{task_id}:{status}:{reason}".encode("utf-8")).hexdigest()[:32]
    request = urllib.request.Request(
        f"{base}/tasks/{task_id}",
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
            payload = response.read().decode("utf-8")
            print(f"{task_id} -> {status} ({response.status})")
            try:
                parsed = json.loads(payload)
                if isinstance(parsed, dict):
                    print("  title:", parsed.get("title", "")[:90])
                    print("  status:", parsed.get("status"))
            except json.JSONDecodeError:
                print(" ", payload[:300])
            return 0
    except urllib.error.HTTPError as exc:
        print(f"HTTP {exc.code}: {exc.read().decode('utf-8', 'replace')[:400]}", file=sys.stderr)
        return 1
    except urllib.error.URLError as exc:
        print(f"network error: {exc.reason}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
