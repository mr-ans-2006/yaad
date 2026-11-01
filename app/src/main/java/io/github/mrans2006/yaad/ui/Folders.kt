package io.github.mrans2006.yaad.ui

import io.github.mrans2006.yaad.core.Tag
import io.github.mrans2006.yaad.data.Shot
import java.util.Calendar

/**
 * A folder on the Folders tab. Folders are not stored anywhere: they are worked out from the
 * auto-tags every time the list changes, so they are always up to date.
 *
 *  - "All": every screenshot.
 *  - One folder per auto-tag (Code, Tools, Places, Study, Hacks, Shopping, Quotes, Chats), only if it has screenshots.
 *    A screenshot with two tags (e.g. "Chats,Study") sits in both folders.
 *  - "Untagged": text was read, but no tag fitted.
 *  - "No text": nothing readable was found (e.g. photos, Urdu script), or reading failed.
 * Screenshots still being read show up in "All" only, until they get their tags.
 */
data class Folder(val key: String, val name: String, val tag: Tag?, val shots: List<Shot>) {
    val count get() = shots.size

    companion object {
        const val ALL = "all"
        const val UNTAGGED = "untagged"
        const val NO_TEXT = "notext"
        fun tagKey(label: String) = "tag:$label"
    }
}

object Folders {
    /** Newest first. */
    private fun newest(list: List<Shot>) = list.sortedByDescending { it.takenAt }

    private fun hasNoText(s: Shot) = s.status == Shot.STATUS_FAILED || (s.status == Shot.STATUS_DONE && s.text.isBlank())
    private fun isUntagged(s: Shot) = s.status == Shot.STATUS_DONE && s.text.isNotBlank() && s.tagList.isEmpty()

    fun build(shots: List<Shot>): List<Folder> {
        val all = newest(shots)
        val folders = mutableListOf(Folder(Folder.ALL, "All", null, all))
        // Known tags first, in the order core/ defines them; any other label after, A–Z.
        val labels = all.flatMap { it.tagList }.distinct()
        val known = Tag.entries.filter { t -> labels.any { it.equals(t.label, ignoreCase = true) } }
        val other = labels.filter { Tag.fromLabel(it) == null }.sortedBy { it.lowercase() }
        known.forEach { tag ->
            folders += Folder(Folder.tagKey(tag.label), tag.label, tag, all.filter { s -> s.tagList.any { it.equals(tag.label, ignoreCase = true) } })
        }
        other.forEach { label -> folders += Folder(Folder.tagKey(label), label, null, all.filter { label in it.tagList }) }
        all.filter(::isUntagged).takeIf { it.isNotEmpty() }?.let { folders += Folder(Folder.UNTAGGED, "Untagged", null, it) }
        all.filter(::hasNoText).takeIf { it.isNotEmpty() }?.let { folders += Folder(Folder.NO_TEXT, "No text", null, it) }
        return folders
    }

    fun find(shots: List<Shot>, key: String): Folder? = build(shots).firstOrNull { it.key == key }

    /** Groups a folder's screenshots under date headers: Today, Yesterday, This week, then month names. */
    fun byDate(shots: List<Shot>, now: Long = System.currentTimeMillis()): List<Pair<String, List<Shot>>> {
        val cal = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val today = cal.timeInMillis
        val yesterday = today - 86_400_000L
        val week = today - 6 * 86_400_000L
        val thisYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        fun header(t: Long): String = when {
            t >= today -> "Today"
            t >= yesterday -> "Yesterday"
            t >= week -> "This week"
            else -> {
                val c = Calendar.getInstance().apply { timeInMillis = t }
                val m = months[c.get(Calendar.MONTH)]
                if (c.get(Calendar.YEAR) == thisYear) m else "$m ${c.get(Calendar.YEAR)}"
            }
        }
        val out = LinkedHashMap<String, MutableList<Shot>>()
        newest(shots).forEach { out.getOrPut(header(it.takenAt)) { mutableListOf() } += it }
        return out.map { it.key to it.value }
    }
}
