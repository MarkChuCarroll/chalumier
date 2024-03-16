package org.goodmath.chalumier.scad

import org.goodmath.chalumier.design.instruments.Instrument
import org.goodmath.chalumier.design.Profile


class FluteModel(private val inst: Instrument,
                 private val facets: Int? = null,
                 private val ringWidth: Double = 2.0,
                 private val eccentricity: Double = 1.2,
                 private val plateThickness: Double = 2.0) {
    private val holes = (0 until inst.numberOfHoles).map { i ->
        Hole(inst.holePositions[i], inst.holeDiameters[i])
    }
    private val inner = makeStack("bore", inst.innerKinks, inst.inner)
    private val outer = makeStack("body", inst.outerKinks, inst.outer, facets)

    private fun makeStack(name: String, kinks: List<Double>, profile: Profile, facets: Int? = null): Shape {
        val union =
            Union(
                kinks.indices.map { i ->
                    val lowerPos = if (i == 0)  { 0.0 } else { kinks[i] }
                    val lowerDiameter = profile(lowerPos)
                    val upperPos = if (i == kinks.size - 1) {
                        // Extend the body to make room for the cork.
                        inst.length + (getEmbouchureHole().diameter * 0.5)

                    } else {
                        kinks[i+1]
                    }
                    val upperDiameter = profile(upperPos)
                    val cyl = Cylinder(upperPos - lowerPos, lowerDiameter/2.0, upperDiameter/2.0, facets ?: 0 )
                    if (lowerPos != 0.0) {
                        Translate(ThreeDimensionalValue(0.0, 0.0, lowerPos), listOf(cyl))
                    } else {
                        cyl
                    }
                } )

        return Labelled("$name stack",union)
    }

    private fun diametersAt(elevation: Double): Pair<Double, Double> {
        return Pair(inst.inner(elevation), inst.outer(elevation))
    }

    private fun makeHoleRing(hole: Hole): Shape {
        val (inner, outer) = diametersAt(hole.elevation)

        return Labelled("hole ring", Rotate(
            ThreeDimensionalValue(0.0, -90.0, 0.0),
            listOf(Translate(
                ThreeDimensionalValue(hole.elevation, 0.0, inner / 2.0),
                listOf(Difference(listOf(Cylinder(
                    (outer - inner) / 2.0 + ringWidth,
                    hole.diameter / 2.0 + ringWidth,
                    hole.diameter / 2.0 + ringWidth),
                    Cylinder((outer - inner) / 2.0 + 2.0,
                        hole.diameter / 2.0,
                        hole.diameter / 2.0))))))))

    }


    private fun makeHole(hole: Hole): Shape {
        val bodyDiam = inst.outer(hole.elevation)
        return Rotate(
            ThreeDimensionalValue(0.0, -90.0, 0.0),
            listOf(Translate(
                ThreeDimensionalValue(hole.elevation, 0.0, 0.0),
                listOf(
                    Cylinder(
                        bodyDiam / 1.0, hole.diameter / 2.0,
                        hole.diameter / 2.0
                    )
                )
            ))
        )
    }


    private fun makeEmbouchurePlate(
        elevation: Double,
        diameter: Double): Shape {
        // The embouchure plate is basically on oval draped over a cylinder the diameter of the flute
        // bore. But OpenSTL doesn't support that kind of operation, se we need to do it in a slightly
        // harder way.
        //
        // * Basically, we start with a cylinder that matches the flute bore.
        // * Then we create a second cylinder that's larger than the bore - the outer body diameter,
        //   plus the desired thickness of the plate.
        // * We subtract the bore from that - and now we've got a hollow cylinder.
        // * Then we intersect that with an ovoid cylinder the size of the desired plate placed at a right angle.
        // * That will give us two mirror images of the basic plate.
        // * We extract one of them by intersecting with a cube, and voilà! - we have a plate.
        val (inner, outer) = diametersAt(elevation)

        val innerCylinder = Cylinder(
            diameter * 8.0,
            inner / 2.0,
            inner / 2.0)

        val outerCylinder = Cylinder(
            diameter * 8,
            outer / 2.0 + plateThickness,
            outer / 2.0 + plateThickness
        )
        val hollow = Difference(listOf(outerCylinder, innerCylinder))
        val oval = Translate(
            ThreeDimensionalValue(0.0, 0.0, diameter * 2.0),
            listOf(
                Rotate(ThreeDimensionalValue(90.0, 0.0, 0.0),
                    listOf(Scale(ThreeDimensionalValue(1.0, 1.6, 1.0),
                        listOf(Cylinder(outer * 3.0 * diameter, diameter)))))))

        val plate = Labelled(
            "new plate",
            Rotate(ThreeDimensionalValue(0.0, 0.0, -90.0),
                listOf(Intersection(listOf(hollow, oval)))))

        val withHole = Labelled("plate with hole",
            Difference(listOf(
                plate,
                Translate(ThreeDimensionalValue(0.0, 0.0, diameter*2.0),
                    listOf(Rotate(ThreeDimensionalValue(0.0, -90.0, 0.0),
                        listOf(Scale(ThreeDimensionalValue(eccentricity, 1.0, 1.0),
                            listOf(Cylinder(
                                100.0,
                                diameter / 2.0,
                                diameter / 2.0))))))))))

        return Translate(ThreeDimensionalValue(0.0, 0.0, elevation - diameter * 2), listOf(withHole))
    }

    private fun getEmbouchureHole(): Hole {
        return holes.last()
    }

    private fun makeBody(): Shape {
        val rotation = this.facets?.let {
            if (it != 0) {
                360.0 / facets.toDouble() / 2.0
            } else {
                0.0
            }
        } ?: 0.0

        val holeRings = holes.slice(0 until holes.size - 1).map {
            makeHoleRing(it)
        }

        val body = Difference(
            listOf(
                Labelled(
                    "body + hole rings",
                    Union(
                        // Rotate outer body so that the facets line up with the finger holes.
                        listOf(
                            Rotate(
                                ThreeDimensionalValue(0.0, 0.0, rotation),
                                listOf(outer)
                            )) + holeRings
                    )
                ),
                inner,
                makeEmbouchureHole()
            )
        )
        holes.slice(0 until holes.size - 1).forEach { h -> body.add(makeHole(h)) }
        return body
    }

    private fun makeEmbouchureHole(): Shape {
        val emb = getEmbouchureHole()
        return Translate(
            ThreeDimensionalValue(0.0, 0.0, emb.elevation),
            listOf(
                Rotate(
                    ThreeDimensionalValue(0.0, -90.0, 0.0),
                    listOf(
                        Scale(
                            ThreeDimensionalValue(eccentricity, 1.0, 1.0),
                            listOf(
                                Cylinder(
                                    inst.outer(emb.elevation),
                                    emb.diameter / 2.0, emb.diameter / 2.0
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun makeCork(): Shape {
        val emb = getEmbouchureHole()
        val corkPos = emb.elevation + 1.75 * emb.diameter
        val corkRad = inst.inner(inst.inner.end()) / 2.0 + 1.0
        val corkThickness = corkPos - emb.elevation
        return Labelled(
            "cork",
            Translate(ThreeDimensionalValue(0.0, 0.0, corkPos),
                listOf(Cylinder(corkThickness, corkRad, corkRad))))
    }

    private fun makeFlute(): Shape {
        val emb = getEmbouchureHole()
        return Union(listOf(
            makeBody(),
            makeEmbouchurePlate(emb.elevation, emb.diameter),
            makeCork()))
    }

    fun render(): String {
        return "// ${inst.name}\n\n${makeFlute().render(0)}"
    }

}


