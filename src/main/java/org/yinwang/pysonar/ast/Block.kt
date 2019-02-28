package org.yinwang.pysonar.ast

class Block(var seq: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.BLOCK, file, start, end, line, col) {

    init {
        addChildren(seq)
    }

    override fun toString(): String {
        return "(block:$seq)"
    }

}
