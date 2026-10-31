package io.github.mrans2006.yaad.battery

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.mrans2006.yaad.core.GuideButton
import io.github.mrans2006.yaad.core.PhoneBrand
import io.github.mrans2006.yaad.core.SettingsTarget

/** Which phone family this phone belongs to (Samsung, Xiaomi, …). Worked out once. */
object ThisPhone {
    val brand: PhoneBrand by lazy { PhoneBrand.detect(Build.MANUFACTURER, Build.BRAND) }
}

/**
 * Opens Settings screens for the battery guide.
 *
 * Each guide button carries a list of places to try (see core/BatteryGuides.kt).
 * We try them one by one. If a screen doesn't exist on this phone, Android throws
 * an error; we catch it and simply try the next one. The last places in every list
 * are standard Android screens, so something always opens and the app never crashes.
 */
object SettingsDoors {

    /**
     * Tries each place in order. Returns which one opened: 0 = the first (best) one,
     * 1 or more = a backup screen, -1 = nothing could be opened at all.
     */
    fun open(activity: Activity, button: GuideButton): Int {
        button.targets.forEachIndexed { i, target ->
            if (tryOpen(activity, target)) return i
        }
        return -1
    }

    fun tryOpen(activity: Activity, target: SettingsTarget): Boolean {
        val intent = toIntent(target, activity.packageName) ?: return false
        return try {
            activity.startActivity(intent)
            true
        } catch (_: Exception) {
            // ActivityNotFoundException (screen doesn't exist on this phone) or
            // SecurityException (screen exists but is private). Either way: try the next one.
            false
        }
    }

    /** Turns our plain-text description into a real Android Intent. */
    fun toIntent(target: SettingsTarget, pkg: String): Intent? {
        val intent = when {
            target.isComponent -> Intent().setComponent(ComponentName(target.packageName!!, target.className!!))
            target.action != null -> Intent(target.action)
            else -> return null
        }
        if (target.withAppPackage) intent.data = Uri.parse("package:$pkg")
        for ((key, value) in target.extras) {
            intent.putExtra(key, value.replace("{pkg}", pkg).replace("{label}", "Yaad"))
        }
        return intent
    }
}
