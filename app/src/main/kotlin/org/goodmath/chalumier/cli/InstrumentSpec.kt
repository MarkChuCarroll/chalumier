package org.goodmath.chalumier.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.peanuuutz.tomlkt.Toml
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText

@Serializable
enum class InstrumentType {
    Flute, Shawm, Whistle, Reedpipe
}

@Serializable
enum class Hole {
    Open, Closed
}

@Serializable
data class FingeringSpec(
    val noteName: String,
    val fingering: List<Hole>,
    val nTh: Int? = null
)

@Serializable
data class InstrumentSpec(
    val name: String,
    val instrumentType: String,
    val numberOfHoles: Int,
    val fingerings: List<FingeringSpec>,
    val innerDiameters: List<Pair<Double, Double>>? = null,
    val outerDiameters: List<Pair<Double, Double>>? = null,
    val length: Double? = null,
    val maxLength: Double? = null,
    val closedTop: Boolean = false,
    val initialLength: Double? = null,
    val transpose: Int = 0,
    val tweakEmissions: Double? = 0.0,
    val minHoleDiameters: List<Double>? = null,
    val maxHoleDiameters: List<Double>? = null,
    val outerAdd: Boolean = false,
    val topClearanceFraction: Double = 0.0,
    val bottomClearanceFraction: Double = 0.0,
    val scale: Double = 1.0,
    val minHoleSpacing: List<Double?>? = null,
    val maxHoleSpacing: List<Double?>? = null,
    val balance: List<Double?>? = null,
    val holeAngles: List<Double>? = null,
    val holeHorizAngles: List<Double>? = null,
    val divisions: List<List<Pair<Int, Double>>>? = null
) {


    companion object {
        fun readFromFile(specFile: Path): InstrumentSpec {
            if (specFile.notExists()) {
                throw IOException("File ${specFile} does not exist")
            }
            val spec = specFile.readText()
            return Toml.decodeFromString<InstrumentSpec>(spec)
        }

    }
}

