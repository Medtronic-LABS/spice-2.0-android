package com.medtroniclabs.opensource.ncd.assessment.repo

import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject


class NCDPregnancyRepo @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    suspend fun ncdPregnancyCreate(requestModel: PregnancyDetailsModel): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = apiHelper.ncdPregnancyCreate(requestModel)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun ncdPregnancyDetails(requestModel: HashMap<String, Any>): Resource<PregnancyDetailsModel> {
        return try {
            val response = apiHelper.ncdPregnancyDetails(requestModel)
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
}