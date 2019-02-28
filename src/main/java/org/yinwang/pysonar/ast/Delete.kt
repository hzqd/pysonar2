package org.yinwang.pysonar.ast

class Delete(var targets: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.DELETE, file, start, end, line, col) {

    init {
        addChildren(targets)
    }

    override fun toString(): String {
        return "<Delete:$targets>"
    }

}
