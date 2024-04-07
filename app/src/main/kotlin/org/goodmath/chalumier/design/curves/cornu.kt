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
package org.goodmath.chalumier.design.curves

import kotlin.math.*

/*
 * This file implements curves in curved profiles using shapes
 * from a cornu curve (also known as Euler's spiral, or a clothoid).
 * These curves are widely used as the shapes for transitions and diffusions,
 * because they have the property that curvature of a point on the curve is
 * proportional to that points distance along the spiral (or, if this makes it
 * easier to understand, a vehicle travelling along the spiral at constant speed
 * will experience a constant acceleration).
 *
 * This curve is computed using fresnel integrals - the clothoid is produced by
 * plotting the fresnel S and C integrals on the two axes of a grid.
 *
 * All of that is an elaborate way of saying that this is an implementation
 * of a nice transition curve for profiles.
 */

// ph: implementation adapted from cephes

/**
 * Helper function: Evaluate a polynomial at a point.
 *
 * @param x the value of x
 * @param coeff the polynomial represented as a set of coefficients.
 *   The coefficient of x^index is at `coeff[index]`.
 */
fun evalPolynomial(x: Double, coeff: List<Double>): Double {
    return coeff.reversed()
        .reduce { sum, next -> sum * x + next }
}

// Approximations of a bunch of fresnel power series, I think?
val sNumerator: List<Double> = listOf(
    -2991.8191940101983,
    708840.0452577386,
    -6.297414862058625E7,
    2.5489088057337637E9,
    -4.429795180596978E10,
    3.180162978765678E11
).reversed()

val sDenominator = listOf(
    1.0,
    281.3762688899943,
    45584.78108065326,
    5173438.887700964,
    4.193202458981112E8,
    2.2441179564534092E10,
    6.073663894900846E11
).reversed()

val cNumerator = listOf(
    -4.9884311457357354E-8,
    9.504280628298596E-6,
    -6.451914356839651E-4,
    0.018884331939670384,
    -0.20552590095501388,
    1.0
).reversed()

val cDenominator = listOf(
    3.99982968972496E-12,
    9.154392157746574E-10,
    1.2500186247959882E-7,
    1.2226278902417902E-5,
    8.680295429417843E-4,
    0.04121420907221998,
    1.0
).reversed()


val fNumerator: List<Double>
    get() = listOf(
        0.4215435550436775,
        0.1434079197807589,
        0.011522095507358577,
        3.45017939782574E-4,
        4.6361374928786735E-6,
        3.055689837902576E-8,
        1.0230451416490724E-10,
        1.7201074326816183E-13,
        1.3428327623306275E-16,
        3.763297112699879E-20
    ).reversed()

val fDenominator = listOf(
    1.0,
    0.7515863983533789,
    0.11688892585919138,
    0.0064405152650885865,
    1.5593440916415301E-4,
    1.8462756734893055E-6,
    1.1269922476399903E-8,
    3.6014002958937136E-11,
    5.887545336215784E-14,
    4.5200143407412973E-17,
    1.2544323709001127E-20
).reversed()

val gNumerator = listOf(
    0.5044420736433832,
    0.1971028335255234,
    0.018764858409257526,
    6.840793809153931E-4,
    1.1513882611188428E-5,
    9.828524436884223E-8,
    4.4534441586175015E-10,
    1.0826804113902088E-12,
    1.375554606332618E-15,
    8.363544356306774E-19,
    1.8695871016278324E-22
).reversed()

val gDenominator = listOf(
    1.0,
    1.4749575992512833,
    0.33774898912002,
    0.02536037414203388,
    8.146791071843061E-4,
    1.2754507566772912E-5,
    1.0431458965757199E-7,
    4.6068072814652043E-10,
    1.1027321506624028E-12,
    1.3879653125957886E-15,
    8.391588162831187E-19,
    1.8695871016278324E-22
).reversed()

fun Double.squared(): Double = this * this

/**
 * Compute the position on a clothoid curve corresponding to a value of the
 * parameter of the two fresnel integrals.
 *
 * MarkCC: I've done my best to
 * clear up the implementation to make it slightly let cryptic,
 * but differential equations are always going to be tricky.
 */
fun fresnel(signedX: Double): Pair<Double, Double> {
    var sResult: Double
    var cResult: Double

    val x = signedX.absoluteValue
    if (x.squared() < 2.5625) {
        val t = x.squared().squared()
        sResult = x * x.squared() * evalPolynomial(t, sNumerator) / evalPolynomial(t, sDenominator)
        cResult = x * evalPolynomial(t, cNumerator) / evalPolynomial(t, cDenominator)
    } else if (x > 36974.0) {
        sResult = 0.5
        cResult = 0.5
    } else {
        val t = 1.0 / (PI * x.squared())
        val u = 1.0 / (PI*x.squared()).squared()
        val f = 1.0 - u * evalPolynomial(u, fNumerator) / evalPolynomial(u, fDenominator)
        val g = t * evalPolynomial(u, gNumerator) / evalPolynomial(u, gDenominator)
        val c = cos(PI * .5 * x.squared())
        val s = sin(PI * .5 * x.squared())
        cResult = 0.5 + (f * s - g * c) / (PI * x)
        sResult = 0.5 - (f * c + g * s) / (PI * x)
    }
    if (signedX < 0) {
        cResult = -cResult
        sResult = -sResult
    }
    return Pair(sResult, cResult)
}

fun evalCornu(t: Double): Pair<Double, Double> {
    val sqrtPiOver2 = sqrt(PI * .5)
    val (s, c) = fresnel(t / sqrtPiOver2)
    return Pair(s * sqrtPiOver2, c * sqrtPiOver2)
}
