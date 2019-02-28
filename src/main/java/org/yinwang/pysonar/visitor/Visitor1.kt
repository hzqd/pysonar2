package org.yinwang.pysonar.visitor


import org.yinwang.pysonar.State
import org.yinwang.pysonar.ast.*
import org.yinwang.pysonar.ast.Node
import org.yinwang.pysonar.types.Type

import java.util.ArrayList

interface Visitor1<T, P> {

    fun visit(node: Node, param: P): T {
        when (node.nodeType) {
            NodeType.ALIAS -> return visit(node as Alias, param)
            NodeType.ASSERT -> return visit(node as Assert, param)
            NodeType.ASSIGN -> return visit(node as Assign, param)
            NodeType.ATTRIBUTE -> return visit(node as Attribute, param)
            NodeType.AWAIT -> return visit(node as Await, param)
            NodeType.BINOP -> return visit(node as BinOp, param)
            NodeType.BLOCK -> return visit(node as Block, param)
            NodeType.BREAK -> return visit(node as Break, param)
            NodeType.BYTES -> return visit(node as Bytes, param)
            NodeType.CALL -> return visit(node as Call, param)
            NodeType.CLASSDEF -> return visit(node as ClassDef, param)
            NodeType.COMPREHENSION -> return visit(node as Comprehension, param)
            NodeType.CONTINUE -> return visit(node as Continue, param)
            NodeType.DELETE -> return visit(node as Delete, param)
            NodeType.DICT -> return visit(node as Dict, param)
            NodeType.DICTCOMP -> return visit(node as DictComp, param)
            NodeType.DUMMY -> return visit(node as Dummy, param)
            NodeType.ELLIPSIS -> return visit(node as Ellipsis, param)
            NodeType.EXEC -> return visit(node as Exec, param)
            NodeType.EXPR -> return visit(node as Expr, param)
            NodeType.EXTSLICE -> return visit(node as ExtSlice, param)
            NodeType.FOR -> return visit(node as For, param)
            NodeType.FUNCTIONDEF -> return visit(node as FunctionDef, param)
            NodeType.GENERATOREXP -> return visit(node as GeneratorExp, param)
            NodeType.GLOBAL -> return visit(node as Global, param)
            NodeType.HANDLER -> return visit(node as Handler, param)
            NodeType.IF -> return visit(node as If, param)
            NodeType.IFEXP -> return visit(node as IfExp, param)
            NodeType.IMPORT -> return visit(node as Import, param)
            NodeType.IMPORTFROM -> return visit(node as ImportFrom, param)
            NodeType.INDEX -> return visit(node as Index, param)
            NodeType.KEYWORD -> return visit(node as Keyword, param)
            NodeType.LISTCOMP -> return visit(node as ListComp, param)
            NodeType.MODULE -> return visit(node as Module, param)
            NodeType.NAME -> return visit(node as Name, param)
            NodeType.NODE -> return visit(node, param)
            NodeType.PASS -> return visit(node as Pass, param)
            NodeType.PRINT -> return visit(node as Print, param)
            NodeType.PYCOMPLEX -> return visit(node as PyComplex, param)
            NodeType.PYFLOAT -> return visit(node as PyFloat, param)
            NodeType.PYINT -> return visit(node as PyInt, param)
            NodeType.PYLIST -> return visit(node as PyList, param)
            NodeType.PYSET -> return visit(node as PySet, param)
            NodeType.RAISE -> return visit(node as Raise, param)
            NodeType.REPR -> return visit(node as Repr, param)
            NodeType.RETURN -> return visit(node as Return, param)
            NodeType.SEQUENCE -> return visit(node as Sequence, param)
            NodeType.SETCOMP -> return visit(node as SetComp, param)
            NodeType.SLICE -> return visit(node as Slice, param)
            NodeType.STARRED -> return visit(node as Starred, param)
            NodeType.STR -> return visit(node as Str, param)
            NodeType.SUBSCRIPT -> return visit(node as Subscript, param)
            NodeType.TRY -> return visit(node as Try, param)
            NodeType.TUPLE -> return visit(node as Tuple, param)
            NodeType.UNARYOP -> return visit(node as UnaryOp, param)
            NodeType.UNSUPPORTED -> return visit(node as Unsupported, param)
            NodeType.URL -> return visit(node as Url, param)
            NodeType.WHILE -> return visit(node as While, param)
            NodeType.WITH -> return visit(node as With, param)
            NodeType.WITHITEM -> return visit(node as Withitem, param)
            NodeType.YIELD -> return visit(node as Yield, param)
            NodeType.YIELDFROM -> return visit(node as YieldFrom, param)

            else -> throw RuntimeException("unexpected node")
        }
    }

    fun visit(node: Sequence, param: P): T {
        when (node.nodeType) {
            NodeType.PYLIST -> return visit(node as PyList, param)
            NodeType.PYSET -> return visit(node as PySet, param)
            else //TUPLE
            -> return visit(node as Tuple, param)
        }
    }

    fun <N : Node, O : T> visit(list: List<N>, param: P): List<O> {
        val result = ArrayList<O>()
        for (elem in list) {
            result.add(visit(elem, param) as O)
        }
        return result
    }

    fun visit(node: Alias, param: P): T
    fun visit(node: Assert, param: P): T
    fun visit(node: Assign, param: P): T
    fun visit(node: Attribute, param: P): T
    fun visit(node: Await, param: P): T
    fun visit(node: BinOp, param: P): T
    fun visit(node: Block, param: P): T
    fun visit(node: Break, param: P): T
    fun visit(node: Bytes, param: P): T
    fun visit(node: Call, param: P): T
    fun visit(node: ClassDef, param: P): T
    fun visit(node: Comprehension, param: P): T
    fun visit(node: Continue, param: P): T
    fun visit(node: Delete, param: P): T
    fun visit(node: Dict, param: P): T
    fun visit(node: DictComp, param: P): T
    fun visit(node: Dummy, param: P): T
    fun visit(node: Ellipsis, param: P): T
    fun visit(node: Exec, param: P): T
    fun visit(node: Expr, param: P): T
    fun visit(node: ExtSlice, param: P): T
    fun visit(node: For, param: P): T
    fun visit(node: FunctionDef, param: P): T
    fun visit(node: GeneratorExp, param: P): T
    fun visit(node: Global, param: P): T
    fun visit(node: Handler, param: P): T
    fun visit(node: If, param: P): T
    fun visit(node: IfExp, param: P): T
    fun visit(node: Import, param: P): T
    fun visit(node: ImportFrom, param: P): T
    fun visit(node: Index, param: P): T
    fun visit(node: Keyword, param: P): T
    fun visit(node: ListComp, param: P): T
    fun visit(node: Module, param: P): T
    fun visit(node: Name, param: P): T
    fun visit(node: Pass, param: P): T
    fun visit(node: Print, param: P): T
    fun visit(node: PyComplex, param: P): T
    fun visit(node: PyFloat, param: P): T
    fun visit(node: PyInt, param: P): T
    fun visit(node: PyList, param: P): T
    fun visit(node: PySet, param: P): T
    fun visit(node: Raise, param: P): T
    fun visit(node: Repr, param: P): T
    fun visit(node: Return, param: P): T
    fun visit(node: SetComp, param: P): T
    fun visit(node: Slice, param: P): T
    fun visit(node: Starred, param: P): T
    fun visit(node: Str, param: P): T
    fun visit(node: Subscript, param: P): T
    fun visit(node: Try, param: P): T
    fun visit(node: Tuple, param: P): T
    fun visit(node: UnaryOp, param: P): T
    fun visit(node: Unsupported, param: P): T
    fun visit(node: Url, param: P): T
    fun visit(node: While, param: P): T
    fun visit(node: With, param: P): T
    fun visit(node: Withitem, param: P): T
    fun visit(node: Yield, param: P): T
    fun visit(node: YieldFrom, param: P): T
}
