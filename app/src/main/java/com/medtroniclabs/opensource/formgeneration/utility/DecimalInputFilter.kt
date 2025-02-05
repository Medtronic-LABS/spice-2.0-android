package com.medtroniclabs.opensource.formgeneration.utility

import android.text.InputFilter
import android.text.Spanned

class DecimalInputFilter : InputFilter {
    private val pattern = Regex("^\\d{0,2}(\\.\\d{0,2})?$")

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val newInput = dest.toString().substring(0, dstart) +
                source.subSequence(start, end) +
                dest.toString().substring(dend)

        return if (pattern.matches(newInput)) {
            null // Accept the input
        } else {
            "" // Reject the input
        }
    }
}