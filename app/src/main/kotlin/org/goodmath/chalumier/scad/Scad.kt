package org.goodmath.chalumier.scad

data class ThreeDimensionalValue(val x: Double, val y: Double, val z: Double) {
    override fun toString(): String {
        return "[$x, $y, $z]"
    }
}

data class Hole(
    val elevation: Double, val diameter: Double
)

interface Shape {
    fun ind(i: Int): String {
        return "   ".repeat(i)
    }

    fun render(i: Int): String
}

class Cylinder(
    val height: Double, val lowerDiam: Double, val upperDiam: Double = lowerDiam, val facets: Int = 0
) : Shape {

    override fun render(i: Int): String {
        return "${ind(i)}cylinder(h=${height}, " + "r1=${lowerDiam}, r2=${upperDiam}," + "\$fn=${facets});\n"
    }
}

class Labelled(val comment: String, val shape: Shape) : Shape {
    override fun render(i: Int): String {
        return "${ind(i)}// $comment\n${this.shape.render(i)}"
    }
}

abstract class Module(val name: String, geos: List<Shape>) : Shape {
    val geometries = ArrayList<Shape>(geos)

    abstract fun renderParams(): String

    override fun render(i: Int): String {
        return "${ind(i)}${name}(${renderParams()}) {\n" + geometries.map { it.render(i + 1) }
            .joinToString("\n") + "${ind(i)}}\n"
    }

    fun add(shape: Shape) {
        this.geometries.add(shape)
    }
}

class Union(shapes: List<Shape>) : Module("union", shapes) {
    override fun renderParams(): String {
        return ""
    }
}

class Difference(shapes: List<Shape>) : Module("difference", shapes) {

    override fun renderParams(): String {
        return ""
    }
}

class Intersection(shapes: List<Shape>) : Module("intersection", shapes) {

    override fun renderParams(): String {
        return ""
    }

}

class Translate(val offset: ThreeDimensionalValue, shapes: List<Shape>) : Module("translate", shapes) {

    override fun renderParams(): String {
        return offset.toString()
    }
}

class Rotate(val rotation: ThreeDimensionalValue, shapes: List<Shape>) : Module("rotate", shapes) {

    override fun renderParams(): String {
        return rotation.toString()
    }
}

class Scale(val scale: ThreeDimensionalValue, shapes: List<Shape>) : Module("scale", shapes) {
    override fun renderParams(): String {
        return scale.toString()
    }
}


