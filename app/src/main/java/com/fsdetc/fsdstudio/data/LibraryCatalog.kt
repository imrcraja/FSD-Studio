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
        // V3.1 expansion — every username below verified against the profile
        // API (HTTP 200 + uuid) on 2026-10-22 before being added.
        LibraryPlayer(label = "Technoblade", badge = null, character = null, searchName = "Technoblade"),
        LibraryPlayer(label = "GeorgeNotFound", badge = null, character = null, searchName = "GeorgeNotFound"),
        LibraryPlayer(label = "Sapnap", badge = null, character = null, searchName = "Sapnap"),
        LibraryPlayer(label = "TommyInnit", badge = null, character = null, searchName = "TommyInnit"),
        LibraryPlayer(label = "WilburSoot", badge = null, character = null, searchName = "WilburSoot"),
        LibraryPlayer(label = "Philza", badge = null, character = null, searchName = "Philza"),
        LibraryPlayer(label = "CaptainSparklez", badge = null, character = null, searchName = "CaptainSparklez"),
        LibraryPlayer(label = "DanTDM", badge = null, character = null, searchName = "DanTDM"),
        LibraryPlayer(label = "PewDiePie", badge = null, character = null, searchName = "PewDiePie"),
        LibraryPlayer(label = "jeb_", badge = null, character = null, searchName = "jeb_"),
        LibraryPlayer(label = "Dinnerbone", badge = null, character = null, searchName = "Dinnerbone"),
        LibraryPlayer(label = "Herobrine", badge = null, character = null, searchName = "Herobrine"),
        LibraryPlayer(label = "MHF Steve", badge = null, character = null, searchName = "MHF_Steve"),
        LibraryPlayer(label = "MHF Alex", badge = null, character = null, searchName = "MHF_Alex"),
    )

    /**
     * Mob characters — Mojang's official MHF accounts: real player profiles
     * whose skins ARE the mobs, so they render through the normal character
     * pipeline (username search → character RenderDetail), unlike the true
     * mob models in [MOBS]. Shown as the Library's "Mobs & More" section.
     * Every account was verified against the profile API on 2026-10-22;
     * MHF_MagmaCube returned 404 (no such account) — the Magma Cube account
     * is MHF_LavaSlime.
     */
    val MOB_CHARACTERS: List<LibraryPlayer> = listOf(
        LibraryPlayer(label = "Blaze", badge = null, character = null, searchName = "MHF_Blaze"),
        LibraryPlayer(label = "Ghast", badge = null, character = null, searchName = "MHF_Ghast"),
        LibraryPlayer(label = "Slime", badge = null, character = null, searchName = "MHF_Slime"),
        LibraryPlayer(label = "Cave Spider", badge = null, character = null, searchName = "MHF_CaveSpider"),
        LibraryPlayer(label = "Enderman", badge = null, character = null, searchName = "MHF_Enderman"),
        LibraryPlayer(label = "Iron Golem", badge = null, character = null, searchName = "MHF_Golem"),
        LibraryPlayer(label = "Squid", badge = null, character = null, searchName = "MHF_Squid"),
        LibraryPlayer(label = "Mooshroom", badge = null, character = null, searchName = "MHF_MushroomCow"),
        LibraryPlayer(label = "Ocelot", badge = null, character = null, searchName = "MHF_Ocelot"),
        LibraryPlayer(label = "Zombie Pigman", badge = null, character = null, searchName = "MHF_PigZombie"),
        LibraryPlayer(label = "Wither Skeleton", badge = null, character = null, searchName = "MHF_WSkeleton"),
        LibraryPlayer(label = "Wither", badge = null, character = null, searchName = "MHF_Wither"),
        LibraryPlayer(label = "Witch", badge = null, character = null, searchName = "MHF_Witch"),
        LibraryPlayer(label = "Snow Golem", badge = null, character = null, searchName = "MHF_SnowGolem"),
        LibraryPlayer(label = "Magma Cube", badge = null, character = null, searchName = "MHF_LavaSlime"),
        LibraryPlayer(label = "Chicken", badge = null, character = null, searchName = "MHF_Chicken"),
        LibraryPlayer(label = "Spider", badge = null, character = null, searchName = "MHF_Spider"),
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
