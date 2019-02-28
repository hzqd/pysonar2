package org.yinwang.pysonar.demos

import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.`$`
import java.util.SortedSet
import java.util.TreeSet


/**
 * Turns a list of [Style]s into HTML spans.
 */
internal class StyleApplier(path: String, private val source: String  // input source code
                            , runs: List<Style>) {

    private val tags = TreeSet<Tag>()

    private var buffer: StringBuilder? = null  // html output buffer

    // Current offset into the source being copied into the html buffer.
    private var sourceOffset = 0


    internal abstract inner class Tag : Comparable<Tag> {
        var offset: Int = 0
        var style: Style? = null


        override fun compareTo(other: Tag): Int {
            if (this === other) {
                return 0
            }
            if (this.offset < other.offset) {
                return -1
            }
            return if (other.offset < this.offset) {
                1
            } else this.hashCode() - other.hashCode()
        }


        internal open fun insert() {
            // Copy source code up through this tag.
            if (offset > sourceOffset) {
                copySource(sourceOffset, offset)
            }
        }
    }


    internal inner class StartTag(style: Style) : Tag() {
        init {
            offset = style.start
            this.style = style
        }


        override fun insert() {
            super.insert()
            if (Analyzer.self.hasOption("debug")) {
                when (style!!.type) {
                    Style.Type.ANCHOR -> {
                        buffer!!.append("<a name='" + style!!.url + "'")
                        buffer!!.append(", id ='" + style!!.id + "'")
                        if (style!!.highlight != null && !style!!.highlight!!.isEmpty()) {
                            val ids = `$`.joinWithSep(style!!.highlight!!, "\",\"", "\"", "\"")
                            buffer!!.append(", onmouseover='highlight(").append(ids).append(")'")
                        }
                    }
                    Style.Type.LINK -> {
                        buffer!!.append("<a href='" + style!!.url + "'")
                        buffer!!.append(", id ='" + style!!.id + "'")
                        if (style!!.highlight != null && !style!!.highlight!!.isEmpty()) {
                            val ids = `$`.joinWithSep(style!!.highlight!!, "\",\"", "\"", "\"")
                            buffer!!.append(", onmouseover='highlight(").append(ids).append(")'")
                        }
                    }
                    else -> {
                        buffer!!.append("<span class='")
                        buffer!!.append(toCSS(style!!)).append("'")
                    }
                }
            } else {
                when (style!!.type) {
                    Style.Type.ANCHOR -> {
                        buffer!!.append("<a name='" + style!!.url + "'")
                        buffer!!.append(", xid ='" + style!!.id + "'")
                    }
                    Style.Type.LINK -> {
                        buffer!!.append("<a href='" + style!!.url + "'")
                        buffer!!.append(", xid ='" + style!!.id + "'")
                    }
                    else -> {
                        buffer!!.append("<span class='")
                        buffer!!.append(toCSS(style!!)).append("'")
                    }
                }
            }
            if (style!!.message != null) {
                buffer!!.append(", title='")
                buffer!!.append(style!!.message)
                buffer!!.append("'")
            }
            buffer!!.append(">")
        }
    }


    internal inner class EndTag(style: Style) : Tag() {
        init {
            offset = style.end
            this.style = style
        }


        override fun insert() {
            super.insert()
            when (style!!.type) {
                Style.Type.ANCHOR, Style.Type.LINK -> buffer!!.append("</a>")
                else -> buffer!!.append("</span>")
            }
        }
    }


    init {
        for (run in runs) {
            tags.add(StartTag(run))
            tags.add(EndTag(run))
        }
    }


    /**
     * @return the html
     */
    fun apply(): String {
        buffer = StringBuilder()

        for (tag in tags) {
            tag.insert()
        }
        // Copy in remaining source beyond last tag.
        if (sourceOffset < source.length) {
            copySource(sourceOffset, source.length)
        }
        return buffer!!.toString()
    }


    /**
     * Copies code from the input source to the output html.
     *
     * @param begin the starting source offset
     * @param end   the end offset, or -1 to go to end of file
     */
    private fun copySource(begin: Int, end: Int) {
        // Be robust if the analyzer gives us bad offsets.
        try {
            val src = escape(if (end == -1)
                source.substring(begin)
            else
                source.substring(begin, end))
            buffer!!.append(src)
        } catch (x: RuntimeException) {
            // This can happen with files with weird encodings
            // Igore them because of the rareness
        }

        sourceOffset = end
    }


    private fun escape(s: String): String {
        return s.replace("&", "&amp;")
                .replace("'", "&#39;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }


    private fun toCSS(style: Style): String {
        return style.type.toString().toLowerCase().replace("_", "-")
    }
}
