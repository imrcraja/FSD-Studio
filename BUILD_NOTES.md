# FSD Studio — Build Notes

## App icon (owner decision, 2026-10-22 — overrides the original spec fallback)

The owner supplied two logo images and personally chose the **3D glossy voxel "F"**
tile (chunky blocky F in purple/blue/white on a dark purple rounded-square tile).
Source file: `~/workspace/user/files/28886_1_n3aw.png` (1254×1254). This replaces
the earlier emoji-diamond (`fsd_diamond`) fallback everywhere. The flat minimal "F"
image (`28887_0_z22g.jpg`) was explicitly **not** chosen and is not used anywhere.

Generated assets (all resized from the chosen source with PIL/LANCZOS):

- **Legacy mipmaps** — full-canvas resizes of the tile art:
  `mipmap-mdpi` 48×48, `mipmap-hdpi` 72×72, `mipmap-xhdpi` 96×96,
  `mipmap-xxhdpi` 144×144, `mipmap-xxxhdpi` 192×192, each as both
  `ic_launcher.png` and `ic_launcher_round.png` (identical art; the launcher
  applies the round mask on device).
- **Adaptive icon** (`mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`):
  background `@color/ic_launcher_background` is now **#160A2A** (the app
  background). The foreground `drawable-nodpi/ic_launcher_foreground.png`
  (432×432) is the owner's tile extracted from the source's black letterbox
  margins (tile bbox 67,65–1185,1183) and placed at 400×400 (~93%) centred over
  #160A2A — tile art kept intact with a minimal inset so launcher masks don't
  clip the artwork's edges.
- **In-app logo** — `drawable-nodpi/fsd_logo.png` (512×512, full-canvas resize),
  wired into the UI: Home screen header (54dp, rounded) and the Settings → About
  card (72dp, rounded), for splash/about branding.

The old diamond foreground PNG was overwritten by the new foreground; no code or
resource references the diamond or the unchosen flat-F image.

## CI build result (2026-10-22)
- Repo: https://github.com/fsdetc/FSD-Studio (created via browser; the GitHub connector's write calls returned 403 "Resource not accessible by integration" for this repo, so all 61 files were uploaded via the GitHub web UI in 20 commits; workflow file last).
- Run #1 FAILED at :app:compileDebugKotlin — 23 errors in 3 files (missing strings ×14, SettingsCard receiver typed as `Column` instead of `ColumnScope`, nullable `ResponseBody?` in SkinRepository ×3). All fixed locally, re-uploaded (commits d110f8d, 9833a13, ae51775).
- Run #4 "Fix compile errors" (39697669203): SUCCESS in 1m 34s. Artifact FSD-Studio-V1-debug downloaded; APK verified locally (18.9 MB, manifest + dex + launcher icons + fsd_logo present) and delivered to the owner as ~/workspace/your_files/FSD-Studio-V1.apk (sha256 e9a630f711ef44e1c368eea6c6c10dd55e12a91d148fb5910943b04d5988f88e).
- NOTE: CI debug builds are signed with the runner's ephemeral debug key — a later test APK may need an uninstall-before-install on the phone. The launch build will use one fixed release key.

## V2 changes (version 1.1, versionCode 2 — 2026-10-22)

### Plans & entitlements rework
- `Plan { FREE, PRO, MAX }` replaces the `isPro` boolean (DataStore key `plan`;
  migration in Entitlements: stored `is_pro=true` → PRO). Single source of truth:
  poses FREE 10 / paid 18, max size 512 / 1024, downloads 5/day FREE else
  unlimited, AI/day FREE 3 / PRO 3 / MAX 50, AI boost (Gemini) = MAX flag.
- All one-time-purchase copy removed. Display prices: Pro ৳49/wk · ৳99/mo,
  Max ৳79/wk · ৳169/mo.

### Download gate
- `data/DownloadGate.kt` + DataStore counter `dl_<yyyy-MM-dd>` guard every
  gallery save (pose detail, mob detail, local/history detail, AI result):
  FREE at 5/day → toast "Daily free downloads used up" + open Plans, no save.
  Remaining count shown on RenderDetail (chip line) and on Home under the
  character card. On the 3rd successful FREE download of a day, a once-per-day
  (`popup_<date>` flag) AlertDialog offers the plans — never on app start.

### Subscription screen
- `ui/ProScreen.kt` replaced by `ui/SubscriptionScreen.kt` (route "pro"
  unchanged): Weekly/Monthly toggle, PRO (solid) and MAX (gradient + "Best"
  badge) cards with ✓/—/value feature rows, big price, "Choose Pro/Max" →
  dialog "Purchases open at launch… no payment is taken", current-plan badge,
  and a Free/Pro/Max comparison mini-table. Upgrade touchpoints: Home crown
  icon-button, locked pose/size, download limit, Settings plan card.

### Character library
- New `ui/LibraryScreen.kt` + `data/LibraryCatalog.kt`. Players: Mr FSD
  (Creator pick, static CharacterInfo from the known profile), Steve/Alex via
  the new `CharacterInfo.renderId` ("steve"/"alex"; render URLs use renderId
  everywhere), Notch/Dream via username search. Mobs: 12 mobs with
  skinrender.dev mob renders; tapping a mob opens RenderDetail in MOB mode
  (pose chips per mob, background selector, gated Download + Share, history
  kind "MOB"). Bottom nav unchanged (5 tabs); Library opens from a Home button.

### Themes & settings
- `AppTheme { FSD_PURPLE, MIDNIGHT, LIGHT }` in theme/Theme.kt; all screens now
  read MaterialTheme.colorScheme instead of hard-coded Fsd* colours (the Fsd*
  values are the FSD_PURPLE scheme). Settings: theme radio rows, default render
  background (`default_bg`) and size (`default_size`, plan-clamped) applied as
  RenderDetail's initial selection, plan card → Subscription.
- Settings AI section: optional personal Gemini API key (`gemini_key`, stored
  in app-private DataStore, never leaves the device except to Google's API),
  Save/Clear, explanation + active-engine status line.

### AI provider layer + errors
- `data/AiEngine.kt`: PollinationsEngine (unchanged behaviour: flux+reference →
  flux → turbo) and GeminiEngine (stored key; downloads the character body
  render as the reference image; model order gemini-2.5-flash-image →
  gemini-3.1-flash-lite-image; 429 → Quota, 400/403 → BadKey, timeout →
  Timeout). With a key, Gemini runs first and any failure falls back to the free
  engine with a small "Gemini unavailable — used free engine" note.
- `data/UiErrors.kt`: every network surface maps failures to human strings
  (offline / timeout / server busy / not found / generic) — no raw exception
  text in UI; grid cells show a tap-to-retry placeholder (GridThumb), the
  detail screen shows an error + Retry, Home search and the AI screen use the
  typed messages too.

### Performance
- Coil request sizing: pose grid thumbnails request size=320, library
  thumbnails 256, detail keeps 512/1024 — the API renders at the requested size,
  so grids no longer download full-size renders.
- Coil ImageLoader (app singleton in AppContainer) is now explicit: memory
  cache capped at 15% of app memory, disk cache 50 MB; OkHttp ETag caching from
  V1 is unchanged (render URLs are immutable, so repeats are cache hits).
- Lazy grids use stable item keys (poses, library, history) to avoid
  recomposition churn; all repository calls are wrapped in Result/runCatching
  at the ViewModel boundary so IO failures surface as UI errors, not crashes.
- No fake claims: no R8/shrink tuning or baseline profiles were added in V2;
  the wins above are the request-size, cache and error-handling changes.

## V2 CI result (2026-10-22)
- 30 changed/new files uploaded via web UI (8 "V2 update" commits), ProScreen.kt deleted (9d7ead1). Final run #13 (39703789301): SUCCESS in 1m 27s.
- V2 APK verified locally and delivered as ~/workspace/your_files/FSD-Studio-V2.apk. Gemini verdict stands: free-tier image quota returns 429 on the owner's key (tested gemini-2.5-flash-image, gemini-3.1-flash-image, gemini-3.1-flash-lite-image) — V2 ships Pollinations as the free engine + optional user-supplied Gemini key in Settings.

## V3 changes (versionCode 3 / versionName "1.2", 2026-10-22)

Owner testing feedback round + a spec amendment (owner decision) that removed
the user-supplied Gemini key and turned the AI tab into edit mode.

### Bug fixes
- **Monthly price bug (SubscriptionScreen):** the billing toggle's state was
  wired per-card, so tapping "Monthly" could leave prices showing the weekly
  values. Now ONE hoisted `billingPeriod` (rememberSaveable) drives all four
  values — Pro ৳49/Max ৳79 "/week" or Pro ৳99/Max ৳169 "/month" — derived in
  exactly one place for both cards. Compare table pose counts updated to
  12 / 30 / 30; "All 30 poses" copy.
- **Message placement:** transient messages (saved / failed / limit) no longer
  appear as top banners or Toasts over content (the limit message used to hide
  the Max card's name + "Best" badge). AppContainer now has a snackbar bus;
  the Nav Scaffold shows a bottom-anchored SnackbarHost above the bottom bar.
  Limit-hit still posts the message AND navigates to Plans. All Toasts removed.
- **Locked pose previews (PosesScreen):** locked cells now LOAD the real
  320 px render under a 60% black scrim + centered lock icon + PRO chip —
  never blank. Only the tap stays locked (→ Plans).

### Watermark (previews only)
- `WatermarkOverlay` (ui/components.kt): Canvas overlay, "FSD Studio" tiled
  diagonally (-30°, white ~14% alpha, bold, clipped to 16 dp rounded shape).
- Applied over the big previews in RenderDetailScreen (player, mob AND local
  modes), the AiScreen result, and the StudioScreen preview.
- Downloads stay clean: verified every gallery save uses raw downloaded
  bytes/files (never a composited bitmap), all behind DownloadGate.saveWithGate.

### Poses library — the main feature, now 30
- New `data/PoseCatalog.kt`: `PoseDef(id, @StringRes labelRes, renderType,
  params, free)`; catalog order = display order; 12 FREE first (default, walk,
  wave, sit, dab, hero_closeup [bust], look_left, run, sneak, cheer, dance,
  side_profile), then 18 locked (originals + back_view, walk_stride,
  run_stride, dance_freeze, look_right, look_up, hero_angle, top_view,
  shadow_stand). Recipes use only documented params (pose, frame, yaw, pitch,
  headYaw, headPitch, shadow).
- `Api.renderUrl` gained an `extra: Map<String,String>` merged into the query;
  the hard-coded `Api.POSES` list is gone. `DetailRequest.RemotePose` carries
  renderType + extraParams. `Entitlements.maxPoseIndex` replaced by
  `poseUnlocked(pose, plan)` (free poses for everyone; all for Pro/Max).

### Studio tab (NEW, free) — ui/StudioScreen.kt, 6th bottom tab
- Works on the current character. No character → empty state with Home /
  Library buttons. UPLOAD source → flat preview + honest note (Studio needs an
  online skin) — no fake controls.
- Preview: body render (512) built from committed state, WatermarkOverlay +
  BgSelector. Controls: full pose-chip catalog (locked → Plans; selecting a
  pose initialises camera/joints FROM that pose's recipe), camera sliders
  (yaw -180..180, pitch -60..60, roll -45..45, zoom 0.5..2.5), position
  (offsetX/offsetY -0.5..0.5), expandable advanced joints (headYaw/headPitch
  -90..90, arm pitches -120..120, leg pitches -90..90), toggles
  shadow/cape/overlay, Reset.
- API discipline: sliders edit a local value while dragging; the render URL
  (Coil key) changes ONLY on slider release or chip/toggle tap.
- Export: plan size (512 free / 1024 paid) → DownloadGate → gallery + History
  record kind "STUDIO", title includes the pose label. Downloads-left shown.

### Languages — real in-app switching (6)
- Full translations created: values-hi, values-bn, values-ur, values-es,
  values-id — every key translated (first-pass), placeholders preserved,
  brand terms kept. Verified: 223 keys in each of the 6 files, identical key
  order, zero %1$s/%1$d mismatches, all XML parses.
- Mechanism: DataStore `app_language` mirrored into SharedPreferences
  ("fsd_prefs") for synchronous reads; MainActivity.attachBaseContext wraps
  the context with that Locale. Settings language rows are real radios for
  English / हिन्दी / বাংলা / اردو / Español / Bahasa Indonesia — selecting
  one saves and recreates the activity. No "coming soon" rows remain.

### Account, User ID, Log out, Reset (Settings)
- New Account card at top: avatar = current character's face render (else the
  app logo), name = character name or "Guest creator", User ID row — the id is
  "FSD-" + 10 uppercase hex chars from a UUID, generated once into DataStore
  `user_id` — with a Copy button (clipboard + "User ID copied" snackbar).
  The owner can use this ID server-side later to manage plans/bans.
- Log out (confirm dialog): clears character, plan (→ FREE), theme / render
  defaults / language; KEEPS history, daily counters and the user id.
- Reset app data (danger, confirm dialog): wipes all DataStore prefs, Room
  history rows + history files, Coil + OkHttp caches; user id regenerates.
- "Replay tutorial" row added (see below).

### First-time tutorial — ui/TutorialScreen.kt
- 4 pages (Welcome / Find your character / Poses & Studio / AI & downloads),
  simple state-based page switcher (no new dependency), Skip + Next /
  "Get started", dot indicators. Shown from the Nav host only while
  `seen_tutorial` is not true (null-loading state shows a blank frame, never
  an app flash); finishing/skipping sets it. It calls no API and consumes no
  limits. Settings replay sets the flag false.

### AMENDMENT (owner decision): Gemini key REMOVED, AI = built-in edit mode
- The "Your Gemini API key" feature is removed completely: no key field in
  Settings, no Save/Clear UI, no `gemini_key` anywhere, no GeminiEngine, no
  engine selection. The Settings AI section is a single info row: "AI edits
  are built in — no setup needed." AI runs on ONE built-in engine
  (Pollinations, keyless) with no user setup. Log out no longer touches any
  key (there is none).
- AI tab = EDIT MODE: the source is the current character's body render
  (512 px; the app does not persist a last-viewed pose render, so the default
  body render is the source), shown at the top labelled "Editing this
  render". The user's text is an EDIT instruction; the outgoing prompt is
  "Edit this Minecraft character render: <text>. Keep the same character, the
  same skin and outfit, and the same Minecraft blocky style — only apply the
  requested change." sent through Pollinations WITH the reference URL
  (image= param) first; the engine retries without the reference on failure.
  UPLOAD-source characters fall back to prompt-only with an honest one-line
  label. Kept: daily counters (FREE 3 / PRO 3 / MAX 50), one-in-flight,
  typed errors, result → History (kind AI, title "AI edit"), watermark on
  the result preview, gated download. Strings reframed: "Describe your
  edit", button "Apply edit".

### NOT implemented (honest note)
- **Item in hand (feedback #9):** skinrender.dev has NO item/equipment
  parameter — verified against the API's documented parameter set — so a
  held item cannot be rendered by the pose/Studio pipeline. Not faked.
  (The AI edit mode can *depict* an item from a text instruction, but that
  is AI artwork, not a render feature.)

### Self-checks (V3)
- (a) R.string.* used vs defined in values/strings.xml: zero missing (the one
  regex hit "ok" is android.R.string.ok, a framework string).
- (b) Key counts equal across values / -hi / -bn / -ur / -es / -id: 223 each,
  same order, placeholders intact.
- (c) No "coming soon" language rows remain (language card is real radios);
  no one-time/lifetime purchase copy anywhere.
- (d) DownloadGate.saveWithGate guards every gallery save: RenderDetail
  (player/mob/local via the shared helper), History items (they open through
  LocalDetail), AI download, Studio export, Library/mob (through the mob
  detail). No direct saveToGallery call exists outside a gate lambda.
- (e) No Toast and no "gemini" references remain in Kotlin or strings.

## V3.1 content expansion (2026-10-22, local — version bump at build time)

Owner's overnight instruction: posing and the character library must feel
"huge". Pure content update on top of the V3 build; no version bump here.

### Poses: 30 → 60 (free 12 → 18)
- All 30 V3 entries unchanged and in place; 30 new `PoseDef` recipes appended
  in `data/PoseCatalog.kt`, using only documented skinrender.dev params
  (frame, yaw/pitch/roll, zoom, headYaw/headPitch, shadow, bust renderType).
- 6 new FREE (lead the expansion block): walk_quarter (walk frame 0.25),
  run_start (run 0.25), look_down (headPitch 35), three_quarter (yaw -35),
  sit_relaxed (sit + headPitch 18/headYaw -15), greet_bust (wave bust,
  zoom 1.3).
- 24 new locked: walk_late, run_late, dance_quarter, dance_late, zombie_walk,
  jump_peak (frame variants), sneak_peek (sneak + headYaw 45), hero_low
  (flex, pitch -30 + shadow), flex_bust / think_bust / salute_bust /
  point_bust / dab_bust / shrug_bust (bust close-ups), face_closeup (bust,
  zoom 1.8), back_quarter (yaw 135), over_shoulder (yaw 160 + headYaw -50),
  tiny (zoom 0.5), action_tilt (run + roll 18), spin_tpose (tpose + yaw 45),
  shadow_dance (dance + shadow), power_punch (punch, roll -12, frame 0.5),
  big_cheer (cheer, pitch -15), sit_view (sit, pitch 30).
- Entitlement is the per-entry `free` flag (`Entitlements.poseUnlocked`);
  PosesScreen iterates the catalog in order with no free-first assumption,
  so the appended mixed block behaves correctly.
- Labels: 30 new `pose_*` keys (+ `lib_mobs_more`) added to all 6 values
  files at matching positions — 254 keys per file, identical order, XML
  parses, placeholders untouched.
- Count copy updated: SubscriptionScreen compare row literal
  `listOf("12", "30", "30")` → `listOf("18", "60", "60")`; `popup_body` and
  `sub_feat_poses` now say 60 poses (all 6 locales).

### Character library: players 5 → 19, mob characters +17
- VERIFY-FIRST: every candidate was checked live against
  `https://skinrender.dev/api/profile/<name>` (HTTP 200 + uuid in JSON)
  before being added — nothing was invented.
- Players (+14, username-search entries like Notch/Dream): Technoblade,
  GeorgeNotFound, Sapnap, TommyInnit, WilburSoot, Philza, CaptainSparklez,
  DanTDM, PewDiePie, jeb_, Dinnerbone, Herobrine, MHF_Steve, MHF_Alex.
  All 14 candidates PASSED — no player candidate failed verification.
- Mobs & More (+17, NEW `LibraryCatalog.MOB_CHARACTERS` + a third
  LibraryScreen section, string `lib_mobs_more`): Mojang's MHF accounts are
  real player profiles whose skins are the mobs, so they open through the
  normal character RenderDetail (not the /render/mob endpoint models, which
  stay untouched at 12). Added: MHF_Blaze, MHF_Ghast, MHF_Slime,
  MHF_CaveSpider, MHF_Enderman, MHF_Golem (Iron Golem), MHF_Squid,
  MHF_MushroomCow (Mooshroom), MHF_Ocelot, MHF_PigZombie (Zombie Pigman),
  MHF_WSkeleton (Wither Skeleton), MHF_Wither, MHF_Witch, MHF_SnowGolem,
  MHF_LavaSlime (Magma Cube), MHF_Chicken, MHF_Spider.
- FAILED verification (skipped silently, recorded here per protocol):
  MHF_MagmaCube — HTTP 404 "No player named mhf_magmacube"; the real Magma
  Cube MHF account is MHF_LavaSlime, which passed and is included.
- Library totals now: 19 players + 12 endpoint mob models + 17 mob
  characters = 48 library entries. Names are hardcoded literals in the
  catalog (existing pattern), so no new name strings were needed beyond
  the section title.

### Self-checks (V3.1)
- (a) String keys: 254 in each of values / -hi / -bn / -ur / -es / -id,
  identical key order across all six, all XML parses; every `R.string.*`
  referenced from Kotlin resolves (zero missing).
- (b) PoseCatalog: 60 entries, all ids unique, the original 30 verified
  first and in original order; the 6 new free entries lead the expansion
  block; `extra = arrayOf(...)` convention used throughout; braces/parens
  balanced.
- (c) Diff summary — PoseCatalog.kt: doc comment updated + 30 entries
  appended (no existing line altered). LibraryCatalog.kt: 14 player lines
  appended to PLAYERS + new MOB_CHARACTERS list (MOBS untouched).
  LibraryScreen.kt: one appended section block (existing sections
  untouched). SubscriptionScreen.kt: compare-row literal only.
  6 × strings.xml: 31 keys added per file + "30"→"60" inside popup_body /
  sub_feat_poses only. BUILD_NOTES.md: this section. versionCode /
  versionName NOT bumped (parent does it at build time).
