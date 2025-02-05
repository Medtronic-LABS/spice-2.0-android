package com.medtroniclabs.opensource.repo

import android.location.Location
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryDetails
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryRequest
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.model.AboveFiveYearsSubmitRequest
import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class AboveFiveYearsRepository @Inject constructor(
    private var roomHelper: RoomHelper,
    private var apiHelper: ApiHelper
) {
    suspend fun getStaticMetaData(
        menuType: String
    ): Resource<Boolean> {
        return try {
            val response = apiHelper.getAboveFiveYearsMetaData()
            if (response.isSuccessful) {
                response.body()?.entity?.apply {
                    roomHelper.deleteExaminationsComplaints(menuType)
                    roomHelper.insertExaminationsComplaint(
                        generateChipItemByType(
                            presentingComplaints,
                            systemicExaminations,
                            medicalSupplies,
                            cost,
                            patientStatus
                        )
                    )
                    roomHelper.deleteDiagnosisList(MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name)
                    //TODO: Type should be given from backend until we added type
                    diseaseCategories.onEach { it.type = MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name }
                    roomHelper.saveDiagnosisList(diseaseCategories)
                }
                SecuredPreference.putBoolean(
                    SecuredPreference.EnvironmentKey.IS_ABOVE_FIVE_YEARS_LOADED.name,
                    true
                )
                Resource(state = ResourceState.SUCCESS, true)
            } else {
                Resource(state = ResourceState.ERROR)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            SecuredPreference.putBoolean(
                SecuredPreference.EnvironmentKey.IS_ABOVE_FIVE_YEARS_LOADED.name,
                false
            )
            Resource(state = ResourceState.ERROR)
        }

    }

    private fun generateChipItemByType(
        presentingComplaints: List<MedicalReviewMetaItems>,
        systemicExaminations: List<MedicalReviewMetaItems>,
        medicalSupplies: List<MedicalReviewMetaItems>,
        cost: List<MedicalReviewMetaItems>,
        patientStatus: List<MedicalReviewMetaItems>
    ): List<MedicalReviewMetaItems> {
        val chipItemList = ArrayList<MedicalReviewMetaItems>()
        presentingComplaints.forEach {
            it.category = MedicalReviewTypeEnums.PresentingComplaints.name
        }
        systemicExaminations.forEach {
            it.category = MedicalReviewTypeEnums.SystemicExaminations.name
        }
        patientStatus.forEach { it.type = MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name }
        chipItemList.addAll(presentingComplaints)
        chipItemList.addAll(systemicExaminations)
        chipItemList.addAll(medicalSupplies)
        chipItemList.addAll(cost)
        chipItemList.addAll(patientStatus)
        return chipItemList
    }

    suspend fun createAboveFiveYears(
        details: PatientListRespModel,
        selectedComplaintsExaminationsPair: Pair<List<String?>, List<String?>>,
        enteredComplaintsExaminationsClinicalNotes: Triple<String, String, String>,
        lastLocation: Location?,
        prescriptionEncounterId: String?
    ): Resource<AboveFiveYearsSummaryDetails> {
        return try {
            val request = createSubmitRequest(
                details,
                selectedComplaintsExaminationsPair,
                enteredComplaintsExaminationsClinicalNotes,
                lastLocation,
                prescriptionEncounterId
            )
            val response = request?.let { apiHelper.createAboveFiveYearsResult(it) }
            if (response != null && response.isSuccessful) {
                val res = response.body()
                if (res?.status == true) {
                    Resource(state = ResourceState.SUCCESS, data = res.entity)
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    private fun createSubmitRequest(
        details: PatientListRespModel,
        selectedComplaintsExaminationsPair: Pair<List<String?>, List<String?>>,
        enteredComplaintsExaminationsClinicalNotes: Triple<String, String, String>,
        lastLocation: Location?,
        prescriptionEncounterId: String?
    ): AboveFiveYearsSubmitRequest? {
        return details.patientId?.let { patientId ->
                    details.memberId?.let { memberId ->
                        AboveFiveYearsSubmitRequest(
                            id = prescriptionEncounterId,
                            assessmentType = MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name,
                            presentingComplaints = selectedComplaintsExaminationsPair.first.filterNotNull(),
                            presentingComplaintsNotes = enteredComplaintsExaminationsClinicalNotes.first,
                            systemicExaminations = selectedComplaintsExaminationsPair.second.filterNotNull(),
                            systemicExaminationsNotes = enteredComplaintsExaminationsClinicalNotes.second,
                            clinicalNotes = enteredComplaintsExaminationsClinicalNotes.third,
                            encounter = MedicalReviewEncounter(
                                id = prescriptionEncounterId,
                                patientId = patientId,
                                provenance = ProvanceDto(),
                                latitude = lastLocation?.latitude ?: 0.0,
                                longitude = lastLocation?.longitude ?: 0.0,
                                householdId = details.houseHoldId,
                                memberId = memberId,
                                startTime = DateUtils.getCurrentDateAndTime(
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                                ),
                                endTime = DateUtils.getCurrentDateAndTime(
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                                ),
                                referred = true
                            )
                        )
                }
        }
    }

    suspend fun getSummaryDetailMetaItems(
        type: String
    ): Resource<List<MedicalReviewMetaItems>> {
        return try {
            val response = roomHelper.getSummaryDetailMetaItems(type)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAboveFiveYearsSummaryDetails(
        request: AboveFiveYearsSummaryRequest
    ): Resource<AboveFiveYearsSummaryDetails> {
        return try {
            val response = apiHelper.getAboveFiveYearsSummaryDetails(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}