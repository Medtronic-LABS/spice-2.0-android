package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.MotherNeonateAncSummaryModel
import com.medtroniclabs.opensource.data.model.BpAndWeightRequestModel
import com.medtroniclabs.opensource.data.model.BpAndWeightResponse
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.data.model.PatientEncounterResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class MotherNeonateANCRepo @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    suspend fun getMotherNeoNateAncStaticData(motherNeonateMetaResponse: MutableLiveData<Resource<Boolean>>) {
        try {
            motherNeonateMetaResponse.postLoading()
            val response = apiHelper.getMotherNeoNateAncStaticData()
            if (response.isSuccessful) {
                response.body()?.entity?.let { data ->
                    roomHelper.apply {
                        deleteExaminationsComplaintsForAnc(MedicalReviewTypeEnums.ANC_REVIEW.name)
                        insertExaminationsComplaint(
                            generateChipItemByType(
                                data.presentingComplaints,
                                data.obstetricExaminations,
                                data.pregnancyHistories,
                                data.bloodGroup,
                                data.patientStatus
                            )
                        )
                        roomHelper.deleteDiagnosisList(MedicalReviewTypeEnums.ANC_REVIEW.name)
                        roomHelper.saveDiagnosisList(data.diseaseCategories)
                        SecuredPreference.putBoolean(
                            SecuredPreference.EnvironmentKey.IS_MOTHER_NEONATE_LOADEDANC.name,
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
        pregnancyHistories: List<MedicalReviewMetaItems>,
        bloodGroup: List<MedicalReviewMetaItems>,
        patientStatus: List<MedicalReviewMetaItems>
    ): List<MedicalReviewMetaItems> {
        val chipItemList = mutableListOf<MedicalReviewMetaItems>()
        chipItemList.addAll(presentingComplaints.map {
            it.apply {
                category = MedicalReviewTypeEnums.PresentingComplaints.name
            }
        })
        chipItemList.addAll(obstetricExaminations.map {
            it.apply {
                category = MedicalReviewTypeEnums.ObstetricExaminations.name
            }
        })
        chipItemList.addAll(pregnancyHistories.map {
            it.apply {
                category = MedicalReviewTypeEnums.PregnancyHistories.name
            }
        })
        chipItemList.addAll(bloodGroup.map {
            it.apply {
                type = MedicalReviewTypeEnums.ANC_REVIEW.name
                category = MedicalReviewTypeEnums.BloodGroup.name
            }
        })
        chipItemList.addAll(patientStatus.map {
            it.apply {
                type = MedicalReviewTypeEnums.ANC_REVIEW.name
                category = MedicalReviewTypeEnums.patient_status.name
            }
        })
        return chipItemList
    }

    fun getExaminationsComplaintsForAnc(
        category: String,
        type: String
    ): LiveData<List<MedicalReviewMetaItems>> {
        return roomHelper.getExaminationsComplaintsForAnc(category, type)
    }

    suspend fun saveMotherNeonateAnc(
        motherNeonateAncRequest: MotherNeonateAncRequest,
    ):Resource<PatientEncounterResponse> {
        return try {
            val response = apiHelper.saveMotherNeonateAnc(motherNeonateAncRequest)
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

    suspend fun fetchSummary(request: MotherNeonateAncRequest): Resource<MotherNeonateAncSummaryModel> {
        return try {
            val response = apiHelper.fetchSummary(request)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.status == true) {
                    Resource(state = ResourceState.SUCCESS,response.body()?.entity)
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

    suspend fun fetchWeight(motherNeonateAncRequest: MotherNeonateAncRequest):
            Resource<BpAndWeightResponse> {
        return try {
            val response = apiHelper.fetchWeight(motherNeonateAncRequest)
            if (response.isSuccessful) {
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

    suspend fun fetchBloodPressure(motherNeonateAncRequest: MotherNeonateAncRequest):
            Resource<BpAndWeightResponse> {
        return try {
            val response = apiHelper.fetchBloodPressure(motherNeonateAncRequest)
            if (response.isSuccessful) {
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

    suspend fun createWeight(bpAndWeightRequestModel: BpAndWeightRequestModel): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.createWeight(bpAndWeightRequestModel)
            if (response.isSuccessful) {
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

    suspend fun createBloodPressure(bpAndWeightRequestModel: BpAndWeightRequestModel): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.createBloodPressure(bpAndWeightRequestModel)
            if (response.isSuccessful) {
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

    suspend fun fetchSummaryDetails(motherNeonateAncRequest: MotherNeonateAncRequest): Resource<MotherNeonateAncSummaryModel> {
        return try {
            val response = apiHelper.fetchSummary(motherNeonateAncRequest)
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
