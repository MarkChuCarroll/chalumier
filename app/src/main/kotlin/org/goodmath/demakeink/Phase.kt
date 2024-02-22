package org.goodmath.demakeink

import org.goodmath.demakeink.curves.evalCornu
import org.kotlinmath.*
import kotlin.math.*

/*
 * This file is a load of phase utility functions from demakein.
 * I've tried to reproduce the functionality of the original code, and
 * all the original comments from the Python are here, prefixed with "ph:".
 */

/**
 * The absolute value of a complex number is its modulus - aka
 * its length considered as a vector.
 */
fun Complex.absoluteValue(): Double = mod



/*
 * ph: Unit magnitude complex number represent phase
 */

/**
 * mcc: As far as I can figure out, this is only used by "resonanceScore",
 * which is completely unused.
 */
fun pipeReply(reply: Complex, lengthOnWavelength: Double): Complex {
    val angle = FourPi * lengthOnWavelength
    return complex(cos(angle), sin(angle)) * reply
}

/**
 * ph: Relative phase reply for pipe 0 of two pipe junction
 * with pipe cross section areas a0 and a1
 * and pipe 1 relative phase reply r1.
 *
 */
fun junction2Reply(a0: Double, a1: Double, r1: Double): Pair<Double, Double> {
    val ca0 = a0
    val ca1 = a1
    val pJunc = 2.0  * ca0 / (ca0 - ca1 * ((r1 - 1.0) / (r1 + 1.0)))
    val mag1 = (pJunc / (r1 + 1.0)).absoluteValue
    return Pair(pJunc - 1.0, mag1)
}

/**
 * ph: Relative phase reply for pipe 0 of three pipe junction
 * with pipe cross section areas a0, a1 and a2
 * and relative phase replies r1 and r2.
 */
fun junction3Reply(a0: Double, a1: Double, a2: Double, r1: Double, r2: Double): Triple<Double, Double, Double> {
    val ca0 = a0
    val ca1 = a1
    val ca2 = a2

    val pjunc = 2.0 * ca0 / (ca0 - ca1 * ((r1 - 1.0) / (r1 + 1.0)) - ca2 * ((r2 - 1.0) / (r2 + 1.0)))
    val mag1 = (pjunc / (r1 + 1.0)).absoluteValue
    val mag2 = (pjunc / (r2 + 1.0)).absoluteValue
    return Triple(pjunc - 1.0, mag1, mag2)
}

// ph: Phase in[0,1] plus number of nodes
fun pipeReplyPhase(phaseEnd: Double, lengthOnWavelength: Double): Double {
    return phaseEnd + lengthOnWavelength * 2.0
}

fun tanner(phase: Double): Double {
    val x = phase*PI
    return tan(x)
}

fun floor(c: Complex): Complex = complex(floor(c.re), floor(c.im))

fun ceil(c: Complex): Complex = complex(ceil(c.re), ceil(c.im))

fun signed_sqrt(x: Double) =
    sqrt(abs(x))*(x.sign)

operator fun Complex.rem(n: Complex): Complex {
    // According to python2.7, if x and y are complex, then
    // x // y = floor((x/y).re)
    // x % y = x - (x//y)*y
    // THis is the behavior that ph seemed to be depending on.

    val floored = floor(this / n).re
    return this - (floored * n)
}

fun unTanner(x: Double): Double {
    return atan(x)/PI
}

fun junction2ReplyPhase(a0: Double, a1: Double, p1: Double): Double {
    val shift = floor(p1 + 0.5)
    return unTanner(a1 / a0 * tanner(p1 - shift)) + shift
}

fun junction3ReplyPhase(a0: Double, a1: Double, a2: Double, p1: Double, p2: Double): Double {
    val shift1 = floor(p1 + 0.5)
    val shift2 = floor(p2 + 0.5)
    return unTanner(a1 / a0 * tanner(p1 - shift1) + a2 / a0 * tanner(p2 - shift2)) + shift1 + shift2
}

fun endFlangeLengthCorrection(outerDiameter: Double, innerDiameter: Double): Double {
    val a: Double = innerDiameter / 2.0
    val w: Double = (outerDiameter - innerDiameter) / 2.0

    return a * (0.821 - 0.13 * (0.42 + w / a).pow(-0.54))
}

fun holeLengthCorrection(holeDiameter: Double, boreDiameter: Double, closed: Boolean): Double {
    // ph: No inner correction even, for closed holes.
    // Not sure why, but including an inner correction
    // for closed holes is wrong (I've tried this).
    //
    // Maybe better to treat as bore deviation?

    if (closed) {
        return 0.0
    }

    // As per p.63-64 of Nederveen
    val outerCorrection: Double = 0.7
    val innerCorrection: Double = 1.3 - 0.9 * holeDiameter / boreDiameter
    val a: Double = holeDiameter / 2.0

    return a * (innerCorrection + outerCorrection)
}

fun cornuYx(t: Double, mirror: Boolean): Pair<Double, Double> {
    // ph: Reparamaterize for constant absolute rate of turning
    val newT = sqrt(abs(t)) * (if (t > 0) 1 else -1)
    val (y, x) = evalCornu(newT)
    return if (mirror) {
        Pair(-y, x)
    } else {
        Pair(y, x)
    }
}
