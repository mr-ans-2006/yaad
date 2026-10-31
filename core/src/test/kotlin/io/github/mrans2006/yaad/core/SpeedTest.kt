package io.github.mrans2006.yaad.core

import org.junit.Test
import kotlin.random.Random

/** Rough speed numbers (on the build computer; a phone is a few times slower). */
class SpeedTest {
    @Test fun howFast() {
        val e = TestModel.embedder
        repeat(3) { e.embed("warm up the model") }
        val n = 40
        var t = System.nanoTime()
        repeat(n) { e.embed("a tool for data recovery number $it") }
        val perQuery = (System.nanoTime() - t) / 1e6 / n
        val longText = Samples.all.joinToString(" ") { it.text }
        t = System.nanoTime()
        repeat(10) { e.embedChunks(longText) }
        val perScreenshot = (System.nanoTime() - t) / 1e6 / 10

        // 5,000 fake screenshots with random vectors: how long does one brute-force search take?
        val rnd = Random(1)
        val docs = List(5000) { i ->
            SearchRanker.Doc(i.toLong(), "screenshot text number $i about things", emptyList(),
                Vectors.normalize(FloatArray(Vectors.DIM * 2) { rnd.nextFloat() - 0.5f }))
        }
        val q = e.embed("a tool for data recovery")
        repeat(3) { SearchRanker.rank("a tool for data recovery", q, docs) }
        t = System.nanoTime()
        repeat(10) { SearchRanker.rank("a tool for data recovery", q, docs) }
        val perSearch = (System.nanoTime() - t) / 1e6 / 10
        println("SPEED embed query: %.1f ms | embed a long screenshot text: %.1f ms | rank 5000 screenshots: %.1f ms"
            .format(perQuery, perScreenshot, perSearch))
    }
}
