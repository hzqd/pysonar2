package org.yinwang.pysonar.ast

class Unsupported(file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.UNSUPPORTED, file, start, end, line, col) {

    override fun toString(): String {
        return "(unsupported)"
    }
}
