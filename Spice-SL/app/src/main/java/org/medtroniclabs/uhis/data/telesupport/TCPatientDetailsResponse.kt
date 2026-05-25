package org.medtroniclabs.uhis.data.telesupport

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
)

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
)

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
