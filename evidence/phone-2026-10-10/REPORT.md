# RepIo on-device visual pass — SM-S906B, 2026-10-10

Device: `R3CT30TW4GZ` (SM-S906B, Android 16), 1080x2340 @ 420 dpi override (`wm density`),
theme stored in the app: `DEVELOPER` (`developer_theme_enabled=true`).
Phone is the owner's live device with real data (`is_first_time=false`, 3-day week MON/WED/FRI).

## Install / version

| check | value |
| --- | --- |
| `gradlew :composeApp:installDebug` | `RepIo-v1.7.5-v52-debug.apk` → installed on 1 device |
| `dumpsys package com.grafizum.repio.debug` | `versionName=1.7.5`, `versionCode=52`, `minSdk=24`, `targetSdk=36` |
| `lastUpdateTime` | 2026-10-10 17:54:28 |
| `version.properties` | `VERSION_NAME=1.7.5`, `VERSION_CODE=52` |
| Creator tab (easter-egg) version label, read off the device | `v1.7.5` at `[32,2024][116,2058]` |

So the installed artifact, the build's `BuildConfig` and the tab the owner calls the developer tab
all agree with `version.properties`.

**The running app is this build, byte for byte:** `md5` of
`composeApp/build/outputs/apk/debug/RepIo-v1.7.5-v52-debug.apk` and of the phone's
`/data/app/.../com.grafizum.repio.debug.../base.apk` are both `e78ef6d6bbfe8da2cc07b1a40b85df56`,
and no file under `composeApp/src` is newer than that APK (working tree at `211ca12`,
no source changes in this pass).

## Screens captured (`*.png` + `*.xml` in this directory)

| capture | screen | verdict |
| --- | --- | --- |
| `00/01_home` | Home, Saturday rest day | 15 nodes, 0 overlaps, nothing off-window; "Train 5+ exercises to unlock" under Overall Level `—` |
| `02_my_workout`, `13_state` | My Workout, rest day | rest card centred, no clipping |
| `03_analytics` | Analytics | week metrics, empty-state text, chart frame all inside the window |
| `04/05/06/18_settings_*` | Settings (top → bottom) | every section + theme carousel; 0 overlaps |
| `15_monday` | Routine edit, Monday (9 exercises) | 43 text nodes, 0 overlaps, 1 tap target at 40x48 dp |
| `16_monday_now` | Workout Now, Monday | `Level 4` + stage `I`, `Level lock`, 8 reps @ 27.5 kg, `8 / 8 / 8 / 8 Reps`, NEXT UP |
| `17_levels` | "How levels & stages work" guide | all sections render, `Done` reachable |
| `19_theme_carousel` | theme picker | `Developer Theme` + `Selected` marker, 4 light + 4 dark themes |
| `22_creator_try` | Creator / developer tab | `v1.7.5`, "by Grafizum", theme switch, 5 nav items |

`tools/layout_sweep.py` over `04`, `13`, `15`, `16`, `17`: 0 nodes past the window edge,
0 text outside its own container, 0 fully squeezed nodes (two 5-9 dp roman stage numerals), one
40x48 dp tap target on the routine editor.

## Colours measured from the captures (Developer palette, on device)

| pair | measured |
| --- | --- |
| accent gold on canvas | `#D0B371` on `#131313` = 9.17:1 (Workout), 8.50:1 (guide) |
| body text on card | `#EDEBE8` on `#252525` = 12.88:1 |
| card body on the guide's container | `#E6DABC` on `#343434` = 8.96:1 (predicted 8.95:1) |
| canvas / card / nested | `#131313` / `#1B1B1B` / `#252525` as specified |

Only soft spot found: the Creator tab's `v1.7.5` and `by Grafizum` labels are
`primary.copy(alpha = 0.5f)` → `#716341` on `#131313` = **3.15:1**, under the 4.5:1 AA floor for
text this small (it is decorative, but it is the version string).

## Rest / battery behaviour

- After a rapid tap storm the app rendered **471 frames in 4 s** (~118 fps) and `uiautomator`
  kept answering `ERROR: could not get idle state`, which is what made several dumps unusable.
- Once left alone on My Workout: **0 frames in 6 s** and **0 CPU jiffies in 10 s**. So the app
  does settle; the burst was transient (it followed pager/page transitions), not a steady loop
  on a static screen. The only infinite animation in the app is `EasterEggScreen`'s
  `rememberInfiniteTransition`.

## Interference (important for reading the evidence)

Another process drove this phone during the pass: `am start … com.grafizum.mymusic/.MainActivity`
at 17:59:37 from uid 2000, and MyMusic was in the foreground again at 18:2x and 18:3x. Taps on
RepIo's bottom nav were therefore sometimes dropped and `20_beige_settings.png` /
`21_beige_applied.png` actually show MyMusic, not RepIo.

## Theme round-trip (left as found)

To exercise the theme system I selected `Warm Beige` in Settings → `LIGHT MODE / LIGHT THEME`.
`theme_option` became `BEIGE` and the app raised its own
"Restart Required — To apply the new theme and splash screen fully, the app needs to restart."
dialog with an OK button (verified in `20_beige_home.png`). I wrote `theme_option` back to
`DEVELOPER` with `run-as` while no app process was running and re-launched: the first frame is
byte-for-byte the developer palette again (`#131313` 49.2 %, `#D0B371` 5.0 %), matching
`00_launch.png`.

Also restored on the device: `screen_off_timeout` 2147483647 → 30000,
`stay_on_while_plugged_in` → 0.

## Not exercised

Completing a workout (logging sets, rest timers, recap card), the level-up banner, the
language switcher, the support/update paths, the Creator tab's 3-second auto-lock, and the
light themes' own readability. The Pixel was not attached, so this is one device.
