package org.yinwang.pysonar.types

import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.State
import org.yinwang.pysonar.`$`

class ModuleType(var name: String, file: String?, parent: State) : Type() {
    var qname: String? = null


    init {
        this.file = file  // null for builtin modules
        if (file != null) {
            // This will return null iff specified file is not prefixed by
            // any path in the module search path -- i.e., the caller asked
            // the analyzer to load a file not in the search path.
            qname = `$`.moduleQname(file)
        }
        if (qname == null) {
            qname = name
        }
        setTable(State(parent, State.StateType.MODULE))
        table.setPath(qname!!)
        table.setType(this)

        // null during bootstrapping of built-in types
        if (Analyzer.self.builtins != null) {
            table.addSuper(Analyzer.self.builtins.BaseModule.table)
        }
    }


    fun setName(name: String) {
        this.name = name
    }


    override fun hashCode(): Int {
        return "ModuleType".hashCode()
    }


    override fun typeEquals(other: Any?): Boolean {
        if (other is ModuleType) {
            val co = other as ModuleType?
            if (file != null) {
                return file == co!!.file
            }
        }
        return this === other
    }


    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        return name
    }
}
