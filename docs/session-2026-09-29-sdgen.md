# Session handoff — 2026-09-29 — the SDXL render pipeline (squat test set)

Cold-start notes for whoever picks this up tomorrow. **The code for this task is not
in this repository.** It lives at `~/sdgen`, outside the RepIo tree. This file is the
index to that work; `TODO.md` carries the same summary in the ledger format.

## Status: one shared character, sheet head, animation FAILED twice and is parked (2026-09-30)

**The look changed SIX times on 2026-09-29**, each time from looking at the render:
photoreal athlete → grey robot → cartoon robot → grey character with visible muscles →
minimalist grey character → **one shared character across the whole library, with a
smooth sheet-like featureless head**, trained muscle lit orange. Only the current set
is on disk; earlier ones were deleted at the owner's request.

```bash
xdg-open ~/sdgen/out/_contact_sheet.png
```

Current look, verbatim from the script:

- positive: `minimalist 3d character, simple smooth grey body, slight muscle
  definition, smooth blank featureless head, no face, matte grey, plain light grey
  background`
- **`CHARACTER_SEED = 20342067`** — every render uses this one seed, because the owner
  picked `squat/goblet_squat` as the body to base the whole library on. That is the
  seed goblet_squat already rendered under, so the approved body carries forward.
  A shared seed biases every image to the same proportions, grey and lighting; it does
  **not** make the body pixel-identical, because the pose and prompt still move it.
  `--vary-seed` opts back into independent per-image noise.

**Not done — owner review needed.** I still cannot see these images. Everything I say
about how they look is measured, not observed.

**THE ANIMATION DOES NOT WORK. It was attempted twice on 2026-09-30, failed twice, and
the owner asked to stop.** Do not read anything below as a success.

- `out/pushup/` (13 fresh denoises, `pushup_reel.py`) came back as **a quadruped**.
  Floor contacts measure 2 on most frames and 4 on the deepest ones, and a side-on
  push-up collapses both arms and both legs onto each other - a horizontal body with
  four limbs on the ground is a quadruped by definition, and SDXL resolves the
  ambiguity toward the animal.
- `out/chain/` (9 chained img2img frames, `pushup_chain.py`) came back as **a simple
  deformation of a person**, even though it inherited the body from the APPROVED
  `goblet_squat` render and chained each frame from the previous one at 0.55
  strength. Frame-to-frame change fell from ~7.6 to ~4.6, so the body genuinely
  stopped drifting - it was just deformed to begin with.

**The root cause under both:** an OpenPose skeleton contains no body. Eighteen points
and coloured lines - no head size, no torso width, no limb thickness, no neck, no
stance. Both attempts asked SDXL to invent a torso around a stick figure while prone,
which is its worst case; the 601 approved renders are all upright, so nothing in this
library is evidence that the case has been solved. Chaining fixes the drift, not the
first invention - and the first invention is the deformed one.

**A lesson about my own reporting, which is the more useful half.** I reported the
first reel as verified on the grounds that "the aspect ratio reads as a plank at the
bottom (1.60) against the lockout (1.16)". A dog has exactly that silhouette. I had
chosen a metric that could confirm the assumption and could not disconfirm it, and
presented it as evidence. The measurement that actually mattered - floor contacts -
was not run until after the owner had already watched the gif and rejected it. A
number that cannot come out the other way is not evidence.

**Zero bytes were downloaded.** SDXL and ControlNet OpenPose were already cached from
the Sep 14 probe; the cache is still 12 GB and still holds the same two repos.

### What is still true and worth keeping

The VRAM work is reusable. `enable_model_cpu_offload` cannot work on this machine at
all - SDXL's UNet (5.1 GB fp16) and the SDXL ControlNet (2.5 GB) are both resident for
the whole denoising loop, 7.6 GB of weights on a 7.7 GB card, so it dies around step
11 of 30 however the allocator is tuned. `enable_sequential_cpu_offload` streams the
layers and peak VRAM becomes one layer rather than one model. And in diffusers 0.40
`enable_vae_slicing` / `enable_vae_tiling` have moved onto the VAE itself
(`pipe.vae.enable_slicing()`).

The still images in `out/squat/` are untouched and still good.

### If it is picked up again

Do not re-approach it the same way. The honest options are a different base model that
handles prone human figures, img2img plus an explicit body mask so only the pose region
is redrawn, or dropping video entirely in favour of stills of the rep - which is what
the library is actually for, and which this character already does well. Do **not**
reach for AnimateDiff without asking: it needs a multi-GB motion-adapter download the
owner explicitly declined, and its only SDXL-compatible module is beta.

The original note read: SDXL is a still-image model and
`generate_exercise_images.py` writes one PNG per prompt — there is no flag that makes
it animate, and this script cannot do it. The parts do exist: `diffusers 0.40` ships
`AnimateDiffPipeline` and the SDXL ControlNet pipelines, and ffmpeg is at
`/usr/bin/ffmpeg`. The cost is a motion adapter plus a ControlNet **OpenPose** model
(several more GB over the same slow HF path) — without OpenPose holding the pose,
AnimateDiff wanders and you get a twitching figure, not push-ups — and it will be slow
on 8 GB with CPU offload. Open question for the owner: how long a clip? A 2-3 second
loop is the cheap test. A cheap stopgap also exists: 6-8 stills of the push-up phases
stitched into a GIF, one render's cost, not smooth but shows the character in position
immediately.

**The one thing to know first**

"The 2D animation we were working on" is **not** a skeletal rig in the app. It is a
local image-generation pipeline that renders exercise reference photos with SDXL on
this machine's GPU. A rig (`data/ExerciseMotion.kt` + a Compose Canvas demo) was
built on a mis-read earlier today and has been **fully deleted** — files, the
`motionHeight` WindowSize token, three `form_demo_*` strings, the AGENTS.md and
AGENT_HANDOFF.md sections. The app is back to the 387 tests it had before it. Do not
rebuild it.

## Where the work lives

```
~/sdgen/
  generate_exercise_images.py   the render script (edited this session)
  fetch_torch.sh, wheels/       CUDA 13 wheel fetch (13 wheels, 2.6 GB)
  venv/bin/python               the ONLY interpreter that works
  out/squat/*.png               8 rendered images
  out/manifest.json             per-image prompt + seed
  out/_contact_sheet.png        4x2 grid of the set, for review
  render.log                    live log
```

Board task: `task_350w6y7021374i57330m6y0t` (status `review`).
The cancelled rig task `task_4w384e1p516o2k4v731t1h1w` stays cancelled — it is the
record of the mis-read, not something to redo.

## State: it renders

Environment is fixed and verified:

```
torch 2.14.0+cu130 | cuda: True | NVIDIA GeForce RTX 4070 Laptop GPU
diffusers 0.40.0, transformers 5.17.0, accelerate 1.15.0, safetensors 0.8.0
```

8 distinct 832x1216 images in `out/squat/`, ~27 s each under CPU offload. The style
holds across the set (neutral grey studio background, corner luminance 97-176,
contrast stdev 37-45), so the locked prefix is doing its job.

## Three bugs, and the one that matters

Each was found by looking at the **output**, not the log.

**1. `libnccl.so.2` import error.** The three CUDA-13 libraries that appear to be
missing from PyPI are `0.0.1` sdists published under *suffixed* names
(`nvidia-cuda-cupti-cu13`). The real builds on `download.pytorch.org/whl/cu130` use the
**unsuffixed** names: `nvidia-cuda-cupti`, `nvidia-cuda-nvrtc`, `nvidia-nvtx`. It is a
name change, not a version hunt. Installed with
`pip install --no-index --find-links wheels --no-deps`.

**2. OOM on 8 GB.** `torch_dtype="float16"` is a *string*, and diffusers 0.40 warns
and silently falls back to float32, which wants ~13 GB. Now it is `torch.float16`,
plus a VRAM-aware branch: `enable_model_cpu_offload()` under 12 GB, `pipe.to("cuda")`
above, and `enable_attention_slicing()` always.

**3. All 8 images were byte-identical.** This is the one to remember. SDXL reads only
the first **77 CLIP tokens** of a prompt and silently drops the rest. `STYLE_PREFIX`
was ~80 tokens, so the per-exercise movement text — the *only* thing that differed
between images — sat past the cut in every single prompt. One fixed seed plus one
identical truncated prompt is one image, rendered eight times in 27 s each. The only
trace in the log was one CLIP truncation warning that reads like boilerplate.

The fix, all in `generate_exercise_images.py`:

- **Movement first, style second.** Whatever is last is what gets thrown away.
- **Trimmed `STYLE_PREFIX`**, and pulled "neutral mid squat position" out of it — it
  was forcing a squat pose onto deadlifts and presses, which was wrong anyway.
- **Per-image seed** derived from the name via `crc32(name) % 100_000`, so one image
  re-renders reproducibly and no two share noise by accident.
- **`check_prompt_lengths()`** raises *before* the run. It caught 18 of 32 prompts
  still over the limit on the first trim. This is the guard that should exist for
  every generative pipeline.

## How to run it

```bash
cd ~/sdgen
HF_HUB_DISABLE_XET=1 ./venv/bin/python -u generate_exercise_images.py --test-set --out out
```

Two things about this machine, both already handled in the script's docstring:

- **`HF_HUB_DISABLE_XET=1` is required.** The hf-xet transport stalls at 0 KB/min on
  the SDXL weights, the same failure mode as the pytorch CDN. Plain HTTPS from the Hub
  runs at ~5 MB/s.
- **Launch backgrounded as `setsid nohup … &` with `</dev/null`.** A plain `nohup &`
  gets killed when the tool call returns.

Model weights are cached in `~/.cache/huggingface` (~7 GB), so runs after the first
load straight to rendering.

## The negative prompt has blocked the brief four times

This is the single most useful lesson from the whole session, and it is written into
the script. The negative prompt fought the target on every single pass, each time in a
different direction:

1. Photographic pass had `illustration, cartoon, 3d render` in the NEGATIVE — the
   three words the robot look needed most.
2. Robot pass removed them but left realism unblocked, and drifted back to photoreal.
3. Cartoon pass worked, but the character still came back too robotic because nothing
   blocked `robot, metal, android, circuitry, panel lines, visor`.
4. Current pass: the brief asked for VISIBLE MUSCLES while the negative still carried
   `bare muscle, flesh, skin` — actively rendering the exact opposite of the request.

Three rules came out of it:

- **Cartoon/character words go in the POSITIVE prompt, realism in the NEGATIVE one.**
  Never rely on a word's absence meaning its opposite.
- **Never block the subject.** `robot, metal, mechanical, android` is in the negative
  on purpose now.
- **When the negative overflows 77 tokens, cut generic defect words, not art
  direction.** `blurry, bad anatomy, cropped` cost as many tokens as `photorealistic`
  and buy far less: a soft render is a reroll away, a photorealistic one is a failed
  art direction.

And a fourth, from pass 5, which is the subtle one:

- **You can also block the subject from the POSITIVE side.** `visible sculpted muscle
  definition, lean athletic body` reads as a bodybuilder, and the brief at that point
  was "minimalistic... only so much to be able to tell". The fix was to *add*
  `bodybuilder, bulging muscles, six pack` to the negative and soften the positive to
  `slight muscle definition`. The muscle words in the negative are now the exact
  opposite of the ones removed in pass 4 — the figure needs *some* muscle to read the
  movement, just not many. Over-describing blocks the subject just as effectively as
  forbidding it.

## Next, in order

1. **Look at `~/sdgen/out/_contact_sheet.png`.** The images have not been eyeballed at
   all — I have no way to see them, so every claim about how they look is measured,
   not observed. Check that it reads as a character rather than a robot, that the
   muscles are actually visible, that the head is convincingly featureless, that the
   eight really are eight different exercises, and that the orange lands on the right
   muscle.
2. If the style drifts, adjust `STYLE_PREFIX`/`STYLE_NEGATIVE` and **re-render
   everything** — they are a set, not a wish list. Re-check the token budget after any
   edit; `check_prompt_lengths` will fail the run if it no longer fits.
3. Then `--all` for the full 32-pattern library (~32 × 27 s ≈ 15 min). All 32 prompts
   already pass the length guard, and they now all share `CHARACTER_SEED`.
4. **The animation FAILED twice and is parked** - see the top of this file. Do not spend
   more renders on the same approach; the skeleton genuinely has no body in it.
5. The library is **32 movement patterns, not 601 exercises** — the pool is mapped
   onto patterns on purpose. Wiring the rendered images into the app is a separate task
   and has not been started.

## Two things that will bite you first

- **The script SKIPS any file that already exists.** Rendering into a populated `out/`
  prints `exists, skipping` and produces nothing. `rm -rf out` before every re-render
  of a changed style — the style change is invisible otherwise, and you will sit
  looking at yesterday's images.
- **`STYLE_PREFIX` + `STYLE_NEGATIVE` are a set, not a wish list.** They have been
  changed together six times; see the section above on how each change broke something
  different. After any edit run `check_prompt_lengths` before rendering, not after.

## If something looks wrong with the environment

`tools/dashboard_update_notes.py` was added this session because
`dashboard_set_status.py` only sends `status`, and a PATCH carrying any other field is
rejected with `400 validation_error` unless a `reason` rides along too — the API
requires one on *every* state-changing call, not just a status change. Use:

```bash
python tools/dashboard_update_notes.py task_abc123 --file notes.txt
```

Board state is in `TODO.md`; app state is unchanged from before this session (387
tests green, no `version.properties` bump — nothing shipped).
