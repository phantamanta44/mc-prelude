package st.evening.mc.prelude.api.data.tjson

import st.evening.mc.prelude.api.type.Maybe
import st.evening.mc.prelude.api.type.orElse

class TypedJsonParser(private val buffer: String) {
    private var pos: Int = 0

    private var peeked: Maybe<PosToken?> = Maybe.Nothing()

    private fun peek(): PosToken? = peeked.orElse { tokenizeNext().also { peeked = Maybe.Just(it) } }

    private fun PosToken.slice(): String = buffer.substring(from, to)

    private fun stepOver(token: PosToken) {
        pos = token.to
        peeked = Maybe.Nothing()
    }

    @Throws(JsonParseException::class)
    private fun throwParseError(pos: Int, message: String): Nothing {
        var line = 1
        var col = 1
        val iter = buffer.iterator()
        repeat(pos) {
            if (iter.next() == '\n') {
                line++
                col = 1
            } else {
                col++
            }
        }
        throw JsonParseException(line, col, message)
    }

    // TOKENIZER -------------------------------------------------------------------------------------------------------

    @Throws(JsonParseException::class)
    private fun tokenizeNext(): PosToken? {
        var tokenStart = pos
        tokenize@ while (true) {
            if (tokenStart >= buffer.length) return null
            if (buffer[tokenStart].isWhitespace()) {
                tokenStart++
                continue
            }
            if (buffer[tokenStart] == '/' && tokenStart + 1 < buffer.length) {
                when (buffer[tokenStart + 1]) {
                    '/' -> {
                        tokenStart += 2
                        while (buffer[tokenStart] != '\n') {
                            tokenStart++
                            if (tokenStart >= buffer.length) return null
                        }
                        tokenStart++
                        continue
                    }

                    '*' -> {
                        tokenStart += 2
                        var commentDepth = 1
                        while (true) {
                            if (tokenStart + 1 >= buffer.length) {
                                throwParseError(tokenStart + 1, "Unclosed block comment")
                            }
                            when (buffer[tokenStart]) {
                                '/' -> {
                                    if (buffer[tokenStart + 1] == '*') {
                                        tokenStart += 2
                                        commentDepth++
                                        continue
                                    }
                                }

                                '*' -> {
                                    if (buffer[tokenStart + 1] == '/') {
                                        tokenStart += 2
                                        commentDepth--
                                        if (commentDepth <= 0) {
                                            continue@tokenize
                                        }
                                        continue
                                    }
                                }
                            }
                            tokenStart++
                        }
                    }
                }
            }
            break
        }
        when (buffer[tokenStart]) {
            '{' -> return PosToken(tokenStart, tokenStart + 1, Token.OpenObject)
            '}' -> return PosToken(tokenStart, tokenStart + 1, Token.CloseObject)
            '[' -> return PosToken(tokenStart, tokenStart + 1, Token.OpenArray)
            ']' -> return PosToken(tokenStart, tokenStart + 1, Token.CloseArray)
            ':' -> return PosToken(tokenStart, tokenStart + 1, Token.KeySep)
            ',' -> return PosToken(tokenStart, tokenStart + 1, Token.ElemSep)
            '"' -> return tokenizeStringLiteral(tokenStart)
        }
        var tokenEnd = tokenStart + 1
        while (tokenEnd < buffer.length && isStringTokenChar(buffer[tokenEnd])) {
            tokenEnd++
        }
        val tokenString = buffer.substring(tokenStart, tokenEnd)
        when (tokenString) {
            "null" -> return PosToken(tokenStart, tokenEnd, Token.NullLiteral)
            "true" -> return PosToken(tokenStart, tokenEnd, Token.TrueLiteral)
            "false" -> return PosToken(tokenStart, tokenEnd, Token.FalseLiteral)
        }
        return tokenizeNumberLiteral(tokenString, tokenStart, tokenEnd)
    }

    @Throws(JsonParseException::class)
    private fun tokenizeStringLiteral(tokenStart: Int): PosToken {
        var i = tokenStart + 1
        val stringBuf = StringBuilder()
        while (true) {
            if (i >= buffer.length) throwParseError(tokenStart, "Reached EOF while reading a string")
            when (val c = buffer[i]) {
                '"' -> return PosToken(tokenStart, i + 1, Token.StringLiteral(stringBuf.toString()))
                '\\' -> {
                    if (++i >= buffer.length) throwParseError(tokenStart, "Reached EOF while reading a string")
                    when (val esc = buffer[i]) {
                        '"', '\\', '/' -> {
                            stringBuf.append(esc)
                            i++
                        }

                        'b' -> {
                            stringBuf.append('\b')
                            i++
                        }

                        'f' -> {
                            stringBuf.append('\u000c')
                            i++
                        }

                        'n' -> {
                            stringBuf.append('\n')
                            i++
                        }

                        'r' -> {
                            stringBuf.append('\r')
                            i++
                        }

                        't' -> {
                            stringBuf.append('\t')
                            i++
                        }

                        'u' -> {
                            if (i + 5 >= buffer.length) {
                                throwParseError(tokenStart, "Reached EOF while reading a string")
                            }
                            val code = buffer.substring(i + 1, i + 5)
                            try {
                                stringBuf.append(Char(code.toInt(16)))
                            } catch (_: NumberFormatException) {
                                throwParseError(i, "Bad unicode escape sequence: \\u$code")
                            }
                            i += 5
                        }

                        else -> throwParseError(i - 1, "Bad escape sequence: \\$esc")
                    }
                }

                else -> {
                    if (c.isISOControl()) {
                        throwParseError(i, "Illegal control character: ${c.code.toString(16)}")
                    } else {
                        stringBuf.append(c)
                        i++
                    }
                }
            }
        }
    }

    @Throws(JsonParseException::class)
    private fun tokenizeNumberLiteral(tokenString: String, tokenStart: Int, tokenEnd: Int): PosToken {
        val match = PATTERN_NUMBER.matchEntire(tokenString)
            ?: throwParseError(tokenStart, "Syntax error at token: $tokenString")
        val (numStr, frac, tag) = match.destructured
        if (tag.isEmpty()) {
            return if (frac.isEmpty()) {
                try {
                    PosToken(tokenStart, tokenEnd, Token.IntLiteral(numStr.toInt()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad integer literal: $tokenString")
                }
            } else {
                try {
                    PosToken(tokenStart, tokenEnd, Token.DoubleLiteral(numStr.toDouble()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad double literal: $tokenString")
                }
            }
        }
        when (tag[0]) {
            'b', 'B' -> {
                if (frac.isNotEmpty()) {
                    throwParseError(tokenStart, "Byte literal has a fractional part: $tokenString")
                }
                try {
                    return PosToken(tokenStart, tokenEnd, Token.ByteLiteral(numStr.toByte()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad byte literal: $tokenString")
                }
            }

            's', 'S' -> {
                if (frac.isNotEmpty()) {
                    throwParseError(tokenStart, "Short literal has a fractional part: $tokenString")
                }
                try {
                    return PosToken(tokenStart, tokenEnd, Token.ShortLiteral(numStr.toShort()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad short literal: $tokenString")
                }
            }

            'l', 'L' -> {
                if (frac.isNotEmpty()) {
                    throwParseError(tokenStart, "Long literal has a fractional part: $tokenString")
                }
                try {
                    return PosToken(tokenStart, tokenEnd, Token.LongLiteral(numStr.toLong()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad long literal: $tokenString")
                }
            }

            'f', 'F' -> {
                try {
                    return PosToken(tokenStart, tokenEnd, Token.FloatLiteral(numStr.toFloat()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad float literal: $tokenString")
                }
            }

            'd', 'D' -> {
                try {
                    return PosToken(tokenStart, tokenEnd, Token.DoubleLiteral(numStr.toDouble()))
                } catch (_: NumberFormatException) {
                    throwParseError(tokenStart, "Bad double literal: $tokenString")
                }
            }

            else -> throw IllegalStateException(tokenString)
        }
    }

    // PARSER ----------------------------------------------------------------------------------------------------------

    @Throws(JsonParseException::class)
    private inline fun peekAssert(expected: () -> String): PosToken = peek()
        ?: throwParseError(pos, "Reached EOF while scanning for ${expected()}")

    @Throws(JsonParseException::class)
    private inline fun consume(expected: () -> String): PosToken = peekAssert(expected).also { stepOver(it) }

    @Throws(JsonParseException::class)
    private inline fun <reified T : Token> consumeAssert(expected: () -> String): T {
        val token = peekAssert(expected)
        if (token.token !is T) throwParseError(token.from, "Expected ${expected()} but got: ${token.slice()}")
        stepOver(token)
        return token.token
    }

    @Throws(JsonParseException::class)
    fun takeObject(): TJson.Object {
        consumeAssert<Token.OpenObject> { "{" }
        val firstToken = consume { "key or }" }
        when (val firstKey = firstToken.token) {
            Token.CloseObject -> return TJson.Object()
            is Token.StringLiteral -> {
                consumeAssert<Token.KeySep> { ":" }
                val firstValue = takeValue()
                val obj = TJson.Object()
                obj[firstKey.value] = firstValue
                while (true) {
                    val token = consume { ", or }" }
                    when (token.token) {
                        Token.CloseObject -> return obj

                        Token.ElemSep -> {
                            val key = consumeAssert<Token.StringLiteral> { "key" }
                            consumeAssert<Token.KeySep> { ":" }
                            val value = takeValue()
                            obj[key.value] = value
                        }

                        else -> throwParseError(token.from, "Expected , or } but got: ${token.slice()}")
                    }
                }
            }

            else -> throwParseError(firstToken.from, "Expected key or } but got: ${firstToken.slice()}")
        }
    }

    @Throws(JsonParseException::class)
    fun takeArray(): TJson.Array {
        consumeAssert<Token.OpenArray> { "[" }
        val firstToken = peekAssert { "value or ]" }
        if (firstToken.token == Token.CloseArray) {
            stepOver(firstToken)
            return TJson.Array()
        } else {
            val firstValue = takeValue()
            val arr = TJson.Array()
            arr += firstValue
            while (true) {
                val token = consume { ", or ]" }
                when (token.token) {
                    Token.CloseArray -> return arr

                    Token.ElemSep -> {
                        val value = takeValue()
                        arr += value
                    }

                    else -> throwParseError(token.from, "Expected , or ] but got: ${token.slice()}")
                }
            }
        }
    }

    @Throws(JsonParseException::class)
    fun takeValue(): TJson {
        val token = peekAssert { "a value" }
        when (val t = token.token) {
            Token.OpenObject -> return takeObject()
            Token.OpenArray -> return takeArray()

            Token.TrueLiteral -> {
                stepOver(token)
                return TJson.Bool(true)
            }

            Token.FalseLiteral -> {
                stepOver(token)
                return TJson.Bool(false)
            }

            Token.NullLiteral -> {
                stepOver(token)
                return TJson.Null
            }

            is Token.StringLiteral -> {
                stepOver(token)
                return TJson.String(t.value)
            }

            is Token.ByteLiteral -> {
                stepOver(token)
                return TJson.Byte(t.value)
            }

            is Token.ShortLiteral -> {
                stepOver(token)
                return TJson.Short(t.value)
            }

            is Token.IntLiteral -> {
                stepOver(token)
                return TJson.Int(t.value)
            }

            is Token.LongLiteral -> {
                stepOver(token)
                return TJson.Long(t.value)
            }

            is Token.FloatLiteral -> {
                stepOver(token)
                return TJson.Float(t.value)
            }

            is Token.DoubleLiteral -> {
                stepOver(token)
                return TJson.Double(t.value)
            }

            else -> throwParseError(token.from, "Expected a value but got: ${token.slice()}")
        }
    }

    @Throws(JsonParseException::class)
    fun checkFullyConsumed() {
        val token = peek()
        if (token != null) {
            throwParseError(token.from, "Trailing characters in JSON")
        }
    }

    companion object {
        private val PATTERN_NUMBER: Regex =
            Regex("""(-?(?:0|[1-9]\d*)(\.\d+)?(?:e[+-]?\d+)?)([bslfd])?""", RegexOption.IGNORE_CASE)

        private fun isStringTokenChar(c: Char): Boolean = when (c) { // from gson JsonReader#isLiteral
            '/', '\\', ';', '#', '=', '{', '}', '[', ']', ':', ',', ' ', '\t', '\u000c', '\r', '\n' -> false
            else -> true
        }

        @Throws(JsonParseException::class)
        fun parse(text: String): TJson {
            val parser = TypedJsonParser(text)
            return parser.takeValue().also { parser.checkFullyConsumed() }
        }

        @Throws(JsonParseException::class)
        fun parseObject(text: String): TJson.Object {
            val parser = TypedJsonParser(text)
            return parser.takeObject().also { parser.checkFullyConsumed() }
        }

        @Throws(JsonParseException::class)
        fun parseArray(text: String): TJson.Array {
            val parser = TypedJsonParser(text)
            return parser.takeArray().also { parser.checkFullyConsumed() }
        }
    }

    private class PosToken(val from: Int, val to: Int, val token: Token)

    private sealed interface Token {
        object OpenObject : Token
        object CloseObject : Token
        object OpenArray : Token
        object CloseArray : Token
        object KeySep : Token
        object ElemSep : Token
        object NullLiteral : Token
        object TrueLiteral : Token
        object FalseLiteral : Token
        data class StringLiteral(val value: String) : Token
        data class ByteLiteral(val value: Byte) : Token
        data class ShortLiteral(val value: Short) : Token
        data class IntLiteral(val value: Int) : Token
        data class LongLiteral(val value: Long) : Token
        data class FloatLiteral(val value: Float) : Token
        data class DoubleLiteral(val value: Double) : Token
    }
}

class JsonParseException(val line: Int, val col: Int, val parseError: String) : Exception("$parseError (at $line:$col)")
