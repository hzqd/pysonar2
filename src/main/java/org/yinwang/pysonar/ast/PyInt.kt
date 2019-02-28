package org.yinwang.pysonar.ast

import java.math.BigInteger

class PyInt(s: String, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.PYINT, file, start, end, line, col) {

    var value: BigInteger

    init {
        var s = s

        s = s.replace("_".toRegex(), "")
        var sign = 1

        if (s.startsWith("+")) {
            s = s.substring(1)
        } else if (s.startsWith("-")) {
            s = s.substring(1)
            sign = -1
        }

        val base: Int
        if (s.startsWith("0b")) {
            base = 2
            s = s.substring(2)
        } else if (s.startsWith("0x")) {
            base = 16
            s = s.substring(2)
        } else if (s.startsWith("x")) {
            base = 16
            s = s.substring(1)
        } else if (s.startsWith("0o")) {
            base = 8
            s = s.substring(2)
        } else if (s.startsWith("0") && s.length >= 2) {
            base = 8
            s = s.substring(1)
        } else {
            base = 10
        }

        value = BigInteger(s, base)
        if (sign == -1) {
            value = value.negate()
        }
    }

    override fun toString(): String {
        return "(int:$value)"
    }

}
