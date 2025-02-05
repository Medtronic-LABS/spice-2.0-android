package com.medtroniclabs.opensource.ui.boarding.repo

import com.google.gson.Gson
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.EncryptionUtil
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.ErrorResponse
import com.medtroniclabs.opensource.data.LoginResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.data.DeviceDetails
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {

    suspend fun doLogin(username: String, password: String, deviceDetails: DeviceDetails): Resource<LoginResponse> {
        return try {
            val securePassword = EncryptionUtil.getSecurePassword(password)
            val builder = MultipartBody.Builder()
            builder.setType(MultipartBody.FORM)
            builder.addFormDataPart(DefinedParams.Username, username)
            builder.addFormDataPart(DefinedParams.Password, securePassword)
            val response = apiHelper.doLogin(builder.build())
            if (response.isSuccessful) {
                val headers = response.headers().toMultimap()
                saveTokenInformation(headers)
                val versionCheckResponse = apiHelper.checkAppVersion()
                if (versionCheckResponse.isSuccessful) {
                    val appVersionResponse = versionCheckResponse.body()
                    if (appVersionResponse?.entity == true) {
                        val deviceDetailsAPI = apiHelper.updateDeviceDetails(deviceDetails)
                        if (deviceDetailsAPI.isSuccessful) {
                            val deviceDetailsResponse = deviceDetailsAPI.body()
                            if (deviceDetailsResponse?.status == true) {
                                val loginResponseModel = response.body()
                                loginResponseModel?.let {
                                    SecuredPreference.putUserDetails(it)
                                    SecuredPreference.putBoolean(
                                        SecuredPreference.EnvironmentKey.IS_TERMS_AND_CONDITIONS_APPROVED.name,
                                        it.isTermsAndConditionsAccepted == true
                                    )
                                    it.culture?.let { culture ->
                                        val isEnabled =
                                            CommonUtils.checkIfTranslationEnabled(culture.name)
                                        SecuredPreference.setUserPreference(
                                            culture.id,
                                            culture.name,
                                            isEnabled
                                        )
                                    }
                                    saveUserNameAndPassword(username, securePassword)
                                }
                                Resource(state = ResourceState.SUCCESS, data = response.body())
                            } else
                                Resource(state = ResourceState.ERROR)
                        } else
                            Resource(
                                state = ResourceState.ERROR,
                                message = getErrorMessage(response.errorBody())
                            )
                    } else
                        Resource(
                            state = ResourceState.ERROR,
                            message = appVersionResponse?.message,
                            optionalData = true //Show update app alert dialog
                        )
                } else
                    Resource(
                        state = ResourceState.ERROR,
                        message = getErrorMessage(response.errorBody())
                    )
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

    private fun saveUserNameAndPassword(userName: String, password: String) {
        SecuredPreference.putString(
            SecuredPreference.EnvironmentKey.PASSWORD.name,
            password
        )
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISLOGGEDIN.name,
            true
        )
    }

    private fun saveTokenInformation(headers: Map<String, List<String>>) {
        if (headers.containsKey(DefinedParams.Authorization)
            && (headers[DefinedParams.Authorization]?.size
                ?: 0) > 0
        ) {
            headers[DefinedParams.Authorization]?.get(0)?.let { token ->
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.TOKEN.name,
                    token
                )
            }
        }

        if (headers.containsKey(DefinedParams.TenantId)
            && (headers[DefinedParams.TenantId]?.size
                ?: 0) > 0
        ) {
            headers[DefinedParams.TenantId]?.get(0)?.let { token ->
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.TENANT_ID.name,
                    token
                )
            }
        }
    }

}