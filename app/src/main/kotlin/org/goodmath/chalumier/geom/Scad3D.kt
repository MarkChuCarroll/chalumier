package org.goodmath.chalumier.geom

import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.errors.InvalidOperationException
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
import kotlin.text.StringBuilder
import kotlin.text.repeat


interface Transform<T: Transform<T>> {
    fun preWrap(): List<String>
    fun postWrap(): List<String>
    operator fun invoke(bounds: Bounds3D): Bounds3D
    val kind: String
    fun mergeWith(other: T): T
    val priority: Int
}

data class Label(val label: String): Transform<Label>{
    override val kind: String = "label"
    override val priority = 100
    override fun mergeWith(other: Label): Label {
        return Label(label + other.label)
    }

    override fun preWrap(): List<String> {
        return listOf("// begin($label)")
    }

    override fun postWrap(): List<String> {
        return listOf("// end($label)")
    }

    override fun invoke(bounds: Bounds3D): Bounds3D {
        return bounds
    }
}

data class Translation(val x: Double, val y: Double, val z: Double): Transform<Translation> {
    override val kind = "translation"
    override val priority: Int = 90
    override fun mergeWith(other: Translation): Translation {
        return Translation(x + other.x, y + other.y, z + other.z)
    }

    override fun preWrap(): List<String> {
        return listOf("translate([$x, $y, $z]) {")
    }

    override fun postWrap(): List<String> {
        return listOf("}")
    }

    override operator fun invoke(bounds: Bounds3D): Bounds3D {
        val translation = ThreeDPoint(x, y, z)
        return Bounds3D(bounds.min + translation, bounds.max + translation)
    }


}

data class Rotation(val x: Double, val y: Double, val z: Double): Transform<Rotation> {
    override val priority: Int = 50

    override val kind = "rotation"
    override fun mergeWith(other: Rotation): Rotation {
        return Rotation(x + other.x, y + other.y, z + other.z)
    }

    override fun preWrap(): List<String> {
        return listOf("rotate([$x, $y, $z]) {")
    }

    override fun postWrap(): List<String> {
        return listOf("}")
    }

    override fun invoke(bounds: Bounds3D): Bounds3D {
        return Bounds3D(bounds.min.rotate(x, y, z), bounds.max.rotate(x, y, z))
    }
}

data class Scale(val x: Double, val y: Double, val z: Double): Transform<Scale> {
    override val kind = "scale"
    override val priority: Int     =60
    override fun mergeWith(other: Scale): Scale {
        return Scale(x*other.x, y*other.y, z*other.z)
    }
    override fun preWrap(): List<String> {
        return listOf("scale([$x, $y, $z]) {")
    }

    override fun postWrap(): List<String> {
        return listOf("}")
    }

    override fun invoke(bounds: Bounds3D): Bounds3D {
        return Bounds3D(bounds.min.scaleBy(ThreeDPoint(x, y, z)), bounds.max.scaleBy(ThreeDPoint(x, y, z)))
    }

}


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


    private fun rotateInPlane(x: Double, y: Double, rotationDegrees: Double): Pair<Double, Double> {
        val theta = atan2(y, x)
        val mag = hypot(x, y)
        val newAngle = theta + (rotationDegrees*PI/180.0)
        return Pair(x * cos(newAngle), y * sin(newAngle))
    }

    fun rotateVector(xAngle: Double, yAngle: Double, zAngle: Double): ThreeDPoint {
        // For computing the effect of the X rotation, we're looking
        // at the YZ plane - that is, Y is acting as the horizontal (X),
        // and Z as the vertical (Y).
        val (zAfterX, yAfterX) = rotateInPlane(z, y, xAngle)
        val afterX = ThreeDPoint(x, yAfterX, zAfterX)
        // For the Y rotation, it's the XZ plane.
        val (xAfterY, zAfterY) = rotateInPlane(afterX.x, afterX.z, yAngle)
        val afterY = ThreeDPoint(xAfterY, afterX.y, zAfterY)
        val (xAfterZ, yAfterZ) = rotateInPlane(afterY.x, afterY.y, zAngle)
        return ThreeDPoint(xAfterZ, yAfterZ, afterY.z)
    }



    // Treating the point like a vector, rotate it.
    fun rotate(xAngle: Double, yAngle: Double, zAngle: Double): ThreeDPoint {
        val xNorm = normalizeAngle(xAngle)
        val yNorm = normalizeAngle(yAngle)
        val zNorm = normalizeAngle(zAngle)
        if (xNorm % 90.0 == 0.0 && yNorm % 90.0 == 0.0 && zNorm % 90.0 == 0.0) {
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
        } else {
            return rotateVector(xNorm, yNorm, zNorm)
        }
    }

    fun scaleBy(xFactor: Double, yFactor: Double, zFactor: Double): ThreeDPoint {
        return ThreeDPoint(x*xFactor, y*yFactor, z*zFactor)
    }

    companion object {
        val ZERO = ThreeDPoint(0.0, 0.0, 0.0)
        val ONE = ThreeDPoint(1.0, 1.0, 1.0)
    }
}

abstract class Scad3D(transforms: List<Transform<*>>): ThreeDBody<Scad3D> {
    private val transformsMap: MutableMap<String, Transform<*>> = transforms.associateBy { it::class.java.name }.toMutableMap()


    fun<T: Transform<T>> addTransform(newT: T) {
        val t = transformsMap[newT.kind]
        if (t == null) {
            transformsMap[newT.kind] = newT
        } else {
            t as T
            transformsMap[newT.kind] = t.mergeWith(newT)
        }
    }

    override fun label(label: String) {
        addTransform(Label(label))
    }
    override fun toText(): String {
        val result = StringBuilder()
        result.append("include <bosl2/std.scad>\n\n")
        result.append(render(0))
        return result.toString()
    }

    override fun save(dir: Path, filename: String) {
        (dir / "$filename.scad").writeText(toText())
    }

    val transforms: List<Transform<*>>
        get() = transformsMap.values.sortedBy { it.priority }

    open fun render(i: Int): String {
        var ind = i
        val result = StringBuilder()

        for (transform in transforms) {
            for (line in transform.preWrap()) {
                result.append(indent(i))
                result.append(line)
                result.append("\n")
                ind++
            }
        }
        result.append(renderCore(ind))
        for (transform in transforms) {
            ind--
            for (line in transform.postWrap()) {
                result.append(indent(ind))
                result.append(line)
                result.append("\n")
            }
        }
        return result.toString()
    }

    abstract fun renderCore(i: Int): String

    abstract fun coreBounds(): Bounds3D

    fun indent(i: Int): String = "   ".repeat(i)

    override fun bounds(): Bounds3D {
        var cb: Bounds3D = coreBounds()
        for (t in transforms) {
            cb = t(cb)
        }
        return cb
    }


    override fun rotate(x: Double, y: Double, z: Double) {
        addTransform(Rotation(x, y, z))
    }

    override fun translate(x: Double, y: Double, z: Double) {
        addTransform(Translation(x, y, z))
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

    override fun scale(factor: Double) {
        addTransform(Scale(factor, factor, factor))
    }

    override fun positionNicely(): Scad3D {
        val result = copy()
        result.translate(-0.5 * (bounds().min.x + bounds().max.x),
                    -0.5 * (bounds().min.y + bounds().max.y),
                    -bounds().min.z,
                )
        return result
    }
}

class ScadStatement(val stmt: String,
                    val bound: Bounds3D,
                    transforms: List<Transform<*>> = emptyList()
): Scad3D(transforms) {

    override fun renderCore(i: Int): String {
        return "${indent(i)}$stmt;\n"
    }

    override fun copy(): Scad3D {
        return ScadStatement(stmt, bound, transforms)
    }

    override fun coreBounds(): Bounds3D {
        return bound
    }
}

abstract class ScadModule(
    val name: String,
    val body: List<Scad3D>,
    transforms: List<Transform<*>> = emptyList()): Scad3D(transforms) {
    abstract fun renderParams(): String?

    override fun renderCore(i: Int): String {
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


class ScadUnion(members: List<Scad3D>,
    transforms: List<Transform<*>> = emptyList()): ScadModule("union", members, transforms) {
    override fun renderParams(): String? {
        return null
    }

    override fun coreBounds(): Bounds3D {
        val bodyBounds = body.map { it.bounds() }
        val xMax = bodyBounds.maxOf { it.max.x }
        val xMin = bodyBounds.minOf { it.min.x }
        val yMax = bodyBounds.maxOf { it.max.y }
        val yMin = bodyBounds.minOf { it.min.y }
        val zMax = bodyBounds.maxOf { it.max.z }
        val zMin = bodyBounds.minOf { it.min.z }
        return Bounds3D(ThreeDPoint(xMin, yMin, zMin), ThreeDPoint(xMax, yMax, zMax))
    }

    override fun copy(): Scad3D {
        return ScadUnion(body, transforms)
    }
}

class ScadIntersection(members: List<Scad3D>,
    transforms: List<Transform<*>> = emptyList()): ScadModule("intersection", members, transforms) {
    override fun renderParams(): String? {
        return null
    }

    override fun coreBounds(): Bounds3D {
        val bodyBounds = body.map { it.bounds() }
        val xMax = bodyBounds.minOf { it.max.x }
        val xMin = bodyBounds.maxOf { it.min.x }
        val yMax = bodyBounds.minOf { it.max.y }
        val yMin = bodyBounds.maxOf { it.min.y }
        val zMax = bodyBounds.minOf { it.max.z }
        val zMin = bodyBounds.maxOf { it.min.z }
        return Bounds3D(ThreeDPoint(xMin, yMin, zMin), ThreeDPoint(xMax, yMax, zMax))
    }

    override fun copy(): Scad3D {
        return ScadIntersection(body, transforms)
    }
}

class ScadDifference(members: List<Scad3D>, transforms: List<Transform<*>> = emptyList()): ScadModule("difference", members, transforms) {
    override fun renderParams(): String? {
        return null
    }

    override fun coreBounds(): Bounds3D {
        return body[0].bounds()
    }

    override fun copy(): Scad3D {
        return ScadDifference(body, transforms)
    }
}


// Note that this requires dot-scad.
class ScadExtrudedShape(val crossSection: (List<Double>) -> Scad2DShape,
                        val profiles: List<Profile>,
    transforms: List<Transform<*>> = emptyList()): Scad3D(transforms) {

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
            }
            if (i == 0 || (i < (zPositions.size - 1) && lows != highs)) {
                Pair(z, crossSection(highs))
            } else {
                null
            }
        }.filterNotNull()

    override fun renderCore(i: Int): String {
        val result = StringBuilder()
        val h0 = layers[0].first
        layers.zipWithNext().forEach { (low, high) ->
            val (lowHeight, lowProf) = low
            val (highHeight, highProf) = high
            result.append(indent(i))
            result.append(
                "translate([0.0, 0.0, ${lowHeight-h0}]) linear_extrude(${highHeight - lowHeight}, scale=${highProf.extent().maxX / lowProf.extent().maxX}) {\n"
            )
            result.append(indent(i + 1))
            result.append(lowProf.render())
            result.append("\n")
            result.append(indent(i))
            result.append("}\n")
        }
  //      result.append(indent(i))

//        result.append("skin(profiles=[$sliceProfiles], z=[${zs.joinToString(", ")}], slices=10, caps=true) { }\n")
        return result.toString()
    }

    override fun coreBounds(): Bounds3D {
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

    override fun copy(): Scad3D {
        TODO("Not yet implemented")
    }
}

class ScadExtrusion(val zPositions: List<Double>, val shapes: List<Scad2DShape>,
    transforms: List<Transform<*>> = emptyList()): Scad3D(transforms) {

    val layers: List<Pair<Double, Scad2DShape>> = zPositions.zip(shapes)

    override fun coreBounds(): Bounds3D {
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

    override fun copy(): Scad3D {
        return ScadExtrusion(zPositions, shapes, transforms)
    }

    override fun renderCore(i: Int): String {
        val result = StringBuilder()
        val h0 = layers[0].first
        layers.zipWithNext().forEach { (low, high) ->
            val (lowHeight, lowProf) = low
            val (highHeight, highProf) = high
            result.append(indent(i))
            result.append(
                "translate([0, 0, ${lowHeight-h0}]) linear_extrude(${highHeight - lowHeight}, scale=${highProf.extent().maxX / lowProf.extent().maxX}) {\n"
            )
            result.append(indent(i + 1))
            result.append(lowProf.render())
            result.append("\n")
            result.append(indent(i))
            result.append("}\n")
        }
        return result.toString()
    }
}

class ScadPolyhedron(val vertices: List<ThreeDPoint>, val faces: ArrayList<List<Int>>,
                     val convexity: Int = 0,
    transforms: List<Transform<*>> = emptyList()): Scad3D(transforms) {
    override fun renderCore(i: Int): String {
        val vertStr = vertices.map { "[${it.x}, ${it.y}, ${it.z}]" }.joinToString(", ")
        val facesStr = faces.map { face ->
            "[${face.map { it.toString() }.joinToString(", ")}]"
        }.joinToString(", ")
        return "${indent(i)}polyhedron(points=[${vertStr}], faces=[$facesStr]);\n"
    }

    override fun coreBounds(): Bounds3D {
        return pointSetBounds(vertices)
    }

    override fun copy(): Scad3D {
        return ScadPolyhedron(vertices, faces, convexity, transforms)
    }
}

fun pointSetBounds(points: List<ThreeDPoint>): Bounds3D {
    val xMax = points.maxOf { it.x }
    val xMin = points.minOf { it.x }
    val yMax = points.maxOf { it.y }
    val yMin = points.minOf { it.y }
    val zMax = points.maxOf { it.z }
    val zMin = points.minOf { it.z }
    return Bounds3D(
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
            Bounds3D(
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
            Bounds3D(
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


