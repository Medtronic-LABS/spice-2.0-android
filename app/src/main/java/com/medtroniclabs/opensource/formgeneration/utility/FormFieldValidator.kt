package com.medtroniclabs.opensource.formgeneration.utility

import android.util.Patterns
import java.util.regex.Pattern

object FormFieldValidator {

    private const val PHONE_NUMBER_REGEX = "([0-9])\\1{4}"


    fun isValidMobileNumber(mobileNumber: String): Boolean {
        return Patterns.PHONE.matcher(mobileNumber).matches() && validatePhoneNumber(mobileNumber)
    }

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        val pattern = Pattern.compile(PHONE_NUMBER_REGEX)
        val matcher = pattern.matcher(phoneNumber)
        if (matcher.find()) {
            return false
        }
        return true
    }

}