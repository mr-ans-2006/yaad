package io.github.mrans2006.yaad.core

/**
 * Fake "OCR output" for 13 typical screenshots: the lines a phone would read, with text heights.
 * Status-bar junk and buttons are included on purpose, like real screenshots have.
 */
object Samples {
    data class Screen(val name: String, val lines: List<OcrLine>) {
        val text get() = lines.joinToString("\n") { it.text }
    }

    private fun s(name: String, vararg l: Pair<String, Float>) = Screen(name, l.map { OcrLine(it.first, it.second) })

    val all = listOf(
        s("recuva", "9:41" to 30f, "87%" to 30f, "recuva.com" to 32f, "Recuva" to 90f, "Restore deleted files" to 60f,
            "Accidentally deleted an important file? Lost something important when your" to 34f,
            "computer crashed? No problem! Recuva recovers files from your Windows" to 34f,
            "computer, recycle bin, digital camera card, or MP3 player." to 34f, "Free Download" to 40f),
        s("llama", "10:12" to 30f, "github.com/ggerganov/llama.cpp" to 32f, "ggerganov / llama.cpp" to 64f,
            "LLM inference in C/C++" to 40f, "68.2k stars  9.8k forks" to 32f, "Code  Issues  Pull requests" to 34f, "README.md" to 34f),
        s("kolachi", "Kolachi" to 80f, "4.5 (12,340 reviews)" to 34f, "Seafood restaurant · Rs 3,000-4,000" to 36f,
            "Do Darya, Phase 8 DHA, Karachi" to 36f, "Open · Closes 1 am" to 36f, "Directions  Start  Call  Save" to 32f),
        s("spoon", "Ayesha @ayesha_tips" to 36f, "Life hack:" to 40f,
            "Put a wooden spoon across a pot of boiling water and it won't boil over. Works every time." to 40f,
            "Reply" to 28f, "Repost" to 28f, "Like" to 28f),
        s("dp", "Lecture 7: Dynamic Programming" to 64f,
            "Memoization stores the answer to each subproblem so it is never computed twice." to 36f,
            "Example: Fibonacci with a table, O(n) instead of O(2^n)" to 36f, "Next lesson" to 30f),
        s("buds", "daraz.pk" to 30f, "Xiaomi Redmi Buds 5 Wireless Earbuds" to 52f, "Rs. 6,499" to 60f,
            "Rs. 9,999 -35%" to 32f, "Free Delivery" to 32f, "Add to Cart" to 38f, "Buy Now" to 38f),
        s("quote", "\u201CThe best time to plant a tree was 20 years ago. The second best time is now.\u201D" to 54f,
            "Chinese proverb" to 34f),
        s("chat", "Ahmed" to 44f, "online" to 28f, "bro send me the DSA notes for tomorrow's exam" to 36f,
            "ok sending in 5 min" to 36f, "Type a message" to 32f),
        s("vpn", "Proton VPN" to 70f, "Free plan: no ads, no logs, unlimited data" to 40f,
            "Get Proton VPN Free" to 44f, "protonvpn.com" to 30f),
        s("snapseed", "Snapseed" to 70f, "Healing: remove unwanted objects from your photos" to 40f,
            "Google LLC · Photography" to 30f, "Install" to 36f),
        s("testdisk", "TestDisk Step By Step" to 66f,
            "TestDisk is powerful free data recovery software! It recovers lost partitions" to 34f,
            "and makes non-booting disks bootable again." to 34f, "cgsecurity.org/wiki/TestDisk" to 32f),
        s("cs50", "CS50: Introduction to Computer Science" to 60f, "Harvard University · Free online course" to 36f,
            "cs50.harvard.edu/x" to 32f, "Enroll now" to 36f),
        s("fib", "def fibonacci(n):" to 40f, "    if n < 2: return n" to 40f,
            "    return fibonacci(n-1) + fibonacci(n-2)" to 40f, "main.py  Python 3.12" to 30f),
    )
}
