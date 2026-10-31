package io.github.mrans2006.yaad.pickup

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.mrans2006.yaad.YaadApp
import java.time.Duration

/**
 * Auto-pick: how Yaad notices new screenshots. There are FOUR safety nets, because
 * Oppo's ColorOS (and other phones) love to kill apps in the background:
 *
 *  1. While Yaad is running, a ContentObserver hears "the gallery changed" instantly.
 *  2. A WorkManager job with a "content trigger" wakes up when the gallery changes,
 *     even if Yaad is closed (as long as the phone lets it).
 *  3. A periodic job checks every hour, just in case.
 *  4. Every time you open Yaad, it catches up on anything it missed.
 */
object AutoPick {
    private const val SCAN = "yaad-scan"
    private const val WATCH = "yaad-watch"
    private const val HOURLY = "yaad-hourly"

    /** Run a scan now (or soon). If one is already running, it will pick the new ones up. */
    fun scanNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ScanWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(SCAN, ExistingWorkPolicy.KEEP, request)
    }

    /** Queue another scan after the current one (used when a big import needs more time). */
    internal fun scanAgainAfter(context: Context) {
        val request = OneTimeWorkRequestBuilder<ScanWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(SCAN, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Safety nets 2 and 3. Called when the app starts. */
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(WATCH, ExistingWorkPolicy.KEEP, watchRequest())
        wm.enqueueUniquePeriodicWork(
            HOURLY, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScanWorker>(Duration.ofHours(1)).build(),
        )
    }

    internal fun rearmWatcher(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(WATCH, ExistingWorkPolicy.REPLACE, watchRequest())
    }

    private fun watchRequest() = OneTimeWorkRequestBuilder<GalleryChangedWorker>()
        .setConstraints(
            Constraints.Builder()
                .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                .setTriggerContentUpdateDelay(Duration.ofSeconds(2))
                .setTriggerContentMaxDelay(Duration.ofSeconds(10))
                .build(),
        )
        .build()

    /** Safety net 1: listens while the app's process is alive. */
    fun listen(context: Context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = scanNow(context)
        }
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
    }
}

/** Woken up by Android when the gallery changes. It starts a scan and then waits again. */
class GalleryChangedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AutoPick.scanNow(applicationContext)
        AutoPick.rearmWatcher(applicationContext)
        return Result.success()
    }
}

/**
 * The worker that does the real job: find new screenshots, then read them one by one.
 * It reports progress so the home screen can show "Reading 12 of 340".
 */
class ScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val library = (applicationContext as YaadApp).library
        val started = System.currentTimeMillis()
        // Android stops background jobs after ~10 minutes, so we stop at 8 and continue in a new job.
        val keepGoing = { !isStopped && System.currentTimeMillis() - started < 8 * 60_000 }

        library.syncWithGallery()
        while (true) {
            library.processPending(keepGoing) { done, total -> setProgress(workDataOf("done" to done, "total" to total)) }
            // New screenshots may have arrived while we were reading. If so, go round again.
            if (!keepGoing() || library.syncWithGallery() == 0) break
        }

        if (!isStopped && library.pendingCount() > 0) AutoPick.scanAgainAfter(applicationContext)
        return Result.success()
    }
}
