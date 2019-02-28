package org.yinwang.pysonar.ast

/**
 * Expression statement.
 */
class Expr(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.EXPR, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<Expr:$value>"
    }

}
