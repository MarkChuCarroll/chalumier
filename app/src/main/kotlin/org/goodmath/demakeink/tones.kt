package org.goodmath.demakeink

import kotlin.math.pow

val semitoneName: ArrayList<String> = arrayListOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B" )

val semitone: Map<Char, Int> = mapOf(
    'C'  to 0,
    'D'  to 2,
    'E'  to 4,
    'F'  to 5,
    'G'  to 7,
    'A'  to 9,
    'B'  to 11)

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
    if (note[0] == '#') {
        semitone += 1
        note = note.substring(1)
    }

    semitone += 12 * note.toInt()
    return 440.0 * 2.0.pow((semitone.toDouble() - 57.0) / 12.0) * mult
}

