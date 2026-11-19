"""Hand-written English method descriptions for the v5 catalog.

The v5 progression CSV carries no descriptions; build_progression.py merges
texts from the previous catalog where names reconciled, and these modules
supply the rest. One entry per catalog slug; the part modules are split by
equipment category so coverage gaps are easy to see.

A key that matches no catalog row fails the build (build_progression.py
validates), so a typo surfaces immediately instead of silently leaving one
exercise without a description.
"""

from hand_desc_barbell import DESCRIPTIONS as _BARBELL
from hand_desc_bodyweight import DESCRIPTIONS as _BODYWEIGHT
from hand_desc_cable_bands import DESCRIPTIONS as _CABLE_BANDS
from hand_desc_dumbbell import DESCRIPTIONS as _DUMBBELL
from hand_desc_kettlebell import DESCRIPTIONS as _KETTLEBELL
from hand_desc_machine import DESCRIPTIONS as _MACHINE
from hand_desc_other import DESCRIPTIONS as _OTHER

HAND_DESCRIPTIONS: dict[str, str] = {
    **_OTHER,
    **_BODYWEIGHT,
    **_MACHINE,
    **_KETTLEBELL,
    **_CABLE_BANDS,
    **_BARBELL,
    **_DUMBBELL,
}
