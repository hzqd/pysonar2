package org.yinwang.pysonar.ast

class With(var items: List<Withitem>, var body: Block, file: String, isAsync: Boolean, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.WITH, file, start, end, line, col) {
    var isAsync = false

    init {
        this.isAsync = isAsync
        addChildren(items)
        addChildren(body)
    }

    override fun toString(): String {
        return "<With:$items:$body>"
    }

}
