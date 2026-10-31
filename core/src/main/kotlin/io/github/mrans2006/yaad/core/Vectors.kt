package io.github.mrans2006.yaad.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Small math helpers for "embeddings".
 *
 * An embedding is a list of 384 numbers that describes what a piece of text MEANS.
 * Texts with similar meaning get lists that point in a similar direction.
 * "Cosine similarity" measures that: 1.0 = same direction (same meaning), 0 = unrelated.
 */
object Vectors {
    const val DIM = 384

    /** Make the vector length 1, so cosine similarity becomes a simple dot product. */
    fun normalize(v: FloatArray): FloatArray {
        var sum = 0.0
        for (x in v) sum += x * x
        val len = sqrt(sum).toFloat()
        if (len == 0f) return v
        return FloatArray(v.size) { v[it] / len }
    }

    /** Dot product. For normalized vectors this IS the cosine similarity. */
    fun dot(a: FloatArray, b: FloatArray, bOffset: Int = 0): Float {
        var s = 0f
        for (i in a.indices) s += a[i] * b[bOffset + i]
        return s
    }

    /**
     * A screenshot can have several vectors (one per chunk of a long text), stored back to back.
     * Its similarity to the query is the BEST chunk's similarity.
     */
    fun bestMatch(query: FloatArray, packed: FloatArray): Float {
        if (packed.isEmpty()) return 0f
        var best = -1f
        var offset = 0
        while (offset + query.size <= packed.size) {
            val s = dot(query, packed, offset)
            if (s > best) best = s
            offset += query.size
        }
        return best
    }

    /** Floats -> bytes, so the database can store them. 4 bytes per number. */
    fun toBytes(v: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (x in v) buf.putFloat(x)
        return buf.array()
    }

    /** Bytes -> floats (the opposite of [toBytes]). */
    fun fromBytes(bytes: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buf.getFloat() }
    }
}
