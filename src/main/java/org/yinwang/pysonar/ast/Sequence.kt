package org.yinwang.pysonar.ast

abstract class Sequence(nodeType: NodeType, var elts: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(nodeType, file, start, end, line, col) {

    init {
        addChildren(elts)
    }

}
