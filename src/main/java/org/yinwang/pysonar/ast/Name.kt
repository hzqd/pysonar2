package org.yinwang.pysonar.ast

class Name : Node {

    val id: String  // identifier
    var type: NameType

    /**
     * Returns `true` if this name node is the `attr` child
     * (i.e. the attribute being accessed) of an [Attribute] node.
     */
    val isAttribute: Boolean
        get() = parent is Attribute && (parent as Attribute).attr === this

    @JvmOverloads
    constructor(id: String, file: String? = null, start: Int = -1, end: Int = -1, line: Int = -1, col: Int = -1) : super(NodeType.NAME, file, start, end, line, col) {
        this.id = id
        this.name = id
        this.type = NameType.LOCAL
    }

    constructor(id: String, type: NameType, file: String, start: Int, end: Int, line: Int, col: Int) : super(NodeType.NAME, file, start, end, line, col) {
        this.id = id
        this.type = type
    }

    override fun toString(): String {
        return "($id:$line:$col)"
    }

    override fun toDisplay(): String {
        return id
    }

}// generated name
