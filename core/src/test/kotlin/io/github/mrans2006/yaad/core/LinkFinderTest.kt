package io.github.mrans2006.yaad.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkFinderTest {
    @Test fun findsFullAndBareLinks() {
        val links = LinkFinder.find("See https://recuva.com/download. Code at github.com/mr-ans-2006/yaad, mail me@x.com")
        assertEquals(listOf("https://recuva.com/download", "https://github.com/mr-ans-2006/yaad"), links.map { it.url })
    }

    @Test fun ignoresNormalWords() = assertEquals(0, LinkFinder.find("Version 2.0 is out. Rs. 6,499 only").size)
}
