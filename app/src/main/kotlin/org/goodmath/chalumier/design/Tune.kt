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

import kotlin.math.max

 class Working<T: InstrumentDesigner<U>, U: Instrument<U>>(
    val designer: T
) {
    val parameters = HashMap<String, Double>()
}

/**
 * ph: Modelling the mouthpiece is more difficult than modelling the body of an
 * instrument. Some parameters are most easily determined empirically.
 * This tool tries to explain observed frequencies obtained from an instrument
 * by tweaking parameters to do with the mouthpiece.
 *
 * Resultant parameters should then result in a correctly tuned instrument
 * when the design tool is run again.
 * @param tweak Comma separated list of parameters to tweak.
 * @param observations Comma separated lists of frequency followed by
 *          whether each finger hole is open (0) or closed (1)
 *          (from bottom to top).
 */
open class Tune<D: InstrumentDesigner<T>, T: Instrument<T>>(val designer: D) {
    val working = Working<D, T>(designer)

    /*
    open var tweak: List<String> by ConfigurationParameter {
        emptyList()
    }

    open var observations: List<Pair<Double, List<Double>>> by ConfigurationParameter {
        emptyList()
    }

     */

    fun constraintScore(state: List<Double>): Double {
        return state.sumOf { max(-it, 0.0) }
    }

    fun errors(state: List<Double>): List<Double> {
        /*        val mod = working.designer(tweak.zip(state))

        val mod = working.designer(
            **dict(zip(self.working.parameters,state))
            )

        instrument = mod.patch_instrument(
            mod.unpack(self.working.designer.state_vec)
            )
        instrument.prepare_phase()

        errors = [ ]

        s = 1200.0/math.log(2)
        for item in self.observations:
            parts = item.split(',')
            assert len(parts) == (mod.n_holes+1)
            fingers = [ int(item2) for item2 in parts[1:] ]
            w_obtained = design.SPEED_OF_SOUND / float(parts[0])
            w_expected = instrument.true_wavelength_near(w_obtained, fingers)

            errors.append( (math.log(w_obtained)-math.log(w_expected))*s )

        return errors

     */
        return emptyList()
    }
}
