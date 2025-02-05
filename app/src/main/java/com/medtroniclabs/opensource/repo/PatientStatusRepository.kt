package com.medtroniclabs.opensource.repo

import com.medtroniclabs.opensource.common.CommonUtils.getTicketType
import com.medtroniclabs.opensource.data.PatientStatusRequest
import com.medtroniclabs.opensource.data.PatientStatusResponse
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class PatientStatusRepository @Inject constructor(
    private var apiHelper: ApiHelper
) {
    suspend fun getPatientStatusDetails(patientDetails: PatientListRespModel, menuType: String): Resource<PatientStatusResponse> {
        try {
            val request = createPatientStatusRequest(patientDetails, menuType)
            val response = request?.let { apiHelper.getPatientStatus(it) }
            if (response?.isSuccessful == true) {
                response.body()?.entity?.let {
                    return Resource(state = ResourceState.SUCCESS, it)
                }
            } else {
                return Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            return Resource(state = ResourceState.ERROR)
        }
        return Resource(state = ResourceState.ERROR)
    }

    private fun createPatientStatusRequest(patientDetails: PatientListRespModel, menuType: String): PatientStatusRequest? {
        return patientDetails.memberId?.let { patientMemberId ->
            getTicketType(menuType)?.let {workflowType ->
                PatientStatusRequest(
                    patientId=patientDetails.patientId,
                    memberId = patientMemberId,
                    type = MedicalReviewTypeEnums.medicalReview.name,
                    gender = patientDetails.gender,
                    ticketType = workflowType,
                    isPregnant = patientDetails.isPregnant ?: false,
                    encounterType = menuType,
                    provenance = ProvanceDto()
                )
            }
        }
    }
}