package org.yinwang.pysonar.ast

class YieldFrom(var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.YIELDFROM, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "<YieldFrom:$start:$value>"
    }

}
