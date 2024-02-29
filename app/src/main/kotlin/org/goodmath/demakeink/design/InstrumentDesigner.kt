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
package org.goodmath.demakeink.design

import java.io.FileWriter
import java.lang.Math.log
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.goodmath.demakeink.diagram.Diagram
import org.goodmath.demakeink.errors.DemakeinException
import org.goodmath.demakeink.errors.RequiredParameterException
import org.goodmath.demakeink.errors.dAssert
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

interface InstrumentGenerator<T: Instrument<T>> {
  fun create(): T
}

fun <T> Int.repeat(f: (i: Int) -> T): List<T> {
  return (0 until this).map { f(it) }
}

@Serializable
data class Fingering(
        val noteName: String,
        val fingers: List<Double>,
        val nth: Double? = null
) {
  fun wavelength(transpose: Int): Double {
    return wavelength(noteName, transpose)
  }
}



fun wavelength(noteName: String, transpose: Int = 0): Double {
  val w = SPEED_OF_SOUND / frequency(noteName)
  return w / (2.0.pow(transpose.toDouble() / 12.0))
}



/**
 * MC: I've refactored this quite a bit, by moving all of the configurables up to be constructor
 * parameters, instead of things randomly initialized into class variables.
 */
open class InstrumentDesigner<T : Instrument<T>>(open val gen: InstrumentGenerator<T>)  {

  /**
   * All of the validations scattered in ph's code are moved here, and called before anything gets
   * filled in in the instrument designer.
   */
  fun validate() {
    dAssert(initialHoleFractions.size == numberOfHoles, "initialHoleFractions has wrong length")
    dAssert(
            initialHoleDiameterFractions.size == numberOfHoles,
            "initialHoleDiameterFractions has wrong length"
    )
    dAssert(
            initialInnerFractions.size == innerDiameters.size - 2,
            "initialInnerFractions has wrong length"
    )
    dAssert(
            initialOuterFractions.size == outerDiameters.size - 2,
            "initialOuterFractions has wrong length"
    )
    checkFingeringHoles(fingerings)
  }


  private fun checkFingeringHoles(fingerings: List<Fingering>): Int {
    val count = fingerings[0].fingers.size
    if (!fingerings.all { it.fingers.size != count }) {
      throw DemakeinException("All fingerings must have the same number of holes")
    }
    return count
  }

  open var fingerings: List<Fingering> by ConfigurationParameter("list of specifications of fingerings and the notes they should produce") {
    throw RequiredParameterException("fingerings")
  }

  open var length: Double by ConfigurationParameter("the length of the instrument") { it.initialLength * it.scale }

  open var maxLength: Double? by ConfigurationParameter("The maximum length that the design should make the instrument when modelling") { null }

  open var closedTop: Boolean by ConfigurationParameter("Is this a closed top instrument?") { false }

  open var initialLength: Double by ConfigurationParameter<InstrumentDesigner<T>, Double>("the initial length of the instrument before modeling") {
    throw RequiredParameterException("initialLength")
  }

  // ph; diameters of inner bore: first is diam at bottom, last is diam at top
  open var innerDiameters: List<Pair<Double, Double>> by ConfigurationParameter<InstrumentDesigner<T>, List<Pair<Double, Double>>>("innerDiameters") {
    throw RequiredParameterException("innerDiameters")
  }

  open var outerDiameters: List<Pair<Double, Double>> by
  ConfigurationParameter<InstrumentDesigner<T>, List<Pair<Double, Double>>>( "outerDiameters") {
    throw RequiredParameterException("outerDiameters", "must have at least two elements")
  }

  open var transpose: Int by ConfigurationParameter("transpose") { 0 }

  /**
   * Experimental. Add a term to the optimization to try to make instrument louder, possibly
   * sacrificing being-in-tune-ness.
   */
  open var tweakEmissions: Double by ConfigurationParameter("tweakEmissions") { 0.0 }

  open var innerAngles: List<Pair<Angle, Angle>?> by ConfigurationParameter("innerAngles") { c ->
    c.innerDiameters.map { null }
  }

  open var outerAngles: List<Pair<Angle, Angle>?> by ConfigurationParameter("outerAngles") { c ->
    c.outerDiameters.map { null }
  }

  open var coneStep: Double by ConfigurationParameter("coneStep") { 0.125 }

  open var numberOfHoles: Int by ConfigurationParameter("numberOfHoles") { c -> c.maxHoleDiameters.size }

  open var minHoleDiameters: List<Double> by ConfigurationParameter("minHoleDiameters") { c -> c.numberOfHoles.repeat { 0.5 } }

  open var maxHoleDiameters: List<Double> by ConfigurationParameter("maxHoleDiameters") {
    throw RequiredParameterException("maxHoleDiameters")
  }

  open var outerAdd: Boolean by ConfigurationParameter("outerAdd") { false }

  open var topClearanceFraction: Double by ConfigurationParameter("topClearanceFraction") { 0.0 }

  open var bottomClearanceFraction: Double by ConfigurationParameter("bottomClearanceFraction") { 0.0 }

  open var scale: Double by ConfigurationParameter("scale") { 1.0 }

  open var minHoleSpacing: List<Double?> by ConfigurationParameter ("a list of values specifying the minimum distance between pairs of holes"){ it.numberOfHoles.repeat { 0.0 } }

  open var maxHoleSpacing: List<Double?> by ConfigurationParameter("maxHoleSpacing") { c ->
    c.numberOfHoles.repeat { c.initialLength }
  }

  open var balance: List<Double?> by ConfigurationParameter("balance") { c -> (c.numberOfHoles - 2).repeat { null } }
  open var holeAngles: List<Double> by ConfigurationParameter("holeAngles") { it.numberOfHoles.repeat { 0.0 } }

  open var initialInnerFractions: List<Double?> by ConfigurationParameter("initialInnerFractions") { c ->
    (c.innerDiameters.size - 2).repeat { (it + 1.0) / (c.innerDiameters.size - 1) }
  }

  open var minInnerFractionSep: List<Double> by ConfigurationParameter("minInnerFractionSep") {
    (it.innerDiameters.size - 1).repeat { 0.0 }
  }
  open var maxInnerFractionSep: List<Double> by ConfigurationParameter("maxInnerFractionSep") {
    (it.innerDiameters.size - 1).repeat { 1.0 }
  }

  open var minInnerSep: List<Double?> by ConfigurationParameter("minInnerSep") {
    (it.innerDiameters.size - 1).repeat { null }
  }

  open var maxInnerSep: List<Double?> by ConfigurationParameter("maxInnerSep") {
    (it.innerDiameters.size - 1).repeat { null }
  }
  open var initialOuterFractions: List<Double?> by ConfigurationParameter("initialOuterFractions") { c ->
    (c.outerDiameters.size - 2).repeat { (it + 1.0) / (c.outerDiameters.size - 1) }
  }

  open var minOuterFractionSep: List<Double> by ConfigurationParameter("minOuterFractionSep") { c ->
    (c.outerDiameters.size - 1).repeat { 0.0 }
  }

  open var maxOuterFractionSep: List<Double> by ConfigurationParameter("maxOuterFractionSep") {
    (it.outerDiameters.size - 2).repeat { 1.0 }
  }

  open var initialHoleFractions: List<Double?> by ConfigurationParameter("initialHoleFractions") { c ->
    c.numberOfHoles.repeat { (it + 3.0) / (c.numberOfHoles + 2) * 0.5 }
  }

  open var initialHoleDiameterFractions: List<Double?> by ConfigurationParameter("initialHoleDiameterFractions") {
    it.numberOfHoles.repeat { 0.75 }
  }


  open var holeHorizAngles: List<Double> by ConfigurationParameter("holeHorizAngles") {
    (0 until numberOfHoles).map { 0.0 }
  }


  open var divisions: List<List<Pair<Int, Double>>> by ConfigurationParameter("divisions") {
    listOf(listOf(Pair(5, 0.0)),
            listOf(Pair(2, 0.0), Pair(5, 0.333)),
            listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.333)),
            listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.0), Pair(5, 0.7)))
  }



  val initialStateVec: ArrayList<Double?>
    get() {
      val result = ArrayList<Double?>()
      result.add(1.0)
      result.addAll(initialHoleFractions)
      result.addAll(initialHoleFractions)
      result.addAll(initialHoleDiameterFractions.map { i -> i?.let { it * it } })
      result.addAll(initialInnerFractions)
      result.addAll(initialOuterFractions)
      return result
    }

  fun unpack(stateVec: List<Double?>): T {
    val (innerLow, innerHigh) = lowHigh(innerDiameters)
    val (outerLow, outerHigh) = lowHigh(outerDiameters)
    val (innerAngleLow, innerAngleHigh) = lowHighOpt(innerAngles)
    val (outerAngleLow, outerAngleHigh) = lowHighOpt(outerAngles)

    val inst = gen.create()
    var p = 0
    inst.length = (stateVec[0] ?: 1.0) * initialLength * scale
    p += 1
    inst.holePositions =
      ArrayList(stateVec.slice(p until p + numberOfHoles).map { it!! * inst.length })
    p += inst.numberOfHoles
    inst.holeDiameters =
      ArrayList(
        stateVec.slice(p until p + numberOfHoles).mapIndexed { i, item ->
          minHoleDiameters[i] +
                  signed_sqrt(item ?: 0.0) * (maxHoleDiameters[i] - minHoleDiameters[i])
        }
      )
    p += numberOfHoles
    val innerKinks =
      ArrayList(
        stateVec.slice(p until p + innerDiameters.size - 2).map { item -> item!! * inst.length }
      )
    p += (innerDiameters.size - 2)
    val outerKinks =
      ArrayList(stateVec.slice(p until p + outerDiameters.size - 2).map { it!! * inst.length })
    p += (outerDiameters.size - 2)

    dAssert(p == stateVec.size, "Invalid state vec; expected size ${p}, but found ${stateVec.size}")

    inst.inner =
            Profile.curvedProfile(
                    ArrayList(listOf(0.0) + innerKinks + listOf(inst.length)),
                    innerLow,
                    innerHigh,
                    innerAngleLow,
                    innerAngleHigh
            )

    inst.outer =
            Profile.curvedProfile(
                    ArrayList(listOf(0.0) + outerKinks + listOf(inst.length)),
                    outerLow,
                    outerHigh,
                    outerAngleLow,
                    outerAngleHigh
            )

    if (outerAdd) {
      inst.outer += inst.inner
    }

    inst.holeAngles = holeAngles
    inst.innerHolePositions = ArrayList(numberOfHoles.repeat { 0.0 })
    inst.holeLengths = ArrayList(numberOfHoles.repeat { 0.0 })

    (0 until numberOfHoles).forEach { i ->
      // ph: Note: approximates bore as cylindrical calculating shift, length
      val radians = inst.holeAngles[i] * PI / 180.0
      val thickness = (inst.outer(inst.holePositions[i]) - inst.inner(inst.holePositions[i])) * 0.5
      val shift = sin(radians) * thickness

      inst.innerHolePositions[i] = inst.holePositions[i] + shift
      inst.holeLengths[i] =
        (sqrt(thickness * thickness + shift * shift)
                // ph: #+ self.hole_extra_height_by_diameter[i] * inst.hole_diameters[i]
                )
      inst.innerKinks = innerKinks
      inst.outerKinks = outerKinks

      inst.coneStep = coneStep
      inst.closedTop = closedTop
    }
    return inst
  }

  fun constraintScore(inst: Instrument<T>): Double {
    val scores = ArrayList<Double>()
    scores.add(inst.length)

    if (maxLength != null) {
      scores.add(maxLength!! * scale - inst.length)
    }
    val inners = ArrayList(listOf(0.0) + inst.innerKinks + listOf(inst.length))
    for (i in 0 until inners.size - 1) {
      val sep = inners[i + 1] - inners[i]
      val minCheck = sep - minInnerFractionSep[i] * inst.length
      scores.add(minCheck)
      val maxCheck = maxInnerFractionSep[i] * inst.length - sep
      scores.add(maxCheck)
      if (minInnerSep[i] != null) {
        scores.add(sep - minInnerSep[i]!! * scale)
      }
      if (maxInnerSep[i] != null) {
        scores.add(maxInnerSep[i]!! * scale - sep)
      }
    }
    val outers = ArrayList(listOf(0.0) + inst.outerKinks + listOf(inst.length))
    for (i in 0 until outers.size - 1) {
      val sep = outers[i + 1] - outers[i]
      val minCheck = sep - minOuterFractionSep[i] * inst.length
      scores.add(minCheck)
      val maxCheck = maxOuterFractionSep[i] * inst.length - sep
      scores.add(maxCheck)
    }
    val bottomCheck = inst.holePositions[0] - bottomClearanceFraction * inst.length
    scores.add(bottomCheck)
    val topCheck = (1.0 - topClearanceFraction) * inst.length - inst.holePositions.last()
    scores.add(topCheck)
    minHoleSpacing.forEachIndexed { i, value ->
      if (value != null) {
        val check = (inst.holePositions[i + 1] - inst.holePositions[i]) - value
        scores.add(check)
      }
    }
    maxHoleSpacing.forEachIndexed { i, value ->
      if (value != null) {
        val check = value - (inst.holePositions[i + 1] - inst.holePositions[i])
        scores.add(check)
      }
    }
    minHoleDiameters.forEachIndexed { i, value ->
      val check = inst.holeDiameters[i] - value
      scores.add(check)
    }
    maxHoleDiameters.forEachIndexed { i, value ->
      val check = value - inst.holeDiameters[i]
      scores.add(check)
    }
    balance.forEachIndexed { i, value ->
      if (value != null) {
        val check =
          value * 0.5 * (inst.holePositions[i + 2] - inst.holePositions[i]) -
                  abs(
                    0.5 * inst.holePositions[i] + 0.5 * inst.holePositions[i + 2] -
                            inst.holePositions[i + 1]
                  )
        scores.add(check)
      }
    }
    return scores.map { x -> -x }.sum()
  }

  /** ph: Hook to modify instrument before scoring. */
  open fun patchInstrument(inst: T): T {
    return inst
  }

  /**
   * ph: Hook for how to calculate emission.
   *
   * Lets Flute_designer rate emission relative to embouchure hole.
   */
  open fun calcEmission(emission: List<Double>, fingers: List<Double>): Double {
    return sqrt(emission.sumOf { e -> e * e })
  }

  fun score(i: T): Double {
    val inst = patchInstrument(i)
    var score = 0.0
    var div = 0.0

    var emissionScore = 0.0
    // ph: var emission_score2 = 0.0
    var emissionDiv = 0.0
    if (tweakEmissions != 0.0) {
      inst.prepare()
    }
    inst.preparePhase()
    val s = 1200.0 / ln(2.0)
    for (item in fingerings) {
      val fingers = item.fingers
      val w1 = item.wavelength(transpose)
      val w2 =
        if (item.nth == null) {
          inst.trueWavelengthNear(w1, fingers)
        } else {
          inst.trueNthWavelengthNear(w1, fingers, item.nth)
        }
      val diff = abs(log(w1) - log(w2)) * s
      // ph: weight = w1
      val weight = 1.0
      // ph: score += weight * diff**3 / (1.0 + (diff/20.0)**2)
      score += weight * diff.pow(3)
      div += weight
      /* ph:
      score += (max(0.0,inst.resonance_score(w2/2.0, fingers)) * 1000.0)**3
      diff += max(0.0,inst.resonance_score(w2/3.0, fingers)) * 100.0
      diff += max(0.0,inst.resonance_score(w2/4.0, fingers)) * 100.0
      if inst.resonance_score(w2/3.0, fingers) < 0.0:
          diff += 100.0
      if inst.resonance_score(w2/4.0, fingers) < 0.0:
          diff += 100.0
       */
      if (tweakEmissions != 0.0) {
        val emissionWeight = 1.0 // ph: w1
        emissionDiv += emissionWeight
        val (_, emission) = inst.resonanceScore(w2, fingers, true)
        val rms = calcEmission(emission!!, fingers)
        // ph: math.sqrt(sum(item*item for item in emission))
        val x = log(rms)
        emissionScore += emissionWeight * x
        // ph: emission_score2 += emission_weight * x * x
      }
    }
    var result = (score / div).pow(1.0 / 3.0)
    if (tweakEmissions != 0.0) {
      val x = emissionScore / emissionDiv
      // ph: #x2 = emission_score2 / emission_div
      // ph: #var = x2-x*x
      result += (tweakEmissions * -x) // ph: #math.sqrt(var)/x
    }
    return result
    // ph: #return ( (score/scale) ** (1.0/2) )*100.0
  }

  val constrainer: (stateVec: ArrayList<Double?>) -> Double = {
    constraintScore(unpack(it))
  }

  val scorer: (stateVec: ArrayList<Double?>) -> Double = {
    score(unpack(it))
  }

  fun _draw(
          diagram: Diagram,
          stateVec: ArrayList<Double?>,
          color: String = "#000000",
          redColor: String = "#ff0000"
  ) {
    val instrument = unpack(stateVec)
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
        listOf(Pair(-0.5 * d, -instrument.length), Pair(0.5 * d, -instrument.length)),
        color
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

  var stateVec: ArrayList<Double?>? = null
  var instrument: T? = null

  val saver: (outputDir: Path, stateVec: ArrayList<Double?>, otherVecs: List<ArrayList<Double?>>) -> Unit =
    { outputDir, stateVec, otherVecs ->
      save(outputDir, stateVec, otherVecs)
    }

  fun save(outputDir: Path, stateVec: ArrayList<Double?>, otherVecs: List<ArrayList<Double?>>) {
    this.stateVec = stateVec
    instrument = unpack(stateVec)
    val output = FileWriter((outputDir / "data.json").toFile())
    output.write(Json.encodeToString(this))
    val patchedInstrument = patchInstrument(instrument!!)
    patchedInstrument.prepare()
    patchedInstrument.preparePhase()
    /*ph:
    #  #any_extra = any( item != 0.0 for item in self.hole_extra_height_by_diameter )
    #  #if any_extra:
    #  #TODO: implement embextra using patch_instrument
    #  mod_instrument = self.unpack( state_vec )
    #  #mod_instrument.hole_extra_height_by_diameter = [ 0.0 ] * len(self.hole_extra_height_by_diameter)
    #  mod_instrument.hole_lengths = [
    #      (mod_instrument.outer(mod_instrument.hole_positions[i])
    #       - mod_instrument.inner(mod_instrument.hole_positions[i])) * 0.5
    #      for i in range(self.n_holes)
    #  ]
    #  mod_instrument.prepare()
    */

    val diagram = Diagram()
    otherVecs.shuffled()
    for (i in 0 until min(20, otherVecs.size)) {
      _draw(diagram, otherVecs[i], "#aaaaaa", "#ffaaaa")
    }
    _draw(diagram, stateVec)
    /* ph:

    #for i in range(self.n_holes):
    #    diagram.circle(0.0, -self.instrument.inner_hole_positions[i], self.instrument.hole_diameters[i],
    #                   '#ff0000')
    #    diagram.circle(0.0, -self.instrument.hole_positions[i], self.instrument.hole_diameters[i])
    #diagram.profile(self.instrument.outer)
    #diagram.profile(self.instrument.stepped_inner)
    #
    #if self.closed_top:
    #    d = self.instrument.stepped_inner(self.instrument.length)
    #    diagram.line([(-0.5*d,-self.instrument.length),(0.5*d,-self.instrument.length)])

     */
    var textX: Double = diagram.maxX
    var textY: Double = 0.0
    val inst = instrument!!
    for (i in 0 until numberOfHoles) {
      var thisY = min(textY, -inst.holePositions[i])
      textY = diagram.text(textX, thisY.toDouble(), "%.1fmm".format(inst.holeDiameters[i]))
      diagram.text(textX + 90.0, thisY, " at %.1fmm".format(instrument!!.holePositions[i]))

      if (i < numberOfHoles - 1) {
        thisY =
          min(textY, (-0.5 * (instrument!!.holePositions[i] + instrument!!.holePositions[i + 1])))
        // ph: #text_y =
        diagram.text(
          textX + 45.0,
          thisY,
          "%.1fmm".format(instrument!!.holePositions[i + 1] - instrument!!.holePositions[i])
        )
      }
    }
    diagram.text(
      textX + 90.0,
      min(textY, -instrument!!.length),
      "%.1fmm".format(instrument!!.length)
    )
    textX = diagram.maxX
    val graphX = textX + 200
    val emitX = graphX + 220
    textY = 0.0
    fingerings.map { item ->
      val note = item.noteName
      val fingers = item.fingers
      val w1 = item.wavelength(transpose)
      var w2: Double =
        if (item.nth != 0.0) {
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
      val probes = (0 until nProbes).map { i -> low * step.pow(i) }
      /* phd:
       patched_instrument.resonance_score(probe, fingers) for probe in probes ]
      #
      #points = [ (graph_x+i*width/n_probes,text_y-score*7.0)
      #           for i,score in enumerate(scores) ]
      #for i in range(len(probes)-1):
      #    if scores[i] > 0 and scores[i+1] < 0: continue
      #    diagram.line(points[i:i+2], '#000000', 0.2)
       */
      val scores = probes.map { probe -> patchedInstrument.resonancePhase(probe, fingers) }

      val points =
        scores.mapIndexed { i, score ->
          Pair<Double, Double>(
            graphX + i * width / nProbes,
            textY - (((score + 0.5) % 1.0) - 0.5) * 14.0
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
            points.slice(i until i + 2),
            "#%02x%02x%02x".format(rgb[0], rgb[1], rgb[2]),
            0.2
          )
        }
      }
      diagram.line(
        listOf(Pair(graphX + width * 0.5, textY + 7), Pair(graphX + width * 0.5, textY - 7)),
        "#0000ff",
        0.2
      )
      diagram.line(listOf(Pair(graphX, textY), Pair(graphX + width, textY)), "#0000ff", 0.2)
      diagram.text(
        textX,
        textY,
        "%5s %s %-4d cents".format(
          describe(w1),
          if (cents == 0) {
            "     "
          } else if (cents > 0) {
            " flat"
          } else {
            "sharp"
          },
          abs(cents)
        )
      )
      /* ph:
      #_, emission = patched_instrument.resonance_score(w2,fingers,True)
      #diagram.text(emit_x, text_y,
      #     ', '.join('%.2f' % item for item in emission)
      #     )
      */
      val phase = patchedInstrument.resonancePhase(w2, fingers)
      diagram.text(emitX, textY, "${phase}")
      /* ph:
      #if any_extra:
      #w3 = mod_instrument.true_wavelength_near(w1, fingers)
      #cents3 = int(round( log2(w3/w1) * 1200.0 ))
      #diagram.text(graph_x + width, text_y,
      #      '%s %-4d cents grad=%.1f diff=%d' % ('     ' if cents3 == 0 else ' flat' if cents3 > 0 else 'sharp', abs(cents3), grad3, cents3-cents)
      #)
      */

      textY -= 25
    }

    diagram.text(
      graphX, textY - 10,
      "Nearby resonances:",
      color = "#000000"
    )
    /*
    #diagram.text(emit_x, text_y - 20,
        #    'Volume of sound emission from\n'
        #    'instrument end and open holes:',
        #    color='#000000',
        #    )
     */
    textY -= 50.0 + 10.0 * max(innerDiameters.size, outerDiameters.size)

    diagram.text(graphX - 150.0, textY, "Outer diameters:", color = "#000000")
    val outerKinks = listOf(0.0) + inst.outerKinks + listOf(inst.length)
    outerDiameters.forEachIndexed { i, item ->
      diagram.text(
        graphX - 150.0, textY + 10.0 + outerDiameters.size - i * 10.0,
        describeLowHigh(item) + "mm at %.1fmm".format(outerKinks[i])
      )
    }

    diagram.text(graphX, textY, "Inner diameters:", color = "#000000")
    val innerKinks = listOf(0.0) + inst.innerKinks + listOf(inst.length)
    innerDiameters.forEachIndexed { i, item ->
      diagram.text(
        graphX, textY + 10.0 + (innerDiameters.size - i) * 10.0,
        describeLowHigh(item) + "mm at %.1fmm".format(outerKinks[i])
      )
    }
    diagram.save(outputDir / "diagram.svg")
  }

  fun run(outputDir: Path) {
    if (!outputDir.exists()) {
      outputDir.createDirectory()
    }
    var stateVec = initialStateVec
//    stateVec = optimize.improve("/bin/zsh", constrainer, scorer, stateVec, monitor = saver)

    save(outputDir, stateVec, emptyList())

    // ph: #print(self._opt_score(state_vec))

  }

  open fun scaler(values: List<Double?>): List<Double?> {
    return values.map {
      if (it != null) { it * scale }
      else { null }
    }
  }



  companion object {
    val O = 0.0
    val X = 1.0
  }

}

/* ph:
def bore_scaler(value, maximum=1e30):
    @property
    def func(self):
        scale = math.sqrt(self.scale) * self.bore_scale
        return [ min(item*scale, maximum) if item is not None else None for item in value ]
    return func

 */

/**
 * @param boreScale Bore diameter is scaled as the square root of the instrument size times this amount.
*/
open class InstrumentDesignerWithBoreScale<T: Instrument<T>>( gen: InstrumentGenerator<T>):
        InstrumentDesigner<T>(gen) {

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
      if (i != null) {
        min(i * scale, maximum)
      } else {
        null
      }
    })
  }

  open val boreScale: Double by ConfigurationParameter { 1.0 }
}

