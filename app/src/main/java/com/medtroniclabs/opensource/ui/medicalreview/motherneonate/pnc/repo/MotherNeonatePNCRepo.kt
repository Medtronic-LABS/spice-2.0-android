package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.pnc.repo

import com.medtroniclabs.opensource.data.model.MotherNeonatePncRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.MedicalReviewSummarySubmitRequest
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryRequest
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryResponse
import com.medtroniclabs.opensource.data.model.PncSubmitResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class MotherNeonatePNCRepo @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    suspend fun getMotherPncStaticData(motherNeonateMetaResponse: MutableLiveData<Resource<Boolean>>) {
        try {
            motherNeonateMetaResponse.postLoading()
            val response = apiHelper.getMotherPncStaticData()
            if (response.isSuccessful) {
                response.body()?.entity?.let { data ->
                    roomHelper.apply {
                        roomHelper.deleteDiagnosisList(
                            MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name
                        )
                        roomHelper.saveDiagnosisList(data.diseaseCategories)
                        deleteExaminationsComplaintsForAnc( MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name)
                        insertExaminationsComplaint(
                            generateChipItemByType(
                                data.presentingComplaints,
                                data.systemicExaminations,
                                data.patientStatus
                            )
                        )
                        SecuredPreference.putBoolean(
                            SecuredPreference.EnvironmentKey.IS_MOTHER_LOADED_PNC.name,
                            true
                        )
                    }
                    motherNeonateMetaResponse.postSuccess()
                } ?: run {
                    motherNeonateMetaResponse.postError()
                }
            } else {
                motherNeonateMetaResponse.postError()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            motherNeonateMetaResponse.postError()
        }
    }

    private fun generateChipItemByType(
        presentingComplaints: List<MedicalReviewMetaItems>,
        obstetricExaminations: List<MedicalReviewMetaItems>,
        patientStatus: List<MedicalReviewMetaItems>

    ): List<MedicalReviewMetaItems> {
        val chipItemList = mutableListOf<MedicalReviewMetaItems>()
        chipItemList.addAll(presentingComplaints.map {
            it.apply {
                type =  MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name
                category = MedicalReviewTypeEnums.PresentingComplaints.name
            }
        })
        chipItemList.addAll(obstetricExaminations.map {
            it.apply {
                type =  MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name
                category = MedicalReviewTypeEnums.SystemicExaminations.name
            }
        })
        chipItemList.addAll(patientStatus.map {
            it.apply {
                type =  MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name
                category = MedicalReviewTypeEnums.patient_status.name
            }
        })
        return chipItemList
    }

    suspend fun getNeonatePncStaticData(motherNeonateMetaResponse: MutableLiveData<Resource<Boolean>>) {
        try {
            motherNeonateMetaResponse.postLoading()
            val response = apiHelper.getNeonatePncStaticData()
            if (response.isSuccessful) {
                response.body()?.entity?.let { data ->
                    roomHelper.apply {
                        deleteExaminationsComplaintsForAnc( MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name)
                        insertExaminationsComplaint(
                            generateNeonateChipItemByType(
                                data.presentingComplaints,
                                data.obstetricExaminations,
                                data.patientStatus
                            )
                        )
                        SecuredPreference.putBoolean(
                            SecuredPreference.EnvironmentKey.IS_NEONATE_LOADED_PNC.name,
                            true
                        )
                    }
                    motherNeonateMetaResponse.postSuccess()
                } ?: run {
                    motherNeonateMetaResponse.postError()
                }
            } else {
                motherNeonateMetaResponse.postError()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            motherNeonateMetaResponse.postError()
        }
    }


    private fun generateNeonateChipItemByType(
        presentingComplaints: List<MedicalReviewMetaItems>,
        obstetricExaminations: List<MedicalReviewMetaItems>,
        patientStatus: List<MedicalReviewMetaItems>
    ): List<MedicalReviewMetaItems> {
        val chipItemList = mutableListOf<MedicalReviewMetaItems>()
        chipItemList.addAll(presentingComplaints.map {
            it.apply {
                type =  MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name
                category = MedicalReviewTypeEnums.PresentingComplaints.name
            }
        })
        chipItemList.addAll(obstetricExaminations.map {
            it.apply {
                type =  MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name
                category = MedicalReviewTypeEnums.ObstetricExaminations.name
            }
        })
        chipItemList.addAll(patientStatus.map {
            it.apply {
                type =  MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name
                category = MedicalReviewTypeEnums.patient_status.name
            }
        })
        return chipItemList
    }

    fun getExaminationsComplaintsForPnc(
        category: String,
        type: String
    ): LiveData<List<MedicalReviewMetaItems>> {
        return roomHelper.getExaminationsComplaintsForPnc(category, type)
    }

    suspend fun getPncSummaryDetails(motherNeonatePncSummaryRequest: MotherNeonatePncSummaryRequest): Resource<MotherNeonatePncSummaryResponse> {
        return try {
            val response = apiHelper.getPncSummaryDetails(motherNeonatePncSummaryRequest)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.status == true) {
                    Resource(state = ResourceState.SUCCESS, response.body()?.entity)
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

    suspend fun saveMotherNeonatePncData(
        motherNeonatePncRequest: MotherNeonatePncRequest,
    ): Resource<PncSubmitResponse> {
        return try {
            val response = apiHelper.saveMotherNeonatePnc(motherNeonatePncRequest)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun summaryCreatePncData(
        summaryCreateRequest: MedicalReviewSummarySubmitRequest
    ): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.createSummarySubmit(summaryCreateRequest)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }


}