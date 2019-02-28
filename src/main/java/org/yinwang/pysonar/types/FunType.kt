package org.yinwang.pysonar.types

import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.State
import org.yinwang.pysonar.ast.FunctionDef
import org.yinwang.pysonar.hash.MyHashMap

import java.util.HashMap
import java.util.HashSet


class FunType : Type {

    var arrows: MutableMap<Type, Type> = MyHashMap()
    var func: FunctionDef
    var cls: ClassType? = null
    var env: State
    var defaultTypes: List<Type>       // types for default parameters (evaluated at def time)

    val returnType: Type
        get() = if (!arrows.isEmpty()) {
            arrows.values.iterator().next()
        } else {
            Types.UNKNOWN
        }


    constructor() {}


    constructor(func: FunctionDef, env: State) {
        this.func = func
        this.env = env
    }


    constructor(from: Type, to: Type) {
        addMapping(from, to)
        table.addSuper(Analyzer.self.builtins.BaseFunction.table)
        table.setPath(Analyzer.self.builtins.BaseFunction.table.path)
    }


    fun addMapping(from: Type, to: Type) {
        if (arrows.size < MAX_ARROWS) {
            arrows[from] = to
        }
    }

    fun removeMapping(from: Type) {
        arrows.remove(from)
    }

    fun getMapping(from: Type): Type? {
        return arrows[from]
    }

    fun oversized(): Boolean {
        return arrows.size >= MAX_ARROWS
    }


    fun setCls(cls: ClassType) {
        this.cls = cls
    }


    fun setDefaultTypes(defaultTypes: List<Type>) {
        this.defaultTypes = defaultTypes
    }


    override fun typeEquals(other: Any?): Boolean {
        if (other is FunType) {
            val fo = other as FunType?
            return fo!!.table.path == table.path || this === other
        } else {
            return false
        }
    }


    override fun hashCode(): Int {
        return "FunType".hashCode()
    }


    private fun subsumed(type1: Type, type2: Type): Boolean {
        return subsumedInner(type1, type2)
    }


    private fun subsumedInner(type1: Type, type2: Type): Boolean {
        if (Type.typeStack.contains(type1, type2)) {
            return true
        }

        if (type1.isUnknownType || type1 === Types.NoneInstance || type1 == type2) {
            return true
        }

        if (type1 is TupleType && type2 is TupleType) {
            val elems1 = type1.eltTypes
            val elems2 = type2.eltTypes

            if (elems1.size == elems2.size) {
                for (i in elems1.indices) {
                    if (!subsumedInner(elems1[i], elems2[i])) {
                        return false
                    }
                }
            }

            return true
        }

        return if (type1 is ListType && type2 is ListType) {
            subsumedInner(type1.toTupleType(), type2.toTupleType())
        } else false

    }


    private fun compressArrows(arrows: Map<Type, Type>): Map<Type, Type> {
        val ret = HashMap<Type, Type>()

        for (e1 in arrows.entries) {
            var subsumed = false

            for (e2 in arrows.entries) {
                if (e1 !== e2 && subsumed(e1.key, e2.key)) {
                    subsumed = true
                    break
                }
            }

            if (!subsumed) {
                ret[e1.key] = e1.value
            }
        }

        return ret
    }


    // If the self type is set, use the self type in the display
    // This is for display purpose only, it may not be logically
    //   correct wrt some pathological programs
    private fun simplifySelf(from: TupleType): TupleType {
        val simplified = TupleType()
        if (from.eltTypes.size > 0) {
            if (cls != null) {
                simplified.add(cls!!.instance)
            } else {
                simplified.add(from.get(0))
            }
        }

        for (i in 1 until from.eltTypes.size) {
            simplified.add(from.get(i))
        }
        return simplified
    }


    override fun printType(ctr: Type.CyclicTypeRecorder): String {

        if (arrows.isEmpty()) {
            return "? -> ?"
        }

        val sb = StringBuilder()

        val num = ctr.visit(this)
        if (num != null) {
            sb.append("#").append(num)
        } else {
            val newNum = ctr.push(this)!!

            var i = 0
            val seen = HashSet<String>()

            for ((from, value) in arrows) {
                val `as` = from.printType(ctr) + " -> " + value.printType(ctr)

                if (!seen.contains(`as`)) {
                    if (i != 0) {
                        if (Analyzer.self.multilineFunType) {
                            sb.append("\n/ ")
                        } else {
                            sb.append(" / ")
                        }
                    }

                    sb.append(`as`)
                    seen.add(`as`)
                }

                i++
            }

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(": ")
            }
            ctr.pop(this)
        }
        return sb.toString()
    }

    companion object {

        private val MAX_ARROWS = 10
    }
}
