package com.kmpile.hueman

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HuemanTest {
    private val hueman = Hueman.default()

    @Test
    fun namesTheCanonicalExample() {
        assertEquals("Classic Rose", hueman.name("#facfea"))
    }

    @Test
    fun acceptsHexWithoutHash() {
        assertEquals(hueman.name("#ff0000"), hueman.name("ff0000"))
    }

    @Test
    fun exactPaletteColorReturnsItself() {
        val brand = Hueman.of(mapOf("Brand Red" to "#ee3333", "Brand Ink" to "#112233"))
        assertEquals("Brand Red", brand.name("#ee3333"))
        assertEquals("Brand Ink", brand.name("112233"))
    }

    @Test
    fun nearestReturnsNameAndHex() {
        val near = hueman.nearest("#000000")
        assertTrue(near.name.isNotEmpty())
        assertTrue(near.hex.startsWith("#"))
    }
}
