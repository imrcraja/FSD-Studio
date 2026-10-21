package com.fsdetc.fsdstudio.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.CharacterInfo
import com.fsdetc.fsdstudio.data.DetailRequest
import com.fsdetc.fsdstudio.data.InvalidSkinException
import com.fsdetc.fsdstudio.data.PlayerNotFoundException
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.db.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Shared state for Home / Poses / detail: current character, search, upload, detail routing. */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container

    val character: StateFlow<CharacterInfo?> = container.characterStore.character
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPro: StateFlow<Boolean> = container.entitlements.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val recentHistory: StateFlow<List<HistoryItem>> = container.historyRepository.items
        .map { it.take(4) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed interface SearchState {
        data object Idle : SearchState
        data object Loading : SearchState
        data class Error(val message: String) : SearchState
    }

    val searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val uploadError = MutableStateFlow<String?>(null)
    val uploading = MutableStateFlow(false)

    /** Set before navigating to the detail route; read by RenderDetailScreen. */
    val detailRequest = MutableStateFlow<DetailRequest?>(null)

    fun openDetail(request: DetailRequest) {
        detailRequest.value = request
    }

    /** Profile search fires ONLY from the Search button — never per keystroke. */
    fun search(username: String) {
        val name = username.trim()
        if (name.isEmpty() || searchState.value is SearchState.Loading) return
        searchState.value = SearchState.Loading
        viewModelScope.launch {
            try {
                val info = container.skinRepository.fetchProfile(name)
                container.characterStore.save(info)
                searchState.value = SearchState.Idle
            } catch (e: PlayerNotFoundException) {
                searchState.value = SearchState.Error(app.getString(R.string.error_player_not_found))
            } catch (e: Exception) {
                searchState.value = SearchState.Error(app.getString(R.string.error_network))
            }
        }
    }

    fun importSkin(uri: Uri) {
        uploading.value = true
        uploadError.value = null
        viewModelScope.launch {
            try {
                val info = SkinBitmaps.importSkin(app, uri)
                container.characterStore.save(info)
            } catch (e: InvalidSkinException) {
                uploadError.value = app.getString(R.string.error_upload_invalid)
            } catch (e: Exception) {
                uploadError.value = app.getString(R.string.error_upload_failed)
            } finally {
                uploading.value = false
            }
        }
    }

    // ------------------------------------------------------------ detail helpers

    /** Download (if needed) + record a remote render into history. Returns the row. */
    suspend fun ensureRemoteHistory(
        url: String,
        title: String,
        subtitle: String,
        sourceName: String,
    ): HistoryItem {
        val bytes = container.skinRepository.downloadBytes(url)
        return container.historyRepository.recordBytes(
            bytes,
            kind = "POSE",
            title = title,
            subtitle = subtitle,
            sourceName = sourceName,
            uniqueKey = "url:$url",
        )
    }

    fun recordRemoteInBackground(url: String, title: String, subtitle: String, sourceName: String) {
        viewModelScope.launch {
            runCatching { ensureRemoteHistory(url, title, subtitle, sourceName) }
        }
    }

    /** Copy a locally built flat render into history (once per file). */
    suspend fun ensureLocalHistory(
        file: File,
        title: String,
        subtitle: String,
        sourceName: String,
    ): HistoryItem = container.historyRepository.recordCopy(
        file,
        kind = "FLAT",
        title = title,
        subtitle = subtitle,
        sourceName = sourceName,
        uniqueKey = "file:${file.absolutePath}",
    )

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { container.historyRepository.deleteById(id) }
    }
}
