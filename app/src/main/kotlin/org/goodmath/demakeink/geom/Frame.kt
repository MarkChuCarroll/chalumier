package org.goodmath.demakeink.geom

import kotlin.math.sqrt

/*
 * This file is part of the kotlin translation of "demakein/geom.py" from
 * the original demakein code. I've tried to reproduce the functionality
 * of the original code, but adding types to hopefully make it harder
 * to screw things up.
 *
 * All the original comments from the Python are here, prefixed with "ph:".
 */

/**
 * ph: Origin and orthogonal basis.
 */
data class Frame(val origin: XYZ, val x: XYZ, val y: XYZ, val z: XYZ) {

    fun apply(point: XYZ): XYZ {
        return origin + x*point.x + y*point.y + z*point.z
    }

    fun unapply(point: XYZ): XYZ {
        val nPoint = point - origin
        return XYZ(x.dot(point), y.dot(point), z.dot(point))
    }
}

data class Path(
        val path: SBasis<XYZ>,
        val velocity: SBasis<XYZ>,
        val normal: SBasis<XYZ>,
        val position: SBasis<Double>) {
    val math = path.math
    fun find(positionParam: Double): Double {
        var pos = positionParam
        var low = 0.0
        var high = 1.0
        for (i in 0 until 32) {
            val mid = (low + high) / 2.0
            val value = position.invoke(mid)
            if (pos < value) {
                high = mid
            } else if (pos > value) {
                low = mid
            } else {
                return mid // Unlikely.
            }
        }

        return (low + high) / 2.0
    }

    fun getLength(): Double = position[0].a1


    fun getFrame(position: Double): Frame {
        val t = find(position)
        val point = path.invoke(t)

        val z = velocity.invoke(t).unit()
        var x = normal.invoke(t)
        x = (x - z * x.dot(z)).unit()
        val y = z.cross(x)
        return Frame(point, x, y, z)
    }

    fun getPoint(position: Double): XYZ {
        val p = find(position)
        return path.invoke(p)
    }

    fun getBentness(a: Double, b: Double): Double {
        val pdp = doubleBridge(XYZMath)

        val aa = find(a)
        val bb = find(b)
        val lin = Linear(aa, bb, DoubleMath)
        val basis = SBasis(listOf(lin), DoubleMath)
        val seg = path.compose(basis, pdp, pdp)
        val straight = SBasis(listOf(seg[0], seg[1]), XYZMath)
        val diff = seg.minus(straight, XYZMath.selfBridge)
        return sqrt(diff.dot(diff).integral()[0].tri() / seg[0].tri().mag2())
    }

    companion object {
        fun path(point0: XYZ, vec0: XYZ, norm0: XYZ, point1: XYZ, vec1: XYZ, norm1: XYZ): Path {
            val ppp = XYZMath.selfBridge
            val pdp = doubleBridge(XYZMath)
            // ph: a = S_basis([Linear(XYZ(0.0,0.0,0.0), XYZ(1.0,1.0,0.0))])
            // ph: b = S_basis([Linear(XYZ(3.0,0.0,0.0), XYZ(0.0,-1.0,0.0))])
            // ph: arc = a + (S_basis([Linear(-a[0].tri(),a[0].tri())])+b).shifted(1)
            val tri = point1 - point0
            var length = tri.mag()
            val vec0unit = vec0.unit()
            val vec1unit = vec1.unit()
            var path = SBasis(
                    listOf(
                            Linear(point0, point1, XYZMath),
                            Linear(point0, point1, XYZMath)), XYZMath)
            var velocity = path.derivative()
            var position = SBasis(listOf(Linear(0.0, 0.0, DoubleMath)), DoubleMath)
            for (i in 0 until 3) {
                val s = length
                path = SBasis(
                        listOf(
                                Linear(point0, point1, XYZMath),
                                Linear(vec0unit.times(s).minus(tri), vec1unit.times(-s).plus(tri), XYZMath)),
                        XYZMath)
                velocity = path.derivative()
                val speed = velocity.dot(velocity).sqrt(6)
                position = speed.integral()
                position = position.minus(position.ONE.scaled(position[0].a0, DoubleMath.selfBridge),
                        DoubleMath.selfBridge)
                length = position[0].a1
            }
            val normal = SBasis(listOf(Linear(norm0, norm1, XYZMath)), XYZMath)
            return Path(path, velocity, normal, position)
        }

    }
}
