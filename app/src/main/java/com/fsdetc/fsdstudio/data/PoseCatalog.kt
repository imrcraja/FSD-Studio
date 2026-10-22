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
 * The pose library — the app's main feature. Catalog order IS display order.
 * 60 entries: the V3 set (the original 18 API poses plus 12 camera/joint
 * recipes) followed by the V3.1 expansion block (30 more frame / camera /
 * close-up recipes built from the same documented parameters). The first 12
 * entries are free and lead the grid; the expansion block adds 6 more free
 * recipes at its head. Entitlement is per-entry via [PoseDef.free]
 * (see [Entitlements.poseUnlocked]), never position-based.
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
        pose("look_left", R.string.pose_look_left, "default", free = true, extra = arrayOf("headYaw" to "40")),
        pose("run", R.string.pose_run, "run", free = true),
        pose("sneak", R.string.pose_sneak, "sneak", free = true),
        pose("cheer", R.string.pose_cheer, "cheer", free = true),
        pose("dance", R.string.pose_dance, "dance", free = true),
        pose("side_profile", R.string.pose_side_profile, "default", free = true, extra = arrayOf("yaw" to "90")),

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
        pose("back_view", R.string.pose_back_view, "default", free = false, extra = arrayOf("yaw" to "180")),
        pose("walk_stride", R.string.pose_walk_stride, "walk", free = false, extra = arrayOf("frame" to "0.5")),
        pose("run_stride", R.string.pose_run_stride, "run", free = false, extra = arrayOf("frame" to "0.5")),
        pose("dance_freeze", R.string.pose_dance_freeze, "dance", free = false, extra = arrayOf("frame" to "0.5")),
        pose("look_right", R.string.pose_look_right, "default", free = false, extra = arrayOf("headYaw" to "-40")),
        pose("look_up", R.string.pose_look_up, "default", free = false, extra = arrayOf("headPitch" to "-35")),
        pose("hero_angle", R.string.pose_hero_angle, "flex", free = false, extra = arrayOf("pitch" to "-18")),
        pose("top_view", R.string.pose_top_view, "default", free = false, extra = arrayOf("pitch" to "55")),
        pose("shadow_stand", R.string.pose_shadow_stand, "default", free = false, extra = arrayOf("shadow" to "true")),

        // --------------------------------------- V3.1 expansion (30 more)
        // Free additions (6) — basic frame/camera variants everyone can use.
        pose("walk_quarter", R.string.pose_walk_quarter, "walk", free = true, extra = arrayOf("frame" to "0.25")),
        pose("run_start", R.string.pose_run_start, "run", free = true, extra = arrayOf("frame" to "0.25")),
        pose("look_down", R.string.pose_look_down, "default", free = true, extra = arrayOf("headPitch" to "35")),
        pose("three_quarter", R.string.pose_three_quarter, "default", free = true, extra = arrayOf("yaw" to "-35")),
        pose(
            "sit_relaxed", R.string.pose_sit_relaxed, "sit",
            free = true, extra = arrayOf("headPitch" to "18", "headYaw" to "-15"),
        ),
        pose(
            "greet_bust", R.string.pose_greet_bust, "wave",
            free = true, renderType = "bust", extra = arrayOf("zoom" to "1.3"),
        ),

        // Locked additions (24) — animation frames, close-ups, dramatic angles.
        pose("walk_late", R.string.pose_walk_late, "walk", free = false, extra = arrayOf("frame" to "0.75")),
        pose("run_late", R.string.pose_run_late, "run", free = false, extra = arrayOf("frame" to "0.75")),
        pose("dance_quarter", R.string.pose_dance_quarter, "dance", free = false, extra = arrayOf("frame" to "0.25")),
        pose("dance_late", R.string.pose_dance_late, "dance", free = false, extra = arrayOf("frame" to "0.75")),
        pose("zombie_walk", R.string.pose_zombie_walk, "zombie", free = false, extra = arrayOf("frame" to "0.5")),
        pose("jump_peak", R.string.pose_jump_peak, "jump", free = false, extra = arrayOf("frame" to "0.5")),
        pose("sneak_peek", R.string.pose_sneak_peek, "sneak", free = false, extra = arrayOf("headYaw" to "45")),
        pose(
            "hero_low", R.string.pose_hero_low, "flex",
            free = false, extra = arrayOf("pitch" to "-30", "shadow" to "true"),
        ),
        pose(
            "flex_bust", R.string.pose_flex_bust, "flex",
            free = false, renderType = "bust", extra = arrayOf("zoom" to "1.3"),
        ),
        pose("think_bust", R.string.pose_think_bust, "think", free = false, renderType = "bust"),
        pose("salute_bust", R.string.pose_salute_bust, "salute", free = false, renderType = "bust"),
        pose("point_bust", R.string.pose_point_bust, "point", free = false, renderType = "bust"),
        pose("dab_bust", R.string.pose_dab_bust, "dab", free = false, renderType = "bust"),
        pose(
            "shrug_bust", R.string.pose_shrug_bust, "shrug",
            free = false, renderType = "bust", extra = arrayOf("headPitch" to "10"),
        ),
        pose(
            "face_closeup", R.string.pose_face_closeup, "default",
            free = false, renderType = "bust", extra = arrayOf("zoom" to "1.8"),
        ),
        pose("back_quarter", R.string.pose_back_quarter, "default", free = false, extra = arrayOf("yaw" to "135")),
        pose(
            "over_shoulder", R.string.pose_over_shoulder, "default",
            free = false, extra = arrayOf("yaw" to "160", "headYaw" to "-50"),
        ),
        pose("tiny", R.string.pose_tiny, "default", free = false, extra = arrayOf("zoom" to "0.5")),
        pose("action_tilt", R.string.pose_action_tilt, "run", free = false, extra = arrayOf("roll" to "18")),
        pose("spin_tpose", R.string.pose_spin_tpose, "tpose", free = false, extra = arrayOf("yaw" to "45")),
        pose("shadow_dance", R.string.pose_shadow_dance, "dance", free = false, extra = arrayOf("shadow" to "true")),
        pose(
            "power_punch", R.string.pose_power_punch, "punch",
            free = false, extra = arrayOf("roll" to "-12", "frame" to "0.5"),
        ),
        pose("big_cheer", R.string.pose_big_cheer, "cheer", free = false, extra = arrayOf("pitch" to "-15")),
        pose("sit_view", R.string.pose_sit_view, "sit", free = false, extra = arrayOf("pitch" to "30")),
    )

    fun byId(id: String): PoseDef? = POSES.firstOrNull { it.id == id }
}
