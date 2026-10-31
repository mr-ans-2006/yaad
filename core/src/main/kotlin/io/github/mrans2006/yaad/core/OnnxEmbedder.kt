package io.github.mrans2006.yaad.core

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

/**
 * Runs the all-MiniLM-L6-v2 model (a small "sentence embedding" AI, ~23 MB) on the device.
 *
 * What happens in [embed]:
 *   1. The tokenizer turns text into token ids.
 *   2. ONNX Runtime runs the model. It gives back one 384-number vector PER TOKEN.
 *   3. "Mean pooling": we average those token vectors into ONE vector for the whole text.
 *   4. We normalize it to length 1, so comparing two texts is a simple dot product.
 *
 * Nothing here touches the internet. The model is a file inside the app.
 */
class OnnxEmbedder(modelBytes: ByteArray, private val tokenizer: WordPieceTokenizer) : TextEmbedder, AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = OrtSession.SessionOptions().use { opts ->
        opts.setIntraOpNumThreads(2)
        env.createSession(modelBytes, opts)
    }

    @Synchronized
    override fun embed(text: String): FloatArray {
        val enc = tokenizer.encode(text)
        val shape = longArrayOf(1, enc.size.toLong())
        val ids = OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(enc.ids), shape)
        val mask = OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(enc.attentionMask), shape)
        val types = OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(LongArray(enc.size)), shape)
        try {
            val inputs = mapOf("input_ids" to ids, "attention_mask" to mask, "token_type_ids" to types)
            session.run(inputs).use { result ->
                @Suppress("UNCHECKED_CAST")
                val hidden = (result[0].value as Array<Array<FloatArray>>)[0] // [tokens][384]
                val pooled = FloatArray(Vectors.DIM)
                for (token in hidden) for (d in 0 until Vectors.DIM) pooled[d] += token[d]
                for (d in 0 until Vectors.DIM) pooled[d] /= hidden.size
                return Vectors.normalize(pooled)
            }
        } finally {
            ids.close(); mask.close(); types.close()
        }
    }

    override fun close() {
        session.close()
    }
}
