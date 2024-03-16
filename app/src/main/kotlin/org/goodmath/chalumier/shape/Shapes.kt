/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goodmath.chalumier.shape

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.jcsg.Polyhedron
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.errors.dAssert
import org.goodmath.chalumier.geom.XYZ
import org.goodmath.chalumier.util.fromEnd

/*
 * A bunch of operations to create shapes and manipulate them in
 * basicways.
 */


fun extrusion(zs: List<Double>, shapes: List<Loop>, name: String? = null): CSG {
    val nZ = zs.size
    val nShape = shapes[0].len()
    val verts: MutableList<Vector3d> = mutableListOf()
    zs.forEachIndexed { i, z ->
        shapes[i].loopValues.forEach { (x, y) ->
            verts.add(Vector3d.xyz(x, y, z))
        }
    }
    val end0 = verts.size
    verts.add(shapes[0].centroid.at(zs[0]))
    val end1 = verts.size
    verts.add(shapes.fromEnd(1).centroid.at(zs.fromEnd(1)))

    val faces = ArrayList<List<Int>>()
    for (i in (0 until nZ - 1)) {
        for (j in (0 until nShape)) {
            faces.add(
                listOf(
                    (i + 1) * nShape + j, i * nShape + j, i * nShape + (j + 1) % nShape
                )
            )
            faces.add(
                listOf(
                    (i + 1) * nShape + j, i * nShape + (j + 1) % nShape, (i + 1) * nShape + (j + 1) % nShape
                )
            )
        }
    }
    for (i in (0 until nShape)) {
        val i1 = (i + 1) * nShape
        faces.add(listOf(i1, i, end0))
        faces.add(
            listOf(
                i + nShape * (nZ - 1), i1 + nShape * (nZ - 1), end1
            )
        )
    }
    return Polyhedron(verts, faces).toCSG()
}


fun block(p1: Vector3d, p2: Vector3d, name: String? = null, ramp: Double = 0.0): CSG {
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
    fun quad(a: Int, b: Int, c: Int, d: Int) {
        faces.add(listOf(a, b, c))
        faces.add(listOf(a, c, d))
    }
    for ((a, b, c) in listOf(listOf(1, 2, 4), listOf(4, 1, 2), listOf(2, 4, 1))) {
        quad(0, a, a + b, b)
        quad(c + b, c + a + b, c + a, c + 0)
    }
    return Polyhedron(verts, faces).toCSG()
}

fun circleCrossSection(params: List<Double>): Loop {
    return circle(params[0])
}

fun extrudeProfile(
    profiles: List<Profile>,
    crossSection: (List<Double>) -> Loop = ::circleCrossSection,
    name: String? = null
): CSG {
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
        if (i == 0 || (i < (pos.size) - 1 && (lows != highs))) {
            zs.add(z)
            shapes.add(crossSection(highs))
        }
    }
    return extrusion(zs, shapes, name)
}


fun prism(
    height: Double, diameter: Double, crossSection: (List<Double>) -> Loop = ::circleCrossSection, name: String? = null
): CSG {
    val span = Profile(arrayListOf(0.0, height), arrayListOf(diameter, diameter))
    return extrudeProfile(listOf(span), crossSection = crossSection, name = name)
}

fun makeSegment(
    instrument: CSG,
    top: Boolean,
    low: Double,
    high: Double,
    radius: Double,
    pad: Double = 0.0,
    clipHalf: Boolean = true
): CSG {
    val (y1, y2) = if (!clipHalf) {
        Pair(-radius, radius)
    } else if (top) {
        Pair(0.0, radius)
    } else {
        Pair(-radius, 0.0)
    }
    val clip = block(Vector3d.xyz(-radius, y1, low - pad), Vector3d.xyz(radius, y2, high + pad))
    val segment = instrument.clone().intersect(clip)
    dAssert((segment.bounds.bounds.y - (high - low + pad * 2)) < 1e-3, "Need more padding for construction")
    val segmentTransform = Transform().translate(Vector3d.xyz(0.0, 0.0, -low))
    if (top) {
        segmentTransform.rotZ(180.0)
    }

    segmentTransform.rotX(-90.0)
    segmentTransform.rotZ(90.0)
    segmentTransform.translate(high - low, 0.0, 0.0)
    return segment.transformed(segmentTransform)
}

fun makeSegments(
    instrument: CSG,
    length: Double,
    radius: Double,
    topFractions: List<Double>,
    bottomFractions: List<Double>,
    pad: Double = 0.0,
    clipHalf: Boolean = true
): Pair<List<CSG>, List<Double>> {
    val parts = ArrayList<CSG>()
    val lengths = ArrayList<Double>()
    val z = topFractions.map { item -> item * length }
    for (i in 0 until topFractions.size - 1) {
        lengths.add(z[i + 1] - z[i])
        parts.add(
            makeSegment(
                instrument, true, z[i], z[i + 1], radius, pad, clipHalf
            )
        )
    }
    val z2 = bottomFractions.map { item -> item * length }
    for (i in 0 until bottomFractions.size - 1) {
        lengths.add(z2[i + 1] - z2[i])
        parts.add(
            makeSegment(
                instrument, false, z2[i], z2[i + 1], radius, pad, clipHalf
            )
        )
    }
    return Pair(parts, lengths)
}

fun CSG.positionNicely(): CSG {
    return transformed(Transform()
        .translate(-0.5*(bounds.min.x + bounds.max.x),
            -0.5*(bounds.min.y + bounds.max.y),
            -bounds.min.z))
}


// ph's code had a function here called makeFormwork, which
// made absolutely no sense (it made calls to pack.pack(template, List<packables>)
// with parameters (list<Point>, double).  Fortunately, it was
// never called. I suspect it's a remnant of an earlier version which
// didn't get deleted. I'm not going to include it here.

// Similarly, the original call had a function "frame_extrusion",
// which was only called from path_extrusion, which was never called.
// So again, I'm not including them.

