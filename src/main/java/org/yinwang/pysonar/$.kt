package org.yinwang.pysonar

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import sun.net.www.protocol.file.FileURLConnection

import java.io.*
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.net.JarURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile


/**
 * unsorted utility class
 */
object `$` {

    val tempDir: String
        get() {
            val systemTemp = systemTempDir
            return makePathString(systemTemp, "pysonar2-" + Analyzer.self.sid)
        }

    val systemTempDir: String
        get() {
            val tmp = System.getProperty("java.io.tmpdir")
            val sep = System.getProperty("file.separator")
            return if (tmp.endsWith(sep)) {
                tmp
            } else tmp + sep
        }


    val gcStats: String
        get() {
            var totalGC: Long = 0
            var gcTime: Long = 0

            for (gc in ManagementFactory.getGarbageCollectorMXBeans()) {
                val count = gc.collectionCount

                if (count >= 0) {
                    totalGC += count
                }

                val time = gc.collectionTime

                if (time >= 0) {
                    gcTime += time
                }
            }

            val sb = StringBuilder()

            sb.append(banner("memory stats"))
            sb.append("\n- total collections: $totalGC")
            sb.append("\n- total collection time: " + formatTime(gcTime))

            val runtime = Runtime.getRuntime()
            sb.append("\n- total memory: " + `$`.printMem(runtime.totalMemory()))

            return sb.toString()
        }

    fun baseFileName(filename: String): String {
        return File(filename).name
    }


    fun hashFileName(filename: String): String {
        return Integer.toString(filename.hashCode())
    }


    fun same(o1: Any?, o2: Any?): Boolean {
        return if (o1 == null) {
            o2 == null
        } else {
            o1 == o2
        }
    }

    fun getTempFile(file: String): String {
        val tmpDir = tempDir
        return makePathString(tmpDir, file)
    }


    /**
     * Returns the parent qname of `qname` -- everything up to the
     * last dot (exclusive), or if there are no dots, the empty string.
     */
    fun getQnameParent(qname: String?): String {
        if (qname == null || qname.isEmpty()) {
            return ""
        }
        val index = qname.lastIndexOf(".")
        return if (index == -1) {
            ""
        } else qname.substring(0, index)
    }


    fun moduleQname(file: String): String? {
        var file = file
        val f = File(file)

        if (f.name.endsWith("__init__.py")) {
            file = f.parent
        } else if (file.endsWith(Globals.FILE_SUFFIX)) {
            file = file.substring(0, file.length - Globals.FILE_SUFFIX.length)
        }

        // remove Windows like '\\' and 'C:'
        file = file.replace("^\\\\".toRegex(), "")
        file = file.replace("^[a-zA-Z]:".toRegex(), "")

        return file.replace(".", "%20").replace('/', '.').replace('\\', '.')
    }


    /**
     * Given an absolute `path` to a file (not a directory),
     * returns the module name for the file.  If the file is an __init__.py,
     * returns the last component of the file's parent directory, else
     * returns the filename without path or extension.
     */
    fun moduleName(path: String): String {
        val f = File(path)
        val name = f.name
        return if (name == "__init__.py") {
            f.parentFile.name
        } else if (name.endsWith(Globals.FILE_SUFFIX)) {
            name.substring(0, name.length - Globals.FILE_SUFFIX.length)
        } else {
            name
        }
    }


    fun arrayToString(strings: Collection<String>): String {
        val sb = StringBuilder()
        for (s in strings) {
            sb.append(s).append("\n")
        }
        return sb.toString()
    }


    fun arrayToSortedStringSet(strings: Collection<String>): String {
        val sorter = TreeSet<String>()
        sorter.addAll(strings)
        return arrayToString(sorter)
    }


    fun writeFile(path: String, contents: String) {
        var out: PrintWriter? = null
        try {
            out = PrintWriter(BufferedWriter(FileWriter(path)))
            out.print(contents)
            out.flush()
        } catch (e: Exception) {
            `$`.die("Failed to write: $path")
        } finally {
            out?.close()
        }
    }


    fun readFile(path: String): String? {
        // Don't use line-oriented file read -- need to retain CRLF if present
        // so the style-run and link offsets are correct.
        val content = getBytesFromFile(path)
        return if (content == null) {
            null
        } else {
            String(content, Charset.forName("UTF-8"))
        }
    }


    fun getBytesFromFile(filename: String): ByteArray? {
        try {
            return FileUtils.readFileToByteArray(File(filename))
        } catch (e: Exception) {
            return null
        }

    }


    internal fun isReadableFile(path: String): Boolean {
        val f = File(path)
        return f.canRead() && f.isFile
    }


    @Throws(IOException::class)
    fun readWhole(`is`: InputStream): String {
        val sb = StringBuilder()
        val bytes = ByteArray(8192)

        var nRead: Int
        while ((nRead = `is`.read(bytes, 0, 8192)) > 0) {
            sb.append(String(bytes, 0, nRead))
        }
        return sb.toString()
    }


    @Throws(Exception::class)
    fun copyResourcesRecursively(originUrl: URL, destination: File) {
        val urlConnection = originUrl.openConnection()
        if (urlConnection is JarURLConnection) {
            copyJarResourcesRecursively(destination, urlConnection)
        } else if (urlConnection is FileURLConnection) {
            FileUtils.copyDirectory(File(originUrl.path), destination)
        } else {
            die("Unsupported URL type: $urlConnection")
        }
    }


    fun copyJarResourcesRecursively(destination: File, jarConnection: JarURLConnection) {
        val jarFile: JarFile
        try {
            jarFile = jarConnection.jarFile
        } catch (e: Exception) {
            `$`.die("Failed to get jar file)")
            return
        }

        val em = jarFile.entries()
        while (em.hasMoreElements()) {
            val entry = em.nextElement()
            if (entry.name.startsWith(jarConnection.entryName)) {
                val fileName = StringUtils.removeStart(entry.name, jarConnection.entryName)
                if (fileName != "/") {  // exclude the directory
                    var entryInputStream: InputStream? = null
                    try {
                        entryInputStream = jarFile.getInputStream(entry)
                        FileUtils.copyInputStreamToFile(entryInputStream!!, File(destination, fileName))
                    } catch (e: Exception) {
                        die("Failed to copy resource: $fileName", e)
                    } finally {
                        if (entryInputStream != null) {
                            try {
                                entryInputStream.close()
                            } catch (e: Exception) {
                            }

                        }
                    }
                }
            }
        }
    }


    fun readResource(resource: String): String {
        val s = Thread.currentThread().contextClassLoader.getResourceAsStream(resource)
        return readWholeStream(s)
    }


    /**
     * get unique hash according to file content and filename
     */
    fun getFileHash(path: String): String {
        val bytes = getBytesFromFile(path)
        return `$`.getContentHash(path.toByteArray()) + "." + getContentHash(bytes)
    }


    fun getContentHash(fileContents: ByteArray?): String {
        val algorithm: MessageDigest

        try {
            algorithm = MessageDigest.getInstance("SHA-1")
        } catch (e: Exception) {
            `$`.die("Failed to get SHA, shouldn't happen")
            return ""
        }

        algorithm.reset()
        algorithm.update(fileContents!!)
        val messageDigest = algorithm.digest()
        val sb = StringBuilder()
        for (aMessageDigest in messageDigest) {
            sb.append(String.format("%02x", 0xFF and aMessageDigest))
        }
        return sb.toString()
    }


    fun escapeQname(s: String): String {
        return s.replace("[.&@%-]".toRegex(), "_")
    }


    fun escapeWindowsPath(path: String): String {
        return path.replace("\\", "\\\\")
    }


    fun toStringCollection(collection: Collection<Int>): Collection<String> {
        val ret = ArrayList<String>()
        for (x in collection) {
            ret.add(x.toString())
        }
        return ret
    }


    fun joinWithSep(ls: Collection<String>, sep: String, start: String?,
                    end: String?): String {
        val sb = StringBuilder()
        if (start != null && ls.size > 1) {
            sb.append(start)
        }
        var i = 0
        for (s in ls) {
            if (i > 0) {
                sb.append(sep)
            }
            sb.append(s)
            i++
        }
        if (end != null && ls.size > 1) {
            sb.append(end)
        }
        return sb.toString()
    }


    fun msg(m: String) {
        if (Analyzer.self != null && !Analyzer.self.hasOption("quiet")) {
            println(m)
        }
    }


    fun msg_(m: String) {
        if (Analyzer.self != null && !Analyzer.self.hasOption("quiet")) {
            print(m)
        }
    }


    fun testmsg(m: String) {
        println(m)
    }


    @JvmOverloads
    fun die(msg: String, e: Exception? = null) {
        System.err.println(msg)

        if (e != null) {
            System.err.println("Exception: $e\n")
        }

        Thread.dumpStack()
        System.exit(2)
    }


    fun readWholeFile(filename: String): String? {
        try {
            return Scanner(File(filename)).useDelimiter("PYSONAR2END").next()
        } catch (e: FileNotFoundException) {
            return null
        }

    }


    fun readWholeStream(`in`: InputStream?): String {
        return Scanner(`in`!!).useDelimiter("\\Z").next()
    }


    fun percent(num: Long, total: Long): String {
        if (total == 0L) {
            return "100%"
        } else {
            val pct = (num * 100 / total).toInt()
            return String.format("%1$3d", pct) + "%"
        }
    }


    fun formatTime(millis: Long): String {
        var sec = millis / 1000
        var min = sec / 60
        sec = sec % 60
        val hr = min / 60
        min = min % 60

        return "$hr:$min:$sec"
    }


    /**
     * format number with fixed width
     */
    fun formatNumber(n: Any, length: Int): String {
        var length = length
        if (length == 0) {
            length = 1
        }

        return if (n is Int) {
            String.format("%1$" + length + "d", n)
        } else if (n is Long) {
            String.format("%1$" + length + "d", n)
        } else {
            String.format("%1$" + length + "s", n.toString())
        }
    }

    fun deleteDirectory(directory: String): Boolean {
        return deleteDirectory(File(directory))
    }

    fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (f in files) {
                    if (f.isDirectory) {
                        deleteDirectory(f)
                    } else {
                        f.delete()
                    }
                }
            }
        }
        return directory.delete()
    }


    fun newSessionId(): String {
        return UUID.randomUUID().toString()
    }


    fun makePath(vararg files: String): File {
        var ret = File(files[0])

        for (i in 1 until files.size) {
            ret = File(ret, files[i])
        }

        return ret
    }


    fun makePathString(vararg files: String): String {
        return unifyPath(makePath(*files).path)
    }


    fun unifyPath(filename: String): String {
        return unifyPath(File(filename))
    }


    fun unifyPath(file: File): String {
        try {
            return file.canonicalPath
        } catch (e: Exception) {
            die("Failed to get canonical path")
            return ""
        }

    }


    fun relPath(path1: String, path2: String): String? {
        val a = unifyPath(path1)
        val b = unifyPath(path2)

        val `as` = a.split("[/\\\\]".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        val bs = b.split("[/\\\\]".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

        var i: Int
        i = 0
        while (i < Math.min(`as`.size, bs.size)) {
            if (`as`[i] != bs[i]) {
                break
            }
            i++
        }

        val ups = `as`.size - i - 1

        var res: File? = null

        for (x in 0 until ups) {
            res = File(res, "..")
        }

        for (y in i until bs.size) {
            res = File(res, bs[y])
        }

        return res?.path
    }


    fun projRelPath(file: String): String {
        return if (file.startsWith(Analyzer.self.projectDir!!)) {
            file.substring(Analyzer.self.projectDir!!.length + 1)
        } else {
            file
        }
    }


    fun projAbsPath(file: String): String {
        return if (file.startsWith("/") || file.startsWith(Analyzer.self.projectDir!!)) {
            file
        } else {
            makePathString(Analyzer.self.projectDir, file)
        }
    }


    fun joinPath(dir: File, file: String): File {
        return joinPath(dir.absolutePath, file)
    }


    fun joinPath(dir: String, file: String): File {
        val file1 = File(dir)
        return File(file1, file)
    }

    fun banner(msg: String): String {
        return "---------------- $msg ----------------"
    }


    fun printMem(bytes: Long): String {
        val dbytes = bytes.toDouble()
        val df = DecimalFormat("#.##")

        return if (dbytes < 1024) {
            df.format(bytes)
        } else if (dbytes < 1024 * 1024) {
            df.format(dbytes / 1024)
        } else if (dbytes < 1024 * 1024 * 1024) {
            df.format(dbytes / 1024.0 / 1024.0) + "M"
        } else if (dbytes < 1024 * 1024 * 1024 * 1024L) {
            df.format(dbytes / 1024.0 / 1024.0 / 1024.0) + "G"
        } else {
            "Too big to show you"
        }
    }

    fun correlateBindings(bindings: List<Binding>): List<List<Binding>> {
        val bdHash = HashMap<Int, List<Binding>>()
        for (b in bindings) {
            val hash = b.hashCode()
            if (!bdHash.containsKey(hash)) {
                bdHash[hash] = ArrayList()
            }
            val bs = bdHash[hash]
            bs.add(b)
        }
        return ArrayList(bdHash.values)
    }

    fun deleteFile(file: String): Boolean {
        return File(file).delete()
    }

    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
        }

    }

}
