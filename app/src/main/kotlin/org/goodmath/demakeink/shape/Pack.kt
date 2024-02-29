package org.goodmath.demakeink.shape

import org.goodmath.demakeink.util.Point
import kotlin.math.pow



class Packable(inShapes: List<Shape>,
                    val rotation: Double?,
                    val dilation: Double,
                    val useUpper: Boolean = true) {
    val shapes: List<Shape>
    val extent: Extent
    val mask: Shape2
    val centroid: Point
    val dilatedMask: Shape2
    val dilatedExtent: Extent

    init {
        if (rotation == null) {
            shapes = inShapes
        } else {
            shapes = inShapes.map { it.copy() }
            for (item in shapes) {
                item.rotate(0, 0, 1, rotation)
            }
        }
        val extent = shapes[0].extent()
        for (item in shapes) {
            item.move(-extent.xMin, -extent.yMin, 0.0)
        }
        this.extent = shapes[0].extent()
        val mask = shapes[0].polygonMask()
        val loop = mask.loop(holes=false)
        this.mask = createPolygon2(loop.values)
        this.centroid = loop.centroid
        val tmpDilatedMask = this.mask.offsetCurve(dilation, 2.0.pow(16))
        dilatedMask = createPolygon2(tmpDilatedMask.loop(holes=false).values)
        dilatedExtent = dilatedMask.extent()
    }

}

interface Pack {
    fun copy(): Pack
    fun put(x: Int, y: Int, p: Packable)

    fun valid(x: Int, y: Int, p: Packable): Boolean

    fun render(bitDiameter: Double, bitPad: Double): Pair<Shape, Shape>

    fun renderPrint(): List<Shape>

}

fun makeSegment(instrument: Shape, top: Double, low: Double,
                high: Double, radius: Double, clip: Boolean=true): Shape {
    throw NotImplementedError()
}

fun makeSegments(instrument: Shape,
                 length: Double,
                 radius: Double,
                 topFractions: List<Double>,
                 bottomFractions: List<Double>,
                 clip: Boolean=true): List<Shape> {
    throw NotImplementedError()
}

fun deconstruct(outer: Shape, bore: Shape,
                topFractions: List<Double>, bottomFractions: List<Double>,
                bitDiameter: Double,
                dilation: Double,
                blockZsize: Double): List<Shape> {
    // ph: Object must be large enough to include end padding
    throw NotImplementedError()
}

