package org.yinwang.pysonar.ast

class Pass(file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.PASS, file, start, end, line, col) {

    override fun toString(): String {
        return "<Pass>"
    }

}
