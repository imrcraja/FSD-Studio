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

## V4 changes — Skin Editor (2026-10-22, local — version bump at build time)

The flagship Studio feature from the owner's point 13: a full 64×64 skin
editor. **The core editor is completely free by the owner's rule** — painting,
colours, zoom, every tool; the ONLY plan check anywhere in the editor is
`SkinEditorViewModel.assetLocked` (premade assets flagged non-free in the
catalog are PRO/MAX), used by the asset cells and the random-outfit pool.

### Architecture (non-destructive by design)
- Two-part state in `SkinEditorViewModel`: `paintBitmap` (the 64×64 skin the
  user paints) + `outfit` (ordered slot → OutfitEntry(assetId, tint?)). The
  displayed/exported skin is always `paint + outfit patches`, composited by
  `SkinAssetRepository.composite` in the FIXED slot order
  **pants → shoes → tops → jewelry → back → head → glasses**
  (`AssetSlots.ORDER`). Painting never touches patches; removing an outfit
  chip restores the painted pixels underneath exactly.
- Start sources: (a) the current character (username/library skins load their
  texture via the render API's `skin` type, upload characters via their local
  file), (b) an uploaded skin file (SAF picker), (c) two programmatic blanks
  from `BlankSkins` — a warm skin-tone base (hair, eyes, mouth, denim shorts,
  per-face shading) and a gray mannequin matching the asset test renders.
- `SkinTemplate.kt` centralizes the classic 64×64 layout: part boxes, the six
  face rects per part/layer, and the BASE/OVERLAY region masks that the layer
  toggle paints through (painting with OVERLAY selected can only touch
  overlay-region pixels, and vice versa; eraser writes transparent).
- Tools (all free): Pencil (drag paints continuous Bresenham runs), Eraser,
  Fill (4-connected exact-colour flood fill, layer-mask constrained),
  Eyedropper (reads from the paint bitmap, then returns to Pencil),
  Undo/Redo (bitmap snapshot stack, cap 40, one snapshot per gesture),
  Clear-part (Head/Torso/Arms/Legs/Overlay/Whole — base regions reset to the
  skin-tone blank's pixels, overlay regions to transparent).
- Canvas: the full 64×64 paint template drawn nearest-neighbour large (the
  raw paint bitmap, so strokes land exactly where seen); one finger paints,
  two fingers pinch-zoom (1–12×) and pan; optional grid (auto-shown once a
  cell is ≥4px) and optional part-region tint + label overlay drawn from
  `SkinTemplate` front faces. Front/Back flat previews show the COMPOSITED
  skin (SkinBitmaps, scale ×9) and update on every edit.
- Colours: 24-color curated palette (FSD purple family + skin tones),
  HSV sliders + hex field, recent-colors row (cap 12).

### Asset library (bundled)
- `app/src/main/assets/skin_assets/`: `catalog.json` + all **130** patch PNGs
  copied from `~/workspace/skin-editor-assets/` (31 FREE / 99 PRO, 40
  tintable, 11 sections). Parsed lazily with org.json (no new dependencies);
  patches decoded on demand into an LruCache.
- Slot rules follow the asset NOTES.md: hats+masks → HEAD (one at a time);
  wings/bags/capes + seasonal vampire_cape → BACK (one at a time); seasonal
  santa_coat/christmas_sweater → TOP; pants → PANTS; shoes → SHOES (over
  pants); tops → TOP; glasses → GLASSES (over head); jewelry → JEWELRY.
  Applying a same-slot asset replaces the slot. Grid cells render the asset
  composited live onto the CURRENT painting's front view (cached per paint
  revision + outfit); locked cells are dimmed with a lock + PRO badge and
  navigate to Plans on tap.
- **Tint derivation:** pixels whose hue is within ±25° of the catalog
  `defaultColor` hue with saturation > 0.15 form the colour family; they are
  remapped by rotating hue to the tint hue and scaling saturation/value by
  the tint÷default ratios. Neutrals (hair, metal, shading detail) are
  untouched; alpha is preserved. Tinted patches are cached per asset+tint.
  UI: 10 curated tint swatches + custom HSV picker, plus "Original".
- "Random outfit" fills every slot from the plan-allowed pool (FREE users:
  free assets only), glasses sometimes skipped, 70% chance of a tint on
  tintable picks.

### Actions
- **Use as my skin** — the composite is saved through the normal upload path
  (`SkinBitmaps.importEditedSkin`: filesDir/skins/<sha>.png + flats) and set
  as the current character, so Poses/Studio/History treat it exactly like an
  uploaded skin.
- **Download skin PNG** — saves the 64×64 PNG to Pictures/FSD Studio via
  MediaStore. **Deliberately NOT through DownloadGate** (owner's rule: your
  own skin is core-free) — but it IS recorded in History as kind "SKIN".
- **Wardrobe** — named skins stored as PNG + index.json under
  filesDir/wardrobe (`WardrobeStore`); list with front-render thumbnails,
  Load / Delete. PNG-only by design (no paint/outfit recipe — Load replaces
  the painting with the saved composite).

### Classic-layout note
Painting and previews use the classic (non-slim) template mapping from
`SkinTemplate.kt` — the same mapping the asset generator used. Slim skins are
painted on their own file pixels (the file is edited in place), but the flat
front/back preview and the part labels are approximate for slim arms, since
the preview renderer uses the classic arm boxes.

### Files
- Created: `data/SkinTemplate.kt`, `data/SkinAssets.kt`,
  `data/WardrobeStore.kt`, `viewmodel/SkinEditorViewModel.kt`,
  `ui/SkinEditorScreen.kt`, `app/src/main/assets/skin_assets/` (catalog +
  130 patches).
- Modified: `data/SkinBitmaps.kt` (decodeSkinBytes/decodeSkinFile,
  importEditedSkin, renderFrontScaled/renderBackScaled), `AppContainer.kt`
  (skinAssets + wardrobe), `ui/Nav.kt` (Routes.EDITOR + wiring; bottom nav
  unchanged at 6 tabs), `MainActivity.kt` (editorVm), `ui/HomeScreen.kt`
  (prominent Skin Editor card), `ui/RenderDetailScreen.kt` ("Edit skin"
  button when the current character's skin is resolvable).
- Strings: **79 new keys × 6 locales → 333 keys per file**, identical order
  in all six values files (editor_*, tool_*, part_*, section_*, slot_*,
  home_editor_*, detail_edit_skin, action_save). versionCode / versionName
  NOT bumped (parent does it at build time).

## V4.1 changes (shipping inside the V4 build, 2026-10-22)

Owner testing feedback on V3.1: in the Studio tab the preview sat at the top
of one long scrolling column and the sliders far below it — every adjustment
meant scroll down → adjust → scroll up → check. Plus a bounded small-polish
pass. No version bump here (rides inside the V4 build).

### Studio tab — sticky preview + debounced live rendering
- **Layout:** `StudioEditor` is now a `BoxWithConstraints` column: a PINNED
  preview box at the top with height = **0.38 × available height** (never
  scrolls), and below it a `LazyColumn` (weight 1, 12 dp spaced,
  12/16 dp content padding) holding ALL controls — BgSelector, pose chips,
  camera sliders, position sliders, the expandable joints section, the
  shadow/cape/overlay toggles, the downloads-left line and the
  Export/Reset row. Slider and result are now in the same view.
- The preview keeps its render image + `WatermarkOverlay`; the no-character
  and UPLOAD-source branches above the editor are untouched (upload skins
  still get the honest flat preview). Entitlements (locked pose chips →
  Plans), the export flow (still `DownloadGate.saveWithGate`, still built
  from the LIVE draft values via `currentParams()`, never the snapshot) and
  snackbar outcomes are unchanged — verified by diff against the pre-change
  file.
- **Debounce design:** `StudioSlider` is now fully controlled — it reports
  every change, so the draft state (and value labels) update live while
  dragging. The preview URL is built only from a snapshot
  (`renderParams`/`renderBg`) refreshed by a `LaunchedEffect` keyed on the
  full draft-parameter map + bg after a **350 ms** settle delay. Rationale:
  one render request per settled position instead of one per drag frame —
  the API-discipline rule survives, but the interaction feels live; settled
  URLs are immutable so repeats hit Coil's cache. The old release-only
  comments were replaced accordingly.

### Polish fixes (one line each)
- **PosesScreen:** upload-source flats grid items got a stable key
  (`key = { it.first }`) — it was the only lazy list without one.
- **LibraryScreen:** back-arrow content description was a hardcoded
  `"Back"` literal; now `R.string.action_back` (localised).
- **RenderDetailScreen:** same hardcoded `"Back"` literal →
  `R.string.action_back`.
- **SettingsScreen:** the account-card avatar `AsyncImage` had no
  placeholder while the face render loads; it now sits on a themed
  `surfaceVariant` background (clipped with the image).

### Strings
- ONE new key `action_back` ("Back" / वापस / পেছনে / واپس / Atrás /
  Kembali), inserted after `action_retry` at the identical position in all
  6 values files → **334 keys per file**, identical key order (verified by
  hash), all XML parses, zero missing `R.string.*` (the single grep hit is
  the pre-existing framework `android.R.string.ok`).

### Deliberately left alone
- HomeScreen / HistoryScreen cell images: they already sit on themed
  `surfaceVariant` placeholder boxes, so no placeholder was added there.
- `FramedImage` (Studio/detail previews) keeps plain `AsyncImage`: its
  background (checkerboard/solid) is the placeholder and its onSuccess/
  onError contract drives history recording — swapping in a loading
  composable would be a behaviour change, out of scope for polish.
- Settings radio rows (language/theme) still respond to the radio button
  only, not the whole row — a behaviour change, not a spacing/placeholder
  fix; noted, not touched.
- DownloadGate.kt / Entitlements.kt untouched (mtimes predate this round).

## V5 changes — Custom Totem Maker (2026-10-22, local — version bump at build time)

The owner's point 14, modelled on skinmc.net/totem: pick a skin, pick a
style, get a totem of undying that IS your character. Entry: a "Totem
Maker" card on Home + a `totem` Nav detail route (bottom nav unchanged at
6 tabs). Kept deliberately simple: one source card, one style selector,
one preview, two outputs.

### Sources
- Default: the current character — username/library skins via their
  texture URL (or the render API's `skin` type for Steve/Alex), uploads
  via their local file; the same resolution the Skin Editor uses.
- In-screen alternatives: upload a different skin PNG (SAF; decoded via
  `SkinBitmaps.decodeSkinBytes`, so 64×32 legacy files convert as
  everywhere else) and fetch-by-username (the normal profile flow →
  texture URL → texture bytes). Fetching does NOT change the app's
  current character — the totem keeps its own source.
- Mob models are out of scope: they never become the current character
  (they exist only as render-detail requests), so the reachable
  "can't make a totem" state is an absent/unresolvable character — the
  screen then shows the mob note and the upload/username options. (The
  Library's MHF "mob characters" are real player profiles with real
  skins, so they work as totem sources, correctly.)

### Preview artwork (local, no API)
- `data/TotemArt.kt` composes the front artwork from the skin bitmap:
  each part's FRONT face (base + overlay composited, the same regions
  as `SkinBitmaps.renderFront`) at the style's proportions,
  nearest-neighbour, transparent background.
- FULL BODY = the classic flat front render (16×32 units).
- MINI ME = a chibi layout (16×26 units): head drawn 12 wide × 13 tall —
  exactly as tall as torso (7) + legs (6) combined.
- The big preview sits on the transparency checkerboard with the
  standard `WatermarkOverlay`; ALL downloads use the clean artwork.

### Output 1 — PNG (FREE)
- Export at 48 px/unit (768 px wide). Routed through
  `DownloadGate.saveWithGate` like every other render download (counts
  toward the free 5/day; Pro/Max unlimited), saved to Pictures/FSD
  Studio, recorded in History as kind "TOTEM".

### Output 2 — Java texture pack (Pro/Max only)
- `data/JavaTotemPack.kt` builds the ZIP: `pack.mcmeta` +
  `assets/minecraft/textures/item/totem_skin.png` (the 64×64 skin,
  base+overlay as-is) + `assets/minecraft/models/item/totem_of_undying.json`.
- The model is a real 3D model: one box element per part (head, torso,
  both arms, both legs) plus a +0.5-inflated overlay twin per part
  (hat/jacket/sleeves/pants), `"texture_size": [64, 64]`, `textures`
  `0` + `particle` → `minecraft:item/totem_skin`.
- UV approach: face rects come from `SkinTemplate.faceRects` (the
  canonical layout shared with the editor and flat renders), mapped
  front→north, back→south, part-right→east, part-left→west, top→up,
  bottom→down; UV arrays are the rect's pixel corners. Verified by a
  Python mirror against the known layout (head front (8,8,8,8), hat
  front (40,8), left-arm overlay front (52,52,4,12), …).
- Geometry: FULL BODY stands 32 tall on y=0, centred x=8/z=8 (torso
  z 6..10, arms at its sides, head x 4..12). MINI ME: legs 6, torso 7,
  head 13 tall × 12 wide/deep (x 2..14, z 2..14), total 26.
- One reconciliation: the full-body head overlay would peak at
  y = 32.5, past the item-model hard bound of 32 — inflated corners
  are clamped into [-16, 32], which touches only that crown plane.
- Display transforms: the renderer centres the 16-unit model space
  before applying them; GUI uses rotation [0, 180, 0] (brings the north
  face — the character's front — toward the viewer), scale 0.45 (full)
  / 0.55 (mini), and a small negative Y translation re-centring the
  scaled figure; ground/hand slots share the scale with a +2 lift.
- Version → pack_format: 1.20.1 → 15, 1.20.4 → 22, 1.21.1 → 34,
  1.21.4 → 46 (selector in the UI, default 1.20.1).
- Saved via the new `data/FileSaver.kt` (MediaStore Downloads →
  Downloads/FSD Studio, `.zip`; app-dir fallback pre-Q).

### Output 3 — Bedrock .mcpack (Pro/Max, "Beta" in the UI)
- **Decision: SHIPPED as Beta.** The standard format could be produced
  confidently: `data/BedrockTotemPack.kt` emits `manifest.json`
  (format_version 2, one "resources" module, fresh UUIDs per pack),
  `textures/entity/totem_skin.png`, `models/entity/totem_skin.geo.json`
  (format_version "1.12.0", one "totem" bone whose cubes mirror the
  Java boxes — Bedrock centres x/z on 0, so origins shift by (−8,0,−8);
  same +0.5 overlays, same 64×64 UV layout with per-face uv/uv_size)
  and `attachables/totem_of_undying.json` (format_version "1.10.0",
  identifier `minecraft:totem_of_undying`, entity_alphatest material,
  `controller.render.item_default`). It is labelled Beta because the
  in-hand placement cannot be device-verified from here.
- Every JSON in both packs is built with org.json and parsed back
  before zipping — validity is structural, not hopeful.

### The "3D model" style decision
- **SKIPPED, per the owner's drop rule.** The reference site's third
  style is a true extruded 3D model; the app has no honest way to
  generate one locally, so the style selector offers exactly the two
  styles that work (Mini me / Full body). Note the Java/Bedrock packs
  themselves DO contain a genuine 3D box model (above) — what was
  dropped is only the third 2D-preview style.

### Gating audit
- Plan checks exist ONLY in `TotemViewModel.downloadJavaPack` /
  `downloadBedrockPack` (FREE → Locked → Plans; also enforced in the
  VM, not just the button). The PNG path uses DownloadGate exactly as
  specified. Nothing else in the feature consults the plan.
- History: pack downloads record kind "TOTEM" with the PREVIEW artwork
  as the entry image (History entries are image-backed; the entry shows
  what the pack looks like — the pack file itself is in Downloads).

### Files
- Created: `data/TotemModel.kt` (styles/editions/versions, part boxes,
  face mapping, ZIP writer), `data/TotemArt.kt`, `data/JavaTotemPack.kt`,
  `data/BedrockTotemPack.kt`, `data/FileSaver.kt`,
  `viewmodel/TotemViewModel.kt`, `ui/TotemMakerScreen.kt`.
- Modified: `ui/Nav.kt` (Routes.TOTEM + wiring), `MainActivity.kt`
  (totemVm), `ui/HomeScreen.kt` (Totem Maker card).
- Strings: **25 new keys × 6 locales → 359 keys per file**, identical
  order (home_totem_*, totem_*), all XML parses, zero missing
  `R.string.*`. versionCode / versionName NOT bumped (parent does it
  at build time).
- Caveat kept from the rest of the app: proportions/UVs use the classic
  (non-slim) arm mapping, like the flat renders and the Skin Editor.
