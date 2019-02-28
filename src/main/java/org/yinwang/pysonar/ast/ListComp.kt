package org.yinwang.pysonar.ast

class ListComp(var elt: Node, var generators: List<Comprehension>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.LISTCOMP, file, start, end, line, col) {

    init {
        addChildren(elt)
        addChildren(generators)
    }

    override fun toString(): String {
        return "<NListComp:$start:$elt>"
    }

}
