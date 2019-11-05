package org.yinwang.pysonar.types

import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.stream.Collectors

class UnionType() : Type() {

    var types: MutableSet<Type>


    val isEmpty: Boolean
        get() = types.isEmpty()


    init {
        this.types = HashSet()
    }


    constructor(vararg initialTypes: Type) : this() {
        for (nt in initialTypes) {
            addType(nt)
        }
    }


    fun setTypes(types: MutableSet<Type>) {
        this.types = types
    }


    fun addType(t: Type) {
        if (t is UnionType) {
            types.addAll(t.types)
        } else {
            types.add(t)
        }
    }


    operator fun contains(t: Type): Boolean {
        return types.contains(t)
    }

    fun firstUseful(): Type? {
        for (type in types) {
            if (!type.isUnknownType && type !== Types.NoneInstance) {
                return type
            }
        }
        return null
    }


    override fun typeEquals(other: Any?): Boolean {
        if (Type.typeStack.contains(this, other)) {
            return true
        } else if (other is UnionType) {
            val types1 = types
            val types2 = other.types
            if (types1.size != types2.size) {
                return false
            } else {
                for (t in types2) {
                    if (!types1.contains(t)) {
                        return false
                    }
                }
                for (t in types1) {
                    if (!types2.contains(t)) {
                        return false
                    }
                }
                return true
            }
        } else {
            return false
        }
    }


    override fun hashCode(): Int {
        return "UnionType".hashCode()
    }


    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        val sb = StringBuilder()

        val num = ctr.visit(this)
        if (num != null) {
            sb.append("#").append(num)
        } else {
            val newNum = ctr.push(this)!!
            val typeStrings = types.stream().map { x -> x.printType(ctr) }.collect<List<String>, Any>(Collectors.toList())
            Collections.sort(typeStrings)
            sb.append("{")
            sb.append(typeStrings.joinToString(" | "))

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(":")
            }

            sb.append("}")
            ctr.pop(this)
        }

        return sb.toString()
    }

    companion object {


        /**
         * Returns true if t1 == t2 or t1 is a union type that contains t2.
         */
        fun contains(t1: Type, t2: Type): Boolean {
            return if (t1 is UnionType) {
                t1.contains(t2)
            } else {
                t1 == t2
            }
        }


        fun remove(t1: Type, t2: Type): Type {
            if (t1 is UnionType) {
                val types = HashSet(t1.types)
                types.remove(t2)
                return UnionType.newUnion(types)
            } else return if (t1 !== Types.CONT && t1 === t2) {
                Types.UNKNOWN
            } else {
                t1
            }
        }


        fun newUnion(types: Collection<Type>): Type {
            var t = Types.UNKNOWN
            for (nt in types) {
                t = union(t, nt)
            }
            return t
        }


        // take a union of two types
        // with preference: other > None > Cont > unknown
        fun union(u: Type, v: Type): Type {
            return if (u == v) {
                u
            } else if (u !== Types.UNKNOWN && v === Types.UNKNOWN) {
                u
            } else if (v !== Types.UNKNOWN && u === Types.UNKNOWN) {
                v
            } else if (u !== Types.NoneInstance && v === Types.NoneInstance) {
                u
            } else if (v !== Types.NoneInstance && u === Types.NoneInstance) {
                v
            } else if (u is TupleType && v is TupleType &&
                    u.size() == v.size()) {
                union(u, v)
            } else {
                UnionType(u, v)
            }
        }

        fun union(u: TupleType, v: TupleType): Type {
            val types = ArrayList<Type>()
            for (i in 0 until u.size()) {
                types.add(union(u.get(i), v.get(i)))
            }
            return TupleType(types)
        }

        fun union(types: Collection<Type>): Type {
            var result = Types.UNKNOWN
            for (type in types) {
                result = UnionType.union(result, type)
            }
            return result
        }

        fun union(vararg types: Type): Type {
            return union(*types)
        }
    }

}
