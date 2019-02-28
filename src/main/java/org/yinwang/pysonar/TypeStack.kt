package org.yinwang.pysonar

import java.util.ArrayList


class TypeStack {


    private val stack = ArrayList<Pair>()

    internal inner class Pair(var first: Any, var second: Any)


    fun push(first: Any, second: Any) {
        stack.add(Pair(first, second))
    }


    fun pop(first: Any, second: Any) {
        stack.removeAt(stack.size - 1)
    }


    fun contains(first: Any, second: Any): Boolean {
        for (p in stack) {
            if (p.first === first && p.second === second || p.first === second && p.second === first) {
                return true
            }
        }
        return false
    }

}
