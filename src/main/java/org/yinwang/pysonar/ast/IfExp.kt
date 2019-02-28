package org.yinwang.pysonar.ast

class IfExp(var test: Node, var body: Node, var orelse: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.IFEXP, file, start, end, line, col) {

    init {
        addChildren(test, body, orelse)
    }

    override fun toString(): String {
        return "<IfExp:$start:$test:$body:$orelse>"
    }

}
