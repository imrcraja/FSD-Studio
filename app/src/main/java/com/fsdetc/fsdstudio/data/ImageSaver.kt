package com.fsdetc.fsdstudio.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Saving renders to the gallery and sharing them — scoped-storage safe. */
object ImageSaver {

    /** Save [file] as a PNG into Pictures/FSD Studio. Returns true on success. */
    suspend fun saveToGallery(context: Context, file: File, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStore(context, file, displayName)
                } else {
                    saveViaAppDir(context, file, displayName)
                }
            }.getOrDefault(false)
        }

    private fun saveViaMediaStore(context: Context, file: File, displayName: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/FSD Studio",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val written = resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
            true
        } ?: false
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return written
    }

    /** API 24–28 fallback: app-specific pictures dir + media scan (no permission needed). */
    private fun saveViaAppDir(context: Context, file: File, displayName: String): Boolean {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FSD Studio")
            .apply { mkdirs() }
        val dest = File(dir, displayName)
        file.copyTo(dest, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("image/png"), null)
        return true
    }

    /** Fire a share chooser for an image file inside filesDir/history, filesDir/flats or cacheDir/shared. */
    fun shareImage(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share render")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun folderSizeBytes(dir: File?): Long =
        if (dir == null || !dir.exists()) 0L
        else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
