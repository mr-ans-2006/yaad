package io.github.mrans2006.yaad.core

/**
 * Finds web links inside the text OCR read, so we can make them tappable.
 * It finds full links ("https://github.com/x/y") and bare ones ("github.com/x/y").
 */
object LinkFinder {

    data class Link(val text: String, val start: Int, val end: Int, val url: String)

    private val tlds = "com|org|net|io|dev|app|ai|co|me|edu|gov|pk|in|uk|us|info|xyz|tech|so|gg|tv|ly|sh|to|page|site|blog|store|link|cc|be|ca|de|fr|it|nl|ru|jp|au|us|cloud|tools|gl"
    private val pattern = Regex(
        """(?i)\b((?:https?://)?(?:www\.)?[a-z0-9][a-z0-9-]*(?:\.[a-z0-9-]+)*\.(?:$tlds)\b(?::\d+)?(?:/[^\s<>"'()\[\]]*)?)"""
    )
    private val email = Regex("""(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}""")

    fun find(text: String): List<Link> {
        val emails = email.findAll(text).map { it.range }.toList()
        return pattern.findAll(text).mapNotNull { m ->
            if (emails.any { m.range.first in it }) return@mapNotNull null // part of an email address
            var raw = m.value.trimEnd('.', ',', ';', ':', '!', '?')
            if (raw.length < 4) return@mapNotNull null
            val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
            Link(raw, m.range.first, m.range.first + raw.length, url)
        }.toList()
    }
}
