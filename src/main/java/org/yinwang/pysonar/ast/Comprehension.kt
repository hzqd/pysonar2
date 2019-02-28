package org.yinwang.pysonar.ast

class Comprehension(var target: Node, var iter: Node, var ifs: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.COMPREHENSION, file, start, end, line, col) {

    init {
        addChildren(target, iter)
        addChildren(ifs)
    }

    override fun toString(): String {
        return "<Comprehension:$start:$target:$iter:$ifs>"
    }

}
