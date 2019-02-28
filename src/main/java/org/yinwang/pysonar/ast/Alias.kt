package org.yinwang.pysonar.ast

class Alias(var name: List<Name>, var asname: Name, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.ALIAS, file, start, end, line, col) {

    init {
        addChildren(name)
        addChildren(asname)
    }

    override fun toString(): String {
        return "<Alias:$name as $asname>"
    }

}
