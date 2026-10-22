package com.fsdetc.fsdstudio.data

import androidx.annotation.StringRes
import com.fsdetc.fsdstudio.R

/**
 * One entry in the player pose library. A pose is no longer just an API pose
 * name: it is a full render recipe — a render [renderType] ("body" or "bust")
 * plus the exact documented skinrender.dev [params] (pose name, camera,
 * animation frame, joint overrides, shadow toggle).
 *
 * [free] marks the poses every plan may use; the rest unlock with Pro/Max
 * (see [Entitlements.poseUnlocked]).
 */
data class PoseDef(
    val id: String,
    @StringRes val labelRes: Int,
    val renderType: String,
    val params: Map<String, String>,
    val free: Boolean,
)

/**
 * The pose library — the app's main feature. Catalog order IS display order,
 * and the 12 free poses come first so the grid leads with what everyone can
 * use. 30 entries: the original 18 API poses plus 12 camera/joint recipes
 * built from documented parameters (yaw/pitch/headYaw/headPitch/frame/shadow).
 */
object PoseCatalog {

    private fun pose(
        id: String,
        @StringRes labelRes: Int,
        apiPose: String,
        free: Boolean,
        renderType: String = "body",
        vararg extra: Pair<String, String>,
    ): PoseDef = PoseDef(
        id = id,
        labelRes = labelRes,
        renderType = renderType,
        params = mapOf("pose" to apiPose, *extra),
        free = free,
    )

    val POSES: List<PoseDef> = listOf(
        // ---------------------------------------------------------- free (12)
        pose("default", R.string.pose_default, "default", free = true),
        pose("walk", R.string.pose_walk, "walk", free = true),
        pose("wave", R.string.pose_wave, "wave", free = true),
        pose("sit", R.string.pose_sit, "sit", free = true),
        pose("dab", R.string.pose_dab, "dab", free = true),
        pose(
            "hero_closeup", R.string.pose_hero_closeup, "wave",
            free = true, renderType = "bust",
        ),
        pose("look_left", R.string.pose_look_left, "default", free = true, "headYaw" to "40"),
        pose("run", R.string.pose_run, "run", free = true),
        pose("sneak", R.string.pose_sneak, "sneak", free = true),
        pose("cheer", R.string.pose_cheer, "cheer", free = true),
        pose("dance", R.string.pose_dance, "dance", free = true),
        pose("side_profile", R.string.pose_side_profile, "default", free = true, "yaw" to "90"),

        // ---------------------------------------------------------- locked (18)
        pose("point", R.string.pose_point, "point", free = false),
        pose("zombie", R.string.pose_zombie, "zombie", free = false),
        pose("tpose", R.string.pose_tpose, "tpose", free = false),
        pose("think", R.string.pose_think, "think", free = false),
        pose("flex", R.string.pose_flex, "flex", free = false),
        pose("salute", R.string.pose_salute, "salute", free = false),
        pose("punch", R.string.pose_punch, "punch", free = false),
        pose("shrug", R.string.pose_shrug, "shrug", free = false),
        pose("jump", R.string.pose_jump, "jump", free = false),
        pose("back_view", R.string.pose_back_view, "default", free = false, "yaw" to "180"),
        pose("walk_stride", R.string.pose_walk_stride, "walk", free = false, "frame" to "0.5"),
        pose("run_stride", R.string.pose_run_stride, "run", free = false, "frame" to "0.5"),
        pose("dance_freeze", R.string.pose_dance_freeze, "dance", free = false, "frame" to "0.5"),
        pose("look_right", R.string.pose_look_right, "default", free = false, "headYaw" to "-40"),
        pose("look_up", R.string.pose_look_up, "default", free = false, "headPitch" to "-35"),
        pose("hero_angle", R.string.pose_hero_angle, "flex", free = false, "pitch" to "-18"),
        pose("top_view", R.string.pose_top_view, "default", free = false, "pitch" to "55"),
        pose("shadow_stand", R.string.pose_shadow_stand, "default", free = false, "shadow" to "true"),
    )

    fun byId(id: String): PoseDef? = POSES.firstOrNull { it.id == id }
}
