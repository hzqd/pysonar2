package org.yinwang.pysonar.ast

class Handler(var exceptions: List<Node>, var binder: Node, var body: Block, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.HANDLER, file, start, end, line, col) {

    init {
        addChildren(binder, body)
        addChildren(exceptions)
    }

    override fun toString(): String {
        return "(handler:$start:$exceptions:$binder)"
    }

}
