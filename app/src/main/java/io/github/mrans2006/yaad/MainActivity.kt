package io.github.mrans2006.yaad

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.pickup.AutoPick
import io.github.mrans2006.yaad.pickup.Permissions
import io.github.mrans2006.yaad.ui.YaadViewModel
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
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadTheme
import java.io.File

/** The one Activity (window) of Yaad. Every screen is drawn inside it with Compose. */
class MainActivity : ComponentActivity() {
    private val vm: YaadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by vm.theme.collectAsStateWithLifecycle()
            val onboarded by vm.prefs.onboarded.collectAsStateWithLifecycle()
            YaadTheme(theme) {
                val dark = Yaad.colors.isDark
                LaunchedEffect(dark) {
                    val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    enableEdgeToEdge(style, style)
                }
                Box(Modifier.fillMaxSize().background(Yaad.colors.background)) {
                    if (onboarded) YaadNavigation() else Onboarding()
                }
            }
        }
    }

    // ---------------- First launch ----------------

    @Composable
    private fun Onboarding() {
        var step by remember { mutableStateOf<OnboardingStep>(OnboardingStep.Welcome) }
        val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            step = if (granted) OnboardingStep.Import(null) else OnboardingStep.NoPermission
            vm.onPermissionResult()
        }
        LaunchedEffect(step) {
            if (step == OnboardingStep.Import(null)) step = OnboardingStep.Import(vm.countScreenshotsOnPhone())
        }
        OnboardingScreen(
            step,
            OnboardingActions(
                onStart = {
                    if (Permissions.canReadScreenshots(this)) step = OnboardingStep.Import(null)
                    else askPermission.launch(Permissions.readImages)
                },
                onImportAll = { vm.finishOnboarding(importExisting = true) },
                onOnlyNew = { vm.finishOnboarding(importExisting = false) },
            ),
        )
    }

    // ---------------- The app's screens ----------------

    @Composable
    private fun YaadNavigation() {
        val nav = rememberNavController()
        val state by vm.state.collectAsStateWithLifecycle()
        val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
            vm.importPicked(uris)
            if (uris.isNotEmpty()) toast(if (uris.size == 1) "Saved to Yaad" else "Saved ${uris.size} images to Yaad")
        }
        val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.onPermissionResult()
            if (!granted) openAppInfo()
        }
        // Every time Yaad comes to the front: catch up on screenshots we missed.
        LifecycleResumeEffect(Unit) {
            vm.onResume()
            onPauseOrDispose { }
        }

        NavHost(nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    state,
                    HomeActions(
                        onQueryChange = vm::onQueryChange,
                        onTagSelected = vm::onTagSelected,
                        onOpen = { nav.navigate("shot/${it.id}") },
                        onAdd = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onSettings = { nav.navigate("settings") },
                        onAskPermission = { askPermission.launch(Permissions.readImages) },
                        onBatteryGuide = { nav.navigate("battery") },
                        onDismissBatteryTip = vm::dismissBatteryTip,
                    ),
                )
            }
            composable("shot/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("id")
                val shot by remember(id) { vm.observeShot(id) }.collectAsStateWithLifecycle(initialValue = null)
                shot?.let {
                    DetailScreen(
                        it,
                        DetailActions(
                            onBack = { nav.popBackStack() },
                            onOpenLink = ::openLink,
                            onOpenInGallery = ::openInGallery,
                            onDelete = { s -> vm.delete(s); nav.popBackStack(); toast("Removed from Yaad") },
                            onRetry = vm::retryFailed,
                        ),
                    )
                }
            }
            composable("settings") {
                val theme by vm.theme.collectAsStateWithLifecycle()
                SettingsScreen(
                    SettingsState(
                        theme = theme,
                        total = state.shots.size,
                        failed = state.shots.count { it.status == Shot.STATUS_FAILED },
                        autoPickOn = state.hasPermission,
                        batteryOk = Permissions.isBatteryUnrestricted(this@MainActivity),
                        version = BuildConfigVersion.name(this@MainActivity),
                    ),
                    SettingsActions(
                        onBack = { nav.popBackStack() },
                        onTheme = vm::setTheme,
                        onBatteryGuide = { nav.navigate("battery") },
                        onScanNow = { AutoPick.scanNow(this@MainActivity); toast("Checking for new screenshots…") },
                        onRetryFailed = vm::retryFailed,
                    ),
                )
            }
            composable("battery") {
                var batteryOk by remember { mutableStateOf(Permissions.isBatteryUnrestricted(this@MainActivity)) }
                LifecycleResumeEffect(Unit) {
                    batteryOk = Permissions.isBatteryUnrestricted(this@MainActivity)
                    onPauseOrDispose { }
                }
                BatteryGuideScreen(
                    batteryOk,
                    BatteryActions(
                        onBack = { nav.popBackStack() },
                        onOpenBatterySettings = ::openBatterySettings,
                        onOpenAppInfo = ::openAppInfo,
                        onOpenAutoLaunch = ::openAutoLaunch,
                    ),
                )
            }
        }
    }

    // ---------------- Doors to other apps ----------------

    private fun openLink(url: String) = tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(url))) { toast("No browser found") }

    private fun openInGallery(shot: Shot) {
        val uri = Uri.parse(shot.uri)
        val shareable = if (uri.scheme == "file") {
            FileProvider.getUriForFile(this, "$packageName.files", File(uri.path!!))
        } else uri
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(shareable, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        tryStart(intent) { toast("No gallery app found") }
    }

    private fun openAppInfo() =
        tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) {}

    private fun openBatterySettings() =
        tryStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) { openAppInfo() }

    /** ColorOS hides its "auto launch" page in different places on different versions. Try them all. */
    private fun openAutoLaunch() {
        val candidates = listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        )
        for (c in candidates) {
            try { startActivity(Intent().setComponent(c)); return } catch (_: Exception) { }
        }
        toast("Couldn't open it directly. Opening Yaad's app info instead.")
        openAppInfo()
    }

    private fun tryStart(intent: Intent, otherwise: () -> Unit) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { otherwise() } catch (_: SecurityException) { otherwise() }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

/** Reads the version name ("1.0") from the installed app. */
object BuildConfigVersion {
    fun name(activity: ComponentActivity): String =
        runCatching { activity.packageManager.getPackageInfo(activity.packageName, 0).versionName }.getOrNull() ?: "1.0"
}
