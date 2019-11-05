package org.yinwang.pysonar.ast

import org.yinwang.pysonar.`$`
import org.yinwang.pysonar.Analyzer

/**
 * A Node is a junction in the program.
 * Since there is no way to put different things in the same segment of the same file,
 * a node is uniquely identified by a file, a start and end point.
 */
abstract class Node : java.io.Serializable, Comparable<Any> {

    var nodeType: NodeType
    var file: String
    var start: Int = 0
    var end: Int = 0
    var line: Int = 0
    var col: Int = 0

    var name: String? = null
    var parent: Node? = null

    val fullPath: String
        get() = if (!file.startsWith("/")) {
            `$`.makePathString(Analyzer.self.projectDir, file)
        } else {
            file
        }

    val astRoot: Node
        get() = if (parent == null) {
            this
        } else parent!!.astRoot

    val docString: Str?
        get() {
            var body: Node? = null
            if (this is FunctionDef) {
                body = this.body
            } else if (this is ClassDef) {
                body = this.body
            } else if (this is PyModule) {
                body = this.body
            }

            if (body is Block && body.seq.size >= 1) {
                val firstExpr = body.seq[0]
                if (firstExpr is Expr) {
                    val docstrNode = firstExpr.value
                    if (docstrNode != null && docstrNode is Str) {
                        return docstrNode
                    }
                }
            }
            return null
        }

    constructor() {}

    constructor(nodeType: NodeType, file: String, start: Int, end: Int, line: Int, col: Int) {
        this.nodeType = nodeType
        this.file = file
        this.start = start
        this.end = end
        this.line = line
        this.col = col
    }

    fun setParent(parent: Node) {
        this.parent = parent
    }

    fun length(): Int {
        return end - start
    }

    fun addChildren(vararg nodes: Node) {
        if (nodes != null) {
            for (n in nodes) {
                n?.setParent(this)
            }
        }
    }

    fun addChildren(nodes: Collection<Node>?) {
        if (nodes != null) {
            for (n in nodes) {
                n?.setParent(this)
            }
        }
    }

    // nodes are equal if they are from the same file and same starting point
    override fun equals(obj: Any?): Boolean {
        if (obj !is Node) {
            return false
        } else {
            val node = obj as Node?
            val file = this.file
            return start == node!!.start &&
                    end == node.end &&
                    `$`.same(file, node.file)
        }
    }

    override fun hashCode(): Int {
        return "$file:$start:$end".hashCode()
    }

    override fun compareTo(o: Any): Int {
        return if (o is Node) {
            start - o.start
        } else {
            -1
        }
    }

    open fun toDisplay(): String {
        return ""
    }

    override fun toString(): String {
        return "(node:$file:$name:$start)"
    }

}
