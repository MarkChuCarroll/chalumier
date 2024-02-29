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
package org.goodmath.demakeink.geom

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * An object capturing the basis math ops. Annoying, but it works.
 */
interface Math<T> {
    val zero: T
    val one: T

    val minusOne: T
    fun neg(a: T): T
    fun plus(a: T, b: T): T
    fun minus(a: T, b: T): T
    fun times(a: T, b: T): T

    fun times(a: T, b: Double): T
    fun div(a: T, b: T): T

    fun div(a: T, b: Double): T

    fun dot(a: T, b: T): Double

    fun reciprocal(a: T): T
    fun lt(one: T, two: T): Boolean
    fun eq(one: T, two: T): Boolean

    fun le(one: T, two: T): Boolean = this.lt(one, two) || this.eq(one, two)
    fun ge(one: T, two: T): Boolean = !lt(one, two)
    fun gt(one: T, two: T): Boolean = !le(one, two)

    val selfBridge: MathBridge<T, T, T>
        get() {
            val me = this
            return object : MathBridge<T, T, T> {
                override val leftMath: Math<T> = me
                override val rightMath: Math<T> = me
                override val prodMath: Math<T> = me
                override fun recip(u: T): T = prodMath.div(me.one, u)
                override fun times(one: T, two: T): T = me.times(one, two)
                override fun neg(u: T): T = me.minus(me.zero, u)
                override fun plus(one: T, two: T): T = me.plus(one, two)
                override fun invert(): MathBridge<T, T, T> {
                    return this
                }
            }
        }
}

object DoubleMath: Math<Double> {
    override val zero: Double = 0.0

    override val one: Double = 1.0
    override val minusOne: Double = this.neg(one)
    override fun eq(one: Double, two: Double): Boolean {
        return one == two
    }

    override fun lt(one: Double, two: Double): Boolean {
        return one < two
    }

    override fun reciprocal(a: Double): Double = 1.0 / a

    override fun neg(a: Double): Double = -a


    override fun dot(a: Double, b: Double): Double = a * b
    override fun div(a: Double, b: Double): Double = a / b
    override fun times(a: Double, b: Double): Double = a * b
    override fun minus(a: Double, b: Double): Double = a - b
    override fun plus(a: Double, b: Double): Double = a + b
}

object XYZMath: Math<XYZ> {
    override val zero: XYZ = XYZ(0.0, 0.0, 0.0)

    override val one: XYZ = XYZ(1.0, 1.0, 1.0).unit()

    override val minusOne: XYZ = -one
    override fun eq(one: XYZ, two: XYZ): Boolean {
        return one.x == two.x && one.y == two.y && one.z == two.z
    }

    override fun lt(one: XYZ, two: XYZ): Boolean {
        return when {
            one.x < two.x -> true
            one.x > two.x -> false
            one.x == two.x && one.y < two.y   -> true
            one.x == two.x && one.y > two.y -> false
            one.x == two.x && one.y == two.y && one.z < two.z -> true
            one.x == two.x && one.y == two.y && one.z == two.z -> false
            else -> false
        }
    }

    override fun reciprocal(a: XYZ): XYZ = this.div(a.unit(), a.mag())

    override fun dot(a: XYZ, b: XYZ): Double = dot(a, b)

    override fun div(a: XYZ, b: Double): XYZ = times(a, 1.0/b)

    override fun div(a: XYZ, b: XYZ): XYZ {
        return times(a, this.reciprocal(b))
    }

    override fun times(a: XYZ, b: Double): XYZ = XYZ(a.x*b, a.y*b,a.z*b)

    override fun times(a: XYZ, b: XYZ): XYZ = a.cross(b)


    override fun minus(a: XYZ, b: XYZ): XYZ = a - b

    override fun plus(a: XYZ, b: XYZ): XYZ = a + b

    override fun neg(a: XYZ): XYZ = -a


}


