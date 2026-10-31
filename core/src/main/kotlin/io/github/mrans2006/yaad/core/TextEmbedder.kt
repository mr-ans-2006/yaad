package io.github.mrans2006.yaad.core

/**
 * Anything that can turn text into a meaning-vector.
 * The real one is [OnnxEmbedder]. Tests can use a fake one.
 */
interface TextEmbedder {
    /** One normalized vector of [Vectors.DIM] numbers. */
    fun embed(text: String): FloatArray

    /**
     * Long texts are cut into chunks (the model only reads ~256 word pieces at a time).
     * Returns all chunk vectors packed back to back (at most [maxChunks]).
     */
    fun embedChunks(text: String, maxChunks: Int = 4): FloatArray {
        val chunks = Chunker.split(text, maxChunks)
        if (chunks.isEmpty()) return FloatArray(0)
        val out = FloatArray(chunks.size * Vectors.DIM)
        chunks.forEachIndexed { i, c -> embed(c).copyInto(out, i * Vectors.DIM) }
        return out
    }
}

/** Splits text into chunks of about [wordsPerChunk] words, keeping lines together where possible. */
object Chunker {
    fun split(text: String, maxChunks: Int, wordsPerChunk: Int = 120): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return words.chunked(wordsPerChunk).take(maxChunks).map { it.joinToString(" ") }
    }
}
