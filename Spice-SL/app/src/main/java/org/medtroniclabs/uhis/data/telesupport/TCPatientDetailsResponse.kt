package org.medtroniclabs.uhis.data.telesupport

import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryDataItem
import java.util.Locale

data class TCPatientDetailsResponse(
    val bioData: TCBioData?,
    val lastTenClinicalInformation: List<TCClinicalInformation>,
    val lastTenCallInformation: List<TCCallInformation>,
)

data class TCBioData(
    val firstName: String?,
    val lastName: String?,
    val age: String?,
    val gender: String?,
    val mobileNumber: String?,
    val upazilaName: String?,
    val address: String?,
    val diagnosis: String?,
    val enrolledDate: String?,
    val cvdRisk: String?,
    val bmi: Double?,
    val comorbidity: String?,
    val lastVisitDate: String?,
    val nextVisitDate: String?,
    val unionName: String?,
    val villageName: String?,
    val referredReason: String?,
    val latestScreeningDate: String?,
    val latestScreenedByName: String?,
    val latestScreenedByRole: String?,
    val latestScreeningAvgSystolic: Int?,
    val latestScreeningAvgDiastolic: Int?,
    val latestScreeningGlucoseType: String?,
    val latestScreeningGlucoseValue: String?,
    val latestScreeningGlucoseUnit: String?,
) {
    private fun getDisplayableName(): String {
        val genderAbbr = CommonUtils.getGenderConstant(gender)
        val fullName = listOfNotNull(firstName, lastName).joinToString(" ")

        return "$fullName - $age - $genderAbbr"
    }

    private fun getBPItem(): PatientHistoryDataItem? {
        latestScreeningAvgSystolic?.let { sys ->
            latestScreeningAvgDiastolic?.let { dia ->
                return PatientHistoryDataItem(R.string.average_blood_pressure, "$sys / $dia", R.string.mm_HG)
            }
        }

        return null
    }

    private fun getBGItem(): PatientHistoryDataItem? {
        latestScreeningGlucoseType?.let { type ->
            latestScreeningGlucoseValue?.let { values ->
                latestScreeningGlucoseUnit?.let { unit ->
                    return if (type == DefinedParams.FBS) {
                        PatientHistoryDataItem(R.string.blood_glucose_fbs, "$values $unit")
                    } else {
                        PatientHistoryDataItem(R.string.blood_glucose_rbs, "$values $unit")
                    }
                }
            }
        }

        return null
    }

    fun getPatientHistoryDataItems(): List<PatientHistoryDataItem> =
        buildList {
            add(PatientHistoryDataItem(0, getDisplayableName()))

            mobileNumber?.let { add(PatientHistoryDataItem(R.string.mobile_number, it)) }

            // age?.let { add(PatientHistoryDataItem(R.string.age, it)) }
            // gender?.let { add(PatientHistoryDataItem(R.string.gender, it)) }

            upazilaName?.let { add(PatientHistoryDataItem(R.string.upazila, it)) }

            val fullAddress = listOfNotNull(unionName, villageName).joinToString(", ")
            add(PatientHistoryDataItem(R.string.address, fullAddress))

            diagnosis?.let { add(PatientHistoryDataItem(R.string.diagnosis, it)) }
            referredReason?.let { add(PatientHistoryDataItem(R.string.referred_reason, it)) }

            enrolledDate?.let {
                add(
                    PatientHistoryDataItem(
                        R.string.enrolled_date,
                        DateUtils.convertDateFormat(
                            enrolledDate,
                            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DATE_ddMMyyyy,
                        ),
                    ),
                )
            }

            add(
                PatientHistoryDataItem(
                    R.string.cvd_risk_bmi,
                    "${cvdRisk ?: "--"} , ${bmi ?: "--"}",
                ),
            )

            getBPItem()?.let { add(it) }

            getBGItem()?.let { add(it) }

            comorbidity?.let { add(PatientHistoryDataItem(R.string.co_morbidity, it)) }

            lastVisitDate?.let {
                add(
                    PatientHistoryDataItem(
                        R.string.last_visit_date,
                        DateUtils.convertDateFormat(
                            lastVisitDate,
                            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DATE_ddMMyyyy,
                        ),
                    ),
                )
            }

            nextVisitDate?.let { nvd ->
                add(
                    PatientHistoryDataItem(
                        R.string.next_visit_date,
                        DateUtils.convertDateFormat(
                            nvd,
                            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DATE_ddMMyyyy,
                        ),
                    ),
                )
                val daysDiff = DateUtils.getDaysDifference(nvd)
                if (daysDiff > 0) {
                    add(PatientHistoryDataItem(R.string.days_due_string, daysDiff.toString()))
                }
            }

            latestScreeningDate?.let {
                add(
                    PatientHistoryDataItem(
                        R.string.screened_date,
                        DateUtils.convertDateFormat(
                            latestScreeningDate,
                            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DATE_ddMMyyyy,
                        ),
                    ),
                )
            }
            latestScreenedByName?.let {
                add(
                    PatientHistoryDataItem(
                        R.string.screened_by,
                        "$it ${RoleConstant.getRoleInDisplayFormatWithBraces(latestScreenedByRole)}",
                    ),
                )
            }
        }
}

data class TCClinicalInformation(
    val visitDate: String?,
    val visitPlace: String?,
    val performedBy: String?,
    val bloodPressure: String?,
    val bgFbs: String?,
    val symptoms: String?,
    val medications: List<String>?,
    val referredToCc: String?,
    val performedByRole: String?,
) {
    fun getPatientHistoryDataItems(): List<PatientHistoryDataItem> {
        val items = mutableListOf<PatientHistoryDataItem>()

        items.add(
            PatientHistoryDataItem(
                R.string.visit_date,
                visitDate?.let {
                    DateUtils.convertDateFormat(
                        visitDate,
                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DATE_ddMMyyyy,
                    )
                },
            ),
        )
        items.add(PatientHistoryDataItem(R.string.visit_place, visitPlace))
        items.add(PatientHistoryDataItem(R.string.performed_by, "$performedByRole ${RoleConstant.getRoleInDisplayFormatWithBraces(performedByRole)}"))
        items.add(PatientHistoryDataItem(R.string.blood_pressure, bloodPressure))
        items.add(PatientHistoryDataItem(R.string.bg_fbs, bgFbs))
        items.add(PatientHistoryDataItem(R.string.symptoms, symptoms))
        items.add(PatientHistoryDataItem(R.string.medication, medications?.joinToString(", ")))
        items.add(PatientHistoryDataItem(R.string.referred_to_cc, referredToCc))

        return items
    }
}

data class TCCallInformation(
    val callDate: String?,
    val calledBy: String?,
    val callCategory: String?,
    val callStatus: String?,
    val agreedToVisit: Boolean?,
    val reason: String?,
    val calledByRole: String?,
    val callDuration: Double?,
) {
    fun getPatientHistoryDataItems(): List<PatientHistoryDataItem> {
        val items = mutableListOf<PatientHistoryDataItem>()

        val calledAt = callDate?.let {
            DateUtils.convertDateFormat(
                callDate,
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_ddMMyyyy,
            )
        }
        val duration = formatDuration()
        // val duration = ""

        items.add(PatientHistoryDataItem(R.string.call_date, "$calledAt $duration"))
        items.add(PatientHistoryDataItem(R.string.called_by, "$calledBy ${RoleConstant.getRoleInDisplayFormatWithBraces(calledByRole)}"))
        items.add(PatientHistoryDataItem(R.string.call_category, getCallCategory(callCategory)))
        items.add(PatientHistoryDataItem(R.string.call_status, callStatus))
        items.add(PatientHistoryDataItem(R.string.agreed_to_visit, getAgreedToVisit(agreedToVisit)))
        items.add(PatientHistoryDataItem(R.string.reason, reason))

        return items
    }

    private fun getAgreedToVisit(visit: Boolean?): String? {
        visit?.let {
            return if (it) "Yes" else "No"
        }
        return null
    }

    private fun getCallCategory(cc: String?): String? = cc

    private fun formatDuration(): String {
        callDuration?.let {
            val totalSeconds = (it * 60).toLong()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            return String.format(Locale.ENGLISH, "(%02d:%02d min)", minutes, seconds)
        }

        return ""
    }
}
