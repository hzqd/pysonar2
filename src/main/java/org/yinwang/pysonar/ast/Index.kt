package org.yinwang.pysonar.ast

class Index(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.INDEX, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<Index:$value>"
    }

}
