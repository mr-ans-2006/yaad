package io.github.mrans2006.yaad

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.ui.theme.Palette
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.HomeState
import io.github.mrans2006.yaad.ui.components.LocalPreviewImages
import io.github.mrans2006.yaad.core.PhoneBrand
import io.github.mrans2006.yaad.ui.screens.BatteryActions
import io.github.mrans2006.yaad.ui.screens.BatteryGuideScreen
import io.github.mrans2006.yaad.ui.screens.DetailActions
import io.github.mrans2006.yaad.ui.screens.DetailScreen
import io.github.mrans2006.yaad.ui.screens.HomeActions
import io.github.mrans2006.yaad.ui.screens.HomeScreen
import io.github.mrans2006.yaad.ui.screens.OnboardingActions
import io.github.mrans2006.yaad.ui.screens.OnboardingScreen
import io.github.mrans2006.yaad.ui.screens.OnboardingStep
import io.github.mrans2006.yaad.ui.screens.SettingsActions
import io.github.mrans2006.yaad.ui.screens.SettingsScreen
import io.github.mrans2006.yaad.ui.screens.SettingsState
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
 * The images land in docs/screenshots/ (the README shows them).
 *
 * Phone size = Oppo Reno 6: 1080 x 2400 pixels = 411 x 891 "dp" at xxhdpi.
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
            keyLine = keyLine, tags = tags, text = text, links = links)

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
        sample(9, "testdisk", "TestDisk Step By Step", "Tools", 12 * 24 * hour),
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
        compose.onRoot().captureRoboImage("../docs/screenshots/$file.png")
    }

    private val home = HomeState(shots = shots, loaded = true, showBatteryTip = false)

    @Test fun homeLight() = shoot("01_home_light", ThemeMode.LIGHT) { HomeScreen(home, HomeActions()) }

    @Test fun homeDark() = shoot("02_home_dark", ThemeMode.DARK) { HomeScreen(home, HomeActions()) }

    @Test fun search() = shoot("03_search_light", ThemeMode.LIGHT) {
        HomeScreen(home.copy(query = "a tool for data recovery", results = listOf(shots[8], shots[0])), HomeActions())
    }

    @Test fun searchDark() = shoot("04_search_dark", ThemeMode.DARK) {
        HomeScreen(home.copy(query = "seafood place by the sea", results = listOf(shots[2])), HomeActions())
    }

    @Test fun importing() = shoot("05_home_importing", ThemeMode.LIGHT) {
        val pending = Shot(id = 99, uri = "sample://quote", source = Shot.SOURCE_AUTO, takenAt = now, status = Shot.STATUS_PENDING)
        HomeScreen(home.copy(shots = listOf(pending) + shots.take(5), pending = 291, progressDone = 121, progressTotal = 412, showBatteryTip = true), HomeActions())
    }

    @Test fun detailLight() = shoot("06_detail_light", ThemeMode.LIGHT) { DetailScreen(shots[0], DetailActions()) }

    @Test @Config(qualifiers = "w411dp-h1350dp-xxhdpi")
    fun detailFull() = shoot("07_detail_full_dark", ThemeMode.DARK) { DetailScreen(shots[0], DetailActions()) }

    @Test fun onboarding() = shoot("08_onboarding", ThemeMode.LIGHT) { OnboardingScreen(OnboardingStep.Welcome, OnboardingActions()) }

    @Test fun onboardingImport() = shoot("09_onboarding_import_dark", ThemeMode.DARK) {
        OnboardingScreen(OnboardingStep.Import(412), OnboardingActions())
    }

    @Test fun battery() = shoot("10_battery_guide", ThemeMode.LIGHT) { BatteryGuideScreen(false, PhoneBrand.SAMSUNG, BatteryActions()) }

    // Full-length battery guides, as each brand's phone would show them (tall canvas so nothing is cut off).
    @Test @Config(qualifiers = "w411dp-h1900dp-xxhdpi")
    fun batterySamsung() = shoot("13_battery_samsung", ThemeMode.LIGHT) { BatteryGuideScreen(false, PhoneBrand.SAMSUNG, BatteryActions()) }

    @Test @Config(qualifiers = "w411dp-h1900dp-xxhdpi")
    fun batteryXiaomi() = shoot("14_battery_xiaomi", ThemeMode.LIGHT) { BatteryGuideScreen(false, PhoneBrand.XIAOMI, BatteryActions()) }

    @Test @Config(qualifiers = "w411dp-h1900dp-xxhdpi")
    fun batteryTranssion() = shoot("15_battery_infinix_tecno", ThemeMode.LIGHT) { BatteryGuideScreen(false, PhoneBrand.TRANSSION, BatteryActions()) }

    @Test @Config(qualifiers = "w411dp-h1900dp-xxhdpi")
    fun batteryVivo() = shoot("16_battery_vivo", ThemeMode.LIGHT) { BatteryGuideScreen(true, PhoneBrand.VIVO, BatteryActions()) }

    // An Oppo phone with the Samsung guide opened under "Other phones", in dark mode.
    @Test @Config(qualifiers = "w411dp-h2800dp-xxhdpi")
    fun batteryOtherPhones() = shoot("17_battery_other_phones_dark", ThemeMode.DARK) {
        BatteryGuideScreen(false, PhoneBrand.OPPO, BatteryActions(), openBrand = PhoneBrand.SAMSUNG)
    }

    @Test @Config(qualifiers = "w200dp-h200dp-xxhdpi")
    fun appIcon() = shoot("12_app_icon", ThemeMode.LIGHT) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(200.dp).background(Palette.Yellow))
    }

    @Test fun settings() = shoot("11_settings_dark", ThemeMode.DARK) {
        SettingsScreen(SettingsState(theme = ThemeMode.DARK, total = 412, autoPickOn = true, batteryOk = true), SettingsActions())
    }
}
