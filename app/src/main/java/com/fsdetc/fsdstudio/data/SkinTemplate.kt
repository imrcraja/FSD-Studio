package com.fsdetc.fsdstudio.data

/** One axis-aligned pixel rectangle on the 64×64 skin template. */
data class PxRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun contains(px: Int, py: Int): Boolean = px in x until x + w && py in y until y + h
}

enum class SkinLayer { BASE, OVERLAY }

enum class SkinPart { HEAD, TORSO, ARM_RIGHT, ARM_LEFT, LEG_RIGHT, LEG_LEFT }

/**
 * The classic (1.8+) 64×64 skin layout as data: every body part is a block with
 * origin (ox, oy) and dimensions (w × h × d); its six faces sit at the standard
 * offsets — top (ox+d, oy), bottom (ox+d+w, oy), right (ox, oy+d),
 * front (ox+d, oy+d), left (ox+d+w, oy+d), back (ox+2d+w, oy+d).
 *
 * This is the same mapping [SkinBitmaps] uses for flat renders and the same one
 * the premade-asset generator used to place its patches, so painting, region
 * masks and asset compositing all agree pixel-for-pixel.
 */
object SkinTemplate {

    const val SIZE = 64

    private data class Block(val ox: Int, val oy: Int, val w: Int, val h: Int, val d: Int)

    private val blocks: Map<Pair<SkinPart, SkinLayer>, Block> = mapOf(
        (SkinPart.HEAD to SkinLayer.BASE) to Block(0, 0, 8, 8, 8),
        (SkinPart.HEAD to SkinLayer.OVERLAY) to Block(32, 0, 8, 8, 8),
        (SkinPart.TORSO to SkinLayer.BASE) to Block(16, 16, 8, 12, 4),
        (SkinPart.TORSO to SkinLayer.OVERLAY) to Block(16, 32, 8, 12, 4),
        (SkinPart.ARM_RIGHT to SkinLayer.BASE) to Block(40, 16, 4, 12, 4),
        (SkinPart.ARM_RIGHT to SkinLayer.OVERLAY) to Block(40, 32, 4, 12, 4),
        (SkinPart.LEG_RIGHT to SkinLayer.BASE) to Block(0, 16, 4, 12, 4),
        (SkinPart.LEG_RIGHT to SkinLayer.OVERLAY) to Block(0, 32, 4, 12, 4),
        (SkinPart.LEG_LEFT to SkinLayer.BASE) to Block(16, 48, 4, 12, 4),
        (SkinPart.LEG_LEFT to SkinLayer.OVERLAY) to Block(0, 48, 4, 12, 4),
        (SkinPart.ARM_LEFT to SkinLayer.BASE) to Block(32, 48, 4, 12, 4),
        (SkinPart.ARM_LEFT to SkinLayer.OVERLAY) to Block(48, 48, 4, 12, 4),
    )

    /** The six face rectangles of one part block, in top/bottom/right/front/left/back order. */
    fun faceRects(part: SkinPart, layer: SkinLayer): List<PxRect> {
        val b = blocks.getValue(part to layer)
        return listOf(
            PxRect(b.ox + b.d, b.oy, b.w, b.d), // top
            PxRect(b.ox + b.d + b.w, b.oy, b.w, b.d), // bottom
            PxRect(b.ox, b.oy + b.d, b.d, b.h), // right
            PxRect(b.ox + b.d, b.oy + b.d, b.w, b.h), // front
            PxRect(b.ox + b.d + b.w, b.oy + b.d, b.d, b.h), // left
            PxRect(b.ox + 2 * b.d + b.w, b.oy + b.d, b.w, b.h), // back
        )
    }

    /** Named faces (same order as [faceRects]) — index 0..5 = top, bottom, right, front, left, back. */
    fun faceRect(part: SkinPart, layer: SkinLayer, face: Int): PxRect = faceRects(part, layer)[face]

    const val FACE_TOP = 0
    const val FACE_BOTTOM = 1
    const val FACE_RIGHT = 2
    const val FACE_FRONT = 3
    const val FACE_LEFT = 4
    const val FACE_BACK = 5

    /** All rectangles belonging to [part] on [layer]. */
    fun rects(part: SkinPart, layer: SkinLayer): List<PxRect> = faceRects(part, layer)

    fun partAt(x: Int, y: Int): SkinPart? {
        for ((key, _) in blocks) {
            if (faceRects(key.first, key.second).any { it.contains(x, y) }) return key.first
        }
        return null
    }

    fun layerAt(x: Int, y: Int): SkinLayer? {
        for ((key, _) in blocks) {
            if (faceRects(key.first, key.second).any { it.contains(x, y) }) return key.second
        }
        return null
    }

    /** True when pixel (x, y) belongs to [layer] — the painting mask for a layer selection. */
    fun inLayer(x: Int, y: Int, layer: SkinLayer): Boolean = layerAt(x, y) == layer

    /** 64×64 mask (row-major) of every pixel on [layer]. */
    fun layerMask(layer: SkinLayer): BooleanArray {
        val mask = BooleanArray(SIZE * SIZE)
        for (part in SkinPart.entries) {
            for (rect in faceRects(part, layer)) {
                for (yy in rect.y until rect.y + rect.h) {
                    for (xx in rect.x until rect.x + rect.w) {
                        mask[yy * SIZE + xx] = true
                    }
                }
            }
        }
        return mask
    }

    val BASE_MASK: BooleanArray by lazy { layerMask(SkinLayer.BASE) }
    val OVERLAY_MASK: BooleanArray by lazy { layerMask(SkinLayer.OVERLAY) }

    fun maskFor(layer: SkinLayer): BooleanArray = if (layer == SkinLayer.BASE) BASE_MASK else OVERLAY_MASK
}
