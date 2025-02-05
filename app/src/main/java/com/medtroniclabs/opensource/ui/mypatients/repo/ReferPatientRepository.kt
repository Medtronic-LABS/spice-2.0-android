package com.medtroniclabs.opensource.ui.mypatients.repo

import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.ReferPatientAPIRequest
import com.medtroniclabs.opensource.data.ReferPatientHealthFacilityItem
import com.medtroniclabs.opensource.data.ReferPatientNameNumber
import com.medtroniclabs.opensource.data.ReferPatientRequest
import com.medtroniclabs.opensource.data.ReferPatientResult
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class ReferPatientRepository @Inject constructor(
    private val apiHelper: ApiHelper,
    private val roomHelper: RoomHelper
) {

    suspend fun getHealthFacilityMetaData(
        districtId: String?
    ): Resource<List<ReferPatientHealthFacilityItem>> {
        try {
            val response = apiHelper.getHealthFacilityMetaData(ReferPatientAPIRequest(districtId))
            if (response.isSuccessful) {
                response.body()?.entityList?.let {
                    return Resource(state = ResourceState.SUCCESS, it)
                }
            } else {
                return Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            return Resource(state = ResourceState.ERROR)
        }
        return Resource(state = ResourceState.ERROR)
    }

    suspend fun getReferPatientMobileUserList(tenantId: String): Resource<List<ReferPatientNameNumber>> {
        try {
            val response = apiHelper.getReferPatientMobileUserList(ReferPatientRequest(tenantId))
            if (response.isSuccessful) {
                response.body()?.entityList?.let {
                    return Resource(state = ResourceState.SUCCESS, it)
                }
            }
        } catch (e: Exception) {
            return Resource(state = ResourceState.ERROR)
        }
        return Resource(state = ResourceState.ERROR)
    }
    suspend fun createReferPatientResult(
        patientReference: String?,
        encounterId: String?,
        selectedItems: Triple<String?, String?, String?>,
        assessmentName: Pair<String?, String>,
        patientId: String?,
        houseHoldId: String?,
        villageId: String?,
        memberId: String?
    ): Resource<HashMap<String,Any>> {
        try {
            val request = createReferPatientRequest(
                patientReference,
                encounterId,
                selectedItems,
                assessmentName,
                patientId,
                houseHoldId,
                villageId,
                memberId
            )
            val response = request?.let { apiHelper.createReferPatientResult(it) }
            if (response != null && response.isSuccessful) {
                response.body()?.entity?.let {
                    return Resource(state = ResourceState.SUCCESS, it)
                }
            }
        } catch (e: Exception) {
            return Resource(state = ResourceState.ERROR)
        }
        return Resource(state = ResourceState.ERROR)
    }
    private fun createReferPatientRequest(
        patientReference: String?,
        encounterId: String?,
        selectedItems: Triple<String?, String?, String?>,
        assessmentName: Pair<String?, String>,
        patientId: String?,
        houseHoldId: String?,
        villageId: String?,
        memberId: String?
    ): ReferPatientResult {
        return ReferPatientResult(
            encounterId = encounterId,
            type = MedicalReviewTypeEnums.medicalReview.name,
            referredReason = selectedItems.third,
            referredSiteId = selectedItems.first,
            referredClinicianId = selectedItems.second,
            patientReference = patientReference,
            referred = true,
            provenance = ProvanceDto(),
            patientStatus = DefinedParams.REFERRED,
            currentPatientStatus = DefinedParams.REFERRED,
            assessmentName = assessmentName.first,
            patientId = patientId,
            householdId = houseHoldId,
            villageId = villageId,
            memberId = memberId,
            category = assessmentName.second
        )
    }
    suspend fun getDefaultHealthFacilityDistrictId(): Resource<HealthFacilityEntity?>? {
        return try {
            val response = roomHelper.getDefaultHealthFacility()
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }
}