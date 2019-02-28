package org.yinwang.pysonar

import java.util.HashMap


class Stats {
    internal var contents: MutableMap<String, Any> = HashMap()


    fun putInt(key: String, value: Long) {
        contents[key] = value
    }


    @JvmOverloads
    fun inc(key: String, x: Long = 1) {
        val old = getInt(key)

        if (old == null) {
            contents[key] = 1
        } else {
            contents[key] = old + x
        }
    }


    fun getInt(key: String): Long? {
        val ret = contents[key] as Long
        return ret ?: 0L
    }


    fun print(): String {
        val sb = StringBuilder()

        for ((key, value) in contents) {
            sb.append("\n- $key: $value")
        }

        return sb.toString()
    }

}
