package org.yinwang.pysonar.ast

import org.yinwang.pysonar.Binding
import org.yinwang.pysonar.Builtins
import org.yinwang.pysonar.State
import org.yinwang.pysonar.types.Type

class ClassDef(var name: Name, var bases: List<Node>, var body: Node, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.CLASSDEF, file, start, end, line, col) {

    init {
        addChildren(name, this.body)
        addChildren(bases)
    }

    fun addSpecialAttribute(s: State, name: String, proptype: Type) {
        val b = Binding(name, Builtins.newTutUrl("classes.html"), proptype, Binding.Kind.ATTRIBUTE)
        s.update(name, b)
        b.markSynthetic()
        b.markStatic()

    }

    override fun toString(): String {
        return "(class:" + name.id + ":" + start + ")"
    }

}
