package org.yinwang.pysonar.ast

class Str(value: Any, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.STR, file, start, end, line, col) {

    var value: String

    init {
        this.value = value.toString()
    }

    override fun toString(): String {
        val summary: String
        if (value.length > 10) {
            summary = value.substring(0, 10)
        } else {
            summary = value
        }
        return "'$summary'"
    }

}
