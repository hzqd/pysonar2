package org.yinwang.pysonar

import org.yinwang.pysonar.ast.Dummy
import org.yinwang.pysonar.ast.Node

import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashMap

import com.google.gson.Gson
import com.google.gson.GsonBuilder

class TestInference(private val testFile: String) {
    private var expecteRefsFile: String? = null
    private var missingRefsFile: String? = null
    private var wrongTypeFile: String? = null

    init {
        if (File(testFile).isDirectory) {
            expecteRefsFile = `$`.makePathString(testFile, "refs.json")
            missingRefsFile = `$`.makePathString(testFile, "missing_refs")
            wrongTypeFile = `$`.makePathString(testFile, "wrong_types")
        } else {
            expecteRefsFile = `$`.makePathString("$testFile.refs.json")
            missingRefsFile = `$`.makePathString("$testFile.missing_refs")
            wrongTypeFile = `$`.makePathString("$testFile.wrong_types")
        }
    }

    fun runAnalysis(dir: String): Analyzer {
        val options = HashMap<String, Any>()
        options["quiet"] = true
        val analyzer = Analyzer(options)
        analyzer.analyze(dir)

        analyzer.finish()
        return analyzer
    }

    fun generateRefs(analyzer: Analyzer) {
        val refs = ArrayList<Map<String, Any>>()
        for ((key, bindings) in analyzer.references) {
            var filename: String? = key.file

            // only record those in the testFile
            if (filename != null && filename.startsWith(Analyzer.self.projectDir!!)) {
                filename = `$`.projRelPath(filename).replace("\\\\".toRegex(), "/")
                val writeout = LinkedHashMap<String, Any>()

                val ref = LinkedHashMap<String, Any>()
                ref["name"] = key.name
                ref["file"] = filename
                ref["start"] = key.start
                ref["end"] = key.end
                ref["line"] = key.line
                ref["col"] = key.col

                val dests = ArrayList<Map<String, Any>>()
                Collections.sort(bindings) { a, b -> if (a.start == b.start) a.end - b.end else a.start - b.start }
                for (b in bindings) {
                    var destFile = b.file
                    if (destFile != null && destFile!!.startsWith(Analyzer.self.projectDir!!)) {
                        destFile = `$`.projRelPath(destFile!!).replace("\\\\".toRegex(), "/")
                        val dest = LinkedHashMap<String, Any>()
                        dest["name"] = b.name
                        dest["file"] = destFile
                        dest["start"] = b.start
                        dest["end"] = b.end
                        dest["line"] = b.line
                        dest["col"] = b.col
                        dest["type"] = b.type.toString()
                        dests.add(dest)
                    }
                }
                if (!dests.isEmpty()) {
                    writeout["ref"] = ref
                    writeout["dests"] = dests
                    refs.add(writeout)
                }
            }
        }

        val json = gson.toJson(refs)
        `$`.writeFile(expecteRefsFile, json)
    }

    fun checkRefs(analyzer: Analyzer): Boolean {
        val missing = ArrayList<String>()
        val wrongType = ArrayList<String>()
        val json = `$`.readFile(expecteRefsFile!!)
        if (json == null) {
            `$`.msg("Expected refs not found in: " + expecteRefsFile +
                    "Please run Test with -generate to generate")
            return false
        }
        val expectedRefs = gson.fromJson<List<*>>(json, List<*>::class.java!!)
        for (r in expectedRefs) {
            val refMap = r.get("ref") as Map<*, *>
            val dummy = makeDummy(refMap)

            val dests = r.get("dests") as List<*>
            val actual = analyzer.references[dummy]

            for (d in dests) {
                val name1 = refMap.get("name") as String
                val file1 = refMap.get("file") as String
                val line1 = Math.floor(refMap.get("line") as Double).toInt()
                val col1 = Math.floor(refMap.get("col") as Double).toInt()
                val type1 = arrayOfNulls<String>(1)

                val fileShort2 = d.get("file") as String
                val file2 = `$`.projAbsPath(fileShort2)
                val start2 = Math.floor(d.get("start") as Double).toInt()
                val end2 = Math.floor(d.get("end") as Double).toInt()
                val line2 = Math.floor(d.get("line") as Double).toInt()
                val col2 = Math.floor(d.get("col") as Double).toInt()
                val type2 = d.get("type") as String

                if (!checkExist(actual, file2, start2, end2)) {
                    val variable = "$name1:$line1:$col1"
                    var loc = "$name1:$line2:$col2"
                    if (file1 != fileShort2) {
                        loc = "$fileShort2:$loc"
                    }
                    val msg = "Missing reference from $variable to $loc"
                    missing.add(msg)
                } else if (!checkType(actual, file2, start2, end2, type2, type1)) {
                    val variable = "$name1:$line1:$col1"
                    var loc = "$name1:$line2:$col2"
                    if (file1 != fileShort2) {
                        loc = "$fileShort2:$loc"
                    }
                    var msg = "Inferred wrong type for $variable. "
                    msg += "Localtion: $loc, "
                    msg += "Expected: " + type1[0] + ", "
                    msg += "Actual: $type2."
                    wrongType.add(msg)
                }
            }
        }

        var success = true

        // record the ref & failed dests if any
        if (missing.isEmpty() && wrongType.isEmpty()) {
            `$`.testmsg("   $testFile")
        } else {
            `$`.testmsg(" - $testFile")
        }

        if (!missing.isEmpty()) {
            var report = missing.joinToString("\n     * ")
            report = "     * $report"
            `$`.testmsg(report)
            `$`.writeFile(missingRefsFile, report)
            success = false
        } else {
            `$`.deleteFile(missingRefsFile)
        }

        if (!wrongType.isEmpty()) {
            var report = wrongType.joinToString("\n     * ")
            report = "     * $report"
            `$`.testmsg(report)
            `$`.writeFile(wrongTypeFile, report)
            success = false
        } else {
            `$`.deleteFile(wrongTypeFile)
        }

        return success
    }

    private fun checkExist(bindings: List<Binding>?, file: String?, start: Int, end: Int): Boolean {
        if (bindings == null) {
            return false
        }

        for (b in bindings) {
            if ((b.file == null && file == null || b.file != null && file != null && b.file == file) &&
                    b.start == start && b.end == end) {
                return true
            }
        }

        return false
    }

    private fun checkType(bindings: List<Binding>?, file: String?, start: Int, end: Int, type: String, actualType: Array<String>): Boolean {
        if (bindings == null) {
            return false
        }

        for (b in bindings) {
            if ((b.file == null && file == null || b.file != null && file != null && b.file == file) &&
                    b.start == start && b.end == end && b.type.toString() == type) {
                return true
            } else {
                actualType[0] = b.type.toString()
            }
        }

        return false
    }

    fun generateTest() {
        val analyzer = runAnalysis(testFile)
        generateRefs(analyzer)
        `$`.testmsg("  * $testFile")
    }

    fun runTest(): Boolean {
        val analyzer = runAnalysis(testFile)
        return checkRefs(analyzer)
    }

    companion object {
        private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

        fun makeDummy(m: Map<String, Any>): Dummy {
            val file = `$`.projAbsPath(m["file"] as String)
            val start = Math.floor(m["start"] as Double).toInt()
            val end = Math.floor(m["end"] as Double).toInt()
            return Dummy(file, start, end, -1, -1)
        }

        // ------------------------- static part -----------------------

        fun testAll(path: String, generate: Boolean): List<String>? {
            val failed = ArrayList<String>()
            if (generate) {
                `$`.testmsg("Generating tests:")
            } else {
                `$`.testmsg("Verifying tests:")
            }

            testRecursive(path, generate, failed)

            if (generate) {
                `$`.testmsg("All tests generated.")
                return null
            } else if (failed.isEmpty()) {
                `$`.testmsg("All tests passed.")
                return null
            } else {
                return failed
            }
        }

        fun testRecursive(path: String, generate: Boolean, failed: MutableList<String>) {
            val file_or_dir = File(path)

            if (file_or_dir.isDirectory) {
                if (path.endsWith(".test")) {
                    val test = TestInference(path)
                    if (generate) {
                        test.generateTest()
                    } else if (!test.runTest()) {
                        failed.add(path)
                    }
                } else {
                    for (file in file_or_dir.listFiles()!!) {
                        testRecursive(file.path, generate, failed)
                    }
                }
            }
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val options = Options(args)
            val argsList = options.args
            val inputDir = `$`.unifyPath(argsList[0])

            // generate expected file?
            val generate = options.hasOption("generate")
            testAll(inputDir, generate)
        }
    }
}
