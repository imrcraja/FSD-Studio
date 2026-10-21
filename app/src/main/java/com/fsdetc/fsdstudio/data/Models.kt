package com.fsdetc.fsdstudio.data

enum class CharacterSource { USERNAME, UPLOAD }

/** The character the user is currently working with. */
data class CharacterInfo(
    val name: String,
    val uuid: String?,
    val model: String?,
    val skinHash: String?,
    val skinUrl: String?,
    /** Absolute path of the copied skin PNG when [source] is [CharacterSource.UPLOAD]. */
    val localSkinPath: String?,
    val source: CharacterSource,
) {
    /** Identifier usable in skinrender.dev render URLs; null for uploaded skins. */
    val renderId: String?
        get() = if (source == CharacterSource.USERNAME) (uuid ?: name) else null

    val uuidShort: String? get() = uuid?.take(8)

    val isSlim: Boolean get() = model?.equals("slim", ignoreCase = true) == true
}

class PlayerNotFoundException : Exception("Player not found")
class InvalidSkinException : Exception("Invalid skin file")

/** What the render-detail screen should show. Passed through [com.fsdetc.fsdstudio.viewmodel.MainViewModel]. */
sealed interface DetailRequest {
    data class RemotePose(
        val renderId: String,
        val poseId: String,
        val poseLabel: String,
        val charName: String,
    ) : DetailRequest

    data class LocalFile(
        val path: String,
        val title: String,
        val subtitle: String,
        val sourceName: String,
        val historyId: Long? = null,
        val deletableHistory: Boolean = false,
    ) : DetailRequest
}
