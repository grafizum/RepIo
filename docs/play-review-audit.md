# Play review audit — RepIo 1.7.6 (version code 59)

Purpose: rule the app in or out as the cause of the production-access denial, so
that if everything here passes, the remaining variable is the application answers
and the installer/engagement numbers.

Method: static audit of this repository on 2026-10-19. Nothing here is a
substitute for the Play Console, which is the only place the tester and install
figures exist.

## Verdict

**No defect in this codebase explains "your app requires more testing".** That
wording is the testing-track decision. The audit found three real release
blockers, but they produce policy rejections, not this email.

The one app-side hypothesis this audit cannot exclude is a crash on the
reviewer's device (see §6), which would explain the email if it happened.

## 1. Packaging and platform

| Check | Evidence | Verdict |
|---|---|---|
| Target API level | `targetSdk = 36` in `composeApp/build.gradle.kts` | PASS (Play requires 35+, 36 from Aug 2026) |
| Minimum API | `minSdk = 24` | PASS (Play rejects bundles below 24 since Sep 2026) |
| Release is signed with the upload key | Gradle throws if a release build is requested without a keystore | PASS |
| R8 / resource shrinking safe | `isMinifyEnabled`, `isShrinkResources`, `proguard-rules.pro` keeps `R$string`/`R$plurals` for the `getString("key")` reflection path | PASS |
| App Check provider in release | `PlayIntegrityAppCheckProviderFactory` when `!BuildConfig.DEBUG`; debug provider only in debug | PASS |
| Cleartext traffic | No `usesCleartextTraffic`, no network security config override | PASS (blocked by default) |
| Debuggable release | Not set anywhere | PASS |
| Backup | `allowBackup=false`, extraction rules present | PASS |
| Icons | 19 adaptive icon XMLs in `mipmap-anydpi-v26` plus `mipmap-night-*` densities | PASS |
| Locales | `locales_config.xml` = en, de, it | PASS |
| Version monotonicity | `version.properties` = 59 / 1.7.6 | PASS (next upload must be 60) |

## 2. Permissions and declarations

Declared in `AndroidManifest.xml`:

| Permission | Assessment |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Normal |
| `POST_NOTIFICATIONS` | Normal; requested at runtime in first-run setup and again from the workout screen on Android 13+ |
| `VIBRATE` | Normal |
| `RECEIVE_BOOT_COMPLETED` | Normal, used to re-arm the skip nudge |
| `SCHEDULE_EXACT_ALARM` | **FAIL / remove.** Not the restricted `USE_EXACT_ALARM`, but Android 14+ denies it by default, a rest timer is not an alarm-clock/calendar core function, and it invites a permissions declaration. `TimerManager` already falls back to `setAndAllowWhileIdle`, so nothing is lost by deleting it. |

No location, camera, microphone, contacts, SMS, storage, accessibility, or
`QUERY_ALL_PACKAGES`. No foreground service.

Declarations that live in Play Console, not in code (see §6).

## 3. Data and privacy

**What leaves the device.** Two paths only:

1. **Support tickets** — `SupportManager` writes `subject`, `message`,
   `device_model`, `app_version`, `timestamp` to Firestore. Behind App Check
   (Play Integrity in release).
2. **Play Core update check** — `AppUpdateChecker`, flexible and dismissible.

Everything else — workout history, plan, levels, settings — is Room/SQLite plus
`SharedPreferences` on the device.

**Verdicts**

| Check | Verdict |
|---|---|
| No ad SDK | PASS — no ads/analytics/crashlytics dependency in the build |
| No analytics profile, no advertising ID | PASS |
| No accounts | PASS — nothing to delete, no account-deletion requirement |
| Data safety must declare the support form | **FAIL** — declaring "no data collected" while this path exists is a mismatch |
| Privacy policy | **PASS** — `site/privacy/index.html` is live at `https://repio-legal.matrix-gian.workers.dev/privacy` (HTTP 200, `text/html`, byte-identical to the repo file) and linked from Settings via `AppLinks.PRIVACY_POLICY`. `grafizum.com/privacy` remains unusable: it scopes itself to the dashboard, not this app. |
| Medical claims | PASS — no "medical", "doctor", "diagnose", "injury", "therapy" strings in the app's copy |

## 4. Content and functionality

| Check | Evidence | Verdict |
|---|---|---|
| Real functionality, not a shell | Onboarding, plan generation, workout player, timers, progression engine, analytics, settings, three locales | PASS |
| No hidden or review-evasion behaviour | The Easter egg is a normal hidden tab unlocked by a gesture and auto-locks; the developer skin defaults to `false` (`staticCompositionLocalOf { false }`) | PASS |
| No user-generated content, no social features | None | PASS |
| No in-app purchases or subscriptions | None | PASS |
| Update flow cannot block review | `AppUpdateChecker` is flexible and dismissible, no-ops offline, no version gate | PASS |
| External links handled by the system | Video URLs open via `Intent.ACTION_VIEW`; no embedded WebView | PASS |

## 5. Review access and first run

- No login, no account, no region lock, no invite code, no server dependency for
  the core flow. `App access` in Console should say "all functionality is
  available without special access".
- First run is a multi-step setup (goal, equipment, days) plus the notification
  permission prompt. A reviewer can finish it; it is worth two lines of
  instructions in the App access field so they know what to pick.
- Launcher entries: 10 `MainActivity*` components — one base activity plus nine
  theme entries, with one theme entry enabled at a time (`MainActivityBeige`). Each
  theme entry is a transient `ThemeLauncherEntry` that starts `MainActivity` and
  finishes. On a dark-mode device the first launch switches the icon to Flame.
  Functional and documented, but non-standard; it is the kind of thing that makes a
  first run look odd, not a policy breach.

## 6. What this audit cannot see

These determine the outcome and none of them are in the repository:

1. **Closed-track installs vs opt-ins.** Opt-in is not install.
2. **Uninstalls during the review week.** A cohort that leaves looks like a
   cohort that never tested.
3. **Whether installs are on the current version** after a mid-test update.
4. **Testing feedback** — if the page is empty, the reviewer has nothing to weigh.
5. **The pre-launch report.** This is the one item that could make the denial an
   app problem: a crash or ANR on the reviewer's device explains "requires more
   testing" completely. There is no Crashlytics in the build, so nothing in this
   repo would have surfaced it.
6. **App content completeness** — health apps declaration, target audience,
   content rating, ads, data safety. Each is answered in Console.

## 7. How to read the result

If §6 items 1–4 are healthy and the pre-launch report is clean, then the app is
not the cause and the remaining variable is the written application: what you
claimed the testers did, and whether that claim holds up against Google's data.

If the pre-launch report shows a crash, or installs are below the opt-in count,
stop here and fix that first — no answer text survives a contradiction with
Google's own numbers.

Fix the three FAILs in §2 and §3 regardless. They are release blockers: they do
not explain this email, but they will stop the app at the production gate.
