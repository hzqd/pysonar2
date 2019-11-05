package org.yinwang.pysonar

import org.junit.Test

import org.junit.Assert.fail

class TestRefs {
    @Test
    fun testRefs() {
        val failed = TestInference.testAll("tests", false)
        if (failed != null) {
            var msg = "Some tests failed. "
            msg += "\n----------------------------- FAILED TESTS ---------------------------"
            for (fail in failed) {
                msg += "\n - $fail"
            }
            msg += "\n----------------------------------------------------------------------"
            fail(msg)
        }
    }
}
