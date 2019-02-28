package org.yinwang.pysonar.types

object Types {
    var ObjectClass = ClassType("object", null, null)
    var ObjectInstance: Type = ObjectClass.instance

    var TypeClass = ClassType("type", null, null)
    var TypeInstance: Type = TypeClass.instance

    var BoolClass = ClassType("bool", null, ObjectClass)
    var BoolInstance: Type = BoolClass.instance

    var IntClass = ClassType("int", null, ObjectClass)
    var IntInstance: Type = IntClass.instance

    var LongClass = ClassType("long", null, ObjectClass)
    var LongInstance: Type = LongClass.instance

    var StrClass = ClassType("str", null, ObjectClass)
    var StrInstance: Type = StrClass.instance

    var FloatClass = ClassType("float", null, ObjectClass)
    var FloatInstance: Type = FloatClass.instance

    var ComplexClass = ClassType("complex", null, ObjectClass)
    var ComplexInstance: Type = ComplexClass.instance

    var NoneClass = ClassType("None", null, ObjectClass)
    var NoneInstance: Type = NoneClass.instance

    // Synthetic types used only for inference purposes
    // They don't exist in Python
    var UNKNOWN: Type = InstanceType(ClassType("?", null, ObjectClass))
    var CONT: Type = InstanceType(ClassType("None", null, null))

    var BaseDict = ClassType("dict", null, ObjectClass)
}
