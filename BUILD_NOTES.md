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
