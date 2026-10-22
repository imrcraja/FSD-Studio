package com.fsdetc.fsdstudio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One saved skin in the editor's wardrobe. */
data class WardrobeEntry(
    val id: String,
    val name: String,
    val createdAt: Long,
    val filePath: String,
)

/**
 * The Skin Editor's wardrobe: finished skins (composited 64×64 PNGs) saved
 * under filesDir/wardrobe with a small JSON index. Loading a wardrobe skin
 * paints it as the editor's paint bitmap — the outfit that produced it is
 * baked into the PNG, so the outfit starts empty on load.
 */
class WardrobeStore(private val context: Context) {

    private val dir: File = File(context.filesDir, "wardrobe").apply { mkdirs() }
    private val indexFile: File = File(dir, "index.json")

    suspend fun list(): List<WardrobeEntry> = withContext(Dispatchers.IO) { readIndex() }

    suspend fun save(name: String, bitmap: Bitmap): WardrobeEntry = withContext(Dispatchers.IO) {
        val id = "w" + System.currentTimeMillis().toString(36) +
            (0..9999).random().toString().padStart(4, '0')
        val file = File(dir, "$id.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val entry = WardrobeEntry(id, name, System.currentTimeMillis(), file.absolutePath)
        writeIndex(readIndex() + entry)
        entry
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val entries = readIndex()
        entries.firstOrNull { it.id == id }?.let { runCatching { File(it.filePath).delete() } }
        writeIndex(entries.filter { it.id != id })
    }

    fun loadBitmap(entry: WardrobeEntry): Bitmap? =
        runCatching { BitmapFactory.decodeFile(entry.filePath) }.getOrNull()

    private fun readIndex(): List<WardrobeEntry> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                WardrobeEntry(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    createdAt = o.optLong("createdAt", 0L),
                    filePath = o.getString("filePath"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeIndex(entries: List<WardrobeEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("createdAt", e.createdAt)
                    .put("filePath", e.filePath),
            )
        }
        indexFile.writeText(arr.toString())
    }
}
