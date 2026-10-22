package com.fsdetc.fsdstudio.data

/**
 * Built-in characters for the Library screen.
 *
 * Players come in two flavours: static characters whose [CharacterInfo] is
 * known up-front (Mr FSD, Steve, Alex — no profile fetch needed) and username
 * entries resolved through the normal search flow (Notch, Dream).
 */
data class LibraryPlayer(
    val label: String,
    /** Small badge on the card, e.g. "Creator pick" (Mr FSD only). */
    val badge: String?,
    /** Set for static entries; tapping saves this as the current character. */
    val character: CharacterInfo?,
    /** Set for username entries; tapping runs the normal profile search. */
    val searchName: String?,
) {
    /** Id for card thumbnails (render URLs accept usernames as ids). */
    val thumbId: String get() = character?.renderId ?: (searchName ?: label)
}

data class MobEntry(
    val id: String,
    val label: String,
    val poses: List<Api.Pose>,
)

object LibraryCatalog {

    val PLAYERS: List<LibraryPlayer> = listOf(
        LibraryPlayer(
            label = "Mr FSD",
            badge = "Creator pick",
            character = CharacterInfo(
                name = "Mr_FSD",
                uuid = "5779c895676840fc94e60fb4e35a81ad",
                model = "classic",
                skinHash = "6e4a1e07a9692f8e59b9e8de82fe14320a9a362cde2abc304f233f8a1e3c0951",
                skinUrl = "https://textures.minecraft.net/texture/" +
                    "6e4a1e07a9692f8e59b9e8de82fe14320a9a362cde2abc304f233f8a1e3c0951",
                localSkinPath = null,
                source = CharacterSource.USERNAME,
            ),
            searchName = null,
        ),
        LibraryPlayer(
            label = "Steve",
            badge = null,
            character = CharacterInfo(
                name = "Steve",
                uuid = null,
                model = "classic",
                skinHash = null,
                skinUrl = null,
                localSkinPath = null,
                source = CharacterSource.USERNAME,
                renderIdOverride = "steve",
            ),
            searchName = null,
        ),
        LibraryPlayer(
            label = "Alex",
            badge = null,
            character = CharacterInfo(
                name = "Alex",
                uuid = null,
                model = "slim",
                skinHash = null,
                skinUrl = null,
                localSkinPath = null,
                source = CharacterSource.USERNAME,
                renderIdOverride = "alex",
            ),
            searchName = null,
        ),
        LibraryPlayer(label = "Notch", badge = null, character = null, searchName = "Notch"),
        LibraryPlayer(label = "Dream", badge = null, character = null, searchName = "Dream"),
    )

    private fun poses(vararg pairs: Pair<String, String>): List<Api.Pose> =
        pairs.map { Api.Pose(it.first, it.second) }

    val MOBS: List<MobEntry> = listOf(
        MobEntry("creeper", "Creeper", poses("default" to "Default", "walk" to "Walk", "hiss" to "Hiss", "look" to "Look")),
        MobEntry("zombie", "Zombie", poses("default" to "Default", "walk" to "Walk", "lunge" to "Lunge", "look" to "Look")),
        MobEntry("skeleton", "Skeleton", poses("default" to "Default", "walk" to "Walk", "aim" to "Aim", "look" to "Look")),
        MobEntry("enderman", "Enderman", poses("default" to "Default", "walk" to "Walk", "scream" to "Scream", "carry" to "Carry")),
        MobEntry("spider", "Spider", poses("default" to "Default", "walk" to "Walk", "rear" to "Rear", "look" to "Look")),
        MobEntry("slime", "Slime", poses("default" to "Default", "walk" to "Walk", "hop" to "Hop", "squash" to "Squash")),
        MobEntry("pig", "Pig", poses("default" to "Default", "walk" to "Walk", "graze" to "Graze", "look" to "Look")),
        MobEntry("cow", "Cow", poses("default" to "Default", "walk" to "Walk", "graze" to "Graze", "look" to "Look")),
        MobEntry("sheep", "Sheep", poses("default" to "Default", "walk" to "Walk", "graze" to "Graze", "look" to "Look")),
        MobEntry("chicken", "Chicken", poses("default" to "Default", "walk" to "Walk", "flap" to "Flap", "look" to "Look")),
        MobEntry("wolf", "Wolf", poses("default" to "Default", "walk" to "Walk", "sit" to "Sit", "look" to "Look")),
        MobEntry("villager", "Villager", poses("default" to "Default", "walk" to "Walk", "trade" to "Trade", "look" to "Look")),
    )
}
