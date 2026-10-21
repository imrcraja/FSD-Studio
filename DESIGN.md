# FSD Studio — Design & Architecture (V1)

## Product shape
Native Android app (Kotlin, single module, single Activity). Default UI language: **English**; strings live in `values/strings.xml` so `values-hi` / `values-bn` packs can be added later without code changes. Dark FSD-purple Material 3 theme.

## Layers
- **UI** (`ui/`): Jetpack Compose screens + bottom navigation — Home, Poses, AI, History, Settings (+ RenderDetail and Pro as destinations).
- **ViewModels** (`viewmodel/`): screen state, one-shot events (toasts/navigation), all coroutines scoped here.
- **Data** (`data/`): `Api.kt` (endpoints + URL builders), `SkinRepository` (profile search), `CharacterStore` (current character, DataStore-backed), `SkinBitmaps` (local flat renders from an uploaded skin bitmap), `HistoryRepository` + Room (`data/db/`) for render history, `ImageSaver` (MediaStore gallery saves + FileProvider sharing), `Prefs` (DataStore), `Entitlements` (single source of truth for Pro status).

## Core flows
1. **Search**: Home → submit-only search → `GET skinrender.dev/api/profile/<name>` → character persisted (uuid, name, skinHash, model) → body render shown via Coil.
2. **Upload**: SAF picker → validate PNG 64×64/64×32 → copy to `filesDir/skins/<sha256>.png` → flat renders (front/back/head/face) composed locally by `SkinBitmaps` (nearest-neighbour crops of the standard layout). Uploaded skins have no public URL, so pose renders & AI reference images apply to username characters; uploads use flat renders + prompt-only AI.
3. **Poses**: grid of 18 poses; each cell = `GET skinrender.dev/render/<uuid>/body?pose=<p>&size=384`. Free = first 10 poses; remaining 8 carry a Pro lock → Pro screen.
4. **RenderDetail**: size (512 free / 1024 Pro), background (transparent/dark/purple/white), Download → MediaStore `Pictures/FSD Studio`, Share via FileProvider; every viewed/downloaded render is recorded in History (file in `filesDir/history/`).
5. **AI**: prompt → daily-limit check (DataStore counter, free 3/day, Pro 50/day, resets at local midnight) → Pollinations GET with the character's public body-render URL as `image=` reference when available → result auto-saved to History. One in-flight request max; failures surface a manual Retry (no retry loops).

## API discipline (owner rule)
- OkHttp disk cache (20 MB) + Coil memory/disk caches — render URLs are immutable, so repeats cost **zero** network requests.
- Profile JSON cached 24 h per username (lowercase key).
- Search fires only on an explicit tap (never per keystroke); timeouts 10 s connect / 20 s read; at most one automatic retry (GET renders) after a 2 s backoff on 429.
- AI calls are gated by the local daily counter *before* any request is made.

## Premium structure (no fake purchases)
`Entitlements.isPro` (DataStore, default `false`) is the only switch; locked UI (poses 11–18, 1024 size, AI limit) reads it. Billing/bKash unlock plugs in here at launch; ads get reserved slots at launch (needs the owner's AdMob account).

## Build & release
GitHub Actions (`.github/workflows/build.yml`): Temurin 17 → `./gradlew assembleDebug` → artifact `FSD-Studio-V1-debug`. Release signing & Play Store come at launch (Play Console $25 account is the known gate; APK also distributed via Discord/website meanwhile).
