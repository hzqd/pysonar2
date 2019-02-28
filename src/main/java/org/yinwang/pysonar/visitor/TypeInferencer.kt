package org.yinwang.pysonar.visitor

import org.yinwang.pysonar.`$`
import org.yinwang.pysonar.Analyzer
import org.yinwang.pysonar.Binding
import org.yinwang.pysonar.Builtins
import org.yinwang.pysonar.State
import org.yinwang.pysonar.ast.*
import org.yinwang.pysonar.CallStackEntry
import org.yinwang.pysonar.types.ClassType
import org.yinwang.pysonar.types.DictType
import org.yinwang.pysonar.types.FunType
import org.yinwang.pysonar.types.InstanceType
import org.yinwang.pysonar.types.ListType
import org.yinwang.pysonar.types.ModuleType
import org.yinwang.pysonar.types.TupleType
import org.yinwang.pysonar.types.Type
import org.yinwang.pysonar.types.Types
import org.yinwang.pysonar.types.UnionType

import org.yinwang.pysonar.Binding.Kind.ATTRIBUTE
import org.yinwang.pysonar.Binding.Kind.CLASS
import org.yinwang.pysonar.Binding.Kind.CONSTRUCTOR
import org.yinwang.pysonar.Binding.Kind.FUNCTION
import org.yinwang.pysonar.Binding.Kind.METHOD
import org.yinwang.pysonar.Binding.Kind.MODULE
import org.yinwang.pysonar.Binding.Kind.PARAMETER
import org.yinwang.pysonar.Binding.Kind.SCOPE
import org.yinwang.pysonar.Binding.Kind.VARIABLE

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class TypeInferencer : Visitor1<Type, State> {

    override fun visit(node: Module, s: State): Type {
        val mt = ModuleType(node.name, node.file, Analyzer.self.globaltable)
        s.insert(`$`.moduleQname(node.file), node, mt, MODULE)
        if (node.body != null) {
            visit(node.body, mt.table)
        }
        return mt
    }

    override fun visit(node: Alias, s: State): Type {
        return Types.UNKNOWN
    }

    override fun visit(node: Assert, s: State): Type {
        if (node.test != null) {
            visit(node.test, s)
        }
        if (node.msg != null) {
            visit(node.msg, s)
        }
        return Types.CONT
    }

    override fun visit(node: Assign, s: State): Type {
        val valueType = visit(node.value, s)
        bind(s, node.target, valueType)
        return Types.CONT
    }

    override fun visit(node: Attribute, s: State): Type {
        val targetType = visit(node.target, s)
        if (targetType is UnionType) {
            val types = targetType.types
            var retType = Types.UNKNOWN
            for (tt in types) {
                retType = UnionType.union(retType, getAttrType(node, tt))
            }
            return retType
        } else {
            return getAttrType(node, targetType)
        }
    }

    override fun visit(node: Await, s: State): Type {
        return if (node.value == null) {
            Types.NoneInstance
        } else {
            visit(node.value, s)
        }
    }

    override fun visit(node: BinOp, s: State): Type {
        val ltype = visit(node.left, s)
        val rtype = visit(node.right, s)
        if (operatorOverridden(ltype, node.op.method)) {
            val result = applyOp(node.op, ltype, rtype, node.op.method, node, node.left)
            if (result != null) {
                return result
            }
        } else if (Op.isBoolean(node.op)) {
            return Types.BoolInstance
        } else if (ltype === Types.UNKNOWN) {
            return rtype
        } else if (rtype === Types.UNKNOWN) {
            return ltype
        } else if (ltype.typeEquals(rtype)) {
            return ltype
        } else if (node.op === Op.Or) {
            if (rtype === Types.NoneInstance) {
                return ltype
            } else if (ltype === Types.NoneInstance) {
                return rtype
            }
        } else if (node.op === Op.And) {
            if (rtype === Types.NoneInstance || ltype === Types.NoneInstance) {
                return Types.NoneInstance
            }
        }

        addWarningToNode(node, "Cannot apply binary operator " + node.op.rep + " to type " + ltype + " and " + rtype)
        return Types.UNKNOWN
    }

    private fun operatorOverridden(type: Type, method: String?): Boolean {
        if (type is InstanceType) {
            val opType = type.table.lookupAttrType(method)
            if (opType != null) {
                return true
            }
        }
        return false
    }

    private fun applyOp(op: Op, ltype: Type, rtype: Type, method: String?, node: Node, left: Node): Type? {
        val opType = ltype.table.lookupAttrType(method)
        if (opType is FunType) {
            return apply((opType as FunType?)!!, ltype, listOf<Type>(rtype), null, null, null, node)
        } else {
            addWarningToNode(left, "Operator method $method is not a function")
            return null
        }
    }

    override fun visit(node: Block, s: State): Type {
        // first pass: mark global names
        for (n in node.seq) {
            if (n is Global) {
                for (name in n.names) {
                    s.addGlobalName(name.id)
                    val nb = s.lookup(name.id)
                    if (nb != null) {
                        Analyzer.self.putRef(name, nb)
                    }
                }
            }
        }

        var returned = false
        var retType = Types.UNKNOWN

        for (n in node.seq) {
            val t = visit(n, s)
            if (!returned) {
                retType = UnionType.union(retType, t)
                if (!UnionType.contains(t, Types.CONT)) {
                    returned = true
                    retType = UnionType.remove(retType, Types.CONT)
                }
            }
        }

        return retType
    }

    override fun visit(node: Break, s: State): Type {
        return Types.NoneInstance
    }

    override fun visit(node: Bytes, s: State): Type {
        return Types.StrInstance
    }

    override fun visit(node: Call, s: State): Type {
        val `fun`: Type
        var selfType: Type? = null

        if (node.func is Attribute) {
            val target = (node.func as Attribute).target
            val attr = (node.func as Attribute).attr
            val targetType = visit(target, s)
            if (targetType !is ModuleType) {
                selfType = targetType
            }
            val b = targetType.table.lookupAttr(attr.id)
            if (b != null) {
                Analyzer.self.putRef(attr, b)
                `fun` = State.makeUnion(b)
            } else {
                Analyzer.self.putProblem(attr, "Attribute is not found in type: " + attr.id)
                `fun` = Types.UNKNOWN
            }
        } else {
            `fun` = visit(node.func, s)
        }

        // Infer positional argument types
        val positional = visit<out Node, Type>(node.args, s)

        // Infer keyword argument types
        val kwTypes = HashMap<String, Type>()
        if (node.keywords != null) {
            for (k in node.keywords!!) {
                kwTypes[k.arg] = visit(k.value, s)
            }
        }

        val kwArg = if (node.kwargs == null) null else visit(node.kwargs, s)
        val starArg = if (node.starargs == null) null else visit(node.starargs, s)

        if (`fun` is UnionType) {
            val types = `fun`.types
            var resultType = Types.UNKNOWN
            for (funType in types) {
                val returnType = resolveCall(funType, selfType, positional, kwTypes, kwArg, starArg, node)
                resultType = UnionType.union(resultType, returnType)
            }
            return resultType
        } else {
            return resolveCall(`fun`, selfType, positional, kwTypes, kwArg, starArg, node)
        }
    }

    override fun visit(node: ClassDef, s: State): Type {
        val classType = ClassType(node.name.id, s)
        val baseTypes = ArrayList<Type>()
        for (base in node.bases) {
            val baseType = visit(base, s)
            if (baseType is ClassType) {
                classType.addSuper(baseType)
            } else if (baseType is UnionType) {
                for (parent in baseType.types) {
                    classType.addSuper(parent)
                }
            } else {
                addWarningToNode(base, "$base is not a class")
            }
            baseTypes.add(baseType)
        }

        // XXX: Not sure if we should add "bases", "name" and "dict" here. They
        // must be added _somewhere_ but I'm just not sure if it should be HERE.
        node.addSpecialAttribute(classType.table, "__bases__", TupleType(baseTypes))
        node.addSpecialAttribute(classType.table, "__name__", Types.StrInstance)
        node.addSpecialAttribute(classType.table, "__dict__",
                DictType(Types.StrInstance, Types.UNKNOWN))
        node.addSpecialAttribute(classType.table, "__module__", Types.StrInstance)
        node.addSpecialAttribute(classType.table, "__doc__", Types.StrInstance)

        // Bind ClassType to name here before resolving the body because the
        // methods need node type as self.
        bind(s, node.name, classType, CLASS)
        if (node.body != null) {
            visit(node.body, classType.table)
        }
        return Types.CONT
    }

    override fun visit(node: Comprehension, s: State): Type {
        bindIter(s, node.target, node.iter, SCOPE)
        visit<out Node, Type>(node.ifs, s)
        return visit(node.target, s)
    }

    override fun visit(node: Continue, s: State): Type {
        return Types.CONT
    }

    override fun visit(node: Delete, s: State): Type {
        for (n in node.targets) {
            visit(n, s)
            if (n is Name) {
                s.remove(n.id)
            }
        }
        return Types.CONT
    }

    override fun visit(node: Dict, s: State): Type {
        val keyType = resolveUnion(node.keys, s)
        val valType = resolveUnion(node.values, s)
        return DictType(keyType, valType)
    }

    override fun visit(node: DictComp, s: State): Type {
        visit<Comprehension, Type>(node.generators, s)
        val keyType = visit(node.key, s)
        val valueType = visit(node.value, s)
        return DictType(keyType, valueType)
    }

    override fun visit(node: Dummy, s: State): Type {
        return Types.UNKNOWN
    }

    override fun visit(node: Ellipsis, s: State): Type {
        return Types.NoneInstance
    }

    override fun visit(node: Exec, s: State): Type {
        if (node.body != null) {
            visit(node.body, s)
        }
        if (node.globals != null) {
            visit(node.globals, s)
        }
        if (node.locals != null) {
            visit(node.locals, s)
        }
        return Types.CONT
    }

    override fun visit(node: Expr, s: State): Type {
        if (node.value != null) {
            visit(node.value, s)
        }
        return Types.CONT

    }

    override fun visit(node: ExtSlice, s: State): Type {
        for (d in node.dims) {
            visit(d, s)
        }
        return ListType()
    }

    override fun visit(node: For, s: State): Type {
        bindIter(s, node.target, node.iter, SCOPE)
        var t1 = Types.UNKNOWN
        var t2 = Types.UNKNOWN
        var t3 = Types.UNKNOWN

        val s1 = s.copy()
        val s2 = s.copy()

        if (node.body != null) {
            t1 = visit(node.body, s1)
            s.merge(s1)
            t2 = visit(node.body, s1)
            s.merge(s1)
        }

        if (node.orelse != null) {
            t3 = visit(node.orelse, s2)
            s.merge(s2)
        }

        return UnionType.union(UnionType.union(t1, t2), t3)
    }

    override fun visit(node: FunctionDef, s: State): Type {
        val env = s.getForwarding()
        val `fun` = FunType(node, env)
        `fun`.table.setParent(s)
        `fun`.table.setPath(s.extendPath(node.name.id))
        `fun`.setDefaultTypes(visit(node.defaults, s))
        Analyzer.self.addUncalled(`fun`)
        val funkind: Binding.Kind

        if (node.isLamba) {
            return `fun`
        } else {
            if (s.stateType == State.StateType.CLASS) {
                if ("__init__" == node.name.id) {
                    funkind = CONSTRUCTOR
                } else {
                    funkind = METHOD
                }
            } else {
                funkind = FUNCTION
            }

            val outType = s.type
            if (outType is ClassType) {
                `fun`.setCls(outType)
            }

            bind(s, node.name, `fun`, funkind)
            return Types.CONT
        }
    }

    override fun visit(node: GeneratorExp, s: State): Type {
        visit<Comprehension, Type>(node.generators, s)
        return ListType(visit(node.elt, s))
    }

    override fun visit(node: Global, s: State): Type {
        return Types.CONT
    }

    override fun visit(node: Handler, s: State): Type {
        var typeval = Types.UNKNOWN
        if (node.exceptions != null) {
            typeval = resolveUnion(node.exceptions, s)
        }
        if (node.binder != null) {
            bind(s, node.binder, typeval)
        }
        return if (node.body != null) {
            visit(node.body, s)
        } else {
            Types.UNKNOWN
        }
    }

    override fun visit(node: If, s: State): Type {
        val type1: Type
        val type2: Type
        val s1 = s.copy()
        val s2 = s.copy()

        // Ignore result because Python can treat anything as bool
        visit(node.test, s)
        inferInstance(node.test, s, s1)

        if (node.body != null) {
            type1 = visit(node.body, s1)
        } else {
            type1 = Types.CONT
        }

        if (node.orelse != null) {
            type2 = visit(node.orelse, s2)
        } else {
            type2 = Types.CONT
        }

        val cont1 = UnionType.contains(type1, Types.CONT)
        val cont2 = UnionType.contains(type2, Types.CONT)

        // decide which branch affects the downstream state
        if (cont1 && cont2) {
            s1.merge(s2)
            s.overwrite(s1)
        } else if (cont1) {
            s.overwrite(s1)
        } else if (cont2) {
            s.overwrite(s2)
        }

        return UnionType.union(type1, type2)
    }

    /**
     * Helper for branch inference for 'isinstance'
     */
    private fun inferInstance(test: Node, s: State, s1: State) {
        if (test is Call) {
            if (test.func is Name) {
                val testFunc = test.func as Name
                if (testFunc.id == "isinstance") {
                    if (test.args.size >= 2) {
                        val id = test.args[0]
                        if (id is Name) {
                            val typeExp = test.args[1]
                            var type = visit(typeExp, s)
                            if (type is ClassType) {
                                type = type.getInstance(null, this, test)
                            }
                            s1.insert(id.id, id, type, VARIABLE)
                        }
                    }

                    if (test.args.size != 2) {
                        addWarningToNode(test, "Incorrect number of arguments for isinstance")
                    }
                }
            }
        }
    }

    override fun visit(node: IfExp, s: State): Type {
        val type1: Type
        val type2: Type
        visit(node.test, s)

        if (node.body != null) {
            type1 = visit(node.body, s)
        } else {
            type1 = Types.CONT
        }
        if (node.orelse != null) {
            type2 = visit(node.orelse, s)
        } else {
            type2 = Types.CONT
        }
        return UnionType.union(type1, type2)
    }

    override fun visit(node: Import, s: State): Type {
        for (a in node.names) {
            val mod = Analyzer.self.loadModule(a.name, s)
            if (mod == null) {
                addWarningToNode(node, "Cannot load module")
            } else if (a.asname != null) {
                s.insert(a.asname.id, a.asname, mod, VARIABLE)
            }
        }
        return Types.CONT
    }

    override fun visit(node: ImportFrom, s: State): Type {
        if (node.module == null) {
            return Types.CONT
        }

        val mod = Analyzer.self.loadModule(node.module, s)

        if (mod == null) {
            addWarningToNode(node, "Cannot load module")
        } else if (node.isImportStar) {
            node.importStar(s, mod)
        } else {
            for (a in node.names) {
                val first = a.name[0]
                val bs = mod.table.lookup(first.id)
                if (bs != null) {
                    if (a.asname != null) {
                        s.update(a.asname.id, bs)
                        Analyzer.self.putRef(a.asname, bs)
                    } else {
                        s.update(first.id, bs)
                        Analyzer.self.putRef(first, bs)
                    }
                } else {
                    val ext = ArrayList(node.module)
                    ext.add(first)
                    val mod2 = Analyzer.self.loadModule(ext, s)
                    if (mod2 != null) {
                        if (a.asname != null) {
                            val binding = Binding.createFileBinding(a.asname.id, mod2.file, mod2)
                            s.update(a.asname.id, binding)
                            Analyzer.self.putRef(a.asname, binding)
                        } else {
                            val binding = Binding.createFileBinding(first.id, mod2.file, mod2)
                            s.update(first.id, binding)
                            Analyzer.self.putRef(first, binding)
                        }
                    }
                }
            }
        }

        return Types.CONT
    }

    override fun visit(node: Index, s: State): Type {
        return visit(node.value, s)
    }

    override fun visit(node: Keyword, s: State): Type {
        return visit(node.value, s)
    }

    override fun visit(node: ListComp, s: State): Type {
        visit<Comprehension, Type>(node.generators, s)
        return ListType(visit(node.elt, s))
    }

    override fun visit(node: Name, s: State): Type {
        val b = s.lookup(node.id)
        if (b != null) {
            Analyzer.self.putRef(node, b)
            Analyzer.self.resolved.add(node)
            Analyzer.self.unresolved.remove(node)
            return State.makeUnion(b)
        } else {
            addWarningToNode(node, "unbound variable " + node.id)
            Analyzer.self.unresolved.add(node)
            val t = Types.UNKNOWN
            t.table.setPath(s.extendPath(node.id))
            return t
        }
    }

    override fun visit(node: Pass, s: State): Type {
        return Types.CONT
    }

    override fun visit(node: Print, s: State): Type {
        if (node.dest != null) {
            visit(node.dest, s)
        }
        if (node.values != null) {
            visit<out Node, Type>(node.values, s)
        }
        return Types.CONT
    }

    override fun visit(node: PyComplex, s: State): Type {
        return Types.ComplexInstance
    }

    override fun visit(node: PyFloat, s: State): Type {
        return Types.FloatInstance
    }

    override fun visit(node: PyInt, s: State): Type {
        return Types.IntInstance
    }

    override fun visit(node: PyList, s: State): Type {
        if (node.elts.size == 0) {
            return ListType()  // list<unknown>
        }

        val listType = ListType()
        for (elt in node.elts) {
            listType.add(visit(elt, s))
            if (elt is Str) {
                listType.addValue(elt.value)
            }
        }

        return listType
    }

    override fun visit(node: PySet, s: State): Type {
        if (node.elts.size == 0) {
            return ListType()
        }

        var listType: ListType? = null
        for (elt in node.elts) {
            if (listType == null) {
                listType = ListType(visit(elt, s))
            } else {
                listType.add(visit(elt, s))
            }
        }

        return listType
    }

    override fun visit(node: Raise, s: State): Type {
        if (node.exceptionType != null) {
            visit(node.exceptionType, s)
        }
        if (node.inst != null) {
            visit(node.inst, s)
        }
        if (node.traceback != null) {
            visit(node.traceback, s)
        }
        return Types.CONT
    }

    override fun visit(node: Repr, s: State): Type {
        if (node.value != null) {
            visit(node.value, s)
        }
        return Types.StrInstance
    }

    override fun visit(node: Return, s: State): Type {
        if (node.value == null) {
            return Types.NoneInstance
        } else {
            val result = visit(node.value, s)

            val entry = Analyzer.self.callStack.top()
            entry?.`fun`?.addMapping(entry.from, result)

            return result
        }
    }

    override fun visit(node: SetComp, s: State): Type {
        visit<Comprehension, Type>(node.generators, s)
        return ListType(visit(node.elt, s))
    }

    override fun visit(node: Slice, s: State): Type {
        if (node.lower != null) {
            visit(node.lower, s)
        }
        if (node.step != null) {
            visit(node.step, s)
        }
        if (node.upper != null) {
            visit(node.upper, s)
        }
        return ListType()
    }

    override fun visit(node: Starred, s: State): Type {
        return visit(node.value, s)
    }

    override fun visit(node: Str, s: State): Type {
        return Types.StrInstance
    }

    override fun visit(node: Subscript, s: State): Type {
        val vt = visit(node.value, s)
        val st = if (node.slice == null) null else visit(node.slice!!, s)

        if (vt is UnionType) {
            var retType = Types.UNKNOWN
            for (t in vt.types) {
                retType = UnionType.union(retType, getSubscript(node, t, st, s))
            }
            return retType
        } else {
            return getSubscript(node, vt, st, s)
        }
    }

    override fun visit(node: Try, s: State): Type {
        var tp1 = Types.UNKNOWN
        var tp2 = Types.UNKNOWN
        var tph = Types.UNKNOWN
        var tpFinal = Types.UNKNOWN

        if (node.handlers != null) {
            for (h in node.handlers) {
                tph = UnionType.union(tph, visit(h, s))
            }
        }

        if (node.body != null) {
            tp1 = visit(node.body, s)
        }

        if (node.orelse != null) {
            tp2 = visit(node.orelse, s)
        }

        if (node.finalbody != null) {
            tpFinal = visit(node.finalbody, s)
        }

        return UnionType(tp1, tp2, tph, tpFinal)
    }

    override fun visit(node: Tuple, s: State): Type {
        val t = TupleType()
        for (e in node.elts) {
            t.add(visit(e, s))
        }
        return t
    }

    override fun visit(node: UnaryOp, s: State): Type {
        return visit(node.operand, s)
    }

    override fun visit(node: Unsupported, s: State): Type {
        return Types.NoneInstance
    }

    override fun visit(node: Url, s: State): Type {
        return Types.StrInstance
    }

    override fun visit(node: While, s: State): Type {
        visit(node.test, s)
        var t1 = Types.UNKNOWN
        var t2 = Types.UNKNOWN
        var t3 = Types.UNKNOWN

        val s1 = s.copy()
        val s2 = s.copy()

        if (node.body != null) {
            t1 = visit(node.body, s1)
            s.merge(s1)

            t2 = visit(node.body, s1)
            s.merge(s1)
        }

        if (node.orelse != null) {
            t3 = visit(node.orelse, s2)
            s.merge(s2)
        }

        return UnionType.union(UnionType.union(t1, t2), t3)
    }

    override fun visit(node: With, s: State): Type {
        for (item in node.items) {
            val `val` = visit(item.context_expr, s)
            if (item.optional_vars != null) {
                bind(s, item.optional_vars, `val`)
            }
        }
        return visit(node.body, s)
    }

    override fun visit(node: Withitem, s: State): Type {
        return Types.UNKNOWN
    }

    override fun visit(node: Yield, s: State): Type {
        return if (node.value != null) {
            ListType(visit(node.value, s))
        } else {
            Types.NoneInstance
        }
    }

    override fun visit(node: YieldFrom, s: State): Type {
        return if (node.value != null) {
            ListType(visit(node.value, s))
        } else {
            Types.NoneInstance
        }
    }

    private fun resolveUnion(nodes: Collection<Node>, s: State): Type {
        var result = Types.UNKNOWN
        for (node in nodes) {
            val nodeType = visit(node, s)
            result = UnionType.union(result, nodeType)
        }
        return result
    }

    fun setAttr(node: Attribute, s: State, v: Type) {
        val targetType = visit(node.target, s)
        if (targetType is UnionType) {
            val types = targetType.types
            for (tp in types) {
                setAttrType(node, tp, v)
            }
        } else {
            setAttrType(node, targetType, v)
        }
    }

    private fun setAttrType(node: Attribute, targetType: Type, v: Type) {
        if (targetType.isUnknownType) {
            addWarningToNode(node, "Can't set attribute for UnknownType")
            return
        }

        val bs = targetType.table.lookupAttr(node.attr.id)
        if (bs != null) {
            for (b in bs) {
                b.addType(v)
                Analyzer.self.putRef(node.attr, b)
            }
        } else {
            targetType.table.insert(node.attr.id, node.attr, v, ATTRIBUTE)
        }
    }

    fun getAttrType(node: Attribute, targetType: Type): Type {
        val bs = targetType.table.lookupAttr(node.attr.id)
        if (bs == null) {
            addWarningToNode(node.attr, "attribute not found in type: $targetType")
            val t = Types.UNKNOWN
            t.table.setPath(targetType.table.extendPath(node.attr.id))
            return t
        } else {
            for (b in bs) {
                Analyzer.self.putRef(node.attr, b)
            }
            return State.makeUnion(bs)
        }
    }

    fun resolveCall(`fun`: Type,
                    selfType: Type?,
                    positional: List<Type>,
                    kwTypes: MutableMap<String, Type>,
                    kwArg: Type?,
                    starArg: Type?,
                    node: Call): Type {
        if (`fun` is FunType) {
            return apply(`fun`, selfType, positional, kwTypes, kwArg, starArg, node)
        } else if (`fun` is ClassType) {
            return InstanceType(`fun`, positional, this, node)
        } else {
            addWarningToNode(node, "calling non-function and non-class: $`fun`")
            return Types.UNKNOWN
        }
    }

    fun apply(func: FunType,
              selfType: Type?,
              positional: List<Type>?,
              kwTypes: MutableMap<String, Type>?,
              kwArg: Type?,
              starArg: Type?,
              call: Node?): Type {
        if (call is Call &&
                call.func is Attribute &&
                (call.func as Attribute).attr.id == "append") {
            if (selfType is ListType) {
                val listType = selfType as ListType?
                if (positional != null && positional.size == 1) {
                    listType!!.add(positional[0])
                } else {
                    Analyzer.self.putProblem(call, "Calling append with wrong argument types")
                }
            }
        }

        if (call is Call &&
                call.func is Attribute &&
                (call.func as Attribute).attr.id == "update") {
            if (selfType is DictType) {
                val dict = selfType as DictType?
                if (positional != null && positional.size == 1) {
                    val argType = positional[0]
                    if (argType is DictType) {
                        dict!!.keyType = UnionType.union(dict.keyType, argType.keyType)
                        dict.valueType = UnionType.union(dict.valueType, argType.valueType)
                    }
                } else {
                    Analyzer.self.putProblem(call, "Calling update with wrong argument types")
                }
            }
        }

        Analyzer.self.removeUncalled(func)

        if (func.func != null && !func.func.called) {
            Analyzer.self.nCalled++
            func.func.called = true
        }

        if (func.func == null) {
            // func without definition (possibly builtins)
            return func.returnType
        }

        val argTypes = ArrayList<Type>()

        // Add class or object as first argument if it is not static method
        if (!func.func.isStaticMethod) {
            if (func.func.isClassMethod) {
                if (func.cls != null) {
                    argTypes.add(func.cls)
                } else if (selfType != null && selfType is InstanceType) {
                    argTypes.add(selfType.classType)
                }
            } else {
                // usual method
                if (selfType != null) {
                    argTypes.add(selfType)
                } else {
                    if (func.cls != null) {
                        if (func.func.name.id != "__init__") {
                            argTypes.add(func.cls!!.getInstance(null, this, call))
                        } else {
                            argTypes.add(func.cls!!.instance)
                        }
                    }
                }
            }
        }

        // Put in positional arguments
        if (positional != null) {
            argTypes.addAll(positional)
        }

        bindMethodAttrs(func)

        val callState = State(func.env, State.StateType.FUNCTION)

        if (func.table.parent != null) {
            callState.setPath(func.table.parent!!.extendPath(func.func.name.id))
        } else {
            callState.setPath(func.func.name.id)
        }

        val fromType = bindParams(callState, func.func, argTypes, func.defaultTypes, kwTypes, kwArg, starArg)
        val cachedTo = func.getMapping(fromType)

        if (cachedTo != null) {
            return cachedTo
        } else if (func.oversized()) {
            return Types.UNKNOWN
        } else {
            func.addMapping(fromType, Types.UNKNOWN)
            Analyzer.self.callStack.push(CallStackEntry(func, fromType))
            var toType = visit(func.func.body, callState)
            Analyzer.self.callStack.pop()
            if (missingReturn(toType)) {
                addWarningToNode(func.func.name, "Function not always return a value")

                if (call != null) {
                    addWarningToNode(call, "Call not always return a value")
                }
            }

            toType = UnionType.remove(toType, Types.CONT)
            if (func.func.name.id != "__init__") {
                func.addMapping(fromType, toType)
            } else {
                func.removeMapping(fromType)
            }

            return toType
        }
    }

    private fun bindParams(state: State,
                           func: FunctionDef,
                           pTypes: List<Type>?,
                           dTypes: List<Type>?,
                           hash: MutableMap<String, Type>?,
                           kw: Type?,
                           star: Type?): Type {
        var star = star

        val args = func.args
        val rest = func.vararg
        val restKw = func.kwarg

        val fromType = TupleType()
        val pSize = args?.size ?: 0
        val aSize = pTypes?.size ?: 0
        val dSize = dTypes?.size ?: 0
        val nPos = pSize - dSize

        if (star != null && star is ListType) {
            star = star.toTupleType()
        }

        run {
            var i = 0
            var j = 0
            while (i < pSize) {
                val arg = args[i]
                val aType: Type
                if (i < aSize) {
                    aType = pTypes!![i]
                } else if (i - nPos >= 0 && i - nPos < dSize) {
                    aType = dTypes!![i - nPos]
                } else {
                    if (hash != null && args[i] is Name &&
                            hash.containsKey((args[i] as Name).id)) {
                        aType = hash[(args[i] as Name).id]
                        hash.remove((args[i] as Name).id)
                    } else {
                        if (star != null && star is TupleType &&
                                j < star.eltTypes.size) {
                            aType = star.get(j++)
                        } else {
                            aType = Types.UNKNOWN
                            addWarningToNode(args[i], "unable to bind argument:" + args[i])
                        }
                    }
                }
                bind(state, arg, aType, PARAMETER)
                fromType.add(aType)
                i++
            }
        }

        if (restKw != null) {
            if (hash != null && !hash.isEmpty()) {
                val hashType = UnionType.newUnion(hash.values)
                bind(state, restKw, DictType(Types.StrInstance, hashType), PARAMETER)
            } else {
                bind(state, restKw, Types.UNKNOWN, PARAMETER)
            }
        }

        if (rest != null) {
            if (pTypes!!.size > pSize) {
                if (func.afterRest != null) {
                    val nAfter = func.afterRest!!.size
                    for (i in 0 until nAfter) {
                        bind(state, func.afterRest!![i], pTypes[pTypes.size - nAfter + i], PARAMETER)
                    }
                    if (pTypes.size - nAfter > 0) {
                        val restType = TupleType(pTypes.subList(pSize, pTypes.size - nAfter))
                        bind(state, rest, restType, PARAMETER)
                    }
                } else {
                    val restType = TupleType(pTypes.subList(pSize, pTypes.size))
                    bind(state, rest, restType, PARAMETER)
                }
            } else {
                bind(state, rest, Types.UNKNOWN, PARAMETER)
            }
        }

        return fromType
    }

    fun getSubscript(node: Node, vt: Type, st: Type?, s: State): Type {
        if (vt.isUnknownType) {
            return Types.UNKNOWN
        } else {
            if (vt is ListType) {
                return getListSubscript(node, vt, st, s)
            } else if (vt is TupleType) {
                return getListSubscript(node, vt.toListType(), st, s)
            } else if (vt is DictType) {
                if (vt.keyType != st) {
                    addWarningToNode(node, "Possible KeyError (wrong type for subscript)")
                }
                return vt.valueType
            } else if (vt === Types.StrInstance) {
                if (st != null && (st is ListType || st.isNumType)) {
                    return vt
                } else {
                    addWarningToNode(node, "Possible KeyError (wrong type for subscript)")
                    return Types.UNKNOWN
                }
            } else {
                return Types.UNKNOWN
            }
        }
    }

    private fun getListSubscript(node: Node, vt: Type, st: Type?, s: State): Type {
        if (vt is ListType) {
            if (st != null && st is ListType) {
                return vt
            } else if (st == null || st.isNumType) {
                return vt.eltType
            } else {
                val sliceFunc = vt.table.lookupAttrType("__getslice__")
                if (sliceFunc == null) {
                    addError(node, "The type can't be sliced: $vt")
                    return Types.UNKNOWN
                } else if (sliceFunc is FunType) {
                    return apply((sliceFunc as FunType?)!!, null, null, null, null, null, node)
                } else {
                    addError(node, "The type's __getslice__ method is not a function: $sliceFunc")
                    return Types.UNKNOWN
                }
            }
        } else {
            return Types.UNKNOWN
        }
    }

    fun bind(s: State, target: Node?, rvalue: Type, kind: Binding.Kind) {
        if (target is Name) {
            bind(s, (target as Name?)!!, rvalue, kind)
        } else if (target is Tuple) {
            bind(s, target.elts, rvalue, kind)
        } else if (target is PyList) {
            bind(s, target.elts, rvalue, kind)
        } else if (target is Attribute) {
            setAttr(target as Attribute?, s, rvalue)
        } else if (target is Subscript) {
            val sub = target as Subscript?
            val sliceType = if (sub!!.slice == null) null else visit(sub.slice!!, s)
            val valueType = visit(sub.value, s)
            if (valueType is ListType) {
                valueType.setElementType(UnionType.union(valueType.eltType, rvalue))
            } else if (valueType is DictType) {
                if (sliceType != null) {
                    valueType.setKeyType(UnionType.union(valueType.keyType, sliceType))
                }
                valueType.setValueType(UnionType.union(valueType.valueType, rvalue))
            }
        } else if (target != null) {
            addWarningToNode(target, "invalid location for assignment")
        }
    }

    /**
     * Without specifying a kind, bind determines the kind according to the type
     * of the scope.
     */
    fun bind(s: State, target: Node?, rvalue: Type) {
        val kind: Binding.Kind
        if (s.stateType == State.StateType.FUNCTION) {
            kind = VARIABLE
        } else if (s.stateType == State.StateType.CLASS || s.stateType == State.StateType.INSTANCE) {
            kind = ATTRIBUTE
        } else {
            kind = SCOPE
        }
        bind(s, target, rvalue, kind)
    }

    fun bind(s: State, xs: List<Node>, rvalue: Type, kind: Binding.Kind) {
        if (rvalue is TupleType) {
            val vs = rvalue.eltTypes
            if (xs.size != vs.size) {
                reportUnpackMismatch(xs, vs.size)
            } else {
                for (i in xs.indices) {
                    bind(s, xs[i], vs[i], kind)
                }
            }
        } else if (rvalue is ListType) {
            bind(s, xs, rvalue.toTupleType(xs.size), kind)
        } else if (rvalue is DictType) {
            bind(s, xs, rvalue.toTupleType(xs.size), kind)
        } else if (xs.size > 0) {
            for (x in xs) {
                bind(s, x, Types.UNKNOWN, kind)
            }
            addWarningToFile(xs[0].file,
                    xs[0].start,
                    xs[xs.size - 1].end,
                    "unpacking non-iterable: $rvalue")
        }
    }

    // iterator
    fun bindIter(s: State, target: Node, iter: Node, kind: Binding.Kind) {
        val iterType = visit(iter, s)

        if (iterType is ListType) {
            bind(s, target, iterType.eltType, kind)
        } else if (iterType is TupleType) {
            bind(s, target, iterType.toListType().eltType, kind)
        } else {
            val ents = iterType.table.lookupAttr("__iter__")
            if (ents != null) {
                for (ent in ents) {
                    if (ent == null || ent.type !is FunType) {
                        if (!iterType.isUnknownType) {
                            addWarningToNode(iter, "not an iterable type: $iterType")
                        }
                        bind(s, target, Types.UNKNOWN, kind)
                    } else {
                        bind(s, target, (ent.type as FunType).returnType, kind)
                    }
                }
            } else {
                bind(s, target, Types.UNKNOWN, kind)
            }
        }
    }

    fun addError(node: Node, msg: String) {
        Analyzer.self.putProblem(node, msg)
    }

    companion object {

        internal fun bindMethodAttrs(cl: FunType) {
            if (cl.table.parent != null) {
                val cls = cl.table.parent!!.type
                if (cls != null && cls is ClassType) {
                    addReadOnlyAttr(cl, "im_class", cls, CLASS)
                    addReadOnlyAttr(cl, "__class__", cls, CLASS)
                    addReadOnlyAttr(cl, "im_self", cls, ATTRIBUTE)
                    addReadOnlyAttr(cl, "__self__", cls, ATTRIBUTE)
                }
            }
        }

        internal fun addReadOnlyAttr(`fun`: FunType,
                                     name: String,
                                     type: Type,
                                     kind: Binding.Kind) {
            val loc = Builtins.newDataModelUrl("the-standard-type-hierarchy")
            val b = Binding(name, loc, type, kind)
            `fun`.table.update(name, b)
            b.markSynthetic()
            b.markStatic()
        }

        internal fun missingReturn(toType: Type): Boolean {
            var hasNone = false
            var hasOther = false

            if (toType is UnionType) {
                for (t in toType.types) {
                    if (t === Types.NoneInstance || t === Types.CONT) {
                        hasNone = true
                    } else {
                        hasOther = true
                    }
                }
            }

            return hasNone && hasOther
        }

        fun bind(s: State, name: Name, rvalue: Type, kind: Binding.Kind) {
            if (s.isGlobalName(name.id)) {
                val bs = s.lookup(name.id)
                if (bs != null) {
                    for (b in bs) {
                        b.addType(rvalue)
                        Analyzer.self.putRef(name, b)
                    }
                }
            } else {
                s.insert(name.id, name, rvalue, kind)
            }
        }

        private fun reportUnpackMismatch(xs: List<Node>, vsize: Int) {
            val xsize = xs.size
            val beg = xs[0].start
            val end = xs[xs.size - 1].end
            val diff = xsize - vsize
            val msg: String
            if (diff > 0) {
                msg = "ValueError: need more than $vsize values to unpack"
            } else {
                msg = "ValueError: too many values to unpack"
            }
            addWarningToFile(xs[0].file, beg, end, msg)
        }

        fun addWarningToNode(node: Node, msg: String) {
            Analyzer.self.putProblem(node, msg)
        }

        fun addWarningToFile(file: String, begin: Int, end: Int, msg: String) {
            Analyzer.self.putProblem(file, begin, end, msg)
        }
    }
}
