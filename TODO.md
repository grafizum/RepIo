# TODO — RepIo

Task ledger for this project. Mirrored to the Grafizum Command Center via sync runs.
Owner rule (2026-09-13): every task a user prompt creates lives here first.

## How agents use this file

1. A user prompt that asks for work on this project = a new task line here, added immediately.
   Write it as a checkbox line; on the following indented lines give it metadata: a status
   line (planned / in-progress / done), a priority line (low / medium / high / urgent), and a
   notes line with the user's request in short form.
2. Run the sync CLI after editing (config: C:/Temp/sync.config.json, run from WSL — see
   grafizum-dashboard/apps/sync-cli). The sync run is what puts the task on the dashboard.
   Do NOT also create it via POST /v1/tasks — that would duplicate it.
3. Work is finished when the task is done AND the owner's next message is not a
   correction or error about it. Then: flip the checkbox, set the status line to review,
   sync again, and tell the owner in ONE short line ("✅ Ready for review: <title>").
   ONLY the owner marks a task done (button in the dashboard UI) - never write "done".
4. If the owner reports an error or asks for a change on a review task: uncheck it,
   set the status line back to in-progress, fix it, then go back to step 3.
5. Never delete a finished line. Never retitle a task after it synced (the dashboard
   matches by exact title; retitling looks like remove + add).
6. Legacy tasks that predate this file (2026-09-13) are dashboard-only. Do not copy them
   here — that would create duplicates.

## Tasks

- [x] Localize exercise names, method descriptions and search into German and Italian, and give the replace dialog a readable list on small phones
  - status: review
  - priority: high
  - notes: Owner prompt 2026-09-15: "when replacing an exercise on the small screen phones, the exercises are only visible one by one and you have to scroll a tiny space, because the description above is taking all the space... we also need to fix the exercise names and descriptions to be displayed in the other languages if the language is changed, we also need to find the exercises when adding them '+' so that when a user inputs the name in german it shows up in german and not english". All 271 pool exercises translated (names + full method text) for de/it in data/ExerciseTextDe.kt + ExerciseTextIt.kt keyed by canonical-name slug; display-time localization via data/ExerciseLocalization.kt (DB keeps the canonical English name, so equipment/muscle tables and logged sets stay intact); ExerciseSearch now matches canonical + localized names and returns the canonical one; replace dialog rewritten from AlertDialog to a window-capped Dialog with a weighted list (1 card visible before -> 2 full cards plus scroll); version 1.4.3 -> 1.5.3.

- [x] Narrow the replace-exercise muscle filter to the six broad body parts instead of 19 exact muscle groups
  - status: review
  - priority: medium
  - notes: Owner prompt 2026-09-15: "the workout filters when selecting on what muscle group to filter for when replacing the exercise, we need it changed lets only make it for the general muscle not the exact muscle group i think thats too much so fix it pls". New WorkoutTemplates.getBroadMuscleGroups() (Arms, Back, Chest, Core, Legs, Shoulders) now feeds the dialog dropdown (was a 25-entry list mixing 6 broad topics with 19 precise muscles); a broad part means every exercise whose own body part is that part, so the six parts partition the 271-exercise pool exactly (no gaps, no duplicates) - deliberately NOT a union of the per-muscle pool lists, which are "muscles worked" and would leak a chest press into Arms and chin-ups into both Arms and Back; precise groups keep their old pool semantics for templates and tests; the picked exercise still stores its precise muscle (verified on the phone: filter Arme + Kurzhanteln -> 14 alternatives, picking Kurzhantel-Curls stored "Arme · Bizeps", not "Arme"); new ExerciseMuscleFilterTest locks the partition down; version 1.5.3 -> 1.5.4.

- [x] Let the replace-dialog descriptions expand and collapse on tap, and find out why the in-app "update available" nudge never showed
  - status: review
  - priority: medium
  - notes: Owner prompt 2026-09-15: "when replacing an exercise make sure the discription is extendable and retractable when pressed upon" and "also check about that 'an update is available' message inside the app if it works for when there are new updates because currently i didnt see it". Descriptions in the replace alternatives start clamped to two lines and now open in place: the text itself and a Mehr anzeigen / Weniger anzeigen control toggle it, and a description short enough to fit two lines stays a pure replace-target (clickable is disabled unless the text was actually cut off), so expanding can never swap the exercise by accident; verified on the SM-G925F (card 1 grew 192px = two extra lines, label flipped both ways, pressing the text body retracted it, dialog stayed open so nothing was replaced). Update nudge: not broken - Play answered `InstallException: -10 ERROR_APP_NOT_OWNED (The app is not owned by any user on this device)` because the phone runs an adb-installed build, which no code change can nudge; the same session also found and fixed a real defect, the check was launched into `rememberCoroutineScope()` from an ON_RESUME observer and was cancelled whenever the composition restarted (splash handing over to the app), so it silently never completed - it now runs from `repeatOnLifecycle(RESUMED)` and rethrows CancellationException; version 1.5.4 -> 1.6.4.
