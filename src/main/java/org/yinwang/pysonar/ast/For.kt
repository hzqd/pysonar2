package org.yinwang.pysonar.ast

class For(var target: Node, var iter: Node, var body: Block, var orelse: Block, isAsync: Boolean,
          file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.FOR, file, start, end, line, col) {
    var isAsync = false

    init {
        this.isAsync = isAsync
        addChildren(target, iter, body, orelse)
    }

    override fun toString(): String {
        return "<For:$target:$iter:$body:$orelse>"
    }

}
