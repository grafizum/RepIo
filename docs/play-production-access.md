# Google Play production access — what is actually blocking RepIo

Written 2026-10-19, after two "more testing required" decisions.

## 1. What that link is

`https://support.google.com/googleplay/android-developer/answer/14151465` is
**"App testing requirements for new personal developer accounts"**. It is not an
app rejection, not a policy strike, and not a bug report. It is the gate that
applies to every *personal* developer account created after **13 Nov 2023**.

The gate, verbatim from the page:

> Developers with personal accounts created after November 13, 2023, must run a
> closed test for their app with a minimum of **12 testers who have been opted in
> continuously for at least 14 days**. When you meet these criteria, you can apply
> for production access.

So: **production is not "approved" by a reviewer reading your app.** It is
unlocked by a measurable condition in Play Console, and then a reviewer reads
your application form.

## 2. Why it says "more testing required"

The page names exactly two reasons a production-access application is sent back:

1. fewer than 12 opted-in testers, or
2. **insufficient tester engagement** during the period.

Google does not publish a numeric engagement threshold (no "X opens", no "Y
minutes"). That is the whole trick: the headcount is a hard number you can watch,
the engagement is a judgement made from the install/usage data Google already has
for the test track.

Consequences that trip people up, all sourced from the same page:

- **Internal testing does not count.** Only the closed track feeds the counter.
- **Open testing is locked until production access**, so it cannot be used to
  satisfy the requirement.
- The 14 days are **continuous**. A tester who opts out and back in restarts their
  own 14-day clock; if your count drops from 12 to 11, you no longer have 12
  continuously opted in.
- The clock effectively starts when the **12th tester joins**, not when the first
  one did.
- Opting in without installing/using the app satisfies the word "opted in" but
  proves nothing about engagement — which is the second rejection reason.

Distinct devices, account age and "fake tester" penalties are **not** documented
anywhere. Do not spend money on tester farms to fix this; a cohort that does not
use the app is exactly what "insufficient engagement" describes.

## 3. Diagnose it in 10 minutes

In Play Console → your app:

1. **Test and release → Testing → Closed testing → Testers.**
   Read the opted-in count, not the invited list. `Invited 14 / opted in 11` is a
   rejection, and re-applying will not change it.
2. **Closed testing → Track summary / releases.** Confirm the track has a release
   with the current version code and that testers are on *that* track.
3. **Monitor and improve → Ratings and reviews → Testing feedback.** Google tells
   you to summarise this in the form, so it must contain something. Empty feedback
   plus low sessions is the classic "insufficient engagement".
4. **Dashboard → Apply for production.** The tile states whether the criteria are
   met, and the form itself echoes the tester count.

If the count is ≥12 and the dates are ≥14 continuous days, then the blocker is
engagement or the written answers — go to §5 and §6.

## 4. The form: three sections, nine fields

### Part 1 — About your closed test

| Field | What to give |
|---|---|
| How did you recruit testers? | The literal channel: gym, local fitness group, friends/colleagues, and how you invited them (email list, WhatsApp group). Say if no paid service was used. |
| How easy was it to recruit? | The honest option. This is not scored; a false "Very easy" next to 12 testers and no activity reads badly. |
| Engagement description | Which features were used, whether usage matched a real user, and the **differences**. Naming a difference is what makes the answer credible. |
| Feedback summary + how collected | Themes, not a list of compliments. Channel(s): in-app support form, WhatsApp group, Play testing feedback. |

### Part 2 — About your app

| Field | What to give |
|---|---|
| Intended audience | Specific: age band, training level, equipment context, markets, and explicitly not children. |
| Value proposition | What the app does that a notes app or a PDF program does not. |
| First-year installs | A range. `0–10K` is a normal, honest answer for a first release. |

### Part 3 — About your production readiness

| Field | What to give |
|---|---|
| Changes made because of the test | Concrete, dated, verifiable against your release notes. |
| How you decided it was ready | The criteria you set **before** the test, and the evidence against each. |

Google reviews in **7 days or less** (occasionally longer). Re-applying after a
rejection does not reset the tester clock, but it does start another review.

## 5. Paste-ready answers for RepIo

Replace every `[bracket]` with the real number or name. Do not inflate the tester
count: Google compares it with the track. Do not claim features were used if the
feedback does not show it.

**Recruiting**

> I recruited 12 testers personally from my gym and a local fitness group. I know
> all of them in person. I invited them by email to the closed testing track,
> explained in a WhatsApp group that they needed to stay opted in for at least 14
> continuous days, and asked them to use the app for their normal training rather
> than only opening it once. No paid testing service was involved.

**Ease of recruiting**

> Neither difficult nor easy.

**Engagement**

> All 12 testers accepted the invite and installed the build from Google Play, and
> all 12 stayed opted in for [X] consecutive days. [N] completed the first-run
> setup (equipment, training days, goal) and logged at least one full workout;
> [N] used it three or more times per week. Features exercised across the group:
> first-run setup, the generated weekly plan, the workout player with rest timers,
> swapping an exercise, the level-up / evolution flow, the analytics screen, and
> settings including the theme switch. [N] testers also sent a support ticket from
> inside the app, which is the only user-facing backend call in the product.
>
> Usage broadly matched what I expect from a production user: sessions of 30–60
> minutes, [X] times a week, with most of the time spent in the workout player and
> on the home screen. Two differences worth naming: because the testers know me,
> most reported problems to me directly in the group instead of using the in-app
> support form, so support-form usage is lower than I expect in production; and
> because they were testing deliberately, several of them explored settings and
> themes far more than a normal user would.

**Feedback**

> I collected feedback through three channels: the in-app support form, a WhatsApp
> group created for the test, and Google Play's testing feedback. In total I
> received [N] pieces of feedback across [X] releases during the test.
>
> The recurring themes were: (1) [e.g. the first-run setup asked for equipment in
> a way that was unclear for people training at home]; (2) [e.g. rest-timer
> behaviour when the phone was locked]; (3) [e.g. a level appeared not to update
> until the workout was finished]. I also received [N] crash reports from testers
> [/ no crash reports]. Every theme is either fixed in the current build or listed
> below with its status.

**Audience**

> Adults aged 18–45 who strength train or train with bodyweight, from complete
> beginners to intermediate lifters, at home or in a gym, mainly in
> [Germany / Austria / Switzerland] and English-speaking markets. The app is not
> directed at children and contains no advertising or user-generated content.

**Value proposition**

> RepIo builds a balanced weekly training plan around the equipment a person
> actually has, and then tracks strength per exercise instead of per program.
> Every exercise has its own level that moves up or down from the sets and reps the
> user logs, which produces an overall level, a visible progression path per
> movement, and a weekly check that no muscle group is over- or under-trained.
> There is no account, no subscription and no ads: the plan generation, the
> progression engine and the workout history all run on the device.

**First-year installs**

> 0–10K

**Changes made because of the test**

> The closed test ran across [X] releases ([version codes]). Changes driven directly
> by tester feedback:
> - [e.g. 1.7.4 — rest timer now survives the screen being locked and re-arms
>   after a reboot]
> - [e.g. 1.7.5 — the level-up dialog no longer appears behind the workout recap]
> - [e.g. 1.7.6 — first-run equipment selection rewritten after [N] testers could
>   not tell which option matched their home setup]
>
> I also fixed every crash reported by a tester before applying.

**How you decided it was ready**

> I wrote the release criteria before the test started: (a) first-run setup can be
> completed without help; (b) a full workout can be logged end to end, including
> rest timers, a level change and the recap; (c) every screen is reachable and no
> screen is empty on a fresh install; (d) no crash reports in the final release;
> (e) every blocker from the feedback list closed.
>
> In the final build, all [N] active testers completed first-run setup unaided,
> [N] logged a complete workout, the last release ran [X] days with [no crash
> reports / N crash reports that were fixed], and all [N] blocking items from the
> feedback list are closed. That is the evidence I based the decision on.

## 6. App-side gaps that will be hit in the next review

These are separate from the testing gate, and the same help page warns that
"review is not a troubleshooting step".

1. **No privacy policy inside the app, and no RepIo-specific policy at all.**
   `grep -ri privacy composeApp/src/main` returns nothing. Play's User Data policy
   requires a privacy-policy link in the Play Console field **and** within the app,
   for every app, even one that stores everything locally.

   `https://grafizum.com/privacy` is live, but **it does not cover RepIo**. Its own
   scope clause says it covers "this website, the dashboard at
   dashboard.grafizum.com, the phone app at dashboard.grafizum.com/m, and the MCP
   connections the dashboard manages", and it describes accounts, GitHub tokens and
   project content — none of which RepIo has. Pointing the Play Console field at it
   would be a policy/data mismatch of exactly the kind Play flags.

   The policy is now written, published and wired up: `site/privacy/index.html`
   holds the page, the Cloudflare Worker `repio-legal` serves it at
   `https://repio-legal.matrix-gian.workers.dev/privacy` (verified: 200,
   `text/html`, byte-identical to the file), and the app links to it from Settings
   through `AppLinks.PRIVACY_POLICY`. It covers
   workout history and settings stored on the device only, support tickets sent to
   Firebase Firestore (subject, message, device model, app version, timestamp) with
   no account and no advertising identifier, App Check / Play Integrity protecting
   that one endpoint, no ads, no analytics profile, and deletion by uninstalling or
   by email.

   Paste that URL into the Play Console field. The branded host
   `repio.grafizum.com` is attached to the same Worker but does not resolve yet —
   the Cloudflare zone in this project's account does not publish new records.
   See [privacy-policy-deploy-prompt.md](privacy-policy-deploy-prompt.md) for the
   evidence and the remaining step.

2. **Health apps declaration not evidenced.**
   A workout tracker is "Activity and fitness" in the Health apps declaration. It
   is mandatory in App content, including on testing tracks. If it is unanswered,
   production stays blocked no matter what the test looks like.

3. **Data safety does not match the code.**
   `SupportManager` writes `subject`, `message`, `device_model`, `app_version` and
   a server timestamp to Firestore. That is data leaving the device: declare
   "User-generated content → Other" (or the closest available category), collected,
   not shared, encrypted in transit. Declaring "no data collected" while this path
   exists is a mismatch Google can see.

4. **Unused Firebase SDKs inflate that surface.**
   `libs.firebase.ai` and `libs.firebase.auth` are declared in
   `composeApp/build.gradle.kts` but nothing in `src/main` uses them. Remove both,
   and confirm `firebase-firestore` + App Check are the only Firebase pieces that
   ship.

5. **`SCHEDULE_EXACT_ALARM` in the manifest.**
   It is not the restricted `USE_EXACT_ALARM`, and `TimerManager` already degrades
   correctly to `setAndAllowWhileIdle`. But Android 14+ denies it by default, a
   workout rest timer does not qualify as an alarm/calendar core function, and it
   invites a permissions question in review. Drop it and keep the inexact path.

6. **Ten launcher activities, nine disabled, toggled at runtime.**
   `AndroidManifest.xml` declares `MainActivity` plus nine theme entries;
   `RepIoApplication.onCreate` calls `setComponentEnabledSetting` on first launch,
   `excludeFromRecents="true"` and `taskAffinity=""` are set on the launcher. This
   is legal, but on a fresh install the launcher entry changes under the reviewer,
   and "functional reliability" is one of the four things the same page tells you
   to check. For the production build, ship one launcher activity and no runtime
   component toggling; keep the theme switch as an in-app setting.

7. **No crash reporting.**
   There is no Crashlytics. Nothing blocks release, but your only crash signal is
   the Play pre-launch report, and "no crash reports" is a claim you cannot back in
   the readiness answer. Either add Crashlytics (and declare it) or lean on the
   pre-launch report and say so.

8. **Review access is fine — document it anyway.**
   There is no login, so App access is "all functionality is available without
   special access". The first-run setup is several steps, so put two lines of
   reviewer instructions in that field: what to pick on first run, and where the
   workout player and analytics live.

Not a problem: `AppUpdateChecker` uses the flexible, dismissible Play flow and
never gates the app. The Play listing returning 404 is expected while the app is
only on a closed track.

## 7. If 12 testers / 14 days is already met and you were still denied

Google's page names only two reasons, and you have ruled out the first. That
leaves **insufficient tester engagement** — plus the possibility that the count you
can see is not the count Google used.

### 7.1 Reconcile the number first

Do not assume the console figure and Google's figure agree. In order of likelihood:

- **Invited vs opted in.** The testers tab shows both. Only opted-in counts.
- **Installed from the test link.** A tester who installed the APK you sent them, or
  who was already running a sideloaded build, is not the same as one who installed
  from the Play test link and appears in the tester list.
- **A gap during the window.** If anyone left for a day, their 14 days restart and
  the 12-continuous condition is broken even though the total headcount looks fine.
- **Duplicate accounts on one device.** At least one first-hand report describes
  Play Console's tester count dropping sharply and support saying some accounts are
  excluded, with the exact rule not public. This is a report, not a documented rule,
  but it is the reason to use more than 12 testers.
- **You applied on a day when the count dipped.** Screenshot the testers tab on the
  day you apply; it is the only record of what Google saw.

### 7.2 What actually moves the decision in the next 14 days

Ranked by effect per unit of effort. Nothing here is an official threshold —
Google publishes none — but each one attacks a documented or reported failure mode.

1. **Go to 20–25 opted-in testers, not 12.** Buffer against one dropout, one
   duplicate-account exclusion, and one person who never installs. This is the
   single cheapest change.
2. **Make the usage look like training, not like a test.** Each tester: complete
   first-run setup on day 1, log at least 3 workouts in week 1 and 3 in week 2, swap
   an exercise once, open analytics once, send one support ticket. A cohort that
   opens the app once on day 1 and never again is what "insufficient engagement"
   describes.
3. **Ship two releases inside the window, both driven by a reported issue.** One
   around day 6–7, one around day 11–12. Keep the release notes specific ("fixed X
   reported by tester Y"). This is the only hard evidence that the test was useful,
   and it is what the "changes made" answer describes.
4. **Keep a feedback log as you go.** One file: date, tester, what they hit, what
   you changed, version it shipped in. Quotes and screenshots. You cannot
   reconstruct this later, and the form asks for it.
5. **Fix the app-side gaps in §6 before day 1**, not after. A privacy policy that
   does not exist is the kind of thing that turns a production-access review into a
   policy review. The same help page says review is not a troubleshooting step.
6. **Read the Play pre-launch report** and fix crashes/ANRs before the window starts.
   Independent of the tester count, a reviewer who hits a broken screen ends the
   application.
7. **Apply on day 16–17, not day 14.** Three extra days cost nothing and protect
   against a same-day opt-out.

### 7.3 Answers that survive scrutiny

Every claim in the form must be something Google's own data can corroborate:

| If you write | You need |
|---|---|
| "All 12 testers used it" | 12 testers with real sessions — not 12 opt-ins |
| "Testers used every feature" | a feature list you can name, with who used what |
| "We fixed what they reported" | two releases with those fixes in the notes |
| "Usage matched production" | sessions spread over the window, not clustered on day 1 |
| "No crashes" | a clean pre-launch report, or no Crashlytics and say so |

Vague answers are not the main risk; **answers that overstate** are. Write less,
but make every sentence checkable.

### 7.4 Realistic timeline

| Day | Do |
|---|---|
| 0 | Fix §6 (privacy policy, health declaration, data safety, exact alarm, launchers). Cut a release. |
| 1 | Testers install from the Play link and complete setup. |
| 6–7 | Release 2 with the first round of fixes. |
| 11–12 | Release 3 with the second round. |
| 16–17 | Apply, with the tester tab screenshotted. |
| +7 | Decision. |

That is roughly three weeks per attempt, not three months. The trap is applying
again without changing the evidence: a second identical application mostly buys you
another seven-day wait.

### 7.5 If the third attempt fails

Stop re-applying and contact Play developer support from the Console, quoting the
application ID, the date, the tester count at the time, and asking which specific
criterion was not met. Repeating the same submission is the one strategy that
definitely does not converge.

## 8. Order of work

1. Confirm the tester count and the 14 continuous days in Play Console (§3).
2. If it is short: keep everyone opted in, have them actually train with it, and
   wait. Nothing else moves that number. If it is already met, work §7.2 instead —
   headcount alone is no longer the lever.
3. While waiting: write and host a RepIo-specific privacy policy, link it in the app
   and in the Play Console field, complete the health declaration and data safety,
   remove the unused SDKs and the exact-alarm permission, collapse the launcher
   entries.
4. Ship one more closed-test release containing those fixes — the readiness answers
   then describe a real, recent release.
5. Apply again with the §5 answers and real numbers, only after the §7.4 timeline.
