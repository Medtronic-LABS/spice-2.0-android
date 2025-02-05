package com.medtroniclabs.opensource.appextensions

fun String?.textOrHyphen(): String {
    return if (this.isNullOrBlank()) "-" else this.trim()
}

fun String?.textOrEmpty(): String {
    return if (this.isNullOrBlank()) "" else this.trim()
}

fun Int?.numberOrZero(): Int {
    return if (this == null || this < 0) 0 else this
}

fun Double?.removeTrailingPointZero(): String {
    return if (this == null) "" else {
        if (this % 1.0 == 0.0) {
            this.toInt().toString()
        } else {
            this.toString()
        }
    }
}
