package org.goodmath.chalumier.geom

import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.curves.squared
import org.goodmath.chalumier.errors.InvalidOperationException
import org.goodmath.chalumier.geom.Bounds
import org.goodmath.chalumier.geom.Bounds2D
import org.goodmath.chalumier.geom.ThreeDBody
import org.goodmath.chalumier.geom.ThreeDGeometry
import org.goodmath.chalumier.geom.TwoDShape
import org.goodmath.chalumier.util.Point
import java.lang.StringBuilder
import java.nio.file.Path
import kotlin.collections.filterNotNull
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mapIndexed
import kotlin.collections.maxOf
import kotlin.collections.minOf
import kotlin.collections.sorted
import kotlin.collections.toSet
import kotlin.collections.zip
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.let
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.repeat



data class ThreeDPoint(val x: Double, val y: Double, val z: Double) {
    operator fun times(factor: Double): ThreeDPoint {
        return ThreeDPoint(x * factor, y * factor, z * factor)
    }

    operator fun plus(other: ThreeDPoint): ThreeDPoint {
        return ThreeDPoint(x + other.x, y + other.y, z + other.z)
    }

    fun normalizeAngle(theta: Double): Double {
        return if (theta > 180.0) {
            theta - 360.0
        } else if (theta <= -180.0) {
            theta + 360.0
        } else {
            theta
        }
    }

    // Treating the point like a vector, rotate it.
    fun rotate(xAngle: Double, yAngle: Double, zAngle: Double): ThreeDPoint {
        val xNorm = normalizeAngle(xAngle)
        val yNorm = normalizeAngle(yAngle)
        val zNorm = normalizeAngle(zAngle)
        System.err.println("theta_x=${xNorm}, theta_y=${yNorm}, theta_z=${zNorm}")
        // rotate in the XY plane
        val afterZ = when (zNorm) {
            0.0 -> this
            90.0 -> ThreeDPoint(-y, x, z)
            180.0 -> ThreeDPoint(-x, -y, z)
            -90.0 -> ThreeDPoint(y, -x, z)
            else -> throw InvalidOperationException("Only increments of 90 degrees are supported in rotations")
        }
        // rotate in the xz plane
        val afterY = when (yNorm) {
            0.0 -> afterZ
            90.0 -> ThreeDPoint(-afterZ.z, afterZ.y, afterZ.x)
            180.0 -> ThreeDPoint(-afterZ.x, afterZ.y, -afterZ.z)
            -90.0 -> ThreeDPoint(afterZ.z, afterZ.y, -afterZ.x)
            else -> throw InvalidOperationException("Only increments of 90 degrees are supported in rotations")
        }
        return when (xNorm) {
            0.0 -> afterY
            90.0 -> ThreeDPoint(afterY.x, -afterY.z, afterY.y)
            180.0 -> ThreeDPoint(afterY.x, -afterY.y, -afterY.z)
            -90.0 -> ThreeDPoint(afterY.x, afterY.z, -afterY.y)
            else -> throw InvalidOperationException("Only increments of 90 degrees are supported in rotations")
        }
    }
}

abstract class Scad3D: ThreeDBody<Scad3D> {
    override fun toText(): String {
        val result = StringBuilder()
        result.append("include <bosl2/std.scad>\n\n")
        result.append(render(0))
        return result.toString()
    }

    override fun save(dir: Path, filename: String) {
        (dir / "$filename.scad").writeText(toText())
    }

    abstract fun render(i: Int): String

    fun indent(i: Int): String = "   ".repeat(i)

    override fun rotate(x: Double, y: Double, z: Double): Scad3D {
        return RotatedScad(this, x, y, z)
    }

    override fun translate(x: Double, y: Double, z: Double): Scad3D {
        return TranslatedScad(this, x, y, z)
    }

    override fun union(other: Scad3D): Scad3D {
        return ScadUnion(listOf(this, other))
    }

    override fun intersect(other: Scad3D): Scad3D {
        return ScadIntersection(listOf(this, other))
    }

    override fun difference(other: Scad3D): Scad3D {
        return ScadDifference(listOf(this, other))
    }

    override fun scale(scale: Double): Scad3D {
        return ScaledScad(this, scale)
    }

    override fun positionNicely(): Scad3D {
        return translate(
                    -0.5 * (bounds().min.x + bounds().max.x),
                    -0.5 * (bounds().min.y + bounds().max.y),
                    -bounds().min.z,
                )
    }
}

class ScadStatement(val stmt: String,
                    val bound: Bounds
): Scad3D() {

    override fun render(i: Int): String {
        return "${indent(i)}$stmt;\n"
    }

    override fun bounds(): Bounds {
        return bound
    }
}

abstract class ScadModule(
    val name: String,
    val body: List<Scad3D>): Scad3D() {
    abstract fun renderParams(): String?

    override fun render(i: Int): String {
        val result = StringBuilder()
        result.append(indent(i))
        result.append(name)
        result.append("(")
        renderParams()?.let { result.append(it) }
        result.append(") {\n")
        for (b in body) {
            result.append(b.render(i+1))
        }
        result.append(indent(i))
        result.append("}\n")
        return result.toString()
    }
}

class ScaledScad(val orig: Scad3D, val scale: Double): ScadModule("scale", listOf(orig)) {
    override fun renderParams(): String {
        return scale.toString()
    }

    override fun bounds(): Bounds {
        return orig.bounds() * scale
    }
}

class TranslatedScad(val orig: Scad3D, val dx: Double, val dy: Double, val dz: Double): ScadModule("translate", listOf(orig)) {
    override fun renderParams(): String? {
        return "[$dx, $dy, $dz]"
    }

    override fun bounds(): Bounds {
        return orig.bounds()
    }
}

class RotatedScad(val orig: Scad3D, val xAngle: Double, val yAngle: Double, val zAngle: Double): ScadModule("rotate", listOf(orig)) {
    override fun renderParams(): String? {
        return "[$xAngle, $yAngle, $zAngle]"
    }

    private fun rotateInPlane(x: Double, y: Double, rotationDegrees: Double): Pair<Double, Double> {
        val theta = atan2(y, x)
        val mag = hypot(x, y)
        val newAngle = theta + (rotationDegrees*PI/180.0)
        return Pair(x * cos(newAngle), y * sin(newAngle))
    }


    fun rotateVector(orig: ThreeDPoint): ThreeDPoint {
        // For computing the effect of the X rotation, we're looking
        // at the YZ plane - that is, Y is acting as the horizontal (X),
        // and Z as the vertical (Y).
        val (zAfterX, yAfterX) = rotateInPlane(orig.z, orig.y, xAngle)
        val afterX = ThreeDPoint(orig.x, yAfterX, zAfterX)
        // For the Y rotation, it's the XZ plane.
        val (xAfterY, zAfterY) = rotateInPlane(afterX.x, afterX.z, yAngle)
        val afterY = ThreeDPoint(xAfterY, afterX.y, zAfterY)
        val (xAfterZ, yAfterZ) = rotateInPlane(afterY.x, afterY.y, zAngle)
        return ThreeDPoint(xAfterZ, yAfterZ, afterY.z)
    }



    override fun bounds(): Bounds {
        val origBounds = orig.bounds()

        return if (xAngle % 90.0 == 0.0  && yAngle % 90.0 == 0.0 && zAngle % 90.0 == 0.0) {
            pointSetBounds(listOf(origBounds.min.rotate(xAngle, yAngle, zAngle),
                origBounds.max.rotate(xAngle, yAngle, zAngle)))
        } else {
            val rotMin = rotateVector(orig.bounds().min)
            val rotMax = rotateVector(orig.bounds().max)
            return pointSetBounds(listOf(rotMin, rotMax))
        }
    }
}

class ScadUnion(members: List<Scad3D>): ScadModule("union", members) {
    override fun renderParams(): String? {
        return null
    }

    override fun bounds(): Bounds {
        val bodyBounds = body.map { it.bounds() }
        val xMax = bodyBounds.maxOf { it.max.x }
        val xMin = bodyBounds.minOf { it.min.x }
        val yMax = bodyBounds.maxOf { it.max.y }
        val yMin = bodyBounds.minOf { it.min.y }
        val zMax = bodyBounds.maxOf { it.max.z }
        val zMin = bodyBounds.minOf { it.min.z }
        return Bounds(ThreeDPoint(xMin, yMin, zMin), ThreeDPoint(xMax, yMax, zMax))
    }
}

class ScadIntersection(members: List<Scad3D>): ScadModule("intersection", members) {
    override fun renderParams(): String? {
        return null
    }

    override fun bounds(): Bounds {
        val bodyBounds = body.map { it.bounds() }
        val xMax = bodyBounds.minOf { it.max.x }
        val xMin = bodyBounds.maxOf { it.min.x }
        val yMax = bodyBounds.minOf { it.max.y }
        val yMin = bodyBounds.maxOf { it.min.y }
        val zMax = bodyBounds.minOf { it.max.z }
        val zMin = bodyBounds.maxOf { it.min.z }
        return Bounds(ThreeDPoint(xMin, yMin, zMin), ThreeDPoint(xMax, yMax, zMax))
    }
}

class ScadDifference(members: List<Scad3D>): ScadModule("difference", members) {
    override fun renderParams(): String? {
        return null
    }

    override fun bounds(): Bounds {
        return body[0].bounds()
    }
}


// Note that this requires dot-scad.
class ScadExtrudedShape(val crossSection: (List<Double>) -> Scad2DShape, val profiles: List<Profile>): Scad3D() {

    val zPositions = profiles.flatMap { it.pos.toSet() }.sorted()

    /**
     * The profile elevations, combined with the scaled loops of the
     * crossSection into a collection of layers, where each layer is
     * a list of the 3d points of the loop positioned in space.
     */
    val layers: List<Pair<Double, Scad2DShape>> =
        zPositions.mapIndexed { i, z ->
            val lows = profiles.map { it(z) }
            val highs = profiles.map { it(z, true) }
            if (i != 0) {
                Pair(z, crossSection(lows))
            } else if (i == 0 || (i < (zPositions.size - 1) && lows != highs)) {
                Pair(z, crossSection(highs))
            } else {
                null
            }
        }.filterNotNull()

    override fun render(i: Int): String {
        val result = StringBuilder()
        val sliceProfiles = layers.map {
            it.second
        }.map { it.render() }.joinToString(", ")
        val zs = layers.map { it.first }
        result.append(indent(i))
        result.append("skin(profiles=[$sliceProfiles], z=[${zs.joinToString(", ")}], slices=10, caps=true) { }\n")
        return result.toString()
    }

    override fun bounds(): Bounds {
        val points = layers.flatMap { l ->
            val z = l.first
            val ext = l.second.extent()
            val maxX = ext.maxX
            val minX = ext.minX
            val maxY = ext.maxY
            val minY = ext.minY
            listOf(ThreeDPoint(minX, minY, z), ThreeDPoint(maxX, maxY, z))
        }
        return pointSetBounds(points)
    }
}

class ScadExtrusion(val zPositions: List<Double>, val shapes: List<Scad2DShape>): Scad3D() {

    val layers: List<Pair<Double, Scad2DShape>> = zPositions.zip(shapes)

    override fun bounds(): Bounds {
        val points = zPositions.zip(layers).flatMap { (z, l) ->
            val z = l.first
            val ext = l.second.extent()
            val maxX = ext.maxX
            val minX = ext.minX
            val maxY = ext.maxY
            val minY = ext.minY
            listOf(ThreeDPoint(minX, minY, z), ThreeDPoint(maxX, maxY, z))
        }
        return pointSetBounds(points)
    }

    override fun render(i: Int): String {
        val result = StringBuilder()
        val sliceProfiles = layers.map {
            it.second
        }.map { it.render() }.joinToString(", ")
        val zs = layers.map { it.first }
        result.append(indent(i))
        result.append("skin(profiles=[$sliceProfiles], slices=10, z=[${zs.joinToString(", ")}], caps=true) { }")
        return result.toString()
    }
}

class ScadPolyhedron(val vertices: List<ThreeDPoint>, val faces: ArrayList<List<Int>>,
                     convexity: Int = 0): Scad3D() {
    override fun render(i: Int): String {
        val vertStr = vertices.map { "[${it.x}, ${it.y}, ${it.z}]" }.joinToString(", ")
        val facesStr = faces.map { face ->
            "[${face.map { it.toString() }.joinToString(", ")}]"
        }.joinToString(", ")
        return "${indent(i)}polyhedron(points=[${vertStr}], faces=[$facesStr]);\n"
    }

    override fun bounds(): Bounds {
        return pointSetBounds(vertices)
    }
}

fun pointSetBounds(points: List<ThreeDPoint>): Bounds {
    val xMax = points.maxOf { it.x }
    val xMin = points.minOf { it.x }
    val yMax = points.maxOf { it.y }
    val yMin = points.minOf { it.y }
    val zMax = points.maxOf { it.z }
    val zMin = points.minOf { it.z }
    return Bounds(
        min = ThreeDPoint(xMin, yMin, zMin),
        max = ThreeDPoint(xMax, yMax, zMax)
    )
}


object Scad3DGeometry: ThreeDGeometry<Scad3D, Scad2DShape> {
    override val lowerGeometry = Scad2D
    override fun extrudeShape(
        shape: (List<Double>) -> Scad2DShape,
        profiles: List<Profile>
    ): Scad3D {
        return ScadExtrudedShape(shape, profiles)
    }

    override fun extrudeShapes(
        zs: List<Double>,
        shapes: List<Scad2DShape>
    ): Scad3D = ScadExtrusion(zs, shapes)


    override fun cylinder(radius: Double, height: Double): Scad3D {
        return ScadStatement(
            "cylinder($height, $radius);",
            Bounds(
                ThreeDPoint(-radius, -radius, 0.0),
                ThreeDPoint(radius, radius, height)
            )
        )
    }

    override fun cube(
        length: Double,
        width: Double,
        height: Double
    ): Scad3D {
        return ScadStatement(
            "cube($length, $width, $height)",
            Bounds(
                ThreeDPoint(0.0, 0.0, 0.0),
                ThreeDPoint(length, width, height)
            )
        )
    }

    override fun block(p1: ThreeDPoint, p2: ThreeDPoint, ramp: Double): Scad3D {
        val verts = java.util.ArrayList<ThreeDPoint>()
        for (x in listOf(p1.x, p2.x)) {
            for (y in listOf(p1.y, p2.y)) {
                verts.add(ThreeDPoint(x, y, p1.z))
            }
        }
        for (x in listOf(p1.x - ramp, p2.x + ramp)) {
            for (y in listOf(p1.y - ramp, p2.y + ramp)) {
                verts.add(ThreeDPoint(x, y, p2.z))
            }
        }
        val faces = java.util.ArrayList<List<Int>>()
        fun quad(
            a: Int,
            b: Int,
            c: Int,
            d: Int,
        ) {
            faces.add(listOf(a, b, c))
            faces.add(listOf(a, c, d))
        }
        for ((a, b, c) in listOf(listOf(1, 2, 4), listOf(4, 1, 2), listOf(2, 4, 1))) {
            quad(0, a, a + b, b)
            quad(c + b, c + a + b, c + a, c + 0)
        }
        return ScadPolyhedron(verts, faces)
    }
}




fun main() {
    val f = { ds: List<Double> ->
        Scad2D.circle(ds[0])
    }
    val profile = Profile(
        listOf(0.0, 5.0, 10.0, 20.0, 30.0),
        listOf(10.0, 12.0, 14.0, 16.0, 18.0),
        listOf(12.0, 14.0, 16.0, 18.0, 20.0)
    )
    val s = ScadExtrudedShape(f, listOf(profile))
    val result = s.toText()
    System.err.println(result)
}


