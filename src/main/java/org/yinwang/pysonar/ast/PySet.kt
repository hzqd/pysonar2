package org.yinwang.pysonar.ast

class PySet(elts: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Sequence(NodeType.PYSET, elts, file, start, end, line, col) {

    override fun toString(): String {
        return "<List:$start:$elts>"
    }

}
