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

import kotlin.math.sqrt


data class XYZ(val x: Double, val y: Double, val z: Double) {
    operator fun unaryMinus(): XYZ {
        return XYZ(-x, -y, -z)
    }

    operator fun plus(other: XYZ): XYZ {
        return XYZ(x+other.x, y+other.y, z+other.z)
    }

    operator fun minus(other: XYZ): XYZ {
        return XYZ(x-other.x, y-other.y, z-other.z)
    }

    operator fun times(other: Double): XYZ {
        return XYZ(x*other, y*other, z*other)
    }

    fun dot(other: XYZ): Double {
        return  x * other.x + y * other.y + z * other.z
    }

    fun cross(other: XYZ): XYZ {
        return XYZ(y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x)
    }

    fun mag2(): Double = dot(this)

    fun mag(): Double {
        return sqrt(mag2())
    }

    fun unit(): XYZ {
        return this * (1.0/mag())
    }

    fun reciprocal(): XYZ {
        val origMag = mag()
        return unit().times(1.0/origMag)
    }
}
