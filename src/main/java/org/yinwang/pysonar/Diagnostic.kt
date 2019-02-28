package org.yinwang.pysonar


class Diagnostic(var file: String, var category: Category, var start: Int, var end: Int, var msg: String) {
    enum class Category {
        INFO, WARNING, ERROR
    }


    override fun toString(): String {
        return "<Diagnostic:$file:$category:$msg>"
    }
}
