package com.medtroniclabs.opensource.ncd.assessment.repo

import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class BloodPressureRepo @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    fun riskFactorListing() = roomHelper.getRiskFactorEntity()

    suspend fun createBpLog(hashMap: HashMap<String, Any>): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = apiHelper.bpLogCreate(hashMap)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun bpLogList(request: BPBGListModel): Resource<BPBGListModel> {
        return try {
            val response = apiHelper.bpLogList(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun createBpLogForNurse(hashMap: HashMap<String, Any>): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = apiHelper.bpLogCreateForNurse(hashMap)
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