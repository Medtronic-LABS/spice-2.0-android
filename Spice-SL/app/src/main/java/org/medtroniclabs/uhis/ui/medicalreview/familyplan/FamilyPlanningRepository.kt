package org.medtroniclabs.uhis.ui.medicalreview.familyplan

import android.location.Location
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.AboveFiveYearsSummaryRequest
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.model.Contraceptive
import org.medtroniclabs.uhis.data.model.FamilyPlanningContraceptivesRequest
import org.medtroniclabs.uhis.data.model.FamilyPlanningCreateResponse
import org.medtroniclabs.uhis.data.model.FamilyPlanningSummaryResponse
import org.medtroniclabs.uhis.data.model.MedicalReviewEncounter
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.ClientType
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CombineOralContraceptive
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CombinedOralContraceptiveComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Condoms
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.EmergencyContraceptive
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Implants
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Injectables
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.MicrolutQuantity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherImplantComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherInjectableComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherPermanentMethodComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherProgestinOnlyOralsComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.PermanentMethod
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.PostPartum
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.ProgestinOnlyOrals
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class FamilyPlanningRepository @Inject constructor(
    private var roomHelper: RoomHelper,
    private var apiHelper: ApiHelper,
) {
    suspend fun getStaticMetaData(menuType: String): Resource<Boolean> =
        try {
            val response = apiHelper.getFamilyPlanningStaticData()
            if (response.isSuccessful) {
                response.body()?.entity?.apply {
                    roomHelper.deleteExaminationsComplaints(menuType)
                    roomHelper.insertExaminationsComplaint(
                        generateChipItemByType(
                            familyPlanning,
                        ),
                    )
                }
                SecuredPreference.putBoolean(
                    SecuredPreference.EnvironmentKey.IS_FAMILY_PLANNING_LOADED.name,
                    true,
                )
                Resource(state = ResourceState.SUCCESS, true)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SecuredPreference.putBoolean(
                SecuredPreference.EnvironmentKey.IS_FAMILY_PLANNING_LOADED.name,
                false,
            )
            Resource(state = ResourceState.ERROR)
        }

    private fun generateChipItemByType(familyPlanning: List<MedicalReviewMetaItems>): List<MedicalReviewMetaItems> {
        val chipItemList = ArrayList<MedicalReviewMetaItems>()
        familyPlanning.forEach { item ->
            val chipItem = MedicalReviewMetaItems(
                id = item.id,
                name = item.name,
                type = item.type,
                category = item.category,
                displayOrder = item.displayOrder,
                value = item.value,
            )
            chipItemList.add(chipItem)
        }
        return chipItemList
    }

    suspend fun createFamilyPlanningMR(
        details: PatientListRespModel,
        resultPair: Pair<HashMap<String, Any>, List<ChipViewItemModel>>,
        occupation: String?,
        maritalStatus: String?,
        encounterId: String?,
        lastLocation: Location?,
        notes: String?,
    ): Resource<FamilyPlanningCreateResponse> =
        try {
            val response = apiHelper.createFamilyPlanningMR(
                composeSubmitRequest(
                    details,
                    resultPair,
                    occupation,
                    maritalStatus,
                    encounterId,
                    lastLocation,
                    notes,
                ),
            )
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.status == true) {
                    Resource(state = ResourceState.SUCCESS, data = res.entity)
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }

    private fun composeSubmitRequest(
        details: PatientListRespModel,
        resultPair: Pair<HashMap<String, Any>, List<ChipViewItemModel>>,
        occupation: String?,
        maritalStatus: String?,
        encounterId: String?,
        lastLocation: Location?,
        notes: String?,
    ): FamilyPlanningContraceptivesRequest {
        val resultMap = resultPair.first
        return FamilyPlanningContraceptivesRequest(
            id = encounterId,
            patientId = details.id,
            assessmentName = MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name,
            clinicalNotes = notes,
            encounter = MedicalReviewEncounter(
                id = encounterId,
                patientId = details.patientId,
                provenance = ProvanceDto(),
                latitude = lastLocation?.latitude ?: 0.0,
                longitude = lastLocation?.longitude ?: 0.0,
                householdId = details.houseHoldId,
                memberId = details.memberId,
                startTime = DateUtils.getCurrentDateAndTime(
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                ),
                endTime = DateUtils.getCurrentDateAndTime(
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                ),
                referred = true,
                villageId = details.villageId,
            ),
            contraceptive = Contraceptive(
                occupation = occupation?.takeIf { it.isNotBlank() },
                maritalStatus = maritalStatus.takeIf { !(it.equals(DEFAULT_ID)) },
                clientType = (resultMap[ClientType] as? String)?.takeIf { it.isNotEmpty() },
                postPartum = (resultMap[PostPartum] as? String)?.takeIf { it.isNotEmpty() },
                combinedOralContraceptive = (resultMap[CombineOralContraceptive] as? String)?.takeIf { it.isNotEmpty() },
                otherCombinedOralContraceptive = (resultMap[CombinedOralContraceptiveComments] as? String)?.takeIf { it.isNotEmpty() },
                progestinOnlyOrals = (resultMap[ProgestinOnlyOrals] as? String)?.takeIf { it.isNotEmpty() },
                otherProgestinOnlyOrals = (resultMap[OtherProgestinOnlyOralsComments] as? String)?.takeIf { it.isNotEmpty() },
                microlutQuantity = (resultMap[MicrolutQuantity]as? Long)?.takeIf { it != -1L },
                injectables = (resultMap[Injectables] as? String)?.takeIf { it.isNotEmpty() },
                otherInjectables = (resultMap[OtherInjectableComments] as? String)?.takeIf { it.isNotEmpty() },
                iucd = resultPair.second.mapNotNull { it.value },
                implants = (resultMap[Implants] as? String)?.takeIf { it.isNotEmpty() },
                otherImplants = (resultMap[OtherImplantComments] as? String)?.takeIf { it.isNotEmpty() },
                condoms = (resultMap[Condoms] as? String)?.takeIf { it.isNotEmpty() },
                emergencyContraceptive = (resultMap[EmergencyContraceptive] as? String)?.takeIf { it.isNotEmpty() },
                permanentMethod = (resultMap[PermanentMethod] as? String)?.takeIf { it.isNotEmpty() },
                otherPermanentMethod = (resultMap[OtherPermanentMethodComments] as? String)?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun getMetaListByType(type: String): Resource<List<MedicalReviewMetaItems>> =
        try {
            val response = roomHelper.getExaminationsComplaintByType(type)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getFamilyPlanningSummaryDetails(request: AboveFiveYearsSummaryRequest): Resource<FamilyPlanningSummaryResponse> =
        try {
            val response = apiHelper.getFamilyPlanningMRSummaryDetails(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
}
