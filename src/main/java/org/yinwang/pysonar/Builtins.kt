package org.yinwang.pysonar

import org.yinwang.pysonar.ast.PyModule
import org.yinwang.pysonar.ast.Url
import org.yinwang.pysonar.types.*

import java.util.HashMap

import org.yinwang.pysonar.Binding.Kind.*

/**
 * This file is messy. Should clean up.
 */
class Builtins {


    // XXX:  need to model "types" module and reconcile with these types
    var Builtin: ModuleType
    var objectType: ClassType
    var BaseType: ClassType
    var BaseList: ClassType
    var BaseListInst: InstanceType
    var BaseArray: ClassType
    var BaseTuple: ClassType
    var BaseModule: ClassType
    var BaseFile: ClassType
    var BaseFileInst: InstanceType
    var BaseException: ClassType
    var BaseStruct: ClassType
    var BaseFunction: ClassType  // models functions, lambas and methods
    var BaseClass: ClassType  // models classes and instances

    var Datetime_datetime: ClassType
    var Datetime_date: ClassType
    var Datetime_time: ClassType
    var Datetime_timedelta: ClassType
    var Datetime_tzinfo: ClassType
    var Time_struct_time: InstanceType


    internal var builtin_exception_types = arrayOf("ArithmeticError", "AssertionError", "AttributeError", "BaseException", "Exception", "DeprecationWarning", "EOFError", "EnvironmentError", "FloatingPointError", "FutureWarning", "GeneratorExit", "IOError", "ImportError", "ImportWarning", "IndentationError", "IndexError", "KeyError", "KeyboardInterrupt", "LookupError", "MemoryError", "NameError", "NotImplemented", "NotImplementedError", "OSError", "OverflowError", "PendingDeprecationWarning", "ReferenceError", "RuntimeError", "RuntimeWarning", "StandardError", "StopIteration", "SyntaxError", "SyntaxWarning", "SystemError", "SystemExit", "TabError", "TypeError", "UnboundLocalError", "UnicodeDecodeError", "UnicodeEncodeError", "UnicodeError", "UnicodeTranslateError", "UnicodeWarning", "UserWarning", "ValueError", "Warning", "ZeroDivisionError")


    /**
     * The set of top-level native modules.
     */
    private val modules = HashMap<String, NativeModule>()


    internal fun newClass(name: String, table: State): ClassType {
        return newClass(name, table, null)
    }


    internal fun newClass(name: String, table: State?,
                          superClass: ClassType, vararg moreSupers: ClassType): ClassType {
        val t = ClassType(name, table, superClass)
        for (c in moreSupers) {
            t.addSuper(c)
        }
        return t
    }


    internal fun newModule(name: String): ModuleType? {
        return ModuleType(name, null, Analyzer.self.globaltable)
    }


    internal fun newException(name: String, t: State?): ClassType {
        return newClass(name, t, BaseException)
    }


    internal fun newFunc(): FunType {
        return FunType()
    }


    internal fun newFunc(type: Type?): FunType? {
        var type = type
        if (type == null) {
            type = Types.UNKNOWN
        }
        return FunType(Types.UNKNOWN, type)
    }


    @JvmOverloads
    internal fun newList(type: Type = Types.UNKNOWN): ListType {
        return ListType(type)
    }


    internal fun newDict(ktype: Type, vtype: Type): DictType {
        return DictType(ktype, vtype)
    }


    internal fun newTuple(vararg types: Type): TupleType {
        return TupleType(*types)
    }


    internal fun newUnion(vararg types: Type): UnionType {
        return UnionType(*types)
    }


    internal fun list(vararg names: String): Array<String> {
        return names
    }


    private abstract inner class NativeModule internal constructor(protected var name: String) {
        protected var module: ModuleType? = null
        protected var table: State? = null  // the module's symbol table


        init {
            modules[name] = this
        }


        /**
         * Lazily load the module.
         */
        internal fun getModule(): ModuleType? {
            if (module == null) {
                createModuleType()
                initBindings()
            }
            return module
        }


        abstract fun initBindings()


        protected fun createModuleType() {
            if (module == null) {
                module = newModule(name)
                table = module!!.table
                Analyzer.self.moduleTable.insert(name, liburl(), module!!, MODULE)
            }
        }


        protected fun update(name: String, url: Url, type: Type?, kind: Binding.Kind) {
            table!!.insert(name, url, type!!, kind)
        }


        protected fun addClass(name: String, url: Url, type: Type) {
            table!!.insert(name, url, type, CLASS)
        }


        protected fun addClass(type: ClassType) {
            table!!.insert(type.name, liburl(type.name), type, CLASS)
        }


        protected fun addMethod(cls: ClassType, name: String, type: Type?) {
            cls.table.insert(name, liburl(cls.name + "." + name), newFunc(type)!!, METHOD)
        }

        protected fun addMethod(cls: ClassType, name: String) {
            cls.table.insert(name, liburl(cls.name + "." + name), newFunc(), METHOD)
        }


        protected fun addFunction(module: ModuleType, name: String, type: Type?) {
            val url = if (this.module === module)
                liburl(module.qname + "." + name)
            else
                newLibUrl(module.table.path, module.table.path + "." + name)
            module.table.insert(name, url, newFunc(type)!!, FUNCTION)
        }


        protected fun addFunction(name: String, type: Type?) {
            addFunction(module!!, name, type)
        }


        // don't use this unless you're sure it's OK to share the type object
        protected fun addFunctions_beCareful(type: Type, vararg names: String) {
            for (name in names) {
                addFunction(name, type)
            }
        }


        protected fun addNoneFuncs(vararg names: String) {
            addFunctions_beCareful(Types.NoneInstance, *names)
        }


        protected fun addNumFuncs(vararg names: String) {
            addFunctions_beCareful(Types.IntInstance, *names)
        }


        protected fun addStrFuncs(vararg names: String) {
            addFunctions_beCareful(Types.StrInstance, *names)
        }


        protected fun addUnknownFuncs(vararg names: String) {
            for (name in names) {
                addFunction(name, Types.UNKNOWN)
            }
        }


        protected fun addAttr(name: String, url: Url, type: Type?) {
            table!!.insert(name, url, type!!, ATTRIBUTE)
        }


        protected fun addAttr(name: String, type: Type) {
            addAttr(table!!, name, type)
        }


        protected fun addAttr(s: State, name: String, type: Type?) {
            s.insert(name, liburl(s.path + "." + name), type!!, ATTRIBUTE)
        }


        protected fun addAttr(cls: ClassType, name: String, type: Type?) {
            addAttr(cls.table, name, type)
        }

        // don't use this unless you're sure it's OK to share the type object
        protected fun addAttributes_beCareful(type: Type, vararg names: String) {
            for (name in names) {
                addAttr(name, type)
            }
        }


        protected fun addNumAttrs(vararg names: String) {
            addAttributes_beCareful(Types.IntInstance, *names)
        }


        protected fun addStrAttrs(vararg names: String) {
            addAttributes_beCareful(Types.StrInstance, *names)
        }


        protected fun addUnknownAttrs(vararg names: String) {
            for (name in names) {
                addAttr(name, Types.UNKNOWN)
            }
        }


        protected open fun liburl(): Url {
            return newLibUrl(name)
        }


        protected open fun liburl(anchor: String): Url {
            return newLibUrl(name, anchor)
        }

        override fun toString(): String {
            return if (module == null)
                "<Non-loaded builtin module '$name'>"
            else
                "<NativeModule:$module>"
        }
    }

    init {
        buildTypes()
    }


    private fun buildTypes() {
        BuiltinsModule()
        val bt = Builtin.table

        objectType = newClass("object", bt)
        BaseType = newClass("type", bt, objectType)
        BaseTuple = newClass("tuple", bt, objectType)
        BaseList = newClass("list", bt, objectType)
        BaseListInst = InstanceType(BaseList)
        BaseArray = newClass("array", bt)
        val numClass = newClass("int", bt, objectType)
        BaseModule = newClass("module", bt)
        BaseFile = newClass("file", bt, objectType)
        BaseFileInst = InstanceType(BaseFile)
        BaseFunction = newClass("function", bt, objectType)
        BaseClass = newClass("classobj", bt, objectType)
    }


    internal fun init() {
        buildObjectType()
        buildTupleType()
        buildArrayType()
        buildListType()
        buildDictType()
        buildNumTypes()
        buildStrType()
        buildModuleType()
        buildFileType()
        buildFunctionType()
        buildClassType()

        modules["__builtin__"].initBindings()  // eagerly load these bindings

        ArrayModule()
        AudioopModule()
        BinasciiModule()
        Bz2Module()
        CPickleModule()
        CStringIOModule()
        CMathModule()
        CollectionsModule()
        CryptModule()
        CTypesModule()
        DatetimeModule()
        DbmModule()
        ErrnoModule()
        ExceptionsModule()
        FcntlModule()
        FpectlModule()
        GcModule()
        GdbmModule()
        GrpModule()
        ImpModule()
        ItertoolsModule()
        MarshalModule()
        MathModule()
        Md5Module()
        MmapModule()
        NisModule()
        OperatorModule()
        OsModule()
        ParserModule()
        PosixModule()
        PwdModule()
        PyexpatModule()
        ReadlineModule()
        ResourceModule()
        SelectModule()
        SignalModule()
        ShaModule()
        SpwdModule()
        StropModule()
        StructModule()
        SysModule()
        SyslogModule()
        TermiosModule()
        ThreadModule()
        TimeModule()
        UnicodedataModule()
        ZipimportModule()
        ZlibModule()
        UnittestModule()
    }


    /**
     * Loads (if necessary) and returns the specified built-in module.
     */
    operator fun get(name: String): ModuleType? {
        if (!name.contains(".")) {  // unqualified
            return getModule(name)
        }

        val mods = name.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        var type: Type? = getModule(mods[0]) ?: return null
        for (i in 1 until mods.size) {
            type = type!!.table.lookupType(mods[i])
            if (type !is ModuleType) {
                return null
            }
        }
        return type as ModuleType?
    }


    private fun getModule(name: String): ModuleType? {
        val wrap = modules[name]
        return wrap?.getModule()
    }


    internal fun buildObjectType() {
        val obj_methods = arrayOf("__delattr__", "__format__", "__getattribute__", "__hash__", "__init__", "__new__", "__reduce__", "__reduce_ex__", "__repr__", "__setattr__", "__sizeof__", "__str__", "__subclasshook__")
        for (m in obj_methods) {
            objectType.table.insert(m, newLibUrl("stdtypes"), newFunc(), METHOD)
        }
        objectType.table.insert("__doc__", newLibUrl("stdtypes"), Types.StrInstance, CLASS)
        objectType.table.insert("__class__", newLibUrl("stdtypes"), Types.UNKNOWN, CLASS)
    }


    internal fun buildTupleType() {
        val bt = BaseTuple.table
        val tuple_methods = arrayOf("__add__", "__contains__", "__eq__", "__ge__", "__getnewargs__", "__gt__", "__iter__", "__le__", "__len__", "__lt__", "__mul__", "__ne__", "__new__", "__rmul__", "count", "index")
        for (m in tuple_methods) {
            bt.insert(m, newLibUrl("stdtypes"), newFunc(), METHOD)
        }
        bt.insert("__getslice__", newDataModelUrl("object.__getslice__"), newFunc(), METHOD)
        bt.insert("__getitem__", newDataModelUrl("object.__getitem__"), newFunc(), METHOD)
        bt.insert("__iter__", newDataModelUrl("object.__iter__"), newFunc(), METHOD)
    }


    internal fun buildArrayType() {
        val array_methods_none = arrayOf("append", "buffer_info", "byteswap", "extend", "fromfile", "fromlist", "fromstring", "fromunicode", "index", "insert", "pop", "read", "remove", "reverse", "tofile", "tolist", "typecode", "write")
        for (m in array_methods_none) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.NoneInstance)!!, METHOD)
        }
        val array_methods_num = arrayOf("count", "itemsize")
        for (m in array_methods_num) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.IntInstance)!!, METHOD)
        }
        val array_methods_str = arrayOf("tostring", "tounicode")
        for (m in array_methods_str) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.StrInstance)!!, METHOD)
        }
    }


    internal fun buildListType() {
        BaseList.table.insert("__getslice__", newDataModelUrl("object.__getslice__"),
                newFunc(BaseListInst)!!, METHOD)
        BaseList.table.insert("__getitem__", newDataModelUrl("object.__getitem__"),
                newFunc(BaseList)!!, METHOD)
        BaseList.table.insert("__iter__", newDataModelUrl("object.__iter__"),
                newFunc(BaseList)!!, METHOD)

        val list_methods_none = arrayOf("append", "extend", "index", "insert", "pop", "remove", "reverse", "sort")
        for (m in list_methods_none) {
            BaseList.table.insert(m, newLibUrl("stdtypes"), newFunc(Types.NoneInstance)!!, METHOD)
        }
        val list_methods_num = arrayOf("count")
        for (m in list_methods_num) {
            BaseList.table.insert(m, newLibUrl("stdtypes"), newFunc(Types.IntInstance)!!, METHOD)
        }
    }


    internal fun numUrl(): Url {
        return newLibUrl("stdtypes", "typesnumeric")
    }


    internal fun buildNumTypes() {
        val bft = Types.FloatInstance.table
        val float_methods_num = arrayOf("__abs__", "__add__", "__coerce__", "__div__", "__divmod__", "__eq__", "__float__", "__floordiv__", "__format__", "__ge__", "__getformat__", "__gt__", "__int__", "__le__", "__long__", "__lt__", "__mod__", "__mul__", "__ne__", "__neg__", "__new__", "__nonzero__", "__pos__", "__pow__", "__radd__", "__rdiv__", "__rdivmod__", "__rfloordiv__", "__rmod__", "__rmul__", "__rpow__", "__rsub__", "__rtruediv__", "__setformat__", "__sub__", "__truediv__", "__trunc__", "as_integer_ratio", "fromhex", "is_integer")
        for (m in float_methods_num) {
            bft.insert(m, numUrl(), newFunc(Types.FloatInstance)!!, METHOD)
        }
        val bnt = Types.IntInstance.table
        val num_methods_num = arrayOf("__abs__", "__add__", "__and__", "__class__", "__cmp__", "__coerce__", "__delattr__", "__div__", "__divmod__", "__doc__", "__float__", "__floordiv__", "__getattribute__", "__getnewargs__", "__hash__", "__hex__", "__index__", "__init__", "__int__", "__invert__", "__long__", "__lshift__", "__mod__", "__mul__", "__neg__", "__new__", "__nonzero__", "__oct__", "__or__", "__pos__", "__pow__", "__radd__", "__rand__", "__rdiv__", "__rdivmod__", "__reduce__", "__reduce_ex__", "__repr__", "__rfloordiv__", "__rlshift__", "__rmod__", "__rmul__", "__ror__", "__rpow__", "__rrshift__", "__rshift__", "__rsub__", "__rtruediv__", "__rxor__", "__setattr__", "__str__", "__sub__", "__truediv__", "__xor__")
        for (m in num_methods_num) {
            bnt.insert(m, numUrl(), newFunc(Types.IntInstance)!!, METHOD)
        }
        bnt.insert("__getnewargs__", numUrl(), newFunc(newTuple(Types.IntInstance))!!, METHOD)
        bnt.insert("hex", numUrl(), newFunc(Types.StrInstance)!!, METHOD)
        bnt.insert("conjugate", numUrl(), newFunc(Types.ComplexInstance)!!, METHOD)

        val bct = Types.ComplexInstance.table
        val complex_methods = arrayOf("__abs__", "__add__", "__div__", "__divmod__", "__float__", "__floordiv__", "__format__", "__getformat__", "__int__", "__long__", "__mod__", "__mul__", "__neg__", "__new__", "__pos__", "__pow__", "__radd__", "__rdiv__", "__rdivmod__", "__rfloordiv__", "__rmod__", "__rmul__", "__rpow__", "__rsub__", "__rtruediv__", "__sub__", "__truediv__", "conjugate")
        for (c in complex_methods) {
            bct.insert(c, numUrl(), newFunc(Types.ComplexInstance)!!, METHOD)
        }
        val complex_methods_num = arrayOf("__eq__", "__ge__", "__gt__", "__le__", "__lt__", "__ne__", "__nonzero__", "__coerce__")
        for (cn in complex_methods_num) {
            bct.insert(cn, numUrl(), newFunc(Types.IntInstance)!!, METHOD)
        }
        bct.insert("__getnewargs__", numUrl(), newFunc(newTuple(Types.ComplexInstance))!!, METHOD)
        bct.insert("imag", numUrl(), Types.IntInstance, ATTRIBUTE)
        bct.insert("real", numUrl(), Types.IntInstance, ATTRIBUTE)
    }


    internal fun buildStrType() {
        Types.StrInstance.table.insert("__getslice__", newDataModelUrl("object.__getslice__"),
                newFunc(Types.StrInstance)!!, METHOD)
        Types.StrInstance.table.insert("__getitem__", newDataModelUrl("object.__getitem__"),
                newFunc(Types.StrInstance)!!, METHOD)
        Types.StrInstance.table.insert("__iter__", newDataModelUrl("object.__iter__"),
                newFunc(Types.StrInstance)!!, METHOD)

        val str_methods_str = arrayOf("capitalize", "center", "decode", "encode", "expandtabs", "format", "index", "join", "ljust", "lower", "lstrip", "partition", "replace", "rfind", "rindex", "rjust", "rpartition", "rsplit", "rstrip", "strip", "swapcase", "title", "translate", "upper", "zfill")
        for (m in str_methods_str) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str.$m"),
                    newFunc(Types.StrInstance)!!, METHOD)
        }

        val str_methods_num = arrayOf("count", "isalnum", "isalpha", "isdigit", "islower", "isspace", "istitle", "isupper", "find", "startswith", "endswith")
        for (m in str_methods_num) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str.$m"),
                    newFunc(Types.IntInstance)!!, METHOD)
        }

        val str_methods_list = arrayOf("split", "splitlines")
        for (m in str_methods_list) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str.$m"),
                    newFunc(newList(Types.StrInstance))!!, METHOD)
        }
        Types.StrInstance.table.insert("partition", newLibUrl("stdtypes", "str.partition"),
                newFunc(newTuple(Types.StrInstance))!!, METHOD)
    }


    internal fun buildModuleType() {
        val attrs = arrayOf("__doc__", "__file__", "__name__", "__package__")
        for (m in attrs) {
            BaseModule.table.insert(m, newTutUrl("modules.html"), Types.StrInstance, ATTRIBUTE)
        }
        BaseModule.table.insert("__dict__", newLibUrl("stdtypes", "modules"),
                newDict(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE)
    }


    internal fun buildDictType() {
        val url = "datastructures.html#dictionaries"
        val bt = Types.BaseDict.table

        bt.insert("__getitem__", newTutUrl(url), newFunc(), METHOD)
        bt.insert("__iter__", newTutUrl(url), newFunc(), METHOD)
        bt.insert("get", newTutUrl(url), newFunc(), METHOD)

        bt.insert("items", newTutUrl(url),
                newFunc(newList(newTuple(Types.UNKNOWN, Types.UNKNOWN)))!!, METHOD)

        bt.insert("keys", newTutUrl(url), newFunc(BaseList)!!, METHOD)
        bt.insert("values", newTutUrl(url), newFunc(BaseList)!!, METHOD)

        val dict_method_unknown = arrayOf("clear", "copy", "fromkeys", "get", "iteritems", "iterkeys", "itervalues", "pop", "popitem", "setdefault", "update")
        for (m in dict_method_unknown) {
            bt.insert(m, newTutUrl(url), newFunc(), METHOD)
        }

        val dict_method_num = arrayOf("has_key")
        for (m in dict_method_num) {
            bt.insert(m, newTutUrl(url), newFunc(Types.IntInstance)!!, METHOD)
        }
    }


    internal fun buildFileType() {
        val table = BaseFile.table

        table.insert("__enter__", newLibUrl("stdtypes", "contextmanager.__enter__"), newFunc(), METHOD)
        table.insert("__exit__", newLibUrl("stdtypes", "contextmanager.__exit__"), newFunc(), METHOD)
        table.insert("__iter__", newLibUrl("stdtypes", "iterator-types"), newFunc(), METHOD)

        val file_methods_unknown = arrayOf("__enter__", "__exit__", "__iter__", "flush", "readinto", "truncate")
        for (m in file_methods_unknown) {
            table.insert(m, newLibUrl("stdtypes", "file.$m"), newFunc(), METHOD)
        }

        val methods_str = arrayOf("next", "read", "readline")
        for (m in methods_str) {
            table.insert(m, newLibUrl("stdtypes", "file.$m"), newFunc(Types.StrInstance)!!, METHOD)
        }

        val num = arrayOf("fileno", "isatty", "tell")
        for (m in num) {
            table.insert(m, newLibUrl("stdtypes", "file.$m"), newFunc(Types.IntInstance)!!, METHOD)
        }

        val methods_none = arrayOf("close", "seek", "write", "writelines")
        for (m in methods_none) {
            table.insert(m, newLibUrl("stdtypes", "file.$m"), newFunc(Types.NoneInstance)!!, METHOD)
        }

        table.insert("readlines", newLibUrl("stdtypes", "file.readlines"), newFunc(newList(Types.StrInstance))!!, METHOD)
        table.insert("xreadlines", newLibUrl("stdtypes", "file.xreadlines"), newFunc(Types.StrInstance)!!, METHOD)
        table.insert("closed", newLibUrl("stdtypes", "file.closed"), Types.IntInstance, ATTRIBUTE)
        table.insert("encoding", newLibUrl("stdtypes", "file.encoding"), Types.StrInstance, ATTRIBUTE)
        table.insert("errors", newLibUrl("stdtypes", "file.errors"), Types.UNKNOWN, ATTRIBUTE)
        table.insert("mode", newLibUrl("stdtypes", "file.mode"), Types.IntInstance, ATTRIBUTE)
        table.insert("name", newLibUrl("stdtypes", "file.name"), Types.StrInstance, ATTRIBUTE)
        table.insert("softspace", newLibUrl("stdtypes", "file.softspace"), Types.IntInstance, ATTRIBUTE)
        table.insert("newlines", newLibUrl("stdtypes", "file.newlines"), newUnion(Types.StrInstance, newTuple(Types.StrInstance)), ATTRIBUTE)
    }


    internal fun buildFunctionType() {
        val t = BaseFunction.table

        for (s in list("func_doc", "__doc__", "func_name", "__name__", "__module__")) {
            t.insert(s, Url(DATAMODEL_URL), Types.StrInstance, ATTRIBUTE)
        }

        t.insert("func_closure", Url(DATAMODEL_URL), newTuple(), ATTRIBUTE)
        t.insert("func_code", Url(DATAMODEL_URL), Types.UNKNOWN, ATTRIBUTE)
        t.insert("func_defaults", Url(DATAMODEL_URL), newTuple(), ATTRIBUTE)
        t.insert("func_globals", Url(DATAMODEL_URL), DictType(Types.StrInstance, Types.UNKNOWN),
                ATTRIBUTE)
        t.insert("func_dict", Url(DATAMODEL_URL), DictType(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE)

        // Assume any function can become a method, for simplicity.
        for (s in list("__func__", "im_func")) {
            t.insert(s, Url(DATAMODEL_URL), FunType(), METHOD)
        }
    }


    // XXX:  finish wiring this up.  ClassType needs to inherit from it somehow,
    // so we can remove the per-instance attributes from NClassDef.
    internal fun buildClassType() {
        val t = BaseClass.table

        for (s in list("__name__", "__doc__", "__module__")) {
            t.insert(s, Url(DATAMODEL_URL), Types.StrInstance, ATTRIBUTE)
        }

        t.insert("__dict__", Url(DATAMODEL_URL), DictType(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE)
    }


    internal inner class BuiltinsModule : NativeModule("__builtin__") {
        init {
            module = newModule(name)
            Builtin = module
            table = module!!.table
        }


        protected fun addFunction(name: String, url: Url, type: Type?) {
            table!!.insert(name, url, newFunc(type)!!, FUNCTION)
        }


        override fun initBindings() {
            Analyzer.self.moduleTable.insert(name, liburl(), module!!, MODULE)
            table!!.addSuper(BaseModule.table)

            addClass("object", newLibUrl("functions", "object"), Types.ObjectClass)
            addFunction("type", newLibUrl("functions", "type"), Types.TypeClass)

            addFunction("bool", newLibUrl("functions", "bool"), Types.BoolInstance)
            addClass("int", newLibUrl("functions", "int"), Types.IntClass)
            addClass("str", newLibUrl("functions", "func-str"), Types.StrClass)
            addClass("long", newLibUrl("functions", "long"), Types.LongClass)
            addClass("float", newLibUrl("functions", "float"), Types.FloatClass)
            addClass("complex", newLibUrl("functions", "complex"), Types.ComplexClass)

            addClass("None", newLibUrl("constants", "None"), Types.NoneInstance)

            addClass("dict", newLibUrl("stdtypes", "typesmapping"), Types.BaseDict)
            addFunction("file", newLibUrl("functions", "file"), BaseFileInst)
            addFunction("list", newLibUrl("functions", "list"), InstanceType(BaseList))
            addFunction("tuple", newLibUrl("functions", "tuple"), InstanceType(BaseTuple))

            // XXX:  need to model the following as built-in class types:
            //   basestring, bool, buffer, frozenset, property, set, slice,
            //   staticmethod, super and unicode
            val builtin_func_unknown = arrayOf("apply", "basestring", "callable", "classmethod", "coerce", "compile", "copyright", "credits", "delattr", "enumerate", "eval", "execfile", "exit", "filter", "frozenset", "getattr", "help", "input", "intern", "iter", "license", "long", "property", "quit", "raw_input", "reduce", "reload", "reversed", "set", "setattr", "slice", "sorted", "staticmethod", "super", "type", "unichr", "unicode")
            for (f in builtin_func_unknown) {
                addFunction(f, newLibUrl("functions", f), Types.UNKNOWN)
            }

            val builtin_func_num = arrayOf("abs", "all", "any", "cmp", "coerce", "divmod", "hasattr", "hash", "id", "isinstance", "issubclass", "len", "max", "min", "ord", "pow", "round", "sum")
            for (f in builtin_func_num) {
                addFunction(f, newLibUrl("functions", f), Types.IntInstance)
            }

            for (f in list("hex", "oct", "repr", "chr")) {
                addFunction(f, newLibUrl("functions", f), Types.StrInstance)
            }

            addFunction("dir", newLibUrl("functions", "dir"), newList(Types.StrInstance))
            addFunction("map", newLibUrl("functions", "map"), newList(Types.UNKNOWN))
            addFunction("range", newLibUrl("functions", "range"), newList(Types.IntInstance))
            addFunction("xrange", newLibUrl("functions", "range"), newList(Types.IntInstance))
            addFunction("buffer", newLibUrl("functions", "buffer"), newList(Types.UNKNOWN))
            addFunction("zip", newLibUrl("functions", "zip"), newList(newTuple(Types.UNKNOWN)))


            for (f in list("globals", "vars", "locals")) {
                addFunction(f, newLibUrl("functions.html#$f"), newDict(Types.StrInstance, Types.UNKNOWN))
            }

            for (f in builtin_exception_types) {
                addClass(f, newLibUrl("exceptions", f),
                        newClass(f, Analyzer.self.globaltable, objectType))
            }
            BaseException = table!!.lookupType("BaseException") as ClassType?

            addAttr("True", newLibUrl("constants", "True"), Types.BoolInstance)
            addAttr("False", newLibUrl("constants", "False"), Types.BoolInstance)
            addAttr("None", newLibUrl("constants", "None"), Types.NoneInstance)
            addFunction("open", newTutUrl("inputoutput.html#reading-and-writing-files"), BaseFileInst)
            addFunction("__import__", newLibUrl("functions", "__import__"), newModule("<?>"))

            Analyzer.self.globaltable.insert("__builtins__", liburl(), module!!, ATTRIBUTE)
            Analyzer.self.globaltable.putAll(table!!)
        }
    }


    internal inner class ArrayModule : NativeModule("array") {


        override fun initBindings() {
            addClass("array", liburl("array.array"), BaseArray)
            addClass("ArrayType", liburl("array.ArrayType"), BaseArray)
        }
    }


    internal inner class AudioopModule : NativeModule("audioop") {


        override fun initBindings() {
            addClass(newException("error", table))

            addStrFuncs("add", "adpcm2lin", "alaw2lin", "bias", "lin2alaw", "lin2lin",
                    "lin2ulaw", "mul", "reverse", "tomono", "ulaw2lin")

            addNumFuncs("avg", "avgpp", "cross", "findfactor", "findmax",
                    "getsample", "max", "maxpp", "rms")

            for (s in list("adpcm2lin", "findfit", "lin2adpcm", "minmax", "ratecv")) {
                addFunction(s, newTuple())
            }
        }
    }


    internal inner class BinasciiModule : NativeModule("binascii") {


        override fun initBindings() {
            addStrFuncs(
                    "a2b_uu", "b2a_uu", "a2b_base64", "b2a_base64", "a2b_qp",
                    "b2a_qp", "a2b_hqx", "rledecode_hqx", "rlecode_hqx", "b2a_hqx",
                    "b2a_hex", "hexlify", "a2b_hex", "unhexlify")

            addNumFuncs("crc_hqx", "crc32")

            addClass(newException("Error", table))
            addClass(newException("Incomplete", table))
        }
    }


    internal inner class Bz2Module : NativeModule("bz2") {


        override fun initBindings() {
            val bz2 = newClass("BZ2File", table, BaseFile)  // close enough.
            addClass(bz2)

            val bz2c = newClass("BZ2Compressor", table, objectType)
            addMethod(bz2c, "compress", Types.StrInstance)
            addMethod(bz2c, "flush", Types.NoneInstance)
            addClass(bz2c)

            val bz2d = newClass("BZ2Decompressor", table, objectType)
            addMethod(bz2d, "decompress", Types.StrInstance)
            addClass(bz2d)

            addFunction("compress", Types.StrInstance)
            addFunction("decompress", Types.StrInstance)
        }
    }


    internal inner class CPickleModule : NativeModule("cPickle") {


        override fun liburl(): Url {
            return newLibUrl("pickle", "module-cPickle")
        }


        override fun initBindings() {
            addUnknownFuncs("dump", "load", "dumps", "loads")

            addClass(newException("PickleError", table))

            val picklingError = newException("PicklingError", table)
            addClass(picklingError)
            update("UnpickleableError", liburl(table!!.path + "." + "UnpickleableError"),
                    newClass("UnpickleableError", table, picklingError), CLASS)
            val unpicklingError = newException("UnpicklingError", table)
            addClass(unpicklingError)
            update("BadPickleGet", liburl(table!!.path + "." + "BadPickleGet"),
                    newClass("BadPickleGet", table, unpicklingError), CLASS)

            val pickler = newClass("Pickler", table, objectType)
            addMethod(pickler, "dump")
            addMethod(pickler, "clear_memo")
            addClass(pickler)

            val unpickler = newClass("Unpickler", table, objectType)
            addMethod(unpickler, "load")
            addMethod(unpickler, "noload")
            addClass(unpickler)
        }
    }


    internal inner class CStringIOModule : NativeModule("cStringIO") {


        override fun liburl(): Url {
            return newLibUrl("stringio")
        }


        override fun liburl(anchor: String): Url {
            return newLibUrl("stringio", anchor)
        }

        override fun initBindings() {
            val StringIO = newClass("StringIO", table, BaseFile)
            addFunction("StringIO", InstanceType(StringIO))
            addAttr("InputType", BaseType)
            addAttr("OutputType", BaseType)
            addAttr("cStringIO_CAPI", Types.UNKNOWN)
        }
    }


    internal inner class CMathModule : NativeModule("cmath") {


        override fun initBindings() {
            addFunction("phase", Types.IntInstance)
            addFunction("polar", newTuple(Types.IntInstance, Types.IntInstance))
            addFunction("rect", Types.ComplexInstance)

            for (plf in list("exp", "log", "log10", "sqrt")) {
                addFunction(plf, Types.IntInstance)
            }

            for (tf in list("acos", "asin", "atan", "cos", "sin", "tan")) {
                addFunction(tf, Types.IntInstance)
            }

            for (hf in list("acosh", "asinh", "atanh", "cosh", "sinh", "tanh")) {
                addFunction(hf, Types.ComplexInstance)
            }

            for (cf in list("isinf", "isnan")) {
                addFunction(cf, Types.BoolInstance)
            }

            for (c in list("pi", "e")) {
                addAttr(c, Types.IntInstance)
            }
        }
    }


    internal inner class CollectionsModule : NativeModule("collections") {


        private fun abcUrl(): Url {
            return liburl("abcs-abstract-base-classes")
        }


        private fun dequeUrl(): Url {
            return liburl("deque-objects")
        }


        override fun initBindings() {
            val callable = newClass("Callable", table, objectType)
            callable.table.insert("__call__", abcUrl(), newFunc(), METHOD)
            addClass(callable)

            val iterableType = newClass("Iterable", table, objectType)
            // TODO should this jump to url like https://docs.python.org/2.7/library/stdtypes.html#iterator.__iter__ ?
            iterableType.table.insert("__next__", abcUrl(), newFunc(), METHOD)
            iterableType.table.insert("__iter__", abcUrl(), newFunc(), METHOD)
            addClass(iterableType)

            val Hashable = newClass("Hashable", table, objectType)
            Hashable.table.insert("__hash__", abcUrl(), newFunc(Types.IntInstance)!!, METHOD)
            addClass(Hashable)

            val Sized = newClass("Sized", table, objectType)
            Sized.table.insert("__len__", abcUrl(), newFunc(Types.IntInstance)!!, METHOD)
            addClass(Sized)

            val containerType = newClass("Container", table, objectType)
            containerType.table.insert("__contains__", abcUrl(), newFunc(Types.IntInstance)!!, METHOD)
            addClass(containerType)

            val iteratorType = newClass("Iterator", table, iterableType)
            addClass(iteratorType)

            val sequenceType = newClass("Sequence", table, Sized, iterableType, containerType)
            sequenceType.table.insert("__getitem__", abcUrl(), newFunc(), METHOD)
            sequenceType.table.insert("reversed", abcUrl(), newFunc(sequenceType)!!, METHOD)
            sequenceType.table.insert("index", abcUrl(), newFunc(Types.IntInstance)!!, METHOD)
            sequenceType.table.insert("count", abcUrl(), newFunc(Types.IntInstance)!!, METHOD)
            addClass(sequenceType)

            val mutableSequence = newClass("MutableSequence", table, sequenceType)
            mutableSequence.table.insert("__setitem__", abcUrl(), newFunc(), METHOD)
            mutableSequence.table.insert("__delitem__", abcUrl(), newFunc(), METHOD)
            addClass(mutableSequence)

            val setType = newClass("Set", table, Sized, iterableType, containerType)
            setType.table.insert("__getitem__", abcUrl(), newFunc(), METHOD)
            addClass(setType)

            val mutableSet = newClass("MutableSet", table, setType)
            mutableSet.table.insert("add", abcUrl(), newFunc(), METHOD)
            mutableSet.table.insert("discard", abcUrl(), newFunc(), METHOD)
            addClass(mutableSet)

            val mapping = newClass("Mapping", table, Sized, iterableType, containerType)
            mapping.table.insert("__getitem__", abcUrl(), newFunc(), METHOD)
            addClass(mapping)

            val mutableMapping = newClass("MutableMapping", table, mapping)
            mutableMapping.table.insert("__setitem__", abcUrl(), newFunc(), METHOD)
            mutableMapping.table.insert("__delitem__", abcUrl(), newFunc(), METHOD)
            addClass(mutableMapping)

            val MappingView = newClass("MappingView", table, Sized)
            addClass(MappingView)

            val KeysView = newClass("KeysView", table, Sized)
            addClass(KeysView)

            val ItemsView = newClass("ItemsView", table, Sized)
            addClass(ItemsView)

            val ValuesView = newClass("ValuesView", table, Sized)
            addClass(ValuesView)

            val deque = newClass("deque", table, objectType)
            for (n in list("append", "appendLeft", "clear",
                    "extend", "extendLeft", "rotate")) {
                deque.table.insert(n, dequeUrl(), newFunc(Types.NoneInstance)!!, METHOD)
            }
            for (u in list("__getitem__", "__iter__",
                    "pop", "popleft", "remove")) {
                deque.table.insert(u, dequeUrl(), newFunc(), METHOD)
            }
            addClass(deque)

            val defaultdict = newClass("defaultdict", table, objectType)
            defaultdict.table.insert("__missing__", liburl("defaultdict-objects"),
                    newFunc(), METHOD)
            defaultdict.table.insert("default_factory", liburl("defaultdict-objects"),
                    newFunc(), METHOD)
            addClass(defaultdict)

            val argh = "namedtuple-factory-function-for-tuples-with-named-fields"
            val namedtuple = newClass("(namedtuple)", table, BaseTuple)
            namedtuple.table.insert("_fields", liburl(argh),
                    ListType(Types.StrInstance), ATTRIBUTE)
            addFunction("namedtuple", namedtuple)
        }
    }


    internal inner class CTypesModule : NativeModule("ctypes") {


        override fun initBindings() {
            val ctypes_attrs = arrayOf("ARRAY", "ArgumentError", "Array", "BigEndianStructure", "CDLL", "CFUNCTYPE", "DEFAULT_MODE", "DllCanUnloadNow", "DllGetClassObject", "FormatError", "GetLastError", "HRESULT", "LibraryLoader", "LittleEndianStructure", "OleDLL", "POINTER", "PYFUNCTYPE", "PyDLL", "RTLD_GLOBAL", "RTLD_LOCAL", "SetPointerType", "Structure", "Union", "WINFUNCTYPE", "WinDLL", "WinError", "_CFuncPtr", "_FUNCFLAG_CDECL", "_FUNCFLAG_PYTHONAPI", "_FUNCFLAG_STDCALL", "_FUNCFLAG_USE_ERRNO", "_FUNCFLAG_USE_LASTERROR", "_Pointer", "_SimpleCData", "_c_functype_cache", "_calcsize", "_cast", "_cast_addr", "_check_HRESULT", "_check_size", "_ctypes_version", "_dlopen", "_endian", "_memmove_addr", "_memset_addr", "_os", "_pointer_type_cache", "_string_at", "_string_at_addr", "_sys", "_win_functype_cache", "_wstring_at", "_wstring_at_addr", "addressof", "alignment", "byref", "c_bool", "c_buffer", "c_byte", "c_char", "c_char_p", "c_double", "c_float", "c_int", "c_int16", "c_int32", "c_int64", "c_int8", "c_long", "c_longdouble", "c_longlong", "c_short", "c_size_t", "c_ubyte", "c_uint", "c_uint16", "c_uint32", "c_uint64", "c_uint8", "c_ulong", "c_ulonglong", "c_ushort", "c_void_p", "c_voidp", "c_wchar", "c_wchar_p", "cast", "cdll", "create_string_buffer", "create_unicode_buffer", "get_errno", "get_last_error", "memmove", "memset", "oledll", "pointer", "py_object", "pydll", "pythonapi", "resize", "set_conversion_mode", "set_errno", "set_last_error", "sizeof", "string_at", "windll", "wstring_at")
            for (attr in ctypes_attrs) {
                addAttr(attr, Types.UNKNOWN)
            }
        }
    }


    internal inner class CryptModule : NativeModule("crypt") {


        override fun initBindings() {
            addStrFuncs("crypt")
        }
    }


    internal inner class DatetimeModule : NativeModule("datetime") {


        private fun dtUrl(anchor: String): Url {
            return liburl("datetime.$anchor")
        }


        override fun initBindings() {
            // XXX:  make datetime, time, date, timedelta and tzinfo Base* objects,
            // so built-in functions can return them.

            addNumAttrs("MINYEAR", "MAXYEAR")

            Datetime_timedelta = newClass("timedelta", table, objectType)
            val timedelta = Datetime_timedelta
            addClass(timedelta)
            addAttr(timedelta, "min", timedelta)
            addAttr(timedelta, "max", timedelta)
            addAttr(timedelta, "resolution", timedelta)
            addAttr(timedelta, "days", Types.IntInstance)
            addAttr(timedelta, "seconds", Types.IntInstance)
            addAttr(timedelta, "microseconds", Types.IntInstance)

            Datetime_tzinfo = newClass("tzinfo", table, objectType)
            val tzinfo = Datetime_tzinfo
            addClass(tzinfo)
            addMethod(tzinfo, "utcoffset", timedelta)
            addMethod(tzinfo, "dst", timedelta)
            addMethod(tzinfo, "tzname", Types.StrInstance)
            addMethod(tzinfo, "fromutc", tzinfo)

            Datetime_date = newClass("date", table, objectType)
            val date = Datetime_date
            addClass(date)
            addAttr(date, "min", date)
            addAttr(date, "max", date)
            addAttr(date, "resolution", timedelta)

            addMethod(date, "today", date)
            addMethod(date, "fromtimestamp", date)
            addMethod(date, "fromordinal", date)

            addAttr(date, "year", Types.IntInstance)
            addAttr(date, "month", Types.IntInstance)
            addAttr(date, "day", Types.IntInstance)

            addMethod(date, "replace", date)
            addMethod(date, "timetuple", Time_struct_time)

            for (n in list("toordinal", "weekday", "isoweekday")) {
                addMethod(date, n, Types.IntInstance)
            }
            for (r in list("ctime", "strftime", "isoformat")) {
                addMethod(date, r, Types.StrInstance)
            }
            addMethod(date, "isocalendar", newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance))

            Datetime_time = newClass("time", table, objectType)
            val time = Datetime_time
            addClass(time)

            addAttr(time, "min", time)
            addAttr(time, "max", time)
            addAttr(time, "resolution", timedelta)

            addAttr(time, "hour", Types.IntInstance)
            addAttr(time, "minute", Types.IntInstance)
            addAttr(time, "second", Types.IntInstance)
            addAttr(time, "microsecond", Types.IntInstance)
            addAttr(time, "tzinfo", tzinfo)

            addMethod(time, "replace", time)

            for (l in list("isoformat", "strftime", "tzname")) {
                addMethod(time, l, Types.StrInstance)
            }
            for (f in list("utcoffset", "dst")) {
                addMethod(time, f, timedelta)
            }

            Datetime_datetime = newClass("datetime", table, date, time)
            val datetime = Datetime_datetime
            addClass(datetime)

            for (c in list("combine", "fromordinal", "fromtimestamp", "now",
                    "strptime", "today", "utcfromtimestamp", "utcnow")) {
                addMethod(datetime, c, datetime)
            }

            addAttr(datetime, "min", datetime)
            addAttr(datetime, "max", datetime)
            addAttr(datetime, "resolution", timedelta)

            addMethod(datetime, "date", date)

            for (x in list("time", "timetz")) {
                addMethod(datetime, x, time)
            }

            for (y in list("replace", "astimezone")) {
                addMethod(datetime, y, datetime)
            }

            addMethod(datetime, "utctimetuple", Time_struct_time)
        }
    }


    internal inner class DbmModule : NativeModule("dbm") {


        override fun initBindings() {
            val dbm = ClassType("dbm", table, Types.BaseDict)
            addClass(dbm)
            addClass(newException("error", table))
            addStrAttrs("library")
            addFunction("open", dbm)
        }
    }


    internal inner class ErrnoModule : NativeModule("errno") {


        override fun initBindings() {
            addNumAttrs(
                    "E2BIG", "EACCES", "EADDRINUSE", "EADDRNOTAVAIL", "EAFNOSUPPORT",
                    "EAGAIN", "EALREADY", "EBADF", "EBUSY", "ECHILD", "ECONNABORTED",
                    "ECONNREFUSED", "ECONNRESET", "EDEADLK", "EDEADLOCK",
                    "EDESTADDRREQ", "EDOM", "EDQUOT", "EEXIST", "EFAULT", "EFBIG",
                    "EHOSTDOWN", "EHOSTUNREACH", "EILSEQ", "EINPROGRESS", "EINTR",
                    "EINVAL", "EIO", "EISCONN", "EISDIR", "ELOOP", "EMFILE", "EMLINK",
                    "EMSGSIZE", "ENAMETOOLONG", "ENETDOWN", "ENETRESET", "ENETUNREACH",
                    "ENFILE", "ENOBUFS", "ENODEV", "ENOENT", "ENOEXEC", "ENOLCK",
                    "ENOMEM", "ENOPROTOOPT", "ENOSPC", "ENOSYS", "ENOTCONN", "ENOTDIR",
                    "ENOTEMPTY", "ENOTSOCK", "ENOTTY", "ENXIO", "EOPNOTSUPP", "EPERM",
                    "EPFNOSUPPORT", "EPIPE", "EPROTONOSUPPORT", "EPROTOTYPE", "ERANGE",
                    "EREMOTE", "EROFS", "ESHUTDOWN", "ESOCKTNOSUPPORT", "ESPIPE",
                    "ESRCH", "ESTALE", "ETIMEDOUT", "ETOOMANYREFS", "EUSERS",
                    "EWOULDBLOCK", "EXDEV", "WSABASEERR", "WSAEACCES", "WSAEADDRINUSE",
                    "WSAEADDRNOTAVAIL", "WSAEAFNOSUPPORT", "WSAEALREADY", "WSAEBADF",
                    "WSAECONNABORTED", "WSAECONNREFUSED", "WSAECONNRESET",
                    "WSAEDESTADDRREQ", "WSAEDISCON", "WSAEDQUOT", "WSAEFAULT",
                    "WSAEHOSTDOWN", "WSAEHOSTUNREACH", "WSAEINPROGRESS", "WSAEINTR",
                    "WSAEINVAL", "WSAEISCONN", "WSAELOOP", "WSAEMFILE", "WSAEMSGSIZE",
                    "WSAENAMETOOLONG", "WSAENETDOWN", "WSAENETRESET", "WSAENETUNREACH",
                    "WSAENOBUFS", "WSAENOPROTOOPT", "WSAENOTCONN", "WSAENOTEMPTY",
                    "WSAENOTSOCK", "WSAEOPNOTSUPP", "WSAEPFNOSUPPORT", "WSAEPROCLIM",
                    "WSAEPROTONOSUPPORT", "WSAEPROTOTYPE", "WSAEREMOTE", "WSAESHUTDOWN",
                    "WSAESOCKTNOSUPPORT", "WSAESTALE", "WSAETIMEDOUT",
                    "WSAETOOMANYREFS", "WSAEUSERS", "WSAEWOULDBLOCK",
                    "WSANOTINITIALISED", "WSASYSNOTREADY", "WSAVERNOTSUPPORTED")

            addAttr("errorcode", newDict(Types.IntInstance, Types.StrInstance))
        }
    }


    internal inner class ExceptionsModule : NativeModule("exceptions") {


        override fun initBindings() {
            val builtins = get("__builtin__")
            for (s in builtin_exception_types) {
                //                Binding b = builtins.getTable().lookup(s);
                //                table.update(b.getName(), b.getFirstNode(), b.getType(), b.getKind());
            }
        }
    }


    internal inner class FcntlModule : NativeModule("fcntl") {


        override fun initBindings() {
            for (s in list("fcntl", "ioctl")) {
                addFunction(s, newUnion(Types.IntInstance, Types.StrInstance))
            }
            addNumFuncs("flock")
            addUnknownFuncs("lockf")

            addNumAttrs(
                    "DN_ACCESS", "DN_ATTRIB", "DN_CREATE", "DN_DELETE", "DN_MODIFY",
                    "DN_MULTISHOT", "DN_RENAME", "FASYNC", "FD_CLOEXEC", "F_DUPFD",
                    "F_EXLCK", "F_GETFD", "F_GETFL", "F_GETLEASE", "F_GETLK", "F_GETLK64",
                    "F_GETOWN", "F_GETSIG", "F_NOTIFY", "F_RDLCK", "F_SETFD", "F_SETFL",
                    "F_SETLEASE", "F_SETLK", "F_SETLK64", "F_SETLKW", "F_SETLKW64",
                    "F_SETOWN", "F_SETSIG", "F_SHLCK", "F_UNLCK", "F_WRLCK", "I_ATMARK",
                    "I_CANPUT", "I_CKBAND", "I_FDINSERT", "I_FIND", "I_FLUSH",
                    "I_FLUSHBAND", "I_GETBAND", "I_GETCLTIME", "I_GETSIG", "I_GRDOPT",
                    "I_GWROPT", "I_LINK", "I_LIST", "I_LOOK", "I_NREAD", "I_PEEK",
                    "I_PLINK", "I_POP", "I_PUNLINK", "I_PUSH", "I_RECVFD", "I_SENDFD",
                    "I_SETCLTIME", "I_SETSIG", "I_SRDOPT", "I_STR", "I_SWROPT",
                    "I_UNLINK", "LOCK_EX", "LOCK_MAND", "LOCK_NB", "LOCK_READ", "LOCK_RW",
                    "LOCK_SH", "LOCK_UN", "LOCK_WRITE")
        }
    }


    internal inner class FpectlModule : NativeModule("fpectl") {


        override fun initBindings() {
            addNoneFuncs("turnon_sigfpe", "turnoff_sigfpe")
            addClass(newException("FloatingPointError", table))
        }
    }


    internal inner class GcModule : NativeModule("gc") {


        override fun initBindings() {
            addNoneFuncs("enable", "disable", "set_debug", "set_threshold")
            addNumFuncs("isenabled", "collect", "get_debug", "get_count", "get_threshold")
            for (s in list("get_objects", "get_referrers", "get_referents")) {
                addFunction(s, newList())
            }
            addAttr("garbage", newList())
            addNumAttrs("DEBUG_STATS", "DEBUG_COLLECTABLE", "DEBUG_UNCOLLECTABLE",
                    "DEBUG_INSTANCES", "DEBUG_OBJECTS", "DEBUG_SAVEALL", "DEBUG_LEAK")
        }
    }


    internal inner class GdbmModule : NativeModule("gdbm") {


        override fun initBindings() {
            addClass(newException("error", table))

            val gdbm = ClassType("gdbm", table, Types.BaseDict)
            addMethod(gdbm, "firstkey", Types.StrInstance)
            addMethod(gdbm, "nextkey", Types.StrInstance)
            addMethod(gdbm, "reorganize", Types.NoneInstance)
            addMethod(gdbm, "sync", Types.NoneInstance)
            addFunction("open", gdbm)
        }

    }


    internal inner class GrpModule : NativeModule("grp") {


        override fun initBindings() {
            this@Builtins["struct"]
            val struct_group = newClass("struct_group", table, BaseStruct)
            addAttr(struct_group, "gr_name", Types.StrInstance)
            addAttr(struct_group, "gr_passwd", Types.StrInstance)
            addAttr(struct_group, "gr_gid", Types.IntInstance)
            addAttr(struct_group, "gr_mem", Types.StrInstance)
            addClass(struct_group)

            for (s in list("getgrgid", "getgrnam")) {
                addFunction(s, struct_group)
            }
            addFunction("getgrall", ListType(struct_group))
        }
    }


    internal inner class ImpModule : NativeModule("imp") {


        override fun initBindings() {
            addStrFuncs("get_magic")
            addFunction("get_suffixes", newList(newTuple(Types.StrInstance, Types.StrInstance, Types.IntInstance)))
            addFunction("find_module", newTuple(Types.StrInstance, Types.StrInstance, Types.IntInstance))

            val module_methods = arrayOf("load_module", "new_module", "init_builtin", "init_frozen", "load_compiled", "load_dynamic", "load_source")
            for (mm in module_methods) {
                addFunction(mm, newModule("<?>"))
            }

            addUnknownFuncs("acquire_lock", "release_lock")

            addNumAttrs("PY_SOURCE", "PY_COMPILED", "C_EXTENSION",
                    "PKG_DIRECTORY", "C_BUILTIN", "PY_FROZEN", "SEARCH_ERROR")

            addNumFuncs("lock_held", "is_builtin", "is_frozen")

            val impNullImporter = newClass("NullImporter", table, objectType)
            addMethod(impNullImporter, "find_module", Types.NoneInstance)
            addClass(impNullImporter)
        }
    }


    internal inner class ItertoolsModule : NativeModule("itertools") {


        override fun initBindings() {
            val iterator = newClass("iterator", table, objectType)
            addMethod(iterator, "from_iterable", iterator)
            addMethod(iterator, "next")

            for (s in list("chain", "combinations", "count", "cycle",
                    "dropwhile", "groupby", "ifilter",
                    "ifilterfalse", "imap", "islice", "izip",
                    "izip_longest", "permutations", "product",
                    "repeat", "starmap", "takewhile", "tee")) {
                addClass(iterator)
            }
        }
    }


    internal inner class MarshalModule : NativeModule("marshal") {


        override fun initBindings() {
            addNumAttrs("version")
            addStrFuncs("dumps")
            addUnknownFuncs("dump", "load", "loads")
        }
    }


    internal inner class MathModule : NativeModule("math") {


        override fun initBindings() {
            addNumFuncs(
                    "acos", "acosh", "asin", "asinh", "atan", "atan2", "atanh", "ceil",
                    "copysign", "cos", "cosh", "degrees", "exp", "fabs", "factorial",
                    "floor", "fmod", "frexp", "fsum", "hypot", "isinf", "isnan",
                    "ldexp", "log", "log10", "log1p", "modf", "pow", "radians", "sin",
                    "sinh", "sqrt", "tan", "tanh", "trunc")
            addNumAttrs("pi", "e")
        }
    }


    internal inner class Md5Module : NativeModule("md5") {


        override fun initBindings() {
            addNumAttrs("blocksize", "digest_size")

            val md5 = newClass("md5", table, objectType)
            addMethod(md5, "update")
            addMethod(md5, "digest", Types.StrInstance)
            addMethod(md5, "hexdigest", Types.StrInstance)
            addMethod(md5, "copy", md5)

            update("new", liburl(), newFunc(md5), CONSTRUCTOR)
            update("md5", liburl(), newFunc(md5), CONSTRUCTOR)
        }
    }


    internal inner class MmapModule : NativeModule("mmap") {


        override fun initBindings() {
            val mmap = newClass("mmap", table, objectType)

            for (s in list("ACCESS_COPY", "ACCESS_READ", "ACCESS_WRITE",
                    "ALLOCATIONGRANULARITY", "MAP_ANON", "MAP_ANONYMOUS",
                    "MAP_DENYWRITE", "MAP_EXECUTABLE", "MAP_PRIVATE",
                    "MAP_SHARED", "PAGESIZE", "PROT_EXEC", "PROT_READ",
                    "PROT_WRITE")) {
                addAttr(mmap, s, Types.IntInstance)
            }

            for (fstr in list("read", "read_byte", "readline")) {
                addMethod(mmap, fstr, Types.StrInstance)
            }

            for (fnum in list("find", "rfind", "tell")) {
                addMethod(mmap, fnum, Types.IntInstance)
            }

            for (fnone in list("close", "flush", "move", "resize", "seek",
                    "write", "write_byte")) {
                addMethod(mmap, fnone, Types.NoneInstance)
            }

            addClass(mmap)
        }
    }


    internal inner class NisModule : NativeModule("nis") {


        override fun initBindings() {
            addStrFuncs("match", "cat", "get_default_domain")
            addFunction("maps", newList(Types.StrInstance))
            addClass(newException("error", table))
        }
    }


    internal inner class OsModule : NativeModule("os") {


        override fun initBindings() {
            addAttr("name", Types.StrInstance)
            addClass(newException("error", table))  // XXX: OSError

            initProcBindings()
            initProcMgmtBindings()
            initFileBindings()
            initFileAndDirBindings()
            initMiscSystemInfo()
            initOsPathModule()

            val str_attrs = arrayOf("altsep", "curdir", "devnull", "defpath", "pardir", "pathsep", "sep")
            for (s in str_attrs) {
                addAttr(s, Types.StrInstance)
            }

            // TODO this is not needed?
            addAttr("errno", liburl(), newModule("errno"))

            addFunction("urandom", Types.StrInstance)
            addAttr("NGROUPS_MAX", Types.IntInstance)

            for (s in list("_Environ", "_copy_reg", "_execvpe", "_exists",
                    "_get_exports_list", "_make_stat_result",
                    "_make_statvfs_result", "_pickle_stat_result",
                    "_pickle_statvfs_result", "_spawnvef")) {
                addFunction(s, Types.UNKNOWN)
            }
        }


        private fun initProcBindings() {
            addAttr("environ", newDict(Types.StrInstance, Types.StrInstance))

            for (s in list("chdir", "fchdir", "putenv", "setegid", "seteuid",
                    "setgid", "setgroups", "setpgrp", "setpgid",
                    "setreuid", "setregid", "setuid", "unsetenv")) {
                addFunction(s, Types.NoneInstance)
            }

            for (s in list("getegid", "getgid", "getpgid", "getpgrp",
                    "getppid", "getuid", "getsid", "umask")) {
                addFunction(s, Types.IntInstance)
            }

            for (s in list("getcwd", "ctermid", "getlogin", "getenv", "strerror")) {
                addFunction(s, Types.StrInstance)
            }

            addFunction("getgroups", newList(Types.StrInstance))
            addFunction("uname", newTuple(Types.StrInstance, Types.StrInstance, Types.StrInstance,
                    Types.StrInstance, Types.StrInstance))
        }


        private fun initProcMgmtBindings() {
            for (s in list("EX_CANTCREAT", "EX_CONFIG", "EX_DATAERR",
                    "EX_IOERR", "EX_NOHOST", "EX_NOINPUT",
                    "EX_NOPERM", "EX_NOUSER", "EX_OK", "EX_OSERR",
                    "EX_OSFILE", "EX_PROTOCOL", "EX_SOFTWARE",
                    "EX_TEMPFAIL", "EX_UNAVAILABLE", "EX_USAGE",
                    "P_NOWAIT", "P_NOWAITO", "P_WAIT", "P_DETACH",
                    "P_OVERLAY", "WCONTINUED", "WCOREDUMP",
                    "WEXITSTATUS", "WIFCONTINUED", "WIFEXITED",
                    "WIFSIGNALED", "WIFSTOPPED", "WNOHANG", "WSTOPSIG",
                    "WTERMSIG", "WUNTRACED")) {
                addAttr(s, Types.IntInstance)
            }

            for (s in list("abort", "execl", "execle", "execlp", "execlpe",
                    "execv", "execve", "execvp", "execvpe", "_exit",
                    "kill", "killpg", "plock", "startfile")) {
                addFunction(s, Types.NoneInstance)
            }

            for (s in list("nice", "spawnl", "spawnle", "spawnlp", "spawnlpe",
                    "spawnv", "spawnve", "spawnvp", "spawnvpe", "system")) {
                addFunction(s, Types.IntInstance)
            }

            addFunction("fork", newUnion(BaseFileInst, Types.IntInstance))
            addFunction("times", newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance, Types.IntInstance, Types.IntInstance))

            for (s in list("forkpty", "wait", "waitpid")) {
                addFunction(s, newTuple(Types.IntInstance, Types.IntInstance))
            }

            for (s in list("wait3", "wait4")) {
                addFunction(s, newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance))
            }
        }


        private fun initFileBindings() {
            for (s in list("fdopen", "popen", "tmpfile")) {
                addFunction(s, BaseFileInst)
            }

            addFunction("popen2", newTuple(BaseFileInst, BaseFileInst))
            addFunction("popen3", newTuple(BaseFileInst, BaseFileInst, BaseFileInst))
            addFunction("popen4", newTuple(BaseFileInst, BaseFileInst))

            addFunction("open", BaseFileInst)

            for (s in list("close", "closerange", "dup2", "fchmod",
                    "fchown", "fdatasync", "fsync", "ftruncate",
                    "lseek", "tcsetpgrp", "write")) {
                addFunction(s, Types.NoneInstance)
            }

            for (s in list("dup2", "fpathconf", "fstat", "fstatvfs",
                    "isatty", "tcgetpgrp")) {
                addFunction(s, Types.IntInstance)
            }

            for (s in list("read", "ttyname")) {
                addFunction(s, Types.StrInstance)
            }

            for (s in list("openpty", "pipe", "fstat", "fstatvfs",
                    "isatty")) {
                addFunction(s, newTuple(Types.IntInstance, Types.IntInstance))
            }

            for (s in list("O_APPEND", "O_CREAT", "O_DIRECT", "O_DIRECTORY",
                    "O_DSYNC", "O_EXCL", "O_LARGEFILE", "O_NDELAY",
                    "O_NOCTTY", "O_NOFOLLOW", "O_NONBLOCK", "O_RDONLY",
                    "O_RDWR", "O_RSYNC", "O_SYNC", "O_TRUNC", "O_WRONLY",
                    "SEEK_CUR", "SEEK_END", "SEEK_SET")) {
                addAttr(s, Types.IntInstance)
            }
        }


        private fun initFileAndDirBindings() {
            for (s in list("F_OK", "R_OK", "W_OK", "X_OK")) {
                addAttr(s, Types.IntInstance)
            }

            for (s in list("chflags", "chroot", "chmod", "chown", "lchflags",
                    "lchmod", "lchown", "link", "mknod", "mkdir",
                    "mkdirs", "remove", "removedirs", "rename", "renames",
                    "rmdir", "symlink", "unlink", "utime")) {
                addAttr(s, Types.NoneInstance)
            }

            for (s in list("access", "lstat", "major", "minor",
                    "makedev", "pathconf", "stat_float_times")) {
                addFunction(s, Types.IntInstance)
            }

            for (s in list("getcwdu", "readlink", "tempnam", "tmpnam")) {
                addFunction(s, Types.StrInstance)
            }

            for (s in list("listdir")) {
                addFunction(s, newList(Types.StrInstance))
            }

            addFunction("mkfifo", BaseFileInst)

            addFunction("stat", newList(Types.IntInstance))  // XXX: posix.stat_result
            addFunction("statvfs", newList(Types.IntInstance))  // XXX: pos.statvfs_result

            addAttr("pathconf_names", newDict(Types.StrInstance, Types.IntInstance))
            addAttr("TMP_MAX", Types.IntInstance)

            addFunction("walk", newList(newTuple(Types.StrInstance, Types.StrInstance, Types.StrInstance)))
        }


        private fun initMiscSystemInfo() {
            addAttr("confstr_names", newDict(Types.StrInstance, Types.IntInstance))
            addAttr("sysconf_names", newDict(Types.StrInstance, Types.IntInstance))

            for (s in list("curdir", "pardir", "sep", "altsep", "extsep",
                    "pathsep", "defpath", "linesep", "devnull")) {
                addAttr(s, Types.StrInstance)
            }

            for (s in list("getloadavg", "sysconf")) {
                addFunction(s, Types.IntInstance)
            }

            addFunction("confstr", Types.StrInstance)
        }


        private inner class OSPathModule internal constructor(name: String) : NativeModule("os.path") {

            protected override fun initBindings() {

            }
        }

        private fun initOsPathModule() {
            val m = newModule("path")
            val ospath = m!!.table
            ospath.setPath("os.path")  // make sure global qnames are correct

            update("path", newLibUrl("os.path.html#module-os.path"), m, MODULE)

            val str_funcs = arrayOf("_resolve_link", "abspath", "basename", "commonprefix", "dirname", "expanduser", "expandvars", "join", "normcase", "normpath", "realpath", "relpath")
            for (s in str_funcs) {
                addFunction(m, s, Types.StrInstance)
            }

            val num_funcs = arrayOf("exists", "lexists", "getatime", "getctime", "getmtime", "getsize", "isabs", "isdir", "isfile", "islink", "ismount", "samefile", "sameopenfile", "samestat", "supports_unicode_filenames")
            for (s in num_funcs) {
                addFunction(m, s, Types.IntInstance)
            }

            for (s in list("split", "splitdrive", "splitext", "splitunc")) {
                addFunction(m, s, newTuple(Types.StrInstance, Types.StrInstance))
            }

            addFunction(m, "walk", newFunc(Types.NoneInstance))

            addAttr(ospath, "os", this.module)
            ospath.insert("stat", newLibUrl("stat"),
                    // moduleTable.lookupLocal("stat").getType(),
                    newModule("<stat-fixme>")!!, ATTRIBUTE)

            // XXX:  this is an re object, I think
            addAttr(ospath, "_varprog", Types.UNKNOWN)
        }
    }


    internal inner class OperatorModule : NativeModule("operator") {


        override fun initBindings() {
            // XXX:  mark __getslice__, __setslice__ and __delslice__ as deprecated.
            addNumFuncs(
                    "__abs__", "__add__", "__and__", "__concat__", "__contains__",
                    "__div__", "__doc__", "__eq__", "__floordiv__", "__ge__",
                    "__getitem__", "__getslice__", "__gt__", "__iadd__", "__iand__",
                    "__iconcat__", "__idiv__", "__ifloordiv__", "__ilshift__",
                    "__imod__", "__imul__", "__index__", "__inv__", "__invert__",
                    "__ior__", "__ipow__", "__irepeat__", "__irshift__", "__isub__",
                    "__itruediv__", "__ixor__", "__le__", "__lshift__", "__lt__",
                    "__mod__", "__mul__", "__name__", "__ne__", "__neg__", "__not__",
                    "__or__", "__package__", "__pos__", "__pow__", "__repeat__",
                    "__rshift__", "__setitem__", "__setslice__", "__sub__",
                    "__truediv__", "__xor__", "abs", "add", "and_", "concat",
                    "contains", "countOf", "div", "eq", "floordiv", "ge", "getitem",
                    "getslice", "gt", "iadd", "iand", "iconcat", "idiv", "ifloordiv",
                    "ilshift", "imod", "imul", "index", "indexOf", "inv", "invert",
                    "ior", "ipow", "irepeat", "irshift", "isCallable",
                    "isMappingType", "isNumberType", "isSequenceType", "is_",
                    "is_not", "isub", "itruediv", "ixor", "le", "lshift", "lt", "mod",
                    "mul", "ne", "neg", "not_", "or_", "pos", "pow", "repeat",
                    "rshift", "sequenceIncludes", "setitem", "setslice", "sub",
                    "truediv", "truth", "xor")

            addUnknownFuncs("attrgetter", "itemgetter", "methodcaller")
            addNoneFuncs("__delitem__", "__delslice__", "delitem", "delclice")
        }
    }


    internal inner class ParserModule : NativeModule("parser") {


        override fun initBindings() {
            val st = newClass("ST", table, objectType)
            addMethod(st, "compile", Types.NoneInstance)
            addMethod(st, "isexpr", Types.IntInstance)
            addMethod(st, "issuite", Types.IntInstance)
            addMethod(st, "tolist", newList())
            addMethod(st, "totuple", newTuple())

            addAttr("STType", BaseType)

            for (s in list("expr", "suite", "sequence2st", "tuple2st")) {
                addFunction(s, st)
            }

            addFunction("st2list", newList())
            addFunction("st2tuple", newTuple())
            addFunction("compilest", Types.UNKNOWN)
            addFunction("isexpr", Types.BoolInstance)
            addFunction("issuite", Types.BoolInstance)

            addClass(newException("ParserError", table))
        }
    }


    internal inner class PosixModule : NativeModule("posix") {


        override fun initBindings() {
            addAttr("environ", newDict(Types.StrInstance, Types.StrInstance))
        }
    }


    internal inner class PwdModule : NativeModule("pwd") {


        override fun initBindings() {
            val struct_pwd = newClass("struct_pwd", table, objectType)
            for (s in list("pw_nam", "pw_passwd", "pw_uid", "pw_gid",
                    "pw_gecos", "pw_dir", "pw_shell")) {
                struct_pwd.table.insert(s, liburl(), Types.IntInstance, ATTRIBUTE)
            }
            addAttr("struct_pwd", liburl(), struct_pwd)

            addFunction("getpwuid", struct_pwd)
            addFunction("getpwnam", struct_pwd)
            addFunction("getpwall", newList(struct_pwd))
        }
    }


    internal inner class PyexpatModule : NativeModule("pyexpat") {


        override fun initBindings() {
            // XXX
        }
    }


    internal inner class ReadlineModule : NativeModule("readline") {


        override fun initBindings() {
            addNoneFuncs("parse_and_bind", "insert_text", "read_init_file",
                    "read_history_file", "write_history_file",
                    "clear_history", "set_history_length",
                    "remove_history_item", "replace_history_item",
                    "redisplay", "set_startup_hook", "set_pre_input_hook",
                    "set_completer", "set_completer_delims",
                    "set_completion_display_matches_hook", "add_history")

            addNumFuncs("get_history_length", "get_current_history_length",
                    "get_begidx", "get_endidx")

            addStrFuncs("get_line_buffer", "get_history_item")

            addUnknownFuncs("get_completion_type")

            addFunction("get_completer", newFunc())
            addFunction("get_completer_delims", newList(Types.StrInstance))
        }
    }


    internal inner class ResourceModule : NativeModule("resource") {


        override fun initBindings() {
            addFunction("getrlimit", newTuple(Types.IntInstance, Types.IntInstance))
            addFunction("getrlimit", Types.UNKNOWN)

            val constants = arrayOf("RLIMIT_CORE", "RLIMIT_CPU", "RLIMIT_FSIZE", "RLIMIT_DATA", "RLIMIT_STACK", "RLIMIT_RSS", "RLIMIT_NPROC", "RLIMIT_NOFILE", "RLIMIT_OFILE", "RLIMIT_MEMLOCK", "RLIMIT_VMEM", "RLIMIT_AS")
            for (c in constants) {
                addAttr(c, Types.IntInstance)
            }

            val ru = newClass("struct_rusage", table, objectType)
            val ru_fields = arrayOf("ru_utime", "ru_stime", "ru_maxrss", "ru_ixrss", "ru_idrss", "ru_isrss", "ru_minflt", "ru_majflt", "ru_nswap", "ru_inblock", "ru_oublock", "ru_msgsnd", "ru_msgrcv", "ru_nsignals", "ru_nvcsw", "ru_nivcsw")
            for (ruf in ru_fields) {
                addAttr(ru, ruf, Types.IntInstance)
            }
            addClass(ru)

            addFunction("getrusage", ru)
            addFunction("getpagesize", Types.IntInstance)

            for (s in list("RUSAGE_SELF", "RUSAGE_CHILDREN", "RUSAGE_BOTH")) {
                addAttr(s, Types.IntInstance)
            }
        }
    }


    internal inner class SelectModule : NativeModule("select") {


        override fun initBindings() {
            addClass(newException("error", table))

            addFunction("select", newTuple(newList(), newList(), newList()))

            val epoll = newClass("epoll", table, objectType)
            addMethod(epoll, "close", Types.NoneInstance)
            addMethod(epoll, "fileno", Types.IntInstance)
            addMethod(epoll, "fromfd", epoll)
            for (s in list("register", "modify", "unregister", "poll")) {
                addMethod(epoll, s)
            }
            addClass(epoll)

            for (s in list("EPOLLERR", "EPOLLET", "EPOLLHUP", "EPOLLIN", "EPOLLMSG",
                    "EPOLLONESHOT", "EPOLLOUT", "EPOLLPRI", "EPOLLRDBAND",
                    "EPOLLRDNORM", "EPOLLWRBAND", "EPOLLWRNORM")) {
                addAttr(s, Types.IntInstance)
            }


            val poll = newClass("poll", table, objectType)
            addMethod(poll, "register")
            addMethod(poll, "modify")
            addMethod(poll, "unregister")
            addMethod(poll, "poll", newList(newTuple(Types.IntInstance, Types.IntInstance)))
            addClass(poll)

            for (s in list("POLLERR", "POLLHUP", "POLLIN", "POLLMSG",
                    "POLLNVAL", "POLLOUT", "POLLPRI", "POLLRDBAND",
                    "POLLRDNORM", "POLLWRBAND", "POLLWRNORM")) {
                addAttr(s, Types.IntInstance)
            }

            val kqueue = newClass("kqueue", table, objectType)
            addMethod(kqueue, "close", Types.NoneInstance)
            addMethod(kqueue, "fileno", Types.IntInstance)
            addMethod(kqueue, "fromfd", kqueue)
            addMethod(kqueue, "control", newList(newTuple(Types.IntInstance, Types.IntInstance)))
            addClass(kqueue)

            val kevent = newClass("kevent", table, objectType)
            for (s in list("ident", "filter", "flags", "fflags", "data", "udata")) {
                addAttr(kevent, s, Types.UNKNOWN)
            }
            addClass(kevent)
        }
    }


    internal inner class SignalModule : NativeModule("signal") {


        override fun initBindings() {
            addNumAttrs(
                    "NSIG", "SIGABRT", "SIGALRM", "SIGBUS", "SIGCHLD", "SIGCLD",
                    "SIGCONT", "SIGFPE", "SIGHUP", "SIGILL", "SIGINT", "SIGIO",
                    "SIGIOT", "SIGKILL", "SIGPIPE", "SIGPOLL", "SIGPROF", "SIGPWR",
                    "SIGQUIT", "SIGRTMAX", "SIGRTMIN", "SIGSEGV", "SIGSTOP", "SIGSYS",
                    "SIGTERM", "SIGTRAP", "SIGTSTP", "SIGTTIN", "SIGTTOU", "SIGURG",
                    "SIGUSR1", "SIGUSR2", "SIGVTALRM", "SIGWINCH", "SIGXCPU", "SIGXFSZ",
                    "SIG_DFL", "SIG_IGN")

            addUnknownFuncs("default_int_handler", "getsignal", "set_wakeup_fd", "signal")
        }
    }


    internal inner class ShaModule : NativeModule("sha") {


        override fun initBindings() {
            addNumAttrs("blocksize", "digest_size")

            val sha = newClass("sha", table, objectType)
            addMethod(sha, "update")
            addMethod(sha, "digest", Types.StrInstance)
            addMethod(sha, "hexdigest", Types.StrInstance)
            addMethod(sha, "copy", sha)
            addClass(sha)

            update("new", liburl(), newFunc(sha), CONSTRUCTOR)
        }
    }


    internal inner class SpwdModule : NativeModule("spwd") {


        override fun initBindings() {
            val struct_spwd = newClass("struct_spwd", table, objectType)
            for (s in list("sp_nam", "sp_pwd", "sp_lstchg", "sp_min",
                    "sp_max", "sp_warn", "sp_inact", "sp_expire",
                    "sp_flag")) {
                addAttr(struct_spwd, s, Types.IntInstance)
            }
            addAttr("struct_spwd", struct_spwd)

            addFunction("getspnam", struct_spwd)
            addFunction("getspall", newList(struct_spwd))
        }
    }


    internal inner class StropModule : NativeModule("strop") {


        override fun initBindings() {
            table!!.putAll(Types.StrInstance.table)
        }
    }


    internal inner class StructModule : NativeModule("struct") {


        override fun initBindings() {
            addClass(newException("error", table))
            addStrFuncs("pack")
            addUnknownFuncs("pack_into")
            addNumFuncs("calcsize")
            addFunction("unpack", newTuple())
            addFunction("unpack_from", newTuple())

            BaseStruct = newClass("Struct", table, objectType)
            addClass(BaseStruct)
            val t = BaseStruct.table

            addMethod(BaseStruct, "pack", Types.StrInstance)
            addMethod(BaseStruct, "pack_into")
            addMethod(BaseStruct, "unpack", newTuple())
            addMethod(BaseStruct, "unpack_from", newTuple())
            addMethod(BaseStruct, "format", Types.StrInstance)
            addMethod(BaseStruct, "size", Types.IntInstance)
        }
    }


    internal inner class SysModule : NativeModule("sys") {


        override fun initBindings() {
            addUnknownFuncs(
                    "_clear_type_cache", "call_tracing", "callstats", "_current_frames",
                    "_getframe", "displayhook", "dont_write_bytecode", "exitfunc",
                    "exc_clear", "exc_info", "excepthook", "exit",
                    "last_traceback", "last_type", "last_value", "modules",
                    "path_hooks", "path_importer_cache", "getprofile", "gettrace",
                    "setcheckinterval", "setprofile", "setrecursionlimit", "settrace")

            addAttr("exc_type", Types.NoneInstance)

            addUnknownAttrs("__stderr__", "__stdin__", "__stdout__",
                    "stderr", "stdin", "stdout", "version_info")

            addNumAttrs("api_version", "hexversion", "winver", "maxint", "maxsize",
                    "maxunicode", "py3kwarning", "dllhandle")

            addStrAttrs("platform", "byteorder", "copyright", "prefix", "version",
                    "exec_prefix", "executable")

            addNumFuncs("getrecursionlimit", "getwindowsversion", "getrefcount",
                    "getsizeof", "getcheckinterval")

            addStrFuncs("getdefaultencoding", "getfilesystemencoding")

            for (s in list("argv", "builtin_module_names", "path",
                    "meta_path", "subversion")) {
                addAttr(s, newList(Types.StrInstance))
            }

            for (s in list("flags", "warnoptions", "float_info")) {
                addAttr(s, newDict(Types.StrInstance, Types.IntInstance))
            }
        }
    }


    internal inner class SyslogModule : NativeModule("syslog") {


        override fun initBindings() {
            addNoneFuncs("syslog", "openlog", "closelog", "setlogmask")
            addNumAttrs("LOG_ALERT", "LOG_AUTH", "LOG_CONS", "LOG_CRIT", "LOG_CRON",
                    "LOG_DAEMON", "LOG_DEBUG", "LOG_EMERG", "LOG_ERR", "LOG_INFO",
                    "LOG_KERN", "LOG_LOCAL0", "LOG_LOCAL1", "LOG_LOCAL2", "LOG_LOCAL3",
                    "LOG_LOCAL4", "LOG_LOCAL5", "LOG_LOCAL6", "LOG_LOCAL7", "LOG_LPR",
                    "LOG_MAIL", "LOG_MASK", "LOG_NDELAY", "LOG_NEWS", "LOG_NOTICE",
                    "LOG_NOWAIT", "LOG_PERROR", "LOG_PID", "LOG_SYSLOG", "LOG_UPTO",
                    "LOG_USER", "LOG_UUCP", "LOG_WARNING")
        }
    }


    internal inner class TermiosModule : NativeModule("termios") {


        override fun initBindings() {
            addFunction("tcgetattr", newList())
            addUnknownFuncs("tcsetattr", "tcsendbreak", "tcdrain", "tcflush", "tcflow")
        }
    }


    internal inner class ThreadModule : NativeModule("thread") {


        override fun initBindings() {
            addClass(newException("error", table))

            val lock = newClass("lock", table, objectType)
            addMethod(lock, "acquire", Types.IntInstance)
            addMethod(lock, "locked", Types.IntInstance)
            addMethod(lock, "release", Types.NoneInstance)
            addAttr("LockType", BaseType)

            addNoneFuncs("interrupt_main", "exit", "exit_thread")
            addNumFuncs("start_new", "start_new_thread", "get_ident", "stack_size")

            addFunction("allocate", lock)
            addFunction("allocate_lock", lock)  // synonym

            addAttr("_local", BaseType)
        }
    }


    internal inner class TimeModule : NativeModule("time") {


        override fun initBindings() {
            Time_struct_time = InstanceType(newClass("datetime", table, objectType))
            val struct_time = Time_struct_time
            addAttr("struct_time", struct_time)

            val struct_time_attrs = arrayOf("n_fields", "n_sequence_fields", "n_unnamed_fields", "tm_hour", "tm_isdst", "tm_mday", "tm_min", "tm_mon", "tm_wday", "tm_yday", "tm_year")
            for (s in struct_time_attrs) {
                addAttr(struct_time.table, s, Types.IntInstance)
            }

            addNumAttrs("accept2dyear", "altzone", "daylight", "timezone")

            addAttr("tzname", newTuple(Types.StrInstance, Types.StrInstance))
            addNoneFuncs("sleep", "tzset")

            addNumFuncs("clock", "mktime", "time", "tzname")
            addStrFuncs("asctime", "ctime", "strftime")

            addFunctions_beCareful(struct_time, "gmtime", "localtime", "strptime")
        }
    }


    internal inner class UnicodedataModule : NativeModule("unicodedata") {


        override fun initBindings() {
            addNumFuncs("decimal", "digit", "numeric", "combining",
                    "east_asian_width", "mirrored")
            addStrFuncs("lookup", "name", "category", "bidirectional",
                    "decomposition", "normalize")
            addNumAttrs("unidata_version")
            addUnknownAttrs("ucd_3_2_0")
        }
    }


    internal inner class ZipimportModule : NativeModule("zipimport") {


        override fun initBindings() {
            addClass(newException("ZipImportError", table))

            val zipimporter = newClass("zipimporter", table, objectType)
            addMethod(zipimporter, "find_module", zipimporter)
            addMethod(zipimporter, "get_code", Types.UNKNOWN)  // XXX:  code object
            addMethod(zipimporter, "get_data", Types.UNKNOWN)
            addMethod(zipimporter, "get_source", Types.StrInstance)
            addMethod(zipimporter, "is_package", Types.IntInstance)
            addMethod(zipimporter, "load_module", newModule("<?>"))
            addMethod(zipimporter, "archive", Types.StrInstance)
            addMethod(zipimporter, "prefix", Types.StrInstance)

            addClass(zipimporter)
            addAttr("_zip_directory_cache", newDict(Types.StrInstance, Types.UNKNOWN))
        }
    }


    internal inner class ZlibModule : NativeModule("zlib") {


        override fun initBindings() {
            val compress = newClass("Compress", table, objectType)
            for (s in list("compress", "flush")) {
                addMethod(compress, s, Types.StrInstance)
            }
            addMethod(compress, "copy", compress)
            addClass(compress)

            val decompress = newClass("Decompress", table, objectType)
            for (s in list("unused_data", "unconsumed_tail")) {
                addAttr(decompress, s, Types.StrInstance)
            }
            for (s in list("decompress", "flush")) {
                addMethod(decompress, s, Types.StrInstance)
            }
            addMethod(decompress, "copy", decompress)
            addClass(decompress)

            addFunction("adler32", Types.IntInstance)
            addFunction("compress", Types.StrInstance)
            addFunction("compressobj", compress)
            addFunction("crc32", Types.IntInstance)
            addFunction("decompress", Types.StrInstance)
            addFunction("decompressobj", decompress)
        }
    }

    internal inner class UnittestModule : NativeModule("unittest") {

        protected override fun initBindings() {
            val testResult = newClass("TestResult", table, objectType)
            addAttr(testResult, "shouldStop", Types.BoolInstance)
            addAttr(testResult, "testsRun", Types.IntInstance)
            addAttr(testResult, "buffer", Types.BoolInstance)
            addAttr(testResult, "failfast", Types.BoolInstance)

            addMethod(testResult, "wasSuccessful", Types.BoolInstance)
            addMethod(testResult, "stop", Types.NoneInstance)
            addMethod(testResult, "startTest", Types.NoneInstance)
            addMethod(testResult, "stopTest", Types.NoneInstance)
            addMethod(testResult, "startTestRun", Types.NoneInstance)
            addMethod(testResult, "stopTestRun", Types.NoneInstance)
            addMethod(testResult, "addError", Types.NoneInstance)
            addMethod(testResult, "addFailure", Types.NoneInstance)
            addMethod(testResult, "addSuccess", Types.NoneInstance)
            addMethod(testResult, "addSkip", Types.NoneInstance)
            addMethod(testResult, "addExpectedFailure", Types.NoneInstance)
            addMethod(testResult, "addUnexpectedSuccess", Types.NoneInstance)
            addClass(testResult)

            val textTestResult = newClass("TextTestResult", table, testResult)
            addClass(textTestResult)

            val testCase = newClass("TestCase", table, objectType)
            for (s in list("setUp", "tearDown", "setUpClass", "tearDownClass", "skipTest", "debug",
                    "assertEqual", "assertNotEqual", "assertTrue", "assertFalse", "assertIs", "assertIsNot", "assertIsNone",
                    "assertIsNotNone", "assertIn", "assertNotIn", "assertIsInstance", "assertNotIsInstance", "assertRaises",
                    "assertRaisesRegexp", "assertAlmostEqual", "assertNotAlmostEqual", "assertGreater",
                    "assertGreaterEqual", "assertLess", "assertLessEqual", "assertRegexpMatches", "assertNotRegexpMatches",
                    "assertItemsEqual", "assertDictContainsSubset", "addTypeEqualityFunc", "assertMultiLineEqual",
                    "assertSequenceEqual", "assertListEqual", "assertTupleEqual", "assertSetEqual", "assertDictEqual",
                    "fail", "failureException", "addCleanup", "doCleanups")) {
                addMethod(testCase, s, Types.NoneInstance)
            }
            addMethod(testCase, "countTestCases", Types.IntInstance)
            addMethod(testCase, "id", Types.StrInstance)
            addMethod(testCase, "shortDescription", Types.StrInstance)
            addMethod(testCase, "defaultTestResult", testResult)
            addMethod(testCase, "run", testResult)
            addAttr(testCase, "longMessage", Types.BoolInstance)
            addAttr(testCase, "maxDiff", Types.IntInstance)
            addClass(testCase)


            val testSuite = newClass("TestSuite", table, objectType)
            addMethod(testSuite, "addTest", Types.NoneInstance)
            addMethod(testSuite, "addTests", Types.NoneInstance)
            addMethod(testSuite, "run", testResult)
            addMethod(testSuite, "debug", Types.NoneInstance)
            addMethod(testSuite, "countTestCases", Types.IntInstance)
            addMethod(testSuite, "__iter__", newFunc(testCase))
            addClass(testSuite)


            val testLoader = newClass("TestLoader", table, objectType)
            addMethod(testLoader, "loadTestsFromTestCase", testSuite)
            addMethod(testLoader, "loadTestsFromModule", testSuite)
            addMethod(testLoader, "loadTestsFromName", testSuite)
            addMethod(testLoader, "loadTestsFromNames", testSuite)
            addMethod(testLoader, "getTestCaseNames", testCase)
            addMethod(testLoader, "discover", testSuite)
            addAttr(testLoader, "testMethodPrefix", Types.StrInstance)
            addAttr(testLoader, "sortTestMethodsUsing", Types.StrInstance)
            addAttr(testLoader, "suiteClass", newFunc(testSuite))
            addClass(testLoader)

            addAttr("defaultTestLoader", testLoader)

            val textTestRunner = newClass("TextTestRunner", table, objectType)
            addClass(textTestRunner)

            addNoneFuncs("main", "installHandler", "registerResult", "removeResult", "removeHandler")

            addAttr(testResult, "errors", newList(newTuple(testCase, Types.StrInstance)))
            addAttr(testResult, "failures", newList(newTuple(testCase, Types.StrInstance)))
            addAttr(testResult, "skipped", newList(newTuple(testCase, Types.StrInstance)))
            addAttr(testResult, "expectedFailures", newList(newTuple(testCase, Types.StrInstance)))
            addAttr(testResult, "unexpectedSuccesses", newList(newTuple(testCase, Types.StrInstance)))

        }
    }

    companion object {

        val LIBRARY_URL = "http://docs.python.org/library/"
        val TUTORIAL_URL = "http://docs.python.org/tutorial/"
        val REFERENCE_URL = "http://docs.python.org/reference/"
        val DATAMODEL_URL = "http://docs.python.org/reference/datamodel#"


        fun newLibUrl(module: String, name: String): Url {
            return newLibUrl("$module.html#$name")
        }


        fun newLibUrl(path: String): Url {
            var path = path
            if (!path.contains("#") && !path.endsWith(".html")) {
                path += ".html"
            }
            return Url(LIBRARY_URL + path)
        }


        fun newRefUrl(path: String): Url {
            return Url(REFERENCE_URL + path)
        }


        fun newDataModelUrl(path: String): Url {
            return Url(DATAMODEL_URL + path)
        }


        fun newTutUrl(path: String): Url {
            return Url(TUTORIAL_URL + path)
        }
    }
}
