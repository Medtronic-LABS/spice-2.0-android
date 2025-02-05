package com.medtroniclabs.opensource.repo

import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class ExaminationComplaintsRepository @Inject constructor(
    private var roomHelper: RoomHelper
) {
    suspend fun getComplaintsListByType(type: String): Resource<List<MedicalReviewMetaItems>> {
     return try {
            val response = roomHelper.getExaminationsComplaintByType(type)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}