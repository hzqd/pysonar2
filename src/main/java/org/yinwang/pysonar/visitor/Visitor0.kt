package org.yinwang.pysonar.visitor


import org.yinwang.pysonar.ast.*

import java.util.ArrayList

interface Visitor0<T> {

    fun visit(node: Node): T {
        when (node.nodeType) {
            NodeType.ALIAS -> return visit(node as Alias)
            NodeType.ASSERT -> return visit(node as Assert)
            NodeType.ASSIGN -> return visit(node as Assign)
            NodeType.ATTRIBUTE -> return visit(node as Attribute)
            NodeType.AWAIT -> return visit(node as Await)
            NodeType.BINOP -> return visit(node as BinOp)
            NodeType.BLOCK -> return visit(node as Block)
            NodeType.BREAK -> return visit(node as Break)
            NodeType.BYTES -> return visit(node as Bytes)
            NodeType.CALL -> return visit(node as Call)
            NodeType.CLASSDEF -> return visit(node as ClassDef)
            NodeType.COMPREHENSION -> return visit(node as Comprehension)
            NodeType.CONTINUE -> return visit(node as Continue)
            NodeType.DELETE -> return visit(node as Delete)
            NodeType.DICT -> return visit(node as Dict)
            NodeType.DICTCOMP -> return visit(node as DictComp)
            NodeType.DUMMY -> return visit(node as Dummy)
            NodeType.ELLIPSIS -> return visit(node as Ellipsis)
            NodeType.EXEC -> return visit(node as Exec)
            NodeType.EXPR -> return visit(node as Expr)
            NodeType.EXTSLICE -> return visit(node as ExtSlice)
            NodeType.FOR -> return visit(node as For)
            NodeType.FUNCTIONDEF -> return visit(node as FunctionDef)
            NodeType.GENERATOREXP -> return visit(node as GeneratorExp)
            NodeType.GLOBAL -> return visit(node as Global)
            NodeType.HANDLER -> return visit(node as Handler)
            NodeType.IF -> return visit(node as If)
            NodeType.IFEXP -> return visit(node as IfExp)
            NodeType.IMPORT -> return visit(node as Import)
            NodeType.IMPORTFROM -> return visit(node as ImportFrom)
            NodeType.INDEX -> return visit(node as Index)
            NodeType.KEYWORD -> return visit(node as Keyword)
            NodeType.LISTCOMP -> return visit(node as ListComp)
            NodeType.MODULE -> return visit(node as Module)
            NodeType.NAME -> return visit(node as Name)
            NodeType.NODE -> return visit(node)
            NodeType.PASS -> return visit(node as Pass)
            NodeType.PRINT -> return visit(node as Print)
            NodeType.PYCOMPLEX -> return visit(node as PyComplex)
            NodeType.PYFLOAT -> return visit(node as PyFloat)
            NodeType.PYINT -> return visit(node as PyInt)
            NodeType.PYLIST -> return visit(node as PyList)
            NodeType.PYSET -> return visit(node as PySet)
            NodeType.RAISE -> return visit(node as Raise)
            NodeType.REPR -> return visit(node as Repr)
            NodeType.RETURN -> return visit(node as Return)
            NodeType.SEQUENCE -> return visit(node as Sequence)
            NodeType.SETCOMP -> return visit(node as SetComp)
            NodeType.SLICE -> return visit(node as Slice)
            NodeType.STARRED -> return visit(node as Starred)
            NodeType.STR -> return visit(node as Str)
            NodeType.SUBSCRIPT -> return visit(node as Subscript)
            NodeType.TRY -> return visit(node as Try)
            NodeType.TUPLE -> return visit(node as Tuple)
            NodeType.UNARYOP -> return visit(node as UnaryOp)
            NodeType.UNSUPPORTED -> return visit(node as Unsupported)
            NodeType.URL -> return visit(node as Url)
            NodeType.WHILE -> return visit(node as While)
            NodeType.WITH -> return visit(node as With)
            NodeType.WITHITEM -> return visit(node as Withitem)
            NodeType.YIELD -> return visit(node as Yield)
            NodeType.YIELDFROM -> return visit(node as YieldFrom)

            else -> throw RuntimeException("unexpected node")
        }
    }

    fun visit(node: Sequence): T {
        when (node.nodeType) {
            NodeType.PYLIST -> return visit(node as PyList)
            NodeType.PYSET -> return visit(node as PySet)
            else //TUPLE
            -> return visit(node as Tuple)
        }
    }

    fun <N : Node, O : T> visit(list: List<N>): List<O> {
        val result = ArrayList<O>()
        for (elem in list) {
            result.add(visit(elem) as O)
        }
        return result
    }

    fun visit(node: Alias): T
    fun visit(node: Assert): T
    fun visit(node: Assign): T
    fun visit(node: Attribute): T
    fun visit(node: Await): T
    fun visit(node: BinOp): T
    fun visit(node: Block): T
    fun visit(node: Break): T
    fun visit(node: Bytes): T
    fun visit(node: Call): T
    fun visit(node: ClassDef): T
    fun visit(node: Comprehension): T
    fun visit(node: Continue): T
    fun visit(node: Delete): T
    fun visit(node: Dict): T
    fun visit(node: DictComp): T
    fun visit(node: Dummy): T
    fun visit(node: Ellipsis): T
    fun visit(node: Exec): T
    fun visit(node: Expr): T
    fun visit(node: ExtSlice): T
    fun visit(node: For): T
    fun visit(node: FunctionDef): T
    fun visit(node: GeneratorExp): T
    fun visit(node: Global): T
    fun visit(node: Handler): T
    fun visit(node: If): T
    fun visit(node: IfExp): T
    fun visit(node: Import): T
    fun visit(node: ImportFrom): T
    fun visit(node: Index): T
    fun visit(node: Keyword): T
    fun visit(node: ListComp): T
    fun visit(node: Module): T
    fun visit(node: Name): T
    fun visit(node: Pass): T
    fun visit(node: Print): T
    fun visit(node: PyComplex): T
    fun visit(node: PyFloat): T
    fun visit(node: PyInt): T
    fun visit(node: PyList): T
    fun visit(node: PySet): T
    fun visit(node: Raise): T
    fun visit(node: Repr): T
    fun visit(node: Return): T
    fun visit(node: SetComp): T
    fun visit(node: Slice): T
    fun visit(node: Starred): T
    fun visit(node: Str): T
    fun visit(node: Subscript): T
    fun visit(node: Try): T
    fun visit(node: Tuple): T
    fun visit(node: UnaryOp): T
    fun visit(node: Unsupported): T
    fun visit(node: Url): T
    fun visit(node: While): T
    fun visit(node: With): T
    fun visit(node: Withitem): T
    fun visit(node: Yield): T
    fun visit(node: YieldFrom): T
}
