package com.medtroniclabs.opensource.ui.mypatients.repo

import com.google.gson.Gson
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.PatientDetailRequest
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.data.PatientVisitRequest
import com.medtroniclabs.opensource.ncd.data.PatientVisitResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import javax.inject.Inject

class PatientRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {

    suspend fun getUserVillages(): List<VillageEntity> {
        return roomHelper.getUserVillages()
    }

    suspend fun getMenuForClinicalWorkflows(): List<ClinicalWorkflowEntity> {
        return roomHelper.getMenuForClinicalWorkflows()
    }

    suspend fun getPatients(
        request: PatientDetailRequest
    ): Resource<PatientListRespModel> {
        return try {
            val response = apiHelper.getPatient(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
    suspend fun getPatientBasedOnId(id: String): Resource<PatientListRespModel> {
        return try {
            val response = roomHelper.getPatientBasedOnId(id)
            Gson().fromJson(response.patientDetails, PatientListRespModel::class.java)
                ?.let { data ->
                    Resource(state = ResourceState.SUCCESS, data = data)
                } ?: Resource(state = ResourceState.ERROR)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun createPatientVisit(request: PatientVisitRequest): Resource<PatientVisitResponse> {
        return try {
            val response = apiHelper.createPatientVisit(request)
            if (response.isSuccessful) {
                response.body()?.entity?.let {
                    Resource(state = ResourceState.SUCCESS, it)
                } ?: Resource(state = ResourceState.ERROR)

            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun ncdPatientRemove(request: NCDPatientRemoveRequest): Resource<Boolean> {
        return try {
            val response = apiHelper.ncdPatientRemove(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }
}