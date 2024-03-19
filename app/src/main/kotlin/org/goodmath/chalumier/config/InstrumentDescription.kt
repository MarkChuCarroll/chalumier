package org.goodmath.chalumier.config

import java.io.Reader
import java.io.StreamTokenizer

/* Rough grammar
file: IDENT object
object:  "{"  field+ "}"
field: IDENT "="  value
value: string | double | int | array | object | none
array: "[" value(,) "]"
comment: # ......
 */

class DescriptionParseException(msg: String, line: Int) : Exception("$msg at $line")

class InstrumentDescription(val name: String, val values: Map<String, Any>) {

}

class DescriptionParser(val input: Reader) {
  val tokenizer = setupTokenizer()
  fun setupTokenizer(): StreamTokenizer {
      val t = StreamTokenizer(input).also {
          it.commentChar('#'.code)
          it.eolIsSignificant(false)
          it.parseNumbers()
      }
      listOf(
            ',',
            ']',
            '[',
            '}',
            '{',
            '=',
        )
        .forEach { t.ordinaryChar(it.code) }
    return t
  }

  enum class TokenType {
    TIdent,
    TNumber,
    TStr,
    TComma,
    TEq,
    TLBrack,
    TRBrack,
    TLCurly,
    TRCurly,
    TEof
  }

  var currentTokenType: TokenType = TokenType.TEof
  var tokStr: String? = null
  var tokNum: Double? = null
  fun nextToken(): TokenType {
    val t = tokenizer.nextToken()
    currentTokenType = when (t) {
      StreamTokenizer.TT_EOF -> {
        tokStr = null
        tokNum = null
        TokenType.TEof
      }
      StreamTokenizer.TT_WORD -> {
        tokStr = tokenizer.sval
        tokNum = null
        TokenType.TIdent
      }
      StreamTokenizer.TT_NUMBER -> {
        tokStr = null
        tokNum = tokenizer.nval
        TokenType.TNumber
      }
      '"'.code -> {
        tokStr = tokenizer.sval
        tokNum = null
        TokenType.TStr
      }
      '['.code -> {
        tokStr = "["
        tokNum = null
        TokenType.TLBrack
      }
      ']'.code -> {
        tokStr = "]"
        tokNum = null
        TokenType.TRBrack
      }
      '{'.code -> {
        tokStr = "{"
        tokNum = null
        TokenType.TLCurly
      }
      '}'.code -> {
        tokStr = "}"
        tokNum = null
        TokenType.TRCurly
      }
      ','.code -> {
        tokStr = ","
        tokNum = null
        TokenType.TComma
      }
      '='.code -> {
        tokStr = "="
        tokNum = null
        TokenType.TEq
      }
      else -> throw DescriptionParseException("Unknown token type ${t}", tokenizer.lineno())
    }
    return currentTokenType
  }

  fun expect(t: TokenType) {
    if (currentTokenType != t) {
      throw DescriptionParseException("Expected a ${t}, but found ${currentTokenType}",
        tokenizer.lineno())
    }
  }

  fun parseConfig(): InstrumentDescription {
    nextToken()
    expect(TokenType.TIdent)
    val name = tokStr!!
    nextToken()
    val body = parseObject()
    nextToken()
    expect(TokenType.TEof)
    return InstrumentDescription(name, body)
  }

  fun parseField(): Pair<String, Any> {
    expect(TokenType.TIdent)
    val name = tokStr!!
    nextToken()
    expect(TokenType.TEq)
    nextToken()
    val v = parseValue()
    return Pair(name, v)
  }

  fun parseValue(): Any {
    return when(currentTokenType) {
      TokenType.TStr -> tokStr!!
      TokenType.TIdent -> tokStr!!
      TokenType.TNumber -> tokNum!!
      TokenType.TLBrack -> parseList()
      TokenType.TLCurly -> parseObject()
      else -> throw DescriptionParseException("Expected a value, but found ${currentTokenType}",
        tokenizer.lineno())
    }
  }

  fun parseList(): List<Any> {
    nextToken()
    if (currentTokenType == TokenType.TRBrack) {
      nextToken()
      return emptyList()
    }
    val result = ArrayList<Any>()
    do {
      val v = parseValue()
      result.add(v)
      nextToken()
    } while ((currentTokenType == TokenType.TComma).also { if(it) { nextToken() } })
    expect(TokenType.TRBrack)
    return result
  }

  fun parseObject(): Map<String, Any> {
    val result = HashMap<String, Any>()
    nextToken()
    if (currentTokenType == TokenType.TRCurly) {
      return result
    }
    do {
      expect(TokenType.TIdent)
      val name = tokStr!!
      nextToken()
      expect(TokenType.TEq)
      nextToken()
      val v = parseValue()
      result[name] = v
      nextToken()
    } while ((currentTokenType == TokenType.TComma).also { if(it) { nextToken() } })
    expect(TokenType.TRCurly)
    return result
  }
}

