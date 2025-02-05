package com.medtroniclabs.opensource.ui.patientEdit

import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferValidate
import com.medtroniclabs.opensource.ncd.data.NCDTransferCreateRequest
import com.medtroniclabs.opensource.ncd.data.NCDRegionSiteModel
import com.medtroniclabs.opensource.ncd.data.RegionSiteResponse
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleModel
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleResponse
import javax.inject.Inject

class NCDPatientEditRepository @Inject constructor(
    private var apiHelper: ApiHelper
) {
    suspend fun ncdUpdatePatientDetail(request: HashMap<String, Any>): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.ncdUpdatePatientDetail(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun validatePatientTransfer(request: NCDPatientTransferValidate): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.validatePatientTransfer(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body())
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = StringConverter.getErrorMessage(response.errorBody())
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun createPatientTransfer(request: NCDTransferCreateRequest): Resource<String>? {
        return try {
            val response = apiHelper.createPatientTransfer(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.message)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun searchSite(request: NCDRegionSiteModel): Resource<ArrayList<RegionSiteResponse>> {
        return try {
            val response = apiHelper.searchSite(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.entityList)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun searchRoleUser(request: NCDSiteRoleModel): Resource<ArrayList<NCDSiteRoleResponse>> {
        return try {
            val response = apiHelper.searchRoleUser(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.entityList)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }
}