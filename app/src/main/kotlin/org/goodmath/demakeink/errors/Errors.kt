package org.goodmath.demakeink.errors

fun dAssert(v: Boolean, msg: String) {
  if (!v) {
    throw AssertionException(msg)
  }
}

open class DemakeinException(msg: String, cause: Throwable? = null) : Exception(msg, cause)
class RequiredParameterException(name: String, msg: String = "is a required parameter") :
    DemakeinException("${name} ${msg}")

class AssertionException(msg: String) : DemakeinException(msg)
