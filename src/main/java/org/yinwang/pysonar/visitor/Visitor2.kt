package org.yinwang.pysonar.visitor


import org.yinwang.pysonar.ast.*

import java.util.ArrayList

interface Visitor2<T, P, Q> {

    fun visit(node: Node, param1: P, param2: Q): T {
        when (node.nodeType) {
            NodeType.ALIAS -> return visit(node as Alias, param1, param2)
            NodeType.ASSERT -> return visit(node as Assert, param1, param2)
            NodeType.ASSIGN -> return visit(node as Assign, param1, param2)
            NodeType.ATTRIBUTE -> return visit(node as Attribute, param1, param2)
            NodeType.AWAIT -> return visit(node as Await, param1, param2)
            NodeType.BINOP -> return visit(node as BinOp, param1, param2)
            NodeType.BLOCK -> return visit(node as Block, param1, param2)
            NodeType.BREAK -> return visit(node as Break, param1, param2)
            NodeType.BYTES -> return visit(node as Bytes, param1, param2)
            NodeType.CALL -> return visit(node as Call, param1, param2)
            NodeType.CLASSDEF -> return visit(node as ClassDef, param1, param2)
            NodeType.COMPREHENSION -> return visit(node as Comprehension, param1, param2)
            NodeType.CONTINUE -> return visit(node as Continue, param1, param2)
            NodeType.DELETE -> return visit(node as Delete, param1, param2)
            NodeType.DICT -> return visit(node as Dict, param1, param2)
            NodeType.DICTCOMP -> return visit(node as DictComp, param1, param2)
            NodeType.DUMMY -> return visit(node as Dummy, param1, param2)
            NodeType.ELLIPSIS -> return visit(node as Ellipsis, param1, param2)
            NodeType.EXEC -> return visit(node as Exec, param1, param2)
            NodeType.EXPR -> return visit(node as Expr, param1, param2)
            NodeType.EXTSLICE -> return visit(node as ExtSlice, param1, param2)
            NodeType.FOR -> return visit(node as For, param1, param2)
            NodeType.FUNCTIONDEF -> return visit(node as FunctionDef, param1, param2)
            NodeType.GENERATOREXP -> return visit(node as GeneratorExp, param1, param2)
            NodeType.GLOBAL -> return visit(node as Global, param1, param2)
            NodeType.HANDLER -> return visit(node as Handler, param1, param2)
            NodeType.IF -> return visit(node as If, param1, param2)
            NodeType.IFEXP -> return visit(node as IfExp, param1, param2)
            NodeType.IMPORT -> return visit(node as Import, param1, param2)
            NodeType.IMPORTFROM -> return visit(node as ImportFrom, param1, param2)
            NodeType.INDEX -> return visit(node as Index, param1, param2)
            NodeType.KEYWORD -> return visit(node as Keyword, param1, param2)
            NodeType.LISTCOMP -> return visit(node as ListComp, param1, param2)
            NodeType.MODULE -> return visit(node as PyModule, param1, param2)
            NodeType.NAME -> return visit(node as Name, param1, param2)
            NodeType.PASS -> return visit(node as Pass, param1, param2)
            NodeType.PRINT -> return visit(node as Print, param1, param2)
            NodeType.PYCOMPLEX -> return visit(node as PyComplex, param1, param2)
            NodeType.PYFLOAT -> return visit(node as PyFloat, param1, param2)
            NodeType.PYINT -> return visit(node as PyInt, param1, param2)
            NodeType.PYLIST -> return visit(node as PyList, param1, param2)
            NodeType.PYSET -> return visit(node as PySet, param1, param2)
            NodeType.RAISE -> return visit(node as Raise, param1, param2)
            NodeType.REPR -> return visit(node as Repr, param1, param2)
            NodeType.RETURN -> return visit(node as Return, param1, param2)
            NodeType.SEQUENCE -> return visit(node as Sequence, param1, param2)
            NodeType.SETCOMP -> return visit(node as SetComp, param1, param2)
            NodeType.SLICE -> return visit(node as Slice, param1, param2)
            NodeType.STARRED -> return visit(node as Starred, param1, param2)
            NodeType.STR -> return visit(node as Str, param1, param2)
            NodeType.SUBSCRIPT -> return visit(node as Subscript, param1, param2)
            NodeType.TRY -> return visit(node as Try, param1, param2)
            NodeType.TUPLE -> return visit(node as Tuple, param1, param2)
            NodeType.UNARYOP -> return visit(node as UnaryOp, param1, param2)
            NodeType.UNSUPPORTED -> return visit(node as Unsupported, param1, param2)
            NodeType.URL -> return visit(node as Url, param1, param2)
            NodeType.WHILE -> return visit(node as While, param1, param2)
            NodeType.WITH -> return visit(node as With, param1, param2)
            NodeType.WITHITEM -> return visit(node as Withitem, param1, param2)
            NodeType.YIELD -> return visit(node as Yield, param1, param2)
            NodeType.YIELDFROM -> return visit(node as YieldFrom, param1, param2)

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

    fun <N : Node, O : T> visit(list: List<N>, param1: P, param2: Q): List<O> {
        val result = ArrayList<O>()
        for (elem in list) {
            result.add(visit(elem, param1, param2) as O)
        }
        return result
    }

    fun visit(node: Alias, param1: P, param2: Q): T
    fun visit(node: Assert, param1: P, param2: Q): T
    fun visit(node: Assign, param1: P, param2: Q): T
    fun visit(node: Attribute, param1: P, param2: Q): T
    fun visit(node: Await, param1: P, param2: Q): T
    fun visit(node: BinOp, param1: P, param2: Q): T
    fun visit(node: Block, param1: P, param2: Q): T
    fun visit(node: Break, param1: P, param2: Q): T
    fun visit(node: Bytes, param1: P, param2: Q): T
    fun visit(node: Call, param1: P, param2: Q): T
    fun visit(node: ClassDef, param1: P, param2: Q): T
    fun visit(node: Comprehension, param1: P, param2: Q): T
    fun visit(node: Continue, param1: P, param2: Q): T
    fun visit(node: Delete, param1: P, param2: Q): T
    fun visit(node: Dict, param1: P, param2: Q): T
    fun visit(node: DictComp, param1: P, param2: Q): T
    fun visit(node: Dummy, param1: P, param2: Q): T
    fun visit(node: Ellipsis, param1: P, param2: Q): T
    fun visit(node: Exec, param1: P, param2: Q): T
    fun visit(node: Expr, param1: P, param2: Q): T
    fun visit(node: ExtSlice, param1: P, param2: Q): T
    fun visit(node: For, param1: P, param2: Q): T
    fun visit(node: FunctionDef, param1: P, param2: Q): T
    fun visit(node: GeneratorExp, param1: P, param2: Q): T
    fun visit(node: Global, param1: P, param2: Q): T
    fun visit(node: Handler, param1: P, param2: Q): T
    fun visit(node: If, param1: P, param2: Q): T
    fun visit(node: IfExp, param1: P, param2: Q): T
    fun visit(node: Import, param1: P, param2: Q): T
    fun visit(node: ImportFrom, param1: P, param2: Q): T
    fun visit(node: Index, param1: P, param2: Q): T
    fun visit(node: Keyword, param1: P, param2: Q): T
    fun visit(node: ListComp, param1: P, param2: Q): T
    fun visit(node: PyModule, param1: P, param2: Q): T
    fun visit(node: Name, param1: P, param2: Q): T
    fun visit(node: Pass, param1: P, param2: Q): T
    fun visit(node: Print, param1: P, param2: Q): T
    fun visit(node: PyComplex, param1: P, param2: Q): T
    fun visit(node: PyFloat, param1: P, param2: Q): T
    fun visit(node: PyInt, param1: P, param2: Q): T
    fun visit(node: PyList, param1: P, param2: Q): T
    fun visit(node: PySet, param1: P, param2: Q): T
    fun visit(node: Raise, param1: P, param2: Q): T
    fun visit(node: Repr, param1: P, param2: Q): T
    fun visit(node: Return, param1: P, param2: Q): T
    fun visit(node: SetComp, param1: P, param2: Q): T
    fun visit(node: Slice, param1: P, param2: Q): T
    fun visit(node: Starred, param1: P, param2: Q): T
    fun visit(node: Str, param1: P, param2: Q): T
    fun visit(node: Subscript, param1: P, param2: Q): T
    fun visit(node: Try, param1: P, param2: Q): T
    fun visit(node: Tuple, param1: P, param2: Q): T
    fun visit(node: UnaryOp, param1: P, param2: Q): T
    fun visit(node: Unsupported, param1: P, param2: Q): T
    fun visit(node: Url, param1: P, param2: Q): T
    fun visit(node: While, param1: P, param2: Q): T
    fun visit(node: With, param1: P, param2: Q): T
    fun visit(node: Withitem, param1: P, param2: Q): T
    fun visit(node: Yield, param1: P, param2: Q): T
    fun visit(node: YieldFrom, param1: P, param2: Q): T
}
