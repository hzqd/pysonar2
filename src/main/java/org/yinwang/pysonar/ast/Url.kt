package org.yinwang.pysonar.ast

/**
 * virtual-AST node used to represent virtual source locations for builtins
 * as external urls.
 */
class Url(var url: String) : Node() {

    override fun toString(): String {
        return "<Url:\"$url\">"
    }

}
