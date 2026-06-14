package com.kmpile.hueman

import com.github.ajalt.colormath.Color
import com.github.ajalt.colormath.model.RGB

/** A palette entry: a human color [name] and its [hex] value (e.g. "Classic Rose", "#facfea"). */
public data class NamedColor(val name: String, val hex: String)

/**
 * Maps any color to the closest human-readable name from a palette, using perceptual CIELAB distance
 * (a K-D tree keeps lookups ~O(log n)).
 *
 * ```
 * val hueman = Hueman.default()
 * hueman.name("#facfea")   // "Classic Rose"
 * ```
 *
 * Build one with [default] (the bundled ~32k meodai/color-names palette) or [of] (your own palette).
 * A [Hueman] is immutable and thread-safe; build it once and reuse it.
 */
public class Hueman private constructor(private val tree: LabKdTree) {

    /** The [NamedColor] perceptually closest to [color]. */
    public fun nearest(color: Color): NamedColor = tree.nearest(color)

    /** The [NamedColor] closest to a hex string, with or without a leading '#' ("#facfea" / "facfea"). */
    public fun nearest(hex: String): NamedColor = nearest(RGB(withHash(hex)))

    /** The name of the color closest to [color]. */
    public fun name(color: Color): String = nearest(color).name

    /** The name of the color closest to [hex]. */
    public fun name(hex: String): String = nearest(hex).name

    public companion object {
        /** Backed by the bundled meodai/color-names palette (~32k names, MIT — see NOTICE). The palette
         *  ships with CIELAB precomputed, so this just parses + builds the tree (no color conversions). */
        public fun default(): Hueman = Hueman(LabKdTree.build(parseBundledPalette()))

        /** Backed by a custom palette of name → hex. */
        public fun of(palette: Map<String, String>): Hueman =
            of(palette.map { (name, hex) -> NamedColor(name, hex) })

        /** Backed by a custom [palette] (each hex is converted to CIELAB here). */
        public fun of(palette: List<NamedColor>): Hueman {
            require(palette.isNotEmpty()) { "palette must not be empty" }
            val points = palette.map { c ->
                val lab = RGB(withHash(c.hex)).toLAB()
                LabPoint(lab.l, lab.a, lab.b, c)
            }
            return Hueman(LabKdTree.build(points))
        }
    }
}

internal fun withHash(hex: String): String = if (hex.startsWith("#")) hex else "#$hex"

/**
 * Decode the generated [colorData] into palette points. Each record is `name | hex | L | a | b`
 * (fields joined by U+0001), records joined by U+0002 — LAB precomputed at build time.
 */
private fun parseBundledPalette(): List<LabPoint> {
    val unit = 1.toChar()
    val rs = 2.toChar()
    return colorData.split(rs).mapNotNull { record ->
        if (record.isEmpty()) return@mapNotNull null
        val f = record.split(unit)
        if (f.size < 5) return@mapNotNull null
        LabPoint(f[2].toFloat(), f[3].toFloat(), f[4].toFloat(), NamedColor(f[0], f[1]))
    }
}
