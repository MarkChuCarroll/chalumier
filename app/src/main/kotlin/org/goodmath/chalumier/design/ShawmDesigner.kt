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
package org.goodmath.chalumier.design

import org.goodmath.chalumier.config.*
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path

open class ReedInstrumentDesigner(override val name: String,
                                  outputDir: Path):
    InstrumentDesigner(name, outputDir) {

  open val boreBaseline = 4.0

  open var bore by DoubleParameter("Bore diameter at top. (ie reed diameter)") { 4.0 }

  // ph: reed_virtual_length = 25.0
  // ph: reed_virtual_top = 1.0
  // ph: reed_virtual_length = 50.0
  // ph: reed_virtual_top = 0.125

  // ph: From c5 drone
  open var reedVirtualLength by
      DoubleParameter("Virtual length of reed, as a multiple of bore diameter.") { 34.0 }

  open var reedVirtualTop by
      DoubleParameter("Virtual diameter of top of reed, proportion of bore diameter.") { 1.0 }

  override var transpose by IntParameter() { 0 }
  override var closedTop by BooleanParameter() { true }

  override fun patchInstrument(origInst: Instrument): Instrument {
    val inst = origInst.copy()
    // MarkCC: ph originally had a "true_length" and "true_inner" defined
    // here. But "true_inner" was never used, so I dropped it. True_length
    // was just initialized to "length" before length was modified.
    inst.trueLength = length
    val reedLength = bore * reedVirtualLength
    val reedTop = bore * reedVirtualTop
    val reed = Profile.makeProfile(listOf(listOf(0.0, bore),arrayListOf(reedLength, reedTop)))
    inst.inner += reed
    inst.length += reedLength
    return inst
  }

  fun boreScaler(value: List<Double>): ArrayList<Double> {
    val scale = bore / boreBaseline
    return ArrayList(value.map { it * scale })
  }

  companion object {
    fun inRange(low: Double, high: Double, n: Int): MutableList<Double> {
      return ArrayList((1 until n + 1).map {
        val i = it.toDouble()
        (i + 1.0) * (high - low) / (n + 2) + low
      })
    }

    fun fullRange(low: Double, high: Double, n: Int): MutableList<Double> {
      return ArrayList((0 until n).map {
        val i = it.toDouble()
        i * (high - low) / (n - 1.0) + low
      })
    }
  }
}

class ReedDroneDesigner(override val name: String,
    outputDir: Path) : ReedInstrumentDesigner(name, outputDir) {

  override var initialLength by DoubleParameter { wavelength("C4") * 0.25 }
  override var innerDiameters by
      ListOfDoublePairParameter() {
        boreScaler(listOf(4.0, 4.0)).map { Pair(it, it) }.toMutableList()
      }
  override var outerDiameters by ListOfDoublePairParameter() {
        boreScaler(listOf(24.0, 12.0)).map { Pair(it, it) }.toMutableList()
      }
  override var holeHorizAngles by ListOfDoubleParameter() { mutableListOf() }

  // I"m not sure what this is for; I"m going to leave it out until I"m
  // sure it does something.
  // withFingerPad =arrayListOf()

  override var fingerings by ListOfFingeringsParam {
          listOf(Fingering("C4", ArrayList<Double>(), 1.0)).toMutableList()
  }

  override var divisions by ListOfListOfIntDoublePairParam { emptyList() }
}

open class ReedpipeDesigner(override val name: String, outputDir: Path):
    ReedInstrumentDesigner(name, outputDir) {
  override var innerDiameters by
      ListOfDoublePairParameter() {
        boreScaler(listOf(4.0, 4.0)).map { Pair(it, it) }.toMutableList()
      }

  override var outerDiameters by
      ListOfDoublePairParameter() { boreScaler(listOf(12.0, 12.0)).map { Pair(it, it) } }

  override var minHoleDiameters by
      ListOfDoubleParameter() { boreScaler(listOf(2.5).repeat(8)).toMutableList() }

  override var maxHoleDiameters by
      ListOfDoubleParameter() { boreScaler(listOf(4.0).repeat(8)).toMutableList() }

  // ph: max_hole_spacing = design.scaler([ 8O, 4O,4O,4O,None,4O,4O, 20 ])

  override var balance by
      ListOfOptDoubleParameter() { mutableListOf(0.2, 0.075, 0.3, 0.3, 0.075, null) }

  // ph: balance = [ None, 0.1, 0.1, 0.3, 0.3, 0.1, None ]
  // ph: balance = [ 0.2, 0.1, None, None, 0.1, None ]

  override var holeAngles by
      ListOfDoubleParameter() { mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) }

  override var holeHorizAngles by
      ListOfDoubleParameter() { mutableListOf(-25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 180.0) }

  // with_fingerpad = [1,1,1,1,1,1,1,1]

  override var initialLength by DoubleParameter() { wavelength("C4") * 0.25 }

  override var fingerings by ListOfFingeringsParam {
        mutableListOf(
            Fingering("C4",arrayListOf(X, X, X, X, X, X, X, X), 1.0),
            Fingering("D4",arrayListOf(O, X, X, X, X, X, X, X), 1.0),
            Fingering("E4",arrayListOf(O, O, X, X, X, X, X, X), 1.0),
            Fingering("F4",arrayListOf(O, O, O, X, X, X, X, X), 1.0),
            Fingering("G4",arrayListOf(O, O, O, O, X, X, X, X), 1.0),
            Fingering("A4",arrayListOf(O, O, O, O, O, X, X, X), 1.0),
            // ph: ("Bb4", [O,O,O,X,X,O,X,X), 1),
            Fingering("B4",arrayListOf(O, O, O, O, O, O, X, X), 1.0),
            Fingering("C5",arrayListOf(O, O, O, O, O, X, O, X), 1.0),
            // ph: ("C#5", [O,O,O,O,O,X,X,O), 1),
            Fingering("D5",arrayListOf(O, O, O, O, O, X, O, O), 1.0)
        )
      }

  override var divisions by ListOfListOfIntDoublePairParam {
        // ph: [ (3, 0.5) ],
        // ph: [ (3, 0.5), (7, 0.1) ],
        mutableListOf(mutableListOf(Pair(0, 0.5), Pair(3, 0.5), Pair(7, 0.1)))
      }
}

abstract class AbstractShawmDesigner(override val name: String,
                                     outputDir: Path):
    ReedInstrumentDesigner(name, outputDir) {
  override var innerDiameters: List<Pair<Double, Double>> by ListOfDoublePairParameter() {
    boreScaler(fullRange(16.0, 4.0, 10)).map { Pair(it, it) }
    // ph:arrayListOf(  16.0, 14.0, 12.0, 10.0, 8.0, 6.0, 4.0, 1.5 )
  }
  override var initialInnerFractions by ListOfDoubleParameter() { fullRange(0.2, 0.9, 8) }
  override var minInnerFractionSep by ListOfDoubleParameter() { ArrayList(listOf(0.02).repeat(9)) }
  override var outerDiameters by ListOfDoublePairParameter() {
    boreScaler(listOf(70.0, 25.0, 25.0)).map { Pair(it, it) }
  }
  override var minOuterFractionSep by ListOfDoubleParameter() { arrayListOf(0.19, 0.8) }
  override var initialOuterFractions by ListOfDoubleParameter() {arrayListOf(0.19) }
  override var outerAngles by ListOfOptAnglePairsParameter() {
      ArrayList(listOf(Angle(AngleDirection.Here, -35.0), Angle(AngleDirection.Up), Angle(AngleDirection.Down))
        .map { Pair(it, it) })
  }
}


/**
 * Designer for a shawm/haut-bois/oboe/bombard with a fingering system similar to recorder.
 *
 * The flare at the end is purely decorative.
 */
open class ShawmDesigner(override val name: String,
                         outputDir: Path):
    AbstractShawmDesigner(name, outputDir) {

  override var minHoleDiameters by ListOfDoubleParameter() {
    boreScaler(listOf(2.0).repeat(9))
  }

  override var maxHoleDiameters by ListOfDoubleParameter() {
    // ph:  max_hole_diameters = bore_scaler([ 12.0 ] * 8)
    boreScaler(listOf(6.0).repeat(9))
  }

  override var initialHoleDiameterFractions by ListOfDoubleParameter() {
      ArrayList(listOf(0.5).repeat(9))
  }

  override var initialHoleFractions by ListOfDoubleParameter() {
   ArrayList(arrayListOf(7, 6, 5, 4, 3, 2, 1, 0, 0).map { i -> 0.5 - (0.6 * i.toDouble()) })
  }

  override var maxHoleSpacing by ListOfOptDoubleParameter(){
    scaler(listOf(80.0, 40.0, 40.0, 40.0, null, 40.0, 40.0, 20.0))
  }

  override var balance by ListOfOptDoubleParameter() {
    // ph: balance = [0.2, 0.1, 0.3, 0.3, 0.1, None]
   arrayListOf(null, 0.1, 0.1, 0.3, 0.3, 0.1, null)
    // ph: balance = [0.2, 0.1, None, None, 0.1, None]
  }

  override var holeAngles by ListOfDoubleParameter() {
      arrayListOf(0.0, -30.0, -30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0)
  }

  override var holeHorizAngles by ListOfDoubleParameter() {
      arrayListOf(30.0, -25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 180.0)
  }

  // with_fingerpad = [0,1,1,1,1,1,1,1,1]

  override var initialLength by DoubleParameter() { wavelength("B3") * 0.35 }

  override var fingerings by ConfigParameter(ListOfFingeringsKind) {
    arrayListOf(
        Fingering("B3",arrayListOf(X, X, X, X, X, X, X, X, X), 1.0),
        Fingering("B4",arrayListOf(X, X, X, X, X, X, X, X, X), 2.0),
        Fingering("C4",arrayListOf(O, X, X, X, X, X, X, X, X), 1.0),
        Fingering("D4",arrayListOf(O, O, X, X, X, X, X, X, X), 1.0),
        Fingering("E4",arrayListOf(O, O, O, X, X, X, X, X, X), 1.0),
        Fingering("F4",arrayListOf(O, X, X, O, X, X, X, X, X), 1.0),
        Fingering("F#4",arrayListOf(O, O, X, X, O, X, X, X, X), 1.0),
        Fingering("G4",arrayListOf(O, O, O, O, O, X, X, X, X), 1.0),
        // ph: Fingering("G#4",arrayListOf(O, O,X,X,X,O,X,X,X), 1),
        Fingering("A4",arrayListOf(O, O, O, O, O, O, X, X, X), 1.0),
        Fingering("Bb4",arrayListOf(O, O, O, O, X, X, O, X, X), 1.0),
        Fingering("B4",arrayListOf(O, O, O, O, O, O, O, X, X), 1.0),
        Fingering("C5",arrayListOf(O, O, O, O, O, O, X, O, X), 1.0),
        Fingering("C#5",arrayListOf(O, O, O, O, O, O, X, X, O), 1.0),
        Fingering("D5",arrayListOf(O, O, O, O, O, O, X, O, O), 1.0), // ph: #?
        Fingering("C5",arrayListOf(O, X, X, X, X, X, X, X, X), 2.0),
        Fingering("D5",arrayListOf(O, O, X, X, X, X, X, X, X), 2.0),
        Fingering("E5",arrayListOf(O, O, O, X, X, X, X, X, X), 2.0),
        Fingering(
            "E5",
           arrayListOf(O, O, O, X, X, X, X, X, O),
            2.0
        ), // Register hole exactly at node for E
        Fingering("F5",arrayListOf(O, O, X, O, X, X, X, X, X), 2.0),
        Fingering("F#5",arrayListOf(O, O, O, X, O, X, X, X, X), 2.0),
        Fingering("G5",arrayListOf(O, O, O, O, O, X, X, X, X), 2.0),
        // ph: Fingering("G#5",arrayListOf(O,O,O,X,O,X,X,X), 2),
        Fingering("A5",arrayListOf(O, O, O, O, O, O, X, X, X), 2.0),
        // ph: Fingering("B5", arrayListOf(O,O,X,X,O,X,X,X), 2),
        // ph: Fingering("C5", arrayListOf(O,O,X,X,O,O,X,X), 2),
        // ph: Fingering("B5", arrayListOf(O,O,O,O,O,O,X,X)),
        // ph: Fingering("C6", arrayListOf(O,O,O,O,O,X,O,X))
        )
  }

  override var divisions by ConfigParameter(ListOfListOfIntDoublePairKind) {
   arrayListOf(
       arrayListOf(Pair(4, 0.5)),
       arrayListOf(Pair(1, 0.25), Pair(4, 0.5)),
       arrayListOf(Pair(0, 0.25), Pair(2, 0.5), Pair(5, 0.0))
    )
  }
}

/**
 * Designer for a shawm/haut-bois/oboe/bombard with a simple fingering system and compact hole
 * placement. The flare at the end is purely decorative.
 */
class FolkShawmDesigner(override val name: String,
                        outputDir: Path
): AbstractShawmDesigner(name, outputDir) {

  override var minHoleDiameters by ListOfDoubleParameter() {
    boreScaler(listOf(12.0).repeat(7))
  }

  override var maxHoleDiameters by ListOfDoubleParameter() {
    boreScaler(listOf(12.0).repeat(7))
  }

  override var initialHoleDiameterFractions by ListOfDoubleParameter {
    inRange(1.0, 0.5, 7)
  }

  override var initialHoleFractions by ListOfDoubleParameter() {
   listOf(6, 5, 4, 3, 2, 1, 0).map { i -> 0.75 - 0.1 * i.toDouble() }
  }

  override var balance by ListOfOptDoubleParameter() {arrayListOf(null, 0.05, null, null, 0.05) }

  override var holeHorizAngles by ListOfDoubleParameter() {
   arrayListOf(45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
  }

  // with_fingerpad = [0,1,1,1,1,1,1]

  override var initialLength by DoubleParameter { wavelength("C4") * 0.5 }

  override var fingerings: ArrayList<Fingering> by ConfigParameter(ListOfFingeringsKind) {
   arrayListOf(
        Fingering("C4",arrayListOf(X, X, X, X, X, X, X), 1.0),
        Fingering("C5",arrayListOf(X, X, X, X, X, X, X), 2.0),
        Fingering("C4*3",arrayListOf(X, X, X, X, X, X, X), 3.0),
        Fingering("C4*4",arrayListOf(X, X, X, X, X, X, X), 4.0),
        Fingering("D4",arrayListOf(O, X, X, X, X, X, X), 1.0),
        Fingering("E4",arrayListOf(O, O, X, X, X, X, X), 1.0),
        Fingering("F#4",arrayListOf(O, O, O, X, X, X, X), 1.0),
        Fingering("G4",arrayListOf(O, O, O, O, X, X, X), 1.0),
        Fingering("A4",arrayListOf(O, O, O, O, O, X, X), 1.0),
        Fingering("B4",arrayListOf(O, O, O, O, O, O, X), 1.0),
        // ph: Fingering("C5",     arrayListOf(O, O,O,O,X,X,O), 1),
        Fingering("C#5",arrayListOf(O, O, O, O, O, O, O), 1.0),
        Fingering("D5",arrayListOf(O, X, X, X, X, X, O), 2.0),
        Fingering("D5",arrayListOf(O, X, X, X, X, X, X), 2.0),
        Fingering("E5",arrayListOf(O, O, X, X, X, X, X), 2.0),
        // ph: Fingering("E5",     arrayListOf(O,X,X,X,X,O)),
        Fingering("F#5",arrayListOf(O, O, O, X, X, X, X), 2.0),
        Fingering("G5",arrayListOf(O, O, O, O, X, X, X), 2.0),
        Fingering("A5",arrayListOf(O, O, O, O, O, X, X), 2.0),
        Fingering("B5",arrayListOf(O, O, O, O, O, O, X), 2.0),
        Fingering("C#6",arrayListOf(O, O, O, O, O, O, O), 2.0),
        // ph: Fingering("D6",    arrayListOf(O, X,X,X,X,X,X), 4),
        // ph: Fingering("D4*3",  arrayListOf(O, X,X,X,X,X,X), 3),
        // ph: Fingering("E4*3",  arrayListOf(O, O,X,X,X,X,X), 3),
        // ph: Fingering("F#4*3", arrayListOf(O, O,O,X,X,X,X), 3),
        // ph: Fingering("G4*3",  arrayListOf(O, O,O,O,X,X,X), 3),
        // ph: Fingering("A4*3",  arrayListOf(O, O,O,O,O,X,X), 3),
        // ph: Fingering("B4*3",  arrayListOf(O, O,O,O,O,O,X), 3),
        // ph: Fingering("C#5*3", arrayListOf(O, O,O,O,O,O,O), 3),
        // ph: Fingering("D4*4",  arrayListOf(O, X,X,X,X,X,X), 4),
        // ph: Fingering("E4*4",  arrayListOf(O, O,X,X,X,X,X), 4),
        // ph: Fingering("F#4*4", arrayListOf(O, O,O,X,X,X,X), 4),
        // ph: Fingering("G4*4",  arrayListOf(O, O,O,O,X,X,X), 4),
        // ph: Fingering("A4*4",  arrayListOf(O, O,O,O,O,X,X), 4),
        // ph: Fingering("B4*4",  arrayListOf(O, O,O,O,O,O,X), 4),
        // ph: Fingering("C#5*4", arrayListOf(O, O,O,O,O,O,O), 4),
        )
  }

  override var divisions by ListOfListOfIntDoublePairParam {
      listOf(
          arrayListOf(Pair(3, 0.5)),
          arrayListOf(Pair(0, 0.5), Pair(3, 0.0)),
          arrayListOf(Pair(0, 0.5), Pair(3, 0.25), Pair(5, 0.5)),
          arrayListOf(Pair(-1, 0.5), Pair(0, 0.5), Pair(3, 0.25), Pair(5, 0.5))
          // ph:arrayListOf( (-X,0.45), (-X,0.9), (2,0.0), (2,0.9), (5,0.0), (5,0.5) ),
        )
  }
}
