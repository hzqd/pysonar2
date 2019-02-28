package org.yinwang.pysonar.ast

class Subscript(var value: Node, var slice: Node?  // an NIndex or NSlice
                , file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.SUBSCRIPT, file, start, end, line, col) {

    init {
        addChildren(value, slice)
    }

    override fun toString(): String {
        return "<Subscript:$value:$slice>"
    }

}
