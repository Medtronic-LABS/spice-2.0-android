package com.medtroniclabs.opensource.common

import android.content.Context
import android.util.Patterns
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.formgeneration.model.BPModel
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.mappingkey.Screening
import java.util.regex.Pattern

object Validator {

    private const val PHONE_NUMBER_REGEX = "([0-9])\\1{4}"

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

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

    data class ValidationUIModel(val status: Boolean, val message: String? = null)

    fun checkValidBPInput(
        context: Context,
        list: ArrayList<BPModel>,
        model: FormLayout? = null
    ): ValidationUIModel {
        var validEntries = 0
        var validationModel = ValidationUIModel(true)

        val minValue = model?.minValue ?: Screening.BPAverageMinimumValue
        val maxValue = model?.maxValue ?: Screening.BPAverageMaximumValue
        val pulseMinValue =
            model?.pulseMinValue ?: Screening.PulseMinimumValue
        val pulseMaxValue =
            model?.pulseMaxValue ?: Screening.PulseMaximumValue
        val mandatoryCount = model?.mandatoryCount ?: model?.totalCount ?: 0

        list.forEach { bp ->

            if ((bp.systolic == null && bp.diastolic == null)) {
                if (bp.pulse != null && validationModel.status)
                    validationModel = ValidationUIModel(false)
                //else if all 3 is null then VALID & so continue
            } else if (bp.systolic == null || bp.diastolic == null) {
                if (validationModel.status)
                    validationModel = ValidationUIModel(
                        false,
                        model?.errorMessage ?: context.getString(R.string.default_user_input_error)
                    )
            } else {
                bp.diastolic?.let { diastolic ->
                    bp.systolic?.let { systolic ->
                        var errorMessage =
                            getDiaSysErrorMsg(context, minValue, maxValue, diastolic, systolic)
                        errorMessage?.let {
                            validationModel = validateModel(validationModel, it)
                        } ?: kotlin.run {
                            bp.pulse?.let { pulseVal ->
                                errorMessage = getPulseErrorMsg(
                                    context,
                                    pulseVal,
                                    pulseMinValue,
                                    pulseMaxValue
                                )

                                errorMessage?.let {
                                    validationModel = validateModel(validationModel, it)
                                }
                            }
                        }
                        validEntries = validateEntry(validEntries, errorMessage, validationModel)
                    }
                }
            }
        }

        return if (validEntries < mandatoryCount) ValidationUIModel(
            false,
            validationModel.message
        ) else validationModel
    }

    private fun validateEntry(
        validEntries: Int,
        errorMessage: String?,
        validationModel: ValidationUIModel
    ): Int {
        var entry = validEntries
        if (errorMessage == null && validationModel.status)
            entry++
        return entry
    }

    private fun getPulseErrorMsg(
        context: Context,
        pulseVal: Double,
        pulseMinValue: Double,
        pulseMaxValue: Double
    ): String? {
        var errorMessage: String? = null
        when {
            pulseVal < pulseMinValue -> errorMessage =
                context.getString(
                    R.string.pulse_min_validation,
                    CommonUtils.getDecimalFormatted(pulseMinValue)
                )

            pulseVal > pulseMaxValue -> errorMessage =
                context.getString(
                    R.string.pulse_max_validation,
                    CommonUtils.getDecimalFormatted(pulseMaxValue)
                )
        }
        return errorMessage
    }

    private fun validateModel(validationModel: ValidationUIModel, it: String): ValidationUIModel {
        var model = validationModel
        if (validationModel.status)
            model = ValidationUIModel(false, it)
        return model
    }

    private fun getDiaSysErrorMsg(
        context: Context,
        minValue: Double,
        maxValue: Double,
        diastolic: Double,
        systolic: Double
    ): String? {
        var errorMessage: String? = null
        when {
            diastolic < minValue || systolic < minValue -> errorMessage = context.getString(
                R.string.systolic_diastolic_min_validation,
                CommonUtils.getDecimalFormatted(minValue)
            )

            diastolic > maxValue || systolic > maxValue ->
                errorMessage = context.getString(
                    R.string.systolic_diastolic_max_validation,
                    CommonUtils.getDecimalFormatted(maxValue)
                )

            diastolic > systolic -> errorMessage =
                context.getString(R.string.systolic_greater_than_diastolic)
        }
        return errorMessage
    }
}