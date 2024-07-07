package org.goodmath.chalumier.geom

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.jcsg.Polyhedron
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.shape.Loop
import org.goodmath.chalumier.shape.circleCrossSection
import org.goodmath.chalumier.shape.csgHalfRoundedRectangle
import org.goodmath.chalumier.shape.csgRectangle
import org.goodmath.chalumier.shape.csgRoundedRectangle
import org.goodmath.chalumier.shape.csgSquaredCircle
import org.goodmath.chalumier.shape.extrudeProfile
import org.goodmath.chalumier.shape.extrusion
import org.goodmath.chalumier.util.Point
import kotlin.io.path.div
import kotlin.io.path.writeText

object CSG2DGeometry: TwoDGeometry<CSG2D> {
    override fun circle(
        diameter: Double,
        origin: Point
    ): CSG2D {
        return circle(diameter)
    }

    override fun ellipse(
        width: Double,
        height: Double,
        origin: Point
    ): CSG2D = circle(width).scale2(1.0, height/width)


    override fun square(
        side: Double,
        origin: Point
    ): CSG2D =
        rectangle(
            side, side, origin)

    override fun rectangle(
        width: Double,
        height: Double,
        origin: Point
    ): CSG2D = CSG2D(
        csgRectangle(
            Point(origin.x - width / 2.0, origin.y - height / 2.0),
            Point(origin.x + width / 2.0, origin.y + height / 2.0)
        )
    )

    override fun roundedRectangle(
        width: Double,
        height: Double,
        diameter: Double,
        origin: Point
    ): CSG2D = CSG2D(
        csgRoundedRectangle(
            Point(origin.x - width / 2.0, origin.y - height / 2.0),
            Point(origin.x + width / 2.0, origin.y + height / 2.0), diameter
        )
    )

    override fun squaredCircle(
        xPad: Double, yPad: Double,
        diameter: Double,
        origin: Point
    ): CSG2D = CSG2D(csgSquaredCircle(xPad, yPad, diameter))

    override fun halfRoundedRectangle(
        width: Double,
        height: Double,
        origin: Point
    ): CSG2D =
        CSG2D(
            csgHalfRoundedRectangle(
                Point(origin.x - width / 2.0, origin.y - height / 2.0),
                Point(origin.x + width / 2.0, origin.y + height / 2.0)
            )
        )

    override fun polygon(
        points: List<Point>,
        origin: Point
    ): CSG2D = CSG2D(Loop(points))

}


class CSG3D(var csg: CSG): ThreeDBody<CSG3D> {
    override fun copy(): CSG3D {
        return CSG3D(csg)
    }

    override fun label(label: String) {
    }

    override fun save(dir: java.nio.file.Path, filename: String) {
        (dir / "$filename.stl").writeText(toText())
    }

    override fun toText(): String {
        return csg.toStlString()
    }

    override fun scale(factor: Double) {
        csg = csg.transformed(Transform().scale(factor))
    }

    override fun rotate(x: Double, y: Double, z: Double) {
        csg=
            csg.transformed(
                Transform()
                    .rotX(x)
                    .rotY(y)
                    .rotZ(z)
            )
    }

    override fun translate(
        x: Double,
        y: Double,
        z: Double
    ) {
        csg =
            csg.transformed(
                Transform()
                    .translate(x, y, z)
            )

    }

    override fun union(other: CSG3D): CSG3D {
        return CSG3D(csg.union(other.csg))
    }

    override fun difference(other: CSG3D): CSG3D {
        return CSG3D(csg.difference(other.csg))
    }

    override fun intersect(other: CSG3D): CSG3D {
        return CSG3D(csg.intersect(other.csg))
    }

    override fun bounds(): Bounds3D {
        val b = csg.bounds
        return Bounds3D(
            ThreeDPoint(b.min.x, b.min.y, b.min.z),
            ThreeDPoint(b.max.x, b.max.y, b.max.z)
        )
    }

    override fun positionNicely(): CSG3D {
        return CSG3D(
            csg.transformed(
                Transform()
                    .translate(
                        -0.5 * (bounds().min.x + bounds().max.x),
                        -0.5 * (bounds().min.y + bounds().max.y),
                        -bounds().min.z,
                    ),
            )
        )
    }
}

class CSG2D(val loop: Loop): TwoDShape<CSG2D> {
    override fun scale(factor: Double): CSG2D {
        return CSG2D(loop.scale(factor))
    }

    override fun scale2(xFactor: Double, yFactor: Double): CSG2D {
        return CSG2D(loop.scale2(xFactor, yFactor))
    }

    override fun extent(): Bounds2D {
        val ext = loop.extent()
        return Bounds2D(ext.xMax, ext.xMax, ext.yMin, ext.yMax)
    }

    override fun area(): Double {
        return loop.area
    }


    override fun circumference(): Double {
        return loop.circumference
    }

    override fun withArea(targetArea: Double): CSG2D {
        return CSG2D(loop.withArea(targetArea))
    }

    override fun withCircumference(target: Double): CSG2D {
        return CSG2D(loop.withCircumference(target))
    }

    override fun offset(x: Double, y: Double): CSG2D {
        return CSG2D(loop.offset(x, y))
    }
}

object CSGGeometry: ThreeDGeometry<CSG3D, CSG2D> {
    override val lowerGeometry: TwoDGeometry<CSG2D> = CSG2DGeometry

    override fun extrudeShape(
        shape: (List<Double>) -> CSG2D,
        profiles: List<Profile>
    ): CSG3D {
        val cs = { d: List<Double> -> shape(d).loop }
        return CSG3D(
            extrudeProfile(
                *profiles.toTypedArray(),
                crossSection = cs
            )
        )
    }

    override fun extrudeShapes(
        zs: List<Double>,
        shapes: List<CSG2D>
    ): CSG3D {
        return CSG3D(extrusion(zs, shapes.map { it.loop }))
    }

    override fun cylinder(radius: Double, height: Double): CSG3D {

        return CSG3D(
            extrudeProfile(Profile(listOf(0.0, height), listOf(radius, radius)),
                crossSection = ::circleCrossSection))

    }

    override fun cube(
        length: Double,
        width: Double,
        height: Double
    ): CSG3D {
        val crossSection: (List<Double>) -> Loop = { args ->
            val scale = args[0]
            csgRectangle(Point(0.0, 0.0), Point(scale * length, scale * width))
        }
        return CSG3D(
            extrudeProfile(
                Profile(listOf(0.0, height), listOf(1.0, 1.0)),
                crossSection = crossSection
            )
        )
    }

    override fun block(
        p1: ThreeDPoint,
        p2: ThreeDPoint,
        ramp: Double
    ): CSG3D {
        val verts = ArrayList<Vector3d>()
        for (x in listOf(p1.x, p2.x)) {
            for (y in listOf(p1.y, p2.y)) {
                verts.add(Vector3d.xyz(x, y, p1.z))
            }
        }
        for (x in listOf(p1.x - ramp, p2.x + ramp)) {
            for (y in listOf(p1.y - ramp, p2.y + ramp)) {
                verts.add(Vector3d.xyz(x, y, p2.z))
            }
        }
        val faces = ArrayList<List<Int>>()

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
        return CSG3D(Polyhedron(verts, faces).toCSG())
    }

}
