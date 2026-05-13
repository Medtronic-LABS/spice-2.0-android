package org.medtroniclabs.uhis.ui.dashboard.ncd.repository

import org.medtroniclabs.uhis.data.NCDUserDashboardRequest
import org.medtroniclabs.uhis.data.NCDUserDashboardResponse
import org.medtroniclabs.uhis.db.local.RoomHelper
import java.time.LocalDate
import javax.inject.Inject

class DashboardLocalRepository @Inject constructor(
    private val roomHelper: RoomHelper,
) {
    suspend fun getLocalDashboardDetails(request: NCDUserDashboardRequest): NCDUserDashboardResponse {
        val (start, end) = resolveDateRange(request)
        val ssIds = request.filterBySs ?: emptyList()
        val subVillageIds = request.filterBySubVillages ?: emptyList()
        val row = roomHelper.getDashboardCounts(start, end, ssIds, subVillageIds)
        return NCDUserDashboardResponse(
            screened = row.screened ?: 0,
            assessed = row.assessed ?: 0,
            registered = row.registered ?: 0,
            referred = row.referred ?: 0,
            dispensed = row.dispensed ?: 0,
            investigated = row.investigated ?: 0,
            nutritionistLifestyleCount = row.nutritionistLifestyleCount ?: 0,
            psychologicalNotesCount = row.psychologicalNotesCount ?: 0,
            familyPlanningCount = row.familyPlanningCount ?: 0,
            pregnantWomenRegistrationCount = row.pregnantWomenRegistrationCount ?: 0,
            pregnancyOutcomeCount = row.pregnancyOutcomeCount ?: 0,
            ancCount = row.ancCount ?: 0,
            pncCount = row.pncCount ?: 0,
            childVisitCount = row.childVisitCount ?: 0,
            tbAssessmentCount = row.tbAssessmentCount ?: 0,
            tbContactTracingCount = row.tbContactTracingCount ?: 0,
            eyeCareCount = row.eyeCareCount ?: 0,
            cataractCount = row.cataractCount ?: 0,
            householdRegisteredCount = row.householdRegisteredCount ?: 0,
            pwIdentifiedFirst4MonthsWithAncCount = row.pwIdentifiedFirst4MonthsWithAncCount ?: 0,
            anc3PlusCount = row.anc3PlusCount ?: 0,
            highRiskPregnantWomenCount = row.highRiskPregnantWomenCount ?: 0,
            totalNcdServicesCount = row.totalNcdServicesCount ?: 0,
            ncdScreeningFirstServiceCount = row.ncdScreeningFirstServiceCount ?: 0,
            ncdFollowUpAssessmentCount = row.ncdFollowUpAssessmentCount ?: 0,
            ncdFollowUpReferralCount = row.ncdFollowUpReferralCount ?: 0,
            glassesSoldCustomStatusCount = row.glassesSoldCustomStatusCount ?: 0,
            ncdServicesInCataractCampCount = row.ncdServicesInCataractCampCount ?: 0,
            patientsReferredForOperationCount = row.patientsReferredForOperationCount ?: 0,
        )
    }

    private fun resolveDateRange(request: NCDUserDashboardRequest): Pair<String?, String?> {
        // Returns yyyy-MM-dd strings for SQL substr(date,1,10) comparison
        request.customDate?.let {
            val start = it.startDate?.take(10)
            val end = it.endDate?.take(10)
            return Pair(start, end)
        }
        return when (request.sortField?.lowercase()) {
            "today" -> {
                val d = LocalDate.now().toString()
                d to d
            }
            "week" -> {
                val today = LocalDate.now()
                val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
                startOfWeek.toString() to today.toString()
            }
            "month" -> {
                val today = LocalDate.now()
                val startOfMonth = today.withDayOfMonth(1)
                startOfMonth.toString() to today.toString()
            }
            else -> Pair(null, null)
        }
    }
}
