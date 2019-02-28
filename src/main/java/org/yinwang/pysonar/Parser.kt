package org.yinwang.pysonar

import org.apache.commons.io.FileUtils
import org.yinwang.pysonar.ast.*

import java.io.File
import java.io.OutputStreamWriter
import java.net.URL
import java.util.ArrayList

import com.google.gson.Gson
import com.google.gson.GsonBuilder


class Parser {

    internal var python2Process: Process? = null
    internal var python3Process: Process? = null
    private val exchangeFile: String
    private val endMark: String
    private val jsonizer: String
    private val parserLog: String
    private var file: String? = null
    private var content: String? = null

    private var logCount = 0

    init {
        exchangeFile = `$`.getTempFile("json")
        endMark = `$`.getTempFile("end")
        jsonizer = `$`.getTempFile("dump_python")
        parserLog = `$`.getTempFile("parser_log")

        startPythonProcesses()
    }


    // start or restart python processes
    private fun startPythonProcesses() {
        if (python2Process != null) {
            python2Process!!.destroy()
        }
        if (python3Process != null) {
            python3Process!!.destroy()
        }

        // copy dump_python.py to temp dir
        try {
            val url = Thread.currentThread().contextClassLoader.getResource(dumpPythonResource)
            FileUtils.copyURLToFile(url!!, File(jsonizer))
        } catch (e: Exception) {
            `$`.die("Failed to copy resource file:$dumpPythonResource")
        }

        python2Process = startInterpreter(PYTHON2_EXE)
        if (python2Process != null) {
            `$`.msg("started: $PYTHON2_EXE")
        }

        python3Process = startInterpreter(PYTHON3_EXE)
        if (python3Process != null) {
            `$`.msg("started: $PYTHON3_EXE")
        }

        if (python2Process == null && python3Process == null) {
            `$`.die("You don't seem to have either of Python or Python3 on PATH")
        }
    }


    fun close() {
        if (python2Process != null) {
            python2Process!!.destroy()
        }

        if (python3Process != null) {
            python3Process!!.destroy()
        }

        if (!Analyzer.self.hasOption("debug")) {
            File(exchangeFile).delete()
            File(endMark).delete()
            File(jsonizer).delete()
            File(parserLog).delete()
        }
    }


    fun convert(o: Any): Node? {
        if (o !is Map<*, *>) {
            return null
        }

        val map = o as Map<String, Any>

        val type = map["type"] as String
        val startDouble = map["start"] as Double
        val endDouble = map["end"] as Double
        val lineDouble = map["lineno"] as Double
        val colDouble = map["col_offset"] as Double

        val start = startDouble?.toInt() ?: 0
        val end = endDouble?.toInt() ?: 1
        val line = lineDouble?.toInt() ?: 1
        val col = if (colDouble == null) 1 else colDouble.toInt() + 1


        if (type == "Module") {
            val b = convertBlock(map["body"])
            return Module(b, file, start, end, line, col)
        }

        if (type == "alias") {         // lower case alias
            val qname = map["name"] as String
            val names = segmentQname(qname, start + "import ".length, false)
            val asname = if (map["asname"] == null) null else Name(map["asname"] as String)
            return Alias(names, asname!!, file!!, start, end, line, col)
        }

        if (type == "Assert") {
            val test = convert(map["test"])
            val msg = convert(map["msg"])
            return Assert(test!!, msg!!, file!!, start, end, line, col)
        }

        // assign could be x=y=z=1
        // turn it into one or more Assign nodes
        // z = 1; y = z; x = z
        if (type == "Assign") {
            val targets = convertList<Node>(map["targets"])
            val value = convert(map["value"])
            if (targets!!.size == 1) {
                return Assign(targets[0], value!!, file!!, start, end, line, col)
            } else {
                val assignments = ArrayList<Node>()
                val lastTarget = targets[targets.size - 1]
                assignments.add(Assign(lastTarget, value!!, file!!, start, end, line, col))

                for (i in targets.size - 2 downTo 0) {
                    val nextAssign = Assign(targets[i], lastTarget, file!!, start, end, line, col)
                    assignments.add(nextAssign)
                }

                return Block(assignments, file!!, start, end, line, col)
            }
        }

        if (type == "Attribute") {
            val value = convert(map["value"])
            var attr = convert(map["attr_name"]) as Name?
            if (attr == null) {
                attr = Name(map["attr"] as String)
            }
            return Attribute(value!!, attr, file!!, start, end, line, col)
        }

        if (type == "AugAssign") {
            val target = convert(map["target"])
            val value = convert(map["value"])
            val op = convertOp(map["op"])
            val operation = BinOp(op, target!!, value!!, file!!, target.start, value.end, value.line, value.col)
            return Assign(target, operation, file!!, start, end, line, col)
        }

        if (type == "BinOp") {
            val left = convert(map["left"])
            val right = convert(map["right"])
            val op = convertOp(map["op"])

            // desugar complex operators
            if (op === Op.NotEqual) {
                val eq = BinOp(Op.Equal, left!!, right!!, file!!, start, end, line, col)
                return UnaryOp(Op.Not, eq, file!!, start, end, line, col)
            }

            if (op === Op.LtE) {
                val lt = BinOp(Op.Lt, left!!, right!!, file!!, start, end, line, col)
                val eq = BinOp(Op.Eq, left, right, file!!, start, end, line, col)
                return BinOp(Op.Or, lt, eq, file!!, start, end, line, col)
            }

            if (op === Op.GtE) {
                val gt = BinOp(Op.Gt, left!!, right!!, file!!, start, end, line, col)
                val eq = BinOp(Op.Eq, left, right, file!!, start, end, line, col)
                return BinOp(Op.Or, gt, eq, file!!, start, end, line, col)
            }

            if (op === Op.NotIn) {
                val `in` = BinOp(Op.In, left!!, right!!, file!!, start, end, line, col)
                return UnaryOp(Op.Not, `in`, file!!, start, end, line, col)
            }

            if (op === Op.NotEq) {
                val `in` = BinOp(Op.Eq, left!!, right!!, file!!, start, end, line, col)
                return UnaryOp(Op.Not, `in`, file!!, start, end, line, col)
            }

            return BinOp(op, left!!, right!!, file!!, start, end, line, col)

        }

        if (type == "BoolOp") {
            val values = convertList<Node>(map["values"])
            if (values == null || values.size < 2) {
                `$`.die("impossible number of arguments, please fix the Python parser")
            }
            val op = convertOp(map["op"])
            var ret = BinOp(op, values!![0], values[1], file!!, start, end, line, col)
            for (i in 2 until values.size) {
                ret = BinOp(op, ret, values[i], file!!, start, end, line, col)
            }
            return ret
        }

        if (type == "Bytes") {
            val s = map["s"]
            return Bytes(s, file!!, start, end, line, col)
        }

        if (type == "Call") {
            val func = convert(map["func"])
            val args = convertList<Node>(map["args"])
            val keywords = convertList<Keyword>(map["keywords"])
            val kwargs = convert(map["kwarg"])
            val starargs = convert(map["starargs"])
            return Call(func!!, args!!, keywords, kwargs!!, starargs!!, file!!, start, end, line, col)
        }

        if (type == "ClassDef") {
            val name = convert(map["name_node"]) as Name?      // hack
            val bases = convertList<Node>(map["bases"])
            val body = convertBlock(map["body"])
            return ClassDef(name!!, bases!!, body!!, file!!, start, end, line, col)
        }

        // left-fold Compare into
        if (type == "Compare") {
            val left = convert(map["left"])
            val ops = convertListOp(map["ops"])
            val comparators = convertList<Node>(map["comparators"])
            var result: Node = BinOp(ops!![0], left!!, comparators!![0], file!!, start, end, line, col)
            for (i in 1 until comparators.size) {
                val compNext = BinOp(ops[i], comparators[i - 1], comparators[i], file!!, start, end, line, col)
                result = BinOp(Op.And, result, compNext, file!!, start, end, line, col)
            }
            return result
        }

        if (type == "comprehension") {
            val target = convert(map["target"])
            val iter = convert(map["iter"])
            val ifs = convertList<Node>(map["ifs"])
            return Comprehension(target!!, iter!!, ifs!!, file!!, start, end, line, col)
        }

        if (type == "Break") {
            return Break(file!!, start, end, line, col)
        }

        if (type == "Continue") {
            return Continue(file!!, start, end, line, col)
        }

        if (type == "Delete") {
            val targets = convertList<Node>(map["targets"])
            return Delete(targets!!, file!!, start, end, line, col)
        }

        if (type == "Dict") {
            val keys = convertList<Node>(map["keys"])
            val values = convertList<Node>(map["values"])
            return Dict(keys!!, values!!, file!!, start, end, line, col)
        }

        if (type == "DictComp") {
            val key = convert(map["key"])
            val value = convert(map["value"])
            val generators = convertList<Comprehension>(map["generators"])
            return DictComp(key!!, value!!, generators!!, file!!, start, end, line, col)
        }

        if (type == "Ellipsis") {
            return Ellipsis(file!!, start, end, line, col)
        }

        if (type == "ExceptHandler") {
            val exception = convert(map["type"])
            val exceptions: MutableList<Node>?

            if (exception != null) {
                exceptions = ArrayList()
                exceptions.add(exception)
            } else {
                exceptions = null
            }

            val binder = convert(map["name"])
            val body = convertBlock(map["body"])
            return Handler(exceptions!!, binder!!, body!!, file!!, start, end, line, col)
        }

        if (type == "Exec") {
            val body = convert(map["body"])
            val globals = convert(map["globals"])
            val locals = convert(map["locals"])
            return Exec(body!!, globals!!, locals!!, file!!, start, end, line, col)
        }

        if (type == "Expr") {
            val value = convert(map["value"])
            return Expr(value!!, file!!, start, end, line, col)
        }

        if (type == "For" || type == "AsyncFor") {
            val target = convert(map["target"])
            val iter = convert(map["iter"])
            val body = convertBlock(map["body"])
            val orelse = convertBlock(map["orelse"])
            return For(target!!, iter!!, body!!, orelse!!, type == "AsyncFor", file!!, start, end, line, col)
        }

        if (type == "FunctionDef" || type == "Lambda" || type == "AsyncFunctionDef") {
            val name = if (type == "Lambda") null else convert(map["name_node"]) as Name?
            val argsMap = map["args"] as Map<String, Any>
            val args = convertList<Node>(argsMap["args"])
            val defaults = convertList<Node>(argsMap["defaults"])
            val body = if (type == "Lambda") convert(map["body"]) else convertBlock(map["body"])

            // handle vararg depending on different python versions
            var vararg: Name? = null
            val varargObj = argsMap["vararg"]
            if (varargObj is String) {
                vararg = Name(argsMap["vararg"] as String)
            } else if (varargObj is Map<*, *>) {
                val argName = varargObj.get("arg") as String
                vararg = Name(argName)
            }

            // handle kwarg depending on different python versions
            var kwarg: Name? = null
            val kwargObj = argsMap["kwarg"]
            if (kwargObj is String) {
                kwarg = Name(argsMap["kwarg"] as String)
            } else if (kwargObj is Map<*, *>) {
                val argName = kwargObj.get("arg") as String
                kwarg = Name(argName)
            }

            val isAsync = type == "AsyncFunctionDef"

            var decors: List<Node>? = ArrayList()
            if (map.containsKey("decorator_list")) {
                decors = convertList(map["decorator_list"])
            }

            return FunctionDef(name, args!!, body!!, defaults!!, vararg, kwarg, decors!!, file!!, isAsync, start, end, line, col)
        }

        if (type == "GeneratorExp") {
            val elt = convert(map["elt"])
            val generators = convertList<Comprehension>(map["generators"])
            return GeneratorExp(elt!!, generators!!, file!!, start, end, line, col)
        }

        if (type == "Global") {
            val names = map["names"] as List<String>
            val nameNodes = ArrayList<Name>()
            for (name in names) {
                nameNodes.add(Name(name))
            }
            return Global(nameNodes, file!!, start, end, line, col)
        }

        if (type == "Nonlocal") {
            val names = map["names"] as List<String>
            val nameNodes = ArrayList<Name>()
            for (name in names) {
                nameNodes.add(Name(name))
            }
            return Global(nameNodes, file!!, start, end, line, col)
        }

        if (type == "If") {
            val test = convert(map["test"])
            val body = convertBlock(map["body"])
            val orelse = convertBlock(map["orelse"])
            return If(test!!, body!!, orelse!!, file!!, start, end, line, col)
        }

        if (type == "IfExp") {
            val test = convert(map["test"])
            val body = convert(map["body"])
            val orelse = convert(map["orelse"])
            return IfExp(test!!, body!!, orelse!!, file!!, start, end, line, col)
        }


        if (type == "Import") {
            val aliases = convertList<Alias>(map["names"])
            locateNames(aliases!!, start)
            return Import(aliases, file!!, start, end, line, col)
        }

        if (type == "ImportFrom") {
            val module = map["module"] as String
            val level = (map["level"] as Double).toInt()
            val moduleSeg = if (module == null) null else segmentQname(module, start + "from ".length + level, true)
            val names = convertList<Alias>(map["names"])
            locateNames(names!!, start)
            return ImportFrom(moduleSeg!!, names, level, file!!, start, end, line, col)
        }

        if (type == "Index") {
            val value = convert(map["value"])
            return Index(value!!, file!!, start, end, line, col)
        }

        if (type == "keyword") {
            val arg = map["arg"] as String
            val value = convert(map["value"])
            return Keyword(arg, value!!, file!!, start, end, line, col)
        }

        if (type == "List") {
            val elts = convertList<Node>(map["elts"])
            return PyList(elts!!, file!!, start, end, line, col)
        }

        if (type == "Starred") { // f(*[1, 2, 3, 4])
            val value = convert(map["value"])
            return Starred(value!!, file!!, start, end, line, col)
        }

        if (type == "ListComp") {
            val elt = convert(map["elt"])
            val generators = convertList<Comprehension>(map["generators"])
            return ListComp(elt!!, generators!!, file!!, start, end, line, col)
        }

        if (type == "Name") {
            val id = map["id"] as String
            return Name(id, file, start, end, line, col)
        }

        if (type == "NameConstant") {
            val strVal: String
            val value = map["value"]
            if (value == null) {
                strVal = "None"
            } else if (value is Boolean) {
                strVal = if (value) "True" else "False"
            } else if (value is String) {
                strVal = value
            } else {
                `$`.msg("[WARNING] NameConstant contains unrecognized value: $value, please report issue")
                strVal = ""
            }
            return Name(strVal, file, start, end, line, col)
        }

        // another name for Name in Python3 func parameters?
        if (type == "arg") {
            val id = map["arg"] as String
            return Name(id, file, start, end, line, col)
        }

        if (type == "Num") {

            val num_type = map["num_type"] as String
            if (num_type == "int") {
                return PyInt(map["n"] as String, file!!, start, end, line, col)
            } else if (num_type == "float") {
                return PyFloat(map["n"] as String, file!!, start, end, line, col)
            } else {
                var real = map["real"]
                var imag = map["imag"]

                if (real is String) {
                    if (real == "Infinity") {
                        real = java.lang.Double.POSITIVE_INFINITY
                    } else if (real == "-Infinity") {
                        real = java.lang.Double.NEGATIVE_INFINITY
                    }
                }
                if (imag is String) {
                    if (imag == "Infinity") {
                        imag = java.lang.Double.POSITIVE_INFINITY
                    } else if (real == "-Infinity") {
                        imag = java.lang.Double.NEGATIVE_INFINITY
                    }
                }
                return PyComplex(real as Double, imag as Double, file!!, start, end, line, col)
            }
        }

        if (type == "SetComp") {
            val elt = convert(map["elt"])
            val generators = convertList<Comprehension>(map["generators"])
            return SetComp(elt!!, generators!!, file!!, start, end, line, col)
        }

        if (type == "Pass") {
            return Pass(file!!, start, end, line, col)
        }

        if (type == "Print") {
            val values = convertList<Node>(map["values"])
            val destination = convert(map["destination"])
            return Print(destination!!, values!!, file!!, start, end, line, col)
        }

        if (type == "Raise") {
            val exceptionType = convert(map["type"])
            val inst = convert(map["inst"])
            val tback = convert(map["tback"])
            return Raise(exceptionType!!, inst!!, tback!!, file!!, start, end, line, col)
        }

        if (type == "Repr") {
            val value = convert(map["value"])
            return Repr(value!!, file!!, start, end, line, col)
        }

        if (type == "Return") {
            val value = convert(map["value"])
            return Return(value!!, file!!, start, end, line, col)
        }

        if (type == "Await") {
            val value = convert(map["value"])
            return Return(value!!, file!!, start, end, line, col)
        }

        if (type == "Set") {
            val elts = convertList<Node>(map["elts"])
            return PySet(elts!!, file!!, start, end, line, col)
        }

        if (type == "SetComp") {
            val elt = convert(map["elt"])
            val generators = convertList<Comprehension>(map["generators"])
            return SetComp(elt!!, generators!!, file!!, start, end, line, col)
        }

        if (type == "Slice") {
            val lower = convert(map["lower"])
            val step = convert(map["step"])
            val upper = convert(map["upper"])
            return Slice(lower!!, step!!, upper!!, file!!, start, end, line, col)
        }

        if (type == "ExtSlice") {
            val dims = convertList<Node>(map["dims"])
            return ExtSlice(dims!!, file!!, start, end, line, col)
        }

        if (type == "Str") {
            var s = map["s"] as String
            if (s.length >= 6 && s.startsWith("\"\"\"") && s.endsWith("\"\"\"")) {
                s = s.substring(3, s.length - 3)
            } else if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length - 1)
            }
            return Str(s, file!!, start, end, line, col)
        }

        if (type == "Subscript") {
            val value = convert(map["value"])
            val slice = convert(map["slice"])
            return Subscript(value!!, slice, file!!, start, end, line, col)
        }

        if (type == "Try") {
            val body = convertBlock(map["body"])
            val orelse = convertBlock(map["orelse"])
            val handlers = convertList<Handler>(map["handlers"])
            val finalbody = convertBlock(map["finalbody"])
            return Try(handlers!!, body!!, orelse!!, finalbody!!, file!!, start, end, line, col)
        }

        if (type == "TryExcept") {
            val body = convertBlock(map["body"])
            val orelse = convertBlock(map["orelse"])
            val handlers = convertList<Handler>(map["handlers"])
            return Try(handlers!!, body!!, orelse!!, null!!, file!!, start, end, line, col)
        }

        if (type == "TryFinally") {
            val body = convertBlock(map["body"])
            val finalbody = convertBlock(map["finalbody"])
            return Try(null!!, body!!, null!!, finalbody!!, file!!, start, end, line, col)
        }

        if (type == "Tuple") {
            val elts = convertList<Node>(map["elts"])
            return Tuple(elts!!, file!!, start, end, line, col)
        }

        if (type == "UnaryOp") {
            val op = convertOp(map["op"])
            val operand = convert(map["operand"])
            return UnaryOp(op, operand!!, file!!, start, end, line, col)
        }

        if (type == "While") {
            val test = convert(map["test"])
            val body = convertBlock(map["body"])
            val orelse = convertBlock(map["orelse"])
            return While(test!!, body!!, orelse!!, file!!, start, end, line, col)
        }

        if (type == "With" || type == "AsyncWith") {
            val items = ArrayList<Withitem>()

            var context_expr = convert(map["context_expr"])
            var optional_vars = convert(map["optional_vars"])
            val body = convertBlock(map["body"])

            // Python 3 puts context_expr and optional_vars inside "items"
            if (context_expr != null) {
                val item = Withitem(context_expr, optional_vars, file!!, -1, -1, -1, -1)
                items.add(item)
            } else {
                val itemsMap = map["items"] as List<Map<String, Any>>

                for (m in itemsMap) {
                    context_expr = convert(m["context_expr"])
                    optional_vars = convert(m["optional_vars"])
                    val item = Withitem(context_expr!!, optional_vars, file!!, -1, -1, -1, -1)
                    items.add(item)
                }
            }

            val isAsync = type == "AsyncWith"
            return With(items, body!!, file!!, isAsync, start, end, line, col)
        }

        if (type == "Yield") {
            val value = convert(map["value"])
            return Yield(value!!, file!!, start, end, line, col)
        }

        if (type == "YieldFrom") {
            val value = convert(map["value"])
            return Yield(value!!, file!!, start, end, line, col)
        }

        `$`.msg("\n[Please Report]: unexpected ast node: " + map["type"])
        return Unsupported(file!!, start, end, line, col)
    }


    private fun <T> convertList(o: Any?): List<T>? {
        if (o == null) {
            return null
        } else {
            val `in` = o as List<Map<String, Any>>?
            val out = ArrayList<T>()

            for (m in `in`!!) {
                val n = convert(m)
                if (n != null) {
                    out.add(n as T?)
                }
            }

            return out
        }
    }


    // cpython ast doesn't have location information for names in the Alias node, thus we need to locate it here.
    private fun locateNames(names: List<Alias>, start: Int) {
        var start = start
        for (a in names) {
            for (name in a.name) {
                start = content!!.indexOf(name.id, start)
                name.start = start
                name.end = start + name.id.length
                start = name.end
                if (a.asname != null) {
                    start = content!!.indexOf(a.asname.id, start)
                    a.asname.start = start
                    a.asname.end = start + a.asname.id.length
                    a.asname.file = file  // file is missing for asname node
                    start = a.asname.end
                }
            }
        }
    }


    private fun convertListNode(o: Any?): List<Node>? {
        if (o == null) {
            return null
        } else {
            val `in` = o as List<Map<String, Any>>?
            val out = ArrayList<Node>()

            for (m in `in`!!) {
                val n = convert(m)
                if (n != null) {
                    out.add(n)
                }
            }

            return out
        }
    }


    private fun convertBlock(o: Any?): Block? {
        if (o == null) {
            return null
        } else {
            val body = convertListNode(o)
            return if (body == null || body.isEmpty()) {
                null
            } else {
                Block(body, file!!, 0, 0, 0, 0)
            }
        }
    }


    private fun convertListOp(o: Any?): List<Op>? {
        if (o == null) {
            return null
        } else {
            val `in` = o as List<Map<String, Any>>?
            val out = ArrayList<Op>()

            for (m in `in`!!) {
                val n = convertOp(m)
                if (n != null) {
                    out.add(n)
                }
            }

            return out
        }
    }


    fun convertOp(map: Any): Op {
        val type = (map as Map<String, Any>)["type"] as String

        if (type == "Add" || type == "UAdd") {
            return Op.Add
        }

        if (type == "Sub" || type == "USub") {
            return Op.Sub
        }

        if (type == "Mult") {
            return Op.Mul
        }

        if (type == "MatMult") {
            return Op.MatMult
        }

        if (type == "Div") {
            return Op.Div
        }

        if (type == "Pow") {
            return Op.Pow
        }

        if (type == "Eq") {
            return Op.Equal
        }

        if (type == "Is") {
            return Op.Eq
        }

        if (type == "Lt") {
            return Op.Lt
        }

        if (type == "Gt") {
            return Op.Gt
        }


        if (type == "BitAnd") {
            return Op.BitAnd
        }

        if (type == "BitOr") {
            return Op.BitOr
        }

        if (type == "BitXor") {
            return Op.BitXor
        }


        if (type == "In") {
            return Op.In
        }


        if (type == "LShift") {
            return Op.LShift
        }

        if (type == "FloorDiv") {
            return Op.FloorDiv
        }

        if (type == "Mod") {
            return Op.Mod
        }

        if (type == "RShift") {
            return Op.RShift
        }

        if (type == "Invert") {
            return Op.Invert
        }

        if (type == "And") {
            return Op.And
        }

        if (type == "Or") {
            return Op.Or
        }

        if (type == "Not") {
            return Op.Not
        }

        if (type == "NotEq") {
            return Op.NotEqual
        }

        if (type == "IsNot") {
            return Op.NotEq
        }

        if (type == "LtE") {
            return Op.LtE
        }

        if (type == "GtE") {
            return Op.GtE
        }

        if (type == "NotIn") {
            return Op.NotIn
        }

        `$`.msg("[please report] unsupported operator: $type")
        return Op.Unsupported
    }


    internal fun segmentQname(qname: String, start: Int, hasLoc: Boolean): List<Name> {
        val result = ArrayList<Name>()

        var i = 0
        while (i < qname.length) {
            var name = ""
            while (Character.isSpaceChar(qname[i])) {
                i++
            }
            val nameStart = i

            while (i < qname.length &&
                    (Character.isJavaIdentifierPart(qname[i]) || qname[i] == '*') &&
                    qname[i] != '.') {
                name += qname[i]
                i++
            }

            val nameStop = i
            val nstart = if (hasLoc) start + nameStart else -1
            val nstop = if (hasLoc) start + nameStop else -1
            result.add(Name(name, file, nstart, nstop, 0, 0))
            i++
        }

        return result
    }


    fun prettyJson(json: String): String {
        val obj = gson.fromJson<Map<*, *>>(json, Map<*, *>::class.java!!)
        return gson.toJson(obj)
    }

    fun startInterpreter(pythonExe: String): Process? {
        val p: Process
        try {
            val builder = ProcessBuilder(pythonExe, "-i", jsonizer)
            builder.redirectErrorStream(true)
            builder.redirectOutput(File(parserLog + "-" + logCount++))
            builder.environment().remove("PYTHONPATH")
            p = builder.start()
        } catch (e: Exception) {
            `$`.msg("Failed to start: $pythonExe")
            return null
        }

        return p
    }


    fun parseFile(filename: String): Node? {
        file = filename
        content = `$`.readFile(filename)

        val node2 = parseFileInner(filename, python2Process!!)
        if (node2 != null) {
            return node2
        } else if (python3Process != null) {
            val node3 = parseFileInner(filename, python3Process!!)
            if (node3 == null) {
                Analyzer.self.failedToParse.add(filename)
                return null
            } else {
                return node3
            }
        } else {
            Analyzer.self.failedToParse.add(filename)
            return null
        }
    }


    fun parseFileInner(filename: String, pythonProcess: Process): Node? {
        //        _.msg("parsing: " + filename);

        val exchange = File(exchangeFile)
        val marker = File(endMark)
        cleanTemp()

        val s1 = `$`.escapeWindowsPath(filename)
        val s2 = `$`.escapeWindowsPath(exchangeFile)
        val s3 = `$`.escapeWindowsPath(endMark)
        val dumpCommand = "parse_dump('$s1', '$s2', '$s3')"

        if (!sendCommand(dumpCommand, pythonProcess)) {
            cleanTemp()
            return null
        }

        val waitStart = System.currentTimeMillis()
        while (!marker.exists()) {
            if (System.currentTimeMillis() - waitStart > TIMEOUT) {
                Analyzer.self.failedToParse.add(filename)
                cleanTemp()
                startPythonProcesses()
                return null
            }

            try {
                Thread.sleep(1)
            } catch (e: Exception) {
                cleanTemp()
                return null
            }

        }

        val json = `$`.readFile(exchangeFile)
        if (json != null) {
            cleanTemp()
            val map = gson.fromJson<Map<*, *>>(json, Map<*, *>::class.java!!)
            return convert(map)
        } else {
            cleanTemp()
            return null
        }
    }


    private fun cleanTemp() {
        File(exchangeFile).delete()
        File(endMark).delete()
    }


    private fun sendCommand(cmd: String, pythonProcess: Process): Boolean {
        try {
            val writer = OutputStreamWriter(pythonProcess.outputStream)
            writer.write(cmd)
            writer.write("\n")
            writer.flush()
            return true
        } catch (e: Exception) {
            `$`.msg("\nFailed to send command to interpreter: $cmd")
            return false
        }

    }

    companion object {

        private val PYTHON2_EXE = "python"
        private val PYTHON3_EXE = "python3"
        private val TIMEOUT = 30000
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val dumpPythonResource = "org/yinwang/pysonar/python/dump_python.py"
    }

}
