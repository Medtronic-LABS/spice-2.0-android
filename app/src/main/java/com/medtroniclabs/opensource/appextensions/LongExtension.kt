package com.medtroniclabs.opensource.appextensions

import com.medtroniclabs.opensource.common.DateUtils.DATE_TIME_DISPLAY_FORMAT
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


fun Long.convertToUtcDateTime(): String {
    val instant = Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC)
    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    return formatter.format(instant)
}

fun Long.convertToLocalDateTime(format: String = DATE_TIME_DISPLAY_FORMAT): String {
    val date = Date(this)
    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
    return dateFormat.format(date)
}

fun LocalDate.getLongTime(): Long {
    val localDateTime = this.atStartOfDay()
    return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun LocalDate.toString(format: String): String {
    val formatter = DateTimeFormatter.ofPattern(format)
    return this.format(formatter)
}

fun LocalDate.getFirstAndLastDateOfMonth(): Pair<LocalDate, LocalDate> {
    val localDate = LocalDate.now()
    val firstDateOfMonth = localDate.withDayOfMonth(1)
    val lastDateOfMonth = localDate.withDayOfMonth(localDate.lengthOfMonth())
    return Pair(firstDateOfMonth, lastDateOfMonth)
}