package org.yinwang.pysonar.ast

class Print(var dest: Node, var values: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.PRINT, file, start, end, line, col) {

    init {
        addChildren(dest)
        addChildren(values)
    }

    override fun toString(): String {
        return "<Print:$values>"
    }

}
