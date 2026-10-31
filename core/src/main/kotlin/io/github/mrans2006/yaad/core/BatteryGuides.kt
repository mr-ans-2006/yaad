package io.github.mrans2006.yaad.core

/**
 * One place a button can try to open in the phone's Settings.
 *
 * Either a specific screen inside another app ([packageName] + [className], called
 * a "component"), or a standard Android Settings [action]. These are only *descriptions*:
 * the app turns them into real Android Intents (see app/.../battery/SettingsDoors.kt).
 * Keeping them as plain text here means we can test them on a normal computer.
 */
data class SettingsTarget(
    val packageName: String? = null,
    val className: String? = null,
    val action: String? = null,
    /** Add "package:io.github...yaad" as the data, so the screen opens on Yaad itself. */
    val withAppPackage: Boolean = false,
    /** Extra values to pass. "{pkg}" becomes Yaad's package name and "{label}" becomes "Yaad". */
    val extras: Map<String, String> = emptyMap(),
) {
    val isComponent get() = packageName != null && className != null

    companion object {
        fun component(pkg: String, cls: String, extras: Map<String, String> = emptyMap()) =
            SettingsTarget(packageName = pkg, className = cls, extras = extras)

        // Standard Android screens. These exist on every phone, so they are the safety net.
        /** The list where you can turn battery optimisation off for an app. Needs no permission. */
        val BATTERY_OPTIMISATION_LIST = SettingsTarget(action = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
        /** Yaad's own "App info" page. */
        val APP_INFO = SettingsTarget(action = "android.settings.APPLICATION_DETAILS_SETTINGS", withAppPackage = true)
    }
}

/** A button on a guide step. Yaad tries each target in order and opens the first one that works. */
data class GuideButton(val label: String, val targets: List<SettingsTarget>)

/** One numbered step: a short title, plain-English instructions and optional buttons. */
data class GuideStep(val title: String, val body: String, val buttons: List<GuideButton> = emptyList())

/** Everything Yaad shows for one phone family. */
data class BatteryGuide(val brand: PhoneBrand, val intro: String, val steps: List<GuideStep>)

/**
 * The battery guides, one per phone family.
 *
 * The steps are written in Yaad's own words, based on the guides at
 * https://dontkillmyapp.com (a great free site with steps for almost every phone).
 * Phone makers move these settings around between versions, so each step mentions
 * the most common places. The screen names inside other apps (the components) come
 * from what other open-source apps use; none of them is official, which is why every
 * button ends with standard Android screens that always exist.
 */
object BatteryGuides {
    private const val PKG = "{pkg}"

    // ---- Safety nets added to the end of every button ----
    private val thenBattery = listOf(SettingsTarget.BATTERY_OPTIMISATION_LIST, SettingsTarget.APP_INFO)
    private val thenAppInfo = listOf(SettingsTarget.APP_INFO, SettingsTarget.BATTERY_OPTIMISATION_LIST)

    private val appInfoButton = GuideButton("Open Yaad's app info", thenAppInfo)
    private val batteryListButton = GuideButton("Open battery optimisation", thenBattery)

    private const val RECENTS_TAIL = "Clearing your recent apps then won't close Yaad."

    // ---------------- Samsung (One UI) ----------------
    val samsung = BatteryGuide(
        PhoneBrand.SAMSUNG,
        "Samsung puts apps to \u201Csleep\u201D after a few days and can stop them in the background. Two changes fix it.",
        listOf(
            GuideStep(
                "Set battery to Unrestricted",
                "Open Yaad's app info → Battery → choose Unrestricted.\n" +
                    "Older phones: App info → Battery → Optimise battery usage → All apps → turn Yaad off.",
                listOf(appInfoButton),
            ),
            GuideStep(
                "Take Yaad out of Sleeping apps",
                "Settings → Battery (or Device care → Battery) → Background usage limits.\n" +
                    "If Yaad is in Sleeping apps or Deep sleeping apps, remove it. Then add Yaad to Never sleeping apps.\n" +
                    "Also turn off \u201CPut unused apps to sleep\u201D, or Samsung may put Yaad back to sleep later.",
                listOf(
                    GuideButton(
                        "Open battery settings",
                        listOf(
                            SettingsTarget.component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                            SettingsTarget.component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                            SettingsTarget.component("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                        ) + thenBattery,
                    ),
                ),
            ),
            GuideStep(
                "Keep Yaad open in Recents",
                "Open your recent apps, tap Yaad's icon at the top of its card and choose \u201CKeep open\u201D. " +
                    "Newer One UI versions don't have this option, and that's fine.",
            ),
        ),
    )

    // ---------------- Xiaomi / Redmi / POCO (MIUI / HyperOS) ----------------
    val xiaomi = BatteryGuide(
        PhoneBrand.XIAOMI,
        "Xiaomi phones block apps from starting in the background unless you allow it.",
        listOf(
            GuideStep(
                "Turn on Autostart",
                "Open Yaad's app info → turn on Autostart.\n" +
                    "On some versions it's in Security app → Permissions → Autostart, or App info → App permissions → Background autostart.",
                listOf(
                    GuideButton(
                        "Open Autostart",
                        listOf(
                            SettingsTarget.component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                            SettingsTarget(action = "miui.intent.action.APP_PERM_EDITOR", extras = mapOf("extra_pkgname" to PKG)),
                        ) + thenAppInfo,
                    ),
                ),
            ),
            GuideStep(
                "Battery saver: No restrictions",
                "Open Yaad's app info → Battery saver → choose No restrictions.",
                listOf(
                    GuideButton(
                        "Open Yaad's battery saver",
                        listOf(
                            SettingsTarget.component(
                                "com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                                mapOf("package_name" to PKG, "package_label" to "{label}"),
                            ),
                        ) + thenAppInfo,
                    ),
                ),
            ),
            GuideStep(
                "Lock Yaad in Recents",
                "Open your recent apps and pull Yaad's card down (or long-press it and tap the lock). A small lock appears. $RECENTS_TAIL",
            ),
        ),
    )

    // ---------------- Vivo / iQOO (Funtouch OS / OriginOS) ----------------
    val vivo = BatteryGuide(
        PhoneBrand.VIVO,
        "Vivo stops apps that start or use power in the background unless you allow them.",
        listOf(
            GuideStep(
                "Allow Auto-start",
                "Settings → Apps (or More settings → Applications) → Autostart → turn on Yaad.\n" +
                    "Older phones: i Manager → App manager → Autostart manager.",
                listOf(
                    GuideButton(
                        "Open Auto-start",
                        listOf(
                            SettingsTarget.component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                            SettingsTarget.component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                            SettingsTarget.component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                        ) + thenAppInfo,
                    ),
                ),
            ),
            GuideStep(
                "Allow background power use",
                "Settings → Battery → Background power consumption management (or High background power consumption) → allow Yaad.\n" +
                    "Then App info → Battery → choose Unrestricted or Don't optimise, if your phone shows it.",
                listOf(
                    GuideButton(
                        "Open background power",
                        listOf(
                            SettingsTarget.component("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"),
                        ) + thenBattery,
                    ),
                    appInfoButton,
                ),
            ),
            GuideStep(
                "Lock Yaad in Recents",
                "Open your recent apps, then swipe Yaad's card down or tap its menu and choose the lock. $RECENTS_TAIL",
            ),
        ),
    )

    // ---------------- Infinix / Tecno / itel (XOS / HiOS) ----------------
    val transsion = BatteryGuide(
        PhoneBrand.TRANSSION,
        "Infinix and Tecno phones use the Phone Master app to stop background apps.",
        listOf(
            GuideStep(
                "Turn on Auto-start",
                "Open Phone Master → Toolbox → Auto-start management (called Launch manager on some phones) → allow Yaad. " +
                    "If there's a second switch to run in the background, turn that on too.",
                listOf(
                    GuideButton(
                        "Open Auto-start",
                        listOf(
                            SettingsTarget.component("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
                        ) + thenAppInfo,
                    ),
                ),
            ),
            GuideStep(
                "Turn off power saving for Yaad",
                "Settings → Battery (Battery Lab on some phones) → Battery saving settings → Power saving management for apps → turn it off for Yaad.\n" +
                    "Then App info → Battery → choose Unrestricted or Don't optimise, if your phone shows it.",
                listOf(batteryListButton, appInfoButton),
            ),
            GuideStep(
                "Lock Yaad in Recents",
                "Open your recent apps, tap the small arrow or menu on Yaad's card and choose the lock. $RECENTS_TAIL",
            ),
        ),
    )

    // ---------------- Oppo / Realme / OnePlus (ColorOS / OxygenOS) ----------------
    val oppo = BatteryGuide(
        PhoneBrand.OPPO,
        "ColorOS closes background apps hard. These three switches let Yaad notice screenshots while it's closed.",
        listOf(
            GuideStep(
                "Allow background activity",
                "Open Yaad's app info → Battery usage → turn on \u201CAllow background activity\u201D.\n" +
                    "Older ColorOS: Settings → Battery → More settings → Optimise battery use → All apps → Yaad → Don't optimise.",
                listOf(
                    appInfoButton,
                    GuideButton(
                        "Open battery settings",
                        listOf(
                            SettingsTarget.component("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
                        ) + thenBattery,
                    ),
                ),
            ),
            GuideStep(
                "Allow auto launch",
                "Settings → Apps → Auto launch → turn on Yaad. On some phones it's App info → \u201CAllow auto launch\u201D (or \u201CAuto start-up\u201D).",
                listOf(
                    GuideButton(
                        "Open auto launch",
                        listOf(
                            SettingsTarget.component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                            SettingsTarget.component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                            SettingsTarget.component("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
                            SettingsTarget.component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                            SettingsTarget.component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                        ) + thenAppInfo,
                    ),
                ),
            ),
            GuideStep(
                "Lock Yaad in Recents",
                "Open your recent apps, tap the ⋮ on Yaad's card (or pull it down) and choose Lock. $RECENTS_TAIL",
            ),
        ),
    )

    // ---------------- Pixel and other Android phones ----------------
    val generic = BatteryGuide(
        PhoneBrand.GENERIC,
        "Most phones only need one change: let Yaad use the battery in the background.",
        listOf(
            GuideStep(
                "Set battery to Unrestricted",
                "Open Yaad's app info → App battery usage (or Battery) → choose Unrestricted.\n" +
                    "Older Android: Settings → Apps → Special app access → Battery optimisation → All apps → Yaad → Don't optimise.",
                listOf(appInfoButton, batteryListButton),
            ),
            GuideStep(
                "Don't pause Yaad when unused",
                "In Yaad's app info, turn off \u201CPause app activity if unused\u201D (sometimes called \u201CRemove permissions if app is unused\u201D).",
                listOf(appInfoButton),
            ),
        ),
    )

    /** All guides, in the order they appear under "Other phones". */
    val all: List<BatteryGuide> = listOf(samsung, xiaomi, vivo, transsion, oppo, generic)

    fun forBrand(brand: PhoneBrand): BatteryGuide = all.first { it.brand == brand }

    /** The guide for this phone first, then every other guide. */
    fun ordered(mine: PhoneBrand): List<BatteryGuide> = listOf(forBrand(mine)) + all.filter { it.brand != mine }

    const val MORE_HELP_URL = "https://dontkillmyapp.com"
}
