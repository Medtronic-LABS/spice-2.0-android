package com.medtroniclabs.opensource.ncd.registration.repo

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.common.AppConstants
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.CountryModel
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.data.model.RegistrationResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import okhttp3.RequestBody
import javax.inject.Inject

class RegistrationRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {
    fun fetchConsentForm(formType: String): LiveData<String> {
        return roomHelper.getConsent(formType)
    }

    fun getCountries(
        tag: String
    ): Resource<LocalSpinnerResponse> {
        return try {
            val countryList = ArrayList<CountryModel>()
            SecuredPreference.getUserDetails()?.country?.let { country ->
                countryList.add(country)
                Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, countryList))
            } ?: run {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getCounties(
        tag: String,
        selectedParent: Long
    ): Resource<LocalSpinnerResponse> {
        return try {
            val response = roomHelper.getDistricts(selectedParent)
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getSubCounties(
        tag: String,
        selectedParent: Long
    ): Resource<LocalSpinnerResponse> {
        return try {
            val response = roomHelper.getChiefDoms(selectedParent)
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAllVillages(
        tag: String,
        selectedParent: Long
    ): Resource<LocalSpinnerResponse> {
        return try {
            val response = roomHelper.getVillagesByChiefDom(selectedParent)
            Resource(
                state = ResourceState.SUCCESS,
                LocalSpinnerResponse(tag, CommonUtils.getModifiedResponse(response))
            )
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAllPrograms(
        tag: String
    ): Resource<LocalSpinnerResponse> {
        return try {
            val response = roomHelper.getPrograms().filter { it.healthFacilityIds.contains(SecuredPreference.getOrganizationId()) }
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun registerPatient(hashMap: RequestBody): Resource<RegistrationResponse> {
        return try {
            val response = apiHelper.registerPatient(hashMap)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun validatePatient(
        requestMap: HashMap<String, Any>,
        patientCreateReq: Pair<HashMap<String, Any>, List<FormLayout?>?>
    ): Resource<Pair<HashMap<String, Any>, List<FormLayout?>?>> {
        return try {

            val response = apiHelper.validatePatient(CommonUtils.validationRequest(requestMap))

            if (response.isSuccessful && response.body()?.status == true) {
                //Not a duplicate patient
                Resource(state = ResourceState.SUCCESS, data = patientCreateReq)
            } else if (response.code() == AppConstants.CONFLICT_ERROR_CODE) {
                //Duplicate patient found
                val duplicateEntity = StringConverter.getDuplicatePatientMap(response.errorBody())

                if (duplicateEntity.isNullOrEmpty())
                    Resource(state = ResourceState.ERROR)
                else
                    Resource(
                        state = ResourceState.ERROR,
                        data = Pair(duplicateEntity, null)
                    )
            } else {
                //Error returned on Patient Validate API
                Resource(
                    state = ResourceState.ERROR,
                    message = CommonUtils.getErrorMessage(response.errorBody())
                )
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}