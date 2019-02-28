package org.yinwang.pysonar.ast

class DictComp(var key: Node, var value: Node, var generators: List<Comprehension>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.DICTCOMP, file, start, end, line, col) {

    init {
        addChildren(key)
        addChildren(generators)
    }

    override fun toString(): String {
        return "<DictComp:$start:$key>"
    }

}
