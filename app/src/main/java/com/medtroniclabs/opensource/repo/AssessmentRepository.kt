package com.medtroniclabs.opensource.repo

import android.location.Location
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.db.entity.AssessmentEntity
import com.medtroniclabs.opensource.db.entity.MedicalComplianceEntity
import com.medtroniclabs.opensource.db.entity.MentalHealthEntity
import com.medtroniclabs.opensource.db.entity.SignsAndSymptomsEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.model.assessment.AssessmentMemberDetails
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.assessment.AssessmentNCDEntity
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class AssessmentRepository @Inject constructor(
    private var roomHelper: RoomHelper,
    private var apiHelper: ApiHelper
) {

    suspend fun savePNCAssessment(
        second: String,
        third: String,
        memberDetail: AssessmentMemberDetails,
        referralResult: Pair<String?, ArrayList<String>>,
        lastLocation: Location?,
        otherDetails: HashMap<String, Any>?,
        childMemberIdFollowupIDAndDeathOfNewBorn: Triple<Long, Long?, Boolean?>,
        childReferralResult: Pair<String?, ArrayList<String>>
    ): Resource<Pair<AssessmentEntity, AssessmentEntity>> {
        return try {
            val motherAssessmentEntity = getAssessmentEntity(
                memberDetail,
                second,
                otherDetails,
                referralResult,
                lastLocation,
                RMNCH.pnc_mother_key,
                followUpId = childMemberIdFollowupIDAndDeathOfNewBorn.second
            )
            motherAssessmentEntity.id = roomHelper.saveAssessment(motherAssessmentEntity)
            val childMemberDetail =
                roomHelper.getAssessmentMemberDetails(childMemberIdFollowupIDAndDeathOfNewBorn.first)

            val childAssessmentEntity = getAssessmentEntity(
                childMemberDetail,
                third,
                if (childReferralResult.first.equals(ReferralStatus.Referred.name)) otherDetails else null,
                childReferralResult,
                lastLocation,
                RMNCH.pnc_neonate_key,
                followUpId = childMemberIdFollowupIDAndDeathOfNewBorn.second
            )
            childAssessmentEntity.id = roomHelper.saveAssessment(childAssessmentEntity)

            childMemberIdFollowupIDAndDeathOfNewBorn.third?.let { deathOfNewborn ->
                if (deathOfNewborn){
                    roomHelper.updateMemberDeceasedStatus(childMemberDetail.id, false)
                }
            }

            /**/
            roomHelper.updateNeonatePatientId(memberDetail.id, childMemberDetail.id)
            /**/

            Resource(
                state = ResourceState.SUCCESS,
                data = Pair(motherAssessmentEntity, childAssessmentEntity)
            )

        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    private fun getAssessmentEntity(
        memberDetail: AssessmentMemberDetails,
        second: String,
        otherDetails: HashMap<String, Any>?,
        referralResult: Pair<String?, ArrayList<String>>,
        lastLocation: Location?,
        menuId: String,
        followUpId: Long?
    ): AssessmentEntity {

        val assessmentEntity = AssessmentEntity(
            householdMemberLocalId = memberDetail.id,
            memberId = memberDetail.memberId,
            householdId = memberDetail.householdId,
            patientId = memberDetail.patientId,
            villageId = memberDetail.villageId,
            assessmentType = menuId,
            assessmentDetails = second,
            otherDetails = if (otherDetails != null) StringConverter.convertGivenMapToString(
                otherDetails
            ) else null,
            isReferred = getReferralStatus(referralResult.first),
            referralStatus = getReferralResult(referralResult.first),
            referredReason = referralResult.second,
            followUpId = followUpId,
            latitude = lastLocation?.latitude ?: 0.0,
            longitude = lastLocation?.longitude ?: 0.0
        )

        return assessmentEntity
    }


    suspend fun saveAssessment(
        resultData: String,
        memberDetails: AssessmentMemberDetails,
        menuId: String?,
        referralResult: Pair<String?, ArrayList<String>>?,
        lastLocation: Location?,
        otherDetails: HashMap<String, Any>? = null,
        followUpId: Long? = null
    ): Resource<AssessmentEntity> {
        return try {
            val assessmentEntity = menuId?.let { menu ->
                AssessmentEntity(
                    householdMemberLocalId = memberDetails.id,
                    memberId = memberDetails.memberId,
                    householdId = memberDetails.householdId,
                    patientId = memberDetails.patientId,
                    villageId = memberDetails.villageId,
                    assessmentType = menu.uppercase(Locale.getDefault()),
                    assessmentDetails = resultData,
                    otherDetails = if (otherDetails != null) StringConverter.convertGivenMapToString(
                        otherDetails
                    ) else null,
                    isReferred = getReferralStatus(referralResult?.first),
                    referralStatus = getReferralResult(referralResult?.first),
                    referredReason = referralResult?.second,
                    followUpId = followUpId,
                    latitude = lastLocation?.latitude ?: 0.0,
                    longitude = lastLocation?.longitude ?: 0.0
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

    private fun getReferralResult(result: String?): ReferralStatus {
        return when (result) {
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
    }

    private fun getReferralStatus(value: String?): Boolean {
        return value != null && value == ReferralStatus.Referred.name
    }

    suspend fun updateOtherAssessmentDetails(
        assessmentEntity: AssessmentEntity?,
        otherAssessmentDetails: HashMap<String, Any>,
        lastLocation: Location?
    ): Resource<String> {
        return withContext(Dispatchers.IO) {
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
            } catch (e: Exception) {
                Resource(state = ResourceState.ERROR)
            }
        }
    }

    suspend fun updateOtherAssessmentDetails(
        pair: Pair<AssessmentEntity, AssessmentEntity>?,
        otherAssessmentDetails: HashMap<String, Any>,
        lastLocation: Location?
    ): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                val motherAssessmentEntity = pair?.first
                if (motherAssessmentEntity != null) {
                    motherAssessmentEntity.otherDetails =
                        StringConverter.convertGivenMapToString(otherAssessmentDetails)
                    lastLocation?.let {
                        motherAssessmentEntity.latitude = it.latitude
                        motherAssessmentEntity.longitude = it.longitude
                    }
                    roomHelper.updateOtherAssessmentDetails(motherAssessmentEntity)
                }
                val childAssessmentEntity = pair?.second
                if (childAssessmentEntity != null && childAssessmentEntity.isReferred) {
                    childAssessmentEntity.otherDetails =
                        StringConverter.convertGivenMapToString(otherAssessmentDetails)
                    lastLocation?.let {
                        childAssessmentEntity.latitude = it.latitude
                        childAssessmentEntity.longitude = it.longitude
                    }
                    roomHelper.updateOtherAssessmentDetails(childAssessmentEntity)
                }
                Resource(state = ResourceState.SUCCESS)
            } catch (e: Exception) {
                Resource(state = ResourceState.ERROR)
            }
        }
    }

    suspend fun getSymptomListByType(
        type: String,
    ): List<SignsAndSymptomsEntity> {
        return try {
            roomHelper.getSymptomListByType(type)
        } catch (_: Exception) {
            listOf()
        }
    }

    suspend fun getFormData(
        formType: String
    ): Resource<FormResponse> {
        return try {
            val response = roomHelper.getFormData(formType)
            val formFieldsType = object : TypeToken<FormResponse>() {}.type
            val formFields: FormResponse = Gson().fromJson(response, formFieldsType)
            Resource(state = ResourceState.SUCCESS, data = formFields)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getNearestHealthFacility(): Resource<ArrayList<Map<String, Any>>> {
        val healthFacilityList = roomHelper.getNearestHealthFacility()
        val dropDownList = ArrayList<Map<String, Any>>()
        for ((_, healthFacilityEntity) in healthFacilityList.withIndex()) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to healthFacilityEntity.name,
                    DefinedParams.id to healthFacilityEntity.fhirId.toString()
                )
            )
        }
        return Resource(state = ResourceState.SUCCESS, data = dropDownList)
    }

    suspend fun getNearestHealthFacility(
        tag: String
    ): Resource<LocalSpinnerResponse> {
        return try {
            val response = roomHelper.getNearestHealthFacility()
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getUnSyncedAssessmentCount(): Int {
        return roomHelper.getUnSyncedAssessmentCount()
    }

    suspend fun updatePregnancyAncDetail(
        hhmLocalId: Long,
        visitCount: Long,
        clinicalDate: String?
    ) {
        roomHelper.updatePregnancyAncDetail(hhmLocalId, visitCount, clinicalDate)
    }

    suspend fun getChildPatientId(parentId: Long): Long? {
        return roomHelper.getChildPatientId(parentId)
    }

    fun getAssessmentFormData(
        formType: String,
        workFlow: String
    ): LiveData<String> {
        return roomHelper.getAssessmentFormData(formType, workFlow)
    }

    suspend fun getSymptomList(): List<SignsAndSymptomsEntity> {
        return roomHelper.getSymptomList()
    }

    suspend fun getMedicationParentComplianceList(): List<MedicalComplianceEntity> {
        return roomHelper.getMedicalParentComplianceList()
    }

    suspend fun getMedicationChildComplianceList(parentId: Long): List<MedicalComplianceEntity> {
        return roomHelper.getMedicalChildComplianceList(parentId)
    }

    suspend fun saveAssessmentInformation(createRequest: AssessmentNCDEntity) =
        roomHelper.saveAssessmentInformation(createRequest)

    suspend fun getAllAssessmentRecords(uploadStatus: Boolean) = roomHelper.getAllAssessmentRecords(uploadStatus)

    suspend fun deleteAssessmentList(isUploaded: Boolean) =
        roomHelper.deleteAssessmentList(isUploaded)
    suspend fun updateAssessmentUploadStatus(id: Long, uploadStatus: Boolean) =
        roomHelper.updateAssessmentUploadStatus(id, uploadStatus)

    suspend fun getAssessmentOfflineList(uploadStatus: Boolean) = roomHelper.getAllAssessmentRecords(uploadStatus)
    suspend fun createAssessmentNCD(request: JsonObject) = apiHelper.createAssessmentNCD(request)
    suspend fun getMentalQuestion(type: String): MentalHealthEntity =
        roomHelper.getModelQuestions(type)
}