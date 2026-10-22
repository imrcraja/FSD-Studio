package com.fsdetc.fsdstudio.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Saving generated non-image files (the totem packs) into the public
 * Downloads/FSD Studio folder — the file counterpart of [ImageSaver],
 * scoped-storage safe on every supported API level.
 */
object FileSaver {

    /** Save [bytes] as [displayName] into Downloads/FSD Studio. Returns true on success. */
    suspend fun saveToDownloads(
        context: Context,
        bytes: ByteArray,
        displayName: String,
        mimeType: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, bytes, displayName, mimeType)
            } else {
                saveViaAppDir(context, bytes, displayName, mimeType)
            }
        }.getOrDefault(false)
    }

    private fun saveViaMediaStore(
        context: Context,
        bytes: ByteArray,
        displayName: String,
        mimeType: String,
    ): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/FSD Studio",
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val written = resolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            true
        } ?: false
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return written
    }

    /** API 24–28 fallback: app-specific downloads dir + media scan (no permission needed). */
    private fun saveViaAppDir(
        context: Context,
        bytes: ByteArray,
        displayName: String,
        mimeType: String,
    ): Boolean {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "FSD Studio")
            .apply { mkdirs() }
        val dest = File(dir, displayName)
        dest.writeBytes(bytes)
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mimeType), null)
        return true
    }
}
