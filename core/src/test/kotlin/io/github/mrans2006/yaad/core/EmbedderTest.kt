package io.github.mrans2006.yaad.core

import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Kotlin + ONNX embedding must match the Python reference (same model file). */
class EmbedderTest {
    private val reference = JSONArray(javaClass.getResource("/reference.json")!!.readText())

    @Test fun matchesPythonReference() {
        for (i in 0 until reference.length()) {
            val case = reference.getJSONObject(i)
            val expected = case.getJSONArray("vec").let { a -> FloatArray(a.length()) { a.getDouble(it).toFloat() } }
            val actual = TestModel.embedder.embed(case.getString("text"))
            val cos = Vectors.dot(expected, actual)
            println("cosine(kotlin, python) = %.5f  for \"%s\"".format(cos, case.getString("text")))
            assertTrue("vectors differ for ${case.getString("text")}: $cos", cos > 0.995f)
        }
    }

    @Test fun similarMeaningScoresHigher() {
        val e = TestModel.embedder
        val q = e.embed("a tool for data recovery")
        val good = Vectors.dot(q, e.embed("Restore deleted files from your phone"))
        val bad = Vectors.dot(q, e.embed("Best biryani in Karachi, open till 2am"))
        println("recovery vs restore = %.3f, vs biryani = %.3f".format(good, bad))
        assertTrue(good > bad + 0.2f)
    }

    @Test fun vectorsSurviveTheDatabaseRoundTrip() {
        val v = TestModel.embedder.embed("hello")
        val back = Vectors.fromBytes(Vectors.toBytes(v))
        assertTrue(v.contentEquals(back))
    }
}
