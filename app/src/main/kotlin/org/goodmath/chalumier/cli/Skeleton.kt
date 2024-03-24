package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText

class Skeleton: ChalumierCommand("skeleton", "Generate a skeleton for an instrument description") {
    val instrument: String by option("--instrument", help="The type of instrument").required()
    val out: Path? by option("-o", help="the name of the generated skeleton file").path()
    override fun run() {
        val des = builder.getDesigner(instrument, Path("/tmp/null"))
        val result = des.generateDescriptionTemplate()
        val o = out
        if (o != null) {
            o.writeText(result)
        } else {
            echo(result)
        }
    }
}
