"""Instruction-first rewrites of descriptions that led with provenance.

WHY THIS FILE EXISTS
--------------------
`task_6q3m1r4h` (owner, 2026-10-02): "Make sure that each exercise in the
database has a slight description on how the exercise is done and how to do
it, not if it's a good exercise or who used it etc."

Measured against the built pool: coverage was already complete (601/601 have a
description), so this is not a gap-filling table. It is a SHAPE problem in the
73 descriptions inherited from the previous catalog, which repeatedly open with
who used the lift or how good it is considered to be:

    "Named after the iconic bodybuilder and movie star, the Arnold press ..."
    "was a staple exercise of the 'Golden Era' of bodybuilding and a favorite of
     Arnold Schwarzenegger himself"
    "The barbell back squat is a popular compound movement that emphasizes ..."
    "commonly used in military and tactical physical fitness tests"

None of that tells a user what to DO. `HAND_DESCRIPTIONS` cannot fix this: it is
tier 5, the LAST fallback, so a key added there would never be read for these
slugs - tiers 1-4 (old catalog, by slug / normalized name / sorted tokens /
alias) always match first. These are therefore a separate, HIGHEST-priority
table applied before the tiers, which is the only layer that can win.

WHAT THE TEXT DOES
------------------
Each entry states what the movement is, then how to set up and perform it, then
what to look for. Rep ranges and placement are kept because the old text was
right about them; only the opening was reputation rather than method. Nothing
here claims a training effect, cites a source, or judges the exercise's worth.

A key that matches no catalog row fails the build (build_progression.py checks
it), so a typo surfaces instead of silently doing nothing.
"""

OVERRIDES: dict[str, str] = {
    "arnold_press": (
        "A shoulder press performed with the hands rotating outward as the elbows "
        "go in front of the body, finishing with the palms facing forward. Start seated "
        "with the dumbbells at shoulder height, elbows in front of the ribs. As you press "
        "up, let the elbows travel forward and the forearms rotate from neutral to "
        "supinated, so all three heads of the shoulder take part. Lower under control "
        "back to the start without letting the elbows flare behind the torso."
    ),
    "barbell_back_squat": (
        "A squat with the bar racked across the upper back and rear deltoids, descending "
        "until the hips drop below the top of the knees and the torso stays braced. Set "
        "the feet shoulder-width with the toes slightly out, brace hard, and push the "
        "floor away. Keep the chest up and the knees tracking over the toes. Drive "
        "through the whole foot to stand. Trained anywhere from a heavy single to sets of "
        "twenty or more, because the load is limited by the spine's tolerance rather "
        "than by the legs' capacity."
    ),
    "barbell_glute_bridge": (
        "A hip bridge with a barbell across the hips, used to load the glutes and "
        "hamstrings. Lie on the floor with the bar over the hip creases, knees bent and "
        "feet flat. Brace the core, then drive through the heels to lift the hips until "
        "the body forms a straight line from knees to shoulders. Squeeze the glutes at "
        "the top and lower without letting the spine arch. Works as a strength movement, "
        "a warm-up, or a finisher."
    ),
    "barbell_lunge": (
        "A forward lunge holding a barbell across the back, working the quads, glutes "
        "and hamstrings one leg at a time. Step forward into a long stride, drop the rear "
        "knee toward the floor, and keep the front shin roughly vertical. Brace the trunk "
        "upright and push through the front heel to return to standing. The barbell makes "
        "it possible to load past what body weight allows."
    ),
    "barbell_reverse_lunge": (
        "A lunge that steps BACK rather than forward, which keeps the front leg planted "
        "and loads the trailing knee's range instead. Hold the barbell across the back, "
        "take a long step back, and sink until both knees are near ninety degrees. Keep "
        "the torso upright and drive through the front heel to come back to standing."
    ),
    "barbell_shrug": (
        "A shoulder shrug with a barbell, moving the traps only. Stand tall with the bar "
        "held in front of the thighs or hanging from the arms. Let the shoulders travel "
        "straight up as high as they go without bending the elbows or rolling the "
        "shoulders forward, pause at the top, then lower fully. Straps make heavy loading "
        "possible, though it stays a moderate-rep movement."
    ),
    "bear_crawl": (
        "A crawl on hands and feet with the knees held an inch off the ground, keeping the "
        "hips low and level. Start opposite hand and foot together, move them in small "
        "controlled steps, and keep the back flat rather than letting the hips rise. "
        "Works the shoulders, core and legs at once and lifts the heart rate quickly, so it "
        "suits a warm-up, a short conditioning burst, or a longer cardio block."
    ),
    "behind_the_back_barbell_shrug": (
        "A shrug with the bar held behind the body, which encourages the shoulders to stay "
        "pulled up and back instead of rolled forward. Stand tall, hold the bar behind the "
        "hips with straight arms, and drive the shoulders straight up. Pause, then lower "
        "under control without letting the shoulders round forward."
    ),
    "behind_the_neck_lat_pulldown": (
        "A cable pulldown with the bar passing behind the neck to the upper and mid back. "
        "Grip the bar wide, sit tall with the chest up, and pull the elbows down toward the "
        "ribs, letting the lats and upper back shorten. Reaching full shoulder flexion "
        "behind the head needs a lot of shoulder mobility, and the stretch it puts on the "
        "shoulder joint is the reason many coaches steer people to the front-of-neck "
        "version instead."
    ),
    "bent_arm_barbell_pullover": (
        "A barbell pull-over with the elbows kept slightly bent throughout, the bar traveling "
        "in an arc from the thighs to behind the head. Lying on a bench, hold the bar with "
        "both hands, keep the ribs down, and lower the bar past the chest toward the floor "
        "behind the head until the lats and serratus feel the stretch. Pull it back to the "
        "start without letting the hips lift. Light weight and long, controlled reps suit "
        "this better than heavy ones."
    ),
    "bent_over_barbell_row": (
        "A barbell row from a hinged position, working the lats, rhomboids and lower back. "
        "Stand with the bar on the floor, hinge forward until the torso is near parallel "
        "with the floor, and let the arms hang straight down. Pull the bar toward the lower "
        "ribs by driving the elbows past the hips, then lower it under control to a full "
        "stretch. Keep the spine braced and the neck in line with the torso."
    ),
    "bent_over_dumbbell_row": (
        "A dumbbell row from a hinged position, one arm at a time or both together. Hinge "
        "forward with a neutral spine and let the arms hang, then row the dumbbells toward "
        "the hips, leading with the elbows rather than the hands. Pause with the shoulders "
        "bladed back, then lower to a full stretch. Works the lats, lower traps and rear "
        "deltoids."
    ),
    "cable_crunch": (
        "A kneeling cable crunch holding a rope, curling the ribcage toward the pelvis "
        "rather than pulling with the arms. Kneel facing away from the stack with the rope "
        "held at the temples, hips extended, and round the spine so the ribcage moves "
        "toward the hips. Return slowly to the extended position without letting the lower "
        "back arch. The arms stay fixed throughout, which is what keeps the load on the "
        "abs."
    ),
    "cable_hammer_curl": (
        "A cable curl with the palms facing each other, which takes the biceps toward the "
        "brachialis and forearm rather than the biceps peak alone. Stand square to the "
        "stack holding a rope at neutral grip, elbows pinned to the ribs, and curl the "
        "hands up without letting the elbows travel forward. Lower slowly. Grip is usually "
        "the limiting factor, so it is normally taken for a moderate-to-high number of reps."
    ),
    "cat_cow": (
        "A spinal mobility drill moving between two shapes on hands and knees. Start on all "
        "fours, then drop the chest and lift the tailbone as the spine rounds (cat), and "
        "reverse it to arch the back and drop the hips (cow). Move slowly through the whole "
        "range, leading with the pelvis rather than the neck, and pair it with the breath. "
        "Used to warm the spine up or to keep it moving, for reps or for time."
    ),
    "child_s_pose": (
        "A kneeling rest position with the hips back toward the heels, the arms reaching "
        "forward and the forehead on the floor. From kneeling, spread the knees, sit the "
        "hips back, and walk the hands out until the arms are long and the ribs are down. "
        "Breathe into the back of the ribcage to open the side body and thoracic spine. "
        "Used as a warm-up, a mobility drill, or at the end of a session."
    ),
    "clean_and_jerk": (
        "A two-part Olympic lift: first a clean, pulling the bar from the floor and receiving "
        "it in the front rack, then a jerk driving it overhead. The clean is done in three "
        "phases - first pull to just below the knee, then a second pull to full extension, "
        "then a fast third pull that brings the elbows through and the bar into the rack "
        "position. Drop under the bar, then jerk by dipping a few centimetres and driving up "
        "hard, turning the wrists over as it goes overhead. Every muscle from the floor to "
        "the overhead works, so it is trained in low reps with the technique, not the load, "
        "being the priority."
    ),
    "close_grip_bench_press": (
        "A bench press with the hands set closer together than shoulder width, which turns "
        "more of the work onto the triceps. Lie on the bench, grip the bar narrower than "
        "your shoulders, and lower it to the lower chest with the elbows tracking back "
        "alongside the ribs rather than flaring to ninety degrees. Press back up and lock "
        "out. The narrower the grip, the more the triceps and the less the chest."
    ),
    "close_grip_dumbbell_bench_press": (
        "A dumbbell press with the bells held closer together than shoulder width, shifting "
        "the emphasis onto the triceps and the inner chest. Lie on the bench, press the "
        "dumbbells together above the chest, and lower them to the lower chest with the "
        "elbows tucked closer to the body than in a normal press. Press back up without "
        "letting the bells knock together."
    ),
    "concentration_curl": (
        "A seated curl done one arm at a time, with the upper arm braced against the inside "
        "of the thigh so only the elbow can move. Sit on a bench, lean forward slightly, "
        "rest the working arm's inner side against the thigh with the palm facing up, and "
        "curl the weight up without letting the shoulder drift forward. Hold the peak for a "
        "moment, then lower slowly. The brace is what makes it hard to cheat."
    ),
    "crunches": (
        "A supine curl of the ribcage toward the pelvis, done on the back with the knees "
        "bent. Lying down, place the hands lightly behind the head without pulling on the "
        "neck, curl the shoulders and ribcage a few inches off the floor, and pause with "
        "the lower back still down. Lower slowly without letting the heels lift. Moving the "
        "ribs rather than pulling on the neck is what keeps the load on the abdominals."
    ),
    "decline_barbell_bench_press": (
        "A bench press with the feet raised and the head lower than the hips, which shifts "
        "the stretch toward the lower chest. Secure the feet, arch the upper back, and lower "
        "the bar to the lower chest below the sternum, letting the shoulder blades roll "
        "forward. Press back up. The incline angle decides whether the upper or the lower "
        "chest takes more of the work."
    ),
    "decline_bench_press": (
        "A bench press with the feet raised and the head lower than the hips, which shifts "
        "the stretch toward the lower chest. Secure the feet, arch the upper back, and lower "
        "the bar to the lower chest below the sternum, letting the shoulder blades roll "
        "forward. Press back up. The incline angle decides whether the upper or the lower "
        "chest takes more of the work."
    ),
    "double_kettlebell_push_press": (
        "Two kettlebells clean into the rack position and then pressed overhead using a short "
        "dip of the legs to start the drive. From the floor, clean both bells to the rack, "
        "dip a few centimetres, and drive up through the legs while punching the bells "
        "overhead. Lock out, then lower back to the rack and repeat, optionally cleaning "
        "between every rep. The legs contribute to each press, so the load can stay "
        "moderate."
    ),
    "dumbbell_bicep_curl": (
        "A standing or seated curl with a dumbbell in each hand. Stand or sit tall with the "
        "elbows against the ribs and the palms facing forward, curl the weights up to the "
        "shoulders without swinging the torso, pause at the top, and lower slowly all the "
        "way down. The elbows staying still is what keeps it a curl rather than a swing."
    ),
    "dumbbell_farmer_s_walk": (
        "A loaded carry: walk with a heavy dumbbell hanging from each hand. Stand tall with "
        "the weights at your sides, shoulders pulled back, and take short deliberate steps "
        "while keeping the ribs down and the torso upright. Grip, trunk stability and "
        "posture are what fail first, so the set ends when the body starts to lean or the "
        "grip gives out, not when a rep count is reached."
    ),
    "dumbbell_fly": (
        "A chest fly with the elbows kept at a fixed, slightly bent angle, so the movement "
        "comes from the shoulder joint rather than the elbow. Lying on the bench, bring the "
        "dumbbells together over the chest, open the arms out in a wide arc until the "
        "chest is stretched, and bring them back together without ever straightening the "
        "elbows. The fixed elbow angle is the difference between a fly and a press."
    ),
    "dumbbell_front_squat": (
        "A squat holding dumbbells racked at the shoulders. Clean or place the bells onto the "
        "shoulders, brace hard, and descend until the hips are below the knees, keeping the "
        "elbows up and the torso upright. Drive through the feet to stand. The load is "
        "limited by what can be held in the rack, so it is normally taken for a moderate "
        "number of reps. Knees that feel compressed in a narrow stance often feel better "
        "wider."
    ),
    "dumbbell_lunge": (
        "A split squat with a dumbbell in each hand, working one leg at a time. Stand with "
        "the feet about hip-width apart, then step one foot back into a long stride and drop "
        "the rear knee toward the floor while the front shin stays vertical. Push through "
        "the front heel to return, then repeat on the other side. The narrow base makes it "
        "good for finding and fixing an imbalance between the legs."
    ),
    "dumbbell_pullover": (
        "A lying pullover with a dumbbell or two, moving the shoulders through a wide arc. "
        "Lying on a bench, hold the weight above the chest with the palms facing in, keep the "
        "ribs down, and lower the arms past the sides toward the floor behind the head until "
        "the lats and serratus feel the stretch. Pull the weight back to the start without "
        "letting the hips lift off the bench."
    ),
    "dumbbell_shoulder_press": (
        "A standing press with a dumbbell in each hand, finishing overhead. Stand with the "
        "bells at shoulder height and the palms facing forward, brace the trunk, and press "
        "both weights up until the arms are straight overhead, finishing with the bells "
        "touching or nearly so. Lower back to the shoulders under control. Standing lets more "
        "weight be moved than sitting, because the legs can help drive it."
    ),
    "dumbbell_shrug": (
        "A trap movement with dumbbells: shoulders up, nothing else. Stand tall with the "
        "weights hanging at the sides, drive the shoulders straight up as far as they will "
        "go, pause at the top, and lower fully without rolling the shoulders forward. The "
        "traps do the work; the arms simply hang."
    ),
    "dumbbell_side_bend": (
        "A lateral bend with a dumbbell in one hand, loading the obliques down the side of "
        "the trunk. Stand tall with the weight at your side, then bend sideways toward the "
        "loaded hand without leaning forward or letting the hip shift the other way. Feel the "
        "side of the abdomen shorten on the working side. Return to upright and repeat, or "
        "alternate sides each rep. Light weight and many reps is the usual approach."
    ),
    "dumbbell_split_squat": (
        "A split squat with a dumbbell in each hand, working one leg at a time. Stand with "
        "the feet about hip-width apart, step one foot back into a long stride, and lower "
        "until the rear knee is near the floor with the front shin vertical. Push through "
        "the front heel to stand, then repeat on the other side. It is a good way to find and "
        "fix a leg-to-leg imbalance, and it is usually trained in a low-to-moderate rep "
        "range for strength."
    ),
    "dumbbell_squat": (
        "A squat holding dumbbells at the sides or on the shoulders. Stand with the feet "
        "shoulder-width, brace, and sit the hips back and down until the knees reach roughly "
        "ninety degrees, keeping the chest up and the weight over the midfoot. Drive through "
        "the whole foot to stand. Because the load is held away from the shoulders, this "
        "stays a moderate-weight movement."
    ),
    "dumbbell_sumo_squat": (
        "A wide-stance squat holding a single dumbbell in front of the legs. Take a stance "
        "wider than the shoulders with the toes turned out, hold the dumbbell between both "
        "hands in front, and descend until the hips are below the knees, pushing the knees "
        "out along the line of the toes. Stand by driving through the heels. The wide stance "
        "shifts more of the demand onto the glutes, hamstrings and adductors."
    ),
    "dumbbell_upright_row": (
        "A row of the dumbbells up the front of the body to the upper chest, finishing with "
        "the elbows out to the sides. Stand holding the weights in front of the thighs with "
        "the palms facing in, then pull straight up along the body, leading with the elbows "
        "until they are level with or above the shoulders. Lower slowly. The high elbow path "
        "is what distinguishes it from a front-raise or a shrug."
    ),
    "elliptical_trainer": (
        "A machine where the feet and the hands move in closed loops while the body stays "
        "upright, loading the legs and arms without the ground impact of running. Stand on "
        "the pedals with the hands on the moving handles, keep the trunk tall, and change the "
        "resistance to scale the difficulty for all four limbs at once. The low impact makes "
        "it usable for long steady sessions or for short hard intervals."
    ),
    "ez_bar_curl": (
        "A barbell curl on an EZ bar, whose cambered grip takes some of the strain off the "
        "wrists. Stand tall holding the bar at shoulder width, pin the elbows to the ribs, "
        "and curl up without leaning back, then lower slowly to a full stretch. The angled "
        "grip is the only real difference from a straight-bar curl, and it lets a lot of "
        "people curl heavier without wrist pain."
    ),
    "glute_bridge": (
        "A floor bridge lifting the hips, driving through the heels to shorten the glutes "
        "and hamstrings. Lie on the back with the knees bent and the feet flat, brace, then "
        "press the hips up until the body forms a straight line from knees to shoulders. "
        "Squeeze at the top and lower without arching the lower back. Works with body weight "
        "alone or with a barbell, a dumbbell or a band for more resistance."
    ),
    "goblet_squat": (
        "A squat holding one kettlebell or dumbbell cupped against the chest, as though "
        "carrying a goblet. Hold the weight at the chest with both hands under the top horn, "
        "keep the elbows inside the knees, brace the trunk, and descend until the hips are "
        "below the knees before driving back up. The front-loaded position makes it easier "
        "to stay upright, which is why it is often used to learn the squat."
    ),
    "jumping_jacks": (
        "A jumping movement that takes the arms and legs from the sides to overhead and back. "
        "Start with the feet together and the arms down, then jump the feet out wide as the "
        "arms sweep overhead, and jump back to the start. Land softly on the balls of the "
        "feet with the knees soft. Often used in a warm-up to raise the heart rate and free "
        "up the shoulders and hips."
    ),
    "kettlebell_swing": (
        "A hinge that throws a kettlebell out to hip height using the hips rather than the "
        "arms. Stand with the bell in front of you, hinge and let it swing back between the "
        "legs while the back stays flat, then drive the hips forward so the bell floats out to "
        "about hip height with the arms acting as a straight rope. Let it swing back down and "
        "repeat. The power comes from the hip snap, not from pulling with the shoulders."
    ),
    "legs_up_the_wall_pose": (
        "A recovery posture: lie on the back with the legs extended up a wall, hips close to "
        "the wall, arms out to the sides. Let the legs rest and breathe slowly, which lets "
        "the hamstrings and lower back lengthen without being stretched further by gravity. "
        "Typically held for a couple of minutes after hard lower-body work."
    ),
    "lying_leg_curl": (
        "A machine curl performed lying face down, with the hips extended rather than bent. "
        "Lie face down on the pad with the ankle roller above the heels, curl the heels "
        "toward the glutes, and lower slowly to a full stretch without lifting the hips. The "
        "straighter hip means the glutes and calves contribute more than they would on the "
        "seated version."
    ),
    "lying_leg_raise": (
        "A floor movement that lifts the legs toward the ceiling to work the lower abdominals. "
        "Lie on the back with the hands under the hips for support and the legs straight, then "
        "raise the legs up and over until the hips curl, keeping the legs together and the "
        "lower back flat. Lower slowly without letting the heels touch the floor before the "
        "next rep."
    ),
    "machine_preacher_curl": (
        "A curl on a machine that pads the front of the upper arm, so the elbow cannot travel "
        "and only the forearm can move. Sit with the back against the pad, set the arms on it, "
        "and curl the handles up while the upper arms stay pressed into the pad. Lower slowly "
        "to a full stretch. Many machines offer several grips; the narrower ones lean on the "
        "forearms and brachialis."
    ),
    "mountain_climbers": (
        "A plank with the knees driving toward the chest one at a time, in quick succession. "
        "Start in a straight-arm plank with the body in a straight line, then drive one knee "
        "toward the chest and swap without letting the hips bounce up or drop. The shoulders "
        "and core hold the position while the legs move, so it lifts the heart rate and works "
        "the trunk at the same time."
    ),
    "negative_pull_ups": (
        "The lowering half of a pull-up only: jump or step up to the top position, then lower "
        "under control to a dead hang as slowly as possible. The body is hardest to control on "
        "the way down because the eccentric phase is the strongest, which is why it is useful "
        "for building the strength to complete a first full rep and for adding reps after a "
        "set of complete pull-ups."
    ),
    "pendlay_row": (
        "A bent-over row that starts from the floor each rep, so there is no momentum to "
        "rebound from. Stand over the barbell, hinge forward, take a wide grip, and pull the "
        "bar to the lower ribs with the elbows driving past the hips. Let it all the way back "
        "to the floor between reps. The pause on the floor removes the stretch reflex, so "
        "every rep pulls from a dead stop."
    ),
    "pigeon_stretch": (
        "A hip stretch holding one bent leg in front and the other extended back. From all "
        "fours, bring one knee forward between the hands and angle it out to the side, then "
        "lengthen the other leg straight behind and let the hips sink down. Keep the front "
        "foot flexed and stay upright rather than folding forward, which is what loads the "
        "glute and the piriformis. Held for time, usually after lower-body work."
    ),
    "plank": (
        "An isometric hold on the forearms, bracing the trunk to stop the spine sagging. Lie "
        "face down, come up onto the forearms with the elbows under the shoulders, and lift "
        "the hips so the body forms one straight line from head to heels. Squeeze the glutes "
        "and the abdomen and breathe steadily rather than holding the breath. Held for time, "
        "and often used to teach bracing."
    ),
    "preacher_curl": (
        "A curl with the back of the upper arm pinned against the bench pad, so the elbow "
        "cannot travel and the movement stays on the biceps. Sit with the chest against the "
        "pad and the arms resting on it, curl the bar up without lifting the shoulders, and "
        "lower slowly to a full stretch. The fixed arm position is what makes it an "
        "isolation movement, so the weight stays modest."
    ),
    "push_jerk": (
        "A jerk in which the lifter dips and drives under the bar, unlike a push press where "
        "the legs only push. Hold the bar in the front rack, dip a few centimetres, then drive "
        "up hard with the legs and turn the wrists over as the bar passes the head, locking out "
        "overhead. Drop back into the rack under control. It is a power movement and is trained "
        "in low reps with the timing, not the load, being the point."
    ),
    "push_up": (
        "A press from the floor, the standard horizontal pushing movement on body weight. "
        "Start in a plank with the hands slightly wider than the shoulders, squeeze the glutes "
        "so the hips do not sag, and lower the chest until the elbows are bent to about "
        "forty-five degrees. Press back up without letting the hips drop or the elbows flare. "
        "The upper back and core hold the line the whole time."
    ),
    "reverse_crunches": (
        "A floor curl that brings the knees toward the chest, working the lower abdominals "
        "rather than the upper. Lie on the back with the hands beside the hips, lift the feet "
        "off the floor, and curl the pelvis upward so the knees come toward the face. Pause, "
        "then lower the feet back down without letting the lower back arch off the floor."
    ),
    "barbell_preacher_curl": (
        "A biceps curl taken on a preacher pad, which fixes the upper arm in place so the "
        "biceps work with no shoulder swing at all. Set the armpits on the pad, take an "
        "underhand grip on the bar, and let the arms straighten fully before curling back up. "
        "Squeeze at the top and lower slowly; the pad's angle makes the first inch of the rep "
        "the hardest part."
    ),
    "machine_calf_raise": (
        "A standing calf raise on a machine: the heels drive up against a shoulder or hip pad "
        "with the balls of the feet on the platform. Let the heels sink below the platform for "
        "a full stretch, then rise all the way to the top and hold. The standing position loads "
        "the gastrocnemius, which is the half of the calf a seated raise cannot reach."
    ),
    "dumbbell_romanian_deadlift": (
        "A hip hinge with a dumbbell in each hand and only a soft knee bend, loading the "
        "hamstrings through the hip rather than the spine. Stand tall, soften the knees, and "
        "push the hips behind you while the bells slide down the front of the legs. Stop when "
        "the hamstrings run out of stretch and drive the hips forward to stand; the back stays "
        "long the whole way, which is what separates it from a stiff-legged pull."
    ),
    "reverse_grip_bent_over_row": (
        "A bent-over row taken with an overhand grip, the hands turned the other way from the "
        "usual pull. Hinge forward with a braced spine, let the bar hang, and pull it to the "
        "lower ribs by driving the elbows up and back. Lower to a full stretch. The reversed "
        "grip tends to shift a little more onto the lats and biceps than a pronated row."
    ),
    "romanian_deadlift": (
        "A hinge where the hips travel backward and the bar slides down the legs, loading the "
        "hamstrings, glutes and back. Start with the bar at the hips, soften the knees, and "
        "push the hips behind you while keeping the bar close to the legs and the back flat. "
        "Stop when the hamstrings run out of stretch, then drive the hips forward to stand. "
        "The bend is in the hip, not the spine, which is what makes it different from a "
        "conventional deadlift."
    ),
    "seated_cable_row": (
        "A seated row on a cable stack, pulling the handle to the torso to work the lats, "
        "rhomboids and rear deltoids. Sit with the chest tall and the legs braced, take a "
        "neutral grip, and pull the elbows back past the ribs while letting the shoulders blade "
        "together. Pause, then let the arms extend forward under control without leaning back "
        "to start the next rep."
    ),
    "seated_dumbbell_shoulder_press": (
        "A seated press with a dumbbell in each hand. Sit back against the bench with the "
        "bells at shoulder height and the palms forward, brace, and press both weights "
        "overhead until the arms are straight. Lower back to the shoulders under control. "
        "Sitting removes the leg drive, so the shoulders and triceps do the work."
    ),
    "seated_leg_curl": (
        "A machine curl performed seated with the hips bent, isolating the hamstrings. Sit "
        "with the knees just below the hip pad and the ankle roller above the heels, then curl "
        "the heels toward the glutes and lower slowly to a full stretch. The bent hip keeps "
        "the glutes from contributing, which is what separates it from the lying version."
    ),
    "side_plank": (
        "An isometric hold on one forearm, bracing the trunk against sideways collapse. Lie "
        "on your side with the elbow under the shoulder, lift the hips so the body forms a "
        "straight line, and stack the feet or stagger them for stability. Squeeze the bottom "
        "of the abdomen and hold without letting the hips drop. Taken for time on each side."
    ),
    # Four more found by ExerciseDescriptionShapeTest, which is why that case
    # exists: a first scan for the most obvious vocabulary missed these because
    # "ubiquitous", "a favorite of" and "fitness test" are less blatant than
    # "Named after ...". Same defect, same fix.
    "close_grip_lat_pulldown": (
        "A pull-down taken with the hands closer together than usual, which narrows the path "
        "and brings the elbows in alongside the head. Sit at the machine, take a grip just "
        "inside shoulder width, lean back very slightly with the chest up, and pull the bar "
        "to the top of the chest by driving the elbows down. Let the arms lengthen back to "
        "the top of the stretch under control. The narrower the grip, the more the lower "
        "traps and the shorter the range."
    ),
    "lat_pulldown": (
        "A seated cable row done from above, pulling the bar to the chest to work the lats. "
        "Sit with the thighs secured under the pad and the chest tall, take a grip wider than "
        "the shoulders, lean back only slightly, and pull the bar to the top of the chest "
        "with the elbows driving down toward the back pockets. Let the bar rise back to a full "
        "stretch overhead. Grip is often the first thing to fail, so straps help on the "
        "heavier sets."
    ),
    "kneeling_wrist_stretch": (
        "A kneeling stretch for the wrists and forearms. Kneel with the knees together and "
        "turn the hands so the backs of the hands rest flat on the floor, fingers pointing "
        "back toward the knees. Keep the hips over the heels and lean the body gently "
        "backwards until the forearms and the fronts of the wrists open up. Ease off "
        "immediately if any position produces a sharp pain rather than a stretch."
    ),
    "pull_up": (
        "A bodyweight pull from a dead hang to the chin over the bar, working the upper back, "
        "biceps and core. Hang from the bar with the palms turned away, pull the shoulder "
        "blades down and back first, then drive the elbows down until the chin clears the bar. "
        "Lower under control to a full dead hang rather than dropping. The narrower the grip "
        "the more the back and biceps, the wider the more the chest and arms. It is also the "
        "standard measure of upper-body strength relative to body weight, since almost nothing "
        "else scales that way."
    ),
    # A THIRD batch, and the one that matters most: these had no provenance
    # language at all, so they passed the reputation filter while still failing
    # the owner's actual request. "The leg extension is an isolation exercise for
    # the quads" names the muscle and stops - it never says how to do it. The
    # tripwire in ExerciseDescriptionShapeTest flags short text for exactly this
    # reason; a blocklist of reputation words alone would have shipped these.
    "leg_extension": (
        "A seated knee extension that isolates the quadriceps. Sit in the machine with the "
        "knee joint lined up with the machine's pivot and the pad resting on the shin just "
        "above the ankle. Extend the knee to lift the pad and squeeze the quad at the top, "
        "then lower slowly until the knee is almost straight. The slow return and the squeeze "
        "at the top are what make it work, rather than swinging the weight."
    ),
    "decline_crunch": (
        "A crunch performed on a decline bench with the hips higher than the head, so gravity "
        "pulls harder on the upper abdominals. Secure the legs at the top of the bench, put "
        "the hands lightly behind the head without pulling on the neck, and curl the ribcage "
        "toward the hips, letting the shoulder blades roll up off the bench. Lower slowly "
        "until the back is flat again."
    ),
    "deficit_deadlift": (
        "A deadlift started with the bar raised several inches off the floor, which lengthens "
        "the range and forces the hips to sit lower before the lift begins. Stand with the "
        "bar on a platform roughly shin-high, take the grip, and brace before pulling. Drive "
        "the floor away and stand, letting the knees and hips rise together, then lock out "
        "and reverse the path. The extra range of motion below the knee is what makes this "
        "stiffer than a conventional deadlift."
    ),
    "machine_triceps_extension": (
        "A triceps extension on a machine, working the arms through a fixed arc. Sit with the "
        "upper arms resting flat on the pad and the forearms under the roller. Extend the "
        "elbows to straighten the forearms and squeeze the triceps at the top, then bend the "
        "elbows slowly to a full stretch without letting the upper arms lift off the pad. "
        "Keeping the arms pinned to the pad is what keeps the load on the triceps."
    ),
    "incline_barbell_bench_press": (
        "A bench press with the head raised above the hips, which shifts more of the work to "
        "the upper chest. Set the bench to roughly thirty degrees, set the shoulder blades back "
        "and down into the pad, and lower the bar to the upper chest just below the collarbone. "
        "Press back up and slightly toward the head. A steeper incline moves further toward "
        "the shoulders, a shallower one toward the chest."
    ),
    "incline_dumbbell_press": (
        "A dumbbell press done on an incline bench, angled to reach the upper chest. Set the "
        "bench to about thirty degrees and press the shoulder blades into the pad, then lower "
        "the dumbbells to the upper chest just below the collarbone with the wrists stacked "
        "over the elbows. Press back up without letting the dumbbells drift forward of the "
        "wrists at the top."
    ),
    "dumbbell_tricep_kickback": (
        "A triceps extension performed with the upper arm held fixed and the forearm swinging "
        "like a hammer. Stand hinged forward with the elbow drawn back alongside the ribs and "
        "locked there, the forearm hanging straight down. Straighten the elbow to drive the "
        "weight up until the arm is parallel to the floor, squeezing the triceps, then lower "
        "slowly. The upper arm staying still is the whole exercise; if it travels forward the "
        "shoulder takes over."
    ),
    "bench_dips": (
        "A dip between two parallel benches, working the triceps and the fronts of the "
        "shoulders. Sit on the front bench, grip the edge, and walk the feet back so the "
        "hips hang off the front bench between the two. Bend the elbows straight back to lower "
        "the hips until the elbows are near ninety degrees, keeping the shoulders down and away "
        "from the ears. Press back up to the starting position without letting the shoulders "
        "shrug."
    ),
    "single_leg_glute_bridge": (
        "A glute bridge done on one leg, which doubles the load on that side's glute and "
        "turns the exercise into an imbalance check. Lie on the back with one knee bent and the "
        "foot flat, and the other leg extended with the knee straight. Brace, drive through the "
        "planted heel to lift the hips until the body forms a straight line, squeezing the glute "
        "at the top. Lower slowly without arching the lower back."
    ),
    "kettlebell_single_leg_deadlift": (
        "A kettlebell deadlift done standing on one leg. Hold the bell in the opposite hand to "
        "the standing leg, hinge forward on the standing leg while sending the free leg back "
        "and up, and lower the bell toward the floor between the feet or just outside the "
        "standing foot. Return to standing under control. The single-leg base turns it into a "
        "balance and core exercise as well as a hinge."
    ),
    "single_leg_extension": (
        "A leg extension performed one leg at a time on the machine. Sit in the pad with the "
        "knee joint aligned to the machine's pivot, then extend the knee to lift the pad and "
        "squeeze the quadriceps at the top. Lower slowly to a full stretch. Working one side "
        "at a time makes it easier to feel the quad and to notice a difference between the legs."
    ),
    "single_leg_lying_leg_curl": (
        "A lying leg curl performed one leg at a time. Lie face down on the pad with the hips "
        "flat, curl one heel toward the glute while the other leg stays extended and heavy, and "
        "lower slowly to a full stretch. Doing one side at a time lets each hamstring be loaded "
        "properly and makes a difference between the legs easier to feel."
    ),
    "single_arm_dumbbell_row": (
        "A dumbbell row taken one arm at a time, braced on a bench or a thigh. Rest one hand "
        "on the support, let the working arm hang straight down, and row the dumbbell toward "
        "the hip by driving the elbow past the body. Pause with the shoulder bladed back, then "
        "lower to a full stretch. One arm at a time takes more load through the lats and makes "
        "it easy to even out a difference between sides."
    ),
    "sit_ups": (
        "A trunk curl from lying down to sitting up, done with the feet anchored or free. Lie "
        "on the back with the knees bent, cross the arms or place the hands beside the ears, "
        "and curl up until the torso is upright without pulling hard on the neck. Lower slowly "
        "and without dropping onto the back. The anchor point changes how much the hip flexors "
        "take over, so both ends exist."
    ),
    "skull_crusher": (
        "A lying triceps extension, where the bar travels behind the head toward the floor. "
        "Lie on a bench, hold an EZ bar or barbell with the hands shoulder-width, keep the "
        "upper arms vertical, and bend the elbows to lower the bar past the forehead toward "
        "the floor behind the head. Extend the elbows to bring it back to the starting "
        "position. The upper arms stay pointed at the ceiling throughout, which is what keeps "
        "the triceps rather than the shoulders doing the work."
    ),
    "spoto_press": (
        "A bench press that pauses for a couple of seconds with the bar on the chest before "
        "pressing. Lie on the bench, lower the bar to the chest, hold it motionless for two to "
        "three seconds without letting it bounce, then press it back up. The pause takes all "
        "momentum out of the rep and puts more time under tension through the chest and "
        "triceps."
    ),
    "stair_climber": (
        "A machine with pedals that travel up and down, driven by the legs while the body "
        "stays mostly upright. Step onto the pedals, stand tall, and drive them with one foot "
        "then the other, raising the pace for intervals or keeping it steady for longer "
        "sessions. The glutes and calves do most of the work on top of the cardiovascular load."
    ),
    "standing_calf_raise": (
        "A calf raise performed standing, loading the gastrocnemius. Stand tall with the feet "
        "about hip-width and the balls of the feet on a step or the floor, then rise onto the "
        "balls as high as possible without leaning back. Pause at the top and lower until the "
        "heels drop below the step for a full stretch. Often taken for a high number of reps "
        "or held for time."
    ),
    "stationary_bike": (
        "A machine that turns pedals at a set resistance, loading the legs without impact. "
        "Sit with the seat high enough that the knee is slightly bent at the bottom of the "
        "stroke, set the resistance, and pedal at the cadence you are targeting. Turning the "
        "resistance up or the cadence faster turns the same machine into an interval tool, and "
        "there are upright, recumbent and fan-bike variants that add arm work."
    ),
    "treadmill_running": (
        "Running on a machine, which lets the speed and the incline be changed without "
        "stopping. Start with a walk to warm up, set the speed, and increase the incline "
        "before raising the pace again, which is what makes the treadmill different from "
        "running outdoors. Keep the cadence light and step under the body rather than leaning "
        "forward off the front of the belt."
    ),
}