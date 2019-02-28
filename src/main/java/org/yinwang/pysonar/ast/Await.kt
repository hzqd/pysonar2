package org.yinwang.pysonar.ast

class Await(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.AWAIT, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<Await:$value>"
    }

}
