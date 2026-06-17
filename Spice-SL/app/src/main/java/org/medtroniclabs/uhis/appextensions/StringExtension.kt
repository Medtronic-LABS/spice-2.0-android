package org.medtroniclabs.uhis.appextensions

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun String?.textOrHyphen(): String = if (this.isNullOrBlank()) "-" else this.trim()

fun String?.textOrDoubleHyphen(): String = if (this.isNullOrBlank()) "--" else this.trim()

fun String?.textOrEmpty(): String = if (this.isNullOrBlank()) "" else this.trim()

fun Int?.numberOrZero(): Int = if (this == null || this < 0) 0 else this

fun Double?.removeTrailingPointZero(): String =
    if (this == null) {
        ""
    } else {
        if (this % 1.0 == 0.0) {
            this.toInt().toString()
        } else {
            this.toString()
        }
    }

fun String.getLocalDate(): LocalDate {
    val offsetDateTime = OffsetDateTime.parse(this)
    return offsetDateTime.toLocalDate()
}

fun String.getLongDate(format: String): Long {
    val formatter = DateTimeFormatter.ofPattern(format)
    val localDate = LocalDate.parse(this, formatter)
    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
