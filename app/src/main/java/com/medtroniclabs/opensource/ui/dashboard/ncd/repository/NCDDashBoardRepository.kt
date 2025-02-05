package com.medtroniclabs.opensource.ui.dashboard.ncd.repository

import com.medtroniclabs.opensource.common.StringConverter.getErrorMessage
import com.medtroniclabs.opensource.data.NCDUserDashboardRequest
import com.medtroniclabs.opensource.data.NCDUserDashboardResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class NCDDashBoardRepository @Inject constructor(
    private var apiHelper: ApiHelper,
) {

    suspend fun getUserDashboardDetails(request: NCDUserDashboardRequest): Resource<NCDUserDashboardResponse> {
        return try {
            val response = apiHelper.getUserDashboardDetails(request)
            if (response.isSuccessful) {
                response.body()?.entity?.let {
                    Resource(state = ResourceState.SUCCESS, it)
                } ?: Resource(state = ResourceState.ERROR)
            } else {
                Resource(state = ResourceState.ERROR, message = getErrorMessage(response.errorBody()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }

    }
}
