package org.yinwang.pysonar.ast

class PyFloat(s: String, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.PYFLOAT, file, start, end, line, col) {

    var value: Double = 0.toDouble()

    init {
        var s = s
        s = s.replace("_".toRegex(), "")
        if (s == "inf") {
            this.value = java.lang.Double.POSITIVE_INFINITY
        } else if (s == "-inf") {
            this.value = java.lang.Double.NEGATIVE_INFINITY
        } else {
            this.value = java.lang.Double.parseDouble(s)
        }
    }

    override fun toString(): String {
        return "(float:$value)"
    }

}
