package org.medtroniclabs.uhis.repo

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.registration.QRCodeRequest
import org.medtroniclabs.uhis.data.registration.UserDashboardRequest
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.SymptomEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import javax.inject.Inject

class OnBoardingRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper,
) {
    suspend fun getFormData(formType: String): Resource<FormResponse> =
        try {
            val response = roomHelper.getFormData(formType)
            val formFieldsType = object : TypeToken<FormResponse>() {}.type
            val formFields: FormResponse = Gson().fromJson(response, formFieldsType)
            Resource(state = ResourceState.SUCCESS, data = formFields)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getFormBasedOnType(
        formTypeOne: String,
        formTypeTwo: String,
    ) = roomHelper.getFormBasedOnType(formTypeOne, formTypeTwo)

    suspend fun riskFactorListing() = roomHelper.getAllRiskFactorEntityList()

    suspend fun validatePatient(request: JsonObject) = apiHelper.validatePatient(request)

    suspend fun createPatient(request: JsonObject) = apiHelper.createPatient(request)

    suspend fun getAllVillages(): List<VillageEntity> = roomHelper.getAllVillageEntity()

    suspend fun getVillageList(selectedParent: Long): List<VillageEntity> = roomHelper.getVillageList(selectedParent)

    suspend fun getOtherVillage(): VillageEntity = roomHelper.getOtherVillage()

    suspend fun getProgramList(selectedParent: Long?): Any {
        // val site = SecuredPreference.getSelectedSiteEntity()?.id ?: -1
        val site = -1
        return if (selectedParent == null) {
            roomHelper.getProgramList(site.toString())
        } else {
            roomHelper.getProgramList(selectedParent, site.toString())
        }
    }

    suspend fun getSubVillages(villageId: Long): List<SubVillageEntity> = roomHelper.getSubVillagesByShasthyaShebikaId(villageId)

    suspend fun getUpazilaListByDistrictID(districtID: Long): Any = roomHelper.getUpazilaListByDistrict(districtID)

    suspend fun getSubVillageByVillageId(villageId: Long): List<SubVillageEntity> = roomHelper.getSubVillage(villageId)

    suspend fun getUpazilaList(): Any = roomHelper.getUpazilaList()

    suspend fun getAllChiefDoms(): Any = roomHelper.getAllChiefDoms()

    suspend fun validateQRCodeValidation(qrCodeRequest: QRCodeRequest) = apiHelper.validateQRCodeValidation(qrCodeRequest)

    suspend fun updateSequenceCode(
        villageId: Long,
        newSequenceCode: Long,
    ) = roomHelper.updateSequenceCode(villageId, newSequenceCode)

    suspend fun createPatientVisit(request: MedicalReviewBaseRequest) = apiHelper.createPatientVisit(request)

    suspend fun createScreeningLog(createPatientRequest: JsonObject) = apiHelper.createScreeningLog(createPatientRequest)

    suspend fun getUpazilaListEyeCareOnly(): Any = roomHelper.getUpazilaListEyeCareOnly()

    suspend fun getUpazilaListCataractOnly(): Any = roomHelper.getUpazilaListCataractOnly()

    suspend fun getSiteList(
        userSite: Boolean,
        userId: Long,
    ) = roomHelper.getSiteEntity(userSite, userId)

    suspend fun validateSession() = apiHelper.validateSession()

    suspend fun getUpazilaById(upazilaId: Long) = roomHelper.getUpazilaById(upazilaId)

    suspend fun getSymptomListByType(type: String): List<SymptomEntity> = roomHelper.getSymptomsListByType(type)

    suspend fun getProviderDashboardDetails(request: UserDashboardRequest) = apiHelper.getProviderDashboardDetails(request)
}
