#!/usr/bin/env python3
"""Read the task board from the Grafizum Command Center API.

Reads the token from .env (never printed), lists tasks for the project.
Usage:
  python tools/dashboard_tasks.py            # open tasks
  python tools/dashboard_tasks.py --all      # every task with its status
  python tools/dashboard_tasks.py --json     # raw JSON for further processing
"""
from __future__ import annotations

import argparse
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


def fetch_all_tasks(path: str, env: dict[str, str]) -> list[dict]:
    """Read every page of a task list.

    `GET /v1/tasks` PAGINATES: the default page is 50 and the RepIo project
    holds 77. A single unpaginated read returns exactly 50 and silently drops
    the rest - which is how "5 open tasks" looked plausible while a sixth open
    item existed, and how seven tasks filed on the board stayed invisible to a
    session that read only this tool. Always loop until offset >= meta.total.
    """
    separator = "&" if "?" in path else "?"
    offset = 0
    collected: list[dict] = []
    while True:
        page = api_get(f"{path}{separator}limit=50&offset={offset}", env)
        batch = rows(page)
        if not batch:
            break
        collected.extend(batch)
        meta = page.get("meta") if isinstance(page, dict) else None
        total = meta.get("total") if isinstance(meta, dict) else None
        offset += len(batch)
        # A missing meta must not loop forever: stop as soon as a short page
        # comes back, which is the server saying there is nothing more.
        if len(batch) < 50 or (isinstance(total, int) and offset >= total):
            break
    return collected


def api_get(path: str, env: dict[str, str]) -> object:
    base = env.get("GRAFIZUM_DASHBOARD_URL", "https://api.grafizum.com/v1").rstrip("/")
    # .env points GRAFIZUM_DASHBOARD_URL at the events route (.../v1/events), which
    # only makes sense for the version-bump POST. Strip it to get the API root.
    for suffix in ("/events",):
        if base.endswith(suffix):
            base = base[: -len(suffix)]
    token = env.get("GRAFIZUM_DASHBOARD_TOKEN", "")
    if not token:
        sys.exit("GRAFIZUM_DASHBOARD_TOKEN is not set in .env or the environment")
    request = urllib.request.Request(
        f"{base}/{path.lstrip('/')}",
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/json",
            # Cloudflare's bot management (error 1010) rejects urllib's default
            # signature, so send an ordinary browser-like User-Agent.
            "User-Agent": "curl/8.5.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        sys.exit(f"HTTP {exc.code} on {path}: {exc.read().decode('utf-8', 'replace')[:500]}")
    except urllib.error.URLError as exc:
        sys.exit(f"network error on {path}: {exc.reason}")


OPEN_STATES = {"todo", "open", "backlog", "in_progress", "in-progress", "blocked", "planned"}


def rows(payload: object) -> list[dict]:
    if isinstance(payload, list):
        return [r for r in payload if isinstance(r, dict)]
    if isinstance(payload, dict):
        for key in ("tasks", "data", "items", "results"):
            value = payload.get(key)
            if isinstance(value, list):
                return [r for r in value if isinstance(r, dict)]
            if isinstance(value, dict) and isinstance(value.get("tasks"), list):
                return [r for r in value["tasks"] if isinstance(r, dict)]
    return []


def title(task: dict) -> str:
    for key in ("title", "name", "summary"):
        if isinstance(task.get(key), str):
            return task[key]
    return "(untitled)"


def status(task: dict) -> str:
    value = task.get("status")
    if isinstance(value, dict):
        value = value.get("name") or value.get("id") or value.get("state")
    return str(value or "unknown").lower()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--all", action="store_true", help="include finished tasks")
    parser.add_argument("--json", action="store_true", help="print raw JSON")
    parser.add_argument("--project", default="", help="override the project id")
    args = parser.parse_args()

    env = load_env()
    project = args.project or env.get("GRAFIZUM_PROJECT_ID", "")
    path = "tasks"
    if project:
        path = f"tasks?project_id={project}"

    tasks = fetch_all_tasks(path, env)
    if args.json:
        print(json.dumps(tasks, indent=2, ensure_ascii=False))
        return

    if not args.all:
        tasks = [t for t in tasks if status(t) in OPEN_STATES]

    print(f"{len(tasks)} task(s) on the board for project {project or '(all)'}\n")
    for index, task in enumerate(tasks, start=1):
        flags = []
        for key in ("priority", "assignee", "owner", "id"):
            if task.get(key):
                flags.append(f"{key}={task[key]}")
        print(f"{index}. [{status(task)}] {title(task)}")
        if flags:
            print(f"   {' | '.join(flags)}")
        for key in ("notes", "description", "detail", "details", "body"):
            value = task.get(key)
            if isinstance(value, str) and value.strip():
                print("   " + value.strip().replace("\n", "\n   ")[:1200])
                break
        print()


if __name__ == "__main__":
    main()
