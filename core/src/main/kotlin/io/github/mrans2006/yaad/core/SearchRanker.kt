package io.github.mrans2006.yaad.core

/**
 * The search brain. Given what you typed, it ranks your screenshots.
 *
 * "Hybrid" search = two scores added together:
 *   1. Meaning score (semantic): cosine similarity between the query's vector and the screenshot's.
 *      This is how "a tool for data recovery" finds "Restore deleted files".
 *   2. Word score (keyword): how many of your words literally appear in the screenshot.
 *      This helps with names, like "recuva" or "kolachi", that the model may not know well.
 *
 * At a few thousand screenshots, checking every single one ("brute force") takes milliseconds,
 * so we don't need a fancy search index.
 */
object SearchRanker {

    /** What the ranker needs to know about one screenshot. */
    data class Doc(val id: Long, val text: String, val tags: List<String>, val vectors: FloatArray?)

    data class Hit(val id: Long, val score: Float, val meaning: Float, val words: Float)

    /** Below this meaning score, a result needs at least one matching word to be shown. */
    const val MEANING_FLOOR = 0.27f

    private val stopWords = setOf(
        "a", "an", "the", "for", "to", "of", "in", "on", "and", "or", "is", "it", "that", "this", "with",
        "my", "me", "i", "some", "about", "from", "how", "what", "where", "which", "find", "show", "screenshot",
        "screenshots", "thing", "things", "one", "any", "at", "by", "be", "was", "can", "do", "does",
    )

    fun queryWords(query: String): List<String> =
        query.lowercase().split(Regex("""[^\p{L}\p{N}#+.]+""")).map { it.trim('.') }
            .filter { it.length >= 2 && it !in stopWords }.distinct()

    fun rank(query: String, queryVector: FloatArray?, docs: List<Doc>, limit: Int = 100): List<Hit> {
        val words = queryWords(query)
        val phrase = query.trim().lowercase()
        // Build each word's pattern once (not once per screenshot): "word at the start of a word".
        val patterns = words.map { Regex("""(^|[^\p{L}\p{N}])""" + Regex.escape(it)) }
        return docs.mapNotNull { doc ->
            val meaning = if (queryVector != null && doc.vectors != null) Vectors.bestMatch(queryVector, doc.vectors) else 0f
            val text = doc.text.lowercase()
            val wordHits = patterns.count { it.containsMatchIn(text) }
            val tagHit = doc.tags.any { t -> words.any { it == t.lowercase() || it == t.lowercase().trimEnd('s') } }
            val wordScore = if (words.isEmpty()) 0f else wordHits.toFloat() / words.size
            val phraseBonus = if (phrase.length >= 4 && text.contains(phrase)) 0.15f else 0f
            val score = meaning + 0.35f * wordScore + phraseBonus + (if (tagHit) 0.1f else 0f)
            val keep = meaning >= MEANING_FLOOR || wordHits > 0 || tagHit
            if (keep) Hit(doc.id, score, meaning, wordScore) else null
        }.sortedByDescending { it.score }.take(limit)
    }
}
