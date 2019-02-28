package org.yinwang.pysonar.types

import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.TypeStack

import java.util.ArrayList
import java.util.Collections


class TupleType() : Type() {

    var eltTypes: MutableList<Type>


    init {
        this.eltTypes = ArrayList()
        table.addSuper(Analyzer.self.builtins.BaseTuple.table)
        table.setPath(Analyzer.self.builtins.BaseTuple.table.path)
    }


    constructor(eltTypes: MutableList<Type>) : this() {
        this.eltTypes = eltTypes
    }


    constructor(elt0: Type) : this() {
        this.eltTypes.add(elt0)
    }


    constructor(elt0: Type, elt1: Type) : this() {
        this.eltTypes.add(elt0)
        this.eltTypes.add(elt1)
    }


    constructor(vararg types: Type) : this() {
        Collections.addAll(this.eltTypes, *types)
    }


    fun setElementTypes(eltTypes: MutableList<Type>) {
        this.eltTypes = eltTypes
    }


    fun add(elt: Type) {
        eltTypes.add(elt)
    }


    operator fun get(i: Int): Type {
        return eltTypes[i]
    }

    fun size(): Int {
        return eltTypes.size
    }

    fun toListType(): ListType {
        val t = ListType()
        for (e in eltTypes) {
            t.add(e)
        }
        return t
    }


    override fun typeEquals(other: Any?): Boolean {
        if (Type.typeStack.contains(this, other)) {
            return true
        } else if (other is TupleType) {
            val types1 = eltTypes
            val types2 = other.eltTypes

            if (types1.size == types2.size) {
                Type.typeStack.push(this, other)
                for (i in types1.indices) {
                    if (!types1[i].typeEquals(types2[i])) {
                        Type.typeStack.pop(this, other)
                        return false
                    }
                }
                Type.typeStack.pop(this, other)
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }


    override fun hashCode(): Int {
        return "TupleType".hashCode()
    }


    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        val sb = StringBuilder()

        val num = ctr.visit(this)
        if (num != null) {
            sb.append("#").append(num)
        } else {
            val newNum = ctr.push(this)!!
            var first = true
            if (eltTypes.size != 1) {
                sb.append("(")
            }

            for (t in eltTypes) {
                if (!first) {
                    sb.append(", ")
                }
                sb.append(t.printType(ctr))
                first = false
            }

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(":")
            }

            if (eltTypes.size != 1) {
                sb.append(")")
            }
            ctr.pop(this)
        }
        return sb.toString()
    }

}
