package com.medtroniclabs.opensource.ncd.medicalreview.repo

import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModel
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModelDetails
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class NCDTreatmentPlanRepo @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    fun getFrequencies() = roomHelper.getFrequencies()

    suspend fun updateNCDTreatmentPlan(request: NCDTreatmentPlanModel): Resource<APIResponse<NCDTreatmentPlanModel>> {
        return try {
            val response = apiHelper.updateNCDTreatmentPlan(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getNCDTreatmentPlan(request: NCDTreatmentPlanModelDetails): Resource<APIResponse<NCDTreatmentPlanModelDetails>> {
        return try {
            val response = apiHelper.getNCDTreatmentPlan(request)
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