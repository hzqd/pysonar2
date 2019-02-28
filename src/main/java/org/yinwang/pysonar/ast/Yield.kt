package org.yinwang.pysonar.ast

class Yield(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.YIELD, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<Yield:$start:$value>"
    }

}
