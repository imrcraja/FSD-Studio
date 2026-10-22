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

    data class Pose(val id: String, val label: String)

    fun profileUrl(username: String): String = "$BASE/api/profile/$username"

    /**
     * Render URL builder. [extra] carries any additional documented API
     * parameters (camera yaw/pitch/roll, fov, zoom, offsetX/offsetY, pose
     * frame, joint overrides, cape/overlay/shadow/shading toggles) — entries
     * in [extra] win over the convenience arguments.
     *
     * @param bgHex background colour as hex without '#', or null for a transparent render.
     */
    fun renderUrl(
        id: String,
        type: String,
        size: Int,
        pose: String? = null,
        bgHex: String? = null,
        extra: Map<String, String> = emptyMap(),
    ): String {
        val params = linkedMapOf<String, String>()
        params["size"] = size.toString()
        if (pose != null) params["pose"] = pose
        for ((k, v) in extra) params[k] = v
        if (bgHex != null) params["bg"] = bgHex
        return "$BASE/render/$id/$type?" +
            params.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    /** Mob render: GET /render/mob/<mob>/<body|head>?size=&pose=&bg= */
    fun mobRenderUrl(
        mob: String,
        type: String,
        size: Int,
        pose: String? = null,
        bgHex: String? = null,
    ): String {
        val params = mutableListOf("size=$size")
        if (pose != null) params += "pose=$pose"
        if (bgHex != null) params += "bg=$bgHex"
        return "$BASE/render/mob/$mob/$type?" + params.joinToString("&")
    }

    fun aiUrl(prompt: String, seed: Int, referenceUrl: String?, model: String = "flux"): String {
        val encodedPrompt = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        val sb = StringBuilder("https://image.pollinations.ai/prompt/")
            .append(encodedPrompt)
            .append("?width=1024&height=1024&seed=")
            .append(seed)
            .append("&model=")
            .append(model)
            .append("&nologo=true")
        if (referenceUrl != null) {
            sb.append("&image=").append(URLEncoder.encode(referenceUrl, "UTF-8"))
        }
        return sb.toString()
    }
}
