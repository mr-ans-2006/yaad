package io.github.mrans2006.yaad.core

import java.text.Normalizer

/**
 * Turns English text into the numbers (token ids) the search model understands.
 *
 * The model (all-MiniLM-L6-v2) was trained with Google's "BERT WordPiece" tokenizer,
 * so we must split text EXACTLY the same way it did, or the model gets confused.
 * The steps are:
 *   1. Clean:      remove invisible control characters, lowercase, strip accents (é -> e).
 *   2. Split:      cut on spaces, and make every punctuation mark its own piece.
 *   3. WordPiece:  break each word into the longest pieces found in vocab.txt.
 *                  "screenshots" -> "screens", "##hot", "##s"  (## means "glued to the previous piece")
 *   4. Wrap:       put [CLS] at the start and [SEP] at the end, like the model expects.
 *
 * @param vocab every line of vocab.txt, in order. A token's id is its line number.
 */
class WordPieceTokenizer(vocab: List<String>, private val maxLength: Int = 256) {

    private val ids: Map<String, Int> = vocab.withIndex().associate { (i, t) -> t to i }
    private val clsId = ids.getValue("[CLS]")
    private val sepId = ids.getValue("[SEP]")
    private val unkId = ids.getValue("[UNK]")

    /** The result of tokenizing: ids plus a mask of 1s (one per real token). */
    data class Encoding(val ids: LongArray, val attentionMask: LongArray) {
        val size get() = ids.size
    }

    /** Text -> word pieces (as strings). Handy for tests and for learning. */
    fun tokenize(text: String): List<String> {
        val pieces = mutableListOf<String>()
        for (word in basicSplit(text)) pieces += wordPiece(word)
        return pieces
    }

    /** Text -> ids, ready for the model. Long texts are cut at [maxLength] tokens. */
    fun encode(text: String): Encoding {
        val pieceIds = tokenize(text).map { ids[it] ?: unkId }.take(maxLength - 2)
        val all = LongArray(pieceIds.size + 2)
        all[0] = clsId.toLong()
        pieceIds.forEachIndexed { i, id -> all[i + 1] = id.toLong() }
        all[all.size - 1] = sepId.toLong()
        return Encoding(all, LongArray(all.size) { 1L })
    }

    /** How many word pieces a text has (without [CLS]/[SEP]). */
    fun countPieces(text: String): Int = tokenize(text).size

    // ---- Step 1 and 2: clean and split ------------------------------------------------

    private fun basicSplit(text: String): List<String> {
        // Clean: drop control characters, turn all whitespace into plain spaces.
        val cleaned = StringBuilder()
        for (ch in text) {
            val cp = ch.code
            if (cp == 0 || cp == 0xFFFD || (Character.isISOControl(ch) && !ch.isWhitespace())) continue
            if (isCjk(cp)) cleaned.append(' ').append(ch).append(' ')
            else cleaned.append(if (ch.isWhitespace()) ' ' else ch)
        }
        // Lowercase and strip accents: "Café" -> "cafe".
        val lowered = Normalizer.normalize(cleaned.toString().lowercase(), Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }

        val out = mutableListOf<String>()
        for (word in lowered.split(' ')) {
            if (word.isEmpty()) continue
            // Every punctuation character becomes its own token.
            val current = StringBuilder()
            for (ch in word) {
                if (isPunctuation(ch)) {
                    if (current.isNotEmpty()) { out += current.toString(); current.clear() }
                    out += ch.toString()
                } else current.append(ch)
            }
            if (current.isNotEmpty()) out += current.toString()
        }
        return out
    }

    // ---- Step 3: WordPiece --------------------------------------------------------------

    private fun wordPiece(word: String): List<String> {
        if (word.length > 100) return listOf("[UNK]")
        val pieces = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var found: String? = null
            // Try the longest possible piece first, then shrink it one letter at a time.
            while (start < end) {
                val sub = (if (start > 0) "##" else "") + word.substring(start, end)
                if (ids.containsKey(sub)) { found = sub; break }
                end--
            }
            if (found == null) return listOf("[UNK]") // no piece fits: the whole word is unknown
            pieces += found
            start = end
        }
        return pieces
    }

    private fun isPunctuation(ch: Char): Boolean {
        val cp = ch.code
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        return when (Character.getType(ch).toByte()) {
            Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
            Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }

    private fun isCjk(cp: Int) = cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
        cp in 0x2A700..0x2B73F || cp in 0x2B740..0x2B81F || cp in 0x2B820..0x2CEAF ||
        cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F
}
