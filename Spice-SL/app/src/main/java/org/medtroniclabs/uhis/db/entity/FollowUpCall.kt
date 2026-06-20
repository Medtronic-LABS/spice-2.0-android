package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryDataItem
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP_CALL
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import java.util.Locale

@Entity(tableName = FOLLOW_UP_CALL)
data class FollowUpCall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val followUpId: Long,
    val callDate: String,
    val duration: Double?,
    val attempts: Int = 0,
    val status: FollowUpCallStatus = FollowUpCallStatus.UNSUCCESSFUL,
    val callType: String?,
    val isWillingToVisitUHC: Boolean?,
    val visitRejectReason: String?,
    val otherVisitRejectReason: String?,
    val unSuccessfulCallReason: String?,
    val wrongNumber: Boolean = false,
    val isSynced: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val calledByUserId: String = SecuredPreference.getUserId().toString(),
    val calledByUserFullName: String? = AssessmentUtil.getLocalServiceProvidedByName(),
    val calledByUserRole: String = SecuredPreference.getRole(),
) {
    @Ignore
    var callRegisterId: Long = 0

    fun toPatientHistoryDataItem(): List<PatientHistoryDataItem> {
        val items = mutableListOf<PatientHistoryDataItem>()
        val calledAt = DateUtils.convertDateFormat(
            callDate,
            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DATE_ddMMyyyy,
        )
        val duration = formatDuration()
        items.add(PatientHistoryDataItem(R.string.call_date, "$calledAt $duration"))
        items.add(PatientHistoryDataItem(R.string.called_by, "$calledByUserFullName ${RoleConstant.getRoleInDisplayFormatWithBraces(calledByUserRole)}"))
        items.add(PatientHistoryDataItem(R.string.call_category, getCallCategory(callType)))
        items.add(PatientHistoryDataItem(R.string.call_status, status.name))
        items.add(PatientHistoryDataItem(R.string.agreed_to_visit, getAgreedToVisit(isWillingToVisitUHC)))
        items.add(PatientHistoryDataItem(R.string.reason, visitRejectReason))
        return items
    }

    private fun formatDuration(): String {
        duration?.let {
            val totalSeconds = (it * 60).toLong()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            return String.format(Locale.ENGLISH, "(%02d:%02d min)", minutes, seconds)
        }
        return ""
    }

    private fun getCallCategory(cc: String?): String? {
        if (cc == "MISSED_VISIT") {
            return DefinedParams.MISSED_VISIT
        }

        return cc
    }

    private fun getAgreedToVisit(visit: Boolean?): String? {
        visit?.let {
            return if (it) "Yes" else "No"
        }
        return null
    }
}
