package org.yinwang.pysonar.ast

class Assign(var target: Node, var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.ASSIGN, file, start, end, line, col) {

    init {
        addChildren(target)
        addChildren(value)
    }

    override fun toString(): String {
        return "($target = $value)"
    }
}
