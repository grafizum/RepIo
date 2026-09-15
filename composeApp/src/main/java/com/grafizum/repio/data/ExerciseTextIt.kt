package com.grafizum.repio.data

/**
 * Italian names and method descriptions for the built-in exercise pool.
 *
 * Keyed by the canonical-name slug from [ExerciseLocalization.slugify] — see
 * [ExerciseLocalization] for why the key is the English name's slug and not the English
 * name itself. `ExerciseLocalizationTest` asserts that this table covers every pool
 * exercise exactly once and that no entry names an exercise outside the pool, so a typo in
 * a key fails the build instead of quietly leaving one exercise in English.
 */
internal val EXERCISE_TEXT_IT: Map<String, ExerciseText> = mapOf(
    // --- Corpo libero ---
    "archer_pull_ups" to ex(
        "Trazioni Archer",
        "Appenditi alla sbarra con una mano vicino all'estremità e l'altra più al centro. Tira su spostando il peso verso il braccio largo, mantenendo l'altro braccio disteso di lato."
    ),
    "bicycle_crunches" to ex(
        "Crunch bicicletta",
        "Sdraiati sulla schiena con le mani dietro la testa. Avvicina un ginocchio al gomito opposto ed estendi l'altra gamba, alternando i lati senza tirare il collo."
    ),
    "bird_dogs" to ex(
        "Bird-Dog",
        "Parti in quadrupedia. Estendi braccio e gamba opposti fino all'altezza del busto, fermati un istante e cambia lato senza far ruotare il bacino."
    ),
    "back_extensions" to ex(
        "Estensioni lombari",
        "Fissa il bacino sul cuscinetto della panca per le estensioni. Fletti il busto in avanti dall'anca e risali usando lombari e glutei."
    ),
    "box_jumps" to ex(
        "Salto sul box",
        "Mettiti davanti a un box stabile. Salta in modo esplosivo e atterra morbido con le ginocchia flesse, poi scendi con controllo."
    ),
    "calf_raises" to ex(
        "Calf raise",
        "Mettiti con le punte dei piedi su un gradino. Sollevati il più possibile sulle punte, fermati e abbassa i talloni sotto il gradino."
    ),
    "chin_ups" to ex(
        "Trazioni a presa supina",
        "Appenditi alla sbarra a presa supina, alla larghezza delle spalle. Porta il mento sopra la sbarra e scendi con controllo."
    ),
    "clamshells" to ex(
        "Clamshell",
        "Sdraiati su un fianco con le ginocchia flesse e i piedi uniti. Apri il ginocchio superiore come una conchiglia, stringi il gluteo in alto e chiudi lentamente."
    ),
    "copenhagen_planks" to ex(
        "Plank di Copenhagen",
        "Sdraiati su un fianco con la gamba superiore su una panca. Attiva il core e solleva il bacino da terra, mantenendo la posizione."
    ),
    "cossack_squats" to ex(
        "Cossack squat",
        "Stai in piedi a gambe larghe con le punte rivolte in avanti. Sposta il peso su una gamba e scendi in uno squat profondo mentre l'altra resta distesa, poi cambia lato."
    ),
    "crunches" to ex(
        "Crunch",
        "Sdraiati sulla schiena con le ginocchia flesse e i piedi a terra. Solleva le spalle da terra premendo la zona lombare, poi scendi lentamente."
    ),
    "dead_hangs" to ex(
        "Sospensione a braccia distese",
        "Appenditi alla sbarra con le braccia distese e le spalle rilassate. Mantieni la posizione per costruire presa e resistenza dei dorsali."
    ),
    "decline_sit_ups_weighted" to ex(
        "Sit-up su panca declinata (con peso)",
        "Fissa i piedi sulla panca declinata e tieni un peso al petto. Arrotola il busto verso l'alto e scendi con controllo."
    ),
    "dips" to ex(
        "Dip alle parallele",
        "Sostieniti sulle parallele con le braccia distese. Scendi flettendo i gomiti finché le spalle superano i gomiti, poi spingi di nuovo su."
    ),
    "dips_chest_focus" to ex(
        "Dip con enfasi sul petto",
        "Sostieniti alle parallele e inclina il busto in avanti. Scendi finché le spalle sono sotto i gomiti mantenendo l'inclinazione, poi spingi su."
    ),
    "dirty_dogs" to ex(
        "Dirty Dog",
        "Parti in quadrupedia con le ginocchia flesse. Solleva un ginocchio di lato come un cane all'idrante, mantenendo il bacino orizzontale, poi abbassalo."
    ),
    "donkey_kicks" to ex(
        "Donkey kick",
        "Parti in quadrupedia. Calcia una gamba in alto e indietro con il ginocchio flesso, stringi il gluteo in alto e scendi."
    ),
    "dragon_flags" to ex(
        "Dragon flag",
        "Sdraiati supino e afferra una panca dietro la testa. Solleva gambe e bacino, poi scendi con tutto il corpo disteso senza toccare terra."
    ),
    "false_grip_holds" to ex(
        "Sospensione a presa falsa",
        "Aggancia i polsi sopra la sbarra con le nocche rivolte verso l'alto. Appenditi o mantieni la posizione per costruire presa e avambracci."
    ),
    "finger_push_ups" to ex(
        "Piegamenti sulle dita",
        "Esegui i piegamenti sulle punte delle dita invece che sui palmi, per rinforzare dita e avambracci, mantenendo il corpo allineato."
    ),
    "fire_hydrants" to ex(
        "Fire hydrant",
        "Parti in quadrupedia. Solleva un ginocchio di lato all'altezza del bacino con la schiena piatta, poi abbassa e cambia lato."
    ),
    "fist_squeezes" to ex(
        "Stretta dei pugni",
        "Stringi i pugni con tutta la forza per qualche secondo, poi rilascia. Ripeti per rinforzare la presa."
    ),
    "frog_pumps" to ex(
        "Frog pump",
        "Sdraiati supino con le piante dei piedi unite e le ginocchia aperte. Spingi il bacino in alto stringendo i glutei, poi scendi."
    ),
    "garhammer_raises" to ex(
        "Garhammer raise",
        "Appenditi alla sbarra. Solleva le gambe tese a circa 45 gradi, poi scendi lentamente senza oscillare."
    ),
    "glute_bridges" to ex(
        "Ponte per glutei",
        "Sdraiati supino con le ginocchia flesse e i piedi a terra. Spingi il bacino in alto stringendo i glutei in cima, poi scendi lentamente."
    ),
    "handstand_holds" to ex(
        "Verticale statica",
        "Entra in verticale contro il muro. Mantieni la posizione con braccia distese, core attivo e punte dei piedi tese."
    ),
    "handstand_shrugs" to ex(
        "Shrug in verticale",
        "In verticale al muro, solleva le spalle verso le orecchie e poi riabbassale."
    ),
    "hanging_leg_raises" to ex(
        "Sollevamento gambe alla sbarra",
        "Appenditi alla sbarra. Solleva le gambe tese o poco flesse all'altezza del bacino o più in alto senza oscillare, poi scendi lentamente."
    ),
    "hanging_side_raises" to ex(
        "Sollevamento laterale alla sbarra",
        "Appenditi alla sbarra. Solleva le gambe di lato, prima da un lato e poi dall'altro, con controllo e senza oscillazioni."
    ),
    "hollow_body_holds" to ex(
        "Hollow body hold",
        "Sdraiati supino e solleva gambe e spalle da terra, premendo la zona lombare a terra. Mantieni la posizione."
    ),
    "inverted_curls" to ex(
        "Curl inverso alla sbarra",
        "Afferra una sbarra o degli anelli a presa supina con il corpo inclinato sotto. Porta il petto alla sbarra flettendo i gomiti, poi scendi."
    ),
    "inverted_rows" to ex(
        "Rematore inverso",
        "Metti una sbarra all'altezza del bacino e appenditi sotto con il corpo disteso. Porta il petto alla sbarra stringendo la schiena, poi scendi lentamente."
    ),
    "jumping_jacks" to ex(
        "Jumping jack",
        "Stai in piedi con i piedi uniti e le braccia lungo i fianchi. Salta aprendo le gambe e portando le braccia sopra la testa, poi torna alla posizione iniziale."
    ),
    "l_sits" to ex(
        "L-sit",
        "Sostieniti alle parallele o premi i palmi a terra. Solleva le gambe tese in avanti, mantenendole parallele al suolo."
    ),
    "l_sit_pull_ups" to ex(
        "Trazioni in L-sit",
        "Appenditi alla sbarra e solleva le gambe in posizione a L. Tira su mantenendo la L e scendi con controllo."
    ),
    "lateral_plank_walks" to ex(
        "Camminata in plank laterale",
        "Mantieni un plank laterale sull'avambraccio. Fai passare avanti e indietro la gamba superiore senza far cadere il bacino."
    ),
    "leg_raises" to ex(
        "Sollevamento gambe",
        "Sdraiati supino con le gambe tese. Sollevale verso il soffitto senza inarcare la zona lombare, poi scendi lentamente."
    ),
    "lunges" to ex(
        "Affondi",
        "Stai in piedi, fai un passo avanti e scendi finché entrambe le ginocchia non formano circa 90 gradi. Spingi per tornare in piedi e cambia gamba."
    ),
    "mountain_climbers" to ex(
        "Mountain climber",
        "Parti in plank. Porta rapidamente un ginocchio al petto e alterna le gambe mantenendo il bacino basso."
    ),
    "muscle_ups_bw" to ex(
        "Muscle-up",
        "Tira in modo esplosivo finché il petto supera la sbarra, poi spingi sopra la sbarra in un dip, chiudendo con le braccia distese."
    ),
    "nordic_curls" to ex(
        "Nordic curl",
        "Inginocchiati con le caviglie fissate. Scendi con il busto lentamente e con controllo, resistendo con i femorali, poi tirati su."
    ),
    "plank_to_down_dogs" to ex(
        "Plank to down dog",
        "Dal plank spingi il bacino in alto e indietro nel cane a testa in giù, poi torna in plank. Muoviti con controllo."
    ),
    "planks" to ex(
        "Plank",
        "Sostieniti su avambracci e punte dei piedi. Mantieni il corpo in linea retta, attiva il core e non far cadere il bacino."
    ),
    "pike_holds" to ex(
        "Pike hold",
        "Dalla posizione di piegamento porta i piedi vicini e solleva il bacino in posizione di pike. Mantieni con gambe e braccia distese."
    ),
    "pull_ups" to ex(
        "Trazioni alla sbarra",
        "Appenditi alla sbarra a presa prona, poco più larga delle spalle. Porta il petto alla sbarra e scendi con controllo."
    ),
    "push_ups" to ex(
        "Piegamenti sulle braccia",
        "Parti in plank alto con le mani sotto le spalle. Scendi con il petto verso terra e spingi di nuovo su, mantenendo il corpo allineato."
    ),
    "reverse_snow_angels" to ex(
        "Snow angel inverso",
        "Sdraiati prono e solleva leggermente braccia e gambe da terra. Muovi le braccia dall'alto verso i fianchi come un angelo nella neve, senza appoggiarle."
    ),
    "russian_twists" to ex(
        "Russian twist",
        "Siediti con le ginocchia flesse e inclinati leggermente indietro. Ruota il busto da un lato all'altro con il petto alto e il core attivo."
    ),
    "scapular_pull_ups" to ex(
        "Trazioni scapolari",
        "Appenditi alla sbarra con le braccia distese. Deprimi e avvicina le scapole per sollevarti di qualche centimetro, poi rilascia."
    ),
    "scapular_push_ups" to ex(
        "Piegamenti scapolari",
        "Parti in plank. Avvicina e allontana le scapole senza flettere i gomiti."
    ),
    "shoulder_taps" to ex(
        "Tocco spalle",
        "Parti in plank alto. Tocca con una mano la spalla opposta, alternando, senza muovere il bacino."
    ),
    "side_lying_leg_raises" to ex(
        "Sollevamento gamba su un fianco",
        "Sdraiati su un fianco con le gambe tese. Solleva la gamba superiore tesa verso il soffitto e scendi lentamente."
    ),
    "side_plank_crunches" to ex(
        "Crunch in plank laterale",
        "Mantieni un plank laterale sull'avambraccio. Avvicina gomito e ginocchio superiori, poi riestendi."
    ),
    "side_plank_leg_raises" to ex(
        "Sollevamento gamba in plank laterale",
        "Mantieni un plank laterale sull'avambraccio. Solleva e abbassa la gamba superiore mantenendo il bacino stabile."
    ),
    "side_plank_rotations" to ex(
        "Rotazione in plank laterale",
        "Mantieni un plank laterale sull'avambraccio. Ruota il busto e passa il braccio superiore sotto il corpo, poi riapri."
    ),
    "side_planks" to ex(
        "Plank laterale",
        "Sostieniti su un avambraccio con i piedi sovrapposti. Solleva il bacino finché il corpo è in linea retta e mantieni la posizione."
    ),
    "side_steps" to ex(
        "Passi laterali",
        "Metti un elastico attorno alle cosce e stai con i piedi alla larghezza dei fianchi. Fai passi laterali mantenendo la tensione, poi torna indietro."
    ),
    "sissy_squats" to ex(
        "Sissy squat",
        "Tieniti a un sostegno e inclinati leggermente indietro. Scendi flettendo le ginocchia con il busto inclinato all'indietro, poi risali."
    ),
    "sliding_leg_curls" to ex(
        "Leg curl con slider",
        "Sdraiati supino con i talloni su slider o asciugamani. Fai scivolare i piedi verso i glutei, poi riestendi."
    ),
    "split_squats" to ex(
        "Split squat",
        "Un piede avanti e l'altro dietro. Scendi in verticale finché il ginocchio posteriore non sfiora terra, poi spingi sul piede anteriore."
    ),
    "squats" to ex(
        "Squat",
        "Stai in piedi a piedi larghezza spalle. Abbassa il bacino come per sederti, poi spingi sui talloni per rialzarti."
    ),
    "step_ups" to ex(
        "Step-up",
        "Mettiti davanti a una panca. Sali con un piede spingendo sul tallone, poi scendi e cambia gamba."
    ),
    "sumo_squats" to ex(
        "Sumo squat",
        "Stai a gambe larghe con le punte rivolte verso l'esterno. Scendi tra le gambe con il petto alto e le ginocchia in linea con le punte, poi spingi su."
    ),
    "superman_extensions" to ex(
        "Superman extension",
        "Sdraiati prono e solleva braccia, petto e gambe da terra. Fermati un istante e scendi lentamente."
    ),
    "supermans" to ex(
        "Superman",
        "Sdraiati prono con le braccia distese. Solleva contemporaneamente braccia, petto e gambe, fermati un istante e scendi."
    ),
    "tibialis_raises" to ex(
        "Tibiali",
        "Siediti con i talloni fissati e un peso sulle punte, oppure stai con la schiena al muro. Porta le punte verso gli stinchi e scendi lentamente."
    ),
    "tip_toe_walks" to ex(
        "Camminata sulle punte",
        "Cammina sulle punte per una distanza o un tempo definiti per rinforzare i polpacci."
    ),
    "towel_hangs" to ex(
        "Sospensione all'asciugamano",
        "Avvolgi un asciugamano sulla sbarra per ogni mano e appenditi. Mantieni la posizione per rinforzare la presa."
    ),
    "underhand_inverted_rows" to ex(
        "Rematore inverso a presa supina",
        "Metti una sbarra all'altezza del bacino e appenditi sotto a presa supina. Porta il petto alla sbarra e scendi lentamente."
    ),
    "v_ups" to ex(
        "V-up",
        "Sdraiati supino con le braccia sopra la testa. Solleva insieme gambe e busto formando una V, poi scendi con controllo."
    ),
    "wall_sits" to ex(
        "Wall sit",
        "Fai scivolare la schiena lungo il muro finché le cosce sono parallele al suolo. Mantieni con le ginocchia a 90 gradi."
    ),
    "wall_slides" to ex(
        "Wall slide",
        "Stai con la schiena al muro e le braccia piegate a 90 gradi. Fai scorrere le braccia su e giù mantenendole aderenti al muro."
    ),
    "weighted_dips" to ex(
        "Dip con sovraccarico",
        "Esegui i dip con un manubrio tra i piedi o con una cintura zavorrata. Scendi finché le spalle sono sotto i gomiti, poi spingi su."
    ),
    "wide_grip_pull_ups" to ex(
        "Trazioni a presa larga",
        "Appenditi alla sbarra con presa larga. Porta il petto verso la sbarra e scendi con controllo."
    ),
    "windshield_wipers" to ex(
        "Tergicristallo",
        "Sdraiati supino con le braccia aperte e le gambe sollevate. Abbassa le gambe da un lato all'altro come un tergicristallo, tenendo le spalle a terra."
    ),
    "y_w_t_raises" to ex(
        "Y-W-T raise",
        "Sdraiati prono e forma con le braccia le lettere Y, W e T, sollevando ogni posizione da terra per lavorare la parte alta della schiena."
    ),

    // --- Manubri ---
    "arnold_presses" to ex(
        "Arnold press",
        "Tieni i manubri all'altezza delle spalle con i palmi verso di te. Spingi in alto ruotando i palmi verso l'esterno finché le braccia sono distese; in discesa ruota al contrario."
    ),
    "concentration_curls" to ex(
        "Curl di concentrazione",
        "Siediti con il gomito appoggiato all'interno della coscia. Fletti il manubrio verso la spalla senza muovere il braccio e scendi lentamente."
    ),
    "decline_dumbbell_presses" to ex(
        "Panca declinata con manubri",
        "Sdraiati su una panca declinata con un manubrio per mano. Scendi verso la parte bassa del petto e spingi in alto finché le braccia sono distese."
    ),
    "dumbbell_back_extensions" to ex(
        "Estensioni lombari con manubrio",
        "Tieni un manubrio al petto sulla panca per le estensioni. Fletti il busto in avanti dall'anca e risali con lombari e glutei."
    ),
    "dumbbell_bench_presses" to ex(
        "Panca piana con manubri",
        "Sdraiati sulla panca piana con un manubrio per mano all'altezza del petto. Spingi in alto finché le braccia sono distese, poi scendi con controllo."
    ),
    "dumbbell_calf_raises" to ex(
        "Calf raise con manubri",
        "Tieni i manubri ai fianchi con le punte dei piedi su un gradino. Sollevati sulle punte, fermati e abbassa i talloni sotto il gradino."
    ),
    "dumbbell_clean_presses" to ex(
        "Clean & press con manubri",
        "Porta un manubrio da terra o dall'alto in modo esplosivo sulla spalla, poi spingilo sopra la testa finché il braccio è disteso."
    ),
    "dumbbell_crunches" to ex(
        "Crunch con manubrio",
        "Sdraiati supino tenendo un manubrio al petto. Arrotola le spalle verso l'alto premendo la zona lombare a terra, poi scendi."
    ),
    "dumbbell_curls" to ex(
        "Curl con manubri",
        "Stai in piedi con i manubri ai fianchi e i palmi rivolti in avanti. Fletti verso le spalle tenendo i gomiti aderenti ai fianchi, poi scendi lentamente."
    ),
    "dumbbell_deadlifts" to ex(
        "Stacco da terra con manubri",
        "Stai con i manubri davanti alle cosce. Fletti il busto dall'anca con la schiena piatta e scendi oltre le ginocchia, poi risali in piedi."
    ),
    "dumbbell_face_pulls" to ex(
        "Face pull con manubri",
        "Tieni manubri leggeri e solleva le braccia ai lati con i pollici in alto, poi fletti i gomiti portando i pesi verso il viso e stringendo le spalle posteriori."
    ),
    "dumbbell_floor_presses" to ex(
        "Floor press con manubri",
        "Sdraiati a terra con un manubrio per mano al petto. Spingi in alto e scendi finché gli omeri toccano il pavimento."
    ),
    "dumbbell_flyes" to ex(
        "Croci con manubri",
        "Sdraiati sulla panca con i manubri sopra il petto, i palmi verso l'interno e i gomiti leggermente flessi. Apri le braccia in un arco ampio, poi stringi il petto per riunirle."
    ),
    "dumbbell_front_raises" to ex(
        "Alzate frontali con manubri",
        "Stai con i manubri davanti alle cosce. Sollevali distesi davanti a te fino all'altezza delle spalle, poi scendi lentamente."
    ),
    "dumbbell_glute_bridges" to ex(
        "Ponte per glutei con manubrio",
        "Sdraiati supino con le ginocchia flesse e un manubrio sul bacino. Spingi il bacino in alto stringendo i glutei, poi scendi."
    ),
    "dumbbell_good_mornings" to ex(
        "Good morning con manubri",
        "Tieni i manubri sulle spalle o lungo le cosce. Fletti il busto in avanti dall'anca con la schiena piatta e le ginocchia leggermente flesse, poi risali."
    ),
    "dumbbell_hammer_curls" to ex(
        "Hammer curl con manubri",
        "Tieni i manubri con i palmi rivolti l'uno verso l'altro. Fletti verso le spalle mantenendo la presa neutra, poi scendi lentamente."
    ),
    "dumbbell_hip_thrusts" to ex(
        "Hip thrust con manubrio",
        "Siediti con la parte alta della schiena su una panca e un manubrio sul bacino. Spingi il bacino in alto stringendo i glutei, poi scendi."
    ),
    "dumbbell_kickbacks" to ex(
        "Kickback con manubrio",
        "Fletti il busto in avanti con un braccio al fianco e il gomito flesso. Estendi il braccio dritto all'indietro stringendo il tricipite, poi torna."
    ),
    "dumbbell_lateral_raises" to ex(
        "Alzate laterali con manubri",
        "Stai con i manubri ai fianchi e i gomiti leggermente flessi. Solleva le braccia ai lati fino all'altezza delle spalle, poi scendi lentamente."
    ),
    "dumbbell_leg_curls" to ex(
        "Leg curl con manubrio",
        "Sdraiati prono o siediti tenendo un manubrio tra i piedi. Fletti i talloni verso i glutei, poi scendi lentamente."
    ),
    "dumbbell_leg_raises" to ex(
        "Sollevamento gambe con manubrio",
        "Sdraiati supino tenendo un manubrio tra i piedi o nelle mani. Solleva le gambe tese, poi scendi con controllo."
    ),
    "dumbbell_lunges" to ex(
        "Affondi con manubri",
        "Tieni i manubri ai fianchi. Fai un passo avanti e scendi finché entrambe le ginocchia formano circa 90 gradi, poi torna in piedi e cambia gamba."
    ),
    "dumbbell_overhead_extensions" to ex(
        "Estensioni sopra la testa con manubrio",
        "Tieni un manubrio sopra la testa con entrambe le mani (o una sola). Scendilo dietro la testa flettendo i gomiti, poi spingi di nuovo su."
    ),
    "dumbbell_pinch_grips" to ex(
        "Pinch grip con manubrio",
        "Pizzica un manubrio sul disco superiore con dita e pollice. Tienilo al fianco a tempo per rinforzare la presa."
    ),
    "dumbbell_pullovers" to ex(
        "Pullover con manubrio",
        "Sdraiati sulla panca tenendo un manubrio con entrambe le mani sopra il petto. Scendilo dietro la testa in un arco, poi riportalo sopra il petto."
    ),
    "dumbbell_rear_delt_flyes" to ex(
        "Croci posteriori con manubri",
        "Fletti il busto in avanti con la schiena piatta. Con i gomiti leggermente flessi solleva i manubri ai lati stringendo le spalle posteriori."
    ),
    "dumbbell_reverse_curls" to ex(
        "Curl inverso con manubri",
        "Tieni i manubri a presa prona (palmi verso il basso). Fletti verso le spalle tenendo i gomiti ai fianchi, poi scendi lentamente."
    ),
    "dumbbell_romanian_deadlifts" to ex(
        "Stacco rumeno con manubri",
        "Stai con i manubri davanti alle cosce. Spingi il bacino indietro e fai scendere i pesi lungo le gambe con le ginocchia poco flesse finché senti l'allungamento dei femorali, poi risali."
    ),
    "dumbbell_rows" to ex(
        "Rematore con manubrio",
        "Fletti il busto in avanti appoggiando una mano su una panca e il piede opposto a terra. Tira il manubrio verso il fianco con la schiena piatta, poi scendi con controllo."
    ),
    "dumbbell_russian_twists" to ex(
        "Russian twist con manubrio",
        "Siediti con le ginocchia flesse, un manubrio al petto e il busto leggermente indietro. Ruota il busto da un lato all'altro toccando terra con il peso a ogni lato."
    ),
    "dumbbell_shoulder_presses" to ex(
        "Spinte con manubri",
        "Tieni i manubri all'altezza delle spalle con i palmi in avanti. Spingi sopra la testa finché le braccia sono distese, poi scendi con controllo."
    ),
    "dumbbell_shrugs" to ex(
        "Shrug con manubri",
        "Tieni i manubri ai fianchi con le braccia distese. Solleva le spalle verso le orecchie, fermati e scendi."
    ),
    "dumbbell_side_bends" to ex(
        "Flessioni laterali con manubrio",
        "Tieni un manubrio in una mano al fianco. Piega il busto verso il peso, poi torna su e cambia lato."
    ),
    "dumbbell_side_lunges" to ex(
        "Affondi laterali con manubri",
        "Tieni i manubri al petto o ai fianchi. Fai un passo di lato flettendo quel ginocchio mentre l'altra gamba resta tesa, poi torna al centro."
    ),
    "dumbbell_skullcrushers" to ex(
        "Skullcrusher con manubri",
        "Sdraiati sulla panca con i manubri sopra il petto. Scendili verso la testa flettendo i gomiti, poi riestendi."
    ),
    "dumbbell_split_squats" to ex(
        "Split squat con manubri",
        "Tieni i manubri ai fianchi con un piede appoggiato dietro su una panca. Scendi in verticale finché il ginocchio posteriore sfiora terra, poi spingi su."
    ),
    "dumbbell_squats" to ex(
        "Squat con manubri",
        "Tieni i manubri ai fianchi o alle spalle. Scendi in squat finché le cosce sono almeno parallele al suolo, petto alto, poi spingi su."
    ),
    "dumbbell_step_ups" to ex(
        "Step-up con manubri",
        "Tieni i manubri ai fianchi. Sali su una panca spingendo sul tallone, poi scendi e cambia gamba."
    ),
    "dumbbell_thrusters" to ex(
        "Thruster con manubri",
        "Tieni i manubri alle spalle. Scendi in squat e spingi su portando i manubri sopra la testa in un movimento continuo."
    ),
    "dumbbell_v_ups" to ex(
        "V-up con manubrio",
        "Sdraiati supino tenendo un manubrio sopra la testa. Solleva insieme gambe e busto formando una V, poi scendi con controllo."
    ),
    "dumbbell_windmills" to ex(
        "Windmill con manubrio",
        "Tieni un manubrio sopra la testa con il braccio disteso. Ruota il bacino indietro e fai scendere la mano libera lungo la gamba, poi torna in piedi."
    ),
    "dumbbell_woodchoppers" to ex(
        "Woodchopper con manubrio",
        "Tieni un manubrio con entrambe le mani. Ruota il busto facendolo oscillare in diagonale dall'alto verso il basso (o viceversa) con il core attivo."
    ),
    "dumbbell_wrist_curls" to ex(
        "Curl per polsi con manubri",
        "Siediti con gli avambracci sulle cosce, i palmi verso l'alto e i manubri in mano. Fletti i polsi verso l'alto, poi scendi lentamente."
    ),
    "dumbbell_y_raises" to ex(
        "Y-raise con manubri",
        "Inclinati leggermente in avanti con i manubri ai fianchi. Solleva le braccia in alto e in fuori formando una Y, stringendo la parte alta della schiena."
    ),
    "farmer_s_walks" to ex(
        "Farmer's walk",
        "Tieni un peso pesante in ogni mano con postura eretta e core attivo. Cammina con passi controllati senza far cadere le spalle in avanti."
    ),
    "goblet_squats" to ex(
        "Goblet squat",
        "Tieni un manubrio o un kettlebell al petto. Scendi in uno squat profondo con il petto alto e i gomiti dentro le ginocchia, poi spingi su."
    ),
    "incline_dumbbell_curls" to ex(
        "Curl su panca inclinata",
        "Siediti su una panca inclinata con i manubri che pendono ai lati. Fletti verso l'alto tenendo i gomiti indietro e le spalle ferme, poi scendi lentamente."
    ),
    "incline_dumbbell_presses" to ex(
        "Panca inclinata con manubri",
        "Sdraiati su una panca inclinata con un manubrio per mano alla parte alta del petto. Spingi in alto fino a distendere le braccia, poi scendi con controllo."
    ),
    "preacher_curls" to ex(
        "Curl alla panca Scott",
        "Siediti alla panca Scott con gli omeri sul cuscinetto. Fletti il bilanciere o i manubri verso l'alto e scendi finché le braccia sono quasi distese."
    ),
    "rear_delt_floor_flyes" to ex(
        "Croci posteriori a terra",
        "Sdraiati prono a terra con manubri leggeri. Solleva le braccia ai lati stringendo le spalle posteriori, poi scendi."
    ),
    "spider_curls" to ex(
        "Spider curl",
        "Sdraiati prono su una panca inclinata con i manubri. Fletti verso le spalle con gli omeri perpendicolari al suolo, poi scendi lentamente."
    ),
    "stiff_leg_dumbbell_deadlifts" to ex(
        "Stacco a gambe tese con manubri",
        "Stai con i manubri davanti alle cosce e le gambe quasi tese. Fletti il busto in avanti facendo scendere i pesi lungo le gambe, poi risali."
    ),
    "zottman_curls" to ex(
        "Zottman curl",
        "Fletti i manubri con i palmi verso l'alto. In alto ruota i palmi verso il basso e scendi lentamente, poi ruota di nuovo per la ripetizione successiva."
    ),
    // --- Bilancieri ---
    "barbell_back_extensions" to ex(
        "Estensioni lombari con bilanciere",
        "Tieni il bilanciere sulla parte alta della schiena sulla panca per le estensioni. Fletti il busto in avanti dall'anca e risali con lombari e glutei."
    ),
    "barbell_bench_presses" to ex(
        "Panca piana con bilanciere",
        "Sdraiati sulla panca con il bilanciere sopra gli occhi e i piedi a terra. Impugna poco più largo delle spalle, scendi al centro del petto e spingi fino a distendere le braccia."
    ),
    "barbell_calf_raises" to ex(
        "Calf raise con bilanciere",
        "Stai con il bilanciere sulle spalle e le punte dei piedi su un gradino. Sollevati sulle punte, fermati e abbassa i talloni sotto il gradino."
    ),
    "barbell_clean_presses" to ex(
        "Clean & press con bilanciere",
        "Porta il bilanciere da terra o dall'alto in modo esplosivo sulle spalle, poi spingilo sopra la testa fino a distendere le braccia."
    ),
    "barbell_crunches" to ex(
        "Crunch con bilanciere",
        "Sdraiati supino tenendo il bilanciere al petto o sopra la testa. Arrotola le spalle da terra mantenendo la zona lombare a terra, poi scendi lentamente."
    ),
    "barbell_curls" to ex(
        "Curl con bilanciere",
        "Stai eretto con il bilanciere a presa supina. Flettilo verso il petto tenendo i gomiti ai fianchi, poi scendi lentamente."
    ),
    "barbell_deadlifts" to ex(
        "Stacco da terra",
        "Stai a piedi larghezza fianchi con il bilanciere sopra il centro del piede. Impugna, attiva il core e spingi sui talloni per alzarti con la schiena piatta, poi scendi con controllo."
    ),
    "barbell_face_pulls" to ex(
        "Face pull con bilanciere",
        "Metti una sbarra o una corda all'altezza del viso. Tira verso il viso con i gomiti alti e larghi, stringendo le spalle posteriori, poi torna."
    ),
    "barbell_floor_presses" to ex(
        "Floor press con bilanciere",
        "Sdraiati a terra con il bilanciere sopra il petto. Scendi finché gli omeri toccano il pavimento, poi spingi su: il pavimento limita l'escursione."
    ),
    "barbell_front_raises" to ex(
        "Alzate frontali con bilanciere",
        "Stai eretto tenendo il bilanciere a presa prona. Sollevalo disteso davanti a te fino all'altezza delle spalle, poi scendi con controllo."
    ),
    "barbell_front_squats" to ex(
        "Front squat",
        "Appoggia il bilanciere davanti alle spalle con i gomiti alti. Scendi in squat con il petto alto e il busto verticale, poi spingi su."
    ),
    "barbell_glute_bridges" to ex(
        "Ponte per glutei con bilanciere",
        "Sdraiati supino con le ginocchia flesse e il bilanciere sul bacino. Spingi il bacino in alto stringendo i glutei finché il corpo è in linea, poi scendi."
    ),
    "barbell_good_mornings" to ex(
        "Good morning con bilanciere",
        "Appoggia il bilanciere sulla parte alta della schiena. Fletti il busto in avanti dall'anca con la schiena piatta e le ginocchia poco flesse, poi risali spingendo il bacino in avanti."
    ),
    "barbell_hip_thrusts" to ex(
        "Hip thrust con bilanciere",
        "Siediti con la parte alta della schiena su una panca e il bilanciere sul bacino. Spingi il bacino in alto stringendo i glutei, poi scendi."
    ),
    "barbell_leg_raises" to ex(
        "Sollevamento gambe con bilanciere",
        "Sdraiati supino tenendo il bilanciere sopra la testa o tra i piedi. Solleva le gambe tese fino alla verticale e scendi lentamente senza inarcare la zona lombare."
    ),
    "barbell_lunges" to ex(
        "Affondi con bilanciere",
        "Tieni il bilanciere sulla parte alta della schiena. Fai un passo avanti e scendi finché entrambe le ginocchia formano circa 90 gradi, poi torna in piedi."
    ),
    "barbell_overhead_extensions" to ex(
        "Estensioni sopra la testa con bilanciere",
        "Tieni il bilanciere sopra la testa a presa stretta. Scendilo dietro la testa flettendo i gomiti, poi spingi su fino a distendere le braccia."
    ),
    "barbell_overhead_presses" to ex(
        "Distensioni sopra la testa con bilanciere",
        "Stai con il bilanciere sulle clavicole. Spingilo sopra la testa fino a distendere le braccia con il core attivo, poi scendi al petto."
    ),
    "barbell_pullovers" to ex(
        "Pullover con bilanciere",
        "Sdraiati sulla panca tenendo il bilanciere a presa stretta sopra il petto. Scendilo dietro la testa in un arco, poi riportalo sopra il petto."
    ),
    "barbell_push_presses" to ex(
        "Push press",
        "Tieni il bilanciere alle spalle con le ginocchia leggermente flesse. Fai un piccolo affondo e spingi il bilanciere sopra la testa usando le gambe."
    ),
    "barbell_rack_pulls" to ex(
        "Rack pull",
        "Metti il bilanciere sui supporti all'altezza delle ginocchia. Impugna e spingi sui talloni per alzarti con la schiena piatta, poi riappoggialo."
    ),
    "barbell_reverse_curls" to ex(
        "Curl inverso con bilanciere",
        "Tieni il bilanciere a presa prona (palmi verso il basso). Flettilo verso le spalle tenendo i gomiti ai fianchi, poi scendi lentamente."
    ),
    "barbell_reverse_wrist_curls" to ex(
        "Curl inverso per polsi con bilanciere",
        "Siediti con gli avambracci sulle cosce e i palmi verso il basso. Fletti i polsi verso l'alto, poi scendi lentamente."
    ),
    "barbell_rollouts" to ex(
        "Rollout con bilanciere",
        "Inginocchiati con il bilanciere carico davanti a te. Fallo rotolare in avanti con il core attivo e la zona lombare piatta, poi riportalo indietro con gli addominali."
    ),
    "barbell_romanian_deadlifts" to ex(
        "Stacco rumeno con bilanciere",
        "Stai con il bilanciere ai fianchi. Spingi il bacino indietro facendo scendere il bilanciere lungo le cosce con le ginocchia poco flesse finché senti l'allungamento dei femorali, poi risali."
    ),
    "barbell_rows" to ex(
        "Rematore con bilanciere",
        "Fletti il busto in avanti con la schiena piatta e il bilanciere a braccia distese. Tiralo verso le costole basse stringendo le scapole, poi scendi con controllo."
    ),
    "barbell_shrugs" to ex(
        "Shrug con bilanciere",
        "Stai eretto tenendo il bilanciere a braccia distese. Solleva le spalle verso le orecchie, fermati e scendi lentamente."
    ),
    "barbell_side_bends" to ex(
        "Flessioni laterali con bilanciere",
        "Tieni il bilanciere sulle spalle o a un fianco. Piega il busto di lato in vita, poi torna su con controllo e cambia lato."
    ),
    "barbell_side_lunges" to ex(
        "Affondi laterali con bilanciere",
        "Tieni il bilanciere sulla parte alta della schiena. Fai un passo di lato flettendo quel ginocchio mentre l'altra gamba resta tesa, poi torna al centro."
    ),
    "barbell_sit_ups" to ex(
        "Sit-up con bilanciere",
        "Sdraiati supino tenendo il bilanciere al petto o sopra la testa. Siediti arrotolando il busto in avanti, poi scendi con controllo."
    ),
    "barbell_skullcrushers" to ex(
        "Skullcrusher con bilanciere",
        "Sdraiati sulla panca tenendo il bilanciere a presa stretta sopra il petto. Scendilo verso la fronte flettendo i gomiti, poi riestendi."
    ),
    "barbell_spider_curls" to ex(
        "Spider curl con bilanciere",
        "Sdraiati prono su una panca inclinata tenendo il bilanciere. Flettilo verso le spalle con gli omeri perpendicolari al suolo, poi scendi lentamente."
    ),
    "barbell_squats" to ex(
        "Squat con bilanciere",
        "Appoggia il bilanciere sulla parte alta della schiena. Scendi in squat finché le cosce sono almeno parallele al suolo, petto alto e ginocchia in linea con le punte, poi spingi su."
    ),
    "barbell_step_ups" to ex(
        "Step-up con bilanciere",
        "Tieni il bilanciere sulla schiena. Sali su una panca spingendo sul tallone anteriore, poi scendi e cambia gamba."
    ),
    "barbell_strict_presses" to ex(
        "Strict press",
        "Stai con il bilanciere sulle clavicole e senza spinta di gambe. Spingilo sopra la testa usando solo spalle e braccia, poi scendi con controllo."
    ),
    "barbell_windmills" to ex(
        "Windmill con bilanciere",
        "Tieni il bilanciere sopra la testa con un braccio disteso. Ruota il bacino indietro e fai scendere la mano libera lungo la gamba, poi torna in piedi."
    ),
    "barbell_wrist_curls" to ex(
        "Curl per polsi con bilanciere",
        "Siediti con gli avambracci sulle cosce e i palmi verso l'alto. Fletti i polsi verso l'alto, poi scendi lentamente."
    ),
    "close_grip_barbell_presses" to ex(
        "Panca a presa stretta",
        "Sdraiati sulla panca con le mani alla larghezza delle spalle. Scendi il bilanciere alla parte bassa del petto con i gomiti aderenti e spingi su."
    ),
    "decline_barbell_presses" to ex(
        "Panca declinata con bilanciere",
        "Sdraiati sulla panca declinata con il bilanciere sopra la parte bassa del petto. Scendilo e spingi fino a distendere le braccia."
    ),
    "incline_barbell_presses" to ex(
        "Panca inclinata con bilanciere",
        "Sdraiati sulla panca inclinata con il bilanciere sopra la parte alta del petto. Scendilo alla parte alta del petto e spingi fino a distendere."
    ),
    "landmine_presses" to ex(
        "Landmine press",
        "Inserisci un'estremità del bilanciere in un angolo landmine. Spingi l'altra estremità sopra la testa dall'altezza della spalla con una traiettoria leggermente diagonale, poi scendi."
    ),
    "landmine_rainbows" to ex(
        "Landmine rainbow",
        "Tieni l'estremità carica del bilanciere sopra la testa. Fallo oscillare in un arco ampio da un lato all'altro con il core attivo."
    ),
    "landmine_twists" to ex(
        "Landmine twist",
        "Tieni l'estremità carica del bilanciere al petto. Ruota il busto da un lato all'altro mantenendo il bacino rivolto in avanti."
    ),
    "military_presses" to ex(
        "Military press",
        "Stai o siediti con il bilanciere sulle clavicole. Spingilo sopra la testa fino a distendere le braccia con il core attivo."
    ),
    "stiff_leg_barbell_deadlifts" to ex(
        "Stacco a gambe tese con bilanciere",
        "Stai con il bilanciere ai fianchi e le gambe quasi tese. Fletti il busto in avanti facendo scendere il bilanciere lungo le gambe finché senti l'allungamento, poi risali."
    ),
    "t_bar_rows" to ex(
        "Rematore al T-bar",
        "Mettiti a cavallo del T-bar e impugna le maniglie. Tira il peso al petto stringendo le scapole, poi scendi."
    ),
    "upright_barbell_rows" to ex(
        "Tirate al mento",
        "Tieni il bilanciere a presa prona. Tiralo fino al mento guidando con i gomiti, poi scendi con controllo."
    ),
    // --- Cavi e macchine ---
    "ab_wheels" to ex(
        "Ab wheel",
        "Inginocchiati e tieni la ruota sotto le spalle. Fallo rotolare in avanti con il core attivo e senza far cadere il bacino, poi riportalo indietro arrotolando gli addominali."
    ),
    "abductor_machines" to ex(
        "Abduzione alla macchina",
        "Siediti nella macchina per l'abduzione con i cuscinetti all'esterno delle cosce. Apri le gambe contro i cuscinetti, fermati e torna lentamente."
    ),
    "adductor_machines" to ex(
        "Adduzione alla macchina",
        "Siediti nella macchina con i cuscinetti all'interno delle cosce. Stringi le gambe contro i cuscinetti, fermati e rilascia lentamente."
    ),
    "assisted_pull_up_machines" to ex(
        "Trazioni assistite",
        "Imposta l'assistenza in modo da completare le ripetizioni. Impugna la sbarra con le ginocchia sul cuscinetto, porta il petto alla sbarra e scendi con controllo."
    ),
    "bayesian_curls_cable" to ex(
        "Bayesian curl al cavo",
        "Mettiti di spalle al cavo basso con la maniglia nella mano del braccio arretrato. Fletti verso la spalla tenendo il gomito fermo, poi scendi lentamente."
    ),
    "behind_the_back_cable_lateral_raises" to ex(
        "Alzate laterali al cavo dietro la schiena",
        "Mettiti con la schiena al cavo basso e la maniglia in una mano dietro di te. Solleva il braccio di lato fino all'altezza della spalla, poi scendi."
    ),
    "cable_abductions" to ex(
        "Abduzione al cavo",
        "Fissa una cavigliera al cavo basso e mettiti di fianco. Porta la gamba esterna di lato contro la resistenza, poi torna."
    ),
    "cable_adductions" to ex(
        "Adduzione al cavo",
        "Fissa una cavigliera al cavo basso e mettiti di fianco. Porta la gamba interna attraverso il corpo contro la resistenza, poi torna."
    ),
    "cable_crossovers" to ex(
        "Croci ai cavi",
        "Metti le carrucole sopra l'altezza delle spalle e impugna una maniglia per mano. Porta le mani insieme davanti al petto stringendo i pettorali, poi apri con controllo."
    ),
    "cable_crunches" to ex(
        "Crunch al cavo",
        "Inginocchiati davanti a un cavo alto con la corda vicino alla testa. Arrotola i gomiti verso le ginocchia tenendo fermo il bacino, poi torna."
    ),
    "cable_curls" to ex(
        "Curl al cavo",
        "Mettiti davanti a un cavo basso con barra o corda. Fletti tenendo i gomiti ai fianchi, poi scendi lentamente."
    ),
    "cable_face_pulls" to ex(
        "Face pull al cavo",
        "Metti la corda all'altezza del viso. Tira verso il viso con i gomiti alti e larghi, stringendo le spalle posteriori e la parte alta della schiena."
    ),
    "cable_flyes" to ex(
        "Croci al cavo",
        "Metti le carrucole all'altezza del petto. Con i gomiti leggermente flessi porta le mani insieme in un arco davanti al petto, poi apri con controllo."
    ),
    "cable_front_raises" to ex(
        "Alzate frontali al cavo",
        "Mettiti di spalle al cavo basso. Solleva la maniglia distesa davanti a te fino all'altezza della spalla, poi scendi lentamente."
    ),
    "cable_kickbacks" to ex(
        "Kickback al cavo",
        "Mettiti davanti a un cavo basso con il gomito flesso al fianco. Estendi il braccio dritto all'indietro stringendo il tricipite, poi torna."
    ),
    "cable_knee_raises" to ex(
        "Sollevamento ginocchia al cavo",
        "Fissa le cavigliere a un cavo basso e appenditi a una sbarra o mettiti nella postazione. Porta le ginocchia al petto contro la resistenza, poi scendi."
    ),
    "cable_lateral_raises" to ex(
        "Alzate laterali al cavo",
        "Mettiti di fianco a un cavo basso. Solleva la maniglia di lato fino all'altezza della spalla, poi scendi lentamente."
    ),
    "cable_pallof_presses" to ex(
        "Pallof press al cavo",
        "Mettiti di fianco a un cavo all'altezza del petto. Spingi la maniglia dritta davanti a te resistendo alla rotazione, poi riportala indietro."
    ),
    "cable_pull_throughs" to ex(
        "Pull-through al cavo",
        "Mettiti di spalle al cavo basso con la corda tra le gambe. Fletti il busto dall'anca e poi spingi il bacino in avanti per raddrizzarti stringendo i glutei."
    ),
    "cable_rear_delt_flyes" to ex(
        "Croci posteriori al cavo",
        "Metti il cavo all'altezza del petto e impugna la maniglia con la mano opposta. Tira attraverso il corpo aprendo il braccio di lato e stringendo la spalla posteriore."
    ),
    "cable_rows_rear_delt_focus" to ex(
        "Rematore al cavo (spalle posteriori)",
        "Usa il cavo basso con presa stretta. Tira verso la parte alta del petto con gomiti alti e larghi per enfatizzare le spalle posteriori."
    ),
    "cable_shrugs" to ex(
        "Shrug al cavo",
        "Tieni una barra fissata a un cavo basso. Solleva le spalle, fermati e scendi."
    ),
    "cable_side_bends" to ex(
        "Flessioni laterali al cavo",
        "Mettiti di fianco a un cavo basso tenendo la maniglia al fianco. Piega il busto in vita lontano dal cavo, poi torna su."
    ),
    "cable_woodchoppers" to ex(
        "Woodchopper al cavo",
        "Metti il cavo in alto o in basso. Tira la maniglia in diagonale attraverso il corpo ruotando il busto, poi torna con controllo."
    ),
    "cable_wrist_curls" to ex(
        "Curl per polsi al cavo",
        "Fissa una barra a un cavo basso e siediti con gli avambracci sulle cosce. Fletti i polsi verso l'alto, poi scendi lentamente."
    ),
    "captain_s_chair_leg_raises" to ex(
        "Sollevamento gambe alla captain's chair",
        "Appoggia gli avambracci sui cuscinetti della captain's chair. Solleva le ginocchia verso il petto senza oscillare, poi scendi lentamente."
    ),
    "chest_press_machines" to ex(
        "Chest press",
        "Siediti nella chest press con le maniglie all'altezza del petto. Spingi in avanti fino a distendere le braccia, poi torna con controllo."
    ),
    "chest_supported_machine_rows" to ex(
        "Rematore con appoggio al petto",
        "Siediti con il petto sul cuscinetto. Tira le maniglie verso il busto stringendo le scapole, poi riestendi con controllo."
    ),
    "close_grip_lat_pulldowns" to ex(
        "Lat machine a presa stretta",
        "Usa una presa stretta sulla sbarra (palmi verso l'interno o proni). Tira la sbarra alla parte alta del petto guidando i gomiti in basso e indietro."
    ),
    "cross_cable_lateral_raises" to ex(
        "Alzate laterali incrociate ai cavi",
        "Mettiti tra due cavi bassi e impugna le maniglie opposte. Solleva entrambe le braccia di lato fino all'altezza delle spalle, poi scendi."
    ),
    "donkey_calf_raises" to ex(
        "Donkey calf raise",
        "Inclinati in avanti con il busto appoggiato e un peso sulla parte bassa della schiena. Sollevati sulle punte, fermati e abbassa i talloni."
    ),
    "drag_curls_cable" to ex(
        "Drag curl al cavo",
        "Mettiti davanti a un cavo basso. Fletti la barra vicino al corpo facendola strisciare sul busto con i gomiti indietro, poi scendi."
    ),
    "egyptian_lateral_raises_cable" to ex(
        "Alzate laterali egiziane al cavo",
        "Metti il cavo all'altezza della spalla e parti con il braccio attraverso il corpo. Sollevalo di lato fino all'altezza della spalla, poi torna."
    ),
    "floor_presses_machine" to ex(
        "Floor press alla macchina",
        "Sdraiati a terra sotto le maniglie della macchina. Spingi in alto e scendi finché gli omeri toccano il pavimento, che limita la profondità."
    ),
    "glute_bridge_machines" to ex(
        "Glute bridge alla macchina",
        "Siediti nella macchina con le spalle sul cuscinetto. Spingi con i piedi per estendere il bacino stringendo i glutei, poi torna."
    ),
    "glute_ham_raises" to ex(
        "Glute ham raise",
        "Fissa le caviglie sulla glute-ham bench con le ginocchia sul bordo del cuscinetto. Scendi con il busto in modo controllato e tirati su con femorali e glutei."
    ),
    "glute_kickbacks" to ex(
        "Glute kickback",
        "Stai in piedi o in ginocchio davanti a un cavo o a una macchina. Calcia una gamba dritta all'indietro stringendo il gluteo in alto, poi torna."
    ),
    "glute_presses_machine" to ex(
        "Glute press",
        "Siediti nella glute press con i piedi sulla pedana. Spingi sui talloni per estendere il bacino, poi torna."
    ),
    "good_mornings_machine" to ex(
        "Good morning alla macchina",
        "Metti il cuscinetto della macchina sulla parte alta della schiena. Fletti il busto in avanti dall'anca con la schiena piatta, poi torna in piedi."
    ),
    "hack_squats" to ex(
        "Hack squat",
        "Entra nella macchina hack con la schiena sul cuscinetto e i piedi sulla pedana. Scendi finché le cosce sono parallele, poi spingi su."
    ),
    "hammer_cable_curls" to ex(
        "Hammer curl al cavo",
        "Fissa una corda a un cavo basso. Fletti a presa neutra con i palmi rivolti l'uno verso l'altro, poi scendi lentamente."
    ),
    "hammer_strength_chest_presses" to ex(
        "Chest press Hammer Strength",
        "Siediti nella macchina Hammer Strength con le maniglie all'altezza del petto. Spingi in avanti fino a distendere le braccia, poi torna con controllo."
    ),
    "hammer_strength_rows" to ex(
        "Rematore Hammer Strength",
        "Siediti nella macchina con il petto sul cuscinetto. Tira le maniglie verso di te stringendo le scapole, poi riestendi."
    ),
    "hammer_strength_shoulder_presses" to ex(
        "Shoulder press Hammer Strength",
        "Siediti nella macchina Hammer Strength con le maniglie all'altezza delle spalle. Spingi sopra la testa fino a distendere le braccia, poi scendi con controllo."
    ),
    "high_cable_curls" to ex(
        "Curl al cavo alto",
        "Metti il cavo sopra l'altezza della spalla e fletti da quell'angolo. Tieni il gomito fermo e scendi lentamente."
    ),
    "hise_shrugs_machine" to ex(
        "Hise shrug",
        "Siediti nella leg press con le spalle sotto i cuscinetti. Solleva le spalle contro il carico, poi scendi."
    ),
    "lateral_cable_leg_raises" to ex(
        "Abduzione gamba al cavo",
        "Fissa una cavigliera a un cavo basso. Solleva la gamba esterna di lato, poi scendi lentamente."
    ),
    "lat_pulldowns" to ex(
        "Lat machine",
        "Siediti alla macchina e impugna la sbarra più larga delle spalle. Tira alla parte alta del petto guidando i gomiti in basso e indietro, poi risali lentamente."
    ),
    "leg_curls" to ex(
        "Leg curl",
        "Sdraiati prono sulla leg curl. Fletti i talloni verso i glutei, fermati e scendi lentamente."
    ),
    "leg_extensions" to ex(
        "Leg extension",
        "Siediti nella macchina con il cuscinetto sugli stinchi. Estendi le gambe fino a distenderle stringendo il quadricipite, poi scendi lentamente."
    ),
    "leg_press_calf_raises" to ex(
        "Calf raise alla leg press",
        "Metti le punte dei piedi sul bordo della pedana. Spingi sulle punte per sollevarla e abbassa i talloni per un allungamento profondo."
    ),
    "leg_presses" to ex(
        "Leg press",
        "Siediti nella leg press con i piedi alla larghezza delle spalle sulla pedana. Abbassala finché le ginocchia sono vicine al petto e spingi su senza bloccarle."
    ),
    "low_cable_flyes" to ex(
        "Croci al cavo basso",
        "Metti le carrucole alla minima altezza. Con i gomiti leggermente flessi porta le mani in alto e insieme davanti al petto stringendo i pettorali."
    ),
    "machine_crunches" to ex(
        "Crunch alla macchina",
        "Siediti nella macchina con il cuscinetto al petto. Arrotola il busto in avanti, poi torna lentamente."
    ),
    "machine_curls" to ex(
        "Curl alla macchina",
        "Siediti nella macchina con le braccia sul cuscinetto. Fletti le maniglie verso l'alto, poi scendi lentamente."
    ),
    "machine_dips" to ex(
        "Dip alla macchina",
        "Usa una stazione per dip o una macchina assistita. Scendi finché le spalle sono sotto i gomiti, poi spingi di nuovo su."
    ),
    "machine_flyes" to ex(
        "Croci alla macchina",
        "Siediti nella pec deck o nella macchina per le croci. Porta i cuscinetti insieme davanti al petto stringendo i pettorali, poi apri lentamente."
    ),
    "machine_lateral_raises" to ex(
        "Alzate laterali alla macchina",
        "Siediti nella macchina per le alzate laterali con le braccia sui cuscinetti. Solleva le braccia all'altezza delle spalle, poi scendi lentamente."
    ),
    "machine_rows" to ex(
        "Rematore alla macchina",
        "Siediti nella macchina con il petto sul cuscinetto. Tira le maniglie al busto stringendo le scapole, poi riestendi."
    ),
    "machine_shrugs" to ex(
        "Shrug alla macchina",
        "Imposta i cuscinetti per le spalle sulla macchina. Solleva le spalle, fermati e scendi."
    ),
    "overhead_cable_extensions" to ex(
        "Estensioni sopra la testa al cavo",
        "Mettiti di spalle a un cavo alto tenendo la corda dietro la testa. Estendi le braccia sopra la testa, poi scendi lentamente."
    ),
    "pec_decks" to ex(
        "Pec deck",
        "Siediti nella pec deck con gli avambracci sui cuscinetti. Porta le braccia insieme davanti al petto stringendo i pettorali, poi apri."
    ),
    "pendulum_squats" to ex(
        "Pendulum squat",
        "Metti i piedi sulla pedana della macchina pendulum. Scendi finché le cosce sono parallele, poi spingi su."
    ),
    "rear_delt_decks" to ex(
        "Rear delt deck",
        "Siediti con il viso verso la macchina e le braccia davanti. Spingi le braccia indietro e di lato stringendo le spalle posteriori."
    ),
    "reverse_hypers" to ex(
        "Reverse hyper",
        "Sdraiati prono con il bacino sul bordo della panca. Solleva le gambe tese dietro di te stringendo glutei e lombari, poi scendi."
    ),
    "reverse_pec_decks" to ex(
        "Reverse pec deck",
        "Siediti con la schiena verso la macchina e le braccia davanti. Tira le braccia indietro e di lato stringendo le spalle posteriori."
    ),
    "rope_crunches" to ex(
        "Crunch alla corda",
        "Inginocchiati davanti a un cavo alto tenendo le estremità della corda alla testa. Arrotola i gomiti verso le ginocchia separando leggermente la corda, poi torna."
    ),
    "rope_pushdowns" to ex(
        "Pushdown alla corda",
        "Mettiti davanti a un cavo alto con una corda. Spingi la corda verso il basso fino a distendere le braccia, separando le estremità in basso, poi torna."
    ),
    "rotary_torso_machines" to ex(
        "Rotazione del busto alla macchina",
        "Siediti nella macchina e impugna le maniglie al petto. Ruota il busto da un lato all'altro con controllo."
    ),
    "seated_abduction_machines" to ex(
        "Abduzione da seduto",
        "Siediti con il busto appoggiato e i cuscinetti all'esterno delle cosce. Apri le gambe contro i cuscinetti, fermati un istante e torna lentamente."
    ),
    "seated_adduction_machines" to ex(
        "Adduzione da seduto",
        "Siediti nella macchina con i cuscinetti all'interno delle cosce. Stringi le gambe, fermati e rilascia lentamente."
    ),
    "seated_calf_raises" to ex(
        "Calf raise da seduto",
        "Siediti nella macchina con i cuscinetti sulle ginocchia. Sollevati sulle punte, fermati e abbassa i talloni per allungare."
    ),
    "seated_leg_curls" to ex(
        "Leg curl da seduto",
        "Siediti nella macchina con il cuscinetto sopra i talloni. Fletti i talloni in basso e indietro verso i glutei, poi torna lentamente."
    ),
    "seated_rows" to ex(
        "Pulley basso",
        "Siediti con i piedi appoggiati e la schiena dritta. Tira la maniglia allo stomaco stringendo le scapole, poi riestendi con controllo."
    ),
    "seated_wide_rows" to ex(
        "Pulley a presa larga",
        "Usa la maniglia larga al pulley. Tiralo al petto con i gomiti aperti stringendo la parte alta della schiena."
    ),
    "shoulder_press_machines" to ex(
        "Shoulder press alla macchina",
        "Siediti nella shoulder press con le maniglie all'altezza delle spalle. Spingi sopra la testa fino a distendere le braccia, poi scendi con controllo."
    ),
    "side_lying_clamshells_machine" to ex(
        "Clamshell alla macchina",
        "Sdraiati su un fianco con le ginocchia flesse e la resistenza di un elastico o della macchina. Apri il ginocchio superiore contro la resistenza, poi chiudi."
    ),
    "smith_machine_calf_raises" to ex(
        "Calf raise alla Smith machine",
        "Appoggia la barra sulle spalle nella Smith machine. Sollevati sulle punte, fermati e abbassa i talloni."
    ),
    "smith_machine_deadlifts" to ex(
        "Stacco alla Smith machine",
        "Metti la barra della Smith machine a metà coscia. Fletti il busto dall'anca con la schiena piatta per scendere, poi risali in piedi."
    ),
    "smith_machine_jm_presses" to ex(
        "JM press alla Smith machine",
        "Sdraiati sulla panca sotto la barra della Smith machine. Scendi la barra verso la parte alta del petto con i gomiti stretti, poi spingi su."
    ),
    "smith_machine_presses" to ex(
        "Shoulder press alla Smith machine",
        "Siediti o stai nella Smith machine con la barra alla parte alta del petto. Spingila sopra la testa, poi scendi con controllo."
    ),
    "smith_machine_rdls" to ex(
        "Stacco rumeno alla Smith machine",
        "Metti la barra della Smith machine all'altezza del bacino. Spingi il bacino indietro facendo scendere la barra lungo le cosce, poi torna."
    ),
    "smith_machine_shrugs" to ex(
        "Shrug alla Smith machine",
        "Stai nella Smith machine con la barra sulle spalle. Solleva le spalle, fermati e scendi."
    ),
    "smith_machine_squats" to ex(
        "Squat alla Smith machine",
        "Appoggia la barra della Smith machine sul trapezio. Scendi in squat finché le cosce sono parallele, petto alto, poi spingi su."
    ),
    "standing_calf_raises" to ex(
        "Calf raise in piedi",
        "Stai con le punte dei piedi sul bordo di un gradino. Sollevati sulle punte, fermati e abbassa i talloni sotto il gradino."
    ),
    "straight_arm_pulldowns" to ex(
        "Pullover al cavo a braccia tese",
        "Stai davanti a un cavo alto con le braccia distese. Tira la sbarra verso le cosce usando i dorsali, mantenendo le braccia tese, poi lascia risalire."
    ),
    "svend_presses_cable" to ex(
        "Svend press al cavo",
        "Stai con una maniglia del cavo stretta tra i palmi all'altezza del petto. Spingi in avanti stringendo il petto, poi torna."
    ),
    "tricep_pushdowns" to ex(
        "Pushdown per tricipiti",
        "Mettiti davanti a un cavo alto con barra o corda. Spingi verso il basso fino a distendere le braccia tenendo i gomiti ai fianchi, poi torna."
    ),
    "v_bar_pulldowns" to ex(
        "Lat machine con presa a V",
        "Usa la maniglia a V alla lat machine. Tira alla parte alta del petto guidando i gomiti in basso, poi risali lentamente."
    ),
    "v_bar_pushdowns" to ex(
        "Pushdown con presa a V",
        "Usa la maniglia a V su un cavo alto. Spingi verso il basso fino a distendere le braccia tenendo i gomiti ai fianchi, poi torna."
    ),
    "v_squat_machines" to ex(
        "V-squat",
        "Siediti nella V-squat con i piedi sulla pedana. Scendi finché le cosce sono parallele, poi spingi su."
    ),
    "wrist_curl_machines" to ex(
        "Curl per polsi alla macchina",
        "Siediti nella macchina con gli avambracci sul cuscinetto e i palmi verso l'alto. Fletti i polsi verso l'alto, poi scendi lentamente."
    ),
    "wrist_rollers" to ex(
        "Rullo per polsi",
        "Tieni un rullo per polsi con un peso attaccato. Arrotola il peso con movimenti alternati dei polsi, poi lascialo scendere lentamente."
    ),
    "y_raises_cable" to ex(
        "Y-raise al cavo",
        "Metti il cavo in basso e tieni la maniglia con il braccio disteso. Solleva il braccio in alto e in fuori formando una Y, stringendo la parte alta della schiena."
    ),
)
