package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.repo

import com.medtroniclabs.opensource.common.StringConverter.getErrorMessage
import com.medtroniclabs.opensource.data.DispensePrescriptionRequest
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.data.DispenseUpdateRequest
import com.medtroniclabs.opensource.data.DispenseUpdateResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class NCDPharmacistRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper

) {
    suspend fun getPrescriptionDispenseList(request: DispenseUpdateRequest): Resource<ArrayList<DispensePrescriptionResponse>> {
        return try {
            val response = apiHelper.getPrescriptionDispenseList(request)
            if (response.isSuccessful) {
                response.body()?.entityList?.let {
                    Resource(state = ResourceState.SUCCESS, it)
                } ?: Resource(state = ResourceState.ERROR)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getDispensePrescriptionHistory(request: DispenseUpdateRequest): Resource<ArrayList<DispensePrescriptionResponse>> {
        return try {
            val response = apiHelper.getDispensePrescriptionHistory(request)
            if (response.isSuccessful) {
                response.body()?.entityList?.let {
                    Resource(state = ResourceState.SUCCESS, it)
                } ?: Resource(state = ResourceState.ERROR)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getShortageReasonList(type: String) = roomHelper.getNCDShortageReason(type)

    suspend fun updateDispensePrescription(request: DispensePrescriptionRequest): Resource<DispenseUpdateResponse> {
        return try {
            val response = apiHelper.updateDispensePrescription(request)
            if (response.isSuccessful) {
                response.body()?.entity?.let {
                    Resource(state = ResourceState.SUCCESS, it)
                } ?: Resource(state = ResourceState.ERROR)
            } else {
                Resource(state = ResourceState.ERROR,   message = getErrorMessage(response.errorBody()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

}