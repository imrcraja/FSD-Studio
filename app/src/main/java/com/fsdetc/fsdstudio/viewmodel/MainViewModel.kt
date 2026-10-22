package com.fsdetc.fsdstudio.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.CharacterInfo
import com.fsdetc.fsdstudio.data.DetailRequest
import com.fsdetc.fsdstudio.data.DownloadGate
import com.fsdetc.fsdstudio.data.Entitlements
import com.fsdetc.fsdstudio.data.InvalidSkinException
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.data.PlayerNotFoundException
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.db.HistoryItem
import com.fsdetc.fsdstudio.data.toErrorKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Shared state for Home / Poses / detail: current character, search, upload, detail routing. */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container

    /** Shared services the screens read limits/defaults from. */
    val entitlements: Entitlements = container.entitlements
    val downloadGate: DownloadGate = container.downloadGate

    val character: StateFlow<CharacterInfo?> = container.characterStore.character
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plan: StateFlow<Plan> = container.entitlements.plan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Plan.FREE)

    /** Downloads left today; null = unlimited (paid plan). */
    val downloadsLeft: StateFlow<Int?> = combine(
        container.entitlements.plan,
        container.entitlements.downloadsToday,
    ) { plan, used ->
        val limit = container.entitlements.downloadsPerDay(plan)
        if (limit == Int.MAX_VALUE) null else (limit - used).coerceAtLeast(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val defaultBgName: StateFlow<String> = container.userSettings.defaultBgName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Transparent")

    val defaultRenderSize: StateFlow<Int> = container.userSettings.defaultSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 512)

    val recentHistory: StateFlow<List<HistoryItem>> = container.historyRepository.items
        .map { it.take(4) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ------------------------------------------------------------ upgrade pop-up

    /** The once-a-day gentle upgrade dialog (fired by the 3rd free download). */
    val showUpgradePopup = MutableStateFlow(false)

    // ------------------------------------------------------------ snackbars

    /** App-wide transient messages, shown by the nav host's bottom SnackbarHost. */
    val snackbarMessages = container.snackbarMessages

    fun postSnackbar(message: String) = container.postSnackbar(message)

    // ------------------------------------------------------------ tutorial

    /** null while the stored flag is still loading, then true/false. */
    val seenTutorial: StateFlow<Boolean?> = container.userSettings.seenTutorial
        .map { seen -> seen as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markTutorialSeen() {
        viewModelScope.launch { container.userSettings.setSeenTutorial(true) }
    }

    /** Settings → "Replay tutorial": the nav host shows the tutorial again. */
    fun replayTutorial() {
        viewModelScope.launch { container.userSettings.setSeenTutorial(false) }
    }

    init {
        viewModelScope.launch {
            container.downloadGate.thirdDownloadEvents.collect {
                showUpgradePopup.value = true
            }
        }
    }

    fun dismissUpgradePopup() {
        showUpgradePopup.value = false
    }

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
                searchState.value = SearchState.Error(app.getString(e.toErrorKind().messageRes))
            }
        }
    }

    /** Library: a static character (Mr FSD / Steve / Alex) becomes current directly. */
    fun selectCharacter(info: CharacterInfo) {
        viewModelScope.launch { container.characterStore.save(info) }
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
        kind: String = "POSE",
    ): HistoryItem {
        val bytes = container.skinRepository.downloadBytes(url)
        return container.historyRepository.recordBytes(
            bytes,
            kind = kind,
            title = title,
            subtitle = subtitle,
            sourceName = sourceName,
            uniqueKey = "url:$url",
        )
    }

    fun recordRemoteInBackground(
        url: String,
        title: String,
        subtitle: String,
        sourceName: String,
        kind: String = "POSE",
    ) {
        viewModelScope.launch {
            runCatching { ensureRemoteHistory(url, title, subtitle, sourceName, kind) }
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
