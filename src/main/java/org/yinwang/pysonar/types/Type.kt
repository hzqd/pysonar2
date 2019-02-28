package org.yinwang.pysonar.types

import org.yinwang.pysonar.State
import org.yinwang.pysonar.TypeStack
import org.yinwang.pysonar.`$`

import java.util.HashMap
import java.util.HashSet


abstract class Type {

    var table = State(null, State.StateType.SCOPE)
    var file: String? = null


    val isNumType: Boolean
        get() = this === Types.IntInstance || this === Types.FloatInstance


    val isUnknownType: Boolean
        get() = this === Types.UNKNOWN

    override fun equals(other: Any?): Boolean {
        return typeEquals(other)
    }

    abstract fun typeEquals(other: Any?): Boolean

    fun setTable(table: State) {
        this.table = table
    }


    fun setFile(file: String) {
        this.file = file
    }


    fun asModuleType(): ModuleType {
        if (this is UnionType) {
            for (t in this.types) {
                if (t is ModuleType) {
                    return t.asModuleType()
                }
            }
            `$`.die("Not containing a ModuleType")
            // can't get here, just to make the @NotNull annotation happy
            return ModuleType(null!!, null, null!!)
        } else if (this is ModuleType) {
            return this
        } else {
            `$`.die("Not a ModuleType")
            // can't get here, just to make the @NotNull annotation happy
            return ModuleType(null!!, null, null!!)
        }
    }


    /**
     * Internal class to support printing in the presence of type-graph cycles.
     */
    protected inner class CyclicTypeRecorder {
        internal var count = 0
        private val elements = HashMap<Type, Int>()
        private val used = HashSet<Type>()


        fun push(t: Type): Int? {
            count += 1
            elements[t] = count
            return count
        }


        fun pop(t: Type) {
            elements.remove(t)
            used.remove(t)
        }


        fun visit(t: Type): Int? {
            val i = elements[t]
            if (i != null) {
                used.add(t)
            }
            return i
        }


        fun isUsed(t: Type): Boolean {
            return used.contains(t)
        }
    }


    abstract fun printType(ctr: CyclicTypeRecorder): String


    override fun toString(): String {
        return printType(CyclicTypeRecorder())
    }

    companion object {
        protected var typeStack = TypeStack()
    }

}
