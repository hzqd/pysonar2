package org.yinwang.pysonar.ast

class Attribute(var target: Node, var attr: Name, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.ATTRIBUTE, file, start, end, line, col) {

    init {
        addChildren(target, attr)
    }

    override fun toString(): String {
        return "<Attribute:" + line + ":" + col + ":" + target + "." + attr.id + ">"
    }
}
