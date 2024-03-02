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
package org.goodmath.chalumier.diagram

import org.goodmath.chalumier.design.Profile
import java.io.FileWriter
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

// $1=width,
// $2=height,
// $3=scale,
// $4=transX
// $5=transX
// $6=negTransX
// $7=negTransY
val PREAMBLE = """
<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN"
"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">

<svg
    width="%(width)fmm"
    height="%(height)fmm"
    viewbox="0 0 %(width)fmm %(height)fmm"
    version="1.1"
    xmlns="http://www.w3.org/2000/svg">
<g transform="scale(%(scale)f,%(scale)f) translate(%(transX)f,%(transY)f)">
<rect x="%(neg_transX)f" y="%(neg_transY)f" width="%(width)f" height="%(height)f" style="fill:#ffffff"/>
"""


val POSTAMBLE = """\
</g></svg>
"""

class Diagram {
    private val commands = ArrayList<String>()
    var minX = 0.0
    var minY = 0.0
    var maxY = 0.0
    var maxX = 0.0

    fun save(filename: Path) {
        // ph: Assume 90dpi (inkscape default
        val scale = 90.0 / 25.4
        val pad = max(maxX - minX, maxY - minY) * 0.1
        val width = (maxX - minX + pad * 2)
        val height = (maxY - minY + pad * 2)
        val transX = -minX + pad
        val transY = -minY + pad
        val negTransX = -transX
        val negTransY = -transY

        val output = FileWriter(filename.toFile())
        output.write(PREAMBLE.format(width, height, scale, transX, transY, negTransX, negTransY))
        commands.forEach { cmd ->
            output.write(cmd + "\n")
        }
        output.write(POSTAMBLE)
        output.close()
    }

    fun require(x: Double, y: Double) {
        minX = min(minX, x)
        maxX = max(maxX, x)
        minY = min(minY, y)
        maxY = max(maxY, y)
    }

    fun circle(x: Double, y: Double, diameter: Double, color: String = "#000000") {
        val radius = diameter * 0.5
        require(x - radius, y - radius)
        require(x + radius, y + radius)
        commands.add(
            "<circle cx=\"${x}\" cy=\"${y}\" r=\"${radius}\" style=\"fill:none;stroke:${color};stroke-width:0.25mm\"/>"
        )
    }

    fun line(points: List<Pair<Double, Double>>, color: String = "#000000", width: Double = 0.25) {
        for ((x, y) in points) {
            require(x, y)
        }
        val allPoints = points.map { "${it.first},${it.second}" }.joinToString(" ")
        commands.add("<polyline points=\"${allPoints}\" style=\"fill:none;stroke:${color};stroke-width:${width}mm\" />")
    }

    fun polygon(points: List<Pair<Double, Double>>, color: String = "#000000", width: Double = 0.25) {
        points.forEach { (x, y) -> require(x, y) }
        val pointsStr = points.joinToString((" ")) { (x, y) -> "${x},${y}" }
        commands.add(
            "<polygon points=\"${pointsStr}\" style=\"fill:none;stroke:${color};stroke-width:${width}mm\" />"
        )
    }

    fun profile(profile: Profile, color: String = "#000000") {
        val points = ArrayList<Pair<Double, Double>>()
        profile.pos.mapIndexed { i, pos ->
            if (i != 0) {
                points.add(Pair(profile.low[i], pos))
            }
            if (i == 0 || (profile.low[i] != profile.high[i] && i < profile.pos.size - 1)) {
                points.add(Pair(profile.high[i], pos))
            }
        }
        line(points.map { (x, y) -> Pair(0.5 * x, -y) }, color)
        line(points.map { (x, y) -> Pair(-0.5 * x, -y) }, color)
    }

    fun text(x: Double, y: Double, text: String, color: String = "#666666"): Double {
        val fontHeight = 8.0
        val yy = y + fontHeight * 0.5
        require(x, yy - fontHeight)
        require(x + text.length * fontHeight * 0.8, yy)
        commands.add(
            "<text x=\"${x}\" y=\"${yy}\" fill=\"${color}s\" font-size=\"10\" font-family=\"monospace\" xml:space=\"preserve\">${text}/text>"
        )
        return y - fontHeight
    }
}
