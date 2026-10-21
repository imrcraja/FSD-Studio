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
