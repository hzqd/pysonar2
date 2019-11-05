package org.yinwang.pysonar

import org.yinwang.pysonar.ast.*
import org.yinwang.pysonar.types.ModuleType
import org.yinwang.pysonar.types.Type
import org.yinwang.pysonar.types.UnionType

import java.util.LinkedHashSet


class Binding(id: String, var node: Node, var type: Type       // inferred type
              , var kind: Kind        // name usage context
) : Comparable<Any> {


    var isStatic = false
        private set         // static fields/methods
    var isSynthetic = false
        private set      // auto-generated bindings
    val isBuiltin = false        // not from a source file

    var name: String     // unqualified name
    var qname: String    // qualified name

    var refs: MutableSet<Node> = LinkedHashSet(1)

    // fields from Def
    var start = -1
    var end = -1
    var line = -1
    var col = -1
    var bodyStart = -1
    var bodyEnd = -1

    var fileOrUrl: String? = null


    val docstring: Str?
        get() {
            val parent = node.parent
            return if (parent is FunctionDef && parent.name === node || parent is ClassDef && parent.name === node) {
                parent.docString
            } else {
                node.docString
            }
        }


    val firstFile: String
        get() {
            val bt = type
            if (bt is ModuleType) {
                val file = bt.asModuleType().file
                return file ?: "<built-in module>"
            }

            val file = file
            return file ?: "<built-in binding>"

        }


    val file: String?
        get() = if (isURL) null else fileOrUrl


    val url: String?
        get() = if (isURL) fileOrUrl else null


    val isURL: Boolean
        get() = fileOrUrl != null && fileOrUrl!!.startsWith("http://")

    enum class Kind {
        ATTRIBUTE, // attr accessed with "." on some other object
        CLASS, // class definition
        CONSTRUCTOR, // __init__ functions in classes
        FUNCTION, // plain function
        METHOD, // static or instance method
        MODULE, // file
        PARAMETER, // function param
        SCOPE, // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE      // local variable
    }


    init {
        this.name = id
        this.qname = type.table.path

        if (node is Url) {
            val url = (node as Url).url
            if (url.startsWith("file://")) {
                fileOrUrl = url.substring("file://".length)
            } else {
                fileOrUrl = url
            }
        } else {
            fileOrUrl = node.file
            if (node is Name) {
                name = (node as Name).id
            }
        }

        initLocationInfo(node)
        Analyzer.self.registerBinding(this)
    }


    private fun initLocationInfo(node: Node) {
        start = node.start
        end = node.end
        line = node.line
        col = node.col

        val parent = node.parent
        if (parent is FunctionDef && parent.name === node || parent is ClassDef && parent.name === node) {
            bodyStart = parent.start
            bodyEnd = parent.end
        } else if (node is PyModule) {
            name = node.name
            start = 0
            end = 0
            bodyStart = node.start
            bodyEnd = node.end
        } else {
            bodyStart = node.start
            bodyEnd = node.end
        }
    }


    fun setQname(qname: String) {
        this.qname = qname
    }


    fun addRef(node: Node) {
        refs.add(node)
    }


    // merge one more type into the type
    // used by stateful assignments which we can't track down the control flow
    fun addType(t: Type) {
        type = UnionType.union(type, t)
    }


    fun setType(type: Type) {
        this.type = type
    }


    fun setKind(kind: Kind) {
        this.kind = kind
    }


    fun markStatic() {
        isStatic = true
    }


    fun markSynthetic() {
        isSynthetic = true
    }


    /**
     * Bindings can be sorted by their location for outlining purposes.
     */
    override fun compareTo(o: Any): Int {
        return if (start == (o as Binding).start) {
            end - o.end
        } else {
            start - o.start
        }
    }


    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("(binding:")
        sb.append(":kind=").append(kind)
        sb.append(":node=").append(node)
        sb.append(":type=").append(type)
        sb.append(":qname=").append(qname)
        sb.append(":refs=")
        if (refs.size > 10) {
            sb.append("[")
            sb.append(refs.iterator().next())
            sb.append(", ...(")
            sb.append(refs.size - 1)
            sb.append(" more)]")
        } else {
            sb.append(refs)
        }
        sb.append(">")
        return sb.toString()
    }


    override fun hashCode(): Int {
        return node.hashCode()
    }

    companion object {

        fun createFileBinding(name: String, filename: String, type: Type): Binding {
            val refNode = Dummy(filename, 0, 0, 0, 0)
            return Binding(name, refNode, type, Binding.Kind.MODULE)
        }
    }
}
