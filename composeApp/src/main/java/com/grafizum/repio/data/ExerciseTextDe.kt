package com.grafizum.repio.data

/**
 * German names and method descriptions for the built-in exercise pool.
 *
 * Keyed by the canonical-name slug from [ExerciseLocalization.slugify] — see
 * [ExerciseLocalization] for why the key is the English name's slug and not the English
 * name itself. `ExerciseLocalizationTest` asserts that this table covers every pool
 * exercise exactly once and that no entry names an exercise outside the pool, so a typo in
 * a key fails the build instead of quietly leaving one exercise in English.
 */
internal val EXERCISE_TEXT_DE: Map<String, ExerciseText> = mapOf(
    // --- Bodyweight ---
    "archer_pull_ups" to ex(
        "Archer-Klimmzüge",
        "Hänge an der Stange, eine Hand nahe am Ende und die andere näher zur Mitte. Ziehe dich hoch und verlager dein Gewicht zum breiten Arm, während der andere Arm seitlich gestreckt bleibt."
    ),
    "bicycle_crunches" to ex(
        "Fahrrad-Crunches",
        "Liege auf dem Rücken, die Hände hinter dem Kopf. Führe ein Knie und den gegenüberliegenden Ellbogen zusammen und strecke das andere Bein aus – Seite für Seite, ohne am Nacken zu ziehen."
    ),
    "bird_dogs" to ex(
        "Bird-Dogs",
        "Starte im Vierfüßlerstand. Strecke gegenüberliegenden Arm und Bein bis auf Rumpfhöhe, halte kurz und wechsle die Seite, ohne dass die Hüfte kippt."
    ),
    "back_extensions" to ex(
        "Rückenstrecken",
        "Fixiere die Hüfte auf dem Polster der Rückenstreckbank. Beuge dich aus der Hüfte nach vorne und richte dich dann mit unterem Rücken und Gesäß wieder auf."
    ),
    "box_jumps" to ex(
        "Box-Jumps",
        "Stell dich vor eine stabile Box. Springe explosiv ab und lande weich mit gebeugten Knien auf der Box, dann steige oder springe kontrolliert herunter."
    ),
    "calf_raises" to ex(
        "Wadenheben",
        "Stell dich mit den Fußballen auf eine Stufe. Drücke dich so hoch wie möglich auf die Zehen, halte kurz und senke die Fersen unter die Stufe ab."
    ),
    "chin_ups" to ex(
        "Chin-ups",
        "Hänge im Untergriff (Handflächen zu dir) schulterbreit an der Stange. Ziehe das Kinn über die Stange und senke dich kontrolliert ab."
    ),
    "clamshells" to ex(
        "Clamshells",
        "Liege auf der Seite, Knie gebeugt und Füße zusammen. Öffne das obere Knie wie eine Muschel, spanne das Gesäß oben an und schließe langsam."
    ),
    "copenhagen_planks" to ex(
        "Copenhagen Planks",
        "Liege auf der Seite, das obere Bein auf einer Bank. Spanne den Rumpf an und hebe die Hüfte vom Boden ab, dann halte die Position."
    ),
    "cossack_squats" to ex(
        "Cossack Squats",
        "Stehe breitbeinig mit nach vorne zeigenden Zehen. Verlager das Gewicht auf ein Bein und gehe in die tiefe Hocke, während das andere Bein gestreckt bleibt, dann Seite wechseln."
    ),
    "crunches" to ex(
        "Crunches",
        "Liege auf dem Rücken, Knie gebeugt und Füße flach. Rolle die Schultern vom Boden ab, drücke den unteren Rücken in den Boden und senke langsam ab."
    ),
    "dead_hangs" to ex(
        "Dead Hangs",
        "Hänge mit gestreckten Armen und entspannten Schultern an der Stange. Halte die Position, um Griffkraft und Ausdauer der Lats aufzubauen."
    ),
    "decline_sit_ups_weighted" to ex(
        "Sit-ups auf der Negativbank (mit Gewicht)",
        "Verankere die Füße auf der Negativbank und halte ein Gewicht an der Brust. Rolle den Oberkörper hoch und senke ihn kontrolliert ab."
    ),
    "dips" to ex(
        "Dips",
        "Stütze dich mit gestreckten Armen auf den Barren. Senke dich ab, indem du die Ellbogen beugst, bis die Schultern unter die Ellbogen sinken, und drücke dich wieder hoch."
    ),
    "dips_chest_focus" to ex(
        "Dips (Brustfokus)",
        "Stütze dich auf den Barren und lehne den Oberkörper nach vorne. Senke dich ab, bis die Schultern unter den Ellbogen sind, halte die Vorlage und drücke dich wieder hoch."
    ),
    "dirty_dogs" to ex(
        "Dirty Dogs",
        "Starte im Vierfüßlerstand mit gebeugten Knien. Hebe ein Knie seitlich ab wie ein Hund am Hydranten, halte die Hüfte waagerecht und senke es wieder ab."
    ),
    "donkey_kicks" to ex(
        "Donkey Kicks",
        "Starte im Vierfüßlerstand. Tritt ein Bein mit gebeugtem Knie nach oben und hinten, spanne das Gesäß oben an und senke ab."
    ),
    "dragon_flags" to ex(
        "Dragon Flags",
        "Liege auf dem Rücken und greife hinter dem Kopf eine Bank. Hebe Beine und Hüfte an und senke den ganzen Körper gestreckt ab, ohne den Boden zu berühren."
    ),
    "false_grip_holds" to ex(
        "False-Grip-Holds",
        "Lege die Handgelenke im falschen Griff über die Stange, die Knöchel zeigen nach oben. Hänge oder halte die Position für Griffkraft und Unterarme."
    ),
    "finger_push_ups" to ex(
        "Finger-Liegestütze",
        "Mache Liegestütze auf den Fingerspitzen statt auf den Handflächen, um Finger und Unterarme zu kräftigen, und halte den Körper dabei gestreckt."
    ),
    "fire_hydrants" to ex(
        "Fire Hydrants",
        "Starte im Vierfüßlerstand. Hebe ein Knie seitlich auf Hüfthöhe, der Rücken bleibt flach, dann senken und Seite wechseln."
    ),
    "fist_squeezes" to ex(
        "Faustdrücken",
        "Balle die Fäuste so fest du kannst für ein paar Sekunden und löse dann. Wiederhole das für mehr Griffkraft."
    ),
    "frog_pumps" to ex(
        "Frog Pumps",
        "Liege auf dem Rücken, die Fußsohlen aneinander und die Knie nach außen. Drücke die Hüfte nach oben, spanne das Gesäß an und senke sie wieder ab."
    ),
    "garhammer_raises" to ex(
        "Garhammer Raises",
        "Hänge an der Stange. Hebe die gestreckten Beine etwa auf 45 Grad an und senke sie langsam ab, ohne zu schwingen."
    ),
    "glute_bridges" to ex(
        "Glute Bridges",
        "Liege auf dem Rücken, Knie gebeugt und Füße flach. Drücke die Hüfte nach oben, spanne das Gesäß oben fest an und senke langsam ab."
    ),
    "handstand_holds" to ex(
        "Handstand Holds",
        "Schwing dich an der Wand in den Handstand. Halte die Position mit gestreckten Armen, angespanntem Rumpf und gestreckten Zehen."
    ),
    "handstand_shrugs" to ex(
        "Handstand-Shrugs",
        "Ziehe im Handstand an der Wand die Schultern Richtung Ohren und drücke sie anschließend wieder nach unten."
    ),
    "hanging_leg_raises" to ex(
        "Hängendes Beinheben",
        "Hänge an der Stange. Hebe die gestreckten oder leicht gebeugten Beine auf Hüfthöhe oder höher, ohne zu schwingen, und senke sie langsam ab."
    ),
    "hanging_side_raises" to ex(
        "Hängendes Seitheben",
        "Hänge an der Stange. Hebe die Beine seitlich nach links und rechts, kontrolliert und ohne Schwung zu holen."
    ),
    "hollow_body_holds" to ex(
        "Hollow Body Holds",
        "Liege auf dem Rücken und hebe Beine und Schultern vom Boden ab, während du den unteren Rücken fest in den Boden drückst. Halte die Position."
    ),
    "inverted_curls" to ex(
        "Inverted Curls",
        "Greife eine Stange oder Ringe im Untergriff und häng schräg darunter. Ziehe die Brust zur Stange und curle dabei mit, dann senke dich wieder ab."
    ),
    "inverted_rows" to ex(
        "Inverted Rows",
        "Setze eine Stange auf Hüfthöhe und häng mit gestrecktem Körper darunter. Ziehe die Brust zur Stange, spanne den Rücken an und senke dich langsam ab."
    ),
    "jumping_jacks" to ex(
        "Jumping Jacks",
        "Stehe mit geschlossenen Füßen und Armen am Körper. Springe, öffne die Beine und führe die Arme über den Kopf, dann zurück in die Ausgangsposition."
    ),
    "l_sits" to ex(
        "L-Sits",
        "Stütze dich auf Barren ab oder drücke die Handflächen in den Boden. Hebe die gestreckten Beine waagerecht nach vorne und halte sie parallel zum Boden."
    ),
    "l_sit_pull_ups" to ex(
        "L-Sit Pull-ups",
        "Hänge an der Stange und hebe die Beine in die L-Position. Ziehe dich hoch, halte das L und senke dich kontrolliert ab."
    ),
    "lateral_plank_walks" to ex(
        "Laterale Plank Walks",
        "Halte einen Seitstütz auf dem Unterarm. Führe das obere Bein wiederholt über das untere und zurück, ohne die Hüfte absinken zu lassen."
    ),
    "leg_raises" to ex(
        "Beinheben",
        "Liege auf dem Rücken, die Beine gestreckt. Hebe sie Richtung Decke, ohne den unteren Rücken zu überstrecken, und senke sie langsam ab."
    ),
    "lunges" to ex(
        "Ausfallschritte",
        "Stehe aufrecht und mache einen Schritt nach vorne, bis beide Knie etwa 90 Grad bilden. Drücke dich zurück in den Stand und wechsle das Bein."
    ),
    "mountain_climbers" to ex(
        "Mountain Climbers",
        "Starte im Unterarm- oder Liegestütz. Zieh abwechselnd das Knie schnell zur Brust und halte die Hüfte dabei tief."
    ),
    "muscle_ups_bw" to ex(
        "Muscle-ups",
        "Ziehe explosiv hoch, bis die Brust über der Stange ist, drücke dich dann über die Stange in den Dip und beende mit gestreckten Armen."
    ),
    "nordic_curls" to ex(
        "Nordic Curls",
        "Knie dich hin und fixiere die Fußknöchel. Senke den Oberkörper langsam und kontrolliert ab und ziehe dich mit den Beinbeugern wieder hoch."
    ),
    "plank_to_down_dogs" to ex(
        "Plank to Down Dog",
        "Drücke aus dem Unterarmstütz die Hüfte hoch und zurück in den Downward Dog, dann zurück in den Stütz. Bewege dich kontrolliert."
    ),
    "planks" to ex(
        "Planks",
        "Stütze dein Gewicht auf Unterarme und Zehen. Halte den Körper in einer geraden Linie, spanne den Rumpf an und lass die Hüfte nicht durchhängen."
    ),
    "pike_holds" to ex(
        "Pike Holds",
        "Zieh aus dem Liegestütz die Füße heran und hebe die Hüfte in den Pike. Halte die Position mit gestreckten Beinen und Armen."
    ),
    "pull_ups" to ex(
        "Klimmzüge",
        "Hänge im Obergriff etwas breiter als schulterbreit an der Stange. Ziehe die Brust zur Stange und senke dich kontrolliert ab."
    ),
    "push_ups" to ex(
        "Liegestütze",
        "Starte im hohen Stütz mit den Händen unter den Schultern. Senke die Brust zum Boden und drücke dich wieder hoch, der Körper bleibt gestreckt."
    ),
    "reverse_snow_angels" to ex(
        "Reverse Snow Angels",
        "Liege bäuchlings und hebe Arme und Beine leicht vom Boden ab. Führe die Arme wie einen Schneeengel vom Überkopf bis zur Seite, ohne sie abzulegen."
    ),
    "russian_twists" to ex(
        "Russian Twists",
        "Setze dich mit gebeugten Knien hin und lehne dich leicht zurück. Rotiere den Oberkörper Seite für Seite, die Brust bleibt hoch und der Rumpf angespannt."
    ),
    "scapular_pull_ups" to ex(
        "Scapular Pull-ups",
        "Hänge mit gestreckten Armen an der Stange. Ziehe die Schulterblätter nach unten zusammen, um dich ein paar Zentimeter zu heben, und lass wieder los."
    ),
    "scapular_push_ups" to ex(
        "Scapular Push-ups",
        "Starte im Unterarmstütz. Bewege die Schulterblätter zusammen und auseinander, ohne die Ellbogen zu beugen."
    ),
    "shoulder_taps" to ex(
        "Shoulder Taps",
        "Starte im hohen Stütz. Tippe abwechselnd mit jeder Hand die gegenüberliegende Schulter an, ohne die Hüfte zu bewegen."
    ),
    "side_lying_leg_raises" to ex(
        "Seitliches Beinheben",
        "Liege auf der Seite, die Beine gestreckt. Hebe das obere Bein gestreckt zur Decke und senke es langsam ab."
    ),
    "side_plank_crunches" to ex(
        "Seitstütz-Crunches",
        "Halte einen Seitstütz auf dem Unterarm. Führe den oberen Ellbogen und das obere Knie zusammen und strecke dann wieder aus."
    ),
    "side_plank_leg_raises" to ex(
        "Seitstütz-Beinheben",
        "Halte einen Seitstütz auf dem Unterarm. Hebe und senke das obere Bein, während die Hüfte stabil bleibt."
    ),
    "side_plank_rotations" to ex(
        "Seitstütz-Rotationen",
        "Halte einen Seitstütz auf dem Unterarm. Rotiere den Oberkörper und führe den oberen Arm unter dem Körper durch, dann wieder aufdrehen."
    ),
    "side_planks" to ex(
        "Seitstütze",
        "Stütze dein Gewicht auf einem Unterarm, die Füße übereinander. Hebe die Hüfte, bis der Körper eine gerade Linie bildet, und halte die Position."
    ),
    "side_steps" to ex(
        "Seitwärts-Schritte",
        "Lege ein Band um die Oberschenkel und stehe hüftbreit. Mache Seitwärts-Schritte, halte die Spannung im Band und gehe zurück."
    ),
    "sissy_squats" to ex(
        "Sissy Squats",
        "Halte dich an einem Halt fest und lehne dich leicht zurück. Senke dich über die Kniebeugung ab, während der Oberkörper zurückgelehnt bleibt, und richte dich wieder auf."
    ),
    "sliding_leg_curls" to ex(
        "Sliding Leg Curls",
        "Liege auf dem Rücken, die Fersen auf Gleitern oder Handtüchern. Ziehe die Füße zum Gesäß und strecke die Beine wieder aus."
    ),
    "split_squats" to ex(
        "Split Squats",
        "Ein Fuß steht vorne, der andere dahinter. Senke dich gerade nach unten, bis das hintere Knie fast den Boden berührt, und drücke dich über den vorderen Fuß hoch."
    ),
    "squats" to ex(
        "Kniebeugen",
        "Stehe schulterbreit. Senke die Hüfte, als würdest du dich nach hinten auf einen Stuhl setzen, und drücke dich über die Fersen in den Stand."
    ),
    "step_ups" to ex(
        "Step-ups",
        "Stehe vor einer Bank. Steige mit einem Fuß auf, drücke dich über die Ferse hoch, senke dich wieder ab und wechsle das Bein."
    ),
    "sumo_squats" to ex(
        "Sumo-Kniebeugen",
        "Stehe breit mit nach außen gedrehten Zehen. Gehe zwischen den Beinen in die Hocke, die Brust bleibt hoch und die Knie zeigen über die Zehen, dann drücke dich hoch."
    ),
    "superman_extensions" to ex(
        "Superman Extensions",
        "Liege bäuchlings und hebe Arme, Brust und Beine vom Boden ab. Halte kurz und senke langsam ab."
    ),
    "supermans" to ex(
        "Supermans",
        "Liege bäuchlings mit gestreckten Armen. Hebe Arme, Brust und Beine gleichzeitig an, halte kurz und senke ab."
    ),
    "tibialis_raises" to ex(
        "Tibialis Raises",
        "Sitze mit fixierten Fersen und einem Gewicht auf den Zehen oder stehe mit dem Rücken an der Wand. Ziehe die Zehen Richtung Schienbein und senke langsam ab."
    ),
    "tip_toe_walks" to ex(
        "Zehenspitzen-Gehen",
        "Gehe eine Strecke oder eine Zeit lang auf den Zehenspitzen, um die Waden zu kräftigen."
    ),
    "towel_hangs" to ex(
        "Handtuch-Hangs",
        "Wickle je ein Handtuch über die Stange und häng dich daran. Halte die Position für mehr Griffkraft."
    ),
    "underhand_inverted_rows" to ex(
        "Inverted Rows im Untergriff",
        "Setze eine Stange auf Hüfthöhe und häng im Untergriff darunter. Ziehe die Brust zur Stange und senke dich langsam ab."
    ),
    "v_ups" to ex(
        "V-Ups",
        "Liege auf dem Rücken mit gestreckten Armen über dem Kopf. Hebe Beine und Oberkörper gleichzeitig zu einem V und senke kontrolliert ab."
    ),
    "wall_sits" to ex(
        "Wandsitze",
        "Rutsche mit dem Rücken an der Wand herunter, bis die Oberschenkel waagerecht sind. Halte die Position mit 90 Grad in den Knien."
    ),
    "wall_slides" to ex(
        "Wall Slides",
        "Stehe mit dem Rücken an der Wand und 90 Grad gebeugten Armen. Schiebe die Arme an der Wand auf und ab und halte Rücken und Arme angelehnt."
    ),
    "weighted_dips" to ex(
        "Dips mit Zusatzgewicht",
        "Mache Dips mit einer Kurzhantel zwischen den Füßen oder einem Dip-Gürtel. Senke dich ab, bis die Schultern unter den Ellbogen sind, und drücke dich hoch."
    ),
    "wide_grip_pull_ups" to ex(
        "Klimmzüge im weiten Griff",
        "Hänge im weiten Griff an der Stange. Ziehe die Brust zur Stange und senke dich kontrolliert ab."
    ),
    "windshield_wipers" to ex(
        "Windshield Wipers",
        "Liege auf dem Rücken, die Arme seitlich und die Beine angehoben. Senke die Beine wie einen Scheibenwischer von Seite zu Seite, die Schultern bleiben unten."
    ),
    "y_w_t_raises" to ex(
        "Y-W-T Raises",
        "Liege bäuchlings und forme mit den Armen ein Y, W und T, indem du jede Position vom Boden abhebst, um den oberen Rücken zu trainieren."
    ),

    // --- Kurzhanteln ---
    "arnold_presses" to ex(
        "Arnold Presses",
        "Halte Kurzhanteln auf Schulterhöhe, die Handflächen zu dir. Drücke sie nach oben und drehe die Handflächen dabei nach außen, bis die Arme gestreckt sind; beim Absenken zurückdrehen."
    ),
    "concentration_curls" to ex(
        "Konzentrations-Curls",
        "Setze dich mit dem Ellbogen am inneren Oberschenkel abgestützt. Curle die Kurzhantel zur Schulter, ohne den Oberarm zu bewegen, und senke langsam ab."
    ),
    "decline_dumbbell_presses" to ex(
        "Kurzhantel-Drücken auf der Negativbank",
        "Liege mit je einer Kurzhantel auf der Negativbank. Senke sie zur unteren Brust ab und drücke sie nach oben, bis die Arme gestreckt sind."
    ),
    "dumbbell_back_extensions" to ex(
        "Rückenstrecken mit Kurzhantel",
        "Halte eine Kurzhantel an der Brust auf der Rückenstreckbank. Beuge dich aus der Hüfte nach vorne und richte dich mit unterem Rücken und Gesäß wieder auf."
    ),
    "dumbbell_bench_presses" to ex(
        "Kurzhantel-Bankdrücken",
        "Liege flach auf der Bank, je eine Kurzhantel auf Brusthöhe. Drücke sie nach oben, bis die Arme gestreckt sind, und senke kontrolliert ab."
    ),
    "dumbbell_calf_raises" to ex(
        "Wadenheben mit Kurzhanteln",
        "Halte Kurzhanteln seitlich und stell die Fußballen auf eine Stufe. Drücke dich auf die Zehen, halte kurz und senke die Fersen unter die Stufe."
    ),
    "dumbbell_clean_presses" to ex(
        "Umsetzen & Drücken mit Kurzhanteln",
        "Setze eine Kurzhantel explosiv vom Boden oder aus dem Hang auf die Schulter um und drücke sie anschließend über Kopf bis zur Streckung."
    ),
    "dumbbell_crunches" to ex(
        "Crunches mit Kurzhantel",
        "Liege auf dem Rücken und halte eine Kurzhantel an der Brust. Rolle die Schultern hoch, drücke den unteren Rücken in den Boden und senke ab."
    ),
    "dumbbell_curls" to ex(
        "Kurzhantel-Curls",
        "Stehe mit Kurzhanteln seitlich, die Handflächen nach vorne. Curle sie zu den Schultern, die Ellbogen bleiben eng am Körper, und senke langsam ab."
    ),
    "dumbbell_deadlifts" to ex(
        "Kurzhantel-Kreuzheben",
        "Stehe mit den Kurzhanteln vor den Oberschenkeln. Beuge dich mit flachem Rücken aus der Hüfte ab und senke sie über die Knie, dann richte dich wieder auf."
    ),
    "dumbbell_face_pulls" to ex(
        "Face Pulls mit Kurzhanteln",
        "Halte leichte Kurzhanteln und hebe die Arme mit den Daumen nach oben seitlich an, dann beuge die Ellbogen und ziehe die Gewichte zum Gesicht, um die hintere Schulter anzuspannen."
    ),
    "dumbbell_floor_presses" to ex(
        "Kurzhantel-Floor Press",
        "Liege flach auf dem Boden, je eine Kurzhantel an der Brust. Drücke sie nach oben und senke sie ab, bis die Oberarme den Boden berühren."
    ),
    "dumbbell_flyes" to ex(
        "Kurzhantel-Flys",
        "Liege auf der Bank, Kurzhanteln über der Brust, die Handflächen zueinander und die Ellbogen leicht gebeugt. Öffne die Arme in einem weiten Bogen und drücke die Brust zusammen, um sie wieder zu schließen."
    ),
    "dumbbell_front_raises" to ex(
        "Kurzhantel-Frontheben",
        "Stehe mit Kurzhanteln an den Oberschenkeln. Hebe sie gestreckt vor dir auf Schulterhöhe und senke sie langsam ab."
    ),
    "dumbbell_glute_bridges" to ex(
        "Glute Bridges mit Kurzhantel",
        "Liege auf dem Rücken, Knie gebeugt, eine Kurzhantel auf der Hüfte. Drücke die Hüfte nach oben, spanne das Gesäß oben an und senke ab."
    ),
    "dumbbell_good_mornings" to ex(
        "Good Mornings mit Kurzhanteln",
        "Halte Kurzhanteln an den Schultern oder an den Oberschenkeln. Beuge dich mit flachem Rücken und leicht gebeugten Knien aus der Hüfte nach vorne und richte dich wieder auf."
    ),
    "dumbbell_hammer_curls" to ex(
        "Kurzhantel-Hammer-Curls",
        "Halte die Kurzhanteln mit zueinander zeigenden Handflächen. Curle sie zu den Schultern und behalte den neutralen Griff bei, dann senke langsam ab."
    ),
    "dumbbell_hip_thrusts" to ex(
        "Hüftstoßen mit Kurzhantel",
        "Sitze mit dem oberen Rücken an einer Bank, eine Kurzhantel über der Hüfte. Drücke die Hüfte nach oben, spanne das Gesäß oben an und senke ab."
    ),
    "dumbbell_kickbacks" to ex(
        "Kurzhantel-Kickbacks",
        "Beuge dich vor, ein Arm seitlich am Körper und der Ellbogen gebeugt. Strecke den Arm gerade nach hinten, spanne den Trizeps an und führe zurück."
    ),
    "dumbbell_lateral_raises" to ex(
        "Kurzhantel-Seitheben",
        "Stehe mit Kurzhanteln seitlich und leicht gebeugten Ellbogen. Hebe die Arme seitlich auf Schulterhöhe und senke sie langsam ab."
    ),
    "dumbbell_leg_curls" to ex(
        "Beinbeuger mit Kurzhantel",
        "Liege bäuchlings oder sitze und halte eine Kurzhantel zwischen den Füßen. Curle die Fersen zum Gesäß und senke langsam ab."
    ),
    "dumbbell_leg_raises" to ex(
        "Beinheben mit Kurzhantel",
        "Liege auf dem Rücken und halte eine Kurzhantel zwischen den Füßen oder in den Händen. Hebe die gestreckten Beine an und senke sie kontrolliert ab."
    ),
    "dumbbell_lunges" to ex(
        "Kurzhantel-Ausfallschritte",
        "Halte Kurzhanteln seitlich. Mache einen Schritt nach vorne und senke dich, bis beide Knie etwa 90 Grad bilden, dann zurück in den Stand und Bein wechseln."
    ),
    "dumbbell_overhead_extensions" to ex(
        "Überkopf-Strecken mit Kurzhantel",
        "Halte eine Kurzhantel mit beiden (oder einer) Händen über Kopf. Senke sie mit gebeugten Ellbogen hinter den Kopf und drücke sie wieder nach oben."
    ),
    "dumbbell_pinch_grips" to ex(
        "Kurzhantel-Pinch-Griff",
        "Kneife eine Kurzhantel mit Fingern und Daumen an der oberen Scheibe. Halte sie seitlich auf Zeit, um die Griffkraft zu stärken."
    ),
    "dumbbell_pullovers" to ex(
        "Kurzhantel-Überzüge",
        "Liege auf der Bank und halte eine Kurzhantel mit beiden Händen über der Brust. Senke sie bogenförmig hinter den Kopf und ziehe sie über die Brust zurück."
    ),
    "dumbbell_rear_delt_flyes" to ex(
        "Reverse Flys mit Kurzhanteln",
        "Beuge dich mit flachem Rücken vor. Hebe mit leicht gebeugten Ellbogen die Kurzhanteln seitlich an und spanne die hintere Schulter an."
    ),
    "dumbbell_reverse_curls" to ex(
        "Reverse-Curls mit Kurzhanteln",
        "Halte die Kurzhanteln im Obergriff (Handflächen nach unten). Curle sie zu den Schultern, die Ellbogen bleiben seitlich, und senke langsam ab."
    ),
    "dumbbell_romanian_deadlifts" to ex(
        "Rumänisches Kreuzheben mit Kurzhanteln",
        "Stehe mit Kurzhanteln an den Oberschenkeln. Schiebe die Hüfte zurück und führe die Gewichte mit leicht gebeugten Knien an den Beinen ab, bis du eine Dehnung spürst, dann zurück."
    ),
    "dumbbell_rows" to ex(
        "Kurzhantel-Rudern",
        "Beuge dich vor und stütze eine Hand auf einer Bank ab, der gegenüberliegende Fuß steht auf. Ziehe die Kurzhantel zur Hüfte, der Rücken bleibt flach, und senke kontrolliert ab."
    ),
    "dumbbell_russian_twists" to ex(
        "Russian Twists mit Kurzhantel",
        "Sitze mit gebeugten Knien und einer Kurzhantel an der Brust und lehne dich leicht zurück. Rotiere den Oberkörper Seite für Seite und tippe das Gewicht jeweils zum Boden."
    ),
    "dumbbell_shoulder_presses" to ex(
        "Kurzhantel-Schulterdrücken",
        "Halte Kurzhanteln auf Schulterhöhe, die Handflächen nach vorne. Drücke sie über Kopf, bis die Arme gestreckt sind, und senke kontrolliert ab."
    ),
    "dumbbell_shrugs" to ex(
        "Shrugs mit Kurzhanteln",
        "Halte Kurzhanteln seitlich mit gestreckten Armen. Ziehe die Schultern Richtung Ohren, halte kurz und senke ab."
    ),
    "dumbbell_side_bends" to ex(
        "Seitneigen mit Kurzhantel",
        "Halte eine Kurzhantel einseitig seitlich. Neige den Oberkörper zur Seite des Gewichts, richte dich wieder auf und wechsle die Seite."
    ),
    "dumbbell_side_lunges" to ex(
        "Seitliche Ausfallschritte mit Kurzhanteln",
        "Halte Kurzhanteln an der Brust oder seitlich. Mache einen Schritt zur Seite und beuge das Knie, während das andere Bein gestreckt bleibt, dann zurück zur Mitte."
    ),
    "dumbbell_skullcrushers" to ex(
        "Kurzhantel-Skullcrushers",
        "Liege auf der Bank und halte Kurzhanteln über der Brust. Senke sie mit gebeugten Ellbogen Richtung Kopf und strecke die Arme wieder."
    ),
    "dumbbell_split_squats" to ex(
        "Split Squats mit Kurzhanteln",
        "Halte Kurzhanteln seitlich, ein Fuß steht hinter dir auf einer Bank. Senke dich gerade ab, bis das hintere Knie fast den Boden berührt, und drücke dich hoch."
    ),
    "dumbbell_squats" to ex(
        "Kurzhantel-Kniebeugen",
        "Halte Kurzhanteln seitlich oder an den Schultern. Gehe in die Hocke, bis die Oberschenkel mindestens waagerecht sind, die Brust bleibt hoch, dann drücke dich hoch."
    ),
    "dumbbell_step_ups" to ex(
        "Step-ups mit Kurzhanteln",
        "Halte Kurzhanteln seitlich. Steige auf eine Bank, drücke dich über die Ferse hoch, senke dich ab und wechsle das Bein."
    ),
    "dumbbell_thrusters" to ex(
        "Kurzhantel-Thrusters",
        "Halte Kurzhanteln an den Schultern. Gehe in die Hocke und drücke dich hoch, um die Kurzhanteln in einer fließenden Bewegung über Kopf zu drücken."
    ),
    "dumbbell_v_ups" to ex(
        "V-Ups mit Kurzhantel",
        "Liege auf dem Rücken und halte eine Kurzhantel über Kopf. Hebe Beine und Oberkörper gleichzeitig zu einem V und senke kontrolliert ab."
    ),
    "dumbbell_windmills" to ex(
        "Windmühlen mit Kurzhantel",
        "Halte eine Kurzhantel gestreckt über Kopf. Drehe die Hüfte zurück und führe die freie Hand am Bein abwärts, dann zurück in den Stand."
    ),
    "dumbbell_woodchoppers" to ex(
        "Woodchoppers mit Kurzhantel",
        "Halte eine Kurzhantel mit beiden Händen. Rotiere den Oberkörper und schwinge sie diagonal von hoch nach tief (oder umgekehrt), der Rumpf bleibt dabei angespannt."
    ),
    "dumbbell_wrist_curls" to ex(
        "Handgelenk-Curls mit Kurzhanteln",
        "Sitze mit den Unterarmen auf den Oberschenkeln, die Handflächen nach oben und die Kurzhanteln in der Hand. Curle die Handgelenke nach oben und senke langsam ab."
    ),
    "dumbbell_y_raises" to ex(
        "Y-Raises mit Kurzhanteln",
        "Beuge dich leicht vor und halte Kurzhanteln seitlich. Hebe die Arme in Y-Form nach oben und außen und spanne den oberen Rücken an."
    ),
    "farmer_s_walks" to ex(
        "Farmer's Walks",
        "Halte in jeder Hand ein schweres Gewicht, die Haltung ist aufrecht und der Rumpf angespannt. Gehe mit gleichmäßigen, kontrollierten Schritten, ohne die Schultern hängen zu lassen."
    ),
    "goblet_squats" to ex(
        "Goblet Squats",
        "Halte eine Kurzhantel oder Kettlebell an der Brust. Gehe tief in die Hocke, die Brust bleibt oben und die Ellbogen zeigen innerhalb der Knie, dann drücke dich hoch."
    ),
    "incline_dumbbell_curls" to ex(
        "Curls auf der Schrägbank",
        "Setze dich auf eine Schrägbank, die Kurzhanteln hängen seitlich. Curle sie hoch, die Ellbogen bleiben zurück und die Schultern stabil, dann senke langsam ab."
    ),
    "incline_dumbbell_presses" to ex(
        "Kurzhantel-Drücken auf der Schrägbank",
        "Liege mit je einer Kurzhantel auf der Schrägbank an der oberen Brust. Drücke sie bis zur Streckung nach oben und senke kontrolliert ab."
    ),
    "preacher_curls" to ex(
        "Preacher Curls",
        "Setze dich an die Scott-Bank und lege die Oberarme auf dem Polster ab. Curle die Stange oder die Kurzhanteln hoch und senke ab, bis die Arme fast gestreckt sind."
    ),
    "rear_delt_floor_flyes" to ex(
        "Reverse Flys am Boden",
        "Liege bäuchlings auf dem Boden mit leichten Kurzhanteln. Hebe die Arme seitlich an, spanne die hintere Schulter an und senke ab."
    ),
    "spider_curls" to ex(
        "Spider Curls",
        "Liege bäuchlings auf einer Schrägbank mit Kurzhanteln. Curle sie zu den Schultern, die Oberarme hängen senkrecht nach unten, und senke langsam ab."
    ),
    "stiff_leg_dumbbell_deadlifts" to ex(
        "Kreuzheben mit gestreckten Beinen und Kurzhanteln",
        "Stehe mit Kurzhanteln an den Oberschenkeln und fast gestreckten Beinen. Beuge dich aus der Hüfte vor und führe die Gewichte an den Beinen ab, dann richte dich wieder auf."
    ),
    "zottman_curls" to ex(
        "Zottman Curls",
        "Curle die Kurzhanteln mit den Handflächen nach oben hoch. Drehe oben auf die Handflächen nach unten und senke langsam ab, dann für die nächste Wiederholung zurückdrehen."
    ),
    // --- Langhanteln ---
    "barbell_back_extensions" to ex(
        "Rückenstrecken mit Langhantel",
        "Halte eine Langhantel auf dem oberen Rücken auf der Rückenstreckbank. Beuge dich aus der Hüfte nach vorne und richte dich über den unteren Rücken und das Gesäß wieder auf."
    ),
    "barbell_bench_presses" to ex(
        "Langhantel-Bankdrücken",
        "Liege flach auf der Bank, die Stange über den Augen und die Füße fest am Boden. Greife etwas breiter als schulterbreit, senke die Stange zur Brustmitte und drücke sie bis zur Streckung."
    ),
    "barbell_calf_raises" to ex(
        "Wadenheben mit Langhantel",
        "Stehe mit der Stange auf den Schultern und den Fußballen auf einer Stufe. Drücke dich auf die Zehen, halte kurz und senke die Fersen unter die Stufe."
    ),
    "barbell_clean_presses" to ex(
        "Umsetzen & Drücken mit Langhantel",
        "Setze die Stange explosiv vom Boden oder aus dem Hang auf die Schultern um und drücke sie anschließend über Kopf bis zur Streckung."
    ),
    "barbell_crunches" to ex(
        "Crunches mit Langhantel",
        "Liege auf dem Rücken und halte die Stange an der Brust oder über Kopf. Rolle die Schultern vom Boden ab, der untere Rücken bleibt am Boden, und senke langsam ab."
    ),
    "barbell_curls" to ex(
        "Langhantel-Curls",
        "Stehe aufrecht und halte die Stange im Untergriff. Curle sie zur Brust, die Ellbogen bleiben an den Seiten, und senke langsam ab."
    ),
    "barbell_deadlifts" to ex(
        "Langhantel-Kreuzheben",
        "Stehe hüftbreit mit der Stange über dem Mittelfuß. Greife sie, spanne den Rumpf an und drücke dich mit flachem Rücken über die Fersen in den Stand, dann kontrolliert ablassen."
    ),
    "barbell_face_pulls" to ex(
        "Face Pulls mit Langhantel",
        "Setze eine Stange oder ein Seil auf Gesichtshöhe. Ziehe sie mit hohen und weiten Ellbogen zum Gesicht, spanne die hintere Schulter an und führe zurück."
    ),
    "barbell_floor_presses" to ex(
        "Langhantel-Floor Press",
        "Liege auf dem Boden mit der Stange über der Brust. Senke sie ab, bis die Oberarme den Boden berühren, und drücke hoch – der Boden begrenzt die Bewegung."
    ),
    "barbell_front_raises" to ex(
        "Langhantel-Frontheben",
        "Stehe aufrecht und halte die Stange im Obergriff. Hebe sie gestreckt vor dir auf Schulterhöhe und senke kontrolliert ab."
    ),
    "barbell_front_squats" to ex(
        "Langhantel-Frontkniebeugen",
        "Lege die Stange auf den vorderen Schultern ab, die Ellbogen zeigen hoch. Gehe in die Hocke, Brust hoch und Oberkörper aufrecht, dann drücke dich wieder hoch."
    ),
    "barbell_glute_bridges" to ex(
        "Glute Bridges mit Langhantel",
        "Liege auf dem Rücken, Knie gebeugt, die Stange auf der Hüfte. Drücke die Hüfte hoch, bis der Körper eine gerade Linie bildet, und senke ab."
    ),
    "barbell_good_mornings" to ex(
        "Good Mornings mit Langhantel",
        "Lege die Stange auf den oberen Rücken. Beuge dich mit flachem Rücken und leicht gebeugten Knien aus der Hüfte vor und richte dich über die Hüftstreckung wieder auf."
    ),
    "barbell_hip_thrusts" to ex(
        "Hüftstoßen mit Langhantel",
        "Sitze auf dem Boden mit dem oberen Rücken an einer Bank und der Stange über der Hüfte. Drücke die Hüfte hoch, spanne das Gesäß oben an, und senke ab."
    ),
    "barbell_leg_raises" to ex(
        "Beinheben mit Langhantel",
        "Liege auf dem Rücken und halte die Stange über Kopf oder zwischen den Füßen. Hebe die gestreckten Beine bis senkrecht und senke langsam ab, ohne den unteren Rücken zu überstrecken."
    ),
    "barbell_lunges" to ex(
        "Langhantel-Ausfallschritte",
        "Halte die Stange auf dem oberen Rücken. Mache einen Schritt nach vorne und senke dich, bis beide Knie etwa 90 Grad bilden, dann zurück in den Stand."
    ),
    "barbell_overhead_extensions" to ex(
        "Überkopf-Strecken mit Langhantel",
        "Halte die Stange mit engem Griff über Kopf. Senke sie mit gebeugten Ellbogen hinter den Kopf und drücke sie wieder bis zur Streckung hoch."
    ),
    "barbell_overhead_presses" to ex(
        "Langhantel-Überkopfdrücken",
        "Stehe mit der Stange am Schlüsselbein. Drücke sie über Kopf bis zur Streckung, der Rumpf bleibt fest, und senke sie zur Brust ab."
    ),
    "barbell_pullovers" to ex(
        "Langhantel-Überzüge",
        "Liege auf der Bank und halte die Stange mit engem Griff über der Brust. Senke sie bogenförmig hinter den Kopf und ziehe sie über die Brust zurück."
    ),
    "barbell_push_presses" to ex(
        "Langhantel-Push Press",
        "Halte die Stange an den Schultern mit leicht gebeugten Knien. Gehe kurz in die Beuge und drücke die Stange mit Beinhilfe über Kopf."
    ),
    "barbell_rack_pulls" to ex(
        "Langhantel-Rack Pulls",
        "Lege die Stange auf Hüfthöhe im Rack ab. Greife sie und drücke dich mit flachem Rücken über die Fersen in den Stand, dann wieder auf die Ablagen ab."
    ),
    "barbell_reverse_curls" to ex(
        "Reverse-Curls mit Langhantel",
        "Halte die Stange im Obergriff (Handflächen nach unten). Curle sie zu den Schultern, die Ellbogen bleiben seitlich, und senke langsam ab."
    ),
    "barbell_reverse_wrist_curls" to ex(
        "Handgelenk-Reverse-Curls mit Langhantel",
        "Sitze mit den Unterarmen auf den Oberschenkeln und den Handflächen nach unten. Curle die Handgelenke nach oben und senke langsam ab."
    ),
    "barbell_rollouts" to ex(
        "Langhantel-Rollouts",
        "Knie dich hin, die beladene Stange vor dir. Rolle sie nach vorne, der Rumpf ist angespannt und der untere Rücken flach, und ziehe sie über die Bauchmuskeln zurück."
    ),
    "barbell_romanian_deadlifts" to ex(
        "Rumänisches Kreuzheben mit Langhantel",
        "Stehe mit der Stange an der Hüfte. Schiebe die Hüfte zurück und führe die Stange mit leicht gebeugten Knien an den Oberschenkeln ab, bis du eine Dehnung im Beinbeuger spürst, dann zurück."
    ),
    "barbell_rows" to ex(
        "Langhantel-Rudern",
        "Beuge dich mit flachem Rücken vor, die Stange hängt auf Armlänge. Ziehe sie zu den unteren Rippen, ziehe die Schulterblätter zusammen, und senke kontrolliert ab."
    ),
    "barbell_shrugs" to ex(
        "Shrugs mit Langhantel",
        "Stehe aufrecht und halte die Stange auf Armlänge. Ziehe die Schultern Richtung Ohren, halte kurz und senke langsam ab."
    ),
    "barbell_side_bends" to ex(
        "Seitneigen mit Langhantel",
        "Halte die Stange auf den Schultern oder seitlich. Neige dich in der Taille zur Seite, richte dich kontrolliert auf und wechsle die Seite."
    ),
    "barbell_side_lunges" to ex(
        "Seitliche Ausfallschritte mit Langhantel",
        "Halte die Stange auf dem oberen Rücken. Mache einen Schritt zur Seite und beuge das Knie, während das andere Bein gestreckt bleibt, dann zurück zur Mitte."
    ),
    "barbell_sit_ups" to ex(
        "Sit-ups mit Langhantel",
        "Liege auf dem Rücken und halte die Stange an der Brust oder über Kopf. Richte dich über das Einrollen des Oberkörpers auf und senke kontrolliert ab."
    ),
    "barbell_skullcrushers" to ex(
        "Langhantel-Skullcrushers",
        "Liege auf der Bank und halte die Stange mit engem Griff über der Brust. Senke sie mit gebeugten Ellbogen zur Stirn und strecke die Arme wieder."
    ),
    "barbell_spider_curls" to ex(
        "Spider Curls mit Langhantel",
        "Liege bäuchlings auf einer Schrägbank und halte die Stange. Curle sie zu den Schultern, die Oberarme hängen senkrecht, und senke langsam ab."
    ),
    "barbell_squats" to ex(
        "Langhantel-Kniebeugen",
        "Lege die Stange auf den oberen Rücken. Gehe in die Hocke, bis die Oberschenkel mindestens waagerecht sind, die Brust bleibt hoch und die Knie zeigen über die Zehen, dann drücke dich hoch."
    ),
    "barbell_step_ups" to ex(
        "Step-ups mit Langhantel",
        "Halte die Stange auf dem Rücken. Steige auf eine Bank, drücke dich über die vordere Ferse hoch, senke dich ab und wechsle das Bein."
    ),
    "barbell_strict_presses" to ex(
        "Langhantel-Strict Press",
        "Stehe mit der Stange am Schlüsselbein und ohne Beinhilfe. Drücke sie nur aus Schultern und Armen über Kopf bis zur Streckung und senke kontrolliert ab."
    ),
    "barbell_windmills" to ex(
        "Windmühlen mit Langhantel",
        "Halte die Stange mit einem gestreckten Arm über Kopf. Drehe die Hüfte zurück und führe die freie Hand am Bein abwärts, dann zurück in den Stand."
    ),
    "barbell_wrist_curls" to ex(
        "Handgelenk-Curls mit Langhantel",
        "Sitze mit den Unterarmen auf den Oberschenkeln und den Handflächen nach oben. Curle die Handgelenke hoch und senke langsam ab."
    ),
    "close_grip_barbell_presses" to ex(
        "Bankdrücken im engen Griff",
        "Liege auf der Bank, die Hände etwa schulterbreit. Senke die Stange mit eng am Körper geführten Ellbogen zur unteren Brust und drücke sie hoch."
    ),
    "decline_barbell_presses" to ex(
        "Langhantel-Drücken auf der Negativbank",
        "Liege mit der Stange über der unteren Brust auf der Negativbank. Senke sie zur unteren Brust und drücke sie bis zur Streckung hoch."
    ),
    "incline_barbell_presses" to ex(
        "Langhantel-Drücken auf der Schrägbank",
        "Liege mit der Stange über der oberen Brust auf der Schrägbank. Senke sie zur oberen Brust und drücke sie bis zur Streckung hoch."
    ),
    "landmine_presses" to ex(
        "Landmine Presses",
        "Stecke ein Ende der Stange in eine Landmine-Ecke. Drücke das andere Ende aus Schulterhöhe in leicht diagonaler Bahn über Kopf und senke wieder ab."
    ),
    "landmine_rainbows" to ex(
        "Landmine Rainbows",
        "Halte das beladene Ende der Landmine über Kopf. Schwinge sie in einem weiten Bogen von Seite zu Seite, der Rumpf bleibt dabei fest."
    ),
    "landmine_twists" to ex(
        "Landmine Twists",
        "Halte das beladene Ende der Landmine an der Brust. Rotiere den Oberkörper Seite für Seite, die Hüfte zeigt nach vorne."
    ),
    "military_presses" to ex(
        "Military Press",
        "Stehe oder sitze mit der Stange am Schlüsselbein. Drücke sie über Kopf bis zur Streckung, der Rumpf bleibt fest."
    ),
    "stiff_leg_barbell_deadlifts" to ex(
        "Kreuzheben mit gestreckten Beinen und Langhantel",
        "Stehe mit der Stange an der Hüfte und fast gestreckten Beinen. Beuge dich aus der Hüfte vor und senke die Stange an den Beinen ab, bis du eine Dehnung spürst, dann richte dich auf."
    ),
    "t_bar_rows" to ex(
        "T-Bar-Rudern",
        "Stelle dich über die T-Bar und greife die Griffe. Ziehe das Gewicht zur Brust, ziehe die Schulterblätter zusammen, und senke kontrolliert ab."
    ),
    "upright_barbell_rows" to ex(
        "Langhantel-Aufrechtes Rudern",
        "Halte die Stange im Obergriff. Ziehe sie mit den Ellbogen führend bis zum Kinn und senke kontrolliert ab."
    ),
    // --- Kabel & Maschinen ---
    "ab_wheels" to ex(
        "Ab Wheels",
        "Knie dich hin und halte das Rad unter den Schultern. Rolle es nach vorne, der Rumpf bleibt angespannt und die Hüfte hängt nicht durch, und ziehe es über das Einrollen der Bauchmuskeln zurück."
    ),
    "abductor_machines" to ex(
        "Abduktoren-Maschine",
        "Setze dich in die Maschine, die Polster außen an den Oberschenkeln. Spreize die Beine gegen die Polster, halte kurz und führe langsam zurück."
    ),
    "adductor_machines" to ex(
        "Adduktoren-Maschine",
        "Setze dich in die Maschine, die Polster innen an den Oberschenkeln. Drücke die Beine gegen die Polster zusammen, halte kurz und löse langsam."
    ),
    "assisted_pull_up_machines" to ex(
        "Klimmzug-Maschine mit Unterstützung",
        "Stelle die Unterstützung so ein, dass du die Wiederholungen schaffst. Greife die Stange mit den Knien auf dem Polster, ziehe die Brust zur Stange und senke dich kontrolliert ab."
    ),
    "bayesian_curls_cable" to ex(
        "Bayesian Curls am Kabel",
        "Stelle dich mit dem Gesicht vom tiefen Zug weg, der Griff in der Hand des hinteren Arms. Curle zur Schulter, der Ellbogen bleibt fixiert, und senke langsam ab."
    ),
    "behind_the_back_cable_lateral_raises" to ex(
        "Seitheben am Kabel hinter dem Rücken",
        "Stehe mit dem Rücken zum tiefen Zug und halte den Griff in einer Hand hinter dir. Hebe den Arm seitlich auf Schulterhöhe und senke ihn wieder ab."
    ),
    "cable_abductions" to ex(
        "Kabel-Abduktion",
        "Befestige eine Fußmanschette am tiefen Zug und stehe seitlich dazu. Führe das äußere Bein gegen den Widerstand seitlich aus und wieder zurück."
    ),
    "cable_adductions" to ex(
        "Kabel-Adduktion",
        "Befestige eine Fußmanschette am tiefen Zug und stehe seitlich dazu. Ziehe das innere Bein gegen den Widerstand über den Körper und wieder zurück."
    ),
    "cable_crossovers" to ex(
        "Kabel-Crossover",
        "Stelle die Rollen über Schulterhöhe ein und halte je einen Griff. Führe die Hände vor der Brust zusammen, spanne die Brust an, und öffne kontrolliert."
    ),
    "cable_crunches" to ex(
        "Kabel-Crunches",
        "Knie dich vor einen hohen Zug, das Seil am Kopf. Rolle die Ellbogen Richtung Knie, die Hüfte bleibt dabei ruhig, und führe zurück."
    ),
    "cable_curls" to ex(
        "Kabel-Curls",
        "Stehe mit dem Gesicht zum tiefen Zug mit Stange oder Seil. Curle hoch, die Ellbogen bleiben an den Seiten, und senke langsam ab."
    ),
    "cable_face_pulls" to ex(
        "Kabel-Face Pulls",
        "Stelle das Seil auf Gesichtshöhe ein. Ziehe es mit hohen und weiten Ellbogen zum Gesicht und spanne die hintere Schulter und den oberen Rücken an."
    ),
    "cable_flyes" to ex(
        "Kabel-Flys",
        "Stelle die Rollen auf Brusthöhe ein. Führe mit leicht gebeugten Ellbogen die Hände bogenförmig vor der Brust zusammen und öffne kontrolliert."
    ),
    "cable_front_raises" to ex(
        "Kabel-Frontheben",
        "Stehe mit dem Rücken zum tiefen Zug. Hebe den Griff gestreckt vor dir auf Schulterhöhe und senke langsam ab."
    ),
    "cable_kickbacks" to ex(
        "Kabel-Kickbacks",
        "Stelle dich zum tiefen Zug, der Ellbogen ist gebeugt an der Seite. Strecke den Arm gerade nach hinten, spanne den Trizeps an und führe zurück."
    ),
    "cable_knee_raises" to ex(
        "Kabel-Knieheben",
        "Befestige Fußmanschetten am tiefen Zug und häng an einer Stange oder stell dich in ein Kabel-Setup. Ziehe die Knie gegen den Widerstand zur Brust und senke ab."
    ),
    "cable_lateral_raises" to ex(
        "Kabel-Seitheben",
        "Stehe seitlich zum tiefen Zug. Hebe den Griff seitlich auf Schulterhöhe und senke langsam ab."
    ),
    "cable_pallof_presses" to ex(
        "Kabel-Pallof-Press",
        "Stehe seitlich zu einem Zug auf Brusthöhe. Drücke den Griff gerade nach vorne und widerstehe der Rotation, dann führe zurück."
    ),
    "cable_pull_throughs" to ex(
        "Kabel-Pull-throughs",
        "Stehe mit dem Rücken zum tiefen Zug, das Seil zwischen den Beinen. Beuge dich aus der Hüfte und drücke sie dann nach vorne in den aufrechten Stand, das Gesäß fest angespannt."
    ),
    "cable_rear_delt_flyes" to ex(
        "Kabel-Reverse-Flys",
        "Stelle den Zug auf Brusthöhe ein und halte den Griff mit der gegenüberliegenden Hand. Ziehe ihn über den Körper und öffne den Arm seitlich, die hintere Schulter fest angespannt."
    ),
    "cable_rows_rear_delt_focus" to ex(
        "Kabel-Rudern (Fokus hintere Schulter)",
        "Nutze den tiefen Zug mit engem Griff. Ziehe zur oberen Brust mit hohen und weiten Ellbogen, um die hintere Schulter zu betonen."
    ),
    "cable_shrugs" to ex(
        "Kabel-Shrugs",
        "Halte eine Stange am tiefen Zug. Ziehe die Schultern nach oben, halte kurz und senke ab."
    ),
    "cable_side_bends" to ex(
        "Kabel-Seitneigen",
        "Stehe seitlich zum tiefen Zug und halte den Griff an der Seite. Neige dich in der Taille vom Kabel weg und richte dich wieder auf."
    ),
    "cable_woodchoppers" to ex(
        "Kabel-Woodchoppers",
        "Stelle den Zug hoch oder tief ein. Ziehe den Griff diagonal über den Körper und rotiere den Rumpf, dann kontrolliert zurück."
    ),
    "cable_wrist_curls" to ex(
        "Kabel-Handgelenk-Curls",
        "Befestige eine Stange am tiefen Zug und sitze mit den Unterarmen auf den Oberschenkeln. Curle die Handgelenke hoch und senke langsam ab."
    ),
    "captain_s_chair_leg_raises" to ex(
        "Beinheben am Captain's Chair",
        "Stütze die Unterarme auf den Polstern des Captain's Chair ab. Hebe die Knie ohne Schwung zur Brust und senke sie langsam ab."
    ),
    "chest_press_machines" to ex(
        "Brustpresse",
        "Setze dich in die Brustpresse, die Griffe auf Brusthöhe. Drücke nach vorne, bis die Arme gestreckt sind, und führe kontrolliert zurück."
    ),
    "chest_supported_machine_rows" to ex(
        "Rudern mit Brustauflage",
        "Sitze mit der Brust am Polster. Ziehe die Griffe zum Oberkörper, ziehe die Schulterblätter zusammen, und strecke kontrolliert aus."
    ),
    "close_grip_lat_pulldowns" to ex(
        "Latzug im engen Griff",
        "Nutze einen engen Griff an der Stange (Handflächen zueinander oder Obergriff). Ziehe die Stange zur oberen Brust und führe die Ellbogen nach unten und hinten."
    ),
    "cross_cable_lateral_raises" to ex(
        "Seitheben an gekreuzten Kabeln",
        "Stelle dich zwischen zwei tiefe Züge und greife die gegenüberliegenden Griffe. Hebe beide Arme seitlich auf Schulterhöhe und senke sie ab."
    ),
    "donkey_calf_raises" to ex(
        "Donkey-Wadenheben",
        "Lehne dich mit abgestütztem Oberkörper nach vorne, ein Gewicht auf dem unteren Rücken. Drücke dich auf die Zehen, halte kurz und senke die Fersen ab."
    ),
    "drag_curls_cable" to ex(
        "Drag Curls am Kabel",
        "Stehe mit dem Gesicht zum tiefen Zug. Curle die Stange dicht am Körper nach oben, die Ellbogen gehen nach hinten, und senke langsam ab."
    ),
    "egyptian_lateral_raises_cable" to ex(
        "Ägyptisches Seitheben am Kabel",
        "Stelle den Zug auf Schulterhöhe ein und starte mit dem Arm quer über dem Körper. Hebe ihn seitlich auf Schulterhöhe und führe zurück."
    ),
    "floor_presses_machine" to ex(
        "Floor Press an der Maschine",
        "Liege unter den Griffen der Maschine auf dem Boden. Drücke nach oben und senke ab, bis die Oberarme den Boden berühren – das begrenzt die Tiefe."
    ),
    "glute_bridge_machines" to ex(
        "Glute-Bridge-Maschine",
        "Setze dich in die Maschine mit den Schultern am Polster. Drücke über die Füße die Hüfte durch, das Gesäß fest angespannt, und führe zurück."
    ),
    "glute_ham_raises" to ex(
        "Glute-Ham-Raises",
        "Fixiere die Fußknöchel an der Glute-Ham-Bank mit den Knien an der Polsterkante. Senke den Oberkörper kontrolliert ab und ziehe dich über Beinbeuger und Gesäß wieder hoch."
    ),
    "glute_kickbacks" to ex(
        "Glute Kickbacks",
        "Stehe oder knie vor einem Kabel oder einer Maschine. Tritt ein Bein gerade nach hinten, spanne das Gesäß oben an und führe zurück."
    ),
    "glute_presses_machine" to ex(
        "Glute-Presse",
        "Setze dich in die Glute-Presse mit den Füßen auf der Plattform. Drücke über die Fersen die Hüfte durch und führe zurück."
    ),
    "good_mornings_machine" to ex(
        "Good Mornings an der Maschine",
        "Lege das Polster der Maschine auf den oberen Rücken. Beuge dich mit flachem Rücken aus der Hüfte vor und richte dich wieder auf."
    ),
    "hack_squats" to ex(
        "Hack Squats",
        "Stelle dich in die Hack-Maschine mit dem Rücken am Polster und den Füßen auf der Plattform. Senke ab, bis die Oberschenkel waagerecht sind, und drücke dich hoch."
    ),
    "hammer_cable_curls" to ex(
        "Hammer-Curls am Kabel",
        "Befestige ein Seil am tiefen Zug. Curle im neutralen Griff hoch, die Handflächen zueinander, und senke langsam ab."
    ),
    "hammer_strength_chest_presses" to ex(
        "Hammer-Strength-Brustpresse",
        "Setze dich in die Hammer-Strength-Maschine, die separat beladenen Griffe auf Brusthöhe. Drücke nach vorne bis zur Streckung und führe die Arme kontrolliert zurück."
    ),
    "hammer_strength_rows" to ex(
        "Hammer-Strength-Rudern",
        "Sitze in der Maschine mit der Brust am Polster. Ziehe die Griffe zu dir, ziehe die Schulterblätter zusammen, und strecke wieder aus."
    ),
    "hammer_strength_shoulder_presses" to ex(
        "Hammer-Strength-Schulterpresse",
        "Sitze in der Hammer-Strength-Maschine, die Griffe auf Schulterhöhe. Drücke über Kopf bis zur Streckung und senke kontrolliert ab."
    ),
    "high_cable_curls" to ex(
        "Curls am hohen Zug",
        "Stelle den Zug über Schulterhöhe ein und curle aus diesem Winkel. Der Ellbogen bleibt fixiert, dann senke langsam ab."
    ),
    "hise_shrugs_machine" to ex(
        "Hise Shrugs an der Maschine",
        "Setze dich in die Beinpresse mit den Schultern unter den Schulterpolstern. Ziehe die Schultern gegen die Last hoch und senke ab."
    ),
    "lateral_cable_leg_raises" to ex(
        "Seitliches Beinheben am Kabel",
        "Befestige eine Fußmanschette am tiefen Zug. Hebe das äußere Bein seitlich an und senke es langsam ab."
    ),
    "lat_pulldowns" to ex(
        "Latzug",
        "Sitze an der Maschine und greife die Stange breiter als schulterbreit. Ziehe sie zur oberen Brust und führe die Ellbogen nach unten und hinten, dann langsam zurück."
    ),
    "leg_curls" to ex(
        "Beinbeuger",
        "Liege bäuchlings auf der Beinbeuger-Maschine. Curle die Fersen zum Gesäß, halte kurz und senke langsam ab."
    ),
    "leg_extensions" to ex(
        "Beinstrecker",
        "Sitze in der Maschine mit dem Polster an den Schienbeinen. Strecke die Beine vollständig, spanne den Quadrizeps an, und senke langsam ab."
    ),
    "leg_press_calf_raises" to ex(
        "Wadenheben an der Beinpresse",
        "Setze die Fußballen auf die Kante der Plattform. Drücke über die Zehen hoch und senke die Fersen für eine tiefe Dehnung ab."
    ),
    "leg_presses" to ex(
        "Beinpresse",
        "Sitze in der Beinpresse, die Füße schulterbreit auf der Plattform. Senke die Plattform ab, bis die Knie fast an der Brust sind, und drücke sie hoch, ohne die Knie durchzustrecken."
    ),
    "low_cable_flyes" to ex(
        "Low Cable Flys",
        "Stelle die Rollen auf die niedrigste Höhe. Führe mit leicht gebeugten Ellbogen die Hände vor der Brust nach oben zusammen und drücke die Brust zusammen."
    ),
    "machine_crunches" to ex(
        "Maschinen-Crunches",
        "Setze dich in die Crunch-Maschine mit dem Polster an der Brust. Rolle den Oberkörper ein und führe langsam zurück."
    ),
    "machine_curls" to ex(
        "Maschinen-Curls",
        "Sitze in der Curl-Maschine mit den Armen auf dem Polster. Curle die Griffe hoch und senke langsam ab."
    ),
    "machine_dips" to ex(
        "Maschinen-Dips",
        "Nutze eine Dip-Station oder eine unterstützte Dip-Maschine. Senke dich ab, bis die Schultern unter den Ellbogen sind, und drücke dich wieder hoch."
    ),
    "machine_flyes" to ex(
        "Maschinen-Flys",
        "Sitze im Pec Deck oder in der Fly-Maschine. Führe die Polster vor der Brust zusammen, spanne die Brust an, und öffne langsam."
    ),
    "machine_lateral_raises" to ex(
        "Maschinen-Seitheben",
        "Sitze in der Seitheben-Maschine mit den Armen auf den Polstern. Hebe die Arme seitlich auf Schulterhöhe und senke sie langsam ab."
    ),
    "machine_rows" to ex(
        "Maschinen-Rudern",
        "Sitze in der Ruder-Maschine mit der Brust am Polster. Ziehe die Griffe zum Oberkörper, ziehe die Schulterblätter zusammen, und strecke aus."
    ),
    "machine_shrugs" to ex(
        "Maschinen-Shrugs",
        "Stelle die Schulterpolster der Maschine ein. Ziehe die Schultern nach oben, halte kurz und senke ab."
    ),
    "overhead_cable_extensions" to ex(
        "Überkopf-Trizepsdrücken am Kabel",
        "Stehe mit dem Rücken zum hohen Zug und halte das Seil hinter dem Kopf. Strecke die Arme über Kopf und senke langsam ab."
    ),
    "pec_decks" to ex(
        "Pec Deck",
        "Sitze im Pec Deck mit den Unterarmen an den Polstern. Führe die Arme vor der Brust zusammen, spanne die Brust an, und öffne."
    ),
    "pendulum_squats" to ex(
        "Pendulum Squats",
        "Setze die Füße auf die Plattform der Pendulum-Maschine. Senke ab, bis die Oberschenkel waagerecht sind, und drücke dich hoch."
    ),
    "rear_delt_decks" to ex(
        "Rear Delt Deck",
        "Setze dich mit dem Gesicht zur Maschine und den Armen vor dir. Drücke die Arme nach hinten und außen und spanne die hintere Schulter an."
    ),
    "reverse_hypers" to ex(
        "Reverse Hypers",
        "Liege bäuchlings auf der Bank mit der Hüfte an der Kante. Hebe die gestreckten Beine hinter dir hoch, Gesäß und unterer Rücken fest angespannt, und senke ab."
    ),
    "reverse_pec_decks" to ex(
        "Reverse Pec Deck",
        "Setze dich mit dem Rücken zur Maschine und den Armen vorne. Ziehe die Arme nach hinten und außen und spanne die hintere Schulter an."
    ),
    "rope_crunches" to ex(
        "Seil-Crunches",
        "Knie dich vor einen hohen Zug und halte die Seilenden am Kopf. Rolle die Ellbogen zu den Knien und ziehe das Seil leicht auseinander, die Hüfte bleibt ruhig, dann führe zurück."
    ),
    "rope_pushdowns" to ex(
        "Seil-Trizepsdrücken",
        "Stelle dich mit einem Seil an einen hohen Zug. Drücke das Seil nach unten, bis die Arme gestreckt sind, und teile die Enden unten, dann führe zurück."
    ),
    "rotary_torso_machines" to ex(
        "Rumpf-Rotationsmaschine",
        "Setze dich in die Maschine und halte die Griffe an der Brust. Rotiere den Oberkörper kontrolliert von Seite zu Seite."
    ),
    "seated_abduction_machines" to ex(
        "Sitzende Abduktionsmaschine",
        "Setze dich in die Maschine mit den Polstern außen an den Oberschenkeln. Spreize die Beine gegen die Polster, halte kurz und führe langsam zurück."
    ),
    "seated_adduction_machines" to ex(
        "Sitzende Adduktionsmaschine",
        "Setze dich in die Maschine mit den Polstern innen an den Oberschenkeln. Drücke die Beine zusammen, halte kurz und löse langsam."
    ),
    "seated_calf_raises" to ex(
        "Sitzendes Wadenheben",
        "Sitze in der Maschine mit den Polstern auf den Knien. Drücke dich auf die Zehen, halte kurz und senke die Fersen für eine Dehnung ab."
    ),
    "seated_leg_curls" to ex(
        "Sitzender Beinbeuger",
        "Sitze in der Maschine mit dem Polster über den Fersen. Curle die Fersen nach unten und hinten zum Gesäß und führe langsam zurück."
    ),
    "seated_rows" to ex(
        "Sitzendes Rudern",
        "Sitze mit abgestützten Füßen und geradem Rücken. Ziehe den Griff zum Bauch, ziehe die Schulterblätter zusammen, und strecke kontrolliert aus."
    ),
    "seated_wide_rows" to ex(
        "Sitzendes Rudern im weiten Griff",
        "Nutze den weiten Griff am sitzenden Rudern. Ziehe ihn mit ausgestellten Ellbogen zur Brust und spanne den oberen Rücken an."
    ),
    "shoulder_press_machines" to ex(
        "Schulterpresse",
        "Sitze in der Maschine mit den Griffen auf Schulterhöhe. Drücke über Kopf bis zur Streckung und senke kontrolliert ab."
    ),
    "side_lying_clamshells_machine" to ex(
        "Clamshell an der Maschine",
        "Liege auf der Seite, die Knie gebeugt, mit Band- oder Maschinenwiderstand. Öffne das obere Knie gegen den Widerstand und schließe wieder."
    ),
    "smith_machine_calf_raises" to ex(
        "Wadenheben in der Smith-Maschine",
        "Lege die Stange auf die Schultern in der Smith-Maschine. Drücke dich auf die Zehen, halte kurz und senke die Fersen ab."
    ),
    "smith_machine_deadlifts" to ex(
        "Kreuzheben in der Smith-Maschine",
        "Stelle die Stange in der Smith-Maschine auf halbe Oberschenkelhöhe ein. Beuge dich mit flachem Rücken aus der Hüfte ab und richte dich wieder auf."
    ),
    "smith_machine_jm_presses" to ex(
        "JM Press in der Smith-Maschine",
        "Liege auf der Bank unter der Smith-Stange. Senke die Stange mit angelegten Ellbogen zur oberen Brust und drücke sie hoch."
    ),
    "smith_machine_presses" to ex(
        "Schulterdrücken in der Smith-Maschine",
        "Sitze oder stehe in der Smith-Maschine mit der Stange an der oberen Brust. Drücke sie über Kopf und senke kontrolliert ab."
    ),
    "smith_machine_rdls" to ex(
        "RDLs in der Smith-Maschine",
        "Stelle die Stange in der Smith-Maschine auf Hüfthöhe ein. Schiebe die Hüfte zurück und führe die Stange an den Oberschenkeln ab, dann zurück."
    ),
    "smith_machine_shrugs" to ex(
        "Shrugs in der Smith-Maschine",
        "Stehe in der Smith-Maschine mit der Stange auf den Schultern. Ziehe die Schultern nach oben, halte kurz und senke ab."
    ),
    "smith_machine_squats" to ex(
        "Kniebeugen in der Smith-Maschine",
        "Lege die Stange in der Smith-Maschine auf den Nacken. Gehe in die Hocke, bis die Oberschenkel waagerecht sind, die Brust bleibt hoch, und drücke dich hoch."
    ),
    "standing_calf_raises" to ex(
        "Stehendes Wadenheben",
        "Stehe mit den Fußballen auf der Kante einer Stufe. Drücke dich auf die Zehen, halte kurz und senke die Fersen unter die Stufe ab."
    ),
    "straight_arm_pulldowns" to ex(
        "Überzüge am Kabel mit gestreckten Armen",
        "Stehe mit gestreckten Armen vor einem hohen Zug. Ziehe die Stange mit den Lats zu den Oberschenkeln, die Arme bleiben gestreckt, und lass sie zurück."
    ),
    "svend_presses_cable" to ex(
        "Svend Presses am Kabel",
        "Stehe mit einem zwischen den Handflächen gepressten Kabelgriff auf Brusthöhe. Drücke nach vorne, spanne die Brust an, und führe zurück."
    ),
    "tricep_pushdowns" to ex(
        "Trizepsdrücken",
        "Stelle dich mit Stange oder Seil an einen hohen Zug. Drücke nach unten, bis die Arme voll gestreckt sind, die Ellbogen bleiben an den Seiten, dann führe zurück."
    ),
    "v_bar_pulldowns" to ex(
        "Latzug am V-Griff",
        "Nutze den V-Griff am Latzug. Ziehe ihn zur oberen Brust und führe die Ellbogen nach unten, dann langsam zurück."
    ),
    "v_bar_pushdowns" to ex(
        "Trizepsdrücken am V-Griff",
        "Nutze den V-Griff an einem hohen Zug. Drücke nach unten, bis die Arme gestreckt sind, die Ellbogen bleiben an den Seiten, dann führe zurück."
    ),
    "v_squat_machines" to ex(
        "V-Squat-Maschine",
        "Setze dich in die V-Squat-Maschine mit den Füßen auf der Plattform. Senke ab, bis die Oberschenkel waagerecht sind, und drücke dich hoch."
    ),
    "wrist_curl_machines" to ex(
        "Handgelenk-Curl-Maschine",
        "Sitze in der Maschine mit den Unterarmen auf dem Polster und den Handflächen nach oben. Curle die Handgelenke hoch und senke langsam ab."
    ),
    "wrist_rollers" to ex(
        "Handgelenk-Roller",
        "Halte einen Handgelenk-Roller mit Gewicht. Rolle das Gewicht durch abwechselnde Handgelenkbewegungen hoch und lass es langsam wieder ab."
    ),
    "y_raises_cable" to ex(
        "Y-Raises am Kabel",
        "Stelle den Zug tief ein und halte den Griff mit gestrecktem Arm. Hebe den Arm in Y-Form nach oben und außen und spanne den oberen Rücken an."
    ),
)
