package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import org.goodmath.chalumier.design.*
import java.nio.file.Path


class Design: CliktCommand() {
//    val designers: Map<String, InstrumentDesigner<*>> = mapOf(
//        "folkflute" to folkFluteDesigner(dir),
//        "pFlute" to pFluteDesigner(dir),
//        "folkWhistle" to folkWhistleDesigner(dir)
//    )
    val instrument by option().choice("folkflute", "pflute", "folkwhistle").required()
    val outputDir: Path by option("--output-dir").path().required()

    fun makeDesigner(name: String): InstrumentDesigner<*> {
        return when (instrument) {
            "folkflute" -> folkFluteDesigner(outputDir)
            "pflute" -> pFluteDesigner(outputDir)
            "folkwhistle" -> folkWhistleDesigner(outputDir)
            else -> throw CliktError()
        }
    }
    override fun run() {
        val des = makeDesigner(instrument)
        val i = des.run(outputDir)
        echo("Designed: " + i)
    }
}

fun main(args: Array<String>) = Design().main(args)

