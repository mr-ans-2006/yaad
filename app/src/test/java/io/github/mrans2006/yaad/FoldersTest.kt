package io.github.mrans2006.yaad

import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.Folder
import io.github.mrans2006.yaad.ui.Folders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How the Folders tab sorts screenshots (plain JVM test, no phone needed). */
class FoldersTest {
    private val day = 86_400_000L
    private val now = 1_760_000_000_000L

    private fun shot(id: Long, tags: String, text: String = "some text", status: Int = Shot.STATUS_DONE, ago: Long = id * day) =
        Shot(id = id, uri = "x://$id", source = Shot.SOURCE_AUTO, takenAt = now - ago, status = status, text = text, tags = tags)

    private val shots = listOf(
        shot(1, "Chats,Study"),
        shot(2, "Code"),
        shot(3, ""),                                      // read, but no tag fitted
        shot(4, "", text = ""),                           // nothing readable (e.g. Urdu script)
        shot(5, "", text = "", status = Shot.STATUS_FAILED),
        shot(6, "", text = "", status = Shot.STATUS_PENDING),
        shot(7, "Code"),
    )

    @Test fun allFolderComesFirstAndHasEverything() {
        val all = Folders.build(shots).first()
        assertEquals(Folder.ALL, all.key)
        assertEquals(7, all.count)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L), all.shots.map { it.id }) // newest first
    }

    @Test fun tagFoldersFollowCoreOrderAndMultiTagShotsAppearInEach() {
        val keys = Folders.build(shots).map { it.key }
        assertEquals(listOf("all", "tag:Code", "tag:Study", "tag:Chats", "untagged", "notext"), keys)
        assertEquals(listOf(1L), Folders.find(shots, "tag:Chats")!!.shots.map { it.id })
        assertEquals(listOf(1L), Folders.find(shots, "tag:Study")!!.shots.map { it.id })
        assertEquals(listOf(2L, 7L), Folders.find(shots, "tag:Code")!!.shots.map { it.id })
    }

    @Test fun nothingIsHidden() {
        assertEquals(listOf(3L), Folders.find(shots, Folder.UNTAGGED)!!.shots.map { it.id })
        assertEquals(listOf(4L, 5L), Folders.find(shots, Folder.NO_TEXT)!!.shots.map { it.id })
        // Still reading: only in "All" until tags arrive.
        val inSomeFolder = Folders.build(shots).drop(1).flatMap { it.shots }.map { it.id }.toSet()
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L, 7L), inSomeFolder)
    }

    @Test fun emptyFoldersAreLeftOut() {
        val keys = Folders.build(listOf(shot(1, "Code"))).map { it.key }
        assertEquals(listOf("all", "tag:Code"), keys)
        assertNull(Folders.find(listOf(shot(1, "Code")), Folder.NO_TEXT))
    }

    @Test fun dateHeaders() {
        val groups = Folders.byDate(listOf(shot(1, "", ago = 0), shot(2, "", ago = day + 1000), shot(3, "", ago = 3 * day), shot(4, "", ago = 400 * day)), now)
        assertEquals(listOf("Today", "Yesterday", "This week"), groups.map { it.first }.take(3))
        assertEquals(4, groups.size)
    }
}
