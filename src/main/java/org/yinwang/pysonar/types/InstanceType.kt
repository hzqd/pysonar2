package org.yinwang.pysonar.types

import org.yinwang.pysonar.State
import org.yinwang.pysonar.ast.Node
import org.yinwang.pysonar.visitor.TypeInferencer


class InstanceType(var classType: Type) : Type() {


    init {
        table.setStateType(State.StateType.INSTANCE)
        table.addSuper(classType.table)
        table.setPath(classType.table.path)
    }

    constructor(c: Type, args: List<Type>, inferencer: TypeInferencer, call: Node) : this(c) {

        // call constructor
        val initFunc = table.lookupAttrType("__init__")
        if (initFunc != null &&
                initFunc is FunType &&
                initFunc.func != null) {
            inferencer.apply((initFunc as FunType?)!!, this, args, null, null, null, call)
        }

        if (classType is ClassType) {
            (classType as ClassType).instance = this
        }
    }


    override fun typeEquals(other: Any?): Boolean {
        return if (other is InstanceType) {
            classType.typeEquals(other.classType)
        } else false
    }


    override fun hashCode(): Int {
        return classType.hashCode()
    }


    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        return (classType as ClassType).name
    }
}
