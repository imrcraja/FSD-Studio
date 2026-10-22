package com.fsdetc.fsdstudio.data

import android.content.Context
import android.graphics.BitmapFactory
import com.fsdetc.fsdstudio.data.db.HistoryDao
import com.fsdetc.fsdstudio.data.db.HistoryItem
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.security.MessageDigest

/**
 * Owns the on-disk history files (filesDir/history) and the Room rows that point at
 * them. Re-recording the same render (same uniqueKey) refreshes its timestamp
 * instead of piling up duplicates.
 */
class HistoryRepository(context: Context, private val dao: HistoryDao) {

    private val historyDir: File = File(context.filesDir, "history").apply { mkdirs() }

    val items: Flow<List<HistoryItem>> = dao.observeAll()

    suspend fun recordBytes(
        bytes: ByteArray,
        kind: String,
        title: String,
        subtitle: String,
        sourceName: String,
        uniqueKey: String,
    ): HistoryItem {
        val file = File(historyDir, "h_" + sha256Hex(uniqueKey).take(24) + ".png")
        file.writeBytes(bytes)
        val (w, h) = pngSize(bytes)
        val now = System.currentTimeMillis()
        val existing = dao.findByKey(uniqueKey)
        return if (existing != null) {
            val updated = existing.copy(
                kind = kind,
                title = title,
                subtitle = subtitle,
                sourceName = sourceName,
                filePath = file.absolutePath,
                createdAt = now,
                width = w,
                height = h,
            )
            dao.update(updated)
            updated
        } else {
            val item = HistoryItem(
                kind = kind,
                title = title,
                subtitle = subtitle,
                filePath = file.absolutePath,
                sourceName = sourceName,
                uniqueKey = uniqueKey,
                createdAt = now,
                width = w,
                height = h,
            )
            item.copy(id = dao.insert(item))
        }
    }

    /** Copy an existing image file (a locally built flat render) into history. */
    suspend fun recordCopy(
        source: File,
        kind: String,
        title: String,
        subtitle: String,
        sourceName: String,
        uniqueKey: String,
    ): HistoryItem = recordBytes(source.readBytes(), kind, title, subtitle, sourceName, uniqueKey)

    suspend fun delete(item: HistoryItem) {
        runCatching { File(item.filePath).delete() }
        dao.delete(item)
    }

    suspend fun deleteById(id: Long) {
        dao.findById(id)?.let { delete(it) }
    }

    /** Remove every history file and row (used by Settings → Reset app data). */
    suspend fun clearAll() {
        runCatching {
            historyDir.listFiles()?.forEach { it.delete() }
        }
        dao.clearAll()
    }

    private fun pngSize(bytes: ByteArray): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return (opts.outWidth.coerceAtLeast(0)) to (opts.outHeight.coerceAtLeast(0))
    }

    companion object {
        fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
