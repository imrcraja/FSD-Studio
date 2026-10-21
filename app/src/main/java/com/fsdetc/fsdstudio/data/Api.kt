package com.fsdetc.fsdstudio.data

import java.net.URLEncoder

/**
 * URL builders + constants for the two free services the app talks to.
 *
 * - skinrender.dev — profiles and renders (keyless, ETag-cached, same URL = same bytes).
 *   Background uses its documented `bg` hex parameter (see https://skinrender.dev/llms.txt).
 * - image.pollinations.ai — free AI images (keyless flux tier).
 */
object Api {
    const val BASE = "https://skinrender.dev"

    const val FREE_POSE_COUNT = 10
    const val FREE_AI_PER_DAY = 3
    const val PRO_AI_PER_DAY = 50

    data class Pose(val id: String, val label: String)

    /** All 18 poses, in API order. The first [FREE_POSE_COUNT] are free. */
    val POSES: List<Pose> = listOf(
        Pose("default", "Default"),
        Pose("walk", "Walk"),
        Pose("run", "Run"),
        Pose("wave", "Wave"),
        Pose("sit", "Sit"),
        Pose("sneak", "Sneak"),
        Pose("point", "Point"),
        Pose("dab", "Dab"),
        Pose("zombie", "Zombie"),
        Pose("tpose", "T-Pose"),
        Pose("cheer", "Cheer"),
        Pose("think", "Think"),
        Pose("flex", "Flex"),
        Pose("salute", "Salute"),
        Pose("dance", "Dance"),
        Pose("punch", "Punch"),
        Pose("shrug", "Shrug"),
        Pose("jump", "Jump"),
    )

    fun profileUrl(username: String): String = "$BASE/api/profile/$username"

    /**
     * @param bgHex background colour as hex without '#', or null for a transparent render.
     */
    fun renderUrl(
        id: String,
        type: String,
        size: Int,
        pose: String? = null,
        bgHex: String? = null,
    ): String {
        val params = mutableListOf("size=$size")
        if (pose != null) params += "pose=$pose"
        if (bgHex != null) params += "bg=$bgHex"
        return "$BASE/render/$id/$type?" + params.joinToString("&")
    }

    fun aiUrl(prompt: String, seed: Int, referenceUrl: String?): String {
        val encodedPrompt = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        val sb = StringBuilder("https://image.pollinations.ai/prompt/")
            .append(encodedPrompt)
            .append("?width=1024&height=1024&seed=")
            .append(seed)
            .append("&model=flux&nologo=true")
        if (referenceUrl != null) {
            sb.append("&image=").append(URLEncoder.encode(referenceUrl, "UTF-8"))
        }
        return sb.toString()
    }
}
