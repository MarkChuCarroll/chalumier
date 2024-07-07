package org.goodmath.chalumier.geom

import org.goodmath.chalumier.design.Profile
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

operator fun StringBuilder.plusAssign(s: Any) {
    this.append(s)
}

interface ScadValue {
    enum class ValueKind {
        Number, Vector, String, Boolean, Expression
    }
    val kind: ValueKind
    fun render(): String
}

class BScadExpression(val expr: String): ScadValue {
    override val kind: ScadValue.ValueKind = ScadValue.ValueKind.Boolean
    override fun render(): String {
        return expr
    }
}


class ScadBoolean(val value: Boolean): ScadValue {
    override val kind: ScadValue.ValueKind = ScadValue.ValueKind.Boolean
    override fun render(): String {
        return value.toString()
    }

}

class ScadNumber(val value: Double): ScadValue {
    override val kind: ScadValue.ValueKind = ScadValue.ValueKind.Number
    override fun render(): String {
        return value.toString()
    }

}

class ScadVector(val values: List<ScadValue>, val memberKind: ScadValue.ValueKind): ScadValue {
    override val kind: ScadValue.ValueKind = ScadValue.ValueKind.Vector
    override fun render(): String {
        return "[${values.joinToString { ", " }}]"
    }
}

class ScadString(val value: String): ScadValue {
    override val kind = ScadValue.ValueKind.String
    override fun render(): String {
        return "\"$value\""
    }
}

fun doIndent(b: StringBuilder, indent: Int) {
     b + "  ".repeat(indent)
}

abstract class BScadTransform(val name: String, val parameters: List<ScadParameter>,
   val children: List<BScadBody>? = null) {

    open fun renderPre(b: StringBuilder, indent: Int) {
        doIndent(b, indent)
        b.append("name(${parameters.joinToString(", ") { it.render() }}) {\n")
    }

    open fun renderPost(b: StringBuilder, indent: Int) {
        if (children != null) {
            for (child in children) {
                b.append(child.render(b, indent+1))
            }
        }
        doIndent(b, indent)
        b.append("} // $name\n")
    }

    abstract fun transform(bounds: Bounds3D): Bounds3D

}

class BScadTranslate(val x: Double, val y: Double, val z: Double,
                     children: List<BScadBody>?=null): BScadTransform("translate",
    listOf(ScadParameter(
        ScadVector(listOf(ScadNumber(x), ScadNumber(y), ScadNumber(z)),
            ScadValue.ValueKind.Number))), children) {
    override fun transform(bounds: Bounds3D): Bounds3D {
        return bounds.offset(x, y, z)
    }
}

fun param(d: Double, name: String? = null): ScadParameter =
    ScadParameter(ScadNumber(d), name)

fun param(ds: List<Double>, name: String? = null): ScadParameter =
    ScadParameter(ScadVector(ds.map { ScadNumber(it) }, ScadValue.ValueKind.Number), name)

fun param(s: String, name: String? = null): ScadParameter =
    ScadParameter(ScadString(s), name)

fun param(b: Boolean, name: String? = null): ScadParameter = ScadParameter(ScadBoolean(b), name)

fun vlParam(values: List<List<Double>>, name: String? = null): ScadParameter {
    return ScadParameter(ScadVector(
        values.map { vs ->
            ScadVector(vs.map {
                ScadNumber(it)
            }, ScadValue.ValueKind.Number)
        },
        ScadValue.ValueKind.Vector
    ), name)
}


class BScadScale(val x: Double, val y: Double, val z: Double,
                 children: List<BScadBody>?=null): BScadTransform("scale",
    listOf(param(listOf(x, y, z))), children) {
    override fun transform(bounds: Bounds3D): Bounds3D {
        return Bounds3D(bounds.min.scaleBy(x, y, z), bounds.max.scaleBy(x, y, z))
    }
}

class BScadRotate(val x: Double, val y: Double, val z:Double,
    children: List<BScadBody>? = null): BScadTransform("rotate",
    listOf(param(listOf(x, y, z))), children) {
    override fun transform(bounds: Bounds3D): Bounds3D {
        return pointSetBounds(listOf(bounds.min.rotate(x, y, z), bounds.max.rotate(x, y, z)))
    }
}

class BScadLabel(val label: String): BScadTransform("label", emptyList()) {
    override fun transform(bounds: Bounds3D): Bounds3D = bounds

    override fun renderPre(b: StringBuilder, indent: Int) {
        doIndent(b, indent)
        b.append("// begin($label)\n")
    }

    override fun renderPost(b: StringBuilder, indent: Int) {
        doIndent(b, indent)
        b.append("// end($label)\n")
    }
}

class BScadColor(val color: String, children: List<BScadBody>? = null): BScadTransform("color", listOf(param(color)), children) {
    override fun transform(bounds: Bounds3D): Bounds3D = bounds
}

abstract class BScadBody(
    val name: String,
    val parameters: List<ScadParameter>,
    withChildren: List<BScadBody> = emptyList(),
    withTransforms: List<BScadTransform> = emptyList()): ThreeDBody<BScadBody> {
    val children = ArrayList(withChildren)
    val transforms = ArrayList(withTransforms)

    fun addTransform(t: BScadTransform) {
        transforms.add(t)
    }

    fun render(b: StringBuilder, indent: Int) {
        var ind = indent
        for (transform in transforms.reversed()) {
            transform.renderPre(b, ind)
            ind++
        }
        doIndent(b, ind)
        renderCore(b, ind)
        for (transform in transforms) {
            ind--
            transform.renderPost(b, ind)
        }
    }

    abstract fun renderCore(b: StringBuilder, indent: Int)

    override fun label(label: String) {
        addTransform(BScadLabel(label))
    }

    override fun save(dir: Path, filename: String) {
        (dir / "${filename}.scad").writeText(toText())
    }

    override fun toText(): String {
        val result = StringBuilder()
        render(result, 0)
        return result.toString()
    }

    override fun scale(factor: Double) {
        addTransform(BScadScale(factor, factor, factor))
    }

    override fun rotate(x: Double, y: Double, z: Double) {
        addTransform(BScadRotate(x, y, z))
    }

    override fun translate(x: Double, y: Double, z: Double) {
        addTransform(BScadTranslate(x, y, z))
    }

    override fun positionNicely(): BScadBody {
        TODO("Not yet implemented")
    }

    override fun intersect(other: BScadBody): BScadBody {
        return BScadIntersection(listOf(this, other))
    }

    override fun difference(other: BScadBody): BScadBody {
        return BScadDifference(listOf(this, other))
    }

    override fun union(other: BScadBody): BScadBody {
        return BScadUnion(listOf(this, other))
    }

}

abstract class BScadCompound(val operator: String,
                             parameters: List<ScadParameter> = emptyList(),
                             children: List<BScadBody> = emptyList(),
                             transforms: List<BScadTransform> = emptyList()):
    BScadBody(operator, parameters, children, transforms) {

    override fun renderCore(b: StringBuilder, indent: Int) {
        doIndent(b, indent)
        b.append("$operator(${parameters.joinToString(", ") { it.render() }})")
        if (children.isNotEmpty()) {
            b.append("{\n")
            for (child in children) {
                child.render(b, indent + 1)
            }
            doIndent(b, indent)
            b.append("}\n")
        } else {
            b.append(";\n")
        }
    }
}

class BScadUnion(children: List<BScadBody>, transforms: List<BScadTransform> = emptyList()):
    BScadCompound("union", parameters = emptyList(), children=children, transforms = transforms) {
    override fun copy(): BScadBody {
        return BScadUnion(children, transforms)
    }

    override fun bounds(): Bounds3D {
        return pointSetBounds(children.flatMap { child ->
            val cb = child.bounds()
            listOf(cb.min, cb.max)
        })
    }
}

class BScadIntersection(children: List<BScadBody>, transforms: List<BScadTransform> = emptyList()):
    BScadCompound("intersection", parameters = emptyList(), children=children, transforms = transforms) {
    override fun copy(): BScadBody {
        return BScadUnion(children, transforms)
    }

    override fun bounds(): Bounds3D {
        val childBounds = children.map { it.bounds() }
        val maxima = childBounds.map { it.max }
        val minima = childBounds.map { it.min }
        val maxX = maxima.minOf { it.x}
        val maxY = maxima.minOf { it.y }
        val maxZ = maxima.minOf { it.z }
        val minX = minima.maxOf { it.x}
        val minY = minima.maxOf { it.y }
        val minZ = minima.maxOf { it.z }
        return Bounds3D(ThreeDPoint(minX, minY, minZ), ThreeDPoint(maxX, maxY, maxZ))
    }

}

class BScadDifference(children: List<BScadBody>, transforms: List<BScadTransform> = emptyList()):
    BScadCompound("difference", parameters = emptyList(), children=children, transforms = transforms) {
    override fun copy(): BScadBody {
        return BScadUnion(children, transforms)
    }

    override fun bounds(): Bounds3D {
        return children[0].bounds()
    }
}

data class ScadParameter(val value: ScadValue, val name: String? = null) {
    fun render(): String {
        return name?.let { "$it=${value.render()}" } ?: value.render()
    }
}

abstract class BScadSimpleBody(val function: String, val args: List<ScadParameter>,
                               transforms: List<BScadTransform> = emptyList()): BScadBody(
    function, args, withTransforms=transforms
) {
    override fun renderCore(b: StringBuilder, indent: Int) {
        doIndent(b, indent)
        b.append("$function(${args.joinToString(", ") { it.render() }})")
    }
}

class BScadCylinder(val h: Double, var r1: Double, val r2: Double,
    transforms: List<BScadTransform> = emptyList()): BScadSimpleBody("cylinder",
    listOf(param(h, "h"), param(r1, "r1"), param(r2, "r2"), param(true, "center")),
    transforms) {
    override fun bounds(): Bounds3D =
        pointSetBounds(listOf(ThreeDPoint(-r1/2.0, -r1/2.0, -h/2.0),
            ThreeDPoint(r1/2.0, r1/2.0, -h/2.0),
            ThreeDPoint(r1/2.0, r1/2.0, h/2.0)))


    override fun copy(): BScadBody = BScadCylinder(h, r1, r2, transforms)
}

class BScadBox(val width: Double, val height: Double, val depth: Double,
               transforms: List<BScadTransform> = emptyList()): BScadSimpleBody("cube",
    listOf(param(listOf(width, height, depth)), param(true, "center")), transforms) {
    override fun bounds(): Bounds3D =
        pointSetBounds(listOf(ThreeDPoint(-width/2.0, -height/2.0, -depth/2.0),
        ThreeDPoint(width/2.0, height/2.0, depth/2.0)))

    override fun copy(): BScadBody {
        return BScadBox(width, height, depth, transforms)
    }
}

fun pointListParam(points: List<ThreeDPoint>,
          name: String?=null): ScadParameter {
    val pointVecs = points.map { listOf(it.x, it.y, it.z) }
    return vlParam(pointVecs, name)
}

class BScadPolygon(val points: List<ThreeDPoint>,
    transforms: List<BScadTransform>): BScadSimpleBody("polygon",
    listOf(pointListParam(points)), transforms) {

    override fun bounds(): Bounds3D = pointSetBounds(points)

    override fun copy(): BScadBody {
        return BScadPolygon(points, transforms)
    }

}

class BScadExtrusion(val profile: Scad2DShape, val layers: List<Double>,
    val height: Double, val scale: Double, transforms: List<BScadTransform>): BScadBody("extrusion",
        listOf(param(height), param(scale, "scale")), withTransforms=transforms) {
    override fun renderCore(b: StringBuilder, indent: Int) {
        doIndent(b, indent)
        b += "linear_extrude($height, scale=$scale) {\n"
        doIndent(b, indent+1)
        b += profile.render()
        b += "\n"
        doIndent(b, indent)
        b += "}\n"
    }

    override fun copy(): BScadBody {
        return BScadExtrusion(profile, layers, height, scale, transforms)
    }

    override fun bounds(): Bounds3D {
        val profileBounds = profile.extent()
        return pointSetBounds(listOf(ThreeDPoint(profileBounds.minX, profileBounds.minY, 0.0),
            ThreeDPoint(profileBounds.maxX*scale, profileBounds.maxY*scale, height)))
    }
}




object BetterScad: ThreeDGeometry<BScadBody, Scad2DShape> {
    override val lowerGeometry: TwoDGeometry<Scad2DShape> = Scad2D


    override fun cylinder(radius: Double, height: Double): BScadBody {
        return BScadCylinder(height, radius, radius)
    }

    override fun cube(width: Double, height: Double, depth: Double): BScadBody {
        return BScadBox(width, height, depth)
    }

    override fun block(p1: ThreeDPoint, p2: ThreeDPoint, ramp: Double): BScadBody {
        val result = BScadBox(p2.x - p1.x, p2.y-p1.y, p2.z-p1.z)
        result.translate(p1.x, p1.y, p1.z)
        return result
    }

    override fun extrudeShapes(zs: List<Double>, shape: Scad2DShape, sizes: List<Pair<Double, Double>>): BScadBody {

    }

    override fun extrudeShape(crossSection: (List<Double>) -> Scad2DShape, profiles: List<Profile>): BScadBody {
        val zs = profiles.flatMap {  it.pos.toSet() }.sorted()
        val layers = ArrayList<Pair<Double, Scad2DShape>>()
        zs.forEachIndexed { i, z ->
            val lows =  profiles.map { it(z) }
            val highs = profiles.map { it(z, true) }
            if (i != 0) {
                layers.add(Pair(z, crossSection(lows)))
            }
            if (i == 0 || (i < (zs.size - 1) && lows != highs)) {
                layers.add(Pair(z, crossSection(highs)))
            }
        }
        val stack = ArrayList<Pair<Double, BScadBody>>()
        val layerProfiles = layers.map { (_, p) -> p.render() }
        val ps = layers.map { (z, _) -> z }
        return BScadExtrusion()



    }

    fun extrudeStackSection(shape: Scad2D, lowDiam: Double, highDiam: Double, pos: Double): BScadBody  {

    }

}
