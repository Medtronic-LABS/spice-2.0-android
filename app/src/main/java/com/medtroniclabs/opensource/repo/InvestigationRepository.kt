package com.medtroniclabs.opensource.repo

import com.google.gson.Gson
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.ErrorResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.LabTestCreateRequest
import com.medtroniclabs.opensource.model.LabTestListRequest
import com.medtroniclabs.opensource.model.LabTestListResponse
import com.medtroniclabs.opensource.model.RemoveLabTestRequest
import com.medtroniclabs.opensource.model.medicalreview.SearchLabTestResponse
import com.medtroniclabs.opensource.model.medicalreview.SearchRequestLabTest
import com.medtroniclabs.opensource.ncd.data.PredictionRequest
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import okhttp3.ResponseBody
import javax.inject.Inject

class InvestigationRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {

    suspend fun searchInvestigationByName(request: SearchRequestLabTest): Resource<ArrayList<SearchLabTestResponse>> {
        return try {
            val response = apiHelper.searchLabTestByName(request)
            if (response.isSuccessful) {
                response.body()?.entityList?.let {
                    Resource(ResourceState.SUCCESS, response.body()?.entityList)
                } ?: kotlin.run {
                    Resource(ResourceState.ERROR)
                }
            } else {
                Resource(ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(ResourceState.ERROR)
        }
    }


    suspend fun createLabTest(request: LabTestCreateRequest): Resource<Map<String, Any>> {
        return try {
            val response = apiHelper.createLabTest(request)
            if (response.isSuccessful) {
                response.body()?.entity?.let {
                    Resource(ResourceState.SUCCESS, response.body()?.entity)
                } ?: kotlin.run {
                    Resource(ResourceState.ERROR)
                }
            } else {
                Resource(ResourceState.ERROR, message = getErrorMessage(response.errorBody()))
            }
        } catch (e: Exception) {
            Resource(ResourceState.ERROR)
        }
    }

    suspend fun getLabTestList(request: LabTestListRequest): Resource<ArrayList<LabTestListResponse>> {
        return try {
            val response = apiHelper.getLabTestList(request)
            if (response.isSuccessful) {
                response.body()?.entityList?.let {
                    Resource(ResourceState.SUCCESS, response.body()?.entityList)
                } ?: kotlin.run {
                    Resource(ResourceState.ERROR)
                }
            } else {
                Resource(ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(ResourceState.ERROR)
        }
    }

    suspend fun removeLabTest(request: RemoveLabTestRequest): Resource<Map<String, Any>> {
        return try {
            val response = apiHelper.removeLabTest(request)
            if (response.isSuccessful) {
                response.body()?.entity?.let {
                    Resource(ResourceState.SUCCESS, response.body()?.entity)
                } ?: kotlin.run {
                    Resource(ResourceState.ERROR)
                }
            } else {
                Resource(ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(ResourceState.ERROR)
        }
    }

    suspend fun getLabTestNudgeList(predictionRequest: PredictionRequest): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.getLabTestNudgeList(predictionRequest)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    private fun getErrorMessage(errorBody: ResponseBody?): String? {
        if (errorBody == null)
            return null
        return try {
            val errorResponse = Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
            return errorResponse.message
        } catch (e: Exception) {
            null
        }
    }

    suspend fun markAsReviewed(request: HashMap<String, Any>): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = apiHelper.markAsReviewed(request)
            if (response.isSuccessful)
                Resource(ResourceState.SUCCESS, response.body())
            else
                Resource(ResourceState.ERROR)
        } catch (e: Exception) {
            Resource(ResourceState.ERROR)
        }
    }
}