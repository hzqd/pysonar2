package org.yinwang.pysonar.ast

class UnaryOp(var op: Op, var operand: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.UNARYOP, file, start, end, line, col) {

    init {
        addChildren(operand)
    }

    override fun toString(): String {
        return "($op $operand)"
    }

}
