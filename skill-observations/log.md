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
