package org.yinwang.pysonar.ast

class Global(var names: List<Name>, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.GLOBAL, file, start, end, line, col) {

    init {
        addChildren(names)
    }

    override fun toString(): String {
        return "<Global:$names>"
    }

}
