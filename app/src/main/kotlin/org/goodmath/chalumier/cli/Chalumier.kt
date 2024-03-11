package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.goodmath.chalumier.design.*
import java.nio.file.Path


class Design: CliktCommand() {
    private val instrument by option().choice("folkflute", "pflute", "folkwhistle").required()
    private val outputDir: Path by option("--output-dir").path().required()
    private val reportingInterval: Int by option("--report-interval").int().default(5000)

    fun makeDesigner(name: String): InstrumentDesigner {
        return when (instrument) {
            "folkflute" -> folkFluteDesigner(outputDir)
            "pflute" -> pFluteDesigner(outputDir)
            "folkwhistle" -> folkWhistleDesigner(outputDir)
            else -> throw CliktError()
        }
    }
    override fun run() {
        val des = makeDesigner(instrument)
        val i = des.run(outputDir, reportingInterval)
        echo("Designed: " + i)
    }
}

fun main(args: Array<String>) = Design().main(args)

