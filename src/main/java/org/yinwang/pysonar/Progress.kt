package org.yinwang.pysonar

class Progress(internal var total: Long, internal var width: Long) {

    internal var startTime: Long = 0
    internal var lastTickTime: Long = 0
    internal var lastCount: Long = 0
    internal var lastRate: Int = 0
    internal var lastAvgRate: Int = 0
    internal var count: Long = 0
    internal var segSize: Long = 0


    init {
        this.startTime = System.currentTimeMillis()
        this.lastTickTime = System.currentTimeMillis()
        this.lastCount = 0
        this.lastRate = 0
        this.lastAvgRate = 0
        this.segSize = total / width
        if (segSize == 0L) {
            segSize = 1
        }
    }


    fun tick(n: Int) {
        count += n.toLong()
        if (count > total) {
            total = count
        }

        val elapsed = System.currentTimeMillis() - lastTickTime

        if (elapsed > 500 || count == total) {
            `$`.msg_("\r")
            val dlen = Math.ceil(Math.log10(total.toDouble())).toInt()
            `$`.msg_(`$`.percent(count, total) + " (" +
                    `$`.formatNumber(count, dlen) +
                    " of " + `$`.formatNumber(total, dlen) + ")")

            val rate: Int
            if (elapsed > 1) {
                rate = ((count - lastCount) / (elapsed / 1000.0)).toInt()
            } else {
                rate = lastRate
            }

            lastRate = rate
            `$`.msg_("   SPEED: " + `$`.formatNumber(rate, MAX_SPEED_DIGITS) + "/s")

            val totalElapsed = System.currentTimeMillis() - startTime
            var avgRate: Int

            if (totalElapsed > 1) {
                avgRate = (count / (totalElapsed / 1000.0)).toInt()
            } else {
                avgRate = lastAvgRate
            }
            avgRate = if (avgRate == 0) 1 else avgRate

            `$`.msg_("   AVG SPEED: " + `$`.formatNumber(avgRate, MAX_SPEED_DIGITS) + "/s")

            val remain = total - count
            val remainTime = remain / avgRate * 1000
            `$`.msg_("   ETA: " + `$`.formatTime(remainTime))

            `$`.msg_("   PARSE ERRS: " + Analyzer.self.failedToParse.size)

            `$`.msg_("       ")      // overflow area

            lastTickTime = System.currentTimeMillis()
            lastAvgRate = avgRate
            lastCount = count
        }
    }


    fun tick() {
        if (!Analyzer.self.hasOption("quiet")) {
            tick(1)
        }
    }

    companion object {

        private val MAX_SPEED_DIGITS = 5
    }
}
