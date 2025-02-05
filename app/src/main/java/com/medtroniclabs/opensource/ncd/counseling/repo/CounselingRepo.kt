package com.medtroniclabs.opensource.ncd.counseling.repo

import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.data.AssessmentResultModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import javax.inject.Inject

class CounselingRepo @Inject constructor(
    private var apiHelper: ApiHelper, private var roomHelper: RoomHelper
) {
    fun getLifestyleAssessments(type: String? = null, category: String) =
        roomHelper.getComorbidities(type, category)

    suspend fun createAssessment(request: NCDCounselingModel, lifestyle: Boolean): Resource<APIResponse<NCDCounselingModel>> {
        return try {
            val response = if (lifestyle) apiHelper.createLifestyle(request) else apiHelper.createPsychological(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun updateAssessment(request: AssessmentResultModel, lifestyle: Boolean): Resource<APIResponse<HashMap<String, Any>>> {
        return try {
            val response = if (lifestyle) apiHelper.updateLifestyle(request) else apiHelper.updatePsychological(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAssessmentList(request: NCDCounselingModel, lifestyle: Boolean): Resource<APIResponse<ArrayList<NCDCounselingModel>>> {
        return try {
            val response = if (lifestyle) apiHelper.getLifestyleList(request) else apiHelper.getPsychological(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body())
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun removeAssessment(request: NCDCounselingModel, lifestyle: Boolean): Resource<APIResponse<NCDCounselingModel>> {
        return try {
            val response = if (lifestyle) apiHelper.removeLifestyle(request) else apiHelper.removePsychological(request)
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