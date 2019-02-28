package org.yinwang.pysonar

import java.util.ArrayList

class Stack<T> {
    private val content = ArrayList<T>()

    fun push(item: T) {
        content.add(item)
    }

    fun top(): T {
        return content[content.size - 1]
    }

    fun pop(): T? {
        return if (!content.isEmpty()) {
            content.removeAt(content.size - 1)
        } else {
            null
        }
    }
}
