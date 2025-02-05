package com.medtroniclabs.opensource.ui.medicalreview.labTechnician

import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.model.LabTestCreateRequest
import com.medtroniclabs.opensource.model.LabTestListRequest
import com.medtroniclabs.opensource.model.LabTestListResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class NCDLabTestRepository @Inject constructor(
    private val apiHelper: ApiHelper
) {

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

    suspend fun updateLabTest(request: LabTestCreateRequest): Resource<APIResponse<Map<String, Any>>> {
        return try {
            val response = apiHelper.updateLabTest(request)
            if (response.isSuccessful)
                Resource(ResourceState.SUCCESS, response.body())
            else
                Resource(ResourceState.ERROR)
        } catch (e: Exception) {
            Resource(ResourceState.ERROR)
        }
    }

}