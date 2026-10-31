package io.github.mrans2006.yaad.pickup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/** Which permission lets us see the screenshots depends on the Android version. */
object Permissions {
    val readImages: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun canReadScreenshots(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, readImages) == PackageManager.PERMISSION_GRANTED

    /** True when the phone will NOT put Yaad to sleep to save battery. */
    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
