package org.yinwang.pysonar.ast

class SetComp(var elt: Node, var generators: List<Comprehension>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.SETCOMP, file, start, end, line, col) {

    init {
        addChildren(elt)
        addChildren(generators)
    }

    override fun toString(): String {
        return "<NSetComp:$start:$elt>"
    }

}
