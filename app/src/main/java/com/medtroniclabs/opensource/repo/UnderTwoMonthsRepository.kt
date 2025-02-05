package com.medtroniclabs.opensource.repo

import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsRequest
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.BirthHistoryRequest
import com.medtroniclabs.opensource.data.BirthHistoryResponse
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.model.medicalreview.SummaryDetails
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class UnderTwoMonthsRepository @Inject constructor(
    private val roomHelper: RoomHelper,
    private val apiHelper: ApiHelper
) {

    suspend fun getStaticMetaData(): Resource<Boolean> {
        return try {
            val response = apiHelper.getUnderTwoMonthsMetaData()
            if (response.isSuccessful) {
                response.body()?.entity?.apply {
                    roomHelper.deleteDiagnosisList(MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name)
                    roomHelper.saveDiagnosisList(diseaseCategories)
                    roomHelper.deleteExaminationsList(MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name)
                    roomHelper.saveExaminationsList(examinations)
                    roomHelper.deleteExaminationsComplaints(MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name)
                    roomHelper.insertExaminationsComplaint(
                        generateChipItemByType(
                            patientStatus,
                            immunisationStatus

                        )
                    )
                }
                SecuredPreference.putBoolean(
                    SecuredPreference.EnvironmentKey.IS_UNDER_TWO_MONTHS_LOADED.name,
                    true
                )
                Resource(state = ResourceState.SUCCESS, true)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SecuredPreference.putBoolean(
                SecuredPreference.EnvironmentKey.IS_UNDER_TWO_MONTHS_LOADED.name,
                false
            )
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun createMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsRequest): Resource<CreateUnderTwoMonthsResponse> {
        return try {
            val response = apiHelper.createMedicalReviewForUnderTwoMonths(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                val errorMessage = StringConverter.getErrorMessage(response.errorBody())
                Resource(state = ResourceState.ERROR, message = errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getBirthHistoryDetailsUnderTwoMonths(request: BirthHistoryRequest): Resource<BirthHistoryResponse> {
        return try {
            val response = apiHelper.getBirthHistoryDetails(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                val errorMessage = StringConverter.getErrorMessage(response.errorBody())
                Resource(state = ResourceState.ERROR, message = errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }


    }

    suspend fun getSummaryDetailMetaItems(
        type: String
    ): Resource<List<MedicalReviewMetaItems>> {
        return try {
            val response = roomHelper.getSummaryDetailMetaItems(type)
            val filteredAndSortedResponse = response
                .filter { item -> item.category == MedicalReviewTypeEnums.patient_status.name }
                .sortedBy { it.displayOrder }
            Resource(state = ResourceState.SUCCESS, data = filteredAndSortedResponse)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
    suspend fun getImmunisationStatusMetaItems(
        type: String
    ): Resource<List<MedicalReviewMetaItems>> {
        return try {
            val response = roomHelper.getSummaryDetailMetaItems(type)
            val filteredAndSortedResponse = response
                .filter { item -> item.category == MedicalReviewTypeEnums.immunisation_status.name }
                .sortedBy { it.displayOrder }
            Resource(state = ResourceState.SUCCESS, data = filteredAndSortedResponse)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    private fun generateChipItemByType(
        patientStatus: List<MedicalReviewMetaItems>,
        immunisationStatus: ArrayList<MedicalReviewMetaItems>
    ): List<MedicalReviewMetaItems> {
        val chipItemList = ArrayList<MedicalReviewMetaItems>()
        patientStatus.forEach { it.type = MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name }
        chipItemList.addAll(patientStatus)

        immunisationStatus.forEach { it.type = MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name }
        chipItemList.addAll(immunisationStatus)
        return chipItemList
    }

    suspend fun getMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsResponse): Resource<SummaryDetails> {
        return try {
            val response =
                apiHelper.getMedicalReviewForUnderTwoMonths(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                val errorMessage = StringConverter.getErrorMessage(response.errorBody())
                Resource(state = ResourceState.ERROR, message = errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }
    suspend fun getWazWhzScore(request: WazWhzScoreRequest): Resource<WazWhzScoreResponse> {
        return try {
            val response = apiHelper.getWazWhzScore(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                val errorMessage = StringConverter.getErrorMessage(response.errorBody())
                Resource(state = ResourceState.ERROR, message = errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }
}

