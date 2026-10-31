package io.github.mrans2006.yaad

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.mrans2006.yaad.brain.Brain
import io.github.mrans2006.yaad.data.Library
import io.github.mrans2006.yaad.data.Prefs
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.data.YaadDatabase
import io.github.mrans2006.yaad.pickup.AutoPick
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A pretend gallery: a tiny SQLite table shaped like Android's MediaStore, so our REAL
 * query (the "LIKE %screenshot%" selection) runs against it.
 */
class FakeMediaStore : ContentProvider() {
    lateinit var db: SQLiteDatabase
    override fun onCreate(): Boolean {
        db = SQLiteDatabase.create(null)
        db.execSQL("CREATE TABLE images (_id INTEGER PRIMARY KEY, _display_name TEXT, datetaken INTEGER, date_added INTEGER, width INTEGER, height INTEGER, relative_path TEXT, bucket_display_name TEXT)")
        return true
    }
    fun add(id: Long, path: String, bucket: String, addedSec: Long) {
        db.execSQL("INSERT INTO images VALUES ($id, 'img$id.jpg', ${addedSec * 1000}, $addedSec, 1080, 2400, '$path', '$bucket')")
    }
    fun remove(id: Long) = db.execSQL("DELETE FROM images WHERE _id = $id")
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
        db.query("images", projection, selection, selectionArgs, null, null, sortOrder)
    override fun getType(uri: Uri): String = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = android.app.Application::class) // Android 13, like the Reno 6
class AutoPickTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var gallery: FakeMediaStore
    private lateinit var db: YaadDatabase
    private lateinit var prefs: Prefs
    private lateinit var library: Library

    @Before fun setUp() {
        gallery = Robolectric.setupContentProvider(FakeMediaStore::class.java, MediaStore.AUTHORITY)
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        db = Room.inMemoryDatabaseBuilder(context, YaadDatabase::class.java).allowMainThreadQueries().build()
        prefs = Prefs(context)
        prefs.setOnboarded()
        library = Library(context, db, prefs, Brain(context))

        gallery.add(1, "DCIM/Screenshots/", "Screenshots", 1_000)        // ColorOS location
        gallery.add(2, "Pictures/Screenshots/", "Screenshots", 2_000)    // the other common location
        gallery.add(3, "DCIM/Camera/", "Camera", 3_000)                  // a normal photo: must be skipped
        gallery.add(4, "Pictures/WhatsApp Images/", "WhatsApp Images", 4_000) // skipped too
        gallery.add(5, "Pictures/Screenshots/Chrome/", "Chrome", 5_000)  // sub-folder of Screenshots
    }

    @After fun tearDown() = db.close()

    private fun shots() = runBlocking { db.shots().observeAll().first() }

    @Test fun picksOnlyScreenshots() = runBlocking {
        assertEquals(3, library.syncWithGallery())
        assertEquals(setOf(1L, 2L, 5L), shots().map { it.mediaId }.toSet())
        assertTrue(shots().all { it.status == Shot.STATUS_PENDING && it.source == Shot.SOURCE_AUTO })
    }

    @Test fun secondScanAddsOnlyNewOnes() = runBlocking {
        library.syncWithGallery()
        gallery.add(6, "DCIM/Screenshots/", "Screenshots", 6_000)
        assertEquals(1, library.syncWithGallery())
        assertEquals(0, library.syncWithGallery())
        assertEquals(4, shots().size)
    }

    @Test fun deletedFromGalleryMeansGoneFromYaad() = runBlocking {
        library.syncWithGallery()
        gallery.remove(2)
        library.syncWithGallery()
        assertEquals(setOf(1L, 5L), shots().map { it.mediaId }.toSet())
    }

    @Test fun removedInYaadStaysRemoved() = runBlocking {
        library.syncWithGallery()
        library.delete(shots().first { it.mediaId == 1L })
        library.syncWithGallery()
        assertEquals(setOf(2L, 5L), shots().map { it.mediaId }.toSet())
    }

    @Test fun onlyNewOnesRespectsTheCutoff() = runBlocking {
        prefs.onlyNewAfter = 1_500 // seconds: screenshot 1 (1000) is older, so it's skipped
        assertEquals(2, library.syncWithGallery())
        assertEquals(setOf(2L, 5L), shots().map { it.mediaId }.toSet())
        prefs.onlyNewAfter = 0
    }

    @Test fun nothingIsImportedBeforeYouChoose() = runBlocking {
        context.getSharedPreferences("yaad", Context.MODE_PRIVATE).edit().clear().commit()
        val fresh = Library(context, db, Prefs(context), Brain(context))
        assertEquals(0, fresh.syncWithGallery())
        assertEquals(0, shots().size)
    }

    @Test fun noPermissionMeansNoCrash() = runBlocking {
        shadowOf(context as android.app.Application).denyPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        assertEquals(0, library.syncWithGallery())
    }

    @Test fun unreadableImageIsMarkedFailedNotCrashed() = runBlocking {
        library.syncWithGallery()
        // There's no real image behind these fake entries, so reading must fail gracefully.
        library.processPending()
        assertTrue(shots().all { it.status == Shot.STATUS_FAILED })
        assertEquals(0, library.pendingCount())
    }

    @Test fun backgroundJobsAreScheduled() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        AutoPick.schedule(context)
        val wm = WorkManager.getInstance(context)
        val watch = wm.getWorkInfosForUniqueWork("yaad-watch").get()
        val hourly = wm.getWorkInfosForUniqueWork("yaad-hourly").get()
        assertEquals(WorkInfo.State.ENQUEUED, watch.single().state)
        assertEquals(WorkInfo.State.ENQUEUED, hourly.single().state)
    }
}
