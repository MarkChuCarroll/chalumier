package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.json.Json
import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.model.FluteModel
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Model: CliktCommand(name="model", help="Generate a 3d model of a design") {
    val modelFile: Path by option("--instrument", help="the path of a JSON file containing a generated instrument").path(mustExist = true).required()
    val facets: Int by option("--facets", help="Number of outer facets on the instrument").int().default(0)
    val output: Path by option("--output", help="The path to write the OpenSCAD file to").path(mustExist = false).required()

    override fun run() {
        val inst = Json.decodeFromString<Instrument>(modelFile.readText())
        output.writeText(FluteModel(inst, facets).render())
    }



}
