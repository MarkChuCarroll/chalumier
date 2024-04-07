/*
 * Copyright 2024 Mark C. Chu-Carroll and Paul Francis Harrison
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
package org.goodmath.chalumier.cli

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText

class Skeleton : ChalumierCommand("skeleton", "Generate a skeleton for an instrument description") {
    val instrument: String by option("--instrument", help = "The type of instrument").required()
    val out: Path? by option("-o", help = "the name of the generated skeleton file").path()

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
