# Skill Observation Log

Observations captured during task-oriented work.

**Status key:** OPEN = not yet actioned | ACTIONED (YYYY-MM-DD) = skill
updated/created | DECLINED (YYYY-MM-DD) = user decided not to pursue —
resolved statuses always carry their resolution date

---
### Observation 1: Intent Visibility in Manifest
**Status:** OPEN
**Date:** 2026-08-09
**Session context:** Fixing email intent functionality.
**Skill:** android-cli
**Type:** open-source
**Phase/Area:** Manifest configuration

**Issue:** Modern Android versions (11+) require <queries> in AndroidManifest.xml to resolve activities for specific intent schemes if they are not common actions.

**Suggested improvement:** Ensure documentation or templates for common actions like 'send email' include the necessary <queries> tag for ACTION_SENDTO.

**Principle:** Always declare intent filter requirements in <queries> when targeting API 30+ to ensure startActivity or resolveActivity works as expected.
n### Observation 2: Dynamic Activity Component Statesn**Status:** OPENn**Date:** 2026-08-13n**Session context:** Debugging "Activity class does not exist" error.n**Skill:** android-clin**Type:** open-sourcen**Phase/Area:** Deployment and Diagnosticsnn**Issue:** When using multiple activity entry points (for themes/icons) and toggling their state via PackageManager.setComponentEnabledSetting, the IDE or ADB may try to launch a disabled activity, resulting in an "Activity class does not exist" error. Clearing app data does not reset these states; only a full uninstall/reinstall or manual pm enable (if permitted) resets them to manifest defaults.nn**Suggested improvement:** When diagnosing "class does not exist" errors for known activities, check the component enabled states via dumpsys package <package_name>.nn**Principle:** Component enabled states are part of the system's package management state and persist beyond app data clearing. Reinstalling is the most reliable way to reset them.
