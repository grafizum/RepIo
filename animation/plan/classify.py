"""Cluster the 601 catalog exercises into motion archetypes (ordered first-match rules)."""
import json
import re
import collections

rows = json.load(open("/home/matrix/StudioProjects/RepIo/animation/plan/catalog.json"))

# (archetype, regex on lowercase name, optional type filter)
RULES = [
    # ---- static first: holds + stretches are a still/breathing loop whatever their name says ----
    ("hold_static", r".*", "isometric_hold_or_carry|carry_exclude"),  # handled in code below
    ("stretch_static", r".*", "mobility_stretching|flow_exclude"),
    # ---- combos / olympic ----
    ("olympic_lift", r"clean|snatch|jerk|turkish get", None),
    ("thruster_combo", r"thruster|lunge press|lunge and press|bridge press", None),
    # ---- arms ----
    ("wrist_curl", r"wrist curl|wrist roller", None),
    ("curl", r"curl", "not:leg curl|hamstring curl|nordic|jefferson"),
    ("triceps_extension", r"skull crusher|tricep|triceps|overhead extension", "not:stretch|dips"),
    # ---- chest / shoulders ----
    ("rollout", r"rollout", None),
    ("pullover", r"pullover", None),
    ("fly", r"\bfly\b|pec deck|pull apart|y-fly|reverse fly", None),
    ("press_horizontal", r"bench press|floor press|chest press|svend press|spoto press|ez-bar press|incline dumbbell press", None),
    ("push_up", r"push.?up|push-ups|push ups", None),
    ("dip", r"\bdips?\b", None),
    ("press_vertical", r"overhead press|shoulder press|arnold press|push press|landmine press|behind the neck press|bottoms-up press", None),
    ("raise_lateral_front", r"lateral raise|front raise", None),
    ("upright_row", r"upright row|high pull", None),
    ("shrug", r"shrug", None),
    ("face_pull", r"face pull|external rotation", None),
    # ---- back ----
    ("pull_up", r"pull.?ups?\b|chin-ups|muscle.?ups?|muscle-up|rope climb", None),
    ("pulldown", r"pulldown", None),
    ("row_inverted", r"inverted row|ring row|trx row", None),
    ("row_seated", r"seated cable row|rowing machine|sled row", None),
    ("row_chest_supported", r"chest-supported|chest supported|bench pull", None),
    ("row_bent_over", r"\brow\b", None),
    ("back_extension", r"back extension|superman|locust", None),
    # ---- legs ----
    ("calf_raise", r"calf raise", None),
    ("leg_press", r"leg press|hack squat", None),
    ("leg_extension", r"leg extension|terminal knee", None),
    ("leg_curl", r"leg curl|hamstring curl|nordic", None),
    ("hip_thrust_bridge", r"hip thrust|glute bridge|hip bridge|glute drive|tabletop hip|bridge", None),
    ("hip_abduction_adduction", r"abduction|adduction|clamshell|fire hydrant|lateral walk|sumo walk", None),
    ("glute_kickback", r"kickback|side kick|donkey kick", None),
    ("swing", r"swing", None),
    ("hinge", r"deadlift|good morning|rack pull|jefferson|kickstand", None),
    ("pistol_squat", r"pistol", None),
    ("lunge_split_squat", r"lunge|split squat|step ups|step-up|cossack", None),
    ("squat", r"squat|box jump|jump squat", None),
    # ---- core ----
    ("floor_core_dynamic", r"dead bug|bird-dog|mountain climber|bear crawl|downward dog|knee drive|plank", None),
    ("leg_raise", r"leg raise|knee raise|toes to bar|hanging pike|flutter|scissor|leg pull-in|dragon flag|windshield|roll over|knee tuck|ball pike|pilates leg pull", None),
    ("crunch_situp", r"crunch|sit-up|sit ups|v ups|v-sit|cocoons|jackknife", None),
    ("rotation_antirotation", r"russian twist|side bend|windmill|halo|spine twist|saw|pallof", None),
    ("floor_core_dynamic", r"dead bug|bird-dog|mountain climber|bear crawl|downward dog to|knee drive|plank", None),
    # ---- carries / locomotion ----
    ("cardio_locomotion", r"treadmill", None),
    ("carry_walk", r"carry|farmer|suitcase|heel-to-toe|walking|walk\b", None),
    ("cardio_locomotion", r"running|treadmill|high knees|jumping jacks|burpees|jump rope|stair climber|elliptical|bike|battle rope|slam|plyo", None),
    ("yoga_flow", r"cat-cow|flow|pedal|to half|roll down|rock|spine stretch", None),
]

STATIC_TYPES = {"isometric_hold_or_carry", "mobility_stretching"}
CARRY = re.compile(r"carry|farmer|suitcase")
FLOW = re.compile(r"cat-cow|\bflow\b|pedal|to half|roll down|rock|downward dog to|spine stretch|saw|spine twist")


def classify(r):
    n = r["name"].lower()
    if r["type"] == "isometric_hold_or_carry":
        return "carry_walk" if CARRY.search(n) else "hold_static"
    if r["type"] == "mobility_stretching":
        return "yoga_flow" if FLOW.search(n) else "stretch_static"
    for arch, pat, flt in RULES[2:]:
        if flt and flt.startswith("not:") and re.search(flt[4:], n):
            continue
        if re.search(pat, n):
            return arch
    return "UNMATCHED"


out = collections.defaultdict(list)
for r in rows:
    out[classify(r)].append(r)
for k, v in sorted(out.items(), key=lambda kv: -len(kv[1])):
    print(f"{k:28s} {len(v):3d}  " + ", ".join(x["name"] for x in v[:6]) + (" ..." if len(v) > 6 else ""))
print("UNMATCHED:", [x["name"] for x in out.get("UNMATCHED", [])])
json.dump({k: [x["id"] for x in v] for k, v in out.items()},
          open("/home/matrix/StudioProjects/RepIo/animation/plan/archetypes.json", "w"), indent=1)
