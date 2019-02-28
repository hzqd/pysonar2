package org.yinwang.pysonar.ast

/**
 * A name alias.  Used for the components of import and import-from statements.
 */
class Withitem(var context_expr: Node, var optional_vars: Node?, file: String, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.WITHITEM, file, start, end, line, col) {

    init {
        addChildren(context_expr, optional_vars)
    }

    override fun toString(): String {
        return "(withitem:$context_expr as $optional_vars)"
    }

}
