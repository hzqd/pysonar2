package org.yinwang.pysonar

import java.util.Objects

class Pair(var first: Any, var second: Any) {

    fun equals(first: Any, second: Any): Boolean {
        return this.first === first && this.second === second || this.first === second && this.second === first
    }
}
