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
    /**
     * Stored render-id override for characters whose render URL id is not their
     * UUID/name — the built-in defaults use "steve" / "alex". Null normally.
     */
    val renderIdOverride: String? = null,
) {
    /** Identifier usable in skinrender.dev render URLs; null for uploaded skins. */
    val renderId: String?
        get() = renderIdOverride
            ?: if (source == CharacterSource.USERNAME) (uuid ?: name) else null

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
        /** Render type for the recipe ("body" or "bust"). */
        val renderType: String = "body",
        /** Full recipe parameters (pose name, camera, frame, joints) from PoseCatalog. */
        val extraParams: Map<String, String> = emptyMap(),
    ) : DetailRequest

    data class LocalFile(
        val path: String,
        val title: String,
        val subtitle: String,
        val sourceName: String,
        val historyId: Long? = null,
        val deletableHistory: Boolean = false,
    ) : DetailRequest

    /** A Library mob render (skinrender.dev /render/mob/...), posed via [poses]. */
    data class MobRender(
        val mobId: String,
        val mobLabel: String,
        val poses: List<Api.Pose>,
    ) : DetailRequest
}
