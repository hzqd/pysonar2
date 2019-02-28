package org.yinwang.pysonar.ast

class PyList(elts: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Sequence(NodeType.PYLIST, elts, file, start, end, line, col) {

    override fun toString(): String {
        return "<List:$start:$elts>"
    }

}
