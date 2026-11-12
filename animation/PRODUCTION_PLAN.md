# RepIo exercise animations: production plan (research + design, 2026-11-12)

Scope: ~500 looping 3D clips (601 catalog entries) x 2 characters (male now, female after the first batch), rendered headless with Blender 5.2.2 on the 32-core workstation (RTX 4070 usually busy with llama-server), delivered inside the Android app (Kotlin / Compose, `minSdk = 24`, `compileSdk = 36`, Coil 2.7.0; no Media3 dependency in `gradle/libs.versions.toml` yet).

Everything below that is marked **measured** was measured today on the existing curl clip (`output/standing-dumbbell-curl/`, 60 PNG frames, 1080x1080 RGBA) or on the current scene files, using temporary copies only (`renders/tmp_plan/`, git-ignored). Nothing in `blender/`, `output/` or `assets/` was changed.

---

## 0. Key decisions (summary)

| Topic | Decision |
|---|---|
| Delivery format (primary) | **Stacked-alpha H.264 MP4** (colour premultiplied on black, alpha as luma, side by side), 512 px tall figure crop, 20 fps, one 2 s loop, `crf 24`: **~42 KB per dynamic clip** (measured 39-50 KB at crf 26-23). Decoded once on device with `MediaCodec` into RGBA frames and played as a frame sequence in Compose. |
| Delivery format (fallback / phase-0) | **Animated WebP with alpha** (same frames, q 60-75, alpha_q 60-70, `minimize_size`): **~195-222 KB**, zero app engineering (Coil `ImageDecoderDecoder`, API 28+ = 98.2 % of devices). Used first (day 1 integration) and kept as fallback if the MP4 decode path fails on a device. |
| Static holds / stretches (112 entries) | **One still WebP (~12 KB)**; the app adds a 4 s "breathing" scale 1.000 to 1.012 in Compose. No video. |
| Library size | 978 dynamic clips x ~45 KB + 224 stills x ~12 KB = **~47 MB** (MP4) / ~194 MB (WebP). |
| Shipping | **Download-on-first-view + disk cache** from Cloudflare R2 (zero egress) behind a versioned manifest; prefetch a workout's clips when it is opened; bundle ~20 top exercises (~1 MB) in assets for offline first run. Play Asset Delivery on-demand packs (per muscle group) are the no-backend alternative. |
| Render engine | **Cycles CPU, 1080x1080, 16 samples + OIDN denoise, full 783k-vert mesh**: 4.3 s/frame measured (vs 7.0 s now), 49 dB vs the 64-sample frame (visually identical). EEVEE is not worth it (31.9 dB, different look, soft-shadow speckle, only 2.8 s). Decimation gives no speed-up (render cost is pixel-bound). |
| Unique frames | Render **21 frames per symmetric loop** (ping-pong, 40 delivered frames at 20 fps) for ~70 % of archetypes; 40 frames for asymmetric motions (olympic lifts, swings, jumps, carries, negatives). |
| Total render time | ~26 k frames for 978 clips: **~31 CPU-hours** at 4.3 s/frame (+30 % re-renders ≈ 40 h). Four to five overnight batches. Authoring/QA, not rendering, is the critical path. |
| Structure | 44 motion archetypes (full mapping in Appendix A); every exercise = archetype + equipment + a small YAML override. One generic `render_exercise.py` for any exercise and either character. |
| First batch | Barbell Bench Press, Barbell Back Squat, Lat Pulldown, Push-Up (bench+barbell+rack, lower-body IK, machine/cable, bodyweight floor) then the female character. |

---

## A. Delivery format for Android

### A.1 What the curl clip looks like, numerically (measured)

* 60 frames, 1080x1080 RGBA PNG, 42 MB on disk. The figure occupies only **393 x 830 px** (union alpha bounding box x 401-793, y 195-1024); **10.1 %** of the canvas is opaque, 0.4 % is semi-transparent (anti-aliased edge).
* Colour variety is low: 6 708 unique RGB values in frame 30, only 306 at 5 bits/channel (clay skin, dark shorts, charcoal iron, one mint highlight).
* Mean absolute change between neighbouring frames: 0.78/255 (slow, smooth motion; 20 fps is plenty).
* Motion is **not** time-symmetric as authored (lift 0.42 s, squeeze, slower lowering): frame k vs frame 60-k differs by up to 3.2/255. See B.4 for the ping-pong consequence.
* Currently shipped files: `render.webp` 565 KB (540 px square, 30 fps, q 88, lossless alpha), `render.webm` 266 KB (VP9 alpha), `render.mp4` 126 KB (H.264 on #121212).

Consequence 1: **crop to the figure**. Delivering the whole 1080 canvas wastes 70 % of the pixels. Every clip gets its own crop (union alpha bbox + 8 px, rounded to multiples of 16, stored in the manifest); the app shows it in a fixed-height box with `ContentScale.Fit`. For the curl this is 416x848 at 1080, i.e. 252x512 at the 512 px delivery height.

Consequence 2: the app's surfaces are not one colour (`DarkBackground #0E0E0E`, `DarkSurface #141414`, `DarkSurfaceVariant #191919`, `DarkSurfaceHigh #1E1E1E`, `DarkSurfaceHighest #282828`, light theme `#F7F4EE / #EFEAE1 / #E2DBD0`, see `ui/theme/Color.kt`). A clip baked on one solid colour would only ever match one card. **Real alpha is required.**

### A.2 Candidate formats, measured on the curl (512 px tall figure crop, 20 fps, 2 s loop unless stated)

PSNR is against the original frames composited on the dark card (#191919) and on the light background (#F7F4EE); matched timestamps.

| Format | Settings | Size | PSNR dark / light | Plays on Android? |
|---|---|---|---|---|
| Animated WebP (alpha) | PIL/libwebp AnimEncoder q75, alpha_q70, method 6, minimize_size | **222 KB** | 40.6 / 40.7 dB | API 28+ via `ImageDecoder` (98.2 % of devices); first frame only below 28 |
| Animated WebP (alpha) | q60, alpha_q60 | 195 KB | 39.2 / 39.2 | same |
| Animated WebP (alpha) | q50, alpha_q50 | 179 KB | 38.6 / 38.6 | same |
| Animated WebP (alpha) | ffmpeg `libwebp_anim` q75 (lossless alpha, no frame merging) | 293 KB | - | same (ffmpeg is the wrong tool: no `alpha_quality`, no `minimize_size`) |
| Animated WebP, no alpha | q75 on nothing (RGB only) | 124 KB | - | shows the cost of the alpha plane: ~100-170 KB |
| Animated WebP lossless | | 1 782 KB | lossless | no |
| VP9 WebM with alpha (`yuva420p`) | crf 30 / 36 | 82 / 60 KB | - | **No alpha on Android** (see A.3) |
| H.264 on #121212 (opaque) | crf 23 / 28, veryslow | 33 / 21 KB | 29 dB (vs #191919 card: wrong colour) | yes, but one theme only |
| HEVC on #121212 (opaque) | crf 28 | 26 KB | - | yes (API 21+ decoders), one theme only |
| AV1 on #121212 (opaque) | SVT-AV1 crf 35 / 45 | 26 / 17 KB | - | software decode only on Android 10-13, mandatory from 14; not worth the device risk at these sizes |
| **Stacked-alpha H.264** (colour \| alpha, 504x512) | crf 18 / 20 / 23 / 26 / 28 | 78 / 65 / **50 / 39** / 34 KB | 37.4 / 37.0 / **36.4 / 35.5** / 34.8 dB (light: -0.9 dB) | yes, any device (MediaCodec H.264 is universal), alpha reconstructed in-app |
| Stacked-alpha HEVC | crf 20 / 24 / 28 | 90 / 64 / 45 KB | 37.6 / 37.0 / 36.1 | yes (API 21+), but not smaller than H.264 at this clip size (container/header overhead dominates) |
| Stacked-alpha H.264 at 384 / 640 px tall | crf 23 | 37 / 64 KB | - | |
| Sprite sheet 8x5, 384 px tall | WebP q80 / lossless / PNG | 300 KB / 1 077 KB / 1 448 KB | - | yes, but a 40-frame sheet at 512 px is 20 MB decoded in RAM |
| Single still (hold/stretch) | WebP q80 alpha_q70 / PNG | **12-14 KB** / 84 KB | - | yes, all API levels |

fps and resolution scaling (WebP q75, alpha_q60-70, measured): 30 fps 320 KB, 24 fps 264 KB, **20 fps 222 KB**, 15 fps 175 KB; 384 px 160 KB, 512 px 222 KB, 640 px 280 KB. Ping-pong vs linear sequence of the same 40 frames: 208 vs 222 KB (WebP has no inter-frame prediction, so symmetric content barely helps the file; it only halves render time).

Reading the table:

* Animated WebP is intra-only coding; every frame costs ~5 KB. Stacked H.264 uses inter prediction and is **4-6x smaller at a quality that is still 35-36 dB** (the preview `renders/tmp_plan/enc/preview_ref_webp_stacked.png` shows the WebP has visible blocking in the dark shorts; the H.264 version is softer but cleaner).
* 20 fps is the sweet spot: motion between frames is < 1/255 on average, 24 fps costs +20 % and 30 fps +45 % for no visible gain on a 2 s curl.
* 512 px tall is enough: the clip is shown at roughly 240-320 dp; on a 3x screen that is 720-960 physical px for the full box, of which the figure is ~80 %. 640 px costs +30 %; 384 px starts to look soft on 3x screens.

### A.3 VP9 alpha on Android: precise answer

* WebM alpha is stored as a `BlockAdditional` side stream (BlockAddID 1) per the WebM spec (https://sites.google.com/a/webmproject.org/wiki/alpha-channel). Android's platform codec list documents VP9 decoding since 4.4 but says nothing about alpha (https://developer.android.com/media/platform/supported-formats).
* ExoPlayer's `MatroskaExtractor` does not surface the alpha BlockAdditional and `MediaCodec` VP9 decoders output opaque YUV. Maintainer statements: "I think it's still the case that Android platform decoders don't support this" (andrewlewis, google/ExoPlayer#7789, 2020, https://github.com/google/ExoPlayer/issues/7789); the live issue androidx/media#1388 "Support transparent video with ExoPlayer" is still **open** (https://github.com/androidx/media/issues/1388): "I'm afraid we don't have an efficient way to support this in media3" (andrewlewis, Dec 2024) and, in the June 2026 update, the codec-framework changes "are not on the roadmap for the next release"; a possible future path is timestamp-aligned colour+alpha tracks joined at render time.
* Conclusion: **a VP9-alpha WebM renders black on Android/ExoPlayer**. Do not ship `render.webm` to the app. It stays useful as a web/preview asset only.

### A.4 Other approaches considered

* **Opaque MP4 per theme** (dark + light): 2 x 21-33 KB, trivial playback, but breaks on every surface shade and on future themes. Rejected.
* **Chroma key in a shader**: AGSL `RuntimeShader` needs API 33; `Modifier.graphicsLayer` has no per-pixel key below that. Rejected for minSdk 24.
* **Media3 `ExoPlayer.setVideoEffects` + custom `GlShaderProgram`** (https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer#setVideoEffects(java.util.List%3Candroidx.media3.common.Effect%3E), `@UnstableApi`, needs `media3-effect`): can composite the stacked video on the GPU into a non-opaque `TextureView`. Works, but it is a GL pipeline per visible player, an unstable API, and more device-specific than decoding to bitmaps. Keep as the "true video" variant if the app ever needs long clips; for 40 frames, bitmaps are simpler.
* **Keyframe-less (skeletal) delivery**: ship bone curves (~40 bones x 4 floats x 40 frames ≈ 25 KB raw, ~6 KB gzipped per exercise) plus one GLB character per sex (decimated to ~30k tris, 2-3 MB) and render on device with Filament/SceneView. Library would be ~10 MB total, but it costs a 3D runtime (+4-6 MB native libs per ABI), phone GPU load, a second look (no Cycles lighting/SSS), and weeks of engineering. Not for v1; the archetype/YAML layer in section D keeps this door open because the pose data is already procedural.
* **Sprite sheets**: same bytes as WebP, worse memory. Rejected.
* **Lottie / 2D**: not applicable to 3D renders.

### A.5 Recommendation

**Primary: stacked-alpha H.264 MP4, decoded on device.**

* Layout: left half = colour premultiplied on black, right half = alpha as luma (`yuv420p`, both halves even-sized, total width multiple of 16).
* 512 px tall figure crop, 20 fps, `crf 24`, `-g 999` (one I-frame per clip; the whole clip is decoded anyway), `-preset veryslow` (offline, size matters more than encode time).
* Target: **<= 50 KB per dynamic clip** (curl: 45 KB), <= 15 KB per still.
* App side (~2-3 days): `MediaExtractor` + `MediaCodec` (H.264 hardware decoder, API 16+) → decode the 40 frames once into `Bitmap`s (left RGB x right alpha, store premultiplied ARGB_8888; 252x512x4 B x 40 = 20 MB per clip, keep an LRU of 2 clips), then a Compose `Image` that advances the frame every 50 ms with `withFrameNanos`. No GL, no ExoPlayer, no unstable API. Decode cost ~100-200 ms per clip on a mid-range phone; show the still WebP (12 KB) as placeholder while decoding.
* Quality note: premultiplying before encoding is what prevents dark fringes on light surfaces (measured light-theme PSNR is only 0.9 dB below dark).

**Fallback and phase-0: animated WebP with alpha (Coil).**

* Same frames, PIL/libwebp q65-75, alpha_q 60-70, `method=6`, `minimize_size=True` → 195-222 KB. Ship this first (one afternoon: `io.coil-kt:coil-gif:2.7.0`, `ImageDecoderDecoder.Factory()` when `SDK_INT >= 28`, as in https://developer.android.com/social-and-messaging/guides/media-animated-gif; the same decoder handles animated WebP, https://coil-kt.github.io/coil/gifs/). Android decodes animated WebP natively through `ImageDecoder` / `AnimatedImageDrawable` since API 28 (https://developer.android.com/reference/android/graphics/ImageDecoder, https://developer.android.com/reference/android/graphics/drawable/AnimatedImageDrawable; set `repeatCount = REPEAT_INFINITE`). On API 24-27 (about 1.3 % of devices, https://apilevels.com/, Oct 2026 data) the first frame is decoded as a still; acceptable.
* The manifest carries both URLs per clip; the app prefers MP4 when its decoder is present and falls back to WebP (or to the still) on any decode error.

**Stills for holds/stretches**: one WebP at the same crop/height, q80, alpha_q70, ~12 KB; the app animates a 4 s ease-in-out scale (1.0 to 1.012) and a 1 px vertical drift to suggest breathing. (Rendering a 60-frame breathing loop would cost 224 x 21 renders and 224 x 30 KB for a near-invisible effect.)

### A.6 Library size and shipping

| | per clip | 978 dynamic (489 x 2 characters) | 224 stills (112 x 2) | total |
|---|---|---|---|---|
| Stacked MP4 crf 24 | ~45 KB | 44 MB | 2.7 MB | **~47 MB** |
| WebP q65 | ~200 KB | 196 MB | 2.7 MB | ~199 MB |

Shipping options (Play limits: base module 500 MB, on-demand/fast-follow asset packs up to 1.5 GB each, 100 packs max, limits are on compressed download size, https://support.google.com/googleplay/android-developer/answer/9859372; install-time packs count toward the listed app size, on-demand/fast-follow do not, https://developer.android.com/guide/playcore/asset-delivery):

1. **Bundled in the AAB**: 47 MB would roughly double the install and every clip fix means an app release. No. Exception: bundle the ~20 most used exercises (~1 MB) in `assets/` so the first workout works offline.
2. **Play Asset Delivery on-demand packs**, one per muscle group x character (18 x 2 = 36 packs of ~1-3 MB): free CDN, offline after fetch, no backend. Costs: Play Core dependency, Play-only distribution, clips versioned with the app. Good second step if the backend-less route is preferred.
3. **Download-on-first-view with cache** (recommended): clips on Cloudflare R2 (free egress) at `clips/v<N>/<character>/<exercise-id>.(mp4|webp)`, one `manifest.json` (version, per-clip width/height/frames/fps/bytes/sha256/urls). Coil's OkHttp disk cache handles WebP for free; the MP4 path uses OkHttp + a 64 MB disk LRU. Prefetch all clips of a workout when the workout screen opens (8 exercises x 45 KB = 360 KB). A new render only needs a manifest bump.

### A.7 Exact packaging commands

Crop values come from the union alpha bbox (computed per clip by the packager, here the curl's 416x848 at 390,186). `fps=20` on a 30 fps source picks every 1.5th frame; render at 20 fps directly instead (section B) and drop the `fps=` filter.

```bash
# 1. stacked-alpha H.264 (primary)
ffmpeg -y -framerate 20 -i frames/%04d.png -filter_complex \
 "[0]crop=416:848:390:186,scale=-2:512:flags=lanczos,format=rgba,split[c][a];\
  [c]premultiply=inplace=1,format=yuv420p[col];[a]alphaextract,format=yuv420p[alp];[col][alp]hstack" \
 -c:v libx264 -preset veryslow -crf 24 -g 999 -bf 2 -profile:v high -pix_fmt yuv420p \
 -movflags +faststart standing-dumbbell-curl.mp4            # curl: ~45 KB

# 2. animated WebP with alpha (fallback) - PIL, because ffmpeg's libwebp_anim has no alpha_quality / minimize_size
python - <<'EOF'
from PIL import Image; import glob
ims=[Image.open(f).convert("RGBA").crop((390,186,806,1034)).resize((252,512),Image.LANCZOS)
     for f in sorted(glob.glob("frames/*.png"))]
ims[0].save("standing-dumbbell-curl.webp", save_all=True, append_images=ims[1:], duration=50, loop=0,
            lossless=False, quality=65, alpha_quality=60, method=6, minimize_size=True)   # ~200 KB
EOF

# 3. still (holds / stretches, and the placeholder of every dynamic clip)
cwebp -q 80 -alpha_q 70 -m 6 -crop 390 186 416 848 -resize 252 512 frames/0001.png -o standing-dumbbell-curl.still.webp   # ~12 KB

# 4. decode check of the stacked file (what the app will do): left half / right half
ffmpeg -i standing-dumbbell-curl.mp4 -filter_complex "[0]split[l][r];[l]crop=iw/2:ih:0:0[c];[r]crop=iw/2:ih:iw/2:0,format=gray[a];[c][a]alphamerge" -f image2 check_%03d.png
```

`tools_package.py` should be extended to (a) compute the crop, (b) emit the three files plus a manifest entry, (c) run the seam/QA checks of section D, and (d) keep `render.webm` only for the web preview.

---

## B. Render optimisation (measured on this machine, 32 CPU threads, Blender 5.2.2)

### B.1 Engine and samples (curl scene, 1080x1080, OIDN denoise on, two frames each, times are per frame)

| Variant | s / frame | PSNR vs current (64 spp) at 512 px delivery | Verdict |
|---|---|---|---|
| Cycles CPU 1080, 64 spp (current) | **7.0** | - | current production (= the owner's 8.5 min / 60 frames) |
| Cycles CPU 1080, 16 spp | **4.3** | 49.1 dB | visually identical; **adopt** |
| Cycles CPU 1080, 8 spp | ~4.9 (*) | - | no gain over 16 (denoiser-bound) |
| Cycles CPU 864, 16 spp | 3.9 | - | small gain, 1.3x downscale margin |
| Cycles CPU 720, 24 spp | 2.2 | 40.7 dB | slightly soft (delivery is 1.08x of render); acceptable for quick previews |
| Cycles CPU 540, 32 spp | 1.5 | 39.8 dB | preview only |
| EEVEE 1080, 16 TAA | 2.8 (+15.6 s shader warm-up on frame 1) | 31.9 dB | different look: flatter, no SSS, soft-shadow speckle; would need a second lighting rig. **Reject** |
| EEVEE 720, 16 TAA | 1.9 | 31.5 dB | reject |
| Cycles CPU 1080, 64 spp, mesh decimated to 25 % (196k verts) | 7.2 | 55.7 dB | identical look, **zero speed-up**, +18-24 s decimate per scene. Render time is pixel/sample-bound, not geometry-bound. Keep the 783k mesh |
| Cycles GPU (RTX 4070, when free) 1080, 64 spp | 4.1 | 72 dB | 1.7x over CPU at 64 spp; worth using only when llama-server is down |

(*) the second timing batch overlapped with another `build_equipment.py` run that someone started on the machine (load average 51), so the 8-spp and the render-border rows are unreliable; re-measure render-border on an idle machine (expected 2-3x gain, see B.2).

Scene setup is cheap: load of the 46 MB character 0.9 s, contact-fitted grip 0.2 s, pose evaluation 0.15 s/frame (783k verts through the armature). The per-frame cost is almost entirely path tracing + denoising of 1.17 Mpx.

### B.2 Render only what is delivered

* **Render border**: the camera is static and the figure covers ~30 % of the canvas, so `scene.render.use_border = True` + `use_crop_to_border = True` with the per-clip union bbox (+32 px safety) should cut path-tracing work by ~3x at identical quality. The bbox is known before rendering: project the evaluated mesh + equipment vertices of all keyframes through the camera with numpy (783k x 4 matrix multiply ≈ 50 ms per frame). My one test of this ran under heavy external load and is inconclusive; verify first thing on an idle machine. If it holds, 1080/16 spp drops to ~1.5-2 s per frame.
* **Render resolution vs delivery**: keep rendering at 1080 (1.6x supersampling of the 512 px delivery, cleanest edges and alpha), or 864 if the border trick does not pan out; never render at delivery size (the denoiser softening shows at 1:1).
* **Denoiser**: OIDN with albedo+normal passes is already on; keep. 16 spp with adaptive sampling (`adaptive_threshold 0.05`) is the floor; the skin SSS and the mint highlight are flat enough that noise is gone at 16.

### B.3 Batching

One Blender process per exercise currently pays ~2 s of start-up plus the 1 s character load; negligible against 21-40 frames x 4 s. Still, the generic renderer should: start Blender once per batch (list of specs), load the character and equipment library once (`bpy.data.libraries.load`), and loop specs, resetting the pose and swapping equipment between them. This also keeps OIDN/Cycles kernels warm. Use `-t 32` (all threads) for one process rather than several processes in parallel: Cycles already saturates the CPU and parallel processes just thrash the cache. Run batches at night via a queue file; the owner's GPU stays untouched (`RENDER_DEVICE=CPU`).

### B.4 Unique frames only

* **Delivered frame count**: 20 fps x 2 s = 40 frames (not 60). Set `scene.render.fps = 20` and author the cycle in normalised phase as now.
* **Ping-pong**: a 1:1 tempo cycle only needs frames for phase 0 to 0.5 (21 frames) and the packager mirrors them. This halves render time for the ~70 % of archetypes whose concentric and eccentric paths are the same (curls, presses, rows, raises, extensions, squats, hinges, bridges, calf raises, crunches ...). The curl as authored has a 0.42/0.45 asymmetric tempo plus a top squeeze, so its mirrored version would differ by up to 3.2/255 per frame; the recommendation is to author all symmetric archetypes with equal up/down timing and a 2-frame top hold, which reads perfectly well as a form demo and makes the loop seam exact by construction. Full 40-frame cycles stay for asymmetric motions: olympic lifts, swings, jumps, carries/walks, negatives, yoga flows, burpees.
* **Holds and stretches**: 1 frame each (112 entries, measured count: 55 `isometric_hold_or_carry` + 76 `mobility_stretching` = 131, minus 9 carries and 12 flows that are animated; the brief's "157" is not what the catalog contains).
* **Characters**: the female character re-uses every spec unchanged; only the mesh, the muscle masks and the contact-fit (grip, foot, seat) re-run. No re-authoring.

### B.5 Total render budget

| Item | Frames | s/frame | Hours |
|---|---|---|---|
| ~340 symmetric clips x 2 characters x 21 frames | 14 300 | 4.3 | 17 |
| ~150 asymmetric clips x 2 x 40 frames | 12 000 | 4.3 | 14 |
| 224 stills | 224 | 4.3 | 0.3 |
| per-clip setup (load, fit, bbox pre-pass, equipment) | 1 200 x 5 s | | 1.7 |
| **Library, first pass** | **~26 500** | | **~33 h** |
| QA re-renders (30 %) | | | +10 h |
| with render border, if confirmed (x0.4) | | | ~17 h |

Five to six overnight runs at 1080/16 spp; two to three with the border trick. The 8.5-min-per-exercise figure becomes ~1.5 min (symmetric, 21 frames) to ~3 min (40 frames).

---

## C. Production structure: motion archetypes

### C.1 Clustering

The 601 catalog entries cluster into **44 archetypes** by rule (`plan/classify.py`, first-match regex on the name with the `type` field deciding static vs animated). 489 entries are animated, 112 are stills. Full id lists are in Appendix A; the table below gives counts, equipment spread (from the catalog `equipment` field) and the archetype's shared base motion.

| Archetype | n | Equipment spread | Base motion (what the procedural pose does) |
|---|---|---|---|
| stretch_static | 64 | Other 53, Bands 11 | single pose, still |
| hold_static | 48 | Other 48 | single pose, still (planks, levers, yoga holds, wall sit, dead hang ...) |
| press_horizontal | 33 | Barbell 15, Dumbbell 7, Machine 4, Kettlebell 4, Cable 1, Other 1, Bodyweight 1 | supine on bench (angle param) or floor; bar/handles from chest to lockout; params: grip width, bench angle, implement, pause |
| curl | 33 | Dumbbell 12, Barbell 11, Kettlebell 4, Machine 3, Cable 2, Bodyweight 1 | elbow flexion; params: supination (curl/hammer/reverse/zottman), seat/incline/preacher/concentration support, one- or two-arm |
| hinge | 27 | Barbell 10, Dumbbell 7, Kettlebell 5, Bands 2, Machine 2, Bodyweight 1 | hip hinge with neutral spine; params: knee bend (RDL vs conventional), stance (sumo), load position, single-leg/kickstand, range (rack pull, deficit) |
| press_vertical | 22 | Barbell 7, Dumbbell 5, Kettlebell 5, Machine 4, Bodyweight 1 | shoulders to overhead; params: seated/standing, implement, path (Arnold rotation, push-press leg drive), one-arm |
| squat | 22 | Barbell 6, Dumbbell 5, Bodyweight 3, Other 3, Machine 2, Kettlebell 2 | bilateral squat; params: load position (back/front/goblet/overhead/zercher), stance, depth, box, jump, pause |
| lunge_split_squat | 22 | Bodyweight 8, Dumbbell 4, Kettlebell 4, Machine 3, Barbell 2, Other 1 | split stance knee flexion; params: forward/reverse/walking/lateral/Bulgarian/step-up, load |
| triceps_extension | 20 | Dumbbell 7, Barbell 4, Cable 4, Kettlebell 3, Machine 1, Bodyweight 1 | elbow extension; params: overhead/lying/kickback/pushdown orientation, implement |
| push_up | 18 | Bodyweight 15, Other 3 | prone press; params: hand width, incline/decline, knees, rings/ball, explosive |
| olympic_lift | 18 | Kettlebell 9, Barbell 8, Dumbbell 1 | hinge-pull-catch(-jerk) sequence; full 40-frame cycle; params: clean/snatch/jerk, hang/dead start, split |
| leg_raise | 17 | Bodyweight 13, Other 4 | hip flexion with fixed trunk; params: hanging/supine/captain's chair, knee bend, range |
| pull_up | 16 | Bodyweight 12, Machine 1, Bands 1, Other 2 | vertical pull on bar/rings; params: grip width & orientation, assist, muscle-up transition, negative-only |
| row_bent_over | 16 | Barbell 7, Kettlebell 3, Dumbbell 2, Machine 2, Cable 2 | hinged torso, elbows back; params: implement, grip, one-arm with bench support |
| cardio_locomotion | 14 | Other 14 | run/step/jump/pedal cycles; full cycle, machine-specific seat/handles |
| hip_thrust_bridge | 13 | Bands 3, Bodyweight 3, Barbell 2, Machine 2, Dumbbell 1, Kettlebell 1, Other 1 | hip extension from floor or bench; params: shoulder support, load, single-leg |
| crunch_situp | 13 | Bodyweight 10, Cable 1, Machine 1, Other 1 | trunk flexion; params: range (crunch vs sit-up), rotation, decline, cable/kneeling |
| hip_abduction_adduction | 12 | Bands 7, Bodyweight 3, Machine 2 | hip ab/adduction; params: side-lying/standing/seated, band |
| raise_lateral_front | 12 | Dumbbell 4, Cable 3, Barbell 2, Machine 2, Bodyweight 1 | straight-arm shoulder raise; params: plane (lateral/front), seated |
| yoga_flow | 12 | Other 12 | two-pose oscillation (cat-cow, down-dog to ... ), full cycle, 3-4 s |
| calf_raise | 11 | Machine 7, Bodyweight 2, Barbell 1, Dumbbell 1 | plantar flexion; params: standing/seated/donkey, machine, single-leg |
| fly | 10 | Dumbbell 5, Machine 2, Bands 1, Cable 1, Bodyweight 1 | horizontal shoulder ad/abduction; params: bench angle, reverse (rear delt) |
| wrist_curl | 9 | Dumbbell 3, Barbell 2, Kettlebell 2, Cable 1, Other 1 | wrist flexion/extension with forearms supported |
| floor_core_dynamic | 9 | Bodyweight 8, Other 1 | quadruped/plank limb cycles (dead bug, bird-dog, mountain climber, bear crawl) |
| carry_walk | 9 | Kettlebell 4, Dumbbell 3, Bodyweight 1, Other 1 | walk cycle in place with load; full cycle |
| pullover | 8 | Barbell 4, Kettlebell 2, Dumbbell 1, Other 1 | supine arc over the head |
| shrug | 8 | Barbell 3, Dumbbell 2, Machine 2, Kettlebell 1 | scapular elevation |
| dip | 8 | Bodyweight 6, Machine 1, Other 1 | supported elbow flexion; params: bars/bench/rings, lean |
| rotation_antirotation | 8 | Kettlebell 3, Dumbbell 2, Bodyweight 2, Cable 1 | trunk rotation / side bend / halo / Pallof |
| leg_curl | 8 | Machine 3, Bodyweight 2, Other 2, Bands 1 | knee flexion; params: lying/seated/nordic/ball/TRX |
| pulldown | 7 | Cable 5, Machine 2 | seated vertical pull; params: grip, one-arm, straight-arm |
| leg_press | 7 | Machine 7 | machine squat; params: foot placement, single-leg, hack/horizontal |
| upright_row | 6 | Barbell 2, Cable 1, Dumbbell 1, Kettlebell 1, Machine 1 | elbows-high pull |
| row_chest_supported | 6 | Dumbbell 3, Barbell 1, Kettlebell 1, Machine 1 | prone on incline bench, row |
| face_pull | 5 | Cable 2, Bodyweight 2, Dumbbell 1 | high pull to face with external rotation |
| pistol_squat | 4 | Bodyweight 2, Dumbbell 1, Kettlebell 1 | single-leg squat |
| row_inverted | 4 | Bodyweight 4 | supine horizontal pull under bar/rings/TRX |
| thruster_combo | 4 | Kettlebell 3, Barbell 1 | squat/lunge + press chained (40 frames) |
| row_seated | 4 | Cable 2, Other 2 | seated horizontal pull (cable row, rower, sled) |
| back_extension | 3 | Other 1, Machine 1, Bodyweight 1 | hip/spine extension on 45-degree bench / floor |
| glute_kickback | 3 | Bodyweight 2, Cable 1 | quadruped/standing hip extension |
| swing | 3 | Kettlebell 2, Dumbbell 1 | ballistic hinge, full cycle |
| leg_extension | 3 | Machine 2, Bands 1 | seated knee extension |
| rollout | 2 | Barbell 1, Other 1 | kneeling roll-out |

(Eleven archetypes cover 300 of the 489 animated entries; the 10 largest animated ones cover 55 %.)

### C.2 Exercise = archetype + equipment + overrides

Each archetype is one Python module `archetypes/<name>.py` exposing `pose(rig, phase, p)` (phase 0-1, `p` = merged parameters) and `setup(scene, p)` (equipment placement, contact fitting, camera preset). Each exercise is a small YAML (section D) that names the archetype, the equipment asset ids (`output/equipment-scan.json` already gives the mapping) and only the parameters that differ from the archetype defaults:

* **Grip / hand**: width (`shoulder x 1.0 / 1.5 / 0.6`), orientation (`pronated | supinated | neutral`), one-arm side, implement handle radius (dumbbell 15.5 mm, barbell 14 mm, EZ 14 mm, kettlebell 17 mm, cable rope 18 mm ...). The contact-fit `fit_grip()` from the curl generalises to any handle given its axis and radius.
* **Stance / support**: feet width, toe-out, split or staggered, seated (bench, machine seat), supine/prone (bench angle -15 / 0 / 30 / 45 / 90 degrees), kneeling, hanging, wall.
* **Range and tempo**: start/end joint angles as a scalar range override, cycle length (1.6-3.0 s), symmetric (ping-pong) or full, top/bottom holds in frames.
* **Target muscles**: taken from the catalog `muscle` field through `muscles.APP_MUSCLE` (all 18 app muscles are mapped); optional secondary list.
* **Camera**: one of six presets (`front34_standing`, `side_bench`, `front34_floor_low`, `side_floor_low`, `back34_standing`, `front_machine`) with a per-exercise azimuth tweak.
* **Equipment anchors**: machines and benches get named empties (`seat`, `pad`, `handle.L/R`, `footplate`) in `equipment.py` so a spec can say `attach: {hands: handle, hips: seat}` and the IK solves the rest.

Example: `ez-bar-curl.yaml` = archetype `curl`, equipment `ez_bar`, `grip: {width: 0.9, orientation: supinated_ez}`; `preacher-curl.yaml` adds `support: preacher_bench, camera: front34_standing(azimuth -40)`; `cable-hammer-curl.yaml` sets `equipment: [cable_station, att_rope], grip: {orientation: neutral}` and the cable's attachment path keeps the rope pointing at the pulley.

### C.3 Static holds and stretches

One still per entry (112 x 2 characters), rendered with the same pipeline at phase 0.5 of a 1-frame "cycle" (no loop or seam checks, highlight check still applies). The app adds breathing (A.5). If a specific hold really needs motion (e.g. "Dead Hang" scapular micro-movement) it can be re-typed as a 21-frame ping-pong later by changing one YAML field; nothing else changes.

---

## D. Data-driven spec and QA gate

### D.1 Spec format (one YAML per exercise, `animation/specs/<exercise-id>.yaml`)

```yaml
id: standing-dumbbell-curl            # ExerciseCatalog id (join key)
archetype: curl                       # archetypes/curl.py
characters: [male, female]            # default; omit = both
equipment:
  - asset: dumbbell                   # assets/equipment/<id>.blend
    attach: hand.L
  - asset: dumbbell
    attach: hand.R
params:                               # only overrides of the archetype defaults
  grip: {orientation: supinated, width: 1.0, handle_r: 0.0155}
  stance: {width: 0.9, toe_out: 8}
  range: {elbow: [10, 130]}
  cycle: {seconds: 2.0, mode: pingpong, hold_top: 2, hold_bottom: 1}
  support: none                       # bench45 | preacher | seat | floor | wall ...
camera: {preset: front34_standing, azimuth: -28, elevation: 9, distance: 3.75, lens: 60}
muscles: {primary: [biceps], secondary: [forearms]}    # default: from catalog via APP_MUSCLE
output: {fps: 20, height: 512, formats: [mp4_stacked, webp, still]}
qa: {expect_contact: [hand.L, hand.R], planted: [foot.L, foot.R]}
```

The generic driver: `blender -b --python render_exercise.py -- --specs specs/*.yaml --character female --out renders/lib/` loads the character once, then per spec: build equipment, run contact fits, compute the union bbox pre-pass, render the unique frames (border-cropped), write `frames/`, call the packager, run QA, append to `manifest.json`. A spec hash (YAML + archetype module + character file + equipment files) is stored next to the output so re-runs skip clips whose inputs did not change.

The manifest the app consumes (`clips/v1/manifest.json`): `{id, character, w, h, frames, fps, loop: "pingpong|full|still", mp4: {url, bytes, sha256}, webp: {...}, still: {...}, crop: [x,y,w,h] in 1080 space}`.

### D.2 Automated QA gate (every clip, every character; fail = clip not published)

| Check | How | Threshold |
|---|---|---|
| Loop seam | existing `tools_package.py` test: `|last - first|` vs neighbour step | seam <= 1.05 x max step (ping-pong clips pass by construction) |
| Frame pops | neighbour mean-abs-diff series | max step <= 3 x median step |
| Grounding / foot sliding | per frame, lowest vertex z of each `planted` bone's vertex group (evaluated mesh) and its xy centroid | z within 3 mm of the floor plane (or seat/pad surface), xy drift <= 5 mm over the clip |
| Hand-equipment contact | distance of each fitted phalanx tip to the handle axis, per frame (same math as `fit_grip`) | within handle_r + finger_half +/- 3 mm; handle centre stays inside the palm sphere (r 45 mm) |
| Interpenetration | `mathutils.bvhtree` overlap of body vs equipment and forearm/upper-arm vs torso vertex groups (25 % decimated proxy for speed, ~0.2 s/frame) | 0 overlapping faces outside the declared contact set, else penetration depth <= 4 mm |
| Highlight present | `hl` attribute max >= 0.9 before render; in the PNGs, pixels within 12 degrees of the mint hue (#00BFA5) | >= 0.3 % of opaque pixels in >= 80 % of frames (visible from this camera) |
| Framing | union alpha bbox vs canvas | >= 16 px margin on every side at 1080 (nothing clipped); crop recorded |
| Alpha sanity | no fully transparent frame; semi-transparent fraction | < 2 % (catches missing film_transparent or fog) |
| Size budget | packaged bytes | mp4 <= 60 KB, webp <= 300 KB, still <= 20 KB (warn, not fail) |

All checks run from the Blender process (geometry) and from the packager (pixels) and write `qa.json` + the contact sheet; a one-page HTML wall of all contact sheets per batch is generated for the human pass.

### D.3 Human spot-check cadence

* 100 % review (loop video + contact sheet, both characters) of the **first exercise of every archetype** (44 clips).
* 1 in 5 of the remaining variants per archetype, all machine-based exercises (seat/handle anchors are the riskiest), all olympic/combo clips.
* 100 % of the first female batch; afterwards female clips follow the same 1-in-5 rule.
* Weekly 30-minute pass over the contact-sheet wall of that week's batch; anything flagged goes back as a YAML fix (never a hand-edit of frames).

---

## E. First batch and roadmap

### E.1 Next 3-4 exercises (before the female character)

| # | Exercise (id) | Why |
|---|---|---|
| 1 | **Barbell Bench Press** (`bench-press`) | Supine body on a bench, two-hand bar grip, bar + plates + rack clearance, side camera preset. Unlocks `press_horizontal` (33) and the bench/anchor mechanism used by `fly`, `pullover`, `row_chest_supported`, `triceps_extension` lying variants (~60 more). |
| 2 | **Barbell Back Squat** (`squat`) | Lower-body IK with feet planted (grounding QA), bar on the back, hip/knee/ankle chain shared with `lunge_split_squat` (22), `hinge` (27), `leg_press`, `calf_raise`, `hip_thrust_bridge` (~90). Biggest coverage lever after the curl. |
| 3 | **Lat Pulldown** (`lat-pulldown`) | First machine: seat + thigh pad anchors, cable + lat bar attachment with a moving cable, vertical pull. De-risks all 85 Cable/Machine entries (`pulldown`, `row_seated`, `leg_press`, `leg_extension`, `leg_curl`, machine presses and curls). |
| 4 | **Push-Up** (`push-up`) | Bodyweight on the floor: prone pose, four contact points, low wide framing (landscape crop), no equipment. Unlocks `push_up` (18), `floor_core_dynamic` (9), the floor-based holds/stretches (~60 stills) and the floor camera preset. |

After these four (plus the curl) the pipeline has touched: free weight in hand, bar in two hands, bench, rack, machine seat + cable, floor contact, standing/supine/prone/seated bodies, and all six camera presets. That is the right moment to add the female mesh: every contact-fit and anchor gets exercised on the second body at once.

### E.2 Phased roadmap (one person, workstation rendering overnight)

| Phase | Content | Estimate |
|---|---|---|
| 0. Delivery plumbing | `tools_package.py` → crop + stacked MP4 + WebP + still + manifest; verify render border on an idle machine; app: Coil WebP path (`coil-gif`), manifest fetch + disk cache, still placeholder, breathing modifier | 3-4 days |
| 1. First batch | bench press, back squat, lat pulldown, push-up as archetype modules + YAML; camera presets; machine anchors for `lat_pulldown`, `flat_bench`, `power_rack`; QA gate v1 (seam, grounding, contact, highlight) | 2 weeks |
| 2. Female character | build on the same skeleton (HBM female base), muscle masks, re-fit contact for the 5 exercises, review both | 1 week |
| 3. App MP4 path | MediaCodec stacked-alpha decoder + frame player, fallback logic, prefetch; switch manifest primary to MP4 | 3 days |
| 4. Archetype sprint | remaining 38 archetypes at ~0.5 day each (pose function + first exercise + QA), highest-count first (hinge, press_vertical, lunge, triceps, pull_up, row, olympic last) | 4 weeks |
| 5. Variant sprint | 489 - 44 YAML overrides at ~10 min each incl. a render + QA look, batched by archetype; 112 stills; nightly renders (~33 CPU-hours total) | 3 weeks |
| 6. Release | Cloudflare R2 upload, manifest v1, bundle the top-20 clips, analytics on clip fetch failures | 2 days |

Total about 11-12 weeks of authoring; machine time is ~40-50 CPU-hours spread over nights and never blocks the GPU.

---

## Appendix A. Archetype → exercise ids (601 entries, generated from `ExerciseCatalog.kt`)

Rules live in `plan/classify.py`; regenerate when the catalog changes. Machine-readable copy: `plan/archetypes.json`.

