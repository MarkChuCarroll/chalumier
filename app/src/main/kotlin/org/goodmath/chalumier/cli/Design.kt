package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.div

class Design: CliktCommand(name = "design", help="Compute an instrument design from a specification") {
    private val templates = mapOf(
        "folkFlute" to { n: String, dir: Path ->  org.goodmath.chalumier.design.folkFluteDesigner(n, dir) },
        "pFlute" to { n: String, dir: Path -> org.goodmath.chalumier.design.pFluteDesigner(n, dir)},
        "folkShawm" to { n: String, dir: Path -> org.goodmath.chalumier.design.FolkShawmDesigner(n, dir) },
        "folkWhistle" to { n: String, dir: Path -> org.goodmath.chalumier.design.folkWhistleDesigner(n, dir) },
        "recorder" to { n: String, dir: Path -> org.goodmath.chalumier.design.RecorderDesigner(n, dir) }
    )
    private val builder = DesignerBuilder(templates)

    private val specfile: Path by argument("instrument-spec.json").path(mustExist = true)
    private val outputDir: Path by option("--output-dir").path().required()
    private val reportingInterval: Int by option("--report-interval").int().default(5000)


    override fun run() {
        val des = builder.getDesigner(specfile, outputDir)
        val i = des.run(::echo, reportingInterval)
        echo("Execution complete! Designed ${i.name}; diagram in ${outputDir / "diagram.svg"}")
    }
}
