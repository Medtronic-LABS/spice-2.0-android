package com.medtroniclabs.opensource.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMdd
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.data.FollowUpPatientModel
import com.medtroniclabs.opensource.data.offlinesync.model.FollowUpCallStatus
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.FollowUp
import com.medtroniclabs.opensource.db.entity.FollowUpCall
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.followup.FollowUpFilter
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class FollowUpRepository @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {

    fun getFollowUpListLiveData(filter: FollowUpFilter, referralLimit: Int): LiveData<List<FollowUpPatientModel>> {
        val villageIds = if (filter.selectedVillages.isNullOrEmpty()) {
            filter.villages
        } else {
            filter.selectedVillages!!.map { it.id!! }
        }

        val fromAndToDate = getFromDateAndToDate(filter, referralLimit)

        return roomHelper.getFollowUpPatientListLiveData(
            filter.type,
            filter.search,
            villageIds,
            fromAndToDate.first,
            fromAndToDate.second
        ).map { list -> list.sortedBy { it.updatedAt } }
    }

    private fun getFromDateAndToDate(filter: FollowUpFilter, referralLimit: Int): Pair<String, String> {
        if (filter.selectedDateRange.isNullOrEmpty()) {
            return Pair("", "")
        }

        if (filter.selectedDateRange?.any { it.name == FollowUpDefinedParams.FilterToday } == true) {
            val date = getTodayDateString(filter.type, referralLimit)
            return Pair(date, date)
        }

        if (filter.selectedDateRange?.any { it.name == FollowUpDefinedParams.FilterTomorrow } == true) {
            val date = getTomorrowDateString(filter.type, referralLimit)
            return Pair(date, date)
        }

        if (filter.selectedDateRange?.any { it.name == FollowUpDefinedParams.FilterCustomize } == true) {
            return getDateRange(filter, referralLimit)
        }

        return Pair("", "")
    }

    private fun getTodayDateString(type: String, referralLimit: Int) : String {
        val format = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd)
        var today = LocalDate.now().atStartOfDay()

        if (type == FollowUpDefinedParams.FU_TYPE_REFERRED)
            today = today.minusDays(referralLimit.toLong())

        return today.format(format)
    }

    private fun getTomorrowDateString(type: String, referralLimit: Int): String {
        val format = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd)
        var tomorrow = LocalDate.now().atStartOfDay().plusDays(1)

        if (type == FollowUpDefinedParams.FU_TYPE_REFERRED)
            tomorrow = tomorrow.minusDays(referralLimit.toLong())

        return tomorrow.format(format)
    }

    private fun getDateRange(filter: FollowUpFilter, referralLimit: Int): Pair<String, String> {
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

    suspend fun getVillageIds(): List<VillageEntity> {
        return roomHelper.getAllVillageEntity()
    }

    suspend fun getUnSyncedFollowUpCount(): Int {
        return roomHelper.getUnSyncedFollowUpCount()
    }

    suspend fun addCallHistory(
        maxSuccessfulCallLimit: Int,
        maxUnSuccessfulCallLimit: Int,
        followUpId: Long,
        callStatus: FollowUpCallStatus,
        patientStatus: String? = null,
        reason: String? = null
    ) {
        val followUp = roomHelper.getFollowUpById(followUpId)
        followUp.syncStatus = OfflineSyncStatus.NotSynced
        followUp.attempts = followUp.attempts + 1

        val callDetail = FollowUpCall(
            followUpId = followUpId,
            callDate = System.currentTimeMillis().convertToUtcDateTime(),
            duration = 0,
            attempts =  followUp.attempts,
            status = callStatus,
            patientStatus = patientStatus,
            reason = reason
        )

        var newFollowUp: FollowUp? = null
        if (callDetail.status == FollowUpCallStatus.SUCCESSFUL) {
            newFollowUp = handleSuccessCall(followUpId, followUp, callDetail, maxSuccessfulCallLimit)
        } else {
            handleUnSuccessfulCall(followUpId, followUp, callDetail, maxUnSuccessfulCallLimit)
        }

        roomHelper.addCallHistory(followUp, callDetail, newFollowUp)
    }

    private suspend fun handleSuccessCall(id: Long,followUp: FollowUp, call: FollowUpCall, maxSuccessfulCallLimit: Int): FollowUp? {
        // Get All other followup with same reason
        call.patientStatus?.let {
            updatePatientStatus(it, id, followUp)
        }

        followUp.successfulAttempts = followUp.successfulAttempts + 1
        if (followUp.successfulAttempts >= maxSuccessfulCallLimit) {
            followUp.isCompleted = true
            roomHelper.updateOtherDuplicateTickets(id, followUp)
        }

        return null
    }

    private suspend fun updatePatientStatus(status: String, id: Long, followUp: FollowUp) {
        followUp.currentPatientStatus = status
        val currentTime = System.currentTimeMillis()
        followUp.updatedAt = currentTime
        followUp.calledAt = currentTime
        when (status.lowercase()) {
            ReferralStatus.Recovered.name.lowercase() -> {
                followUp.isCompleted = true
                roomHelper.updateDuplicateTicketsAsCompleted(id, followUp)
            }

            ReferralStatus.Referred.name.lowercase() -> {
                if (followUp.type == FollowUpDefinedParams.FU_TYPE_REFERRED) {
                    roomHelper.updateOnTreatmentStatus(id, followUp, currentTime)
                } else {
                    followUp.isCompleted = true
                    roomHelper.updateDuplicateTicketsAsCompleted(id, followUp)
                }
            }
            else -> {
                roomHelper.updateOnTreatmentStatus(id, followUp, currentTime)
            }
        }
    }

    private suspend fun handleUnSuccessfulCall(followUpId: Long, followUp: FollowUp, call: FollowUpCall, maxUnSuccessfulCallLimit: Int) {
        followUp.unsuccessfulAttempts = followUp.unsuccessfulAttempts + 1
        if (followUp.unsuccessfulAttempts >= maxUnSuccessfulCallLimit) {
            followUp.isCompleted = true
        }

        if (call.reason?.equals(FollowUpDefinedParams.WRONG_NUMBER, true) == true) {
            followUp.isWrongNumber = true
            followUp.isCompleted = followUp.type != FollowUpDefinedParams.FU_TYPE_HH_VISIT
            roomHelper.updateOtherFollowUpForWrongNumber(followUpId, followUp.memberId)
        }
    }

}