package io.github.mrans2006.yaad.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "just now", "5 min ago", "yesterday", "12 Oct" — friendly dates for the cards. */
fun friendlyDate(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - millis
    val min = 60_000L
    val day = 24 * 60 * min
    return when {
        diff < min -> "just now"
        diff < 60 * min -> "${diff / min} min ago"
        diff < day -> "${diff / (60 * min)} h ago"
        diff < 2 * day -> "yesterday"
        diff < 7 * day -> "${diff / day} days ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(millis))
    }
}
