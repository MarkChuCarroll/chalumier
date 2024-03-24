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

import org.goodmath.chalumier.design.instruments.TaperedFlute
import org.goodmath.chalumier.optimize.Score
import org.goodmath.chalumier.optimize.ScoredParameters
import org.goodmath.chalumier.util.RecordedRandomizer
import org.goodmath.chalumier.util.assertFloatListEquals
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.io.path.Path

/**
 * A batch of tests to help debug the instrument designer.
 */
class InstrumentDesignerTest {

    fun makeDesigner(): InstrumentDesigner<TaperedFlute> {
        val flute = TaperedFluteDesigner("FolkFlute",
            Path("/tmp/test"))
        flute.numberOfHoles = 7
        flute.fingerings = FluteDesigner.fingeringsWithEmbouchure(FluteDesigner.folkFingerings)
        flute.balance = arrayListOf(0.1, null, null, 0.1, null)
        flute.holeAngles = arrayListOf(-30.0, 30.0, 30.0, -30.0, 0.0, 30.0, 0.0)
        flute.maxHoleSpacing = flute.scaler(listOf(45.0, 45.0, null, 45.0, 45.0, null))
        return flute
    }

    @Test
    fun testInitialDesignState() {
        val flute = makeDesigner()
        val i = flute.initialDesignParameters()
        val expected = arrayListOf(1.0, 0.175, 0.2583333333333333, 0.3416666666666667, 0.425, 0.5083333333333333, 0.5916666666666667, 0.97, 0.5625, 0.5625, 0.5625, 0.5625, 0.5625, 0.5625, 0.5625, 0.25, 0.3, 0.7, 0.8, 0.81, 0.9, 0.01, 0.666)
        assertIterableEquals(expected, (0 until i.size).map { i[it] })
    }


    @Test
    fun testMakeInstrumentFromDesign() {
        val fluteDesigner = makeDesigner()
        val i = fluteDesigner.initialDesignParameters()
        val flute = fluteDesigner.makeInstrumentFromParameters(i)
        assertEquals(589.2773628488858, flute.length, 1e-5)
        val expectedHoleLengths = listOf(6.676972266578878, 6.771590437730453, 5.875118062604776, 5.9161637717954125, 5.328290142276424, 5.998255190176693, 5.300000000000001)
        assertFloatListEquals(expectedHoleLengths, flute.holeLengths, 1e-5, "holeLength")
        assertFloatListEquals(
            listOf(13.799999999999999, 13.799999999999999, 16.099999999999998, 18.4, 21.0, 21.0, 18.4, 18.4),
            flute.inner.low, 1e-5, "profile.inner.low")
        assertFloatListEquals(listOf(13.799999999999999, 13.799999999999999, 16.099999999999998, 18.4, 21.0, 21.0, 18.4, 18.4),
            flute.inner.high, 1e-5, "profile.inner.high")
        val expectedInnerPos = listOf(0.0, 147.31934071222145, 176.78320885466573, 412.49415399422, 471.42189027910865, 477.3146639075975, 530.3496265639973, 589.2773628488858)
        assertFloatListEquals(expectedInnerPos, flute.inner.pos, 0e-5, "inner.pos")
        val expectedOuter = listOf(24.65, 24.65, 29.0, 29.0)
        assertFloatListEquals(expectedOuter, flute.outer.low, 1e-5, "profile.outer.low")
        assertFloatListEquals(expectedOuter, flute.outer.high, 1e-5, "profile.outer.high")
        val expectedOuterPos = listOf(0.0, 5.892773628488858, 392.45872365735795, 589.2773628488858)
        assertFloatListEquals(expectedOuterPos, flute.outer.pos, 0e-5, "inner.pos")
        assertFloatListEquals(listOf(5.892773628488858, 392.45872365735795),
            flute.outerKinks, 1e-5, "outerKinks")
        val expectedInnerKinks = listOf(147.31934071222145, 176.78320885466573, 412.49415399422, 471.42189027910865, 477.3146639075975, 530.3496265639973)
        assertFloatListEquals(expectedInnerKinks, flute.innerKinks, 1e-5, "innerKinks")
        val expectedHolePositions = listOf(103.123538498555, 152.2299854026288, 201.33643230670265, 250.44287921077645, 299.54932611485026, 348.6557730189241, 571.5990419634192)
        assertFloatListEquals(expectedHolePositions, flute.holePositions, 1e-5, "holePositions")
        val expectedHoleDiameters = listOf(9.3, 9.3, 9.3, 9.3, 9.3, 9.3, 11.375)
        assertFloatListEquals(expectedHoleDiameters, flute.holeDiameters, 1e-5, "holeDiameters")
        val expectedHoleAngles = listOf(-30.0, 30.0, 30.0, -30.0, 0.0, 30.0, 0.0)
        assertFloatListEquals(expectedHoleAngles, flute.holeAngles, 1e-5, "holeAngles")
        val expectedInnerHolePositions = listOf(100.13750572416477, 155.2583327095394, 203.96386497946688, 247.79709033882523, 299.54932611485026, 351.3382742892493, 571.5990419634192)
        assertFloatListEquals(expectedInnerHolePositions, flute.innerHolePositions, 1e-5, "innerHolePositions")

    }

    @Test
    fun testSteppedProfile() {
        val expectedPos = listOf(0.0, 147.31934071222145, 148.87007061445536, 150.42080051668927, 151.97153041892318, 153.5222603211571, 155.072990223391, 156.6237201256249, 158.17445002785882, 159.72517993009274, 161.27590983232665, 162.82663973456056, 164.37736963679444, 165.92809953902835, 167.47882944126226, 169.02955934349617, 170.58028924573009, 172.131019147964, 173.6817490501979, 175.23247895243182, 176.78320885466573, 189.18904807253702, 201.59488729040828, 214.00072650827957, 226.40656572615086, 238.81240494402212, 251.2182441618934, 263.62408337976467, 276.02992259763596, 288.43576181550725, 300.84160103337854, 313.2474402512498, 325.6532794691211, 338.05911868699235, 350.46495790486364, 362.87079712273487, 375.27663634060616, 387.68247555847745, 400.08831477634874, 412.49415399422, 415.30023667445283, 418.10631935468564, 420.9124020349184, 423.7184847151512, 426.524567395384, 429.3306500756168, 432.13673275584955, 434.94281543608236, 437.74889811631516, 440.55498079654797, 443.3610634767807, 446.1671461570135, 448.9732288372463, 451.77931151747913, 454.5853941977119, 457.3914768779447, 460.1975595581775, 463.0036422384103, 465.80972491864304, 468.61580759887585, 471.42189027910865, 477.3146639075975, 479.840138319807, 482.36561273201653, 484.89108714422605, 487.41656155643557, 489.9420359686451, 492.46751038085455, 494.99298479306407, 497.5184592052736, 500.0439336174831, 502.5694080296926, 505.09488244190214, 507.62035685411166, 510.1458312663212, 512.6713056785306, 515.1967800907402, 517.7222545029497, 520.2477289151592, 522.7732033273687, 525.2986777395782, 527.8241521517878, 530.3496265639973, 589.2773628488858)
        val expectedLow = listOf(13.799999999999999, 13.799999999999999, 13.860526315789471, 13.98157894736842, 14.102631578947367, 14.223684210526315, 14.344736842105261, 14.465789473684211, 14.586842105263155, 14.707894736842107, 14.828947368421051, 14.95, 15.071052631578947, 15.192105263157892, 15.31315789473684, 15.434210526315788, 15.555263157894734, 15.676315789473682, 15.797368421052628, 15.918421052631576, 16.039473684210524, 16.160526315789472, 16.28157894736842, 16.402631578947364, 16.523684210526312, 16.64473684210526, 16.765789473684208, 16.886842105263156, 17.007894736842104, 17.128947368421052, 17.25, 17.371052631578948, 17.492105263157896, 17.61315789473684, 17.73421052631579, 17.855263157894733, 17.97631578947368, 18.09736842105263, 18.218421052631577, 18.339473684210525, 18.461904761904762, 18.585714285714285, 18.70952380952381, 18.83333333333333, 18.957142857142856, 19.08095238095238, 19.204761904761906, 19.32857142857143, 19.45238095238095, 19.576190476190476, 19.7, 19.823809523809523, 19.94761904761905, 20.07142857142857, 20.195238095238096, 20.31904761904762, 20.442857142857143, 20.56666666666667, 20.69047619047619, 20.814285714285713, 20.938095238095237, 21.0, 20.93809523809524, 20.814285714285713, 20.69047619047619, 20.566666666666666, 20.44285714285714, 20.31904761904762, 20.195238095238096, 20.071428571428573, 19.94761904761905, 19.823809523809523, 19.7, 19.576190476190476, 19.45238095238095, 19.32857142857143, 19.20476190476191, 19.08095238095238, 18.95714285714286, 18.83333333333333, 18.709523809523812, 18.585714285714282, 18.461904761904766, 18.4)
        val expectedHigh = listOf(13.799999999999999, 13.860526315789471, 13.98157894736842, 14.102631578947367, 14.223684210526315, 14.344736842105261, 14.465789473684211, 14.586842105263155, 14.707894736842107, 14.828947368421051, 14.95, 15.071052631578947, 15.192105263157892, 15.31315789473684, 15.434210526315788, 15.555263157894734, 15.676315789473682, 15.797368421052628, 15.918421052631576, 16.039473684210524, 16.160526315789472, 16.28157894736842, 16.402631578947364, 16.523684210526312, 16.64473684210526, 16.765789473684208, 16.886842105263156, 17.007894736842104, 17.128947368421052, 17.25, 17.371052631578948, 17.492105263157896, 17.61315789473684, 17.73421052631579, 17.855263157894733, 17.97631578947368, 18.09736842105263, 18.218421052631577, 18.339473684210525, 18.461904761904762, 18.585714285714285, 18.70952380952381, 18.83333333333333, 18.957142857142856, 19.08095238095238, 19.204761904761906, 19.32857142857143, 19.45238095238095, 19.576190476190476, 19.7, 19.823809523809523, 19.94761904761905, 20.07142857142857, 20.195238095238096, 20.31904761904762, 20.442857142857143, 20.56666666666667, 20.69047619047619, 20.814285714285713, 20.938095238095237, 21.0, 20.93809523809524, 20.814285714285713, 20.69047619047619, 20.566666666666666, 20.44285714285714, 20.31904761904762, 20.195238095238096, 20.071428571428573, 19.94761904761905, 19.823809523809523, 19.7, 19.576190476190476, 19.45238095238095, 19.32857142857143, 19.20476190476191, 19.08095238095238, 18.95714285714286, 18.83333333333333, 18.709523809523812, 18.585714285714282, 18.461904761904766, 18.4, 18.4)
        val fluteDesigner = makeDesigner()
        val i = fluteDesigner.initialDesignParameters()
        val flute = fluteDesigner.makeInstrumentFromParameters(i)
        val stepped = flute.inner.asStepped(0.125)
        assertFloatListEquals(expectedPos, stepped.pos, 1e-5, "pos")
        assertFloatListEquals(expectedHigh, stepped.high, 1e-5, "high")
        assertFloatListEquals(expectedLow, stepped.low, 1e-5, "low")
    }

    @Test
    fun testInstrumentDesignerInitialization() {
        val fluteDesigner = makeDesigner()
        val expectedInnerFractions = listOf(0.25, 0.3, 0.7, 0.8, 0.81, 0.9)
        assertFloatListEquals(
            expectedInnerFractions,
            fluteDesigner.initialInnerFractions,
            1e-5,
            "initialInnerFractions"
        )
    }

    fun getRecordedRandom(): RecordedRandomizer {
        val random = RecordedRandomizer(
            doubles = listOf(0.7442288198802703, 0.7995894622947219),
            integers = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            gaussians = listOf(
                4.830559519528934,
                -0.00011018268456026157,
                -0.0009221369180267284,
                0.0018040372391395034,
                -0.0006459913814271084,
                -0.0007769717770577334,
                0.0006682581881567113,
                0.0011569697136274665,
                -0.0006249019802052067,
                0.0003756703177703314,
                -0.0006708149808992422,
                0.0005070150415227693,
                -0.0004789191119228822,
                -0.0001712177559438517,
                0.0007806936821711107,
                0.0009305973870391529,
                0.0007199731210607037,
                0.00031342654323062753,
                0.0007415036187189935,
                -0.0009665587523362405,
                -0.00010184807913805108,
                -0.0004084495759028993,
                1.7726085196983975e-05,
                -0.0011488507966401329
            )
        )

        return random
    }



    @Test
    fun testConstraintScore() {
        val fluteDesigner = makeDesigner()
        val i = fluteDesigner.initialDesignParameters()
        val flute = fluteDesigner.makeInstrumentFromParameters(i)
        assertEquals(589.2773628488858, flute.length, 1e-5)
        var cScore = fluteDesigner.constraintScore(flute)
        assertEquals(45.88965575873958, cScore, 1e-4)
        val j = DesignParameters.generateNewDesignParameters(listOf(i), 0.001, true, getRecordedRandom())
        val jFlute = fluteDesigner.makeInstrumentFromParameters(j)
        val jScore = fluteDesigner.constraintScore(jFlute)
        assertEquals(47.40651968720748, jScore)
    }

    @Test
    fun testGenerateNewModel() {
        val random = Random()
        fun getScores(designer: InstrumentDesigner<TaperedFlute>, params: DesignParameters): ScoredParameters {
            val cScore = designer.constraintScorer(params)
            if (cScore > 0) {
                return ScoredParameters(params, Score(cScore, 0.0))
            } else {
                return ScoredParameters(params, Score(0.0, 1.0 + random.nextDouble()))
            }
        }
        val fluteDesigner = makeDesigner()
        val first = fluteDesigner.initialDesignParameters()
        val expected = listOf(0.9998898173154397, 0.17407786308197326, 0.2601373705724728, 0.34102067528523955, 0.4242230282229423, 0.50900159152149, 0.5928236363802941, 0.9693750980197947, 0.5628756703177703, 0.5618291850191007, 0.5630070150415227, 0.5620210808880771, 0.5623287822440561, 0.5632806936821712, 0.5634305973870392, 0.2507199731210607, 0.3003134265432306, 0.7007415036187189, 0.7990334412476638, 0.809898151920862, 0.8995915504240971, 0.010017726085196984, 0.6648511492033599)
        val up = DesignParameters.generateNewDesignParameters(listOf(first), 0.001, true, getRecordedRandom())
        for (i in 0 until up.size) {
            assertEquals(expected[i], up[i], "Element ${i}")
        }
    }



}
