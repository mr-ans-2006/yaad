package io.github.mrans2006.yaad.core

import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Search speed benchmark. NOT part of the normal test run.
 *
 *     ./gradlew :core:benchmark
 *
 * What it measures, on whatever computer runs it:
 *   - "rank": SearchRanker.rank() over N fake screenshots (N = 500, 1,000, 2,000). This is the
 *     hybrid search itself: meaning score + word score for every screenshot, then sorting.
 *   - "embed query": turning the search words into a meaning-vector with the real
 *     all-MiniLM-L6-v2 model (ONNX Runtime, 2 threads, like the app).
 *
 * The fake screenshots have realistic text lengths (10 to 400 words) and the same number of
 * 384-number vectors the app would store (one per 120-word chunk, at most 4). The vectors are
 * random, which is fine for timing: the maths per vector is the same.
 * Not included: reading the rows from the phone's database (the app keeps them in memory after
 * the first search) and drawing the results on screen.
 *
 * Each number is the MEDIAN of 20 timed runs, after 10 warm-up runs.
 */
class SearchBenchmark {

    private val query = "how to run llama.cpp on android"
    private val sizes = listOf(500, 1000, 2000)
    private val warmUps = 10
    private val runs = 20

    @Test fun searchSpeed() {
        Assume.assumeTrue("Run with ./gradlew :core:benchmark", System.getProperty("yaad.bench") == "true")

        val queryVector = TestModel.embedder.embed(query)
        repeat(warmUps) { TestModel.embedder.embed(query) }
        val embedMs = median(List(runs) { timeMs { TestModel.embedder.embed(query) } })

        val rows = sizes.map { n ->
            val docs = fakeScreenshots(n, Random(n))
            repeat(warmUps) { SearchRanker.rank(query, queryVector, docs) }
            val rankMs = median(List(runs) { timeMs { SearchRanker.rank(query, queryVector, docs) } })
            n to rankMs
        }

        val cpu = cpuName()
        val report = buildString {
            appendLine("Yaad search benchmark (median of $runs runs after $warmUps warm-ups)")
            appendLine("CPU: $cpu | JVM: ${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}")
            appendLine("Query: \"$query\"")
            appendLine("embed query (ONNX, 2 threads): %.2f ms".format(embedMs))
            appendLine()
            appendLine("| Screenshots | Rank only (ms) | Embed query + rank (ms) |")
            appendLine("|---:|---:|---:|")
            for ((n, ms) in rows) appendLine("| %,d | %.2f | %.2f |".format(n, ms, ms + embedMs))
        }
        println(report)
        File("build/benchmark.txt").apply { parentFile.mkdirs() }.writeText(report)
    }

    /** N screenshots of fake OCR text with realistic lengths, plus random normalized vectors. */
    private fun fakeScreenshots(n: Int, rnd: Random): List<SearchRanker.Doc> {
        val words = Samples.all.flatMap { s -> s.text.split(Regex("\\s+")) }.filter { it.isNotBlank() } +
            listOf("the", "and", "you", "free", "download", "chat", "message", "price", "order", "link",
                "notes", "exam", "lecture", "python", "android", "github", "receipt", "total", "delivery")
        val tags = Tag.values().map { it.label }
        return List(n) { i ->
            // Most screenshots are short (a chat, a tweet); a few are long (an article, a code file).
            val wordCount = when (rnd.nextInt(10)) {
                in 0..5 -> rnd.nextInt(10, 60)
                in 6..8 -> rnd.nextInt(60, 180)
                else -> rnd.nextInt(180, 400)
            }
            val text = List(wordCount) { words[rnd.nextInt(words.size)] }.joinToString(" ")
            val chunks = minOf(4, (wordCount + 119) / 120)
            val vectors = FloatArray(chunks * Vectors.DIM)
            repeat(chunks) { c ->
                Vectors.normalize(FloatArray(Vectors.DIM) { rnd.nextFloat() - 0.5f }).copyInto(vectors, c * Vectors.DIM)
            }
            SearchRanker.Doc(i.toLong(), text, tags.shuffled(rnd).take(rnd.nextInt(0, 3)), vectors)
        }
    }

    private inline fun timeMs(block: () -> Unit): Double {
        val t = System.nanoTime(); block(); return (System.nanoTime() - t) / 1e6
    }

    private fun median(xs: List<Double>): Double = xs.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }

    private fun cpuName(): String = runCatching {
        File("/proc/cpuinfo").readLines().first { it.startsWith("model name") }.substringAfter(":").trim() +
            " (${Runtime.getRuntime().availableProcessors()} threads)"
    }.getOrDefault(System.getProperty("os.arch") + " (${Runtime.getRuntime().availableProcessors()} threads)")
}
