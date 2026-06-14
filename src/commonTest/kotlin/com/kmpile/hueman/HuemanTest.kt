package com.kmpile.hueman

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HuemanTest {
    private val hueman = Hueman.default()

    @Test
    fun namesAnExactPaletteColor() {
        // A color present in the palette is its own nearest (LAB distance 0).
        assertEquals("Red", hueman.name("#ff0000"))
        assertEquals("Mint", hueman.name("#3eb489"))
        assertEquals("Lavender", hueman.name("#b56edc"))
    }

    @Test
    fun acceptsHexWithoutHash() {
        assertEquals(hueman.name("#b56edc"), hueman.name("b56edc"))
    }

    @Test
    fun operatorGetMatchesName() {
        assertEquals(hueman.name("#ff0000"), hueman["#ff0000"])
    }

    @Test
    fun topLevelConvenienceUsesDefault() {
        assertEquals("Red", colorName("#ff0000"))
    }

    @Test
    fun customPaletteExactMatch() {
        val brand = Hueman.of("Brand Red" to "#ee3333", "Brand Ink" to "#112233")
        assertEquals("Brand Red", brand.name("#ee3333"))
        assertEquals("Brand Ink", brand["112233"])
    }

    @Test
    fun nearestReturnsNameAndHex() {
        val near = hueman.nearest("#000000")
        assertTrue(near.name.isNotEmpty())
        assertTrue(near.hex.startsWith("#"))
    }
}
