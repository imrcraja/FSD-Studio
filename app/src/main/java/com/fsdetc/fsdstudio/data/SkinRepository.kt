package com.fsdetc.fsdstudio.data

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * All network access lives here.
 *
 * API discipline (owner's rule):
 * - 20 MB OkHttp disk cache; render URLs are immutable so repeats are cache hits.
 * - Profile JSON is cached in memory (LruCache) and in DataStore for 24 h.
 * - On HTTP 429 from skinrender.dev: exactly ONE automatic retry after 2 s, GET only.
 * - AI generation uses a separate long-timeout client; callers enforce one-in-flight.
 */
class SkinRepository(private val context: Context) {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http_cache"), 20L * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(RetryOnceOn429())
        .build()

    val aiClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .build()

    private val memoryProfileCache = android.util.LruCache<String, CharacterInfo>(32)

    /**
     * Fetch a player's profile. Throws [PlayerNotFoundException] on 404,
     * [IOException] on other failures.
     */
    suspend fun fetchProfile(rawName: String): CharacterInfo = withContext(Dispatchers.IO) {
        val name = rawName.trim()
        val key = name.lowercase()
        memoryProfileCache.get(key)?.let { return@withContext it }

        val jsonKey = stringPreferencesKey("profile_$key")
        val tsKey = longPreferencesKey("profile_ts_$key")
        val prefs = context.dataStore.data.first()
        val cachedJson = prefs[jsonKey]
        val cachedTs = prefs[tsKey] ?: 0L
        if (cachedJson != null &&
            System.currentTimeMillis() - cachedTs < PROFILE_TTL_MS
        ) {
            parseProfile(cachedJson)?.let {
                memoryProfileCache.put(key, it)
                return@withContext it
            }
        }

        val request = Request.Builder().url(Api.profileUrl(name)).build()
        httpClient.newCall(request).execute().use { resp ->
            if (resp.code == 404) throw PlayerNotFoundException()
            if (!resp.isSuccessful) throw IOException("Profile HTTP ${resp.code}")
            val body = resp.body.string()
            val info = parseProfile(body) ?: throw IOException("Bad profile data")
            context.dataStore.edit { p ->
                p[jsonKey] = body
                p[tsKey] = System.currentTimeMillis()
            }
            memoryProfileCache.put(key, info)
            info
        }
    }

    /** Raw bytes of any URL (renders, mostly). Cache makes repeats cheap. */
    suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body.bytes()
        }
    }

    /**
     * One AI generation. If a reference image was supplied and the call fails,
     * retries exactly once WITHOUT the reference (per the verified API contract).
     * The returned bytes are re-encoded to PNG by the caller before saving.
     */
    suspend fun generateAiImage(fullPrompt: String, seed: Int, referenceUrl: String?): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                callAi(Api.aiUrl(fullPrompt, seed, referenceUrl))
            } catch (e: Exception) {
                if (referenceUrl != null) {
                    callAi(Api.aiUrl(fullPrompt, seed, null))
                } else {
                    throw e
                }
            }
        }

    private fun callAi(url: String): ByteArray {
        val request = Request.Builder().url(url).header("Accept", "image/*").build()
        aiClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("AI HTTP ${resp.code}")
            return resp.body.bytes()
        }
    }

    private fun parseProfile(json: String): CharacterInfo? = try {
        val o = JSONObject(json)
        val name = o.optString("name")
        if (name.isBlank()) {
            null
        } else {
            CharacterInfo(
                name = name,
                uuid = o.optString("uuid").ifBlank { null },
                model = o.optString("model").ifBlank { null },
                skinHash = o.optString("skinHash").ifBlank { null },
                skinUrl = o.optString("skinUrl").ifBlank { null },
                localSkinPath = null,
                source = CharacterSource.USERNAME,
            )
        }
    } catch (e: Exception) {
        null
    }

    private class RetryOnceOn429 : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code == 429 &&
                request.method == "GET" &&
                request.url.host == "skinrender.dev"
            ) {
                response.close()
                Thread.sleep(2000)
                return chain.proceed(request)
            }
            return response
        }
    }

    companion object {
        private const val PROFILE_TTL_MS = 24L * 60 * 60 * 1000
    }
}
