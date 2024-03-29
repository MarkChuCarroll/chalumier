/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to iun writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goodmath.chalumier.design

import org.goodmath.chalumier.config.*
import org.goodmath.chalumier.design.instruments.*
import org.goodmath.chalumier.diagram.Diagram
import org.goodmath.chalumier.errors.RequiredParameterException
import org.goodmath.chalumier.errors.dAssert
import org.goodmath.chalumier.make.InstrumentMaker
import org.goodmath.chalumier.make.JoinType
import org.goodmath.chalumier.optimize.Optimizer
import org.goodmath.chalumier.optimize.ProgressDisplay
import org.goodmath.chalumier.optimize.Score
import org.goodmath.chalumier.optimize.ScoredParameters
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.*


/**
 * MarkCC: I've refactored this quite a bit from demakein.
 *
 * Everything configurable is now build using configurable parameters,
 * and they're all set up before hte rest of the code. Serialization is
 * done into a human-readable JSON using the configurable parameters.
 *
 * State vectors have been replaced by a data class, to make it
 * a little easier to understand what's going on, and to interpret
 * new states.
 */
abstract class InstrumentDesigner<Inst: Instrument>(
    override val instrumentName: String,
    val outputDir: Path,
    val builder: InstrumentFactory<Inst>): Configurable<InstrumentDesigner<Inst>>(instrumentName) {

    /*
     * Basic definitional parameters of the instrument.
     */
    open var name by StringParameter("The name of the instrument being designed.") {
        instrumentName
    }

    open var rootNote by OptStringParameter("The lowest note on the instrument, when all holes are closed") {
        null
    }

    open var closedTop by BooleanParameter("Is this a closed top instrument?") { false }

    open var transpose by IntParameter("an optional transposition, in chromatic steps, to apply to the instrument specification") { 0 }

    open var numberOfHoles by IntParameter("the number of holes, including embouchure") { c -> c.maxHoleDiameters.size }

    open var fingerings by ConfigParameter(ListOfFingeringsKind,"list of specifications of fingerings and the notes they should produce") {
        throw RequiredParameterException("fingerings")
    }


    /*
    * Parameters that define the profile/size/shape of the instrument.
    */

    open var length by DoubleParameter("the length of the instrument") { it.initialLength * it.scale }

    open var maxLength by OptDoubleParameter(
        "The maximum length that the design should make the instrument when modelling",) {
        null
    }

    open var initialLength by DoubleParameter("the initial length of the instrument before modeling") {
        rootNote?.let { wavelength(it) / 2.0 }?: throw RequiredParameterException("initialLength")
    }


    open var innerDiameters by ListOfDoublePairParameter(
        "A description of the inner bore of the instrument. The first element is the bore diameter at the base " +
                "of the instrument; the last element is the bore diameter at the top of the instrument. " +
                "The bore is piecewise linear, where the intervening elements are boundaries between pieces (kinks). "+
                "The exact placement may be moved as part of the optimization process. As an advanced option " +
                "instead of a single diameter, you can give a tuple (low,high)  to create a step in the " +
                "diameter of the bore.  See the examples/stepped_shawm.py for an example of this.") {
        throw RequiredParameterException("innerDiameters")
    }

    open var outerDiameters by ListOfDoublePairParameter("the diameters of the outer body, from bottom to top") {
        throw RequiredParameterException("outerDiameters", "must have at least two elements")
    }

    open var innerAngles by ListOfOptAnglePairsParameter("angle descriptions for the inner contours of the instrument's bore.") {
        innerDiameters.map { null }
    }

    open var outerAngles by ListOfOptAnglePairsParameter("angle descriptions for the outer contours of the instrument's body") {
        outerDiameters.map { null }
    }

    open var coneStep by DoubleParameter("The size of the step used when translating conic sections into curves") { 0.125 }

    open var topClearanceFraction by DoubleParameter( "how close to the top are finger holes allowed to be placed?") { 0.0 }

    open var bottomClearanceFraction by DoubleParameter("how close to the bottom are finger holes allowed to be placed?") { 0.0 }

    open var scale: Double by DoubleParameter("Scaling factor to apply to the instrument specification") {
        2.0.pow(-transpose/12.0)
    }

    /*
     * Characteristics of holes that effect optimization.
     */

    open var tweakEmissions by DoubleParameter("Experimental term added to the optimization to try to make instrument louder, possibly at the cost of intonation") {  0.0 }


    open var minHoleDiameters by ListOfDoubleParameter("the minimum acceptable diameters of holes") { c -> c.numberOfHoles.repeat { 0.5 } }

    open var maxHoleDiameters by ListOfDoubleParameter("the maximum acceptable diameter of holes") {
        throw RequiredParameterException("maxHoleDiameters")
    }


    open var minHoleSpacing by ListOfOptDoubleParameter(
        "Minimum space between each pair of finger holes") {
        (it.numberOfHoles-1).repeat { 0.0 }
    }

    open var maxHoleSpacing by ListOfOptDoubleParameter("the maximum distance separating each pair of holes") { c ->
        (c.numberOfHoles - 1).repeat { c.initialLength }
    }
    open var balance by ListOfOptDoubleParameter("For each triplet of holes (0, 1, 2), (1, 2, 3), ..., this is a " +
            "value between 0 and 1 specifying how similar the spacings of the pairs of holes should be. " +
            "The smaller the value, the more similar the spacings must be.") { c -> (c.numberOfHoles - 2).repeat { null } }

    open var holeAngles by ListOfDoubleParameter( "Vertical angle of each hole. Using angling can " +
            "make an instrument easier to play due by making the hole spacing more comfortable.") { it.numberOfHoles.repeat { 0.0 } }

open var initialInnerFractions by ListOfDoubleParameter("Initial positions of kinks in the bore, described as " +
        "fractions of the length of the bore. Most the time, this will be automatically " +
        "generated from the inner diameters") { c ->
        (c.innerDiameters.size - 2).repeat { (it + 1.0) / (c.innerDiameters.size - 1) }
    }

    open var minInnerFractionSep by ListOfDoubleParameter("Minimum size of each linear segment of the bore, " +
            "as a fraction of the overall length.") {
        (it.innerDiameters.size - 1).repeat { 0.0 }
    }
    open var maxInnerFractionSep by ListOfDoubleParameter("Maximum size of each linear segment of the bore, " +
            "as a fraction of the overall length.") {
        (it.innerDiameters.size - 1).repeat { 1.0 }
    }

    open var minInnerSep by ListOfOptDoubleParameter("The minimum distance between changes in the bore diameter") {
        (it.innerDiameters.size - 1).repeat { null }
    }

    open var maxInnerSep by ListOfOptDoubleParameter("The maximum distances between changes in the bore diameter") {
        (it.innerDiameters.size - 1).repeat { null }
    }
    open var initialOuterFractions by ListOfDoubleParameter("Initial positions of kinks in the body shape, described as " +
            "fractions of the length of the instrument. Most the time, this will be automatically " +
            "generated from the outer diameters") { c ->
        (c.outerDiameters.size - 2).repeat { (it + 1.0) / (c.outerDiameters.size - 1) }
    }

    open var minOuterFractionSep by ListOfDoubleParameter("Minimum size of each linear segment of the instrument, " +
            "as a fraction of the overall length.") { c ->
        (c.outerDiameters.size - 1).repeat { 0.0 }
    }

    open var maxOuterFractionSep by ListOfDoubleParameter("Maximum size of each linear segment of the bore, " +
            "as a fraction of the overall length.") {
        (it.outerDiameters.size - 1).repeat { 1.0 }
    }

    open var initialHoleFractions by ListOfDoubleParameter("Initial hole locations, defined as fractions of the length of the instrument") { c ->
        c.numberOfHoles.repeat { (it + 3.0) / (c.numberOfHoles + 2) * 0.5 }
    }

    open var initialHoleDiameterFractions by ListOfDoubleParameter("Initial hole diameters, defined as fractions of the instrument's bore size.") {
        it.numberOfHoles.repeat { 0.75 }
    }

    open var holeHorizAngles by ListOfDoubleParameter("Horizontal angle offsets for each hole. Using offsets " +
        "can produce an instrument that is easier to play.") {
        (0 until numberOfHoles).map { 0.0 }
    }

    /*
     * Parameters for the 3d model of the instrument.
     */
    open var decorate by BooleanParameter("When building a 3d model, should embellishments be adde to the body?") { false }

    open var dilate by DoubleParameter("Dilate the body of the instrument by this much") { 0.0 }

    open var join by StringParameter("The type of join in a multi-part model: one of (StraightJoin, WeldJoin, TaperedJoin)") {
        JoinType.StraightJoin.toString()
    }

    open var generatePads by BooleanParameter("Generate pads around holes?") {
        true
    }

    open var thickSockets by BooleanParameter("Make the body thicker around socket joins?") {
        false
    }

    open var gap by DoubleParameter("Size of the gap between sockets")  {  0.0 }

    open var outerAdd by ConfigParameter(BooleanParameterKind, "Should the body thickness be automatically increased?") { false }

    open var divisions by ListOfListOfIntDoublePairParam("For the 3d model, how should it be split into printable pieces?") {
        listOf(
            listOf(Pair(5, 0.0)),
            listOf(Pair(2, 0.0), Pair(5, 0.333)),
            listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.333)),
            listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.0), Pair(5, 0.7))
        )
    }

    /**
     * All the validations scattered in ph's code are moved here, and called before anything gets
     * filled in the instrument designer.
     */
    private fun validate() {
        dAssert(initialHoleFractions.size == numberOfHoles, "initialHoleFractions has wrong length")
        dAssert(
            initialHoleDiameterFractions.size == numberOfHoles, "initialHoleDiameterFractions has wrong length"
        )
        dAssert(
            initialInnerFractions.size == innerDiameters.size - 2, "initialInnerFractions has wrong length"
        )
        dAssert(
            initialOuterFractions.size == outerDiameters.size - 2, "initialOuterFractions has wrong length"
        )
        dAssert(fingerings.all { it.fingers.size == numberOfHoles},
            "Fingerings must have the same number of open/closed as the number of holes")
        dAssert(minHoleSpacing.size == numberOfHoles - 1,"minHoleSpacing must have one fewer value than numberOfHoles ")
        dAssert(maxHoleSpacing.size == numberOfHoles - 1,  "maxHoleSpacing must have one fewer value than numberOfHoles ")
        dAssert(balance.size == numberOfHoles - 2, "balance must have two fewer value than numberOfHoles  ${balance.size} vs ${numberOfHoles})")
        dAssert(balance.all { it == null || (it in 0.0..1.0)}, "balance values must be between 0 and 1")
        dAssert(holeAngles.size == numberOfHoles, "There must be one hole angle per hole")
        dAssert(holeHorizAngles.size == numberOfHoles, "There must be one hole horizontal angle per hole")
    }

    abstract fun readInstrument(path: Path): Inst

    abstract fun writeInstrument(instrument: Inst, path: Path)

    fun getInstrumentMaker(specFilePath: Path): InstrumentMaker<Inst> {
        val inst = readInstrument(specFilePath)
        return getInstrumentMaker(inst)
    }

    abstract fun getInstrumentMaker(spec: Inst): InstrumentMaker<Inst>

    /**
     * The instrument designer and the template instrument define the basic
     * parameters of the instrument we're going to produce. In order to
     * optimize the design, we need to be able to convert them to a state
     * object which can be manipulated by the optimizer. We do this once
     * to produce the initial state - from there, the optimizer will be
     * generating variations on it.
     */
    fun initialDesignParameters(): DesignParameters {
        validate()
        val result = DesignParameters.make(1.0,
            initialHoleFractions,
            ArrayList(initialHoleDiameterFractions.map { it * it}),
            initialInnerFractions, initialOuterFractions)
        return result
    }

    /**
     * When the optimizer changes a parameter set, we need to be able to take that
     * altered state vector and convert it back into an instrument for testing
     * and evaluation.
     */
    fun makeInstrumentFromParameters(parameters: DesignParameters): Instrument {
        val length = parameters.length * initialLength * scale
        val (innerLow, innerHigh) = lowHigh(innerDiameters)
        val (outerLow, outerHigh) = lowHigh(outerDiameters)
        val (innerAngleLow, innerAngleHigh) = lowHighOpt(innerAngles)
        val (outerAngleLow, outerAngleHigh) = lowHighOpt(outerAngles)
        val instHolePositions = ArrayList(parameters.holePositions.map {
            it * length
        })

        val innerKinks = ArrayList(parameters.innerKinks.map {  it * length })
        val outerKinks = ArrayList(parameters.outerKinks.map { it * length })
        val instInner = Profile.curvedProfile(
            ArrayList(listOf(0.0) + innerKinks + listOf(length)),
            innerLow,
            innerHigh,
            innerAngleLow,
            innerAngleHigh
        )
        val instOuterBase = Profile.curvedProfile(
            ArrayList(listOf(0.0) + outerKinks + listOf(length)),
            outerLow,
            outerHigh,
            outerAngleLow,
            outerAngleHigh
        )
        val instOuter = if (outerAdd) {
            instOuterBase + instInner
        } else {
            instOuterBase
        }


        //val instHoleAngles = holeAngles
        val instInnerHolePositions = ArrayList(numberOfHoles.repeat { 0.0 })
        val instHoleLengths = ArrayList(numberOfHoles.repeat { 0.0 })

        (0 until numberOfHoles).forEach { i ->
            // ph: Note: approximates bore as cylindrical calculating shift, length
            val radians = holeAngles[i] * PI / 180.0
            val thickness = (instOuter(instHolePositions[i]) - instInner(instHolePositions[i])) * 0.5
            val shift = sin(radians) * thickness

            instInnerHolePositions[i] = instHolePositions[i] + shift
            instHoleLengths[i] = (sqrt(thickness * thickness + shift * shift))
            // ph: #+ self.hole_extra_height_by_diameter[i] * inst.hole_diameters[i]
        }
        return builder.create(
            this,
            parameters,
            instrumentName,
            length = length,
            closedTop=closedTop,
            coneStep=coneStep,
            holeAngles = holeAngles,
            holeDiameters = ArrayList(parameters.holeAreas.mapIndexed { idx, area ->
                    minHoleDiameters[idx] + signedSqrt(area) * (maxHoleDiameters[idx] - minHoleDiameters[idx])
            }),
            holeLengths = instHoleLengths,
            holePositions = instHolePositions,
            inner = instInner,
            outer = instOuter,
            innerHolePositions = instInnerHolePositions,
            numberOfHoles = numberOfHoles,
            innerKinks = innerKinks,
            outerKinks = outerKinks,
            divisions=divisions)
    }


    /**
     * Compute a constraint score for the instrument, which describes how
     * well the instrument matches its requirements. If the constraint score is 0,
     * then it's a perfect match.
     */
    fun constraintScore(inst: Instrument): Double {
        val scores = ArrayList<Double>()
        scores.add(inst.length)
        val ml = maxLength
        if (ml != null) {
            scores.add(ml * scale - inst.length)
        }

        // Check that the inner kinks are within their bounds.
        val inners = ArrayList(listOf(0.0) + inst.innerKinks + listOf(inst.length))
        for (i in 0 until inners.size - 1) {
            val sep = inners[i + 1] - inners[i]
            scores.add(sep - minInnerFractionSep[i] * inst.length)
            scores.add(maxInnerFractionSep[i] * inst.length - sep)
            if (minInnerSep[i] != null) {
                scores.add(sep - minInnerSep[i]!! * scale)
            }
            if (maxInnerSep[i] != null) {
                scores.add(maxInnerSep[i]!! * scale - sep)
            }
        }

        // Check that the other kinks are within their bounds.
        val outers = ArrayList(listOf(0.0) + inst.outerKinks + listOf(inst.length))
        for (i in 0 until outers.size - 1) {
            val sep = outers[i + 1] - outers[i]
            scores.add(sep - minOuterFractionSep[i] * inst.length)
            scores.add(maxOuterFractionSep[i] * inst.length - sep)
        }

        // Check that the bottom hole is outside the bottom clearance fraction.
        scores.add(inst.holePositions[0] - bottomClearanceFraction * inst.length)

        // CHeck that the top hole is outside the top clearance fraction.
        scores.add((1.0 - topClearanceFraction) * inst.length - inst.holePositions.last())

        // Check that the holes are within their min and max separation bounds.
        for (idx in minHoleSpacing.indices) {
            val minSpacing = minHoleSpacing[idx]
            if (minSpacing != null) {
                scores.add((inst.holePositions[idx + 1] - inst.holePositions[idx]) - minSpacing)
            }
        }
        for (idx in maxHoleSpacing.indices) {
            val spacing = maxHoleSpacing[idx]
            if (spacing != null) {
                scores.add(spacing - (inst.holePositions[idx + 1] - inst.holePositions[idx]))
            }
        }

        // Check that the hole diameters are within their min and max bounds.
        minHoleDiameters.forEachIndexed { i, value ->
            scores.add(inst.holeDiameters[i] - value)
        }
        maxHoleDiameters.forEachIndexed { i, value ->
            scores.add(value - inst.holeDiameters[i])
        }

        // Check the balance.
        balance.forEachIndexed { i, balanceConstraint ->
            if (balanceConstraint != null) {
                scores.add((balanceConstraint * 0.5) * (inst.holePositions[i + 2] - inst.holePositions[i]) -
                        abs(0.5 * inst.holePositions[i] + 0.5 * inst.holePositions[i + 2] - inst.holePositions[i + 1]
                        ))
            }
        }
        val negScores = scores.filter { it < -0.05  }.map { -it }
        return if (negScores.isNotEmpty()) {
            negScores.sum()
        } else {
            0.0
        }
    }

    private fun fullScore(params: DesignParameters): Score {
        val inst = makeInstrumentFromParameters(params)
        return Score(constraintScore(inst), intonationScore(inst))
    }

    /** ph: Hook to modify instrument before scoring. */
    open fun patchInstrument(inst: Instrument): Instrument {
        return inst
    }

    /**
     * ph: Hook for how to calculate emission.
     *
     * Let-s Flute_designer rate emission relative to embouchure hole.
     */
    open fun calcEmission(emission: List<Double>, fingers: List<Hole>): Double {
        return sqrt(emission.sumOf { e -> e * e })
    }

    /**
     * Compute an evaluation score for the instruments intonation and emission.
     */
    fun intonationScore(i: Instrument): Double {
        val inst = patchInstrument(i)
        var score = 0.0
        var div = 0.0

        var emissionScore = 0.0
        var emissionDiv = 0.0
        if (tweakEmissions != 0.0) {
            inst.prepare()
        }
        inst.preparePhase()

        // I think that this is a normalizer. We're going to compute
        // the difference between the actual wavelength of a position
        // with the desired, and then express tha difference in cents.
        // This is a conversion factor for doing that.
        val s = 1200.0 / ln(2.0)
        for (idx in fingerings.indices) {
            val fingering = fingerings[idx]
            val fingers = fingering.fingers
            val desiredWavelength = fingering.wavelength(transpose)
            val actualWavelength = if (fingering.nth == null) {
                inst.trueWavelengthNear(desiredWavelength, fingers)
            } else {
                inst.trueNthWavelengthNear(desiredWavelength, fingers, fingering.nth)
            }
            val diff = abs(ln(desiredWavelength) - ln(actualWavelength)) * s
            val weight = 1.0
            score += weight * diff.pow(3)
            div += weight
            if (tweakEmissions != 0.0) {
                val emissionWeight = 1.0
                emissionDiv += emissionWeight
                val (_, emission) = inst.resonanceScore(actualWavelength, fingers, true)
                emission as ArrayList<Double>
                val rms = calcEmission(emission, fingers)
                val x = ln(rms)
                emissionScore += emissionWeight * x
            }
        }
        var result = (score / div).pow(1.0 / 3.0)
        if (tweakEmissions != 0.0) {
            val x = emissionScore / emissionDiv
            result += (tweakEmissions * -x)
        }
        return result
    }

    val constraintScorer: (DesignParameters) -> Double = {
        constraintScore(makeInstrumentFromParameters(it))
    }

    private var intonationScorer: (DesignParameters) -> Double = {
        intonationScore(makeInstrumentFromParameters(it))
    }

    private fun drawInstrumentOntoDiagram(
        diagram: Diagram,
        instrument: Instrument,
        color: String = "#000000",
        redColor: String = "#ff0000"
    ) {
        instrument.prepare()
        (0 until numberOfHoles).forEach { i ->
            diagram.circle(0.0, -instrument.innerHolePositions[i], instrument.holeDiameters[i], redColor)
            diagram.circle(0.0, -instrument.holePositions[i], instrument.holeDiameters[i], color)
            diagram.profile(instrument.outer, color)
            diagram.profile(instrument.steppedInner, color)
        }

        if (closedTop) {
            val d = instrument.steppedInner(instrument.length)
            diagram.line(
                listOf(Pair(-0.5 * d, -instrument.length), Pair(0.5 * d, -instrument.length)), color
            )
        }

        val tickX = instrument.outer.maximum() * -0.625
        for (pos in instrument.innerKinks) {
            diagram.line(listOf(Pair(tickX, -pos), Pair(tickX - 5, -pos)), color)
        }
        for (pos in instrument.inner.pos.drop(1).dropLast(1)) {
            diagram.line(listOf(Pair(tickX - 2, -pos), Pair(tickX - 3, -pos)), color)
        }
        for (pos in instrument.outerKinks) {
            diagram.line(listOf(Pair(tickX - 10, -pos), Pair(tickX - 15, -pos)), color)
        }
        for (pos in instrument.outer.pos.drop(1).dropLast(1)) {
            diagram.line(listOf(Pair(tickX - 12, -pos), Pair(tickX - 13, -pos)), color)
        }
    }

    /**
     * Set up a value to make it easy to pass the save function as
     * a parameter for the optimizer.
     */
    private val saver = { pars: ScoredParameters,
                  _: List<DesignParameters> -> save(pars) }



    private fun twiddleFiles(path: Path) {
        if (path.exists()) {
            val dir = path.parent
            val name = path.name
            val backup = dir / ("$name.bak")
            if (backup.exists()) {
                backup.deleteExisting()
            }
            path.moveTo(backup)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun save(params: ScoredParameters) {
        twiddleFiles(outputDir / "${name}-parameters.json5")
        val instrument = makeInstrumentFromParameters(params.parameters)
        val patchedInstrument = patchInstrument(instrument) as Inst
        patchedInstrument.prepare()
        patchedInstrument.preparePhase()
        writeInstrument(patchedInstrument, outputDir / "${name}-parameters.json5")


        drawInstrumentDiagram(patchedInstrument)
    }

    private fun drawInstrumentDiagram(patchedInstrument: Inst) {
        val diagram = Diagram()
        drawInstrumentOntoDiagram(diagram, patchedInstrument)

        var textX: Double = diagram.maxX * 1.25
        var textY = 0.0
        for (i in 0 until numberOfHoles) {
            var thisY = min(textY, -patchedInstrument.holePositions[i])
            textY = diagram.text(textX, thisY, "%.1fmm".format(patchedInstrument.holeDiameters[i]))
            diagram.text(textX + 90.0, thisY, " at %.1fmm".format(patchedInstrument.holePositions[i]))

            if (i < numberOfHoles - 1) {
                thisY =
                    min(textY, (-0.5 * (patchedInstrument.holePositions[i] + patchedInstrument.holePositions[i + 1])))
                diagram.text(
                    textX + 45.0,
                    thisY,
                    "%.1fmm".format(patchedInstrument.holePositions[i + 1] - patchedInstrument.holePositions[i])
                )
            }
        }
        diagram.text(
            textX + 90.0, min(textY, -patchedInstrument.length), "%.1fmm".format(patchedInstrument.length)
        )
        textX = diagram.maxX
        val graphX = textX + 200
        val emitX = graphX + 220
        textY = 0.0
        fingerings.map { item ->
            val fingers = item.fingers
            val w1 = item.wavelength(transpose)
            val w2: Double = if (item.nth != 0) {
                patchedInstrument.trueWavelengthNear(w1, fingers)
            } else {
                patchedInstrument.trueNthWavelengthNear(w1, fingers, item.nth)
            }
            val cents = round(log2(w2 / w1) * 1200.0).toInt()
            val nProbes = 301
            val maxCents = 2400.0
            val width = 200
            val step = 0.5.pow(maxCents / ((nProbes - 1) * 0.5 * 1200.0))
            val low = w1 * step.pow(-(nProbes - 1) / 2.0)
            val probes = (0 until nProbes).map { i ->
                low * step.pow(i)
            }

            val scores = probes.map { probe -> patchedInstrument.resonancePhase(probe, fingers) }

            val points = scores.mapIndexed { i, score ->
                Pair(
                    graphX + i * width / nProbes, textY - (((score + 0.5) % 1.0) - 0.5) * 14.0
                )
            }

            (0 until probes.size - 1).forEach { i ->
                val c = floor(scores[i] + 0.5)
                if (c == floor(scores[i + 1] + 0.5)) {
                    fun expr(c: Double, offset: Double): Int {
                        return floor((cos((c / 5.0 + offset) * PI * 2.0) * 0.5 + 0.5) * 200).toInt()
                    }

                    val rgb = listOf(0.0, 1.0 / 3, 2.0 / 3).map { offset -> expr(c, offset) }
                    diagram.line(
                        points.slice(i until i + 2), "#%02x%02x%02x".format(rgb[0], rgb[1], rgb[2]), 0.2
                    )
                }
            }
            diagram.line(
                listOf(Pair(graphX + width * 0.5, textY + 7), Pair(graphX + width * 0.5, textY - 7)), "#0000ff", 0.2
            )
            diagram.line(listOf(Pair(graphX, textY), Pair(graphX + width, textY)), "#0000ff", 0.2)
            diagram.text(
                textX, textY, "%5s %s %-4d cents".format(
                    describe(w1), if (cents == 0) {
                        "     "
                    } else if (cents > 0) {
                        " flat"
                    } else {
                        "sharp"
                    }, abs(cents)
                )
            )
            val phase = patchedInstrument.resonancePhase(w2, fingers)
            diagram.text(emitX, textY, "$phase")
            textY -= 25
        }

        diagram.text(
            graphX, textY - 10, "Nearby resonances:", color = "#000000"
        )
        textY -= 50.0 + 10.0 * max(innerDiameters.size, outerDiameters.size)

        diagram.text(graphX - 150.0, textY-70.0, "CHALUMIER INSTRUMENT DESIGNER", color="#550055")
        diagram.text(graphX - 150.0, textY-50.0, "Design for $name", color="#550055")


        diagram.text(graphX - 150.0, textY, "Outer diameters:", color = "#000000")
        val outerKinks = listOf(0.0) + patchedInstrument.outerKinks + listOf(patchedInstrument.length)
        outerDiameters.forEachIndexed { i, item ->
            diagram.text(
                graphX - 150.0,
                textY + 10.0 + (outerDiameters.size - i) * 10.0,
                describeLowHigh(item) + "mm at %.1fmm".format(outerKinks[i])
            )
        }

        diagram.text(graphX, textY, "Inner diameters:", color = "#000000")
        val innerKinks = listOf(0.0) + patchedInstrument.innerKinks + listOf(patchedInstrument.length)
        innerDiameters.forEachIndexed { i, item ->
            diagram.text(
                graphX,
                textY + 10.0 + (innerDiameters.size - i) * 10.0,
                describeLowHigh(item) + "mm at %.1fmm".format(innerKinks[i])
            )
        }
        twiddleFiles(outputDir / "${name}-design.svg")
        diagram.save(outputDir / "${name}-design.svg")
    }

    fun run(progressDisplay: ProgressDisplay,
            reportingInterval: Int = 5000) : Instrument {
        if (!outputDir.exists()) {
            outputDir.createDirectory()
        }
        val initialDesignParameters = initialDesignParameters()
        val newInstrument = optimizeInstrument(progressDisplay,
            constraintScorer, intonationScorer, initialDesignParameters,
            reportingInterval,
            monitor = saver)
        save(ScoredParameters(newInstrument, fullScore(newInstrument)))
        return makeInstrumentFromParameters(newInstrument)
    }

    open fun scaler(values: List<Double?>): ArrayList<Double?> {
        return ArrayList(values.map {
            if (it != null) {
                it * scale
            } else {
                null
            }
        })
    }

    fun sqrtScaler(values: List<Double?>): List<Double?> {
        val scaleFactor = scale.pow(0.5)
        return values.map {
            if (it != null) {
                it * scaleFactor
            } else {
                null
            }
        }
    }

    fun powerScaler(power: Double, values: List<Double?>): List<Double?> {
        val scaleFactor = scale.pow(power)
        return values.map {
            if (it != null) {
                it * scaleFactor
            } else {
                null
            }
        }
    }

    private fun optimizeInstrument(
        progressDisplay: ProgressDisplay,
        constrainer: (DesignParameters) -> Double,
        scorer: (DesignParameters) -> Double,
        initialDesignParameters: DesignParameters,
        reportingInterval: Int,
        monitor: (ScoredParameters, List<DesignParameters>) -> Unit): DesignParameters {
        val optimizer = Optimizer(instrumentName, initialDesignParameters,  constrainer, scorer, reportingInterval,
            progressReporter = progressDisplay, monitor=monitor)
        return optimizer.optimizeInstrument()
    }

}

abstract class InstrumentDesignerWithBoreScale<Inst: Instrument> (
    n: String,
    dir: Path,
    build: InstrumentFactory<Inst>
   ):  InstrumentDesigner<Inst>(n, dir, build) {

    open fun boreScaler(value: List<Double?>, maximum: Double = 1e30): ArrayList<Double?> {
        val scale = sqrt(scale) * boreScale
        return ArrayList(value.map { i ->
            if (i != null) {
                min(i * scale, maximum)
            } else {
                null
            }
        })
    }

    open fun boreScaler(value: List<Double>, maximum: Double = 1e30): List<Double> {
        val scale = sqrt(scale) * boreScale
        return ArrayList(value.map { i ->
            min(i * scale, maximum)
        })
    }

    open val boreScale: Double by DoubleParameter("Scaling factor to apply to bore diameters") { 1.0 }
}


