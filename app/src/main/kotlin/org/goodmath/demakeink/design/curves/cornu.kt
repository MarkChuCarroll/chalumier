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
package org.goodmath.demakeink.design.curves

import kotlin.math.*

// ph: implementation adapted from cephes

/**
 * Evaluate a polynomial at a point.
 *
 * @param x the value of x
 * @param coeff the polynomial represented as a set of coefficients.
 *   The coefficient of x^n is at coeff[n].
 */
fun evalPolynomial(x: Double, coeff: List<Double>): Double {
    return coeff.reversed()
        .reduce { sum, next -> sum * x + next }
}

// A whole bunch of ?standard? curves that we're going to use.
// Sure would have been nice for ph to tell us WTF they are, eh?

val sn = listOf(
    -2.99181919401019853726E3,
    7.08840045257738576863E5,
    -6.29741486205862506537E7,
    2.54890880573376359104E9,
    -4.42979518059697779103E10,
    3.18016297876567817986E11
).reversed()

val sd = listOf(
    1.00000000000000000000E0,
    2.81376268889994315696E2,
    4.55847810806532581675E4,
    5.17343888770096400730E6,
    4.19320245898111231129E8,
    2.24411795645340920940E10,
    6.07366389490084639049E11
).reversed()

val cn = listOf(
    -4.98843114573573548651E-8,
    9.50428062829859605134E-6,
    -6.45191435683965050962E-4,
    1.88843319396703850064E-2,
    -2.05525900955013891793E-1,
    9.99999999999999998822E-1
).reversed()

val cd = listOf(
    3.99982968972495980367E-12,
    9.15439215774657478799E-10,
    1.25001862479598821474E-7,
    1.22262789024179030997E-5,
    8.68029542941784300606E-4,
    4.12142090722199792936E-2,
    1.00000000000000000118E0
).reversed()


val fn = listOf(
    4.21543555043677546506E-1,
    1.43407919780758885261E-1,
    1.15220955073585758835E-2,
    3.45017939782574027900E-4,
    4.63613749287867322088E-6,
    3.05568983790257605827E-8,
    1.02304514164907233465E-10,
    1.72010743268161828879E-13,
    1.34283276233062758925E-16,
    3.76329711269987889006E-20
).reversed()

val fd = listOf(
    1.00000000000000000000E0,
    7.51586398353378947175E-1,
    1.16888925859191382142E-1,
    6.44051526508858611005E-3,
    1.55934409164153020873E-4,
    1.84627567348930545870E-6,
    1.12699224763999035261E-8,
    3.60140029589371370404E-11,
    5.88754533621578410010E-14,
    4.52001434074129701496E-17,
    1.25443237090011264384E-20
).reversed()

val gn = listOf(
    5.04442073643383265887E-1,
    1.97102833525523411709E-1,
    1.87648584092575249293E-2,
    6.84079380915393090172E-4,
    1.15138826111884280931E-5,
    9.82852443688422223854E-8,
    4.45344415861750144738E-10,
    1.08268041139020870318E-12,
    1.37555460633261799868E-15,
    8.36354435630677421531E-19,
    1.86958710162783235106E-22
).reversed()

val gd = listOf(
    1.00000000000000000000E0,
    1.47495759925128324529E0,
    3.37748989120019970451E-1,

    2.53603741420338795122E-2,
    8.14679107184306179049E-4,
    1.27545075667729118702E-5,
    1.04314589657571990585E-7,
    4.60680728146520428211E-10,
    1.10273215066240270757E-12,
    1.38796531259578871258E-15,
    8.39158816283118707363E-19,
    1.86958710162783236342E-22
).reversed()


fun fresnel(xxa: Double): Pair<Double, Double> {
    var ss = 0.0
    var cc = 0.0

    val x = xxa.absoluteValue
    val x2 = x * x
    if (x2 < 2.5625) {
        val t = x2 * x2
        ss = x * x2 * evalPolynomial(t, sn) / evalPolynomial(t, sd)
        cc = x * evalPolynomial(t, cn) / evalPolynomial(t, cd)
    } else if (x > 36974.0) {
        ss = 0.5
        cc = 0.5
    } else {
        var t = PI * x2
        val u = 1.0 / (t * t)
        t = 1.0 / t
        val f = 1.0 - u * evalPolynomial(u, fn) / evalPolynomial(u, fd)
        val g = t * evalPolynomial(u, gn) / evalPolynomial(u, gd)
        t = PI * .5 * x2
        val c = cos(t)
        val s = sin(t)
        t = PI * x
        cc = 0.5 + (f * s - g * c) / t
        ss = 0.5 - (f * c + g * s) / t
    }
    if (xxa < 0) {
        cc = -cc
        ss = -ss
    }
    return Pair<Double, Double>(ss, cc)
}

fun evalCornu(t: Double): Pair<Double, Double> {
    val spio2 = sqrt(PI * .5)
    val (s, c) = fresnel(t / spio2)
    return Pair<Double, Double>(s * spio2, c * spio2)
}
