package com.fsdetc.fsdstudio.data

import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * The totem's 2D front artwork, composed locally from the skin bitmap:
 * each body part's FRONT face (base + overlay composited, the same regions
 * [SkinBitmaps.renderFront] uses) drawn nearest-neighbour at the style's
 * proportions on a transparent background.
 *
 * - FULL BODY reuses the classic flat front render (16×32 units).
 * - MINI ME uses a chibi layout (16×26 units): the head is drawn 12 wide ×
 *   13 tall — as tall as the torso (7) and legs (6) combined — over a
 *   shortened body, so it genuinely reads as a mini figure.
 *
 * The returned bitmap is the clean export artwork; the on-screen watermark
 * is a Compose overlay only and never touches these pixels.
 */
object TotemArt {

    /** Render the artwork at [scale] pixels per skin unit. */
    fun render(skin: Bitmap, style: TotemStyle, scale: Int): Bitmap = when (style) {
        TotemStyle.FULL -> SkinBitmaps.renderFrontScaled(skin, scale)
        TotemStyle.MINI -> renderMiniMe(skin, scale)
    }

    private fun renderMiniMe(skin: Bitmap, scale: Int): Bitmap {
        val out = Bitmap.createBitmap(16 * scale, 26 * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // Head: 12×13 units centred (x 2..14) — as tall as torso + legs.
        place(canvas, partFront(skin, 8, 8, 40, 8, 8, 8), 2, 0, 12, 13, scale)
        // Torso: 8×7.
        place(canvas, partFront(skin, 20, 20, 20, 36, 8, 12), 4, 13, 8, 7, scale)
        // Arms: 4×7 at the torso's sides (character's right on viewer's left,
        // matching the flat front render).
        place(canvas, partFront(skin, 44, 20, 44, 36, 4, 12), 0, 13, 4, 7, scale)
        place(canvas, partFront(skin, 36, 52, 52, 52, 4, 12), 12, 13, 4, 7, scale)
        // Legs: 4×6.
        place(canvas, partFront(skin, 4, 20, 4, 36, 4, 12), 8, 20, 4, 6, scale)
        place(canvas, partFront(skin, 20, 52, 4, 52, 4, 12), 4, 20, 4, 6, scale)
        return out
    }

    /** One part's front face: the base region with its overlay drawn on top. */
    private fun partFront(skin: Bitmap, bx: Int, by: Int, ox: Int, oy: Int, w: Int, h: Int): Bitmap {
        val base = Bitmap.createBitmap(skin, bx, by, w, h)
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val overlay = Bitmap.createBitmap(skin, ox, oy, w, h)
        Canvas(out).drawBitmap(overlay, 0f, 0f, null)
        return out
    }

    /** Draw [partBmp] scaled (nearest-neighbour) into the unit rect (ux, uy, uw, uh). */
    private fun place(
        canvas: Canvas,
        partBmp: Bitmap,
        ux: Int,
        uy: Int,
        uw: Int,
        uh: Int,
        scale: Int,
    ) {
        val scaled = Bitmap.createScaledBitmap(partBmp, uw * scale, uh * scale, false)
        canvas.drawBitmap(scaled, (ux * scale).toFloat(), (uy * scale).toFloat(), null)
    }
}
