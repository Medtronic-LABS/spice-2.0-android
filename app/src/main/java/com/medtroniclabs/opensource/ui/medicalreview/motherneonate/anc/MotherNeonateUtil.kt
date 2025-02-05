package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar

object MotherNeonateUtil {
    const val EstimatedDeliveryDate: Long = 280
    fun convertNullableIntToString(value: Int?, context: Context): String {
        return value?.toString() ?: context.getString(R.string.hyphen_symbol)
    }

    fun convertNullableDoubleToString(value: Double?, context: Context): String {
        return value?.toString() ?: context.getString(R.string.hyphen_symbol)
    }

    fun convertNullableStringToString(value: String?, context: Context): String {
        return value?.takeIf { it.isNotBlank() } ?: context.getString(R.string.hyphen_symbol)
    }

    fun initTextWatcherForDouble(
        view: AppCompatEditText,
        propertySetter: (Double?) -> Unit
    ) {
        view.doAfterTextChanged {
            val text = it.toString().trim()
            val value = if (text.isNotBlank()) text.toDoubleOrNull() else null
            propertySetter(value)
        }
    }

    fun initTextWatcherForInt(view: AppCompatEditText, propertySetter: (Int?) -> Unit) {
        view.doAfterTextChanged {
            val text = it.toString().trim()
            val value = if (text.isNotBlank()) text.toIntOrNull() else null
            propertySetter(value)
        }
    }

    fun initTextWatcherForString(view: AppCompatEditText, propertySetter: (String) -> Unit) {
        view.doAfterTextChanged {
            val text = it?.toString()?.trim() ?: ""
            propertySetter(text)
        }
    }

    fun isValidInput(
        inputText: String,
        editText: EditText,
        errorTextView: TextView,
        validRange: ClosedRange<Double>,
        errorMessageResId: Int,
        isMandatory: Boolean,
        context: Context
    ): Boolean {
        val input = inputText.toDoubleOrNull()
        if (editText.text.isNullOrBlank()) {
            if (isMandatory) {
                errorTextView.visible()
                errorTextView.text = context.getText(R.string.error_label)
                return false
            }
            errorTextView.gone()
            return true
        }
        if (!(input != null && input in validRange)) {
            errorTextView.visible()
            errorTextView.text = context.getString(errorMessageResId)
            return false
        }
        errorTextView.gone()
        return true
    }

    fun isValidMeasurement(
        valueText: String?,
        errorTextView: TextView,
        minValue: Int,
        maxValue: Int,
        text: AppCompatEditText? = null,
        minErrorMessage: String,
        maxErrorMessage: String,
        context: Context
    ): Boolean {
        val value = valueText?.toIntOrNull()
        val diastolic = text?.text.toString().toIntOrNull()
        if (value == null) {
            // Invalid input, display error message
            errorTextView.gone()
            return true
        }
        if (value < minValue) {
            // Value is less than minimum allowed value, display error message
            errorTextView.text = minErrorMessage
            errorTextView.visible()
            return false
        }
        if (value > maxValue) {
            errorTextView.text = maxErrorMessage
            errorTextView.visible()
            return false
        }

        if (diastolic != null && value < diastolic) {
            errorTextView.text = context.getText(R.string.systolic_diastolic_error)
            errorTextView.visible()
            return false
        }
        // Valid input
        errorTextView.gone()
        return true
    }

    fun isBasicValid(
        valueText: String?,
        errorTextView: TextView,
        minValue: Int,
        errorMessage: String,
        maxValue: Int? = null,
        maxValueError: String? = null,
        context: Context,
        isMandatory: Boolean = false,
        isParity: Boolean = false
    ): Boolean {
        val value = valueText?.toIntOrNull()
        if (value == null) {
            if (isMandatory) {
                errorTextView.text = errorMessage
                errorTextView.visible()
                return false
            }
            // Invalid input, display error message
            errorTextView.gone()
            return true
        }


        if (!isParity && value == minValue) {
            // Value is less than minimum allowed value, display error message
            errorTextView.text = errorMessage
            errorTextView.visible()
            return false
        }

        if (maxValue != null && value > maxValue) {
            errorTextView.text = maxValueError ?: context.getString(R.string.error)
            errorTextView.visible()
            return false
        }
        // Valid input
        errorTextView.gone()
        return true
    }

    fun isDataValid(
        value: Double?,
        view: TextView,
        maxValue: Int,
        editText: AppCompatEditText,
        context: Context
    ): Boolean {
        if (value == null) {
            view.gone()
            return true
        }
        if (!(value >= 1 && value <= maxValue) ) {
            view.visible()
            view.text = context.getString(R.string.value_range_error, maxValue)
            editText.requestFocus()
            return false
        }
        view.gone()
        return true
    }

    fun calculateBp(valueOne: Double?, valueTwo: Double?, context: Context): String {
        return if (valueOne != null && valueTwo != null) {
            "${valueOne.toInt()}/${valueTwo.toInt()} ${context.getString(R.string.mmHg)}"
        } else {
            "${context.getString(R.string.hyphen_symbol)}/${context.getString(R.string.hyphen_symbol)}"
        }
    }

    fun convertWeight(value: Double?, context: Context): String {
        return if (value != null) {
            val formattedValue = if (value % 1 == 0.0) {
                value.toInt().toString()
            } else {
                String.format("%.2f", value).trimEnd('0').trimEnd('.')
            }
            "$formattedValue ${context.getString(R.string.kg)}"
        } else {
            context.getString(R.string.hyphen_symbol)
        }
    }
    fun convertCMS(value: Double?, context: Context): String {
        return if (value != null) {
            "${value.toInt()} ${context.getString(R.string.cms)}"
        } else {
            context.getString(R.string.hyphen_symbol)
        }
    }

    fun convertBeatsPerMinute(value: Double?, context: Context): String {
        return if (value != null) {
            "${value.toInt()} ${context.getString(R.string.beats_per_minute)}"
        } else {
            context.getString(R.string.hyphen_symbol)
        }
    }

    fun Boolean?.toYesNoOrDefault(default: String,trueString: String, falseString: String): String {
        return when (this) {
            true -> trueString.capitalizeFirstChar()
            false -> falseString.capitalizeFirstChar()
            else -> default
        }
    }
}