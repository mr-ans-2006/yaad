package io.github.mrans2006.yaad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Yaad v1 is English-only. These tests pin down what happens with Urdu (see "Known limitations"
 * in the README), so the behaviour is documented and nobody is surprised by it.
 */
class UrduLimitTest {

    @Test fun searchModelOnlySeesSingleUrduLetters() {
        // The English model's vocabulary has no Urdu words, only single letters (and not all of them).
        val pieces = TestModel.tokenizer.tokenize("کراچی")
        assertFalse(pieces.contains("کراچی"))
        assertTrue("got $pieces", pieces.size >= 4)
    }

    @Test fun screenshotWithNoReadableTextIsNeverFound() {
        // When OCR finds no text, Yaad saves: text "", no tags, no vectors ("No text found" in the app).
        val blank = SearchRanker.Doc(1, "", emptyList(), null)
        for (q in listOf("کراچی", "receipt", "karachi")) {
            assertTrue(SearchRanker.rank(q, TestModel.embedder.embed(q), listOf(blank)).isEmpty())
        }
    }
}
