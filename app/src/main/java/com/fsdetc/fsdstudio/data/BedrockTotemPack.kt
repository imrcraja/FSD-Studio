package com.fsdetc.fsdstudio.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Builds the Bedrock Edition `.mcpack` equivalent of the Java pack
 * (marked "Beta" in the UI — the format below is the standard one, but the
 * in-hand placement cannot be device-verified from here):
 *
 * - `manifest.json` — format_version 2, a single "resources" module.
 * - `textures/entity/totem_skin.png` — the 64×64 skin, as-is.
 * - `models/entity/totem_skin.geo.json` — format_version "1.12.0"; one
 *   bone whose cubes mirror the Java model's boxes (same parts, same
 *   +0.5 overlay inflation, same 64×64 UV layout). Bedrock entity space
 *   centres x/z on 0 instead of 8, so cube origins shift by (−8, 0, −8).
 * - `attachables/totem_of_undying.json` — format_version "1.10.0"; binds
 *   the vanilla totem item to that geometry + texture.
 *
 * All JSON is built with org.json and parsed back before zipping.
 */
object BedrockTotemPack {

    private const val OVERLAY_INFLATE = 0.5
    const val GEOMETRY_ID = "geometry.fsd_totem"

    fun build(skinPng: ByteArray, displayName: String, style: TotemStyle): ByteArray {
        val manifest = JSONObject()
            .put("format_version", 2)
            .put(
                "header",
                JSONObject()
                    .put("name", "FSD Studio Totem — $displayName")
                    .put("description", "Custom totem of undying skin made with FSD Studio")
                    .put("uuid", UUID.randomUUID().toString())
                    .put("version", JSONArray().put(1).put(0).put(0))
                    .put("min_engine_version", JSONArray().put(1).put(16).put(0)),
            )
            .put(
                "modules",
                JSONArray().put(
                    JSONObject()
                        .put("type", "resources")
                        .put("uuid", UUID.randomUUID().toString())
                        .put("version", JSONArray().put(1).put(0).put(0)),
                ),
            )
        val geometry = buildGeometry(style)
        val attachable = JSONObject()
            .put("format_version", "1.10.0")
            .put(
                "minecraft:attachable",
                JSONObject().put(
                    "description",
                    JSONObject()
                        .put("identifier", "minecraft:totem_of_undying")
                        .put("materials", JSONObject().put("default", "entity_alphatest"))
                        .put("textures", JSONObject().put("default", "textures/entity/totem_skin"))
                        .put("geometry", JSONObject().put("default", GEOMETRY_ID))
                        .put(
                            "render_controllers",
                            JSONArray().put("controller.render.item_default"),
                        ),
                ),
            )
        // Parse-back validation (structural — see class doc).
        JSONObject(manifest.toString())
        JSONObject(geometry.toString())
        JSONObject(attachable.toString())
        return ZipFiles.write(
            listOf(
                "manifest.json" to manifest.toString().toByteArray(Charsets.UTF_8),
                "textures/entity/totem_skin.png" to skinPng,
                "models/entity/totem_skin.geo.json" to
                    geometry.toString().toByteArray(Charsets.UTF_8),
                "attachables/totem_of_undying.json" to
                    attachable.toString().toByteArray(Charsets.UTF_8),
            ),
        )
    }

    private fun buildGeometry(style: TotemStyle): JSONObject {
        val cubes = JSONArray()
        for (box in TotemModel.partBoxes(style)) {
            cubes.put(cube(box, SkinLayer.BASE, 0.0))
            cubes.put(cube(box, SkinLayer.OVERLAY, OVERLAY_INFLATE))
        }
        val full = style == TotemStyle.FULL
        val description = JSONObject()
            .put("identifier", GEOMETRY_ID)
            .put("texture_width", 64)
            .put("texture_height", 64)
            .put("visible_bounds_width", 3)
            .put("visible_bounds_height", if (full) 3 else 2.5)
            .put(
                "visible_bounds_offset",
                numArr(0.0, if (full) 1.0 else 0.8, 0.0),
            )
        val bone = JSONObject()
            .put("name", "totem")
            .put("pivot", numArr(0.0, 0.0, 0.0))
            .put("cubes", cubes)
        return JSONObject()
            .put("format_version", "1.12.0")
            .put(
                "minecraft:geometry",
                JSONArray().put(
                    JSONObject()
                        .put("description", description)
                        .put("bones", JSONArray().put(bone)),
                ),
            )
    }

    private fun cube(box: TotemPartBox, layer: SkinLayer, inflate: Double): JSONObject {
        val uv = JSONObject()
        for ((name, rect) in TotemModel.faceRects(box.part, layer)) {
            uv.put(
                name,
                JSONObject()
                    .put("uv", JSONArray().put(rect.x).put(rect.y))
                    .put(
                        "uv_size",
                        JSONArray().put(rect.w).put(rect.h),
                    ),
            )
        }
        return JSONObject()
            // Bedrock entity space: x/z centred on 0 (Java model space uses 8).
            .put("origin", numArr(box.x - inflate - 8.0, box.y - inflate, box.z - inflate - 8.0))
            .put(
                "size",
                numArr(box.w + 2 * inflate, box.h + 2 * inflate, box.d + 2 * inflate),
            )
            .put("uv", uv)
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
