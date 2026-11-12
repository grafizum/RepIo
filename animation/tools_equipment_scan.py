"""Step 2: derive the visual equipment each catalog exercise needs (name keyword rules)."""
import re, sys, json, collections
CAT = "/home/matrix/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/ExerciseCatalog.kt"
rows = re.findall(r'ExerciseCatalogEntry\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "((?:[^"\\]|\\.)*)"\)', open(CAT).read())

# (asset, regex on name) — first-match groups are NOT exclusive: an exercise collects every asset whose rule hits.
R = [
 # free weights
 ("Dumbbell", r"single-arm hammer curl|dumbbell|\bdb\b|arnold press|goblet|concentration curl|zottman|spider curl(?! \(ez)|rear delt fly|side-lying lateral raise|strict curl|jefferson|^lying tricep extension$|^overhead tricep extension$|^reverse lunge$|bulgarian split squat$|^decline bench press$|single leg romanian|incline hammer curl|cross body hammer"),
 ("Barbell + plates", r"barbell|bench press(?!.*(smith|dumbbell|ez|machine|cable))|deadlift(?!.*(dumbbell|kettlebell|smith|hex|ez|banded))|squat(?!.*(dumbbell|kettlebell|smith|bodyweight|box|pistol|jump|wall|cossack|split|sumo squat \(|goblet|trx|banded|hack|pause))|^pause squat|clean|snatch(?!.*(dumbbell|kettlebell))|jerk(?!.*kettlebell)|push press(?!.*(dumbbell|kettlebell))|thruster|pendlay|rack pull|good morning$|^good morning|floor press$|^floor press|skull crusher$|preacher curl$|^preacher curl|^reverse curl$|cheat curl|drag curl|bench pull$|^bench pull|behind the neck press|spoto|^romanian deadlift|stiff leg|paused (bench|incline|overhead)|close-grip (bench|incline)|wide-grip bench|landmine|t-bar|reverse grip bent over row$|rear delt row \(barbell"),
 ("EZ curl bar", r"ez[- ]bar|ez bar|\(ez-bar\)"),
 ("Hex / trap bar", r"hex bar"),
 ("Weight plate (handheld)", r"plate pinch|plate pullover|^svend press|weighted (dips|pull-up)"),
 ("Kettlebell", r"kettlebell|suitcase carry"),
 ("Medicine ball", r"medicine ball"),
 ("Stability / exercise ball", r"\(ball\)|ball pike|stability ball"),
 ("Ab wheel", r"ab wheel"),
 # benches & racks
 ("Flat bench", r"bench press(?!.*(incline|decline|smith))|bench pull|bench dips|bench leg pull|^bench |dumbbell fly$|^dumbbell fly|pullover|skull crusher|lying tricep|floor press(?!.*)|bulgarian|chest-supported|single-arm dumbbell row|side-lying lateral|dumbbell hip thrust|barbell hip thrust|kettlebell hip thrust|step ups|close-grip dumbbell bench|seated dumbbell|seated barbell|spoto|wide-grip bench|paused bench|close-grip bench|ez-bar bench press|^bench press|box squat"),
 ("Adjustable bench (incline/decline)", r"incline(?!.*(push-up|treadmill))|decline(?!.*(push-up|crunch))|spider curl|chest-supported|rear delt row|seated dumbbell"),
 ("Decline sit-up bench", r"decline crunch"),
 ("Preacher bench", r"preacher|spider curl"),
 ("Squat / power rack", r"back squat|front squat$|^front squat|overhead squat|pause squat|heel-elevated squat|sumo squat$|rack pull|bench press(?!.*(dumbbell|smith|ez|close-grip dumbbell))|^paused bench|^paused incline|barbell lunge|good morning$|^good morning|barbell overhead press|^paused overhead|behind the neck press|push press$|^push press|push jerk|split jerk|barbell calf raise|barbell shrug|behind the back barbell shrug"),
 ("Landmine pivot", r"landmine|t-bar row"),
 ("Plyo box", r"box jump|box squat|step ups"),
 # cable
 ("Cable station (adjustable pulley)", r"cable|pushdown|pallof|face pull(?!.*(dumbbell|ring|trx))|straight-arm pulldown"),
 ("Lat pulldown machine", r"lat pulldown"),
 ("Seated cable row station", r"seated cable row"),
 ("Cable attachments (straight bar / V-bar / rope / D-handle / ankle strap)", r"cable|pushdown|pulldown|pallof|face pull(?!.*(dumbbell|ring|trx))"),
 # machines
 ("Smith machine", r"smith"),
 ("Leg press (45°)", r"^(?!horizontal).*leg press"),
 ("Horizontal leg press", r"horizontal leg press"),
 ("Hack squat machine", r"hack squat"),
 ("Leg extension machine", r"leg extension"),
 ("Lying leg curl machine", r"lying leg curl"),
 ("Seated leg curl machine", r"seated leg curl"),
 ("Hip adduction/abduction machine", r"^hip adduction|machine hip abduction"),
 ("Chest press machine", r"machine chest press"),
 ("Pec deck / chest fly machine", r"pec deck|machine chest fly"),
 ("Shoulder press machine", r"^machine shoulder press|single-arm machine shoulder press"),
 ("Biceps curl machine", r"machine bicep curl|machine preacher curl|preacher hammer curl"),
 ("Triceps extension machine", r"machine triceps extension"),
 ("Calf raise machines (standing / seated / donkey)", r"machine calf raise|seated calf raise|standing calf raise|donkey calf raise|hack squat calf"),
 ("Assisted pull-up / dip machine", r"^assisted pull ups$|machine assisted dips"),
 ("Back extension bench (45° / roman chair)", r"^back extension"),
 ("Machine back extension", r"machine back extension"),
 ("Ab crunch machine", r"machine seated crunch"),
 ("Plate-loaded machines (glute drive / lateral raise / shrug)", r"plate-loaded (glute|lateral|shrug)|single-arm plate-loaded"),
 ("Captain's chair (knee-raise station)", r"captain's chair"),
 ("Sled", r"\bsled\b"),
 # bodyweight stations
 ("Pull-up bar", r"^(?!assisted pull ups$)(?!.*band).*pull[- ]?ups?\b|chin-ups|hanging|toes to bar|muscle ups$|dead hang$|^dead hang|front lever|back lever|scapular pull|straight bar dips|weighted pull-up|band-assisted|assisted pull ups \(band"),
 ("Dip bars / parallel bars", r"^chest dips|weighted dips|^l sit|planche$|straight bar dips"),
 ("Gymnastic rings", r"\bring\b|rings"),
 ("Suspension trainer (TRX)", r"trx"),
 ("Climbing rope", r"rope climb"),
 ("Vertical pole (human flag)", r"human flag"),
 ("Resistance band (long loop)", r"\bband\b|banded|band-assisted|\(band\)"),
 ("Mini band (short loop)", r"banded (clamshell|fire hydrant|lateral walk|sumo walk|glute bridge|squat)|seated hip abduction \(band"),
 ("Wall", r"\bwall\b|legs-up-the-wall|doorway"),
 ("Battle ropes", r"battle rope"),
 ("Jump rope", r"jump rope"),
 ("Wrist roller", r"wrist roller"),
 ("Yoga mat", r"pose|yoga|pilates|stretch|child's|cat-cow|downward|warrior|dog|lunge to|sphinx|plank|crunch|sit-up|bridge|dead bug|bird|superman|flutter|scissor|v ups|v-sit|hollow|russian twist|leg raise|clamshell|kick|windshield|reverse tabletop|cocoons"),
 # cardio
 ("Air bike", r"air bike"), ("Elliptical trainer", r"elliptical"), ("Treadmill", r"treadmill"),
 ("Stair climber", r"stair climber"), ("Stationary bike", r"stationary bike"), ("Rowing machine", r"rowing machine"),
]
uses = collections.defaultdict(list); none = []
EXCL = {"Barbell + plates": r"kettlebell|dumbbell|band|smith|ez|hex|machine|ball|trx|bodyweight|landmine|t-bar|cable|plate|ring|pause pull|jump|box",
        "Squat / power rack": r"band|smith|ez"}
EXTRA = {"Inverted Row": ["Barbell + plates", "Squat / power rack"], "Dragon Flag": ["Flat bench"],
         "Nordic Hamstring Curl": ["Flat bench"], "Reverse Nordic Curl": ["Yoga mat"], "Decline Push-Up": ["Flat bench"],
         "Incline Push-Up": ["Flat bench"], "Crab Dips": ["Yoga mat"], "Bodyweight Calf Raise": ["Plyo box"],
         "Single Leg Calf Raise": ["Plyo box"], "Deficit Push Ups": ["Weight plate (handheld)"], "Deficit Deadlift": ["Weight plate (handheld)"],
         "Heel-Elevated Squat": ["Weight plate (handheld)"], "Weighted Dips": ["Dip belt"], "Weighted Pull-Up": ["Dip belt"]}
for (id_, name, muscle, cat, typ, lvl, desc) in rows:
    n = name.lower(); hit = False
    for asset, rx in R:
        if re.search(rx, n) and not (asset in EXCL and re.search(EXCL[asset], n)) and not (asset in ("Barbell + plates", "Squat / power rack") and cat != "Barbell"): uses[asset].append(name); hit = True
    for asset in EXTRA.get(name, []): uses[asset].append(name); hit = True
    if not hit: none.append(name)
json.dump({"assets": uses, "bodyweight_only": none}, open(sys.argv[1], "w"), indent=1)
for asset in [a for a, _ in R] + ["Dip belt"]:
    u = uses.get(asset, [])
    if u: print(f"{asset} [{len(u)}]: {', '.join(u[:8])}{' …' if len(u) > 8 else ''}")
print(f"\nNO EQUIPMENT [{len(none)}]: {', '.join(none)}")
