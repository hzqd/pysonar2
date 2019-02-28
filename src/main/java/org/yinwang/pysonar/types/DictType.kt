package org.yinwang.pysonar.types

class DictType(var keyType: Type, var valueType: Type) : Type() {

    init {
        table.addSuper(Types.BaseDict.table)
        table.setPath(Types.BaseDict.table.path)
    }

    fun add(key: Type, `val`: Type) {
        keyType = UnionType.union(keyType, key)
        valueType = UnionType.union(valueType, `val`)
    }

    fun toTupleType(n: Int): TupleType {
        val ret = TupleType()
        for (i in 0 until n) {
            ret.add(keyType)
        }
        return ret
    }

    override fun typeEquals(other: Any?): Boolean {
        if (Type.typeStack.contains(this, other)) {
            return true
        } else if (other is DictType) {
            Type.typeStack.push(this, other)
            val co = other as DictType?
            val result = co!!.keyType.typeEquals(keyType) && co.valueType.typeEquals(valueType)
            Type.typeStack.pop(this, other)
            return result
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        return "DictType".hashCode()
    }

    fun setKeyType(keyType: Type) {
        this.keyType = keyType
    }

    fun setValueType(valueType: Type) {
        this.valueType = valueType
    }

    override fun printType(ctr: Type.CyclicTypeRecorder): String {
        //        StringBuilder sb = new StringBuilder();
        //
        //        Integer num = ctr.visit(this);
        //        if (num != null) {
        //            sb.append("#").append(num);
        //        } else {
        //            ctr.push(this);
        //            sb.append("{");
        //            sb.append(keyType.printType(ctr));
        //            sb.append(" : ");
        //            sb.append(valueType.printType(ctr));
        //            sb.append("}");
        //            ctr.pop(this);
        //        }
        //
        //        return sb.toString();
        return "dict"
    }

}
