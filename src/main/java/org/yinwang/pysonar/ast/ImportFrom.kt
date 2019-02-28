package org.yinwang.pysonar.ast

import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.Binding
import org.yinwang.pysonar.State
import org.yinwang.pysonar.types.ListType
import org.yinwang.pysonar.types.Type

import java.util.ArrayList
import kotlin.collections.Map.Entry

class ImportFrom(var module: List<Name>, var names: List<Alias>, var level: Int, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.IMPORTFROM, file, start, end, line, col) {

    val isImportStar: Boolean
        get() = names.size == 1 && "*" == names[0].name[0].id

    init {
        addChildren(names)
    }

    fun importStar(s: State, mt: Type?) {
        if (mt == null || mt.file == null) {
            return
        }

        val node = Analyzer.self.getAstForFile(mt.file) ?: return

        val names = ArrayList<String>()
        val allType = mt.table.lookupType("__all__")

        if (allType != null && allType is ListType) {
            val lt = allType as ListType?

            for (o in lt!!.values) {
                if (o is String) {
                    names.add(o)
                }
            }
        }

        if (!names.isEmpty()) {
            var start = this.start
            var col = this.col

            for (name in names) {
                val b = mt.table.lookupLocal(name)
                if (b != null) {
                    s.update(name, b)
                } else {
                    val m2 = ArrayList(module)
                    val fakeName = Name(name, this.file, start, start + name.length, this.line, col)
                    m2.add(fakeName)
                    val type = Analyzer.self.loadModule(m2, s)
                    if (type != null) {
                        start += name.length
                        col += name.length
                        s.insert(name, fakeName, type, Binding.Kind.VARIABLE)
                    }
                }
            }
        } else {
            // Fall back to importing all names not starting with "_".
            for ((key, value) in mt.table.entrySet()) {
                if (!key.startsWith("_")) {
                    s.update(key, value)
                }
            }
        }
    }

    override fun toString(): String {
        return "<FromImport:$module:$names>"
    }

}
