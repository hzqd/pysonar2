package org.yinwang.pysonar.ast

class Exec(var body: Node, var globals: Node, var locals: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.EXEC, file, start, end, line, col) {

    init {
        addChildren(body, globals, locals)
    }

    override fun toString(): String {
        return "<Exec:$start:$end>"
    }

}
