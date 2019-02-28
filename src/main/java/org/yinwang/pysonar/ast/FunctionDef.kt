package org.yinwang.pysonar.ast

class FunctionDef(name: Name?, var args: List<Node>, var body: Node, var defaults: List<Node>,
                  var vararg: Name?  // *args
                  , var kwarg: Name?   // **kwarg
                  , private val decorators: List<Node>, file: String, isAsync: Boolean, start: Int, end: Int, line: Int, col: Int) : Node(NodeType.FUNCTIONDEF, file, start, end, line, col) {

    var name: Name
    var afterRest: List<Node>? = null   // after rest arg of Ruby
    var called = false
    var isLamba = false
    var isAsync = false

    val argumentExpr: String
        get() {
            val argExpr = StringBuilder()
            argExpr.append("(")
            var first = true

            for (n in args) {
                if (!first) {
                    argExpr.append(", ")
                }
                first = false
                argExpr.append(n.toDisplay())
            }

            if (vararg != null) {
                if (!first) {
                    argExpr.append(", ")
                }
                first = false
                argExpr.append("*" + vararg!!.toDisplay())
            }

            if (kwarg != null) {
                if (!first) {
                    argExpr.append(", ")
                }
                argExpr.append("**" + kwarg!!.toDisplay())
            }

            argExpr.append(")")
            return argExpr.toString()
        }

    val isStaticMethod: Boolean
        get() {
            for (d in decorators) {
                if (d is Name && d.id == "staticmethod") {
                    return true
                }
            }
            return false
        }

    val isClassMethod: Boolean
        get() {
            for (d in decorators) {
                if (d is Name && d.id == "classmethod") {
                    return true
                }
            }
            return false
        }

    init {
        if (name != null) {
            this.name = name
        } else {
            isLamba = true
            val fn = genLambdaName()
            this.name = Name(fn, file, start, start + "lambda".length, line, col + "lambda".length)
            addChildren(this.name)
        }
        this.isAsync = isAsync
        addChildren(name)
        addChildren(args)
        addChildren(defaults)
        addChildren(vararg, kwarg, this.body)
    }

    override fun toString(): String {
        return "(func:$start:$name)"
    }

    companion object {


        private var lambdaCounter = 0

        fun genLambdaName(): String {
            lambdaCounter = lambdaCounter + 1
            return "lambda%$lambdaCounter"
        }
    }

}
