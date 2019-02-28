package org.yinwang.pysonar.ast

class Assert(var test: Node, var msg: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.ASSERT, file, start, end, line, col) {

    init {
        addChildren(test, msg)
    }

    override fun toString(): String {
        return "<Assert:$test:$msg>"
    }

}
