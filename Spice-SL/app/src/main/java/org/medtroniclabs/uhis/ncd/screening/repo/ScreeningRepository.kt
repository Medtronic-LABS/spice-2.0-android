package org.medtroniclabs.uhis.ncd.screening.repo

import androidx.lifecycle.LiveData
import okhttp3.RequestBody
import org.medtroniclabs.uhis.common.AppConstants
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.db.entity.ScreeningEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.ncd.data.TermsAndConditionsModel
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import javax.inject.Inject

class ScreeningRepository @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper,
) {
    fun getUserHealthFacilityEntity(): LiveData<List<HealthFacilityEntity>> = roomHelper.getSites()

    fun fetchConsentForm(formType: String): LiveData<String> = roomHelper.getConsent(formType)

    fun getMentalQuestion(type: String): LiveData<MentalHealthEntity?> = roomHelper.getMentalQuestion(type)

    suspend fun getVillagesByChiefDom(
        tag: String,
        selectedParent: Long,
    ): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getVillagesByChiefDom(selectedParent)
            Resource(
                state = ResourceState.SUCCESS,
                LocalSpinnerResponse(tag, CommonUtils.getModifiedResponse(response)),
            )
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun savePatientScreeningInformation(screeningEntity: ScreeningEntity) = roomHelper.savePatientScreeningInformation(screeningEntity)

    fun getScreenedPatientCount(
        startDate: Long,
        endDate: Long,
        userId: String,
    ) = roomHelper.getScreenedPatientCount(startDate, endDate, userId)

    fun getScreenedPatientReferredCount(
        startDate: Long,
        endDate: Long,
        userId: String,
        isReferred: Boolean,
    ) = roomHelper.getScreenedPatientReferredCount(startDate, endDate, userId, isReferred)

    suspend fun getAllScreeningRecords(uploadStatus: Boolean): List<ScreeningEntity>? = roomHelper.getAllScreeningRecords(uploadStatus)

    suspend fun createScreeningLog(createPatientRequest: RequestBody) = apiHelper.createScreening(createPatientRequest)

    suspend fun deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds: Long) = roomHelper.deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds)

    suspend fun updateScreeningRecordById(
        id: Long,
        uploadStatus: Boolean,
    ) = roomHelper.updateScreeningRecordById(id, uploadStatus)

    suspend fun validatePatient(
        requestMap: HashMap<String, Any>,
        patientCreateReq: Pair<HashMap<String, Any>, List<FormLayout?>?>,
    ): Resource<Pair<HashMap<String, Any>, List<FormLayout?>?>> =
        try {
            val response = apiHelper.validatePatient(CommonUtils.validationRequest(requestMap))

            if (response.isSuccessful && response.body()?.status == true) {
                // Not a duplicate patient
                Resource(state = ResourceState.SUCCESS, data = patientCreateReq)
            } else if (response.code() == AppConstants.CONFLICT_ERROR_CODE) {
                // Duplicate patient found
                val duplicateEntity = StringConverter.getDuplicatePatientMap(response.errorBody())

                if (duplicateEntity.isNullOrEmpty()) {
                    Resource(state = ResourceState.ERROR, data = patientCreateReq)
                } else {
                    Resource(
                        state = ResourceState.ERROR,
                        data = Pair(duplicateEntity, null),
                        optionalData = true,
                    )
                }
            } else {
                Resource(
                    state = ResourceState.ERROR,
                    message = CommonUtils.getErrorMessage(response.errorBody()),
                    data = patientCreateReq,
                )
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun updateTermsAndConditionsStatus(request: TermsAndConditionsModel): Resource<TermsAndConditionsModel> =
        try {
            val response = apiHelper.updateTermsAndConditionsStatus(request)
            if (response.isSuccessful && response.body()?.status == true) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getScreeningRecordById(id: Long): ScreeningEntity = roomHelper.getScreeningRecordById(id)

    suspend fun getAccountSiteList(userId: Long) = roomHelper.getAccountSiteList(userId)

    suspend fun getAccountSiteListByLevel(
        userId: Long,
        level: String,
    ) = roomHelper.getAccountSiteListByLevel(userId, level)
}
