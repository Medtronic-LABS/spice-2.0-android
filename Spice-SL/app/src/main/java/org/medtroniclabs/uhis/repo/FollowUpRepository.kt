package org.medtroniclabs.uhis.repo

import androidx.lifecycle.LiveData
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMdd
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.FollowUpPatientModel
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.FollowUp
import org.medtroniclabs.uhis.db.entity.FollowUpCall
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.model.followup.FollowUpFilter
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class FollowUpRepository @Inject constructor(
    private val roomHelper: RoomHelper,
) {
    fun getFollowUpListLiveData(
        filter: FollowUpFilter,
        referralLimit: Int,
    ): LiveData<List<FollowUpPatientModel>> {
        val villageIds = filter.selectedVillages?.map { it.id!! } ?: listOf()
        val villageIdsSize = villageIds.size
        val shashthyaShebikaIds = filter.selectedShashtyaShebikas?.map { it.id!! } ?: listOf()
        val shashthyaShebikaIdsSize = shashthyaShebikaIds.size
        // Filter out both from selected reason and get the first one, as it is single selection
        val ncdSelectedReason = filter.ncdSelectedReasons
            ?.map { it.type!! }
            ?.filterNot { it == FollowUpDefinedParams.BOTH }
            ?.firstOrNull()
        // Get first referral facility as is single selection
        val ncdSelectedReferralTo = filter.ncdSelectedReferralTo?.map { it.type!! }?.firstOrNull()
        // If there is any ncd selected reason or ncd referral facility, then remove NCD from the selected referral reason
        val selectedReferralReasonTypes = filter.selectedReferralReasons?.map { it.type!!.lowercase() }?.filterNot {
            (ncdSelectedReason != null || ncdSelectedReferralTo != null) && it == FollowUpDefinedParams.FILTER_NCD.lowercase()
        } ?: listOf()
        val selectedReferralReasonTypesSize = selectedReferralReasonTypes.size

        val fromAndToDate = getFromDateAndToDate(filter, referralLimit)

        val result = roomHelper.getFollowUpPatientListLiveData(
            filter.type,
            filter.search,
            shashthyaShebikaIds,
            shashthyaShebikaIdsSize,
            villageIds,
            villageIdsSize,
            selectedReferralReasonTypes,
            selectedReferralReasonTypesSize,
            ncdSelectedReason,
            ncdSelectedReferralTo,
            fromAndToDate.first,
            fromAndToDate.second,
        )

        return result
    }

    private fun getFromDateAndToDate(
        filter: FollowUpFilter,
        referralLimit: Int,
    ): Pair<String, String> {
        if (filter.selectedDateRange.isNullOrEmpty()) {
            return Pair("", "")
        }

        if (filter.selectedDateRange?.any { it.name == FollowUpDefinedParams.FILTER_TODAY } == true) {
            val date = getTodayDateString(filter.type, referralLimit)
            return Pair(date, date)
        }

        if (filter.selectedDateRange?.any { it.name == FollowUpDefinedParams.FILTER_TOMORROW } == true) {
            val date = getTomorrowDateString(filter.type, referralLimit)
            return Pair(date, date)
        }

        if (filter.selectedDateRange?.any { it.name == FollowUpDefinedParams.FILTER_CUSTOMIZE } == true) {
            return getDateRange(filter, referralLimit)
        }

        return Pair("", "")
    }

    private fun getTodayDateString(
        type: String,
        referralLimit: Int,
    ): String {
        val format = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd)
        var today = LocalDate.now().atStartOfDay()

        if (type == FollowUpDefinedParams.FU_TYPE_REFERRED) {
            today = today.minusDays(referralLimit.toLong())
        }

        return today.format(format)
    }

    private fun getTomorrowDateString(
        type: String,
        referralLimit: Int,
    ): String {
        val format = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd)
        var tomorrow = LocalDate.now().atStartOfDay().plusDays(1)

        if (type == FollowUpDefinedParams.FU_TYPE_REFERRED) {
            tomorrow = tomorrow.minusDays(referralLimit.toLong())
        }

        return tomorrow.format(format)
    }

    private fun getDateRange(
        filter: FollowUpFilter,
        referralLimit: Int,
    ): Pair<String, String> {
        val outputFormat = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd)
        val inputFormat = DateTimeFormatter.ofPattern(DATE_ddMMyyyy)
        var fromDate = LocalDate.parse(filter.fromDate, inputFormat)
        var toDate = LocalDate.parse(filter.toDate, inputFormat)

        if (filter.type == FollowUpDefinedParams.FU_TYPE_REFERRED) {
            fromDate = fromDate.minusDays(referralLimit.toLong())
            toDate = toDate.minusDays(referralLimit.toLong())
        }

        return Pair(fromDate.format(outputFormat), toDate.format(outputFormat))
    }

    suspend fun getUnSyncedFollowUpCount(): Int = roomHelper.getUnSyncedFollowUpCount()

    suspend fun addCallHistory(
        followUpId: Long,
        callType: String?,
        status: FollowUpCallStatus,
        isWillingToVisitUHC: Boolean?,
        visitRejectReason: String?,
        otherVisitRejectReason: String?,
        unSuccessfulCallReason: String?,
        wrongNumber: Boolean,
        totalTimeTaken: Double?,
    ) {
        val followUp = roomHelper.getFollowUpById(followUpId)
        followUp.syncStatus = OfflineSyncStatus.NotSynced
        followUp.attempts += 1

        val lat = SecuredPreference.getDouble(SecuredPreference.EnvironmentKey.CURRENT_LATITUDE.name)
        val lng = SecuredPreference.getDouble(SecuredPreference.EnvironmentKey.CURRENT_LONGITUDE.name)

        val callDetail = FollowUpCall(
            followUpId = followUpId,
            callDate = System.currentTimeMillis().convertToUtcDateTime(),
            duration = totalTimeTaken,
            attempts = followUp.attempts,
            status = status,
            latitude = lat,
            longitude = lng,
            callType = callType,
            isWillingToVisitUHC = isWillingToVisitUHC,
            visitRejectReason = visitRejectReason,
            otherVisitRejectReason = otherVisitRejectReason,
            unSuccessfulCallReason = unSuccessfulCallReason,
            wrongNumber = wrongNumber,
        )

        if (callDetail.status == FollowUpCallStatus.SUCCESSFUL) {
            handleSuccessCall(followUp)
        } else {
            handleUnSuccessfulCall(followUp, callDetail)
        }

        roomHelper.addCallHistory(followUp, callDetail)
    }

    private fun handleSuccessCall(followUp: FollowUp) {
        followUp.successfulAttempts += 1
    }

    private fun handleUnSuccessfulCall(
        followUp: FollowUp,
        call: FollowUpCall,
    ) {
        followUp.unsuccessfulAttempts += 1

        if (call.wrongNumber) {
            followUp.isWrongNumber = true
            followUp.isCompleted = true
        }
    }
}
