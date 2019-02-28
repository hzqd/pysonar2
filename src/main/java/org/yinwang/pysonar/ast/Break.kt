package org.yinwang.pysonar.ast

class Break(file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.BREAK, file, start, end, line, col) {

    override fun toString(): String {
        return "(break)"
    }
}
