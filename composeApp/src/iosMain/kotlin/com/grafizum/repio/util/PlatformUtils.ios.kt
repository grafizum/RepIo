package com.grafizum.repio.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSCalendarUnitWeekday

actual fun openUrl(url: String) {
    // To be implemented using platform.Foundation / platform.UIKit
}

actual fun applyAppIconChange(themeKey: String) {
    // iOS icon switching not implemented here yet
}

actual fun getTodayDayOfWeek(): String {
    val calendar = NSCalendar.currentCalendar
    val weekday = calendar.component(NSCalendarUnitWeekday, fromDate = NSDate())
    return when (weekday.toInt()) {
        2 -> "MONDAY"
        3 -> "TUESDAY"
        4 -> "WEDNESDAY"
        5 -> "THURSDAY"
        6 -> "FRIDAY"
        7 -> "SATURDAY"
        1 -> "SUNDAY"
        else -> "MONDAY"
    }
}
