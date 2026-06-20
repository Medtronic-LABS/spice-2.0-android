package org.medtroniclabs.uhis.data

import androidx.room.Relation
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryData
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryDataItem
import org.medtroniclabs.uhis.db.entity.FollowUpCall
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MMOLL

data class FollowUpPatientModel(
    val id: Long,
    val localPatientId: Long,
    val name: String?,
    val patientId: String?,
    val phoneNumber: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val reason: String?,
    val patientStatus: String?,
    val village: String?,
    val householdId: Long?,
    val householdName: String?,
    val landmark: String?,
    val type: String?,
    val encounterType: String?,
    val encounterName: String?,
    val encounterId: String? = null,
    val calledAt: Long? = null,
    val attempts: Int = 0,
    val nextVisitDate: String? = null,
    val encounterDate: String? = null,
    val isWrongNumber: Boolean,
    val updatedAt: Long,
    val recentCallStatus: String? = null,
    val remainingAttempts: Int = 0,
    @Relation(
        parentColumn = "id",
        entityColumn = "followUpId",
    )
    val followUpCalls: List<FollowUpCall> = emptyList(),
    @Relation(
        parentColumn = "encounterId",
        entityColumn = "encounterId",
    )
    val memberAssessmentHistory: List<MemberAssessmentHistoryEntity> = emptyList(),
) {
    fun getReason(default: String): String =
        if (!reason?.trim().isNullOrEmpty()) {
            reason.trim()
        } else {
            default
        }

    fun toPatientHistoryData(): List<PatientHistoryData> {
        val historyData = mutableListOf<PatientHistoryData>()
        historyData.add(
            PatientHistoryData(
                R.string.bio_data,
                listOf(
                    buildList {
                        phoneNumber?.let {
                            add(PatientHistoryDataItem(R.string.mobile_number, it))
                        }
                        reason?.let {
                            add(PatientHistoryDataItem(R.string.referred_reason, it))
                        }
                        memberAssessmentHistory.firstOrNull()?.observations?.let { safeObservation ->
                            safeObservation.bp?.let {
                                add(PatientHistoryDataItem(R.string.average_blood_pressure, it, R.string.mm_HG))
                            }
                            safeObservation.bg?.let {
                                val formatted = if (it.contains("mmol", ignoreCase = true)) {
                                    it
                                } else {
                                    "$it $MMOLL"
                                }
                                add(
                                    if (safeObservation.bgType == DefinedParams.FBS) {
                                        PatientHistoryDataItem(R.string.blood_glucose_fbs, formatted)
                                    } else {
                                        PatientHistoryDataItem(R.string.blood_glucose_rbs, formatted)
                                    },
                                )
                            }
                        }
                        encounterDate?.let {
                            add(
                                PatientHistoryDataItem(
                                    R.string.last_visit_date,
                                    DateUtils.convertDateFormat(
                                        encounterDate,
                                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                        DATE_ddMMyyyy,
                                    ),
                                ),
                            )
                        }
                        nextVisitDate?.let {
                            add(
                                PatientHistoryDataItem(
                                    R.string.next_visit_date,
                                    DateUtils.convertDateFormat(
                                        nextVisitDate,
                                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                        DATE_ddMMyyyy,
                                    ),
                                ),
                            )
                            val daysDiff = DateUtils.getDaysDifference(nextVisitDate)
                            if (daysDiff > 0) {
                                add(PatientHistoryDataItem(R.string.days_due_string, daysDiff.toString()))
                            }
                        }
                    },
                ),
                false,
            ),
        )
        if (followUpCalls.isNotEmpty()) {
            val callInformationItems = followUpCalls.map { it.toPatientHistoryDataItem() }
            historyData.add(PatientHistoryData(R.string.call_information, callInformationItems))
        }
        return historyData
    }
}
