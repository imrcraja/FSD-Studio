package com.fsdetc.fsdstudio.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.random.Random

/**
 * The app's one built-in AI engine (Pollinations, keyless) — AI edits need no
 * user setup, no API key, no account. Failures surface as the raw network
 * exceptions and are mapped to human messages via [toErrorKind].
 *
 * Attempt order, per the verified behaviour of the service: flux WITH the
 * reference image first (edit mode), then flux without it, then turbo without
 * it. One call chain, no retry storms.
 */
class PollinationsEngine(private val client: OkHttpClient) {

    /**
     * Run one edit/generation for [prompt]. [referenceUrl] is the public URL
     * of the render being edited, when there is one.
     */
    suspend fun generate(prompt: String, referenceUrl: String?): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val seed = Random.nextInt(1, Int.MAX_VALUE)
            val attempts = buildList {
                add("flux" to referenceUrl)
                if (referenceUrl != null) add("flux" to null)
                add("turbo" to null)
            }
            var last: Throwable = IOException("AI request failed")
            for ((model, ref) in attempts) {
                try {
                    return@withContext Result.success(call(Api.aiUrl(prompt, seed, ref, model)))
                } catch (e: Exception) {
                    last = e
                }
            }
            Result.failure(last)
        }

    private fun call(url: String): ByteArray {
        val request = Request.Builder().url(url).header("Accept", "image/*").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpException(resp.code)
            return (resp.body ?: throw IOException("Empty AI response")).bytes()
        }
    }
}
