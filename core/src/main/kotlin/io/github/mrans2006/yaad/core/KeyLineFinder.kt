package io.github.mrans2006.yaad.core

/**
 * Picks the ONE line that best sums up a screenshot. In v1 this line is the "summary".
 *
 * It is a scoring game. Every line gets points, and the highest score wins:
 *   + bigger text than the rest (headings are usually the point of the screenshot)
 *   + a good length (4 to 14 words reads like a title or a sentence)
 *   + mostly real letters (not "12:45 ▲ 87%")
 *   + a little bonus for being near the top
 *   - lines that look like phone UI: the clock, battery, "Reply", "Share", "Follow"...
 */
object KeyLineFinder {

    private val chromeWords = setOf(
        "reply", "share", "like", "likes", "follow", "following", "subscribe", "more", "search", "home",
        "back", "cancel", "ok", "done", "edit", "copy", "send", "message", "messages", "save", "saved",
        "menu", "settings", "next", "skip", "close", "open", "view", "see more", "show more", "translate",
        "retweet", "repost", "comment", "comments", "explore", "reels", "profile", "notifications",
        "type a message", "write a comment", "add a comment", "sign in", "log in", "sign up", "install",
        "directions", "start", "call", "website", "download", "buy", "cart", "add", "to", "now", "public", "code",
        "issues", "pull", "requests", "actions", "repost", "liked", "bookmark", "subscribed", "join",
    )
    private val statusBar = Regex("""^(\d{1,2}[:.]\d{2}(\s?[ap]m)?|\d{1,3}\s?%|lte|4g|5g|volte|wi-?fi|jazz|zong|telenor|ufone)([\s|•·]+.*)?$""", RegexOption.IGNORE_CASE)
    private val urlOnly = Regex("""^(https?://)?[\w.-]+\.[a-z]{2,}(/\S*)?$""", RegexOption.IGNORE_CASE)

    fun find(lines: List<OcrLine>): String {
        val candidates = joinWrapped(lines.map { it.copy(text = clean(it.text)) }.filter { isUseful(it.text) })
        if (candidates.isEmpty()) return lines.firstOrNull { it.text.isNotBlank() }?.text?.trim()?.take(120) ?: ""

        val heights = candidates.map { it.height }.filter { it > 0f }.sorted()
        val median = if (heights.isEmpty()) 0f else heights[heights.size / 2]

        var best = candidates.first()
        var bestScore = Float.NEGATIVE_INFINITY
        candidates.forEachIndexed { index, line ->
            val s = score(line, index, candidates.size, median)
            if (s > bestScore) { bestScore = s; best = line }
        }
        var result = best.text
        // A very short winner (like a repo name "yaad") reads better with the line under it.
        val i = candidates.indexOf(best)
        if (wordCount(result) < 3 && !codeStart.containsMatchIn(result)) {
            // Look at the next three lines for a real sentence (mostly letters, 3+ words).
            val partner = candidates.drop(i + 1).take(3).firstOrNull { wordCount(it.text) >= 3 && letterRatio(it.text) >= 0.5f }
            if (partner != null) result = result + " — " + partner.text
        }
        return shorten(result)
    }

    /**
     * OCR reads a long heading like "Lecture 7: Dynamic Programming" as TWO lines.
     * We glue lines back together when they belong to the same paragraph and have the same text size.
     */
    internal fun joinWrapped(lines: List<OcrLine>): List<OcrLine> {
        val out = mutableListOf<OcrLine>()
        for (line in lines) {
            val prev = out.lastOrNull()
            val sameBlock = prev != null && prev.block >= 0 && prev.block == line.block
            val sameSize = prev != null && prev.height > 0f && line.height > 0f &&
                line.height / prev.height in 0.8f..1.25f
            // Also glue lines that sit right under each other (gap smaller than half a line).
            val close = prev != null && prev.top > 0f && line.top > prev.top &&
                line.top - (prev.top + prev.height) < prev.height * 0.6f
            val prevEndsSentence = prev != null && prev.text.trimEnd().lastOrNull() in setOf('.', '!', '?', ':')
            if (prev != null && sameSize && (sameBlock || (close && !prevEndsSentence))) {
                out[out.size - 1] = prev.copy(text = prev.text + " " + line.text)
            } else out += line
        }
        return out
    }

    /** Keeps summaries short: the first sentence, or about 140 characters cut at a word. */
    internal fun shorten(text: String, max: Int = 140): String {
        if (text.length <= max) return text.trim()
        val sentenceEnd = Regex("""[.!?](\s|$)""").find(text, 30)
        if (sentenceEnd != null && sentenceEnd.range.first < max) return text.substring(0, sentenceEnd.range.first + 1).trim()
        val cut = text.lastIndexOf(' ', max).takeIf { it > 40 } ?: max
        return text.substring(0, cut).trim() + "…"
    }

    internal fun score(line: OcrLine, index: Int, total: Int, medianHeight: Float): Float {
        val text = line.text
        val words = wordCount(text)
        var score = 0f

        // Size: compare with the typical line height on this screenshot.
        // Small differences are just OCR noise, so only clearly bigger/smaller text counts.
        if (medianHeight > 0f && line.height > 0f) {
            val ratio = (line.height / medianHeight).coerceIn(0.6f, 2.2f)
            if (ratio > 1.2f || ratio < 0.85f) score += (ratio - 1f) * 1.6f
        }
        // Length: titles and real sentences are 4..14 words.
        score += when {
            words in 4..14 -> 1.0f
            words in 2..3 -> 0.45f
            words in 15..25 -> 0.85f
            words > 25 -> 0.2f
            else -> -0.4f
        }
        // Letters vs. symbols and digits.
        score += letterRatio(text) * 0.8f
        // In code screenshots, the line that names the function or class says the most.
        if (codeStart.containsMatchIn(text)) score += 0.7f
        // Near the top gets a small bonus (but the very first line is often the status bar).
        if (total > 1) score += (1f - index.toFloat() / total) * 0.35f
        // "@someone" is a username line on social apps, not the content.
        if (text.contains(Regex("""(^|\s)@\w"""))) score -= 0.5f
        // A bare link is useful, but it is rarely the best summary.
        if (urlOnly.matches(text)) score -= 0.5f
        // Sentences that end like a title or statement.
        if (text.first().isUpperCase()) score += 0.1f
        return score
    }

    internal fun isUseful(text: String): Boolean {
        if (text.length < 4) return false
        if (text.count { it.isLetter() } < 3) return false
        val lower = text.lowercase().trim(' ', '.', ':', '•', '·')
        if (lower in chromeWords) return false
        // A row of buttons like "Directions Start Call Save" or "Reply Repost Like Share".
        val words = lower.split(Regex("""[^a-z]+""")).filter { it.isNotEmpty() }
        if (words.isNotEmpty() && words.count { it in chromeWords } >= (words.size * 3 + 3) / 4) return false
        if (statusBar.matches(lower)) return false
        return true
    }

    private val codeStart = Regex("""^\s*(def|class|fun|function|public|private|interface|struct|fn|func)\s+\w""")

    private fun letterRatio(text: String) = text.count { it.isLetter() }.toFloat() / text.length.coerceAtLeast(1)

    private fun clean(text: String) = text.replace(Regex("""\s+"""), " ").trim(' ', '•', '·', '|', '>', '-', '*')

    private fun wordCount(text: String) = text.split(' ').count { w -> w.any { it.isLetterOrDigit() } }
}
