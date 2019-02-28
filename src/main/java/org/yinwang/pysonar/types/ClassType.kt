package org.yinwang.pysonar.types

import org.yinwang.pysonar.State
import org.yinwang.pysonar.ast.Node
import org.yinwang.pysonar.visitor.TypeInferencer

import java.util.ArrayList

class ClassType(var name: String, parent: State?) : Type() {
    var superclass: Type
    private var instance: InstanceType? = null

    init {
        this.setTable(State(parent, State.StateType.CLASS))
        table.setType(this)
        if (parent != null) {
            table.setPath(parent.extendPath(name))
        } else {
            table.setPath(name)
        }
    }


    constructor(name: String, parent: State, superClass: Type?) : this(name, parent) {
        if (superClass != null) {
            addSuper(superClass)
        }
    }


    fun setName(name: String) {
        this.name = name
    }


    fun addSuper(superclass: Type) {
        this.superclass = superclass
        table.addSuper(superclass.table)
    }

    fun getInstance(): InstanceType {
        if (instance == null) {
            instance = InstanceType(this)
        }
        return instance
    }

    fun getInstance(args: List<Type>?, inferencer: TypeInferencer, call: Node): InstanceType {
        if (instance == null) {
            val initArgs = args ?: ArrayList()
            instance = InstanceType(this, initArgs, inferencer, call)
        }
        return instance
    }

    fun setInstance(instance: InstanceType) {
        this.instance = instance
    }

    override fun typeEquals(other: Any?): Boolean {
        return this === other
    }

    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        return "<$name>"
    }
}
