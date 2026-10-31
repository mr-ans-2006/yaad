package io.github.mrans2006.yaad.core

/**
 * Decides which tags (Code, Tools, Places...) a screenshot gets.
 *
 * Two kinds of evidence are added up for each tag:
 *   1. Keywords: each matching keyword adds a little. "github" in the text? Points for Code.
 *   2. Meaning: how close the screenshot's meaning-vector is to the tag's example sentences.
 * A tag is given when its total passes [threshold]. At most [maxTags] tags per screenshot.
 *
 * @param embedder used once at the start to turn the example sentences into vectors.
 */
class AutoTagger(embedder: TextEmbedder?, private val threshold: Float = 0.35f, private val maxTags: Int = 3) {

    private val prototypes: Map<Tag, List<FloatArray>> =
        if (embedder == null) emptyMap() else Tag.entries.associateWith { tag -> tag.examples.map { embedder.embed(it) } }

    data class Scored(val tag: Tag, val score: Float)

    /** Every tag with its score, best first. Useful for debugging and tests. */
    fun scoreAll(text: String, vectors: FloatArray?): List<Scored> {
        val lower = " " + text.lowercase() + " "
        return Tag.entries.map { tag ->
            val hits = tag.keywords.count { kw -> containsWord(lower, kw) }
            val keywordScore = (hits * 0.12f).coerceAtMost(0.48f)
            val meaning = if (vectors == null || vectors.isEmpty()) 0f
                else prototypes[tag].orEmpty().maxOfOrNull { Vectors.bestMatch(it, vectors) } ?: 0f
            Scored(tag, keywordScore + meaning)
        }.sortedByDescending { it.score }
    }

    /** The tags to show for this screenshot. */
    fun tag(text: String, vectors: FloatArray?): List<Tag> {
        if (text.isBlank()) return emptyList()
        val all = scoreAll(text, vectors)
        val best = all.first().score
        return all.filter { it.score >= threshold && it.score >= best - 0.18f }.take(maxTags).map { it.tag }
    }

    private fun containsWord(haystack: String, keyword: String): Boolean {
        var from = 0
        while (true) {
            val i = haystack.indexOf(keyword, from)
            if (i < 0) return false
            val before = haystack[i - 1]
            val afterIndex = i + keyword.length
            val after = if (afterIndex < haystack.length) haystack[afterIndex] else ' '
            val startOk = !before.isLetterOrDigit() || !keyword.first().isLetterOrDigit()
            // Long keywords also match longer forms: "recover" matches "recovers" and "recovery".
            val endOk = !after.isLetterOrDigit() || !keyword.last().isLetterOrDigit() || keyword.length >= 5
            if (startOk && endOk) return true
            from = i + 1
        }
    }
}
