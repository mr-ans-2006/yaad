package io.github.mrans2006.yaad.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import io.github.mrans2006.yaad.brain.Brain
import io.github.mrans2006.yaad.brain.TextReader
import io.github.mrans2006.yaad.core.SearchRanker
import io.github.mrans2006.yaad.core.Vectors
import io.github.mrans2006.yaad.pickup.Permissions
import io.github.mrans2006.yaad.pickup.ScreenshotFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The "library" is the one place that changes Yaad's data.
 * Screens and background jobs ask the library to do things; the library talks to the database.
 */
class Library(
    private val context: Context,
    private val db: YaadDatabase,
    private val prefs: Prefs,
    private val brain: Brain,
) {
    private val dao = db.shots()
    private val reader = TextReader(context)
    private val finder = ScreenshotFinder(context)

    /** Search keeps all vectors in memory; this forgets them whenever the table changes. */
    @Volatile private var docsCache: List<SearchRanker.Doc>? = null

    init {
        db.invalidationTracker.addObserver(object : androidx.room.InvalidationTracker.Observer("shots") {
            override fun onInvalidated(tables: Set<String>) { docsCache = null }
        })
    }

    fun observeAll() = dao.observeAll()
    fun observe(id: Long) = dao.observe(id)
    fun observePendingCount() = dao.observePendingCount()

    /** How many screenshots are on the phone right now (for the "import?" question). */
    suspend fun countScreenshotsOnPhone(): Int = withContext(Dispatchers.IO) {
        if (!Permissions.canReadScreenshots(context)) 0 else runCatching { finder.findAll().size }.getOrDefault(0)
    }

    /**
     * Step 1 of auto-pick: compare the gallery's Screenshots folders with our database.
     *  - New screenshots are added as PENDING (waiting to be read).
     *  - Screenshots you deleted from the gallery are removed from Yaad too.
     * Returns how many new ones were found.
     */
    suspend fun syncWithGallery(): Int = withContext(Dispatchers.IO) {
        if (!Permissions.canReadScreenshots(context)) return@withContext 0
        // Wait until you've answered "import all or only new ones?" on the first-launch screen.
        if (!prefs.onboarded.value) return@withContext 0
        val found = try { finder.findAll() } catch (e: Exception) {
            Log.w(TAG, "gallery query failed", e); return@withContext 0
        }
        val known = dao.knownMediaIds().toHashSet()
        val ignored = dao.ignoredMediaIds().toHashSet()
        val cutoff = prefs.onlyNewAfter

        val fresh = found.filter { it.mediaId !in known && it.mediaId !in ignored && it.addedAtSec >= cutoff }
        if (fresh.isNotEmpty()) {
            dao.insertAll(fresh.map {
                Shot(uri = it.uri, mediaId = it.mediaId, source = Shot.SOURCE_AUTO, displayName = it.name,
                    takenAt = it.takenAt, width = it.width, height = it.height)
            })
        }

        // Gone from the gallery? Then it's gone from Yaad too. (Safety: never do this on an empty answer.)
        if (found.isNotEmpty()) {
            val stillThere = found.map { it.mediaId }.toHashSet()
            val gone = known.filter { it !in stillThere }
            if (gone.isNotEmpty()) dao.deleteByMediaIds(gone)
        }
        prefs.lastScanAt = System.currentTimeMillis()
        fresh.size
    }

    /**
     * Step 2: read every PENDING screenshot (OCR -> key line -> tags -> vectors) and save the result.
     * [onProgress] is told (done, total) after each one. Stops early if [keepGoing] says so.
     */
    suspend fun processPending(
        keepGoing: () -> Boolean = { true },
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Int = withContext(Dispatchers.Default) {
        var done = 0
        val total = dao.pendingCount()
        while (keepGoing()) {
            val batch = dao.pending(limit = 20)
            if (batch.isEmpty()) break
            for (shot in batch) {
                if (!keepGoing()) break
                dao.update(process(shot))
                done++
                onProgress(done, maxOf(total, done))
            }
        }
        done
    }

    /** Reads ONE screenshot. Never throws: problems are saved as FAILED with the reason. */
    suspend fun process(shot: Shot): Shot = try {
        val lines = withContext(Dispatchers.IO) { reader.read(Uri.parse(shot.uri)) }
        val u = brain.understand(lines)
        shot.copy(
            status = Shot.STATUS_DONE,
            text = u.text,
            keyLine = u.keyLine.ifBlank { "No text found" },
            tags = u.tags.joinToString(","),
            links = u.links.joinToString("\n"),
            vectors = u.vectors?.let { Vectors.toBytes(it) },
            error = null,
        )
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) { // includes out-of-memory on giant images: mark it failed, don't crash
        Log.w(TAG, "could not read ${shot.uri}", e)
        shot.copy(status = Shot.STATUS_FAILED, error = e.message ?: e.javaClass.simpleName, keyLine = "Couldn't read this one")
    }

    /**
     * An image was shared to Yaad (or picked in the app). We COPY it into Yaad's private folder,
     * because the app that shared it may take its permission back later.
     */
    suspend fun importShared(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "shared").apply { mkdirs() }
        var count = 0
        for (uri in uris) {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
                val file = File(dir, UUID.randomUUID().toString() + "." + ext)
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    ?: continue
                dao.insert(Shot(uri = Uri.fromFile(file).toString(), source = Shot.SOURCE_SHARED,
                    displayName = file.name, takenAt = System.currentTimeMillis()))
                count++
            } catch (e: Exception) {
                Log.w(TAG, "could not import $uri", e)
            }
        }
        count
    }

    /** Removes a screenshot from Yaad only. The original stays in your gallery. */
    suspend fun delete(shot: Shot) = withContext(Dispatchers.IO) {
        shot.mediaId?.let { dao.ignore(Ignored(it)) } // so auto-pick won't bring it back
        if (shot.source == Shot.SOURCE_SHARED) Uri.parse(shot.uri).path?.let { File(it).delete() }
        dao.delete(shot.id)
    }

    suspend fun retryFailed() = dao.retryFailed()

    suspend fun pendingCount() = dao.pendingCount()

    /** Hybrid search: meaning + words. Returns screenshot ids, best first. */
    suspend fun search(query: String): List<SearchRanker.Hit> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        val docs = docsCache ?: dao.searchRows().map { r ->
            SearchRanker.Doc(r.id, r.text, r.tags.split(',').filter { it.isNotBlank() }, r.vectors?.let { Vectors.fromBytes(it) })
        }.also { docsCache = it }
        val queryVector = runCatching { brain.embedder().embed(query) }.getOrNull()
        SearchRanker.rank(query, queryVector, docs)
    }

    companion object { const val TAG = "Yaad" }
}
