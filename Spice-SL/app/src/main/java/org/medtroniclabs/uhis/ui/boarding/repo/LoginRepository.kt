package org.medtroniclabs.uhis.ui.boarding.repo

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.EncryptionUtil
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.ErrorResponse
import org.medtroniclabs.uhis.data.LoginResponse
import org.medtroniclabs.uhis.ncd.data.DeviceDetails
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private var apiHelper: ApiHelper,
) {
    suspend fun doLogin(
        username: String,
        password: String,
        deviceDetails: DeviceDetails,
    ): Resource<LoginResponse> =
        try {
            val securePassword = EncryptionUtil.getSecurePassword(password)
            val builder = MultipartBody.Builder()
            builder.setType(MultipartBody.FORM)
            builder.addFormDataPart(DefinedParams.USERNAME, username)
            builder.addFormDataPart(DefinedParams.PASSWORD, securePassword)
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
                                        it.isTermsAndConditionsAccepted == true,
                                    )
                                    it.culture?.let { culture ->
                                        val isEnabled =
                                            CommonUtils.checkIfTranslationEnabled(culture.name)
                                        SecuredPreference.setUserPreference(
                                            culture.id,
                                            culture.name,
                                            isEnabled,
                                        )
                                    }
                                    saveUserNameAndPassword(username, securePassword)
                                }
                                Resource(state = ResourceState.SUCCESS, data = response.body())
                            } else {
                                Resource(state = ResourceState.ERROR)
                            }
                        } else {
                            Resource(
                                state = ResourceState.ERROR,
                                message = getErrorMessage(response.errorBody()),
                            )
                        }
                    } else {
                        Resource(
                            state = ResourceState.ERROR,
                            message = appVersionResponse?.message,
                            optionalData = true, // Show update app alert dialog
                        )
                    }
                } else {
                    Resource(
                        state = ResourceState.ERROR,
                        message = getErrorMessage(response.errorBody()),
                    )
                }
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = getErrorMessage(response.errorBody()),
                )
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    private fun getErrorMessage(errorBody: ResponseBody?): String? {
        if (errorBody == null) {
            return null
        }
        return try {
            val errorResponse = Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
            return errorResponse.message
        } catch (e: Exception) {
            null
        }
    }

    private fun saveUserNameAndPassword(
        userName: String,
        password: String,
    ) {
        SecuredPreference.putString(
            SecuredPreference.EnvironmentKey.PASSWORD.name,
            password,
        )
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISLOGGEDIN.name,
            true,
        )
        FirebaseCrashlytics.getInstance().setUserId(userName)
    }

    private fun saveTokenInformation(headers: Map<String, List<String>>) {
        if (headers.containsKey(DefinedParams.AUTHORIZATION) &&
            (
                headers[DefinedParams.AUTHORIZATION]?.size
                    ?: 0
            ) > 0
        ) {
            headers[DefinedParams.AUTHORIZATION]?.get(0)?.let { token ->
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.TOKEN.name,
                    token,
                )
            }
        }

        if (headers.containsKey(DefinedParams.TENANT_ID) &&
            (
                headers[DefinedParams.TENANT_ID]?.size
                    ?: 0
            ) > 0
        ) {
            headers[DefinedParams.TENANT_ID]?.get(0)?.let { token ->
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.TENANT_ID.name,
                    token,
                )
            }
        }
    }
}
