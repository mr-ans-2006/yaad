package io.github.mrans2006.yaad.core

import java.io.File

/** Loads the same model + vocab files the app ships, straight from app/src/main/assets. */
object TestModel {
    private val assets = File(System.getProperty("yaad.assets") ?: "../app/src/main/assets")
    val tokenizer by lazy { WordPieceTokenizer(File(assets, "model/vocab.txt").readLines()) }
    val embedder by lazy { OnnxEmbedder(File(assets, "model/minilm-l6-v2-qint8.onnx").readBytes(), tokenizer) }
}
