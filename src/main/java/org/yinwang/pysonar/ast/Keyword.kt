package org.yinwang.pysonar.ast

/**
 * Represents a keyword argument (name=value) in a function call.
 */
class Keyword(var arg: String, var value: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.KEYWORD, file, start, end, line, col) {

    init {
        addChildren(value)
    }

    override fun toString(): String {
        return "(keyword:$arg:$value)"
    }

    override fun toDisplay(): String {
        return arg
    }

}
