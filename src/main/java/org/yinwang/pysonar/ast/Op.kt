package org.yinwang.pysonar.ast

import org.yinwang.pysonar.`$`

enum class Op {
    // numeral
    Add("+", "__add__"),
    Sub("-", "__sub__"),
    Mul("*", "__mul__"),
    MatMult("@", "__matmult__"),
    Div("/", "__div__"),
    Mod("%", "__mod__"),
    Pow("**", "__pow__"),
    FloorDiv("//", "__floordiv__"),

    // comparison
    Eq("is"),
    Equal("==", "__eq__"),
    Lt("<", "__lt__"),
    Gt(">", "__gt__"),

    // bit
    BitAnd("&", "__and__"),
    BitOr("|", "__or__"),
    BitXor("^", "__xor__"),
    In("in"),
    LShift("<<", "__lshift__"),
    RShift(">>", "__rshift__"),
    Invert("~", "__invert__"),

    // boolean
    And("and"),
    Or("or"),
    Not("not"),

    // synthetic
    NotEqual("!=", "__neq__"),
    NotEq("is not"),
    LtE("<=", "__lte__"),
    GtE(">=", "__gte__"),
    NotIn("not in"),

    // unsupported new operator
    Unsupported("??");

    var rep: String? = null
        private set

    var method: String? = null
        private set

    private constructor(rep: String, method: String?) {
        this.rep = rep
        this.method = method
    }

    private constructor(rep: String) {
        this.rep = rep
        this.method = null
    }

    companion object {

        fun invert(op: Op): Op? {
            if (op == Op.Lt) {
                return Op.Gt
            }

            if (op == Op.Gt) {
                return Op.Lt
            }

            if (op == Op.Eq) {
                return Op.Eq
            }

            if (op == Op.And) {
                return Op.Or
            }

            if (op == Op.Or) {
                return Op.And
            }

            `$`.die("invalid operator name for invert: $op")
            return null  // unreacheable
        }

        fun isBoolean(op: Op): Boolean {
            return op == Eq ||
                    op == Equal ||
                    op == Lt ||
                    op == Gt ||
                    op == NotEqual ||
                    op == NotEq ||
                    op == LtE ||
                    op == GtE ||
                    op == In ||
                    op == NotIn
        }
    }
}
