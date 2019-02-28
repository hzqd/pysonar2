package org.yinwang.pysonar.hash


abstract class HashFunction {
    abstract fun hash(o: Any): Int
}
