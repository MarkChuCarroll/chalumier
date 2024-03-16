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

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

/*
 * Utility functions for converting between string names for tones,
 * and the frequency of those tones.
 */

val semitoneName: ArrayList<String> = arrayListOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B")

val semitone: Map<Char, Int> = mapOf(
    'C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11
)

fun wavelength(noteName: String, transpose: Int = 0): Double {
    val w = SPEED_OF_SOUND / frequency(noteName)
    return w / (2.0.pow(transpose.toDouble() / 12.0))
}

fun frequency(noteName: String): Double {
    var note = noteName
    var mult = 1.0
    if (note.endsWith("hz")) {
        return note.substring(0, note.length - 2).toDouble()
    }
    if ('*' in note) {
        val idx = note.indexOf('*')
        mult = note.substring(idx + 1).toDouble()
        note = note.substring(0, idx)
    }
    var semitone = semitone[note[0].uppercaseChar()]!!
    note = note.substring(1)
    if (note[0] == 'b') {
        semitone -= 1
        note = note.substring(1)
    }
    if (note[0] == '#' || note[0] == 's') {
        semitone += 1
        note = note.substring(1)
    }

    semitone += 12 * note.toInt()
    return 440.0 * 2.0.pow((semitone.toDouble() - 57.0) / 12.0) * mult
}


fun describe(wavelength: Double): String {
    val f = SPEED_OF_SOUND / wavelength

    val note = (round(log2(f / 440.0) * 12.0 + 57)).toInt()
    val octave = note / 12
    val semitone = note % 12
    return "${semitoneName[semitone]}${octave}"
}
