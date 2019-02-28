package org.yinwang.pysonar.ast

class While(var test: Node, var body: Node, var orelse: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.WHILE, file, start, end, line, col) {

    init {
        addChildren(test, body, orelse)
    }

    override fun toString(): String {
        return "<While:$test:$body:$orelse:$start>"
    }

}
