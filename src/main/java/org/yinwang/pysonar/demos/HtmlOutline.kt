package org.yinwang.pysonar.demos

import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.Outliner
import org.yinwang.pysonar.`$`


internal class HtmlOutline(private val analyzer: Analyzer) {
    private var buffer: StringBuilder? = null


    fun generate(path: String): String {
        buffer = StringBuilder(1024)
        val entries = generateOutline(analyzer, path)
        addOutline(entries)
        val html = buffer!!.toString()
        buffer = null
        return html
    }


    fun generateOutline(analyzer: Analyzer, file: String): List<Outliner.Entry> {
        return Outliner().generate(analyzer, file)
    }


    private fun addOutline(entries: List<Outliner.Entry>) {
        add("<ul>\n")
        for (e in entries) {
            addEntry(e)
        }
        add("</ul>\n")
    }


    private fun addEntry(e: Outliner.Entry) {
        add("<li>")

        var style: String? = null
        when (e.kind) {
            Binding.Kind.FUNCTION, Binding.Kind.METHOD, Binding.Kind.CONSTRUCTOR -> style = "function"
            Binding.Kind.CLASS -> style = "type-name"
            Binding.Kind.PARAMETER -> style = "parameter"
            Binding.Kind.VARIABLE, Binding.Kind.SCOPE -> style = "identifier"
        }

        add("<a href='#")
        add(e.getQname())
        add("', xid='" + e.getQname() + "'>")
        add(e.name)
        add("</a>")

        if (e.isBranch) {
            addOutline(e.children)
        }
        add("</li>")
    }


    private fun add(text: String?) {
        buffer!!.append(text)
    }
}
