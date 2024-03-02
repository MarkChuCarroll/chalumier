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
package org.goodmath.chalumier.geom

interface MathBridge<Left, Right, Product> {

    val leftMath: Math<Left>
    val rightMath: Math<Right>
    val prodMath: Math<Product>
    fun plus(one: Left, two: Right): Product

    fun rPlus(one: Right, two: Left): Product {
        return plus(two, one)
    }

    fun minus(one: Left, two: Right): Product = this.plus(one, this.rNeg(two))

    fun rMinus(one: Right, two: Left): Product = this.rPlus(one, this.neg(two))

    fun neg(t: Left): Left

    fun rNeg(u: Right): Right

    fun times(one: Left, two: Right): Product

    fun rTimes(one: Right, two: Left): Product = this.times(two, one)


    fun div(one: Left, two: Right): Product = this.times(one, this.rRecip(two))

    fun rDiv(one: Right, two: Left): Product = this.rTimes(one, this.recip(two))

    fun recip(t: Left): Left

    fun rRecip(u: Right): Right

    // Invert is just a syntactic convenience for when you have a MathBridge<X, Y, Z>,
    // but you need a mathBridge<Y, X, Z>.
    fun invert(): MathBridge<Right, Left, Product> {
        val me = this
        return object : MathBridge<Right, Left, Product> {
            override val leftMath: Math<Right> = me.rightMath
            override val rightMath: Math<Left> = me.leftMath

            override val prodMath: Math<Product> = me.prodMath

            override fun plus(one: Right, two: Left): Product = me.rPlus(one, two)

            override fun neg(t: Right): Right = me.rNeg(t)
            override fun times(one: Right, two: Left): Product = me.rTimes(one, two)

            override fun rRecip(u: Left): Left = me.recip(u)

            override fun recip(t: Right): Right = me.rRecip(t)

            override fun rNeg(u: Left): Left = me.neg(u)

            override fun invert(): MathBridge<Left, Right, Product> {
                return me
            }
        }
    }
}

fun <T> doubleBridge(math: Math<T>): MathBridge<T, Double, T> = object : MathBridge<T, Double, T> {
    override val leftMath: Math<T> = math

    override val rightMath: Math<Double> = DoubleMath

    override val prodMath: Math<T> = math

    override fun rRecip(u: Double): Double {
        return 1.0 / u
    }

    override fun recip(t: T): T {
        return math.reciprocal(t)
    }

    override fun times(one: T, two: Double): T {
        return math.times(one, two)
    }

    override fun rNeg(u: Double): Double {
        return -u
    }

    override fun neg(t: T): T {
        return math.neg(t)
    }

    override fun plus(one: T, two: Double): T {
        throw Exception("Fuck this nonsense")
    }


}
