package org.yinwang.pysonar

import org.yinwang.pysonar.types.ClassType
import org.yinwang.pysonar.types.Type
import org.yinwang.pysonar.types.UnionType

import java.util.ArrayList
import java.util.TreeSet


/**
 * Generates a file outline from the index: a structure representing the
 * variable and attribute definitions in a file.
 */
class Outliner {

    abstract class Entry {
        var qname: String? = null  // entry qualified name
        /**
         * Returns the file offset of the beginning of the identifier referenced
         * by this outline entry.
         */
        var offset: Int = 0  // file offset of referenced declaration
        var kind: Binding.Kind? = null  // binding kind of outline entry


        abstract val isLeaf: Boolean


        abstract val isBranch: Boolean


        abstract var children: List<Entry>


        /**
         * Returns the simple (unqualified) name of the identifier.
         */
        val name: String
            get() {
                val parts = qname!!.split("[.&@%]".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                return parts[parts.size - 1]
            }


        constructor() {}


        constructor(qname: String, offset: Int, kind: Binding.Kind) {
            this.qname = qname
            this.offset = offset
            this.kind = kind
        }


        fun asLeaf(): Leaf {
            return this as Leaf
        }


        fun asBranch(): Branch {
            return this as Branch
        }


        abstract fun hasChildren(): Boolean


        fun getQname(): String? {
            return qname
        }


        fun setQname(qname: String?) {
            if (qname == null) {
                throw IllegalArgumentException("qname param cannot be null")
            }
            this.qname = qname
        }


        fun setKind(kind: Binding.Kind?) {
            if (kind == null) {
                throw IllegalArgumentException("kind param cannot be null")
            }
            this.kind = kind
        }


        override fun toString(): String {
            val sb = StringBuilder()
            toString(sb, 0)
            return sb.toString().trim { it <= ' ' }
        }


        fun toString(sb: StringBuilder, depth: Int) {
            for (i in 0 until depth) {
                sb.append("  ")
            }
            sb.append(kind)
            sb.append(" ")
            sb.append(name)
            sb.append("\n")
            if (hasChildren()) {
                for (e in children) {
                    e.toString(sb, depth + 1)
                }
            }
        }
    }


    /**
     * An outline entry with children.
     */
    class Branch : Entry {
        override var children: List<Entry>? = ArrayList()


        override val isLeaf: Boolean
            get() = false


        override val isBranch: Boolean
            get() = true


        constructor() {}


        constructor(qname: String, start: Int, kind: Binding.Kind) : super(qname, start, kind) {}


        override fun hasChildren(): Boolean {
            return children != null && !children!!.isEmpty()
        }
    }


    /**
     * An entry with no children.
     */
    class Leaf : Entry {
        override val isLeaf: Boolean
            get() = true


        override val isBranch: Boolean
            get() = false


        override var children: List<Entry>
            get() = ArrayList()
            set(children) = throw UnsupportedOperationException("Leaf nodes cannot have children.")


        constructor() {}


        constructor(qname: String, start: Int, kind: Binding.Kind) : super(qname, start, kind) {}


        override fun hasChildren(): Boolean {
            return false
        }
    }


    /**
     * Create an outline for a file in the index.
     *
     * @param scope the file scope
     * @param path  the file for which to build the outline
     * @return a list of entries constituting the file outline.
     * Returns an empty list if the analyzer hasn't analyzed that path.
     */
    fun generate(idx: Analyzer, abspath: String): List<Entry> {
        val mt = idx.loadFile(abspath) ?: return ArrayList()
        return generate(mt.table, abspath)
    }


    /**
     * Create an outline for a symbol table.
     *
     * @param state the file state
     * @param path  the file for which we're building the outline
     * @return a list of entries constituting the outline
     */
    fun generate(state: State, path: String): List<Entry> {
        val result = ArrayList<Entry>()

        val entries = TreeSet<Binding>()
        for (b in state.values()) {
            if (!b.isSynthetic
                    && !b.isBuiltin
                    && path == b.file) {
                entries.add(b)
            }
        }

        for (nb in entries) {
            var kids: List<Entry>? = null

            if (nb.kind == Binding.Kind.CLASS) {
                var realType = nb.type
                if (realType is UnionType) {
                    for (t in (realType as UnionType).types) {
                        if (t is ClassType) {
                            realType = t
                            break
                        }
                    }
                }
                kids = generate(realType.table, path)
            }

            val kid = if (kids != null) Branch() else Leaf()
            kid.offset = nb.start
            kid.setQname(nb.qname)
            kid.setKind(nb.kind)

            if (kids != null) {
                kid.children = kids
            }
            result.add(kid)
        }
        return result
    }
}
