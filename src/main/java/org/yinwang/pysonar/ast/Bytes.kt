package org.yinwang.pysonar.ast

class Bytes(value: Any, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.BYTES, file, start, end, line, col) {

    var value: Any

    init {
        this.value = value.toString()
    }

    override fun toString(): String {
        return "(bytes: $value)"
    }

}
