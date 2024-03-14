package org.goodmath.chalumier.model

import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.design.Profile


class FluteModel(val inst: Instrument, val facets: Int? = null) {
    val holes = (0 until inst.numberOfHoles).map { i ->
        Hole(inst.holePositions[i], inst.holeDiameters[i])
    }
    val inner = stack("bore", inst.innerKinks, inst.inner)
    val outer = stack("body", inst.outerKinks, inst.outer, facets)

    fun stack(name: String, kinks: List<Double>, profile: Profile, facets: Int? = null): Geom {
        System.err.println("LEN = ${inst.length}")
        val union =
            Union(
                (0 until kinks.size).map { i ->
                    val lowerPos = if (i == 0)  { 0.0 } else { kinks[i] }
                    val lowerDiameter = profile(lowerPos)
                    val upperPos = if (i == kinks.size - 1) {
                        // Extend the bodyto make room for the cork.
                        inst.length + (getEmbouchureHole().diameter * 3.0)
                    } else {
                        kinks[i+1]
                    }
                    val upperDiameter = profile(upperPos)
                    val cyl = Cylinder(upperPos - lowerPos, lowerDiameter/2.0, upperDiameter/2.0, facets ?: 0 )
                    if (lowerPos != 0.0) {
                        Translate(ThreeD(0.0, 0.0, lowerPos), listOf(cyl))
                    } else {
                        cyl
                    }
                } )

        return Labelled("$name stack",union)
    }

    private fun diametersAt(elevation: Double): Pair<Double, Double> {
        return Pair(inst.inner(elevation), inst.outer(elevation))
    }

    fun genRing(hole: Hole, ringWidth: Double): Geom {
        val (inner, outer) = diametersAt(hole.elevation)

        return Labelled("hole ring", Rotate(
            ThreeD(0.0, -90.0, 0.0),
            listOf(Translate(
                ThreeD(hole.elevation, 0.0, inner / 2.0),
                listOf(Difference(listOf(Cylinder(
                    (outer - inner) / 2.0 + 2.0,
                    hole.diameter / 2.0 + ringWidth,
                    hole.diameter / 2.0 + ringWidth),
                    Cylinder((outer - inner) / 2.0 + 2.0,
                        hole.diameter / 2.0,
                        hole.diameter / 2.0))))))))

    }


    fun genHole(hole: Hole): Geom {
        val bodyDiam = inst.outer(hole.elevation)
        return Rotate(
            ThreeD(0.0, -90.0, 0.0),
            listOf(Translate(
                ThreeD(hole.elevation, 0.0, 0.0),
                listOf(
                    Cylinder(
                        bodyDiam / 1.0, hole.diameter / 2.0,
                        hole.diameter / 2.0
                    )
                )
            ))
        )
    }


    fun genEmbouchure(
        elevation: Double,
        diameter: Double,
        eccentricity: Double,
        thickness: Double): Geom {
        // The embechoure plate is basically on oval draped over a cylinder the diameter of the flute
        // bore. But OpenSTL doesn't support that kind of operation, se we need to do it in a slightly
        // harder way.
        //
        // Basically, we start with a cylinder that matches the flute bore.
        // Then we create a second cylinder that's larger than the bore - the outer body diameter,
        // plus the desired thickness of the plate.
        // We subtract the bore from that - and now we've got a hollow cylinder.
        // Then we intersect that with a ovoid cylinder the size of the desired plate placed at a right angle.
        // That will give us two mirror images of the basic plate.
        // We extract one of them by intersecting with a cube, and voila - we have a plate.
        val (inner, outer) = diametersAt(elevation)

        val innerCylinder = Cylinder(
            diameter * 8.0,
            inner / 2.0,
            inner / 2.0)

        val outerCylinder = Cylinder(
            diameter * 8,
            outer / 2.0 + thickness,
            outer / 2.0 + thickness
        )
        val hollow = Difference(listOf(outerCylinder, innerCylinder))
        val oval = Translate(
            ThreeD(0.0, 0.0, diameter * 2.0),
            listOf(
                Rotate(ThreeD(90.0, 0.0, 0.0),
                    listOf(Scale(ThreeD(1.0, 1.6, 1.0),
                        listOf(Cylinder(outer * 3.0 * diameter, diameter)))))))

        val plate = Labelled(
            "new plate",
            Rotate(ThreeD(0.0, 0.0, -90.0),
                listOf(Intersection(listOf(hollow, oval)))))

        val withHole = Labelled("plate with hole",
            Difference(listOf(
                plate,
                Translate(ThreeD(0.0, 0.0, diameter*2.0),
                    listOf(Rotate(ThreeD(0.0, -90.0, 0.0),
                        listOf(Scale(ThreeD(eccentricity, 1.0, 1.0),
                            listOf(Cylinder(
                                100.0,
                                diameter / 2.0,
                                diameter / 2.0))))))))))

        return Translate(ThreeD(0.0, 0.0, elevation - diameter * 2), listOf(withHole))
    }

    fun getEmbouchureHole(): Hole {
        return holes.last()
    }

    fun genBody(): Geom {
        val rotation = this.facets?.let {
            if (it != 0) {
                360.0 / facets.toDouble() / 2.0
            } else {
                0.0
            }
        } ?: 0.0

        val holeRings = holes.slice(0 until holes.size - 1).map {
            genRing(it, 3.0)
        }
        val emb = getEmbouchureHole()
        val body = Difference(
            listOf(
                Labelled(
                    "body + holerings",
                    Union(
                        // Rotate outer body so that the facets line up with the finger holes.
                        listOf(
                            Rotate(
                                ThreeD(0.0, 0.0, rotation),
                                listOf(outer)
                            )) + holeRings
                    )
                ),
                inner,
                Translate(
                    ThreeD(0.0, 0.0, emb.elevation),
                    listOf(
                        Rotate(
                            ThreeD(0.0, -90.0, 0.0),
                            listOf(
                                Scale(
                                    ThreeD(1.2, 1.0, 1.0),
                                    listOf(
                                        Cylinder(
                                            inst.outer(emb.elevation),
                                            emb.diameter / 2.0, emb.diameter / 2.0
                                        ))
                                )
                            )
                        )
                    )
                )
            )
        )
        holes.slice(0 until holes.size - 1).forEach { h -> body.add(genHole(h)) }
        return body
    }

    fun genCork(): Geom {
        val emb = getEmbouchureHole()
        val corkPos = emb.elevation + 1.75 * emb.diameter
        val corkRad = inst.inner(inst.inner.end()) / 2.0 + 1.0
        val corkThickness = corkPos - emb.elevation
        return Labelled(
            "cork",
            Translate(ThreeD(0.0, 0.0, corkPos),
                listOf(Cylinder(corkThickness, corkRad, corkRad))))
    }

    fun genFlute(): Geom {
        val emb = getEmbouchureHole()
        return Union(listOf(
            genBody(),
            genEmbouchure(emb.elevation, emb.diameter, 1.2,
                2.0),
            genCork()))
    }

    fun render(): String {

        return "// ${inst.name}\n\n${genFlute().render(0)}"
    }

}


