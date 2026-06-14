package com.kmpile.hueman

import com.github.ajalt.colormath.Color

/** A palette entry with its precomputed CIELAB coordinates — the build input for [LabKdTree]. */
internal class LabPoint(val l: Float, val a: Float, val b: Float, val color: NamedColor)

/**
 * A 3-D K-D tree over the palette in CIELAB space for nearest-color search. sRGB Euclidean distance is
 * perceptually poor, so colors are compared in CIELAB (L, a, b). The bundled palette ships with LAB
 * precomputed (see the codegen), so building the tree is just a parse + sort — no color conversions.
 */
internal class LabKdTree private constructor(private val root: Node) {

    private class Node(
        val l: Float,
        val a: Float,
        val b: Float,
        val color: NamedColor,
        val left: Node?,
        val right: Node?,
    )

    fun nearest(color: Color): NamedColor {
        val lab = color.toLAB()
        var best = root
        var bestDist = Float.MAX_VALUE

        fun search(node: Node?, depth: Int) {
            if (node == null) return
            val dl = lab.l - node.l
            val da = lab.a - node.a
            val db = lab.b - node.b
            val dist = dl * dl + da * da + db * db
            if (dist < bestDist) {
                bestDist = dist
                best = node
            }
            val delta = when (depth % 3) { 0 -> dl; 1 -> da; else -> db }
            val near = if (delta <= 0f) node.left else node.right
            val far = if (delta <= 0f) node.right else node.left
            search(near, depth + 1)
            if (delta * delta < bestDist) search(far, depth + 1)  // other side may hold a closer point
        }

        search(root, 0)
        return best.color
    }

    companion object {
        fun build(points: List<LabPoint>): LabKdTree {
            require(points.isNotEmpty()) { "palette must not be empty" }
            return LabKdTree(buildNode(points.toMutableList(), 0)!!)
        }

        private fun buildNode(points: MutableList<LabPoint>, depth: Int): Node? {
            if (points.isEmpty()) return null
            when (depth % 3) {
                0 -> points.sortBy { it.l }
                1 -> points.sortBy { it.a }
                else -> points.sortBy { it.b }
            }
            val mid = points.size / 2
            val p = points[mid]
            return Node(
                p.l, p.a, p.b, p.color,
                buildNode(points.subList(0, mid).toMutableList(), depth + 1),
                buildNode(points.subList(mid + 1, points.size).toMutableList(), depth + 1),
            )
        }
    }
}
