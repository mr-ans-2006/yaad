package io.github.mrans2006.yaad.core

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

/** Our Kotlin tokenizer must give exactly the same pieces as the official Python one. */
class TokenizerTest {
    private val reference = JSONArray(javaClass.getResource("/reference.json")!!.readText())

    @Test fun matchesOfficialTokenizer() {
        for (i in 0 until reference.length()) {
            val case = reference.getJSONObject(i)
            val expectedIds = case.getJSONArray("ids").let { a -> List(a.length()) { a.getLong(it) } }
            val actual = TestModel.tokenizer.encode(case.getString("text")).ids.toList()
            println("tokens ok: " + case.getString("text"))
            assertEquals("ids for: " + case.getString("text"), expectedIds, actual)
        }
    }

    @Test fun splitsIntoWordPieces() {
        assertEquals(listOf("restore", "deleted", "files"), TestModel.tokenizer.tokenize("Restore DELETED files"))
    }

    @Test fun longTextIsCut() {
        val enc = WordPieceTokenizer(TestModel.tokenizer.let { java.io.File(System.getProperty("yaad.assets"), "model/vocab.txt").readLines() }, maxLength = 16)
            .encode("word ".repeat(100))
        assertEquals(16, enc.size)
    }
}
