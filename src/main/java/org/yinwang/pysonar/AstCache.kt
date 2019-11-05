package org.yinwang.pysonar

import org.yinwang.pysonar.ast.PyModule
import org.yinwang.pysonar.ast.Node

import java.io.*
import java.util.HashMap
import java.util.logging.Level
import java.util.logging.Logger


/**
 * Provides a factory for python source ASTs.  Maintains configurable on-disk and
 * in-memory caches to avoid re-parsing files during analysis.
 */
class AstCache {

    private val cache = HashMap<String, Node>()
    private val parser = Parser()


    /**
     * Clears the memory cache.
     */
    fun clear() {
        cache.clear()
    }


    /**
     * Removes all serialized ASTs from the on-disk cache.
     *
     * @return `true` if all cached AST files were removed
     */
    fun clearDiskCache(): Boolean {
        try {
            `$`.deleteDirectory(File(Analyzer.self.cacheDir))
            return true
        } catch (x: Exception) {
            LOG.log(Level.SEVERE, "Failed to clear disk cache: $x")
            return false
        }

    }


    fun close() {
        parser.close()
        clearDiskCache()
    }


    /**
     * Returns the syntax tree for `path`.  May find and/or create a
     * cached copy in the mem cache or the disk cache.
     *
     * @param path absolute path to a source file
     * @return the AST, or `null` if the parse failed for any reason
     */
    fun getAST(path: String): Node? {
        // Cache stores null value if the parse failed.
        if (cache.containsKey(path)) {
            return cache[path]
        }

        // Might be cached on disk but not in memory.
        var node: Node? = getSerializedModule(path)
        if (node != null) {
            LOG.log(Level.FINE, "reusing $path")
            cache[path] = node
            return node
        }

        node = null
        try {
            LOG.log(Level.FINE, "parsing $path")
            node = parser.parseFile(path)
        } finally {
            cache[path] = node  // may be null
        }

        if (node != null) {
            serialize(node!!)
        }

        return node
    }


    /**
     * Each source file's AST is saved in an object file named for the MD5
     * checksum of the source file.  All that is needed is the MD5, but the
     * file's base name is included for ease of debugging.
     */
    fun getCachePath(sourcePath: String): String {
        return `$`.makePathString(Analyzer.self.cacheDir, `$`.getFileHash(sourcePath))
    }


    // package-private for testing
    internal fun serialize(ast: Node) {
        val path = getCachePath(ast.file)
        var oos: ObjectOutputStream? = null
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(path)
            oos = ObjectOutputStream(fos)
            oos.writeObject(ast)
        } catch (e: Exception) {
            `$`.msg("Failed to serialize: $path")
        } finally {
            try {
                oos?.close() ?: fos?.close()
            } catch (e: Exception) {
            }

        }
    }


    // package-private for testing
    internal fun getSerializedModule(sourcePath: String): PyModule? {
        if (!File(sourcePath).canRead()) {
            return null
        }
        val cached = File(getCachePath(sourcePath))
        return if (!cached.canRead()) {
            null
        } else deserialize(sourcePath)
    }


    // package-private for testing
    internal fun deserialize(sourcePath: String): PyModule? {
        val cachePath = getCachePath(sourcePath)
        var fis: FileInputStream? = null
        var ois: ObjectInputStream? = null
        try {
            fis = FileInputStream(cachePath)
            ois = ObjectInputStream(fis)
            return ois.readObject() as PyModule
        } catch (e: Exception) {
            return null
        } finally {
            try {
                ois?.close() ?: fis?.close()
            } catch (e: Exception) {

            }

        }
    }

    companion object {

        private val LOG = Logger.getLogger(AstCache::class.java!!.getCanonicalName())
    }
}
