package org.yinwang.pysonar.ast

class Raise(var exceptionType: Node, var inst: Node, var traceback: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.RAISE, file, start, end, line, col) {

    init {
        addChildren(exceptionType, inst, traceback)
    }

    override fun toString(): String {
        return "<Raise:$traceback:$exceptionType>"
    }

}
