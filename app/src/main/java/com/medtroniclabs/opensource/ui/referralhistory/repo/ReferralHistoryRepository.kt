package com.medtroniclabs.opensource.ui.referralhistory.repo

import com.medtroniclabs.opensource.data.PncChildMedicalReview
import com.medtroniclabs.opensource.data.history.MedicalReviewHistory
import com.medtroniclabs.opensource.data.history.HistoryEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.model.ReferralData
import com.medtroniclabs.opensource.model.ReferralDetailRequest
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class ReferralHistoryRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {
    suspend fun getReferralTicket(
        request: ReferralDetailRequest
    ): Resource<ReferralData> {
        return try {
            val response = apiHelper.getReferralsDetails(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getPrescription(
        request: ReferralDetailRequest
    ): Resource<HistoryEntity> {
        return try {
            val response = apiHelper.getPrescription(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getInvestigation(
        request: ReferralDetailRequest
    ): Resource<HistoryEntity> {
        return try {
            val response = apiHelper.getInvestigation(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getMedicalReviewHistory(
        request: ReferralDetailRequest
    ): Resource<MedicalReviewHistory> {
        return try {
            val response = apiHelper.getMedicalReviewHistory(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
    suspend fun getMedicalReviewHistoryPNC(
        request: ReferralDetailRequest
    ): Resource<PncChildMedicalReview> {
        return try {
            val response = apiHelper.getMedicalReviewHistoryPNC(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}