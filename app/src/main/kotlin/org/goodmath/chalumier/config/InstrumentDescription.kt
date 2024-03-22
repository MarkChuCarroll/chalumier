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
package org.goodmath.chalumier.config

import java.io.Reader
import java.io.StreamTokenizer

/*
 * What's going on here is that I was really unhappy with the way
 * that Json and its variations were working as a configuration/description
 * language for Chalumier instruments.
 *
 * So being a nerd, I wrote my own. It's similar to something like
 * JSon5, except that it's deeply hooked into the Chalumier
 * configuration parameter scheme, and it's capable of generating
 * less hostile error messages.
 *
 * ## Rough grammar
 *
 * ```
 *  file ::= IDENT object
 *  object ::=  "{"  field+ "}"
 *  field ::=  IDENT "="  value
 *  value ::= string | double | int | array | object | none | true | false
 *  array: "[" (value (, value)*)? "]"
 *  comment: # ......
 */

class DescriptionParseException(msg: String, line: Int) : Exception("Error in instrument description at line $line: $msg")

class InstrumentDescription(val name: String, val values: Map<String, Any?>)

class DescriptionParser(private val input: Reader) {
  private val tokenizer = setupTokenizer()
  private fun setupTokenizer(): StreamTokenizer {
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
    TLBracket,
    TRBracket,
    TLCurly,
    TRCurly,
    TEof
  }

  private var currentTokenType: TokenType = TokenType.TEof

  private var tokStr: String? = null

  private var tokNum: Double? = null

  private fun nextToken(): TokenType {
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
        TokenType.TLBracket
      }
      ']'.code -> {
        tokStr = "]"
        tokNum = null
        TokenType.TRBracket
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
      else -> throw DescriptionParseException("Unknown token type $t", tokenizer.lineno())
    }
    return currentTokenType
  }

  @Throws(DescriptionParseException::class)
  private fun expect(t: TokenType) {
    if (currentTokenType != t) {
      throw DescriptionParseException("Expected a $t, but found $currentTokenType",
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

  private fun parseValue(): Any? {
    return when(currentTokenType) {
      TokenType.TStr -> tokStr!!
      TokenType.TIdent -> {
        val s = tokStr!!
        if (s == "null") {
          null
        } else {
          s
        }
      }
      TokenType.TNumber -> tokNum!!
      TokenType.TLBracket -> parseList()
      TokenType.TLCurly -> parseObject()
      else -> throw DescriptionParseException("Expected a value, but found $currentTokenType",
        tokenizer.lineno())
    }
  }

  private fun parseList(): List<Any?> {
    nextToken()
    if (currentTokenType == TokenType.TRBracket) {
      return emptyList()
    }
    val result = ArrayList<Any?>()
    do {
      val v = parseValue()
      result.add(v)
      nextToken()
    } while ((currentTokenType == TokenType.TComma).also { if(it) { nextToken() } })
    expect(TokenType.TRBracket)
    return result
  }

  private fun parseObject(): Map<String, Any?> {
    val result = HashMap<String, Any?>()
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

