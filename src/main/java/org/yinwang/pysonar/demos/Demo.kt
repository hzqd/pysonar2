package org.yinwang.pysonar.demos

import org.yinwang.pysonar.`$`
import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.Options
import org.yinwang.pysonar.Progress

import java.io.File
import java.util.ArrayList


class Demo {

    private var analyzer: Analyzer? = null
    private var rootPath: String? = null
    private var linker: Linker? = null


    private fun makeOutputDir() {
        if (!OUTPUT_DIR!!.exists()) {
            OUTPUT_DIR!!.mkdirs()
            `$`.msg("Created directory: " + OUTPUT_DIR!!.absolutePath)
        }
    }

    @Throws(Exception::class)
    private fun start(fileOrDir: String, options: MutableMap<String, Any>) {
        val f = File(fileOrDir)
        val rootDir = if (f.isFile) f.parentFile else f
        try {
            rootPath = `$`.unifyPath(rootDir)
        } catch (e: Exception) {
            `$`.die("File not found: $f")
        }

        analyzer = Analyzer(options)
        `$`.msg("Loading and analyzing files")
        try {
            analyzer!!.analyze(f.path)
        } finally {
            analyzer!!.finish()
        }

        generateHtml()
    }


    private fun generateHtml() {
        `$`.msg("\nGenerating HTML")
        makeOutputDir()

        linker = Linker(rootPath, OUTPUT_DIR)
        linker!!.findLinks(analyzer!!)

        val rootLength = rootPath!!.length

        var total = 0
        for (path in analyzer!!.getLoadedFiles()) {
            if (path.startsWith(rootPath!!)) {
                total++
            }
        }

        val progress = Progress(total.toLong(), 50)

        for (path in analyzer!!.getLoadedFiles()) {
            if (path.startsWith(rootPath!!)) {
                progress.tick()
                val destFile = `$`.joinPath(OUTPUT_DIR!!, path.substring(rootLength))
                destFile.parentFile.mkdirs()
                val destPath = destFile.absolutePath + ".html"
                val html = markup(path)
                try {
                    `$`.writeFile(destPath, html)
                } catch (e: Exception) {
                    `$`.msg("Failed to write: $destPath")
                }

            }
        }

        `$`.msg("\nWrote " + analyzer!!.getLoadedFiles().size + " files to " + OUTPUT_DIR)
    }


    private fun markup(path: String): String {
        val source: String?

        try {
            source = `$`.readFile(path)
        } catch (e: Exception) {
            `$`.die("Failed to read file: $path")
            return ""
        }

        val styles = ArrayList<Style>()
        styles.addAll(linker!!.getStyles(path))

        val styledSource = StyleApplier(path, source, styles).apply()
        val outline = HtmlOutline(analyzer).generate(path)

        val sb = StringBuilder()
        sb.append("<html>\n")
                .append("<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<title>").append(path).append("</title>\n")
                .append("<style type='text/css'>\n").append(CSS).append("\n</style>\n")
                .append("<script language=\"JavaScript\" type=\"text/javascript\">\n")
                .append(if (Analyzer.self.hasOption("debug")) JS_DEBUG else JS)
                .append("</script>\n")
                .append("</head>\n<body>\n")
                .append("<table width=100% border='1px solid gray'><tr><td valign='top'>")
                .append(outline)
                .append("</td><td>")
                .append("<pre>")
                .append(addLineNumbers(styledSource))
                .append("</pre>")
                .append("</td></tr></table></body></html>")
        return sb.toString()
    }


    private fun addLineNumbers(source: String): String {
        val result = StringBuilder((source.length * 1.2).toInt())
        var count = 1
        for (line in source.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
            result.append("<span class='lineno'>")
            result.append(String.format("%1$4d", count++))
            result.append("</span> ")
            result.append(line)
            result.append("\n")
        }
        return result.toString()
    }

    companion object {

        private var OUTPUT_DIR: File? = null

        private val CSS = `$`.readResource("org/yinwang/pysonar/css/demo.css")
        private val JS = `$`.readResource("org/yinwang/pysonar/javascript/highlight.js")
        private val JS_DEBUG = `$`.readResource("org/yinwang/pysonar/javascript/highlight-debug.js")


        private fun usage() {
            `$`.msg("Usage:  java -jar pysonar-2.0-SNAPSHOT.jar <file-or-dir> <output-dir>")
            `$`.msg("Example that generates an index for Python 2.7 standard library:")
            `$`.msg(" java -jar pysonar-2.0-SNAPSHOT.jar /usr/lib/python2.7 ./html")
            System.exit(0)
        }


        private fun checkFile(path: String): File {
            val f = File(path)
            if (!f.canRead()) {
                `$`.die("Path not found or not readable: $path")
            }
            return f
        }


        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val options = Options(args)

            val argsList = options.args
            val fileOrDir = argsList[0]
            OUTPUT_DIR = File(argsList[1])

            //        System.out.println("options: " + options.getOptionsMap());
            Demo().start(fileOrDir, options.optionsMap)
            `$`.msg(`$`.gcStats)
        }
    }
}
