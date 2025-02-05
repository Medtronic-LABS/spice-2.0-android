package com.medtroniclabs.opensource.ncd.assessment.repo

import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class GlucoseRepo @Inject constructor(private val apiHelper: ApiHelper) {
    suspend fun glucoseLogCreate(hashMap: HashMap<String, Any>): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = apiHelper.glucoseLogCreate(hashMap)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun glucoseLogList(request: BPBGListModel): Resource<BPBGListModel> {
        return try {
            val response = apiHelper.glucoseLogList(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun glucoseLogCreateForNurse(hashMap: HashMap<String, Any>): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = apiHelper.glucoseLogCreateForNurse(hashMap)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}