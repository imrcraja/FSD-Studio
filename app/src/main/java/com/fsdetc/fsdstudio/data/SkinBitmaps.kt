package com.fsdetc.fsdstudio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Everything that happens to an uploaded skin file, locally on device:
 * validation, import into filesDir/skins, and flat front/back/face renders built
 * from the bitmap with the standard 64×64 skin layout (base + overlay composited
 * per part, nearest-neighbour upscale — the same mapping the project's Python
 * preview tooling uses).
 */
object SkinBitmaps {

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** File of a generated flat render ("front", "back" or "face") for an uploaded skin. */
    fun flatFile(context: Context, skinHash: String, kind: String): File =
        File(File(context.filesDir, "flats"), "${skinHash}_$kind.png")

    /**
     * Validate + import a picked skin image. Throws [InvalidSkinException] for a
     * non-PNG or wrong-sized file, [IOException] when the file cannot be read.
     */
    suspend fun importSkin(context: Context, uri: Uri): CharacterInfo = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cannot open file")
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw InvalidSkinException()
        if (!(bmp.width == 64 && (bmp.height == 64 || bmp.height == 32))) {
            throw InvalidSkinException()
        }
        val sha = sha256Hex(bytes)
        val skinsDir = File(context.filesDir, "skins").apply { mkdirs() }
        File(context.filesDir, "flats").apply { mkdirs() }
        val skinFile = File(skinsDir, "$sha.png")
        if (!skinFile.exists()) skinFile.writeBytes(bytes)

        val normalized = if (bmp.height == 32) convertLegacy(bmp) else bmp
        val front = flatFile(context, sha, "front")
        val back = flatFile(context, sha, "back")
        val face = flatFile(context, sha, "face")
        if (!front.exists()) writePng(renderFront(normalized), front)
        if (!back.exists()) writePng(renderBack(normalized), back)
        if (!face.exists()) writePng(renderFace(normalized), face)

        CharacterInfo(
            name = "Uploaded skin",
            uuid = null,
            model = null,
            skinHash = sha,
            skinUrl = null,
            localSkinPath = skinFile.absolutePath,
            source = CharacterSource.UPLOAD,
        )
    }

    /**
     * Decode raw skin bytes into a mutable 64×64 bitmap (legacy 64×32 files are
     * converted). Returns null for anything that is not a usable skin image.
     * Used by the Skin Editor to load upload files and downloaded textures.
     */
    fun decodeSkinBytes(bytes: ByteArray): Bitmap? {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        if (!(bmp.width == 64 && (bmp.height == 64 || bmp.height == 32))) return null
        val normalized = if (bmp.height == 32) convertLegacy(bmp) else bmp
        return normalized.copy(Bitmap.Config.ARGB_8888, true)
    }

    /** Decode a stored skin file (see [decodeSkinBytes]). */
    fun decodeSkinFile(file: File): Bitmap? =
        runCatching { decodeSkinBytes(file.readBytes()) }.getOrNull()

    /**
     * Save an edited 64×64 skin as a new uploaded character — the same on-disk
     * treatment [importSkin] gives a picked file (filesDir/skins + flat
     * renders), so Poses / Studio / History treat it exactly like an upload.
     */
    suspend fun importEditedSkin(context: Context, bitmap: Bitmap, displayName: String): CharacterInfo =
        withContext(Dispatchers.IO) {
            val bytes = bitmapToPngBytes(bitmap)
            val sha = sha256Hex(bytes)
            val skinsDir = File(context.filesDir, "skins").apply { mkdirs() }
            File(context.filesDir, "flats").apply { mkdirs() }
            val skinFile = File(skinsDir, "$sha.png")
            if (!skinFile.exists()) skinFile.writeBytes(bytes)
            val normalized = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            val front = flatFile(context, sha, "front")
            val back = flatFile(context, sha, "back")
            val face = flatFile(context, sha, "face")
            if (!front.exists()) writePng(renderFront(normalized), front)
            if (!back.exists()) writePng(renderBack(normalized), back)
            if (!face.exists()) writePng(renderFace(normalized), face)
            CharacterInfo(
                name = displayName,
                uuid = null,
                model = null,
                skinHash = sha,
                skinUrl = null,
                localSkinPath = skinFile.absolutePath,
                source = CharacterSource.UPLOAD,
            )
        }

    // ------------------------------------------------------------------ renders

    /** Flat front view at an arbitrary pixel scale (unit = [scale] px). */
    fun renderFrontScaled(skin: Bitmap, scale: Int): Bitmap {
        val out = Bitmap.createBitmap(16 * scale, 32 * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        place(canvas, part(skin, 8, 8, 40, 8, 8, 8), 4, 0, scale) // head + hat
        place(canvas, part(skin, 20, 20, 20, 36, 8, 12), 4, 8, scale) // torso + jacket
        place(canvas, part(skin, 44, 20, 44, 36, 4, 12), 0, 8, scale) // right arm + sleeve
        place(canvas, part(skin, 36, 52, 52, 52, 4, 12), 12, 8, scale) // left arm + sleeve
        place(canvas, part(skin, 4, 20, 4, 36, 4, 12), 4, 20, scale) // right leg + pants
        place(canvas, part(skin, 20, 52, 4, 52, 4, 12), 8, 20, scale) // left leg + pants
        return out
    }

    /** Flat back view at an arbitrary pixel scale (unit = [scale] px). */
    fun renderBackScaled(skin: Bitmap, scale: Int): Bitmap {
        val out = Bitmap.createBitmap(16 * scale, 32 * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        place(canvas, part(skin, 24, 8, 56, 8, 8, 8), 4, 0, scale) // head back + hat
        place(canvas, part(skin, 32, 20, 32, 36, 8, 12), 4, 8, scale) // torso back + jacket
        place(canvas, part(skin, 52, 20, 52, 36, 4, 12), 12, 8, scale) // right arm back
        place(canvas, part(skin, 44, 52, 60, 52, 4, 12), 0, 8, scale) // left arm back
        place(canvas, part(skin, 12, 20, 12, 36, 4, 12), 8, 20, scale) // right leg back
        place(canvas, part(skin, 28, 52, 12, 52, 4, 12), 4, 20, scale) // left leg back
        return out
    }

    /** Flat front view: 16×32 skin units at 32× scale (512×1024), transparent background. */
    fun renderFront(skin: Bitmap): Bitmap {
        val scale = 32
        val out = Bitmap.createBitmap(16 * scale, 32 * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        place(canvas, part(skin, 8, 8, 40, 8, 8, 8), 4, 0, scale) // head + hat
        place(canvas, part(skin, 20, 20, 20, 36, 8, 12), 4, 8, scale) // torso + jacket
        place(canvas, part(skin, 44, 20, 44, 36, 4, 12), 0, 8, scale) // right arm + sleeve
        place(canvas, part(skin, 36, 52, 52, 52, 4, 12), 12, 8, scale) // left arm + sleeve
        place(canvas, part(skin, 4, 20, 4, 36, 4, 12), 4, 20, scale) // right leg + pants
        place(canvas, part(skin, 20, 52, 4, 52, 4, 12), 8, 20, scale) // left leg + pants
        return out
    }

    /** Flat back view, same canvas as the front. */
    fun renderBack(skin: Bitmap): Bitmap {
        val scale = 32
        val out = Bitmap.createBitmap(16 * scale, 32 * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        place(canvas, part(skin, 24, 8, 56, 8, 8, 8), 4, 0, scale) // head back + hat
        place(canvas, part(skin, 32, 20, 32, 36, 8, 12), 4, 8, scale) // torso back + jacket
        place(canvas, part(skin, 52, 20, 52, 36, 4, 12), 12, 8, scale) // right arm back
        place(canvas, part(skin, 44, 52, 60, 52, 4, 12), 0, 8, scale) // left arm back
        place(canvas, part(skin, 12, 20, 12, 36, 4, 12), 8, 20, scale) // right leg back
        place(canvas, part(skin, 28, 52, 12, 52, 4, 12), 4, 20, scale) // left leg back
        return out
    }

    /** Face (head front + hat layer) at 512×512. */
    fun renderFace(skin: Bitmap): Bitmap {
        val scale = 64
        val out = Bitmap.createBitmap(8 * scale, 8 * scale, Bitmap.Config.ARGB_8888)
        place(Canvas(out), part(skin, 8, 8, 40, 8, 8, 8), 0, 0, scale)
        return out
    }

    /** Draw [source] over a solid colour — used when saving a flat render with a background. */
    fun compositeOver(source: Bitmap, argbColor: Int): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(argbColor)
        canvas.drawBitmap(source, 0f, 0f, null)
        return out
    }

    fun bitmapToPngBytes(bmp: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    // ------------------------------------------------------------------ helpers

    /** One body part: base region with its overlay region drawn on top. */
    private fun part(skin: Bitmap, bx: Int, by: Int, ox: Int, oy: Int, w: Int, h: Int): Bitmap {
        val base = Bitmap.createBitmap(skin, bx, by, w, h)
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val overlay = Bitmap.createBitmap(skin, ox, oy, w, h)
        Canvas(out).drawBitmap(overlay, 0f, 0f, null)
        return out
    }

    private fun place(canvas: Canvas, partBmp: Bitmap, ux: Int, uy: Int, scale: Int) {
        val scaled = Bitmap.createScaledBitmap(partBmp, partBmp.width * scale, partBmp.height * scale, false)
        canvas.drawBitmap(scaled, (ux * scale).toFloat(), (uy * scale).toFloat(), null)
    }

    private fun writePng(bmp: Bitmap, file: File) {
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * Legacy 64×32 → 64×64: copy the texture, then build the left leg/arm blocks by
     * mirroring the right ones (the standard Mojang conversion). Overlay regions for
     * limbs stay transparent — legacy skins have none.
     */
    private fun convertLegacy(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, 0f, 0f, null)
        canvas.drawBitmap(mirrorLimbBlock(src, 0, 16), 16f, 48f, null) // left leg
        canvas.drawBitmap(mirrorLimbBlock(src, 40, 16), 32f, 48f, null) // left arm
        return out
    }

    /** Mirror one 16×16 limb block into its opposite-side twin. */
    private fun mirrorLimbBlock(src: Bitmap, sx: Int, sy: Int): Bitmap {
        val out = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // Top (4,0) and bottom (8,0) caps: each flipped in place.
        canvas.drawBitmap(flipH(Bitmap.createBitmap(src, sx + 4, sy, 4, 4)), 4f, 0f, null)
        canvas.drawBitmap(flipH(Bitmap.createBitmap(src, sx + 8, sy, 4, 4)), 8f, 0f, null)
        // Side faces: [right, front, left, back] — the twin swaps right<->left, every face flipped.
        val mapping = listOf(0 to 8, 4 to 4, 8 to 0, 12 to 12) // destX to srcFaceX
        for ((destX, srcFaceX) in mapping) {
            val face = Bitmap.createBitmap(src, sx + srcFaceX, sy + 4, 4, 12)
            canvas.drawBitmap(flipH(face), destX.toFloat(), 4f, null)
        }
        return out
    }

    private fun flipH(bmp: Bitmap): Bitmap {
        val m = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, false)
    }
}
