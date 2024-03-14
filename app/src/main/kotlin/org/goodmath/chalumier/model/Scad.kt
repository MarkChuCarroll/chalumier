package org.goodmath.chalumier.model

import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.design.Profile

data class Conic(
    val height: Double,
    val lowerDiam: Double,
    val upperDiam: Double = lowerDiam
)

data class Hole(
    val elevation: Double,
    val diameter: Double
)

data class Embouchure(
    val elevation: Double,
    val diameter: Double,
    val eccentricity: Double)



interface Geom {
    fun ind(i: Int): String {
        return "   ".repeat(i)
    }
    fun render(i: Int): String
}


class Cylinder(
    val height: Double,
    val lowerDiam: Double,
    val upperDiam: Double = lowerDiam,
    val facets: Int = 0): Geom {

    override fun render(i: Int): String {
        return "${ind(i)}cylinder(h=${height}, " +
                "r1=${lowerDiam}, r2=${upperDiam}," +
                "\$fn=${facets});\n"
    }
}

class Labelled(val comment: String, val geom: Geom): Geom {
    override fun render(i: Int): String {
        return "${ind(i)}// $comment\n${this.geom.render(i)}"
    }
}

abstract class GeoModule(val name: String, geos: List<Geom>): Geom {
    val geometries = ArrayList<Geom>(geos)

    abstract fun renderParams(): String

    override fun render(i: Int): String {
        return "${ind(i)}${name}(${renderParams()}) {\n" +
                geometries.map { it.render(i + 1) }.joinToString("\n") +
                "${ind(i)}}\n"
    }

    fun add(geom: Geom) {
        this.geometries.add(geom)
    }
}

class Union(geoms: List<Geom>): GeoModule("union", geoms) {
    override fun renderParams(): String {
        return ""
    }
}

class Difference(geoms: List<Geom>): GeoModule("difference", geoms) {

    override fun renderParams(): String {
        return ""
    }


}

class Intersection(geoms: List<Geom>): GeoModule("intersection", geoms) {

    override fun renderParams(): String {
        return ""
    }

}

data class ThreeD(val x: Double, val y: Double, val z: Double) {
    override fun toString(): String {
        return "[$x, $y, $z]"
    }
}

class Translate(val offset: ThreeD, geoms: List<Geom> ): GeoModule("translate", geoms) {

    override fun renderParams(): String {
        return offset.toString()
    }
}

class Rotate(val rotation: ThreeD, geoms: List<Geom> ): GeoModule("rotate", geoms) {

    override fun renderParams(): String {
        return rotation.toString()
    }
}

class Scale(val scale: ThreeD, geoms: List<Geom> ): GeoModule("scale", geoms) {
    override fun renderParams(): String {
        return scale.toString()
    }
}


