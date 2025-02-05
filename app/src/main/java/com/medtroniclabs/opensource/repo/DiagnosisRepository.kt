package com.medtroniclabs.opensource.repo

import androidx.lifecycle.MutableLiveData
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.data.DiagnosisDiseaseModel
import com.medtroniclabs.opensource.data.DiagnosisSaveUpdateRequest
import com.medtroniclabs.opensource.data.DiseaseCategoryItems
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class DiagnosisRepository @Inject constructor(
    private var roomHelper: RoomHelper,
    private var apiHelper: ApiHelper
) {

    suspend fun getDiagnosisList(
        diagnosisList: MutableLiveData<Resource<List<DiseaseCategoryItems>>>,
        diagnosisType: String
    ){
        try {
            diagnosisList.postLoading()
            val response = roomHelper.getDiagnosisList(diagnosisType)
            diagnosisList.postSuccess(response)
        } catch (e: Exception) {
            diagnosisList.postError()
        }
    }

    suspend fun saveUpdateDiagnosis(
        request: DiagnosisSaveUpdateRequest
    ): Resource<ArrayList<DiagnosisDiseaseModel>> {
        return try {
            val response = apiHelper.saveUpdateDiagnosis(request)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.status == true) {
                    Resource(state = ResourceState.SUCCESS, data = res.entity)
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            } else{
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getDiagnosisDetails(
        request: CreateUnderTwoMonthsResponse
    ): Resource<ArrayList<DiagnosisDiseaseModel>> {
        return try {
            val response = apiHelper.getDiagnosisDetails(request)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.status == true) {
                    Resource(state = ResourceState.SUCCESS, data = res.entity)
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            } else{
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

}