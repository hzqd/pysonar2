package org.yinwang.pysonar.ast

class Tuple(elts: List<Node>, file: String, start: Int, end: Int, line: Int, col: Int) : Sequence(NodeType.TUPLE, elts, file, start, end, line, col) {

    override fun toString(): String {
        return "<Tuple:$start:$elts>"
    }

    override fun toDisplay(): String {
        val sb = StringBuilder()
        sb.append("(")

        var idx = 0
        for (n in elts) {
            if (idx != 0) {
                sb.append(", ")
            }
            idx++
            sb.append(n.toDisplay())
        }

        sb.append(")")
        return sb.toString()
    }

}
