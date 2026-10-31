package io.github.mrans2006.yaad

import android.app.Application
import io.github.mrans2006.yaad.brain.Brain
import io.github.mrans2006.yaad.data.Library
import io.github.mrans2006.yaad.data.Prefs
import io.github.mrans2006.yaad.data.YaadDatabase
import io.github.mrans2006.yaad.pickup.AutoPick

/**
 * The Application object is created once, before any screen, when Yaad starts.
 * It holds the shared pieces (database, settings, brain, library) so every screen uses the same ones.
 */
class YaadApp : Application() {
    val prefs by lazy { Prefs(this) }
    val database by lazy { YaadDatabase.create(this) }
    val brain by lazy { Brain(this) }
    val library by lazy { Library(this, database, prefs, brain) }

    override fun onCreate() {
        super.onCreate()
        AutoPick.schedule(this)
        AutoPick.listen(this)
    }
}
