package org.yinwang.pysonar.ast

class PyComplex(var real: Double, var imag: Double, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.PYCOMPLEX, file, start, end, line, col) {

    override fun toString(): String {
        return "(" + real + "+" + imag + "j)"
    }

}
