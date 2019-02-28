package org.yinwang.pysonar.ast

class Repr(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.REPR, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<Repr:$value>"
    }

}
