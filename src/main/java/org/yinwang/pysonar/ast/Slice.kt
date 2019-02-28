package org.yinwang.pysonar.ast

class Slice(var lower: Node, var step: Node, var upper: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.SLICE, file, start, end, line, col) {

    init {
        addChildren(lower, step, upper)
    }

    override fun toString(): String {
        return "<Slice:$lower:$step:$upper>"
    }

}
