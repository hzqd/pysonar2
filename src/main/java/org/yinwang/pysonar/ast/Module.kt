package org.yinwang.pysonar.ast

import org.yinwang.pysonar.`$`

class Module(var body: Block, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.MODULE, file, start, end, line, col) {

    init {
        this.name = `$`.moduleName(file)
        addChildren(this.body)
    }

    override fun toString(): String {
        return "(module:$file)"
    }

}
