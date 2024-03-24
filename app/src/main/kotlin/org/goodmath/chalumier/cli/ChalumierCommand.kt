package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.core.CliktCommand
import java.nio.file.Path

abstract class ChalumierCommand(name: String, help: String): CliktCommand(name, help) {
    val templates = mapOf(
        "folkFlute" to { n: String, dir: Path ->  org.goodmath.chalumier.design.folkFluteDesigner(n, dir) },
        "pFlute" to { n: String, dir: Path -> org.goodmath.chalumier.design.pFluteDesigner(n, dir)},
        "folkShawm" to { n: String, dir: Path -> org.goodmath.chalumier.design.FolkShawmDesigner(n, dir) },
        "folkWhistle" to { n: String, dir: Path -> org.goodmath.chalumier.design.folkWhistleDesigner(n, dir) },
        "recorder" to { n: String, dir: Path -> org.goodmath.chalumier.design.RecorderDesigner(n, dir) }
    )
    val builder = InstrumentDesignerFactory(templates)

}