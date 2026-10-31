package io.github.mrans2006.yaad.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The same 9 sample screenshots, but this time the text comes from a REAL OCR engine (Tesseract on
 * the build computer, since ML Kit only runs on phones). Real OCR is messy: it splits headings,
 * misreads characters and keeps the status bar. This checks Yaad still copes.
 */
class RealOcrTest {
    private val screens: Map<String, List<OcrLine>> = JSONObject(javaClass.getResource("/real_ocr.json")!!.readText()).let { json ->
        json.keySet().associateWith { name ->
            val arr = json.getJSONArray(name)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                OcrLine(o.getString("text"), o.getDouble("height").toFloat(), o.getDouble("top").toFloat(), o.optInt("block", -1))
            }
        }
    }
    private val tagger = AutoTagger(TestModel.embedder)
    private val docs = screens.entries.mapIndexed { i, (name, lines) ->
        val text = lines.joinToString("\n") { it.text }
        Triple(i.toLong(), name, SearchRanker.Doc(i.toLong(), text, emptyList(), TestModel.embedder.embedChunks(text)))
    }

    @Test fun keyLinesAndTags() {
        val keyLines = mutableMapOf<String, String>()
        for ((name, lines) in screens) {
            val text = lines.joinToString("\n") { it.text }
            val key = KeyLineFinder.find(lines)
            keyLines[name] = key
            val vec = TestModel.embedder.embedChunks(text)
            val tags = tagger.tag(text, vec).joinToString { it.label } + "  " +
                tagger.scoreAll(text, vec).take(2).joinToString { "%s %.2f".format(it.tag.label, it.score) }
            println("%-9s key: %-60s tags: %s".format(name, key, tags))
        }
        assertEquals("Lecture 7: Dynamic Programming", keyLines["dp"])
        assertTrue(keyLines.getValue("recuva").contains("Restore deleted files"))
        assertTrue(keyLines.values.none { it.startsWith("9:41") || it.startsWith("8:20") })
    }

    @Test fun searchStillWorksOnRealOcrText() {
        val names = docs.associate { it.first to it.second }
        fun top(q: String): String {
            val hits = SearchRanker.rank(q, TestModel.embedder.embed(q), docs.map { it.third })
            println("\"$q\" -> " + hits.take(3).joinToString { "%s %.2f".format(names[it.id], it.score) })
            return names.getValue(hits.first().id)
        }
        assertTrue(top("a tool for data recovery") in setOf("recuva", "testdisk"))
        assertEquals("recuva", top("undelete files"))
        assertEquals("kolachi", top("seafood restaurant karachi"))
        assertEquals("spoon", top("kitchen trick"))
        assertEquals("buds", top("wireless earphones deal"))
        assertEquals("chat", top("ahmed asked for notes"))
    }
}
