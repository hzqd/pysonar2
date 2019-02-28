package org.yinwang.pysonar

import java.util.ArrayList
import java.util.LinkedHashMap

class Options(args: Array<String>) {

    private val optionsMap = LinkedHashMap<String, Any>()


    private val args = ArrayList<String>()


    init {
        var i = 0
        while (i < args.size) {
            var key = args[i]
            if (key.startsWith("--")) {
                if (i + 1 >= args.size) {
                    `$`.die("option needs a value: $key")
                } else {
                    key = key.substring(2)
                    val value = args[i + 1]
                    if (!value.startsWith("-")) {
                        optionsMap[key] = value
                        i++
                    }
                }
            } else if (key.startsWith("-")) {
                key = key.substring(1)
                optionsMap[key] = true
            } else {
                this.args.add(key)
            }
            i++
        }
    }


    operator fun get(key: String): Any {
        return optionsMap[key]
    }


    fun hasOption(key: String): Boolean {
        val v = optionsMap[key]
        return v as? Boolean ?: false
    }


    fun put(key: String, value: Any) {
        optionsMap[key] = value
    }


    fun getArgs(): List<String> {
        return args
    }


    fun getOptionsMap(): Map<String, Any> {
        return optionsMap
    }

    companion object {


        @JvmStatic
        fun main(args: Array<String>) {
            val options = Options(args)
            for (key in options.optionsMap.keys) {
                println(key + " = " + options[key])
            }
        }
    }

}
