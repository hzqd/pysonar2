package org.yinwang.pysonar

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import org.yinwang.pysonar.ast.Name
import org.yinwang.pysonar.ast.Node
import org.yinwang.pysonar.ast.Url
import org.yinwang.pysonar.types.*
import org.yinwang.pysonar.visitor.TypeInferencer

import java.io.File
import java.net.URL
import java.util.*


class Analyzer @JvmOverloads constructor(options: MutableMap<String, Any>? = null) {
    var inferencer = TypeInferencer()
    var sid = `$`.newSessionId()
    var moduleTable = State(null, State.StateType.GLOBAL)
    var loadedFiles: MutableList<String> = ArrayList()
    var globaltable = State(null, State.StateType.GLOBAL)
    var allBindings: MutableList<Binding> = ArrayList()
    var references: ListMultimap<Node, Binding> = ArrayListMultimap.create()
    var resolved: Set<Name> = HashSet()
    var unresolved: Set<Name> = HashSet()
    var semanticErrors: ListMultimap<String, Diagnostic> = ArrayListMultimap.create()
    var cwd: String? = null
    var nCalled = 0
    var multilineFunType = false
    var path: MutableList<String> = ArrayList()
    private val uncalled = HashSet<FunType>()
    private val importStack = HashSet<Any>()

    private val astCache: AstCache
    var cacheDir: String
    var failedToParse: MutableSet<String> = HashSet()
    var stats = Stats()
    var builtins: Builtins
    private var loadingProgress: Progress? = null

    var projectDir: String? = null
    var modelDir: String
    var callStack = Stack<CallStackEntry>()

    var options: MutableMap<String, Any>


    val loadPath: List<String>
        get() {
            val loadPath = ArrayList<String>()
            if (cwd != null) {
                loadPath.add(cwd)
            }
            if (projectDir != null && File(projectDir!!).isDirectory) {
                loadPath.add(projectDir)
            }
            loadPath.addAll(path)
            return loadPath
        }


    // calculate number of defs, refs, xrefs
    val analysisSummary: String
        get() {
            val sb = StringBuilder()
            sb.append("\n" + `$`.banner("analysis summary"))

            val duration = `$`.formatTime(System.currentTimeMillis() - stats.getInt("startTime")!!)
            sb.append("\n- total time: $duration")
            sb.append("\n- modules loaded: " + loadedFiles.size)
            sb.append("\n- semantic problems: " + semanticErrors.size())
            sb.append("\n- failed to parse: " + failedToParse.size)
            var nDef = 0
            var nXRef = 0
            for (b in getAllBindings()) {
                nDef += 1
                nXRef += b.refs.size
            }

            sb.append("\n- number of definitions: $nDef")
            sb.append("\n- number of cross references: $nXRef")
            sb.append("\n- number of references: " + references.size())

            val nResolved = resolved.size.toLong()
            val nUnresolved = unresolved.size.toLong()
            sb.append("\n- resolved names: $nResolved")
            sb.append("\n- unresolved names: $nUnresolved")
            sb.append("\n- name resolve rate: " + `$`.percent(nResolved, nResolved + nUnresolved))
            sb.append("\n" + `$`.gcStats)

            return sb.toString()
        }


    init {
        self = this
        if (options != null) {
            this.options = options
        } else {
            this.options = HashMap()
        }
        this.stats.putInt("startTime", System.currentTimeMillis())
        this.builtins = Builtins()
        this.builtins.init()
        this.cacheDir = createCacheDir()
        this.astCache = AstCache()
        addPythonPath()
        copyModels()
    }


    fun hasOption(option: String): Boolean {
        val op = options[option]
        return if (op != null && op == true) {
            true
        } else {
            false
        }
    }


    fun setOption(option: String) {
        options[option] = true
    }


    // main entry to the analyzer
    fun analyze(path: String) {
        val upath = `$`.unifyPath(path)
        val f = File(upath)
        projectDir = if (f.isDirectory) f.path else f.parent
        loadFileRecursive(upath)
    }


    fun setCWD(cd: String?) {
        if (cd != null) {
            cwd = `$`.unifyPath(cd)
        }
    }


    fun addPaths(p: List<String>) {
        for (s in p) {
            addPath(s)
        }
    }


    fun addPath(p: String) {
        path.add(`$`.unifyPath(p))
    }


    fun setPath(path: List<String>) {
        this.path = ArrayList(path.size)
        addPaths(path)
    }


    private fun addPythonPath() {
        val path = System.getenv("PYTHONPATH")
        if (path != null) {
            val segments = path.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            for (p in segments) {
                addPath(p)
            }
        }
    }


    private fun copyModels() {
        val resource = Thread.currentThread().contextClassLoader.getResource(Globals.MODEL_LOCATION)
        val dest = `$`.getTempFile("models")
        this.modelDir = dest

        try {
            `$`.copyResourcesRecursively(resource!!, File(dest))
            `$`.msg("copied models to: $modelDir")
        } catch (e: Exception) {
            `$`.die("Failed to copy models. Please check permissions of writing to: $dest")
        }

        addPath(dest)
    }


    fun inImportStack(f: Any): Boolean {
        return importStack.contains(f)
    }


    fun pushImportStack(f: Any) {
        importStack.add(f)
    }


    fun popImportStack(f: Any) {
        importStack.remove(f)
    }


    fun getAllBindings(): List<Binding> {
        return allBindings
    }


    internal fun getCachedModule(file: String): ModuleType? {
        val t = moduleTable.lookupType(`$`.moduleQname(file))
        if (t == null) {
            return null
        } else if (t is UnionType) {
            for (tt in t.types) {
                if (tt is ModuleType) {
                    return tt
                }
            }
            return null
        } else return if (t is ModuleType) {
            t
        } else {
            null
        }
    }


    fun getDiagnosticsForFile(file: String): List<Diagnostic> {
        val errs = semanticErrors.get(file)
        return errs ?: ArrayList()
    }


    fun putRef(node: Node, bs: Collection<Binding>) {
        if (node !is Url) {
            val bindings = references.get(node)
            for (b in bs) {
                if (!bindings.contains(b)) {
                    bindings.add(b)
                }
                b.addRef(node)
            }
        }
    }


    fun putRef(node: Node, b: Binding) {
        val bs = ArrayList<Binding>()
        bs.add(b)
        putRef(node, bs)
    }


    fun putProblem(loc: Node, msg: String) {
        val file = loc.file
        if (file != null) {
            addFileErr(file, loc.start, loc.end, msg)
        }
    }


    // for situations without a Node
    fun putProblem(file: String?, begin: Int, end: Int, msg: String) {
        if (file != null) {
            addFileErr(file, begin, end, msg)
        }
    }


    internal fun addFileErr(file: String, begin: Int, end: Int, msg: String) {
        val d = Diagnostic(file, Diagnostic.Category.ERROR, begin, end, msg)
        semanticErrors.put(file, d)
    }


    fun loadFile(path: String): Type? {
        var path = path
        path = `$`.unifyPath(path)
        val f = File(path)

        if (!f.canRead()) {
            return null
        }

        val module = getCachedModule(path)
        if (module != null) {
            return module
        }

        // detect circular import
        if (inImportStack(path)) {
            return null
        }

        // set new CWD and save the old one on stack
        val oldcwd = cwd
        setCWD(f.parent)

        pushImportStack(path)
        val type = parseAndResolve(path)
        popImportStack(path)

        // restore old CWD
        setCWD(oldcwd)
        return type
    }


    private fun parseAndResolve(file: String): Type? {
        loadingProgress!!.tick()
        val ast = getAstForFile(file)

        if (ast == null) {
            failedToParse.add(file)
            return null
        } else {
            val type = inferencer.visit(ast, moduleTable)
            loadedFiles.add(file)
            return type
        }
    }


    private fun createCacheDir(): String {
        val dir = `$`.getTempFile("ast_cache")
        val f = File(dir)
        `$`.msg("AST cache is at: $dir")

        if (!f.exists()) {
            if (!f.mkdirs()) {
                `$`.die("Failed to create tmp directory: $dir. Please check permissions")
            }
        }
        return dir
    }


    /**
     * Returns the syntax tree for `file`.
     *
     *
     */
    fun getAstForFile(file: String): Node? {
        return astCache.getAST(file)
    }


    fun getBuiltinModule(qname: String): ModuleType? {
        return builtins.get(qname)
    }


    fun makeQname(names: List<Name>): String? {
        if (names.isEmpty()) {
            return ""
        }

        var ret = ""

        for (i in 0 until names.size - 1) {
            ret += names[i].id + "."
        }

        ret += names[names.size - 1].id
        return ret
    }


    /**
     * Find the path that contains modname. Used to find the starting point of locating a qname.
     *
     * @param headName first module name segment
     */
    fun locateModule(headName: String): String? {
        val loadPath = loadPath

        for (p in loadPath) {
            val startDir = File(p, headName)
            val initFile = File(`$`.joinPath(startDir, "__init__.py").path)

            if (initFile.exists()) {
                return p
            }

            val startFile = File(startDir.toString() + Globals.FILE_SUFFIX)
            if (startFile.exists()) {
                return p
            }
        }

        return null
    }


    fun loadModule(name: List<Name>, state: State): Type? {
        if (name.isEmpty()) {
            return null
        }

        val qname = makeQname(name)

        val mt = getBuiltinModule(qname!!)
        if (mt != null) {
            state.insert(name[0].id,
                    Url(Builtins.LIBRARY_URL + mt.table.path + ".html"),
                    mt, Binding.Kind.SCOPE)
            return mt
        }

        // If there are more than one segment
        // load the packages first
        var prev: Type? = null
        val startPath = locateModule(name[0].id) ?: return null

        var path = File(startPath)

        for (i in name.indices) {
            path = File(path, name[i].id)
            val initFile = File(`$`.joinPath(path, "__init__.py").path)

            if (initFile.exists()) {
                val mod = loadFile(initFile.path) ?: return null

                val binding = Binding.createFileBinding(name[i].id, initFile.path, mod)

                prev?.table?.update(name[i].id, binding) ?: state.update(name[i].id, binding)

                Analyzer.self.putRef(name[i], binding)
                prev = mod
            } else if (i == name.size - 1) {
                val startFile = File(path.toString() + Globals.FILE_SUFFIX)
                if (startFile.exists()) {
                    val mod = loadFile(startFile.path) ?: return null

                    val binding = Binding.createFileBinding(name[i].id, startFile.path, mod)

                    prev?.table?.update(name[i].id, binding) ?: state.update(name[i].id, binding)

                    Analyzer.self.putRef(name[i], binding)
                    prev = mod
                } else {
                    return null
                }
            }
        }
        return prev
    }


    /**
     * Load all Python source files recursively if the given fullname is a
     * directory; otherwise just load a file.  Looks at file extension to
     * determine whether to load a given file.
     */
    fun loadFileRecursive(fullname: String) {
        val count = countFileRecursive(fullname)
        if (loadingProgress == null) {
            loadingProgress = Progress(count.toLong(), 50)
        }

        val file_or_dir = File(fullname)

        if (file_or_dir.isDirectory) {
            for (file in file_or_dir.listFiles()!!) {
                loadFileRecursive(file.path)
            }
        } else {
            if (file_or_dir.path.endsWith(Globals.FILE_SUFFIX)) {
                loadFile(file_or_dir.path)
            }
        }
    }


    // count number of .py files
    fun countFileRecursive(fullname: String): Int {
        val file_or_dir = File(fullname)
        var sum = 0

        if (file_or_dir.isDirectory) {
            for (file in file_or_dir.listFiles()!!) {
                sum += countFileRecursive(file.path)
            }
        } else {
            if (file_or_dir.path.endsWith(Globals.FILE_SUFFIX)) {
                sum += 1
            }
        }
        return sum
    }


    fun finish() {
        `$`.msg("\nFinished loading files. $nCalled functions were called.")
        `$`.msg("Analyzing uncalled functions")
        applyUncalled()

        // mark unused variables
        for (bset in `$`.correlateBindings(allBindings)) {
            if (unusedBindingSet(bset)) {
                val first = bset[0]
                putProblem(first.node, "Unused variable: " + first.name)
            }
        }

        `$`.msg(analysisSummary)
        close()
    }

    private fun unusedBindingSet(bindings: List<Binding>): Boolean {
        for (binding in bindings) {
            if (!unused(binding)) {
                return false
            }
        }
        return true
    }

    private fun unused(binding: Binding): Boolean {
        return (binding.type !is ClassType &&
                binding.type !is FunType &&
                binding.type !is ModuleType
                && binding.refs.isEmpty())
    }

    fun close() {
        astCache.close()
        `$`.sleep(10)
        if (!`$`.deleteDirectory(`$`.tempDir)) {
            `$`.msg("Failed to delete temp dir: " + `$`.tempDir)
        }
    }

    fun addUncalled(cl: FunType) {
        if (!cl.func.called) {
            uncalled.add(cl)
        }
    }


    fun removeUncalled(f: FunType) {
        uncalled.remove(f)
    }


    fun applyUncalled() {
        val progress = Progress(uncalled.size.toLong(), 50)

        while (!uncalled.isEmpty()) {
            val uncalledDup = ArrayList(uncalled)

            for (cl in uncalledDup) {
                progress.tick()
                inferencer.apply(cl, null, null, null, null, null, null)
            }
        }
    }


    fun getLoadedFiles(): List<String> {
        val files = ArrayList<String>()
        for (file in loadedFiles) {
            if (file.endsWith(Globals.FILE_SUFFIX)) {
                files.add(file)
            }
        }
        return files
    }


    fun registerBinding(b: Binding) {
        allBindings.add(b)
    }


    override fun toString(): String {
        return "(analyzer:" +
                "[" + allBindings.size + " bindings] " +
                "[" + references.size() + " refs] " +
                "[" + loadedFiles.size + " files] " +
                ")"
    }

    companion object {

        // global static instance of the analyzer itself
        var self: Analyzer
    }
}
