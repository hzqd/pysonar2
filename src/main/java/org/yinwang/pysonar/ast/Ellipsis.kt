package org.yinwang.pysonar.ast

class Ellipsis(file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.ELLIPSIS, file, start, end, line, col) {

    override fun toString(): String {
        return "..."
    }

}
