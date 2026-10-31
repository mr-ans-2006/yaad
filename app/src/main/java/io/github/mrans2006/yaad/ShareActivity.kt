package io.github.mrans2006.yaad

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.mrans2006.yaad.pickup.AutoPick
import kotlinx.coroutines.launch

/**
 * Shows up in Android's share sheet as "Save to Yaad".
 * It has no screen of its own: it copies the image(s), says "Saved", and closes.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = sharedImages(intent)
        if (uris.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        lifecycleScope.launch {
            val app = application as YaadApp
            val saved = app.library.importShared(uris)
            AutoPick.scanNow(app) // reads the new images in the background
            val msg = if (saved == 1) "Saved to Yaad" else "Saved $saved images to Yaad"
            Toast.makeText(this@ShareActivity, if (saved == 0) "Couldn't save that image" else msg, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun sharedImages(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else intent.getParcelableExtra(Intent.EXTRA_STREAM),
        )
        Intent.ACTION_SEND_MULTIPLE ->
            (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)).orEmpty()
        else -> emptyList()
    }
}
