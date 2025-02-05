package com.medtroniclabs.opensource.repo

import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.db.entity.SignsAndSymptomsEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderFiveYearsRequest
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class UnderFiveYearsRepository @Inject constructor(
    private var roomHelper: RoomHelper,
    private var apiHelper: ApiHelper
) {

    suspend fun getStaticMetaData(
        menuType: String
    ): Resource<Boolean> {
        return try {
            val response = apiHelper.getUnderFiveYearsMetaData()
            if (response.isSuccessful) {
                response.body()?.entity?.apply {
                    roomHelper.deleteExaminationsComplaints(menuType)
                    roomHelper.insertExaminationsComplaint(
                        generateChipItemByType(
                            systemicExaminations,
                            patientStatus,
                            immunisationStatus,
                            muac
                        )
                    )
                    roomHelper.deleteExaminationsList(MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name)
                    roomHelper.saveExaminationsList(examinations)
                    roomHelper.deleteDiagnosisList(MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name)
                    roomHelper.saveDiagnosisList(diseaseCategories)
                    roomHelper.deleteAllSymptoms()
                    roomHelper.insertSymptoms(convertToSignsAndSymptomsEntity(symptoms))
                }
                SecuredPreference.putBoolean(
                    SecuredPreference.EnvironmentKey.IS_UNDER_FIVE_YEARS_LOADED.name,
                    true
                )
                Resource(state = ResourceState.SUCCESS, true)
            } else {
                Resource(state = ResourceState.ERROR)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            SecuredPreference.putBoolean(
                SecuredPreference.EnvironmentKey.IS_UNDER_FIVE_YEARS_LOADED.name,
                false
            )
            Resource(state = ResourceState.ERROR)
        }

    }

    private fun convertToSignsAndSymptomsEntity(examinationList: List<MedicalReviewMetaItems>): List<SignsAndSymptomsEntity> {
        return examinationList.map { examination ->
            SignsAndSymptomsEntity(
                _id = examination.id,
                symptom = examination.name,
                type = examination.type,
                displayValue = null, // Assuming no direct mapping for this field
                displayOrder = examination.displayOrder,
                value = examination.value
            )
        }
    }

    private fun generateChipItemByType(
        systemicExaminations: List<MedicalReviewMetaItems>,
        patientStatus: List<MedicalReviewMetaItems>,
        immunisationStatus: ArrayList<MedicalReviewMetaItems>,
        muac: List<MedicalReviewMetaItems>
    ): List<MedicalReviewMetaItems> {
        val chipItemList = ArrayList<MedicalReviewMetaItems>()
        systemicExaminations.forEach {
            it.category = MedicalReviewTypeEnums.SystemicExaminations.name
        }
        immunisationStatus.forEach { it.type = MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name }
        muac.forEach { it.type = MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name }
        patientStatus.forEach { it.type = MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name }
        chipItemList.addAll(patientStatus)
        chipItemList.addAll(systemicExaminations)
        chipItemList.addAll(immunisationStatus)
        chipItemList.addAll(muac)
        return chipItemList
    }

    suspend fun createMedicalReviewForUnderFiveYears(request: CreateUnderFiveYearsRequest): Resource<CreateUnderTwoMonthsResponse> {
        return try {
            val response = apiHelper.createMedicalReviewForUnderFiveYears(request)
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

    suspend fun getMuAcStatusMetaItems(
        type: String
    ): Resource<List<MedicalReviewMetaItems>> {
        return try {
            val response = roomHelper.getSummaryDetailMetaItems(type)
            val filteredAndSortedResponse = response
                .filter { item -> item.category == MedicalReviewTypeEnums.muac.name }
                .sortedBy { it.displayOrder }
            Resource(state = ResourceState.SUCCESS, data = filteredAndSortedResponse)
        } catch (e: Exception) {
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