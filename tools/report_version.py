"""Report a built version to the Grafizum dashboard.

Mirrors what the `bumpVersion` Gradle task posts (AGENTS.md): the command center
has no app-version endpoint, so a build is recorded as a verified activity event
on the `v1/events` route. The Idempotency-Key is derived from the version, so
re-reporting the same build is a no-op instead of a second event.

    python report_version.py "what changed" [change-type]
"""

import datetime
import io
import json
import os
import sys
import urllib.error
import urllib.request

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Grafizum credentials are per project and live OUTSIDE the checkout, under
# ~/.config/grafizum/projects/. See tools/grafizum_env.py.
sys.path.insert(0, os.path.join(REPO, "tools"))
from grafizum_env import load_env as env_values  # noqa: E402


def version_props():
    props = {}
    path = os.path.join(REPO, "version.properties")
    for line in io.open(path, encoding="utf-8"):
        line = line.strip()
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def main():
    note = sys.argv[1] if len(sys.argv) > 1 else "build"
    change_type = sys.argv[2] if len(sys.argv) > 2 else "fix"
    env = env_values()
    url = env.get("GRAFIZUM_DASHBOARD_URL")
    token = env.get("GRAFIZUM_DASHBOARD_TOKEN")
    project = env.get("GRAFIZUM_PROJECT_ID") or "project_4k096a683a6r0q3b700n3z5q"
    if not url or not token:
        print("Grafizum dashboard not configured (set GRAFIZUM_DASHBOARD_URL and "
              "GRAFIZUM_DASHBOARD_TOKEN in .env) - report skipped.")
        return
    props = version_props()
    name = props.get("VERSION_NAME", "0")
    code = int(props.get("VERSION_CODE", "0"))
    payload = {
        "type": "app_version",
        "project_id": project,
        "message": f"RepIo {name} (version code {code}) - {change_type}: {note}",
        "reason": f"Version reported from the level-system branch after a local "
                  f"build was installed on the owner's device.",
        "meta": {
            "app": "RepIo",
            "packageName": "com.grafizum.repio",
            "versionName": name,
            "versionCode": code,
            "changeType": change_type,
            "note": note,
            "reportedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        },
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode(), method="POST")
    req.add_header("Authorization", "Bearer " + token)
    req.add_header("Content-Type", "application/json")
    req.add_header("Idempotency-Key", f"repio-{name}-{code}")
    # The edge in front of the API rejects urllib's default agent with a bare
    # 403 / error 1010, so identify the caller the way the Gradle task does.
    req.add_header("User-Agent", "grafizum-agent/1.0 (+https://grafizum.com)")
    try:
        with urllib.request.urlopen(req) as res:
            body = res.read().decode()[:200]
            print(f"dashboard accepted (HTTP {res.status}): {body}")
    except urllib.error.HTTPError as err:
        print(f"dashboard rejected the report (HTTP {err.code}): "
              f"{err.read().decode()[:200]} - the local state is unchanged.")


if __name__ == "__main__":
    import sys
    main()
