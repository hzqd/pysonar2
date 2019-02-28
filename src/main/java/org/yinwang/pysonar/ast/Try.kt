package org.yinwang.pysonar.ast

class Try(var handlers: List<Handler>, var body: Block, var orelse: Block, var finalbody: Block,
          file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.TRY, file, start, end, line, col) {

    init {
        addChildren(handlers)
        addChildren(body, orelse)
    }

    override fun toString(): String {
        return "<Try:$handlers:$body:$orelse>"
    }

}
