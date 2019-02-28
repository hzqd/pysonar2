package org.yinwang.pysonar.ast

class Continue(file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.CONTINUE, file, start, end, line, col) {

    override fun toString(): String {
        return "(continue)"
    }

}
