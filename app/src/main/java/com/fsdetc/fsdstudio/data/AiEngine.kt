package com.fsdetc.fsdstudio.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.random.Random

/** Typed AI-generation failures so the UI can show the right human message. */
sealed interface AiError {
    /** The Gemini key's free quota is exhausted (HTTP 429). */
    data object Quota : AiError

    /** The Gemini key was rejected (HTTP 400/403). */
    data object BadKey : AiError

    /** The request timed out. */
    data object Timeout : AiError

    /** Anything else; [cause] is mapped through [toErrorKind] for display. */
    data class Other(val cause: Throwable) : AiError
}

class AiEngineException(val error: AiError) : Exception(
    when (error) {
        AiError.Quota -> "Gemini quota exceeded"
        AiError.BadKey -> "Gemini key rejected"
        AiError.Timeout -> "AI request timed out"
        is AiError.Other -> error.cause.message ?: "AI request failed"
    },
)

/**
 * One AI image generation. Engines use whichever reference they support:
 * [PollinationsEngine] takes the public reference *URL*, [GeminiEngine] takes
 * the reference image *bytes* (sent inline). The other parameter is ignored.
 */
interface AiEngine {
    suspend fun generate(
        prompt: String,
        referenceUrl: String?,
        referenceBytes: ByteArray?,
    ): Result<ByteArray>
}

/**
 * The free built-in engine (Pollinations, keyless). Attempt order, per the
 * verified behaviour of the service: flux with the reference image, then flux
 * without it, then turbo without it. One call chain, no retry storms.
 */
class PollinationsEngine(private val client: OkHttpClient) : AiEngine {

    override suspend fun generate(
        prompt: String,
        referenceUrl: String?,
        referenceBytes: ByteArray?,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
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

/**
 * Google Gemini image generation with the user's own AI Studio key (BYOK).
 * Request shape verified against the live API: POST
 * /v1beta/models/<model>:generateContent with header `x-goog-api-key` and the
 * reference image as an inline_data part; the reply carries the image in
 * parts[].inlineData.data (base64).
 *
 * Error mapping: 429 → [AiError.Quota] (after trying the next model),
 * 400/403 → [AiError.BadKey], timeouts → [AiError.Timeout].
 */
class GeminiEngine(
    private val client: OkHttpClient,
    private val settings: UserSettings,
) : AiEngine {

    override suspend fun generate(
        prompt: String,
        referenceUrl: String?,
        referenceBytes: ByteArray?,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val key = settings.geminiKey()
            ?: return@withContext Result.failure(AiEngineException(AiError.BadKey))
        var quotaSeen = false
        for (model in MODELS) {
            try {
                return@withContext Result.success(call(model, key, prompt, referenceBytes))
            } catch (e: AiEngineException) {
                when (e.error) {
                    AiError.Quota -> quotaSeen = true // fall through to the next model
                    else -> return@withContext Result.failure(e)
                }
            } catch (e: SocketTimeoutException) {
                return@withContext Result.failure(AiEngineException(AiError.Timeout))
            } catch (e: Exception) {
                return@withContext Result.failure(AiEngineException(AiError.Other(e)))
            }
        }
        Result.failure(
            AiEngineException(
                if (quotaSeen) AiError.Quota
                else AiError.Other(IOException("Gemini request failed")),
            ),
        )
    }

    private fun call(model: String, key: String, prompt: String, referenceBytes: ByteArray?): ByteArray {
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        if (referenceBytes != null) {
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/png")
                        .put("data", Base64.encodeToString(referenceBytes, Base64.NO_WRAP)),
                ),
            )
        }
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .toString()
        val request = Request.Builder()
            .url("$BASE/models/$model:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        client.newCall(request).execute().use { resp ->
            val text = (resp.body ?: throw IOException("Empty Gemini response")).string()
            when {
                resp.code == 429 -> throw AiEngineException(AiError.Quota)
                resp.code == 400 || resp.code == 403 -> throw AiEngineException(AiError.BadKey)
                !resp.isSuccessful -> throw HttpException(resp.code)
            }
            return parseImage(text)
        }
    }

    private fun parseImage(json: String): ByteArray {
        val root = JSONObject(json)
        val candidates = root.optJSONArray("candidates")
            ?: throw IOException("Gemini returned no candidates")
        for (i in 0 until candidates.length()) {
            val parts = candidates.optJSONObject(i)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: continue
            for (j in 0 until parts.length()) {
                val data = parts.optJSONObject(j)
                    ?.optJSONObject("inlineData")
                    ?.optString("data")
                    ?.takeIf { it.isNotBlank() }
                if (data != null) return Base64.decode(data, Base64.DEFAULT)
            }
        }
        throw IOException("Gemini returned no image")
    }

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        /** Verified image models, best first. */
        val MODELS = listOf("gemini-2.5-flash-image", "gemini-3.1-flash-lite-image")
    }
}
