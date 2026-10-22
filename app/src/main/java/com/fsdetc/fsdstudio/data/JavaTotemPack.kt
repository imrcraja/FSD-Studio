package com.fsdetc.fsdstudio.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the Java Edition resource pack that turns the totem of undying
 * into the player's own character:
 *
 * - `pack.mcmeta` — the chosen version's pack_format.
 * - `assets/minecraft/textures/item/totem_skin.png` — the 64×64 skin, as-is.
 * - `assets/minecraft/models/item/totem_of_undying.json` — a real 3D model:
 *   one box element per body part (head, torso, both arms, both legs) plus a
 *   +0.5-inflated twin element per part for the overlay layer (hat, jacket,
 *   sleeves, pants), UV-mapped onto the skin with the standard 64×64 layout
 *   via [TotemModel.faceRects]. `"texture_size": [64, 64]` makes the
 *   pixel-space UVs address the whole texture.
 *
 * Everything is assembled with org.json and parsed back before zipping, so
 * the emitted files are structurally valid JSON by construction.
 */
object JavaTotemPack {

    private const val OVERLAY_INFLATE = 0.5

    /** Item-model coordinates must stay within [-16, 32] or the game rejects the model. */
    private const val COORD_MIN = -16.0
    private const val COORD_MAX = 32.0

    fun build(
        skinPng: ByteArray,
        displayName: String,
        style: TotemStyle,
        version: JavaVersion,
    ): ByteArray {
        val mcmeta = JSONObject().put(
            "pack",
            JSONObject()
                .put("pack_format", version.packFormat)
                .put("description", "FSD Studio Totem — $displayName"),
        )
        val model = buildModel(style)
        // Parse-back validation (structural — see class doc).
        JSONObject(mcmeta.toString())
        JSONObject(model.toString())
        return ZipFiles.write(
            listOf(
                "pack.mcmeta" to mcmeta.toString().toByteArray(Charsets.UTF_8),
                "assets/minecraft/textures/item/totem_skin.png" to skinPng,
                "assets/minecraft/models/item/totem_of_undying.json" to
                    model.toString().toByteArray(Charsets.UTF_8),
            ),
        )
    }

    private fun buildModel(style: TotemStyle): JSONObject {
        val elements = JSONArray()
        for (box in TotemModel.partBoxes(style)) {
            elements.put(element(box, SkinLayer.BASE, 0.0))
            elements.put(element(box, SkinLayer.OVERLAY, OVERLAY_INFLATE))
        }
        return JSONObject()
            .put("texture_size", JSONArray().put(64).put(64))
            .put(
                "textures",
                JSONObject()
                    .put("0", "minecraft:item/totem_skin")
                    .put("particle", "minecraft:item/totem_skin"),
            )
            .put("elements", elements)
            .put("display", display(style))
    }

    private fun element(box: TotemPartBox, layer: SkinLayer, inflate: Double): JSONObject {
        // The full-body head overlay would peak at y=32.5 — half a unit past
        // the model's hard 32 bound — so inflated corners are clamped into
        // [-16, 32]; only that one crown plane is affected.
        val fx = (box.x - inflate).coerceAtLeast(COORD_MIN)
        val fy = (box.y - inflate).coerceAtLeast(COORD_MIN)
        val fz = (box.z - inflate).coerceAtLeast(COORD_MIN)
        val tx = (box.x + box.w + inflate).coerceAtMost(COORD_MAX)
        val ty = (box.y + box.h + inflate).coerceAtMost(COORD_MAX)
        val tz = (box.z + box.d + inflate).coerceAtMost(COORD_MAX)
        val faces = JSONObject()
        for ((name, rect) in TotemModel.faceRects(box.part, layer)) {
            faces.put(
                name,
                JSONObject()
                    .put(
                        "uv",
                        JSONArray()
                            .put(rect.x)
                            .put(rect.y)
                            .put(rect.x + rect.w)
                            .put(rect.y + rect.h),
                    )
                    .put("texture", "#0"),
            )
        }
        return JSONObject()
            .put("from", numArr(fx, fy, fz))
            .put("to", numArr(tx, ty, tz))
            .put("faces", faces)
    }

    /**
     * Display transforms. The renderer centres the 16-unit model space on
     * the origin before applying these, so the translations below re-centre
     * the standing figure after scaling (and, in the GUI, after the 180°
     * turn that brings the model's north face — the character's front —
     * toward the viewer). Scale ≈ 0.45 (full body) / 0.55 (mini me) fits
     * the 32 / 26-unit figure into the 16-unit slot.
     */
    private fun display(style: TotemStyle): JSONObject {
        val scale = if (style == TotemStyle.FULL) 0.45 else 0.55
        val guiLift = if (style == TotemStyle.FULL) -3.6 else -2.75
        fun transform(rotation: DoubleArray?, translation: DoubleArray): JSONObject {
            val o = JSONObject()
            if (rotation != null) o.put("rotation", numArr(*rotation))
            o.put("translation", numArr(*translation))
            o.put("scale", numArr(scale, scale, scale))
            return o
        }
        val hand = transform(null, doubleArrayOf(0.0, 2.0, 0.0))
        return JSONObject()
            .put("gui", transform(doubleArrayOf(0.0, 180.0, 0.0), doubleArrayOf(0.0, guiLift, 0.0)))
            .put("ground", hand)
            .put("thirdperson_righthand", hand)
            .put("thirdperson_lefthand", hand)
            .put("firstperson_righthand", hand)
            .put("firstperson_lefthand", hand)
    }

    /** JSON number array that keeps whole values as ints (8, not 8.0). */
    private fun numArr(vararg values: Double): JSONArray {
        val a = JSONArray()
        for (v in values) {
            if (v == v.toInt().toDouble()) a.put(v.toInt()) else a.put(v)
        }
        return a
    }
}
