package io.github.mrans2006.yaad.core

import org.junit.Assert.assertTrue
import org.junit.Test

class TaggerTest {
    private val tagger = AutoTagger(TestModel.embedder)
    private val expected = mapOf(
        "recuva" to Tag.TOOLS, "llama" to Tag.CODE, "kolachi" to Tag.PLACES, "spoon" to Tag.HACKS,
        "dp" to Tag.STUDY, "buds" to Tag.SHOPPING, "quote" to Tag.QUOTES, "chat" to Tag.CHATS,
        "vpn" to Tag.TOOLS, "snapseed" to Tag.TOOLS, "testdisk" to Tag.TOOLS, "cs50" to Tag.STUDY, "fib" to Tag.CODE,
    )

    @Test fun eachSampleGetsTheRightTag() {
        val misses = mutableListOf<String>()
        for (s in Samples.all) {
            val tags = tagger.tag(s.text, TestModel.embedder.embedChunks(s.text))
            val scores = tagger.scoreAll(s.text, TestModel.embedder.embedChunks(s.text)).take(3)
                .joinToString { "%s %.2f".format(it.tag.label, it.score) }
            println("%-9s -> %-28s (%s)".format(s.name, tags.joinToString { it.label }, scores))
            if (expected[s.name] !in tags) misses += s.name
        }
        assertTrue("missed: $misses", misses.isEmpty())
    }
}
