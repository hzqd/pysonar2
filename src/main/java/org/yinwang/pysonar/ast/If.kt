package org.yinwang.pysonar.ast

class If(var test: Node, var body: Node, var orelse: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.IF, file, start, end, line, col) {

    init {
        addChildren(test, body, orelse)
    }

    override fun toString(): String {
        return "<If:$start:$test:$body:$orelse>"
    }

}
