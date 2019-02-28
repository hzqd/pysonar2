package org.yinwang.pysonar.ast

class Starred(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.STARRED, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<starred:$value>"
    }

}
