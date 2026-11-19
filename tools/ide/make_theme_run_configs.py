#!/usr/bin/env python3
"""Write one run configuration per themed launcher activity.

The app ships four launcher activities, one per theme, so each can be opened
directly on a phone without first installing, launching and switching the theme
in the app. The plain "RepIo" config launches the app normally; these pin a
specific activity, which is exactly what the old RepIo config was doing to the
beige theme by accident.

Generated from the RepIo config so the two cannot drift.
"""
from __future__ import annotations

import pathlib
import re

RUN_CONFIG_DIR = pathlib.Path(".idea/runConfigurations")
TEMPLATE = RUN_CONFIG_DIR / "RepIo.xml"

# Launcher activity class -> the config name the owner picks from the dropdown.
THEMES = {
    "MainActivityFlame": "RepIo (Flame)",
    "MainActivityBeige": "RepIo (Beige)",
    "MainActivityPurple": "RepIo (Purple)",
    "MainActivityEmerald": "RepIo (Emerald)",
}


def main() -> int:
    template = TEMPLATE.read_text(encoding="utf-8")
    if "ACTIVITY_CLASS" in template:
        print("the template must not pin an activity; refusing to copy it")
        return 1

    # The launch options go just before the closing <method> block, which every
    # Android app config has.
    anchor = '    <method v="2">'
    if anchor not in template:
        print("could not find the <method> anchor in the template")
        return 1

    for activity, name in THEMES.items():
        options = (
            '    <option name="MODE" value="specific_activity" />\n'
            '    <option name="ACTIVITY" value="" />\n'
            f'    <option name="ACTIVITY_CLASS" value="com.grafizum.repio.{activity}" />\n'
        )
        body = template.replace(
            f'name="RepIo" type="AndroidRunConfigurationType"',
            f'name="{name}" type="AndroidRunConfigurationType"',
            1,
        )
        body = body.replace(anchor, options + anchor, 1)
        target = RUN_CONFIG_DIR / f"{name}.xml"
        target.write_text(body, encoding="utf-8")
        print(f"wrote {target} -> com.grafizum.repio.{activity}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
