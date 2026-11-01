package io.github.mrans2006.yaad

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.mrans2006.yaad.core.PhoneBrand
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.Folder
import io.github.mrans2006.yaad.ui.Folders
import io.github.mrans2006.yaad.ui.HomeState
import io.github.mrans2006.yaad.ui.components.BottomBarHost
import io.github.mrans2006.yaad.ui.components.LocalPreviewImages
import io.github.mrans2006.yaad.ui.components.YaadTab
import io.github.mrans2006.yaad.ui.screens.BatteryActions
import io.github.mrans2006.yaad.ui.screens.BatteryGuideScreen
import io.github.mrans2006.yaad.ui.screens.DetailActions
import io.github.mrans2006.yaad.ui.screens.DetailScreen
import io.github.mrans2006.yaad.ui.screens.FolderActions
import io.github.mrans2006.yaad.ui.screens.FolderScreen
import io.github.mrans2006.yaad.ui.screens.FoldersActions
import io.github.mrans2006.yaad.ui.screens.FoldersScreen
import io.github.mrans2006.yaad.ui.screens.OnboardingActions
import io.github.mrans2006.yaad.ui.screens.OnboardingScreen
import io.github.mrans2006.yaad.ui.screens.OnboardingStep
import io.github.mrans2006.yaad.ui.screens.SearchActions
import io.github.mrans2006.yaad.ui.screens.SearchScreen
import io.github.mrans2006.yaad.ui.screens.SettingsActions
import io.github.mrans2006.yaad.ui.screens.SettingsScreen
import io.github.mrans2006.yaad.ui.screens.SettingsState
import io.github.mrans2006.yaad.ui.theme.Palette
import io.github.mrans2006.yaad.ui.theme.ThemeMode
import io.github.mrans2006.yaad.ui.theme.YaadTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders Yaad's REAL screens (the same Compose code that runs on the phone) to PNG files,
 * using sample screenshots. Run with:  ./gradlew :app:recordRoborazziDebug
 * The images land in app/build/screenshots-v1.1/.
 *
 * Phone size = Oppo Reno 6: 1080 x 2400 pixels = 411 x 891 "dp" at xxhdpi.
 * Animations can't be seen here: these are still frames.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()

    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L

    private fun sample(id: Long, name: String, keyLine: String, tags: String, ago: Long, text: String = keyLine, links: String = "") =
        Shot(id = id, uri = "sample://$name", source = Shot.SOURCE_AUTO, takenAt = now - ago, status = Shot.STATUS_DONE,
            keyLine = keyLine, tags = tags, text = text, links = links, width = 1080, height = 2340)

    private val shots = listOf(
        sample(1, "recuva", "Recuva — Restore deleted files", "Tools", 2 * hour,
            "recuva.com\nRecuva\nRestore deleted files\nAccidentally deleted an important file? Lost something important when your " +
                "computer crashed? No problem! Recuva recovers files from your Windows computer, recycle bin, digital camera card, or MP3 player.\nFree Download",
            "https://recuva.com"),
        sample(2, "llama", "ggerganov / llama.cpp — LLM inference in C/C++", "Code", 5 * hour),
        sample(3, "kolachi", "Kolachi — Seafood restaurant · Rs 3,000-4,000", "Places", 26 * hour),
        sample(4, "spoon", "Put a wooden spoon across a pot of boiling water and it won't boil over.", "Hacks", 50 * hour),
        sample(5, "dp", "Lecture 7: Dynamic Programming", "Study", 3 * 24 * hour),
        sample(6, "buds", "Xiaomi Redmi Buds 5 Wireless Earbuds", "Shopping", 4 * 24 * hour),
        sample(7, "quote", "\u201CThe best time to plant a tree was 20 years ago. The second best time is now.\u201D", "Quotes", 6 * 24 * hour),
        sample(8, "chat", "bro send me the DSA notes for tomorrow's exam", "Chats,Study", 9 * 24 * hour),
        sample(9, "testdisk", "TestDisk Step By Step", "Tools,Code", 12 * 24 * hour),
        sample(10, "recuva", "", "", 40 * 24 * hour, text = ""),
    )

    private val images by lazy {
        listOf("recuva", "llama", "kolachi", "spoon", "dp", "buds", "quote", "chat", "testdisk").associate { name ->
            val bytes = javaClass.getResourceAsStream("/samples/$name.png")!!.readBytes()
            "sample://$name" to BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
        }
    }

    private fun shoot(file: String, mode: ThemeMode, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalPreviewImages provides images) {
                YaadTheme(mode) { content() }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots-v1.1/$file.png")
    }

    /** A tab screen with the bottom bar, as on the phone. */
    @Composable
    private fun Tab(tab: YaadTab, content: @Composable () -> Unit) = BottomBarHost(true, tab, {}) { content() }

    private val home = HomeState(shots = shots, loaded = true, showBatteryTip = false)
    private val recuvaQuery = home.copy(query = "a tool for data recovery", results = listOf(shots[8], shots[0]))
    private val folder: Folder get() = Folders.find(shots, Folder.tagKey("Tools"))!!

    // 01–02: Search tab before typing: logo, search box, recent searches, battery tip.
    @Test fun searchEmptyLight() = shoot("01_search_empty_light", ThemeMode.LIGHT) {
        Tab(YaadTab.SEARCH) {
            SearchScreen(home.copy(showBatteryTip = true, phoneBrand = PhoneBrand.OPPO,
                recent = listOf("llama.cpp", "seafood place by the sea", "dynamic programming notes")), SearchActions())
        }
    }

    @Test fun searchEmptyDark() = shoot("02_search_empty_dark", ThemeMode.DARK) {
        Tab(YaadTab.SEARCH) {
            SearchScreen(home.copy(pending = 291, progressDone = 121, progressTotal = 412, showBatteryTip = true, phoneBrand = PhoneBrand.SAMSUNG), SearchActions())
        }
    }

    // 03–04: results appear as you type.
    @Test fun searchResultsLight() = shoot("03_search_results_light", ThemeMode.LIGHT) {
        Tab(YaadTab.SEARCH) { SearchScreen(recuvaQuery, SearchActions()) }
    }

    @Test fun searchResultsDark() = shoot("04_search_results_dark", ThemeMode.DARK) {
        Tab(YaadTab.SEARCH) { SearchScreen(recuvaQuery, SearchActions()) }
    }

    // 05–06: Folders tab.
    @Test fun foldersLight() = shoot("05_folders_light", ThemeMode.LIGHT) { Tab(YaadTab.FOLDERS) { FoldersScreen(shots, FoldersActions()) } }

    @Test fun foldersDark() = shoot("06_folders_dark", ThemeMode.DARK) { Tab(YaadTab.FOLDERS) { FoldersScreen(shots, FoldersActions()) } }

    // 07–08: inside a folder ("All"), grouped by date.
    @Test fun folderGridLight() = shoot("07_folder_all_light", ThemeMode.LIGHT) {
        Tab(YaadTab.FOLDERS) { FolderScreen(Folders.find(shots, Folder.ALL), FolderActions(), now = now) }
    }

    @Test fun folderGridDark() = shoot("08_folder_tools_dark", ThemeMode.DARK) {
        Tab(YaadTab.FOLDERS) { FolderScreen(folder, FolderActions(), now = now) }
    }

    // 09–10: detail.
    @Test fun detailLight() = shoot("09_detail_light", ThemeMode.LIGHT) { DetailScreen(shots[0], DetailActions()) }

    @Test fun detailDark() = shoot("10_detail_dark", ThemeMode.DARK) { DetailScreen(shots[0], DetailActions()) }

    @Test @Config(qualifiers = "w411dp-h1450dp-xxhdpi")
    fun detailFull() = shoot("11_detail_full_light", ThemeMode.LIGHT) { DetailScreen(shots[0], DetailActions()) }

    // 12–13: battery guide.
    @Test fun batteryLight() = shoot("12_battery_guide_light", ThemeMode.LIGHT) { BatteryGuideScreen(false, PhoneBrand.SAMSUNG, BatteryActions()) }

    @Test fun batteryDark() = shoot("13_battery_guide_dark", ThemeMode.DARK) { BatteryGuideScreen(true, PhoneBrand.XIAOMI, BatteryActions()) }

    @Test @Config(qualifiers = "w411dp-h2800dp-xxhdpi")
    fun batteryOtherPhones() = shoot("14_battery_other_phones_light", ThemeMode.LIGHT) {
        BatteryGuideScreen(false, PhoneBrand.OPPO, BatteryActions(), openBrand = PhoneBrand.SAMSUNG)
    }

    // Extras: settings, onboarding, icon.
    @Test fun settingsLight() = shoot("15_settings_light", ThemeMode.LIGHT) {
        SettingsScreen(SettingsState(theme = ThemeMode.SYSTEM, total = 412, failed = 3, autoPickOn = true, batteryOk = false, version = "1.1"), SettingsActions())
    }

    @Test fun settingsDark() = shoot("16_settings_dark", ThemeMode.DARK) {
        SettingsScreen(SettingsState(theme = ThemeMode.DARK, total = 412, autoPickOn = true, batteryOk = true, version = "1.1"), SettingsActions())
    }

    @Test fun onboardingLight() = shoot("17_onboarding_light", ThemeMode.LIGHT) { OnboardingScreen(OnboardingStep.Welcome, OnboardingActions()) }

    @Test fun onboardingImportDark() = shoot("18_onboarding_import_dark", ThemeMode.DARK) {
        OnboardingScreen(OnboardingStep.Import(412), OnboardingActions())
    }

    @Test @Config(qualifiers = "w200dp-h200dp-xxhdpi")
    fun appIcon() = shoot("19_app_icon", ThemeMode.LIGHT) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(200.dp).background(Palette.Yellow))
    }
}
