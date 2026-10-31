package io.github.mrans2006.yaad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * The big one: does searching by MEANING actually work on realistic screenshots?
 * Each query lists the screenshot(s) that must come first.
 */
class SearchTest {
    companion object {
        lateinit var docs: List<SearchRanker.Doc>
        val names = Samples.all.mapIndexed { i, s -> i.toLong() to s.name }.toMap()

        @BeforeClass @JvmStatic fun index() {
            docs = Samples.all.mapIndexed { i, s ->
                SearchRanker.Doc(i.toLong(), s.text, emptyList(), TestModel.embedder.embedChunks(s.text))
            }
        }

        fun search(q: String): List<String> {
            val hits = SearchRanker.rank(q, TestModel.embedder.embed(q), docs)
            println("\"$q\" -> " + hits.take(4).joinToString { "%s %.2f".format(names[it.id], it.score) })
            return hits.map { names.getValue(it.id) }
        }
    }

    @Test fun dataRecoveryFindsRestoreDeletedFiles() {
        val top = search("a tool for data recovery")
        // Recuva never says "data recovery", only "restore deleted files". It must still be top 2.
        assertTrue(top.take(2).containsAll(listOf("recuva", "testdisk")))
    }

    @Test fun recoverWithoutWordsInCommon() {
        val top = search("get back files I deleted by mistake")
        assertEquals("recuva", top.first())
    }

    @Test fun placesByMeaning() = assertEquals("kolachi", search("seafood place by the sea").first())

    @Test fun hackByMeaning() = assertEquals("spoon", search("stop water from boiling over").first())

    @Test fun studyByMeaning() = assertEquals("dp", search("algorithm lecture notes").first())

    @Test fun shoppingByMeaning() = assertEquals("buds", search("cheap earphones price").first())

    @Test fun privacyApp() = assertEquals("vpn", search("app to stay private online").first())

    @Test fun photoEditing() = assertEquals("snapseed", search("erase something from a picture").first())

    @Test fun exactNameStillWins() = assertEquals("llama", search("llama").first())

    @Test fun motivation() = assertEquals("quote", search("motivational saying").first())

    @Test fun freeCourse() = assertEquals("cs50", search("free computer science course").first())

    @Test fun pythonCode() = assertEquals("fib", search("recursive python function").first())

    @Test fun unrelatedQueryFindsLittle() {
        val top = search("wedding dress ideas")
        assertTrue("got $top", top.size <= 2)
    }
}
