package org.yinwang.pysonar.ast

class Import(var names: List<Alias>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.IMPORT, file, start, end, line, col) {

    init {
        addChildren(names)
    }

    override fun toString(): String {
        return "<Import:$names>"
    }

}
