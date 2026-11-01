package io.github.mrans2006.yaad.data

import android.content.Context
import io.github.mrans2006.yaad.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small settings saved on the phone (theme, whether onboarding is done, ...). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("yaad", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(ThemeMode.valueOf(sp.getString("theme", ThemeMode.SYSTEM.name)!!))
    val theme: StateFlow<ThemeMode> = _theme
    fun setTheme(mode: ThemeMode) { sp.edit().putString("theme", mode.name).apply(); _theme.value = mode }

    private val _onboarded = MutableStateFlow(sp.getBoolean("onboarded", false))
    val onboarded: StateFlow<Boolean> = _onboarded
    fun setOnboarded() { sp.edit().putBoolean("onboarded", true).apply(); _onboarded.value = true }

    /**
     * If you chose "only new screenshots", this is the moment you chose it (seconds since 1970).
     * Screenshots older than this are skipped. 0 = import everything.
     */
    var onlyNewAfter: Long
        get() = sp.getLong("onlyNewAfter", 0)
        set(v) = sp.edit().putLong("onlyNewAfter", v).apply()

    private val _batteryTipDismissed = MutableStateFlow(sp.getBoolean("batteryTipDismissed", false))
    val batteryTipDismissed: StateFlow<Boolean> = _batteryTipDismissed
    fun dismissBatteryTip() { sp.edit().putBoolean("batteryTipDismissed", true).apply(); _batteryTipDismissed.value = true }

    /** The last few searches (newest first), shown as one-tap chips on the empty Search tab. Stays on the phone. */
    private val _recentQueries = MutableStateFlow(
        sp.getString("recentQueries", "").orEmpty().split('\n').filter { it.isNotBlank() },
    )
    val recentQueries: StateFlow<List<String>> = _recentQueries
    fun addRecentQuery(query: String) {
        val q = query.trim().replace('\n', ' ')
        if (q.length < 2) return
        val list = (listOf(q) + _recentQueries.value.filterNot { it.equals(q, ignoreCase = true) }).take(5)
        sp.edit().putString("recentQueries", list.joinToString("\n")).apply()
        _recentQueries.value = list
    }
    fun clearRecentQueries() { sp.edit().remove("recentQueries").apply(); _recentQueries.value = emptyList() }

    var lastScanAt: Long
        get() = sp.getLong("lastScanAt", 0)
        set(v) = sp.edit().putLong("lastScanAt", v).apply()
}
