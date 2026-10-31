package io.github.mrans2006.yaad.ui

import io.github.mrans2006.yaad.battery.ThisPhone
import io.github.mrans2006.yaad.core.PhoneBrand
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.mrans2006.yaad.YaadApp
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.pickup.AutoPick
import io.github.mrans2006.yaad.pickup.Permissions
import io.github.mrans2006.yaad.ui.theme.ThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the home screen shows, in one object. */
data class HomeState(
    val shots: List<Shot> = emptyList(),
    val query: String = "",
    val results: List<Shot>? = null, // null = not searching
    val searching: Boolean = false,
    val selectedTag: String? = null,
    val pending: Int = 0,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val hasPermission: Boolean = true,
    val showBatteryTip: Boolean = false,
    /** This phone's family (Samsung, Xiaomi, …), so the battery tip can name it. */
    val phoneBrand: PhoneBrand = PhoneBrand.GENERIC,
    val loaded: Boolean = false,
) {
    /** What the grid should show right now: search results or all screenshots, filtered by tag. */
    val visible: List<Shot>
        get() = (results ?: shots).let { list ->
            if (selectedTag == null) list else list.filter { selectedTag in it.tagList }
        }
}

/**
 * The ViewModel is the bridge between the data (Library) and the screens.
 * It survives screen rotations, and turns database updates into [HomeState] the UI can draw.
 */
@OptIn(FlowPreview::class)
class YaadViewModel(app: Application) : AndroidViewModel(app) {
    private val yaad = app as YaadApp
    private val library = yaad.library
    val prefs = yaad.prefs

    private val query = MutableStateFlow("")
    private val selectedTag = MutableStateFlow<String?>(null)
    private val searchResults = MutableStateFlow<List<Long>?>(null)
    private val searching = MutableStateFlow(false)
    private val permission = MutableStateFlow(Permissions.canReadScreenshots(app))
    private val batteryOk = MutableStateFlow(Permissions.isBatteryUnrestricted(app))

    private val progress = WorkManager.getInstance(app).getWorkInfosForUniqueWorkFlow("yaad-scan")
        .map { infos ->
            val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            (running?.progress?.getInt("done", 0) ?: 0) to (running?.progress?.getInt("total", 0) ?: 0)
        }

    private val search = combine(query, searchResults, searching, selectedTag) { q, r, s, t -> SearchBits(q, r, s, t) }
    private data class SearchBits(val q: String, val ids: List<Long>?, val searching: Boolean, val tag: String?)

    val state: StateFlow<HomeState> = combine(
        library.observeAll(), search, library.observePendingCount(), progress,
        combine(permission, batteryOk, prefs.batteryTipDismissed) { p, b, d -> Triple(p, b, d) },
    ) { shots, s, pending, prog, flags ->
        val byId = shots.associateBy { it.id }
        HomeState(
            shots = shots,
            query = s.q,
            results = s.ids?.mapNotNull { byId[it] },
            searching = s.searching,
            selectedTag = s.tag,
            pending = pending,
            progressDone = prog.first,
            progressTotal = prog.second,
            hasPermission = flags.first,
            showBatteryTip = !flags.second && !flags.third,
            phoneBrand = ThisPhone.brand,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    val theme: StateFlow<ThemeMode> = prefs.theme

    init {
        // Search runs a moment after you stop typing, not on every key press.
        viewModelScope.launch {
            query.debounce(250).distinctUntilChanged().collect { q ->
                if (q.isBlank()) { searchResults.value = null; return@collect }
                searching.value = true
                searchResults.value = library.search(q).map { it.id }
                searching.value = false
            }
        }
    }

    fun onQueryChange(q: String) {
        query.value = q
        if (q.isBlank()) searchResults.value = null
    }

    fun onTagSelected(tag: String?) { selectedTag.value = if (selectedTag.value == tag) null else tag }

    /** Called every time the app comes to the front: re-check permissions and catch up. */
    fun onResume() {
        val ctx = getApplication<Application>()
        permission.value = Permissions.canReadScreenshots(ctx)
        batteryOk.value = Permissions.isBatteryUnrestricted(ctx)
        AutoPick.scanNow(ctx)
    }

    fun onPermissionResult() { onResume() }

    fun importPicked(uris: List<Uri>) = viewModelScope.launch {
        if (uris.isEmpty()) return@launch
        library.importShared(uris)
        AutoPick.scanNow(getApplication())
    }

    fun delete(shot: Shot) = viewModelScope.launch { library.delete(shot) }

    fun retryFailed() = viewModelScope.launch { library.retryFailed(); AutoPick.scanNow(getApplication()) }

    fun observeShot(id: Long) = library.observe(id)

    suspend fun countScreenshotsOnPhone() = library.countScreenshotsOnPhone()

    /** Onboarding answer: import everything already on the phone, or only new ones from now. */
    fun finishOnboarding(importExisting: Boolean) {
        if (!importExisting) prefs.onlyNewAfter = System.currentTimeMillis() / 1000
        prefs.setOnboarded()
        AutoPick.scanNow(getApplication())
    }

    fun setTheme(mode: ThemeMode) = prefs.setTheme(mode)
    fun dismissBatteryTip() = prefs.dismissBatteryTip()
}
