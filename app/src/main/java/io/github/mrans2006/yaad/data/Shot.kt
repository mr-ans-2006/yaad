package io.github.mrans2006.yaad.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One remembered screenshot = one row in the database.
 *
 * Room turns this Kotlin class into a database table called "shots".
 * Each property becomes a column.
 */
@Entity(tableName = "shots", indices = [Index(value = ["mediaId"], unique = true), Index("status")])
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Where the image lives: a gallery link (content://...) or a file Yaad copied (file://...). */
    val uri: String,
    /** The gallery's own id for this image. Null for images that were shared into Yaad. */
    val mediaId: Long? = null,
    /** "auto" = picked up from the Screenshots folder, "shared" = shared/picked into Yaad. */
    val source: String,
    val displayName: String = "",
    /** When the screenshot was taken, in milliseconds since 1970. */
    val takenAt: Long,
    val width: Int = 0,
    val height: Int = 0,
    /** PENDING (waiting to be read), DONE, or FAILED. */
    val status: Int = STATUS_PENDING,
    /** All the text OCR found, line by line. */
    val text: String = "",
    /** The one line that sums it up (our v1 "summary"). */
    val keyLine: String = "",
    /** Tag labels, comma separated: "Code,Tools". */
    val tags: String = "",
    /** Links found in the text, one per line. */
    val links: String = "",
    /** The meaning-vectors (embeddings), stored as raw bytes. */
    val vectors: ByteArray? = null,
    val error: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
) {
    val tagList: List<String> get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val linkList: List<String> get() = links.lines().filter { it.isNotBlank() }

    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DONE = 1
        const val STATUS_FAILED = 2
        const val SOURCE_AUTO = "auto"
        const val SOURCE_SHARED = "shared"
    }

    // ByteArray needs these so two Shots compare by content (Kotlin rule for arrays in data classes).
    override fun equals(other: Any?) = other is Shot && other.id == id && other.status == status &&
        other.keyLine == keyLine && other.tags == tags && other.uri == uri
    override fun hashCode() = (id * 31 + status).hashCode()
}

/** Screenshots you deleted from Yaad. We remember their gallery id so auto-pick won't add them back. */
@Entity(tableName = "ignored")
data class Ignored(@PrimaryKey val mediaId: Long)

/** A light version of [Shot] for the search ranker (no thumbnails needed, just text + vectors). */
data class SearchRow(val id: Long, val text: String, val tags: String, val vectors: ByteArray?)
