package io.github.mrans2006.yaad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KeyLineTest {
    @Test fun printAll() {
        for (s in Samples.all) println("%-9s -> %s".format(s.name, KeyLineFinder.find(s.lines)))
    }

    @Test fun picksTheHeadingNotTheClock() {
        val line = KeyLineFinder.find(Samples.all.first { it.name == "dp" }.lines)
        assertEquals("Lecture 7: Dynamic Programming", line)
    }

    @Test fun shortHeadingGetsTheNextLine() {
        val line = KeyLineFinder.find(Samples.all.first { it.name == "vpn" }.lines)
        assertEquals("Proton VPN — Free plan: no ads, no logs, unlimited data", line)
    }

    @Test fun ignoresPhoneChrome() {
        assertFalse(KeyLineFinder.isUseful("9:41"))
        assertFalse(KeyLineFinder.isUseful("Reply"))
        assertFalse(KeyLineFinder.isUseful("87%"))
    }

    @Test fun placeGetsItsDescription() =
        assertEquals("Kolachi — Seafood restaurant · Rs 3,000-4,000", KeyLineFinder.find(Samples.all.first { it.name == "kolachi" }.lines))

    @Test fun codeGetsTheFunctionName() =
        assertEquals("def fibonacci(n):", KeyLineFinder.find(Samples.all.first { it.name == "fib" }.lines))

    @Test fun emptyInputGivesEmptyLine() = assertEquals("", KeyLineFinder.find(emptyList()))
}
