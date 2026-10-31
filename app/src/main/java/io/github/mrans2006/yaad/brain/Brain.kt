package io.github.mrans2006.yaad.brain

import android.content.Context
import io.github.mrans2006.yaad.core.AutoTagger
import io.github.mrans2006.yaad.core.KeyLineFinder
import io.github.mrans2006.yaad.core.LinkFinder
import io.github.mrans2006.yaad.core.OcrLine
import io.github.mrans2006.yaad.core.OnnxEmbedder
import io.github.mrans2006.yaad.core.TextEmbedder
import io.github.mrans2006.yaad.core.WordPieceTokenizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The "brain" loads the search model once and keeps it in memory.
 * Loading takes about half a second, so we do it lazily (only the first time it's needed).
 */
class Brain(private val context: Context) {

    private val lock = Mutex()
    private var embedder: TextEmbedder? = null
    private var tagger: AutoTagger? = null

    /** Gets the model ready (loads it the first time). Safe to call many times. */
    suspend fun embedder(): TextEmbedder = lock.withLock {
        embedder ?: run {
            val vocab = context.assets.open("model/vocab.txt").bufferedReader().readLines()
            val model = context.assets.open("model/minilm-l6-v2-qint8.onnx").use { it.readBytes() }
            OnnxEmbedder(model, WordPieceTokenizer(vocab)).also { embedder = it }
        }
    }

    suspend fun tagger(): AutoTagger {
        val e = embedder()
        return lock.withLock { tagger ?: AutoTagger(e).also { tagger = it } }
    }

    /** Everything Yaad learns from one screenshot's text. */
    data class Understanding(
        val text: String,
        val keyLine: String,
        val tags: List<String>,
        val links: List<String>,
        val vectors: FloatArray?,
    )

    /** OCR lines in -> key line, tags, links and meaning-vectors out. */
    suspend fun understand(lines: List<OcrLine>): Understanding {
        val text = lines.joinToString("\n") { it.text }
        if (text.isBlank()) return Understanding("", "", emptyList(), emptyList(), null)
        val vectors = embedder().embedChunks(text)
        val tags = tagger().tag(text, vectors).map { it.label }
        val links = LinkFinder.find(text).map { it.url }.distinct()
        return Understanding(text, KeyLineFinder.find(lines), tags, links, vectors)
    }
}
