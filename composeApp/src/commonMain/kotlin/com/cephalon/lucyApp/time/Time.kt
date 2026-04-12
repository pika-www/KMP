package com.cephalon.lucyApp.time

expect fun currentTimeMillis(): Long

/**
 * 返回 Asia/Shanghai (UTC+8) 时区的当前日期字符串，格式 yyyy-MM-dd
 */
fun todayDateString(): String {
    val utcPlusEightMs = currentTimeMillis() + 8 * 3600 * 1000L
    val daysSinceEpoch = utcPlusEightMs / (24 * 3600 * 1000L)
    // 从 1970-01-01 推算年月日
    var remaining = daysSinceEpoch
    var year = 1970
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366L else 365L
        if (remaining < daysInYear) break
        remaining -= daysInYear
        year++
    }
    val monthDays = if (isLeapYear(year))
        intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    else
        intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (d in monthDays) {
        if (remaining < d) break
        remaining -= d
        month++
    }
    val day = remaining.toInt() + 1
    return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun isLeapYear(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)
