package org.medtroniclabs.uhis.repo

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.db.entity.AssessmentEntity
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.MedicalComplianceEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBContactTracing
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBScreening
import org.medtroniclabs.uhis.ui.assessment.AssessmentNCDEntity
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import java.util.Locale
import javax.inject.Inject

class AssessmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private var roomHelper: RoomHelper,
    private var apiHelper: ApiHelper,
) {
    suspend fun saveAssessment(
        resultData: String,
        memberDetails: AssessmentMemberDetails,
        menuId: String?,
        referralResult: Pair<String?, ArrayList<String>>?,
        otherDetails: HashMap<String, Any>? = null,
        followUpId: Long? = null,
        status: ArrayList<String>? = null,
    ): Resource<AssessmentEntity> {
        val latitude = SecuredPreference.getDouble(
            SecuredPreference.EnvironmentKey.CURRENT_LATITUDE.name,
            0.0,
        )
        val longitude = SecuredPreference.getDouble(
            SecuredPreference.EnvironmentKey.CURRENT_LONGITUDE.name,
            0.0,
        )
        return try {
            val assessmentEntity = menuId?.let { menu ->
                AssessmentEntity(
                    householdMemberLocalId = memberDetails.id,
                    memberId = memberDetails.memberId,
                    householdId = memberDetails.householdId,
                    patientId = memberDetails.patientId,
                    villageId = memberDetails.subVillageId,
                    assessmentType = menu.uppercase(Locale.getDefault()),
                    assessmentDetails = resultData,
                    otherDetails = if (otherDetails != null) {
                        StringConverter.convertGivenMapToString(
                            otherDetails,
                        )
                    } else {
                        null
                    },
                    isReferred = isReferred(referralResult?.first),
                    referralStatus = getReferralStatus(referralResult?.first),
                    referredReason = referralResult?.second,
                    followUpId = followUpId,
                    status = status,
                    latitude = latitude,
                    longitude = longitude,
                )
            }
            assessmentEntity?.let {
                val id = roomHelper.saveAssessment(assessmentEntity)
                assessmentEntity.id = id
            }
            Resource(state = ResourceState.SUCCESS, data = assessmentEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun saveCallResult(assessmentEntity: AssessmentEntity): Resource<AssessmentEntity> {
        val id = roomHelper.saveAssessment(assessmentEntity)
        assessmentEntity.id = id
        return Resource(ResourceState.SUCCESS, assessmentEntity)
    }

    suspend fun getAssessmentById(assessmentId: Long): Resource<AssessmentEntity> {
        val data = roomHelper.getAssessment(assessmentId)
        return Resource(ResourceState.SUCCESS, data)
    }

    private fun getReferralStatus(result: String?): ReferralStatus =
        when (result) {
            ReferralStatus.Referred.name -> {
                ReferralStatus.Referred
            }

            ReferralStatus.OnTreatment.name -> {
                ReferralStatus.OnTreatment
            }

            else -> {
                ReferralStatus.Recovered
            }
        }

    private fun isReferred(value: String?): Boolean = value == ReferralStatus.Referred.name

    suspend fun updateOtherAssessmentDetails(
        assessmentEntity: AssessmentEntity?,
        otherAssessmentDetails: HashMap<String, Any>,
        lastLocation: Location?,
    ): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                if (assessmentEntity != null) {
                    assessmentEntity.otherDetails =
                        StringConverter.convertGivenMapToString(otherAssessmentDetails)
                    lastLocation?.let {
                        assessmentEntity.latitude = it.latitude
                        assessmentEntity.longitude = it.longitude
                    }
                    roomHelper.updateOtherAssessmentDetails(assessmentEntity)
                }
                Resource(state = ResourceState.SUCCESS)
            } catch (_: Exception) {
                Resource(state = ResourceState.ERROR)
            }
        }

    suspend fun getSymptomListByType(type: String): List<SignsAndSymptomsEntity> =
        try {
            roomHelper.getSymptomListByType(type)
        } catch (_: Exception) {
            listOf()
        }

    suspend fun getFormData(
        formType: String,
        tbType: String? = null,
    ): Resource<FormResponse> =
        try {
            val response =
                if (formType == MenuConstants.FP_MENU_ID) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.FAMILY_PLANNING_FORM + ".json", context.assets)
                } else if (formType == MenuConstants.PREGNANT_WOMEN_PROFILE) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.PREGNANT_WOMEN_PROFILE_FORM + ".json", context.assets)
                } else if (formType == RMNCH.ANC) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.RMNCH_ANC_FORM + ".json", context.assets)
                } else if (formType == MenuConstants.PREGNANCY_OUTCOME) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.PREGNANCY_OUTCOME_FORM + ".json", context.assets)
                } else if (formType == RMNCH.PNC) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.RMNCH_PNC_FORM + ".json", context.assets)
                } else if (formType == RMNCH.ChildHoodVisit) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.RMNCH_CHILD_VISIT_FORM + ".json", context.assets)
                } else if (formType == MenuConstants.NCD_MENU_ID) {
                    CommonUtils.getStringFromAssets(MenuConstants.NCD_MENU_ID + ".json", context.assets)
                } else if (formType == MenuConstants.EYE_CARE_MENU_ID) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.EYE_CARE_FORM + ".json", context.assets)
                } else if (formType == MenuConstants.CATARACT_MENU_ID) {
                    CommonUtils.getStringFromAssets(AssessmentDefinedParams.CATARACT_FORM + ".json", context.assets)
                } else {
                    roomHelper.getFormData(formType)
                }
            val formFieldsType = object : TypeToken<FormResponse>() {}.type
            val formFields: FormResponse = Gson().fromJson(response, formFieldsType)
            if (tbType != null) {
                if (tbType == TBContactTracing) {
                    val filteredList =
                        formFields.formLayout.filter {
                            it.id == TBContactTracing ||
                                it.family == TBContactTracing ||
                                it.id == TBScreening ||
                                it.family == TBScreening
                        }
                    formFields.formLayout = filteredList
                } else {
                    val filteredList =
                        formFields.formLayout.filter { it.id == tbType || it.family == tbType }
                    formFields.formLayout = filteredList
                }
                Resource(state = ResourceState.SUCCESS, data = formFields)
            } else {
                Resource(state = ResourceState.SUCCESS, data = formFields)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getNearestHealthFacility(): Resource<ArrayList<Map<String, Any>>> {
        val healthFacilityList = roomHelper.getNearestHealthFacility()
        val dropDownList = ArrayList<Map<String, Any>>()
        for ((_, healthFacilityEntity) in healthFacilityList.withIndex()) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to healthFacilityEntity.name,
                    DefinedParams.ID to healthFacilityEntity.fhirId.toString(),
                    DefinedParams.IS_DEFAULT to healthFacilityEntity.isDefault,
                    DefinedParams.PHONE_NUMBER to (healthFacilityEntity.phoneNumber ?: ""),
                ),
            )
        }
        return Resource(state = ResourceState.SUCCESS, data = dropDownList)
    }

    suspend fun getHealthFacilityBasedOnVillageId(villageId: Long): Resource<List<HealthFacilityEntity>> =
        Resource(
            state = ResourceState.SUCCESS,
            data = roomHelper.getHealthFacilityBasedOnVillageId(villageId),
        )

    suspend fun getNearestHealthFacility(tag: String): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getNearestHealthFacility()
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getUnSyncedAssessmentCount(): Int = roomHelper.getUnSyncedAssessmentCount()

    suspend fun updatePregnancyAncDetail(
        hhmLocalId: Long,
        visitCount: Long,
        clinicalDate: String?,
    ) {
        roomHelper.updatePregnancyAncDetail(hhmLocalId, visitCount, clinicalDate)
    }

    suspend fun getChildPatientId(parentId: Long): Long? = roomHelper.getChildPatientId(parentId)

    fun getAssessmentFormData(
        formType: String,
        workFlow: String,
    ): LiveData<String> = roomHelper.getAssessmentFormData(formType, workFlow)

    suspend fun getSymptomList(): List<SignsAndSymptomsEntity> = roomHelper.getSymptomList()

    suspend fun getMedicationParentComplianceList(): List<MedicalComplianceEntity> = roomHelper.getMedicalParentComplianceList()

    suspend fun getMedicationChildComplianceList(parentId: Long): List<MedicalComplianceEntity> = roomHelper.getMedicalChildComplianceList(parentId)

    suspend fun saveAssessmentInformation(createRequest: AssessmentNCDEntity) = roomHelper.saveAssessmentInformation(createRequest)

    suspend fun getAllAssessmentRecords(uploadStatus: Boolean) = roomHelper.getAllAssessmentRecords(uploadStatus)

    suspend fun deleteAssessmentList(isUploaded: Boolean) = roomHelper.deleteAssessmentList(isUploaded)

    suspend fun updateAssessmentUploadStatus(
        id: Long,
        uploadStatus: Boolean,
    ) = roomHelper.updateAssessmentUploadStatus(id, uploadStatus)

    suspend fun getAssessmentOfflineList(uploadStatus: Boolean) = roomHelper.getAllAssessmentRecords(uploadStatus)

    suspend fun createAssessmentNCD(request: JsonObject) = apiHelper.createAssessmentNCD(request)

    suspend fun getMentalQuestion(type: String): MentalHealthEntity = roomHelper.getModelQuestions(type)

    suspend fun getSymptomListByTypes(types: List<String>): List<SignsAndSymptomsEntity> =
        try {
            roomHelper.getSymptomListByTypes(types)
        } catch (_: Exception) {
            listOf()
        }

    suspend fun saveAssessmentHistory(history: MemberAssessmentHistoryEntity): Resource<MemberAssessmentHistoryEntity> =
        try {
            history.apply {
                id = roomHelper.insertMemberAssessmentHistory(history)
            }
            Resource(state = ResourceState.SUCCESS, history)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun updateAssessmentHistory(
        history: MemberAssessmentHistoryEntity?,
        followUpDate: String,
    ) {
        try {
            history?.let {
                it.nextFollowUpDate = followUpDate
                roomHelper.updateMemberAssessmentHistory(history)
            }
        } catch (_: Exception) {
        }
    }

    suspend fun getLastServiceHistory(
        memberLocalId: Long,
        serviceTypeFor: String,
    ): MemberAssessmentHistoryEntity? = roomHelper.getLastServiceHistory(memberLocalId, serviceTypeFor)

    suspend fun getLatestMemberServiceBAfterServiceA(
        memberId: Long,
        serviceA: String,
        serviceB: String,
    ) = roomHelper.getLatestMemberServiceBAfterServiceA(memberId, serviceA, serviceB)
}
