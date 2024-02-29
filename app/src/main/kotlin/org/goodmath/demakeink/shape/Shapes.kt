package org.goodmath.demakeink.shape

import org.goodmath.demakeink.design.Profile
import org.goodmath.demakeink.errors.dAssert
import org.goodmath.demakeink.util.Point
import org.goodmath.demakeink.geom.XYZ
import org.goodmath.demakeink.util.fromEnd
import kotlin.math.*



fun extrusion(zs: List<Double>, shapes: List<Loop>, name: String? = null): Shape {
    val nZ = zs.size
    val nShape = shapes[0].len()
    val verts: MutableList<XYZ> = mutableListOf()
    zs.forEachIndexed { i, z ->
        shapes[i].values.forEach { (x,y) ->
            verts.add(XYZ(x, y, z))
        }
    }
    val end0 = verts.size
    verts.add(shapes[0].centroid.at(zs[0]))
    val end1 = verts.size
    verts.add(shapes.fromEnd(1).centroid.at(zs.fromEnd(1)))

    val faces = ArrayList<Triple<Int, Int, Int>>()
    for (i in (0 until nZ-1)) {
        for (j in (0 until nShape)) {
            /*ph:
            #faces.append( (
            #    (i+1)*nshape+j, i*nshape+j,
            #    i*nshape+(j+1)%nshape, (i+1)*nshape+(j+1)%nshape
            #) )
             */
            faces.add(Triple((i + 1) * nShape + j,
                    i * nShape + j,
                    i * nShape + (j + 1) % nShape))
            faces.add(Triple(
                    (i + 1) * nShape + j,
                    i * nShape + (j + 1) % nShape,
                    (i + 1) * nShape + (j + 1) % nShape))
        }
    }
    for (i in (0 until nShape)) {
        val i1 = (i+1)*nShape
        faces.add(Triple(i1, i, end0))
        faces.add(Triple(i+nShape*(nZ-1) , i1+nShape*(nZ-1),
                end1))
    }
    return create(verts, faces, name)
}


fun block(p1: XYZ, p2: XYZ, name: String?=null, ramp: Double=0.0): Shape {
    val verts = ArrayList<XYZ>()
    for (x in listOf(p1.x, p2.x)) {
        for (y in listOf(p1.y, p2.y)) {
            verts.add(XYZ(x, y, p1.z))
        }
    }
    for (x in listOf(p1.x - ramp, p2.x+ramp)) {
        for (y in listOf(p1.y - ramp, p2.y + ramp)) {
            verts.add(XYZ(x, y, p2.z))
        }
    }
    val faces = ArrayList<Triple<Int, Int, Int>>()
    fun quad(a: Int, b: Int, c: Int, d: Int) {
        faces.add(Triple(a, b, c))
        faces.add(Triple(a, c, d))
    }
    for ((a, b, c) in listOf(Triple(1, 2, 4), Triple(4, 1, 2), Triple(2, 4, 1))) {
        quad(0, a, a+b, b)
        quad(c+b, c+a+b, c+a, c+0)
    }
    return create(verts, faces, name)
}

fun circleCrossSection(params: List<Double>): Loop {
    return circle(params[0])
}

fun extrudeProfile(profiles: List<Profile>, crossSection: (List<Double>) -> Loop = ::circleCrossSection , name: String? = null): Shape {
    val zs = ArrayList<Double>()
    val shapes = ArrayList<Loop>()
    val posSet = HashSet<Double>()
    for (item in profiles) {
        posSet.addAll(item.pos)
    }
    val pos = posSet.toList().sorted()
    pos.forEachIndexed { i, z ->
        val lows = profiles.map { item -> item(z) }
        val highs = profiles.map { item -> item(z, true) }
        if (i != 0) {
            zs.add(z)
            shapes.add(crossSection(lows))
        }
        if (i == 0 || (i < (pos.size)-1 && (lows != highs))) {
            zs.add(z)
            shapes.add(crossSection(highs))
        }
    }
    return extrusion(zs, shapes, name)
}


fun prism(height: Double, diameter: Double, crossSection: (List<Double>)->Loop = ::circleCrossSection,
          name: String? = null): Shape {
    val span = Profile(arrayListOf(0.0, height), listOf(diameter, diameter))
    return extrudeProfile(listOf(span), crossSection = crossSection, name = name)
}

fun makeSegment(instrument: Shape,
                top: Boolean,
                low: Double,
                high: Double,
                radius: Double,
                pad: Double =0.0,
                clipHalf: Boolean=true): Shape {
    val (y1, y2) = if (!clipHalf) {
        Pair(-radius, radius)
    } else if (top) {
        Pair(0.0, radius)
    } else {
        Pair(-radius, 0.0)
    }
    val clip = block(XYZ(-radius,y1, low-pad), XYZ(radius, y2, high+pad))
    val segment = instrument.copy()
    segment.clip(clip)
    dAssert((segment.size().y - (high-low+pad*2)) < 1e-3, "Need more padding for construction")
    segment.move(0.0, 0.0, -low)
    if (top) {
        segment.rotate(0, 0, 1, 180.0)
    }
    segment.rotate(1, 0, 0, -90.0)
    segment.rotate(0, 0, 1, 90.0)
    segment.move(high-low, 0.0, 0.0)
    return segment
}

fun makeSegments(instrument: Shape, length: Double, radius: Double,
                 topFractions: List<Double>, bottomFractions: List<Double>,
                 pad: Double=0.0, clipHalf: Boolean = true): Pair<List<Shape>, List<Double>> {
    val parts = ArrayList<Shape>()
    val lengths = ArrayList<Double>()
    val z = topFractions.map { item -> item*length }
    for (i in 0 until topFractions.size-1) {
        lengths.add(z[i+1]-z[i])
        parts.add(makeSegment(instrument, true,
                z[i],z[i+1],
                radius, pad, clipHalf))
    }
    val z2 = bottomFractions.map { item -> item * length }
    for (i in 0 until bottomFractions.size-1) {
        lengths.add(z2[i+1]-z2[i])
        parts.add(makeSegment(instrument, false,
                z2[i],z2[i+1],
                radius, pad, clipHalf))
    }
    return Pair(parts, lengths)
}


// ph's code had a function here called makeFormwork, which
// made absolutely no sense (it made calls to pack.pack(template, List<packables>)
// with parameters (list<Point>, double).  Fortunately, it was
// never called. I suspect it's a remnant of an earlier version which
// didn't get deleted. I'm not going to include it here.

// Similarly, the original call had a function "frame_extrusion",
// which was only called from path_extrusion, which was never called.
// So again, I'm not including them.

