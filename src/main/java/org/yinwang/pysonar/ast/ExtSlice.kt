package org.yinwang.pysonar.ast

class ExtSlice(var dims: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.EXTSLICE, file, start, end, line, col) {

    init {
        addChildren(dims)
    }

    override fun toString(): String {
        return "<ExtSlice:$dims>"
    }

}
