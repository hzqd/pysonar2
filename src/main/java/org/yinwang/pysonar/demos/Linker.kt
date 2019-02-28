package org.yinwang.pysonar.demos

import org.yinwang.pysonar.*
import org.yinwang.pysonar.ast.Node
import org.yinwang.pysonar.types.ModuleType
import org.yinwang.pysonar.types.Type
import org.yinwang.pysonar.types.UnionType

import java.io.File
import java.util.*
import kotlin.collections.Map.Entry
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Collects per-file hyperlinks, as well as styles that require the
 * symbol table to resolve properly.
 */
internal class Linker
/**
 * Constructor.
 *
 * @param root   the root of the directory tree being indexed
 * @param outdir the html output directory
 */
(private val rootPath: String, private val outDir: File  // where we're generating the output html
) {

    // Map of file-path to semantic styles & links for that path.
    private val fileStyles = HashMap<String, List<Style>>()

    // prevent duplication in def and ref links
    var seenDef: MutableSet<Any> = HashSet()
    var seenRef: MutableSet<Any> = HashSet()

    fun findLinks(analyzer: Analyzer) {
        `$`.msg("Adding xref links")
        var progress = Progress(analyzer.getAllBindings().size.toLong(), 50)
        val linkBindings = ArrayList<Binding>()

        for (binding in analyzer.getAllBindings()) {
            if (binding.kind != Binding.Kind.MODULE) {
                linkBindings.add(binding)
            }
        }

        for (bs in `$`.correlateBindings(linkBindings)) {
            processDef(bs)
            progress.tick()
        }

        // highlight definitions
        `$`.msg("\nAdding ref links")
        progress = Progress(analyzer.references.size.toLong(), 50)

        for ((key, value) in analyzer.references) {
            if (Analyzer.self.hasOption("debug")) {
                processRefDebug(key, value)
            } else {
                processRef(key, value)
            }
            progress.tick()
        }

        if (Analyzer.self.hasOption("report")) {
            for (ld in analyzer.semanticErrors.values) {
                for (d in ld) {
                    processDiagnostic(d)
                }
            }
        }
    }


    private fun processDef(bindings: List<Binding>) {
        val first = bindings[0]
        val qname = first.qname

        if (first.isURL || first.start < 0) {
            return
        }

        val types = bindings.stream().map { b -> b.type }.collect<List<Type>, Any>(Collectors.toList())
        val style = Style(Style.Type.ANCHOR, first.start, first.end)
        style.message = UnionType.union(types).toString()
        style.url = first.qname
        style.id = qname
        addFileStyle(first.file, style)
    }


    private fun processDefDebug(binding: Binding) {
        val hash = binding.hashCode()

        if (binding.isURL || binding.start < 0 || seenDef.contains(hash)) {
            return
        }

        seenDef.add(hash)
        val style = Style(Style.Type.ANCHOR, binding.start, binding.end)
        style.message = binding.type.toString()
        style.url = binding.qname
        style.id = "" + Math.abs(binding.hashCode())

        val refs = binding.refs
        style.highlight = ArrayList()


        for (r in refs) {
            style.highlight!!.add(Integer.toString(Math.abs(r.hashCode())))
        }
        addFileStyle(binding.file, style)
    }


    fun processRef(ref: Node, bindings: List<Binding>) {
        val qname = bindings.iterator().next().qname
        val hash = ref.hashCode()

        if (!seenRef.contains(hash)) {
            seenRef.add(hash)

            val link = Style(Style.Type.LINK, ref.start, ref.end)
            link.id = qname

            val types = bindings.stream().map { b -> b.type }.collect<List<Type>, Any>(Collectors.toList())
            link.message = UnionType.union(types).toString()

            // Currently jump to the first binding only. Should change to have a
            // hover menu or something later.
            val path = ref.file
            if (path != null) {
                for (b in bindings) {
                    if (link.url == null) {
                        link.url = toURL(b, path)
                    }

                    if (link.url != null) {
                        addFileStyle(path, link)
                        break
                    }
                }
            }
        }
    }


    fun processRefDebug(ref: Node, bindings: List<Binding>) {
        val hash = ref.hashCode()

        if (!seenRef.contains(hash)) {
            seenRef.add(hash)

            val link = Style(Style.Type.LINK, ref.start, ref.end)
            link.id = Integer.toString(Math.abs(hash))

            val typings = ArrayList<String>()
            for (b in bindings) {
                typings.add(b.type.toString())
            }
            link.message = `$`.joinWithSep(typings, " | ", "{", "}")

            link.highlight = ArrayList()
            for (b in bindings) {
                link.highlight!!.add(Integer.toString(Math.abs(b.hashCode())))
            }

            // Currently jump to the first binding only. Should change to have a
            // hover menu or something later.
            val path = ref.file
            if (path != null) {
                for (b in bindings) {
                    if (link.url == null) {
                        link.url = toURL(b, path)
                    }

                    if (link.url != null) {
                        addFileStyle(path, link)
                        break
                    }
                }
            }
        }
    }


    /**
     * Returns the styles (links and extra styles) generated for a given file.
     *
     * @param path an absolute source path
     * @return a possibly-empty list of styles for that path
     */
    fun getStyles(path: String): List<Style> {
        return stylesForFile(path)
    }


    private fun stylesForFile(path: String?): MutableList<Style> {
        var styles: List<Style>? = fileStyles.get(path)
        if (styles == null) {
            styles = ArrayList()
            fileStyles[path] = styles
        }
        return styles
    }


    private fun addFileStyle(path: String?, style: Style) {
        stylesForFile(path).add(style)
    }


    /**
     * Add additional highlighting styles based on information not evident from
     * the AST.
     */
    private fun addSemanticStyles(nb: Binding) {
        val isConst = CONSTANT.matcher(nb.name).matches()
        when (nb.kind) {
            Binding.Kind.SCOPE -> if (isConst) {
                addSemanticStyle(nb, Style.Type.CONSTANT)
            }
            Binding.Kind.VARIABLE -> addSemanticStyle(nb, if (isConst) Style.Type.CONSTANT else Style.Type.IDENTIFIER)
            Binding.Kind.PARAMETER -> addSemanticStyle(nb, Style.Type.PARAMETER)
            Binding.Kind.CLASS -> addSemanticStyle(nb, Style.Type.TYPE_NAME)
        }
    }


    private fun addSemanticStyle(binding: Binding, type: Style.Type) {
        val path = binding.file
        if (path != null) {
            addFileStyle(path, Style(type, binding.start, binding.end))
        }
    }


    private fun processDiagnostic(d: Diagnostic) {
        val style = Style(Style.Type.WARNING, d.start, d.end)
        style.message = d.msg
        style.url = d.file
        addFileStyle(d.file, style)
    }


    private fun toURL(binding: Binding, filename: String?): String? {

        if (binding.isBuiltin) {
            return binding.url
        }

        val destPath: String?
        if (binding.type is ModuleType) {
            destPath = binding.type.asModuleType().file
        } else {
            destPath = binding.file
        }

        if (destPath == null) {
            return null
        }

        val anchor = "#" + binding.qname
        if (binding.firstFile == filename) {
            return anchor
        }

        if (destPath.startsWith(rootPath)) {
            val relpath: String?
            if (filename != null) {
                relpath = `$`.relPath(filename, destPath)
            } else {
                relpath = destPath
            }

            return if (relpath != null) {
                "$relpath.html$anchor"
            } else {
                anchor
            }
        } else {
            return "file://$destPath$anchor"
        }
    }

    companion object {

        private val CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*")
    }

}
