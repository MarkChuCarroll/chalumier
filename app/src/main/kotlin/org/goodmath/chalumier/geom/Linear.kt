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


/**
 * ph: f(t) = a0*(1-t)+a1*t
 */
open class Linear<T>(val a0: T, val a1: T, val math: Math<T>) {

    operator fun compareTo(other: Linear<T>): Int {
        return when {
            math.lt(a0, other.a0) -> -1
            math.eq(a0, other.a0) && math.lt(a1, other.a1) -> -1
            math.eq(a0, other.a0) && math.eq(a1, other.a1) -> 0
            else -> 1
        }
    }


    operator fun invoke(t: Double): T {
        return math.plus(math.times(a0, (1 - t)), math.times(a1, t))
    }

    fun <U, V> plus(other: Linear<U>, tuvBridge: MathBridge<T, U, V>): Linear<V> {
        return Linear(tuvBridge.plus(a0, other.a0), tuvBridge.plus(a1, other.a1), tuvBridge.prodMath)
    }

    fun <U, V> minus(other: Linear<U>, tuvBridge: MathBridge<T, U, V>): Linear<V> {
        return Linear(tuvBridge.minus(a0, other.a0), tuvBridge.minus(a1, other.a1), tuvBridge.prodMath)
    }


    fun <U, V> times(other: U, bridge: MathBridge<T, U, V>): Linear<V> {
        return Linear(bridge.times(a0, other), bridge.times(a1, other), bridge.prodMath)
    }

    fun times(other: T): Linear<T> {
        return times(other, math.selfBridge)
    }


    fun tri(): T = math.minus(a1, a0)

    fun hat(): T = math.div(math.plus(a1, a0), 2.0)

}
