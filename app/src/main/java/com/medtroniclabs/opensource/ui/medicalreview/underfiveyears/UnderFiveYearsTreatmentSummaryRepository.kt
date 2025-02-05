package com.medtroniclabs.opensource.ui.medicalreview.underfiveyears

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.model.medicalreview.SummaryDetails
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class UnderFiveYearsTreatmentSummaryRepository @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    suspend fun getUnderFiveYearsSummaryDetails(request: CreateUnderTwoMonthsResponse): Resource<SummaryDetails> {
        return try {
            val response = apiHelper.getUnderFiveYearsSummaryDetails(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                val errorMessage = StringConverter.getErrorMessage(response.errorBody())
                Resource(state = ResourceState.ERROR, message = errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    fun getExaminationsComplaints(
        category: String,
        type: String
    ): LiveData<List<MedicalReviewMetaItems>> {
        return roomHelper.getExaminationsComplaintsForAnc(category, type)
    }

}