package org.yinwang.pysonar

import org.yinwang.pysonar.types.FunType
import org.yinwang.pysonar.types.Type

class CallStackEntry(var `fun`: FunType, var from: Type)
