package org.yinwang.pysonar.ast

class BinOp(var op: Op, var left: Node, var right: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.BINOP, file, start, end, line, col) {

    init {
        addChildren(left, right)
    }

    override fun toString(): String {
        return "($left $op $right)"
    }

}
