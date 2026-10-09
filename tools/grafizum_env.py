#!/usr/bin/env python3
"""Resolve Grafizum dashboard credentials for the *current* project.

A dashboard agent key is scoped to specific projects (``GET /v1/projects`` shows
which). So a token belongs to a PROJECT, not to a machine, and putting one in a
repo's ``.env`` means the next project silently overwrites it. This module keeps
one file per project OUTSIDE any checkout:

    ~/.config/grafizum/projects/<slug>.env

and picks the right one for the checkout you are standing in. The token never
needs to live in a repository (``.env`` stays for non-Grafizum secrets such as
keystore passwords).

How the active project is chosen (first match wins):

  1. ``GRAFIZUM_PROJECT`` in the environment — a slug (``repio``) or a
     ``project_...`` id.
  2. The repo ``.env``'s ``GRAFIZUM_PROJECT_ID``, matched against every registry
     file's own ``GRAFIZUM_PROJECT_ID`` (or its filename slug).
  3. The checkout directory name, against a registry file's slug.

Per-key precedence: process environment > registry file > repo ``.env``.

CLI (for humans, never prints a secret):

    python tools/grafizum_env.py --list     # projects with a registry file
    python tools/grafizum_env.py --which    # active project for this checkout
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REGISTRY_DIR = Path(os.path.expanduser("~/.config/grafizum/projects"))

# Grafizum-owned keys. Anything else in a repo .env (keystore paths and
# passwords) is project-local and must keep coming from the repo.
GRAFIZUM_KEYS = (
    "GRAFIZUM_DASHBOARD_URL",
    "GRAFIZUM_DASHBOARD_TOKEN",
    "GRAFIZUM_PROJECT_ID",
    "GRAFIZUM_PROJECT_SLUG",
    "GRAFIZUM_SYNC_KEY",
)


def _read_dotenv(path: Path) -> dict[str, str]:
    """Parse a simple KEY=VALUE file; quotes stripped, blanks and # skipped."""
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def list_projects() -> list[tuple[str, dict[str, str]]]:
    """Every registry file as (slug, values), sorted by slug."""
    if not REGISTRY_DIR.is_dir():
        return []
    found = []
    for path in sorted(REGISTRY_DIR.glob("*.env")):
        found.append((path.stem, _read_dotenv(path)))
    return found


def _registry_file_for(project: str) -> Path | None:
    """Find the registry file matching a slug or a project_... id."""
    project = project.strip()
    if not project:
        return None
    direct = REGISTRY_DIR / f"{project}.env"
    if direct.exists():
        return direct
    for slug, values in list_projects():
        if values.get("GRAFIZUM_PROJECT_ID", "") == project:
            return REGISTRY_DIR / f"{slug}.env"
    return None


def active_project_slug(root: Path = ROOT) -> str | None:
    """Resolve which project's registry file applies to this checkout."""
    # 1. Explicit override.
    override = os.environ.get("GRAFIZUM_PROJECT", "").strip()
    if override:
        path = _registry_file_for(override)
        return path.stem if path else override
    # 2./3. The repo's own .env id, then the checkout directory name.
    repo_env = _read_dotenv(root / ".env")
    for candidate in (repo_env.get("GRAFIZUM_PROJECT_ID", ""), repo_env.get("GRAFIZUM_PROJECT_SLUG", ""), root.name):
        if not candidate:
            continue
        path = _registry_file_for(candidate)
        if path:
            return path.stem
    return None


def load_env(root: Path = ROOT) -> dict[str, str]:
    """Build the merged environment the dashboard tools expect.

    Order, weakest first: repo .env, then the project's registry file, then the
    real process environment. Later writes win, so a shell override always beats
    a stored token.
    """
    env: dict[str, str] = {}
    env.update(_read_dotenv(root / ".env"))

    slug = active_project_slug(root)
    if slug:
        registry = _read_dotenv(REGISTRY_DIR / f"{slug}.env")
        # Only Grafizum keys come from the registry; never let it shadow a
        # project-local secret such as STORE_PASSWORD.
        for key in GRAFIZUM_KEYS:
            if key in registry:
                env[key] = registry[key]
        env.setdefault("GRAFIZUM_PROJECT_SLUG", slug)

    # The process environment is authoritative for anything it sets.
    for key, value in os.environ.items():
        env[key] = value
    return env


def _main(argv: list[str]) -> int:
    if "--list" in argv:
        projects = list_projects()
        if not projects:
            print(f"no project files under {REGISTRY_DIR}")
            return 0
        print(f"{REGISTRY_DIR}")
        for slug, values in projects:
            pid = values.get("GRAFIZUM_PROJECT_ID", "(no id)")
            has_token = "token" if values.get("GRAFIZUM_DASHBOARD_TOKEN") else "NO TOKEN"
            print(f"  {slug:20} {pid}  {has_token}")
        return 0
    if "--which" in argv:
        slug = active_project_slug()
        if not slug:
            print("no registry file matches this checkout "
                  "(set GRAFIZUM_PROJECT or add ~/.config/grafizum/projects/<slug>.env)")
            return 1
        env = load_env()
        print(f"project: {slug}")
        print(f"id:      {env.get('GRAFIZUM_PROJECT_ID', '(unset)')}")
        print(f"file:    {REGISTRY_DIR / (slug + '.env')}")
        print(f"token:   {'set' if env.get('GRAFIZUM_DASHBOARD_TOKEN') else 'MISSING'}")
        return 0
    print(__doc__)
    return 0


if __name__ == "__main__":
    raise SystemExit(_main(sys.argv[1:]))
