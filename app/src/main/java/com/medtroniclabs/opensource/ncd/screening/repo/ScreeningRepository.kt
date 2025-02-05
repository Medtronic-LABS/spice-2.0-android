package com.medtroniclabs.opensource.ncd.screening.repo

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.common.AppConstants
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.MentalHealthEntity
import com.medtroniclabs.opensource.db.entity.ScreeningEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.ncd.data.TermsAndConditionsModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import okhttp3.RequestBody
import javax.inject.Inject

class ScreeningRepository @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {
    fun getUserHealthFacilityEntity(
    ): LiveData<List<HealthFacilityEntity>> {
        return roomHelper.getSites()
    }

    fun fetchConsentForm(formType: String): LiveData<String> {
        return roomHelper.getConsent(formType)
    }

    fun getMentalQuestion(type:String) :  LiveData<MentalHealthEntity?>{
        return  roomHelper.getMentalQuestion(type)
    }

    suspend fun getVillagesByChiefDom(
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

    suspend fun savePatientScreeningInformation(screeningEntity: ScreeningEntity) =
        roomHelper.savePatientScreeningInformation(screeningEntity)

    fun getScreenedPatientCount(startDate: Long, endDate: Long, userId: String) =
        roomHelper.getScreenedPatientCount(startDate, endDate, userId)

    fun getScreenedPatientReferredCount(startDate: Long, endDate: Long, userId: String, isReferred: Boolean) =
        roomHelper.getScreenedPatientReferredCount(startDate, endDate, userId,isReferred)

    suspend fun getAllScreeningRecords(uploadStatus: Boolean): List<ScreeningEntity>? =
        roomHelper.getAllScreeningRecords(uploadStatus)

    suspend fun createScreeningLog(createPatientRequest: RequestBody) =
        apiHelper.createScreening(createPatientRequest)

    suspend fun deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds: Long) =
        roomHelper.deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds)

    suspend fun updateScreeningRecordById(id: Long, uploadStatus: Boolean) =
        roomHelper.updateScreeningRecordById(id, uploadStatus)

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
                    Resource(state = ResourceState.ERROR, data = patientCreateReq)
                else
                    Resource(
                        state = ResourceState.ERROR,
                        data = Pair(duplicateEntity, null),
                        optionalData = true
                    )
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = CommonUtils.getErrorMessage(response.errorBody()),
                    data = patientCreateReq
                )
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun updateTermsAndConditionsStatus(request: TermsAndConditionsModel): Resource<TermsAndConditionsModel> {
        return try {
            val response = apiHelper.updateTermsAndConditionsStatus(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}