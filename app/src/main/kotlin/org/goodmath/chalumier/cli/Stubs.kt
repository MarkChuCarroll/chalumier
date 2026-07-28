package org.goodmath.chalumier.cli

data class MakerState(
    val name: String,
    val stage: String,
    val total: Int,
    val current: Int,
    val multiplier: Int
)

class ProgressMonitor<T>(val name: String) {
    fun updateState(state: T) {
        // Stub: print progress
    }
    fun print(a: Any) {
        println(a)
    }
}

class BoundedTranscript(private val maxLines: Int) {
    private val lines = mutableListOf<String>()
    fun print(line: String) {
        lines.add(line)
        if (lines.size > maxLines) lines.removeAt(0)
        println(line)
    }
    fun transcript(): List<String> = lines.toList()
}
