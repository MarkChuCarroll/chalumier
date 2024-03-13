package org.goodmath.chalumier

/**
 * This is an implementation of CIML - the Chalumier Instrument Markup Language.
 */
class CIML {
}


/**
 * The main parser doesn't actually understand anything about what it's parsing.
 * It just does lexical processing, and then triggers events to tell something
 * else about what it's found.
 *
 * Each of those events returns "true" if they were able to process the event.
 * Each of the data types will have handlers for these events which will
 * trigger other events until something useful happens.
 *
 * There will be a top-level handler for a type like "InstrumentSpec". It will
 * accept an "ident" event for its top-level instrument name, and then
 * only accept a 'startDict". Once it's gotten the start dict, it will start
 * passing on all events to the dict type for its body, until that rejects
 * something, at which point it will look at the rejected event, and if it
 * was an end-dict, it will accept that, return true, and be done.
 *
 * The dict type will try sending the event to each of its fields, and if
 * any of them accepts, ...
 *
 *
 */
interface ParseEvents {
    fun str(s: String): Boolean
    fun int(i: Int): Boolean
    fun ident(id: String): Boolean

    fun startArray(): Boolean
    fun endArray(): Boolean
    fun startDict(): Boolean
    fun endDict(): Boolean

}

class Parsed<T: DataType> {

}

/**
 * I want it to be schema driven, because I'm a bit of a dork.
 */
abstract class DataType(open val name: String) {
    fun ind(i: Int): String = "   ".repeat(i)

    /**
     * Read an elemento of this datatype from the front of the string,
     * and return the parsed result, along  with the remainer of the string
     * that came after the value.
     */
    abstract fun parseFrom(input: String): Pair<Parsed<DataType>, String>

    open fun render(indent: Int): String = name
}
object IntType: DataType("Int") {
}
object DoubleType: DataType("Double") {

}
object StringType: DataType("String") {

}
open class DictType(override val name: String, val fields: Map<String, DataType>): DataType(name) {
    override fun render(i: Int): String {
        val fstr = fields.map { "${ind(i+1)}${it.key} -> ${it.value.render(i+1)}" }.joinToString(",\n")
        return "$name -> Dict(\n${fstr}\n${ind(i)})"
    }
}

data class ArrayType(val elementType: DataType): DataType( "${elementType.name}[]") {
    override fun render(indent: Int): String {
        return "Array(${elementType.render(indent)})"
    }
}

data class OptionalType(val dt: DataType): DataType("${dt.name}?")

class TupleType(elements: List<DataType>): DataType("(${elements.map { it.name }.joinToString(", ")})") {


}

data class EnumType(override val name: String, val values: List<String>): DataType(name) {
    override fun render(ind: Int): String {
        return "Enum $name ( ${values.joinToString (", ")}  )"
    }
}

val FingeringType = DictType("Fingering",
    mapOf("noteName" to StringType,
        "fingering" to ArrayType((EnumType("Hole", listOf("Open", "Close")))),
        "nth" to OptionalType(IntType)))

val InstrumentSpec = DictType("Instrument",
    mapOf("name" to StringType,
        "instrumentType" to StringType,
        "numberOfHoles" to IntType,
        "fingerings" to ArrayType(FingeringType),
        "innerDiameters" to OptionalType(ArrayType(TupleType(listOf(DoubleType, DoubleType)))),
        "length" to OptionalType(DoubleType),
        "minHoleDiameters" to OptionalType(ArrayType(DoubleType))))


fun main() {
    System.out.println(InstrumentSpec.render(1))
}
