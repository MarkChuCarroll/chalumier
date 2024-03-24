package org.goodmath.chalumier.cli


class BoundedTranscript(val size: Int) {
    val elements: Array<String> = Array<String>(size) { "" }
    var front = 0
    fun print(s: String) {
        synchronized(this) {
            elements[front] = s
            front = (front + 1) % size
        }
    }

    operator fun get(idx: Int): String {
        return elements[(front + idx) % size]
    }

    fun transcript(): List<String> {
        synchronized(this) {
            return (0 until size).map {
                this[it]
            }
        }
    }
}
