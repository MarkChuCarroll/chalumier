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

import org.goodmath.demakeink.util.fromEnd
import org.goodmath.demakeink.util.repeat
import kotlin.math.max
import kotlin.math.min


/** ph:
 * length>=1 immutable list of Linear
 *
 *    f(t) = sum self[i](t) * s**t where s = t*(1-t)
 */
open class SBasis<T>(b: List<Linear<T>>, val math: Math<T>) {
    val basis = b.toMutableList()

    operator fun compareTo(other: SBasis<T>): Int {
        val minSize = min(len(), other.len())
        for (i in 0 until minSize) {
            if (this[i] < other[i]) {
                return -1
            } else if (this[i] > other[i]) {
                return 1
            }
            // otherwise, this[i]==other[i], so go on to next index
        }
        return if (minSize < len()) {
            1
        } else if (minSize == len()) {
            0
        } else {
            -1
        }
    }

    val ONE = SBasis(listOf(Linear(math.one, math.one, math)), math)
    val ZERO = SBasis(listOf(Linear(math.zero, math.zero, math)), math)
    val IDENTITY = SBasis(listOf(Linear(math.zero, math.one, math)), math)

    operator fun get(index: Int): Linear<T> = basis[index]
    operator fun set(index: Int, value: Linear<T>) {
        basis[index] = value
    }

    fun len(): Int = basis.size

    fun invoke(d: Double): T {
        val s = d * (1-d)
        var result = basis[0].invoke(d)
        var p = s
        (1..basis.size).forEach { i ->
            result = math.plus(result, math.times(basis[i].invoke(d), p))
            p = p * s
        }
        return result
    }

    override fun toString(): String {
        return "SBasis(${basis.map { it.toString() }})"
    }

    fun<U, V> scaled(factor: U, tuvBridge: MathBridge<T, U, V>): SBasis<V> {
        return SBasis<V>(basis.map { it.times(factor, tuvBridge) }.toMutableList(), tuvBridge.prodMath)
    }


    fun shifted(i: Int): SBasis<T> {
        return SBasis<T>((listOf(basis[0].times(math.zero)).repeat(i) + basis), math)
    }

    fun truncated(n: Int): SBasis<T> {
        return SBasis<T>(basis.slice(0 until n), math)
    }

    fun<U> compat(other: SBasis<U>): Triple<Int, SBasis<T>, SBasis<U>> {
        val size = max(len(), other.len())
        val normalizedThis = if (len() < size) {
            //  self = type(other)(tuple(self) + (self[0]*0,)*(size-len(self)))
            val x = listOf(this[0].times(math.zero)).repeat(size - len())
            SBasis<T>(basis + listOf(this[0].times(math.zero)).repeat(size - len()), math)
        } else {
            this
        }
        val normalizedOther = if (other.len() < size) {
            SBasis<U>(other.basis + listOf(other[0].times(other.math.zero)).repeat(size - other.len()), other.math)
        } else {
            other
        }
        return Triple(size, normalizedThis, normalizedOther)
    }

    fun<U, V> plus(other: SBasis<U>, tuvBridge: MathBridge<T, U, V>): SBasis<V> {
        val (size, nThis, nOther) = compat(other)
        return SBasis<V>((0 until size).map { i: Int -> nThis[i].plus(nOther[i], tuvBridge) }, tuvBridge.prodMath)
    }

    fun<U, V> minus(other: SBasis<U>, tuvBridge: MathBridge<T, U, V>): SBasis<V> {
        val (size, self, o) = compat(other)
        return SBasis<V>((0 until size).map { i -> self.basis[i].minus(o.basis[i], tuvBridge) }, tuvBridge.prodMath)
    }



    fun <U, V> multiplied(
        other: SBasis<U>,
        operator: (T, U) -> V,
        umath: Math<U>,
        vmath: Math<V>
    ): SBasis<V> {
        val zero = operator(
            math.times(basis[0].a0, math.zero),
            umath.times(other.basis[0].a0, umath.zero)
        )
        val c = ArrayList(listOf(Linear(zero, zero, vmath)).repeat(basis.size + other.basis.size))
        for (j in 0 until other.basis.size) {
            for (i in j until j + basis.size) {
                val tri = operator(basis[i - j].tri(), other.basis[j].tri())
                c[i + 1] =
                    c[i + 1].plus(
                            Linear(vmath.times(tri, vmath.minusOne),
                                    vmath.times(tri, vmath.minusOne),
                                    vmath),
                            vmath.selfBridge)


                c[i] = c[i].plus(
                    Linear(
                            operator(basis[i - j].a0, other.basis[j].a0),
                            operator(basis[i - j].a1, other.basis[j].a1),
                            vmath),
                        vmath.selfBridge)
            }
        }

        // ph:
        // #while len(c) > 1 and c[-1] == zero:
        // #    del c[-1]
        return SBasis(c, vmath)
    }


    fun<U, V> times(other: SBasis<U>, bridge: MathBridge<T, U, V>): SBasis<V> {
        return multiplied(other, { a: T, b: U -> bridge.times(a, b) }, other.math, bridge.prodMath)
    }


    fun dot(other: SBasis<T>): SBasis<Double> {
        val op = { a: T, b: T -> math.dot(a, b) }

        return multiplied(other, op, math, DoubleMath)
    }

    fun derivative(): SBasis<T> {
        val c = ArrayList<Linear<T>>()
        for (k in 0 until basis.size - 1) {
            val d = math.times(math.minus(basis[k].a1, basis[k].a0), 1.0 + (2 * k).toDouble())
            c.add(
                Linear(
                    math.plus(d, math.times(basis[k + 1].a0, (k + 1).toDouble())),
                    math.minus(d, math.times(basis[k + 1].a1, (k + 1).toDouble())),
                    math
                )
            )
        }
        val k = basis.size - 1
        val d = math.times(math.minus(basis[k].a1, basis[k].a0), (2 * k + 1).toDouble())
        c.add(Linear(d, d, math))
        return SBasis(c, math)
    }

    fun integral(): SBasis<T> {
        val a = arrayListOf(basis[0].times(math.zero))
        for (k in 1..basis.size + 1) {
            val abat = math.times(basis[k - 1].tri(), math.div(math.minusOne, (2 * k).toDouble()))
            a.add(Linear(abat, abat, math))
        }
        var aTri = math.times(basis[0].a0, math.zero)
        for (k in basis.size - 1 downTo 0) {
            aTri = math.times(
                    basis[k].hat(),
                    math.times(
                            math.times(aTri, (k + 1).toDouble() * 0.5),
                            math.div(math.one, (2 * k + 1).toDouble())
                    )
            )
            a[k] = a[k].plus(
                    Linear(
                            math.times(aTri, -0.5),
                            math.times(aTri, 0.5),
                            math),
                    math.selfBridge)
        }
        return SBasis(a, math)
    }

    fun<U, V> compose(other: SBasis<U>,
                      tuvBridge: MathBridge<T, U, V>,
                      vuvBridge: MathBridge<V, U, V>,
    ): SBasis<V> {
        val s = other.ONE.minus(other, other.math.selfBridge ).times(other, other.math.selfBridge)

        var result = SBasis(listOf(this[0].times(other.math.zero, tuvBridge)), tuvBridge.prodMath)
        for (i in len() - 1 downTo 0) {
            val oneMinusOther = other.ONE.minus(other, other.math.selfBridge)
            val oneMinusOtherScaled = oneMinusOther.scaled(this[i].a0, tuvBridge.invert())
            result = result
                    .times(s, vuvBridge)
                    .plus(oneMinusOtherScaled,tuvBridge.prodMath.selfBridge)
                    .plus(other.scaled(this[i].a1, tuvBridge.invert()), tuvBridge.prodMath.selfBridge)

            // ph: #S_basis([Linear(self[i].a0,self[i].a0)]) + other.scaled(self[i].a1-self[i].a0)
        }
        return result
    }


    fun divided(other: SBasis<T>, k: Int): SBasis<T> {
        val lin = Linear(math.div(this[0].a0, other[0].a0), math.div(this[0].a1, other[0].a1), math)
        val ttt = math.selfBridge
        return leastSquares(
            SBasis(listOf(lin), math),
            { x: SBasis<T> -> other.times(x,ttt ).minus(this, ttt) },
            { x: SBasis<T> -> other },
            { x: SBasis<T> -> SBasis.ZERO(math) },
            k
        )
        /* ph:
            #remainder = self
            #result = [ ]
            #for i in xrange(k):
            #    if len(remainder) <= i:
            #        break
            #    ci = Linear(remainder[i].a0/other[0].a0, remainder[i].a1/other[0].a1)
            #    result.append(ci)
            #    remainder = remainder - (S_basis([ci])*other).shifted(i)
            #return S_basis(result)
         */
    }

    fun reciprocal(k: Int): SBasis<T> {
        return ONE.divided(this, k)
    }

    fun sqrt(k: Int): SBasis<T> {
        // ph: Calculate square root by newton's method
        val tdt = doubleBridge(this.math)
        return leastSquares(
            this,
            { x -> x.times(x, x.math.selfBridge).minus(this, x.math.selfBridge) },
            { x -> x.scaled(2.0, tdt) },
            { x -> ONE.scaled(2.0, tdt) },
            k
        )


        /* ph:
            #result = self
            #for i in xrange(iters):
            #    result = (result+self.divided(result, k)).scaled(0.5)
            #return result
        */
    }

    fun solve(target: SBasis<T>, k: Int, iters: Int = 20): SBasis<T> {
        // ph: Solve self.compose(x) = target for x using Newton's method
        val ttt = math.selfBridge
        var result = target
        val deriv = this.derivative()
        for (i in 0 until iters) {
            result = result.minus(compose(result, ttt, ttt).minus(target, ttt), ttt).divided(deriv.compose(result, ttt, ttt), k)
            result = result.truncated(k)
        }
        return result.truncated(k)
    }

    fun inverse(k: Int, iters: Int = 5): SBasis<T> {
        return solve(IDENTITY(math), k, iters)
    }

    companion object {
        fun <T> ONE(math: Math<T>): SBasis<T> {
            return SBasis(listOf(Linear(math.one, math.one, math)), math)
        }

        fun <T> ZERO(math: Math<T>): SBasis<T> {
            return SBasis(listOf(Linear(math.zero, math.zero, math)), math)
        }

        fun <T> IDENTITY(math: Math<T>): SBasis<T> {
            return SBasis(listOf(Linear(math.zero, math.one, math)), math)
        }

        fun <T> newtonoid(
            initial: SBasis<T>,
            fp: (SBasis<T>) -> SBasis<T>,
            fpp: (SBasis<T>) -> SBasis<T>,
            k: Int
        ): SBasis<T> {
            val tdt = doubleBridge(initial.math)
            // ph: Choose x to minimize the integral of f(x) over [0,1]
            val math = initial.math
            val ttt = math.selfBridge

            fun scoreP(a: SBasis<T>, b: SBasis<T>): T = b.times(fp(a), ttt).integral()[0].tri()
            fun scorePP(a: SBasis<T>, b: SBasis<T>): T {
                val bSquared = b.times(b, ttt)
                val fppa: SBasis<T> = fpp(a)
                val bSquaredFppa: SBasis<T> = bSquared.times(fppa, ttt)
                return bSquaredFppa.integral()[0].tri()
            }
            var result = initial
            // ph: current = score(result)

            // ph: Legendre polynomials
            val X = SBasis.IDENTITY(math).scaled(2.0, tdt).minus(SBasis.ONE(math), ttt)
            val basis = mutableListOf(SBasis.ONE(math), X)
            while (basis.size < k * 2) {
                val n = basis.size
                basis.add(
                    basis.fromEnd(1).times(X, ttt).scaled((2.0 * n.toDouble() + 1.0) / (n.toDouble() + 1.0), tdt)
                        .minus(basis.fromEnd(2).scaled(n.toDouble() / (n.toDouble() + 1.0), tdt), ttt)
                )
            }
            // ph: plot(*basis)
            // ph: foo
            var step: T = math.one
            for (i in 0..k * 8) {
                for (item in basis) {
                    // ph: c = score(result)
                    // ph: b = scorep(result)
                    // ph: a = 0.5*scorepp(result)
                    // ph: step = -b/2a
                    step = math.neg(math.div(scoreP(result,item), scorePP(result,item)))

                    // ph: print step
                    val newResult = result.plus(item.scaled(step, ttt), ttt)
                    // ph: new = result * item.scaled(min_point)
                    // ph: new_score = score(new)
                    // ph: if new_score < current:
                    result = newResult
                    // ph: current = new_score
                }
                // ph: step *= 0.85
                // ph: print current, step
                // ph: if not step: break
            }
            return result
        }


        fun <T> leastSquares(
            guess: SBasis<T>,
            f: (SBasis<T>) -> SBasis<T>,
            fp: (SBasis<T>) -> SBasis<T>,
            fpp: (SBasis<T>) -> SBasis<T>,
            k: Int,
        ): SBasis<T> {
            val math = guess.math
            val ttt = math.selfBridge

            // ph: Choose x to minimize the integral of f(x)^2 over [0,1]
            fun f2p(x: SBasis<T>): SBasis<T> {
                val y = f(x)
                return y.times(y, ttt)
            }

            fun f2pp(x: SBasis<T>): SBasis<T> {
                val yp = fp(x)
                return fpp(x).times(f(x), ttt).plus(yp, ttt).times(yp, ttt).scaled(2.0, doubleBridge(math))
            }
            return newtonoid(guess, ::f2p, ::f2pp,
                    k)
        }

    }
}
