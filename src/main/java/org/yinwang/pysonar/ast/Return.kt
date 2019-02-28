package org.yinwang.pysonar.ast

class Return(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.RETURN, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<Return:$value>"
    }

}
