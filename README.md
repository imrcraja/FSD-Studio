# FSD Studio 🎨

**Minecraft Skin Studio for Android** — by **FSD ETC** (Freedom • Success • Dream)

Search a Minecraft Java username or import your own skin, make pose renders and AI artwork, edit skins, create totems, and save or share your results.

## Features

- **Skin search** — look up Minecraft Java usernames and load their skin renders
- **Skin upload** — import a 64×64 PNG skin for local previews
- **Pose renders** — all poses available in this build are unlocked
- **Studio controls** — adjust camera, position, joints, cape, layers, shadow and background
- **AI edits** — no daily app-side AI cap in this build; external service availability and rate limits may still apply
- **Backgrounds** — transparent, dark, purple and white
- **Exports** — render size up to 1024 px; downloads have no daily app-side cap
- **Skin Editor and Totem Maker** — edit character skins and create custom totem artwork
- **History** — reopen and re-download saved renders
- **First-launch message** — “Unlocked by RC RAJA GAMER 2.0” with a YouTube subscribe link

## Performance and caching

- Coil memory cache and 50 MB image disk cache
- OkHttp 20 MB HTTP cache for repeat network requests
- Minecraft profile details cached in memory and DataStore for 24 hours
- Studio preview parameters are debounced to avoid a network request on every slider movement
- Image loading and network work use background I/O where applicable

The app cannot guarantee that remote render or AI services are always online or free from provider-side throttling.

## How it works

- Player profiles and pose renders: [skinrender.dev](https://skinrender.dev)
- AI images: [Pollinations](https://pollinations.ai)

## Tech

Kotlin · Jetpack Compose (Material 3) · Coil · OkHttp · Room · DataStore · GitHub Actions CI builds

## Credits and licenses

- App code: MIT License (see `LICENSE`) — © FSD ETC
- AndroidX / Compose / Coil / OkHttp / Room / Kotlin coroutines: Apache License 2.0 (their respective owners)
- skinrender.dev and Pollinations: used subject to their public terms
- Minecraft skins and textures belong to their respective owners / Mojang

> **Unofficial fan-made tool. Not affiliated with Mojang AB or Microsoft.** Minecraft is a trademark of Mojang Synergies AB.
