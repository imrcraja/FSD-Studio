package com.fsdetc.fsdstudio.data

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Totem artwork proportions: chibi "Mini me" or normal "Full body". */
enum class TotemStyle { MINI, FULL }

/** Which pack the Totem Maker builds. Bedrock ships as an in-UI "Beta". */
enum class TotemEdition { JAVA, BEDROCK }

/** Java Edition versions the pack can target, with their pack_format values. */
enum class JavaVersion(val label: String, val packFormat: Int) {
    V1_20_1("1.20.1", 15),
    V1_20_4("1.20.4", 22),
    V1_21_1("1.21.1", 34),
    V1_21_4("1.21.4", 46),
}

/**
 * One body part's box in item-model units: [x], [y], [z] is the min corner,
 * [w] × [h] × [d] the size. Boxes stand on y=0 and are centred on x=8 / z=8
 * (the torso's 4-deep body sits at z 6..10; the head is a full cube).
 */
data class TotemPartBox(
    val part: SkinPart,
    val x: Double,
    val y: Double,
    val z: Double,
    val w: Double,
    val h: Double,
    val d: Double,
)

/**
 * The totem's 3D proportions and the canonical skin-UV face mapping shared by
 * the Java model builder and the Bedrock geometry builder.
 *
 * Face mapping (spec): the skin's FRONT face goes to the model's "north"
 * face, BACK to "south", the part's right side to "east", its left side to
 * "west", top to "up", bottom to "down". The rectangles themselves come from
 * [SkinTemplate.faceRects] — the same layout the editor and the flat renders
 * use — so UVs can never drift from the rest of the app.
 */
object TotemModel {

    /** Base (un-inflated) part boxes per style. */
    fun partBoxes(style: TotemStyle): List<TotemPartBox> = when (style) {
        // Full body: 32 tall — legs 12 + torso 12 + head 8.
        TotemStyle.FULL -> listOf(
            TotemPartBox(SkinPart.HEAD, 4.0, 24.0, 4.0, 8.0, 8.0, 8.0),
            TotemPartBox(SkinPart.TORSO, 4.0, 12.0, 6.0, 8.0, 12.0, 4.0),
            TotemPartBox(SkinPart.ARM_RIGHT, 12.0, 12.0, 6.0, 4.0, 12.0, 4.0),
            TotemPartBox(SkinPart.ARM_LEFT, 0.0, 12.0, 6.0, 4.0, 12.0, 4.0),
            TotemPartBox(SkinPart.LEG_RIGHT, 8.0, 0.0, 6.0, 4.0, 12.0, 4.0),
            TotemPartBox(SkinPart.LEG_LEFT, 4.0, 0.0, 6.0, 4.0, 12.0, 4.0),
        )
        // Mini me: 26 tall — legs 6 + torso 7 + a 13-tall, 12-wide/deep head.
        TotemStyle.MINI -> listOf(
            TotemPartBox(SkinPart.HEAD, 2.0, 13.0, 2.0, 12.0, 13.0, 12.0),
            TotemPartBox(SkinPart.TORSO, 4.0, 6.0, 6.0, 8.0, 7.0, 4.0),
            TotemPartBox(SkinPart.ARM_RIGHT, 12.0, 6.0, 6.0, 4.0, 7.0, 4.0),
            TotemPartBox(SkinPart.ARM_LEFT, 0.0, 6.0, 6.0, 4.0, 7.0, 4.0),
            TotemPartBox(SkinPart.LEG_RIGHT, 8.0, 0.0, 6.0, 4.0, 6.0, 4.0),
            TotemPartBox(SkinPart.LEG_LEFT, 4.0, 0.0, 6.0, 4.0, 6.0, 4.0),
        )
    }

    /** Total standing height in model units (32 full body, 26 mini me). */
    fun totalHeight(style: TotemStyle): Double =
        if (style == TotemStyle.FULL) 32.0 else 26.0

    /** Model face name → texture rectangle for one part on one layer. */
    fun faceRects(part: SkinPart, layer: SkinLayer): Map<String, PxRect> {
        val r = SkinTemplate.faceRects(part, layer)
        return linkedMapOf(
            "up" to r[SkinTemplate.FACE_TOP],
            "down" to r[SkinTemplate.FACE_BOTTOM],
            "east" to r[SkinTemplate.FACE_RIGHT],
            "north" to r[SkinTemplate.FACE_FRONT],
            "west" to r[SkinTemplate.FACE_LEFT],
            "south" to r[SkinTemplate.FACE_BACK],
        )
    }
}

/** Minimal in-memory ZIP writer for the generated packs. */
object ZipFiles {

    fun write(entries: List<Pair<String, ByteArray>>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
