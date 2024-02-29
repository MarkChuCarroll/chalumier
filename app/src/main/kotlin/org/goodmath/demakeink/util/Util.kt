package org.goodmath.demakeink.util

fun<T> List<T>.fromEnd(i: Int): T = this[this.size-(i+1)]

fun<T> List<T>.repeat(i: Int): List<T> {
    return (0 until i).flatMap { this }
}

fun<T> MutableList<T>.repeat(i: Int): MutableList<T> {
    return (0 until i).flatMap { this }.toMutableList()
}


