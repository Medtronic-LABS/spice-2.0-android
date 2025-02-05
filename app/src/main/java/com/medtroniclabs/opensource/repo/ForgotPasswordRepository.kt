package com.medtroniclabs.opensource.repo

import com.google.gson.Gson
import com.medtroniclabs.opensource.common.AppConstants.CLIENT_CONSTANT
import com.medtroniclabs.opensource.common.EncryptionUtil
import com.medtroniclabs.opensource.data.ErrorResponse
import com.medtroniclabs.opensource.data.model.RequestChangePassword
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import okhttp3.ResponseBody
import javax.inject.Inject

class ForgotPasswordRepository @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {

    suspend fun forgotPassword(email: String): Resource<Boolean> {
        return try {
            val response = apiHelper.forgotPassword(email, CLIENT_CONSTANT)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = true)
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = getErrorMessage(response.errorBody())
                )
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun verifyToken(token: String): Resource<Boolean> {
        return try {
            val response = apiHelper.verifyToken(token)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = true)
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = getErrorMessage(response.errorBody())
                )
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun resetPassword(
        token: String,
        password: String
    ): Resource<Boolean> {
        return try {
            val securedPassword = EncryptionUtil.getSecurePassword(password)
            val response =
                apiHelper.resetPassword(token, RequestChangePassword(securedPassword))
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = true)
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = getErrorMessage(response.errorBody())
                )
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }


    private fun getErrorMessage(errorBody: ResponseBody?): String? {
        if (errorBody == null)
            return null
        return try {
            val errorResponse = Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
            return errorResponse.message
        } catch (e: Exception) {
            null
        }
    }
}