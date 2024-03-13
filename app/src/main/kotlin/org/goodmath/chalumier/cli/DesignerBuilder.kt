package org.goodmath.chalumier.cli

import org.goodmath.chalumier.design.Fingering
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.errors.ChalumierException

class DesignerBuilder(val templates: Map<String, (name: String) -> InstrumentDesigner>) {

    fun getDesigner(spec: InstrumentSpec): InstrumentDesigner {
        val baseTemplate = templates.get(spec.instrumentType) ?: throw ChalumierException("Unknown instrument type ${spec.instrumentType}")
        val designer = baseTemplate(spec.name)
        designer.numberOfHoles = spec.numberOfHoles
        designer.fingerings = toFingerings(spec.fingerings)
        spec.innerDiameters?.let { designer.innerDiameters = it }
        spec.outerDiameters?.let { designer.outerDiameters = it }
        spec.length?.let { designer.length = it}
        spec.maxLength?.let { designer.maxLength = it}
        designer.closedTop = spec.closedTop
        spec.initialLength?.let { designer.initialLength = it }
        designer.transpose = spec.transpose
        spec.tweakEmissions?.let { designer.tweakEmissions = it }
        spec.minHoleDiameters?.let {
            designer.minHoleDiameters = ArrayList(it) }
        spec.maxHoleDiameters?.let { designer.maxHoleDiameters = ArrayList(it) }
        designer.outerAdd = spec.outerAdd
        designer.topClearanceFraction = spec.topClearanceFraction
        designer.bottomClearanceFraction = spec.bottomClearanceFraction
        designer.scale = spec.scale
        spec.minHoleSpacing?.let { designer.minHoleSpacing = ArrayList(it) }
        spec.maxHoleSpacing?.let { designer.maxHoleSpacing = ArrayList(it) }
        spec.balance?.let { designer.balance = ArrayList(it) }
        spec.holeAngles?.let { designer.holeAngles = ArrayList(it) }
        spec.holeHorizAngles?.let { designer.holeHorizAngles = ArrayList(it)}
        spec.divisions?.let { designer.divisions = ArrayList(it.map { el -> ArrayList(el) })}
        return designer
    }

    private fun toFingerings(fingerings: List<FingeringSpec>): ArrayList<Fingering> {
        return ArrayList(fingerings.map { f ->
            Fingering(f.noteName, f.fingering.map { when(it) {
                Hole.Open -> 0.0
                Hole.Closed -> 1.0
            }
            }, f.nTh)})
    }
}
