package org.yinwang.pysonar.types

import org.yinwang.pysonar.Analyzer

import java.util.ArrayList


class ListType @JvmOverloads constructor(var eltType: Type = Types.UNKNOWN) : Type() {
    var positional: MutableList<Type> = ArrayList()
    var values: MutableList<Any> = ArrayList()


    init {
        table.addSuper(Analyzer.self.builtins.BaseList.table)
        table.setPath(Analyzer.self.builtins.BaseList.table.path)
    }


    fun setElementType(eltType: Type) {
        this.eltType = eltType
    }


    fun add(another: Type) {
        eltType = UnionType.union(eltType, another)
        positional.add(another)
    }


    fun addValue(v: Any) {
        values.add(v)
    }


    operator fun get(i: Int): Type {
        return positional[i]
    }


    fun toTupleType(n: Int): TupleType {
        val ret = TupleType()
        for (i in 0 until n) {
            ret.add(eltType)
        }
        return ret
    }


    fun toTupleType(): TupleType {
        return TupleType(positional)
    }


    override fun typeEquals(other: Any?): Boolean {
        if (Type.typeStack.contains(this, other)) {
            return true
        } else if (other is ListType) {
            val co = other as ListType?
            Type.typeStack.push(this, other)
            val result = co!!.eltType.typeEquals(eltType)
            Type.typeStack.pop(this, other)
            return result
        } else {
            return false
        }
    }


    override fun hashCode(): Int {
        return "ListType".hashCode()
    }


    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        val sb = StringBuilder()

        val num = ctr.visit(this)
        if (num != null) {
            sb.append("#").append(num)
        } else {
            ctr.push(this)
            sb.append("[")
            sb.append(eltType.printType(ctr))
            sb.append("]")
            ctr.pop(this)
        }

        return sb.toString()
    }

}
