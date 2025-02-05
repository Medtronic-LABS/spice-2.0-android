package com.medtroniclabs.opensource.ui.assessment.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.db.entity.AssessmentEntity
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.model.assessment.AssessmentMemberDetails
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.AssessmentRepository
import com.medtroniclabs.opensource.repo.HouseholdMemberRepository
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.assessment.referrallogic.ReferralResultGenerator
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.isDeceased
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssessmentRMNCHNeonateViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var householdMemberRepository: HouseholdMemberRepository,
    private var assessmentRepository: AssessmentRepository,
) : ViewModel() {

    var memberMap: HashMap<String, Any>? = null

    val formLayoutsLiveData = MutableLiveData<Resource<FormResponse>>()

    val memberFormLayoutsLiveData = MutableLiveData<Resource<FormResponse>>()

    val assessmentStringSaveLiveData = MutableLiveData<String?>()

    val assessmentSaveLiveData =
        MutableLiveData<Resource<Pair<AssessmentEntity, AssessmentEntity>>>()

    val childMemberDetailsLiveData = MutableLiveData<Resource<HouseholdMemberEntity>>()

    var referralStatus: String? = null

    fun getFormData(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            memberFormLayoutsLiveData.postLoading()
            memberFormLayoutsLiveData.postValue(assessmentRepository.getFormData(formType))
        }
    }

    fun getFormChildData(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postLoading()
            formLayoutsLiveData.postValue(assessmentRepository.getFormData(formType))
        }
    }

    fun savePNCDetail(
        childDetailMap: HashMap<String, Any>,
        householdId: Long,
        motherDetailMap: HashMap<String, Any>,
        memberDetail: AssessmentMemberDetails,
        childBioDataDetail: HouseholdMemberEntity?,
        followUpId: Long? = null
    ) {
        viewModelScope.launch(dispatcherIO) {
            var deathOfNewBorn = false
            if (childDetailMap.containsKey(RMNCH.deathOfNewborn)) {
                if (childDetailMap[RMNCH.deathOfNewborn] is Boolean) {
                    deathOfNewBorn = childDetailMap[RMNCH.deathOfNewborn] as Boolean
                }
            }
            if (memberMap != null) {
                if (deathOfNewBorn) {
                    memberMap!![isDeceased] = deathOfNewBorn
                }
                val childMemberId = householdMemberRepository.registerMember(
                    memberMap!!,
                    householdId,
                    null,
                    memberDetail.id
                )
                if (childMemberId != null) {
                    savePNCDetails(
                        motherDetailMap,
                        childDetailMap,
                        memberDetail,
                        childMemberId,
                        childFhirId = null,
                        followUpId = followUpId,
                        null
                    )
                }
            } else {
                childBioDataDetail?.let { childDetail ->
                    savePNCDetails(
                        motherDetailMap,
                        childDetailMap,
                        memberDetail,
                        childDetail.id,
                        childDetail.fhirId,
                        followUpId = followUpId,
                        deathOfNewBorn
                    )
                }
            }
        }
    }

    private suspend fun savePNCDetails(
        motherDetailMap: HashMap<String, Any>,
        childDetailMap: HashMap<String, Any>,
        memberDetail: AssessmentMemberDetails,
        childMemberId: Long,
        childFhirId: String? = null,
        followUpId: Long? = null,
        deathOfNewborn: Boolean?
    ) {

        //Update Mother member details to NotSynced for PNC Flow
        if(childFhirId == null) {
            householdMemberRepository.changeMemberDetailsToNotSynced(memberDetail.id)
        }

        val groupMap = HashMap<String, Any>()
        groupMap[RMNCH.PNC] = motherDetailMap[RMNCH.PNC] as Any
        if (groupMap.containsKey(RMNCH.PNC)) {
            val motherMap = groupMap[RMNCH.PNC] as HashMap<String, Any>
            childDetailMap[RMNCH.visitNo] = motherMap[RMNCH.visitNo] as Any
        }
        groupMap[RMNCH.PNCNeonatal] = childDetailMap
        val motherReferralResult =
            ReferralResultGenerator().calculateRMNCHReferralResult(groupMap, false)
        val childReferralResult =
            ReferralResultGenerator().calculateRMNCHReferralResult(groupMap, true)
        val assessmentDetail = getAssessmentDetails(groupMap as HashMap<Any, Any>)
        referralStatus = motherReferralResult.first
        assessmentStringSaveLiveData.postValue(assessmentDetail.first)
        val otherDetails = calculateOtherDetails(groupMap, referralStatus)
        assessmentSaveLiveData.postValue(
            assessmentRepository.savePNCAssessment(
                assessmentDetail.second,
                assessmentDetail.third,
                memberDetail,
                motherReferralResult,
                null,
                otherDetails,
                Triple(childMemberId, followUpId,deathOfNewborn),
                childReferralResult
            )
        )
    }

    private fun calculateOtherDetails(
        assessmentMap: HashMap<Any, Any>,
        referralStatus: String?
    ): HashMap<String, Any>? {
        val otherDetails = HashMap<String, Any>()
        if (referralStatus != null && referralStatus == ReferralStatus.Referred.name) {
            otherDetails[AssessmentDefinedParams.ReferredPHUSiteID] =
                SecuredPreference.getString(SecuredPreference.EnvironmentKey.DEFAULT_SITE_ID.name)
                    ?: "-1"
        }
        if (assessmentMap.containsKey(RMNCH.PNC)) {
            val map = assessmentMap[RMNCH.PNC] as Map<*, *>
            if (map.containsKey(RMNCH.DateOfDelivery)) {
                val dateOfDelivery = map[RMNCH.DateOfDelivery] as String
                DateUtils.convertStringToDate(
                    dateOfDelivery,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                )?.let { deliveryDate ->
                    RMNCH.calculateNextPNCVisitDate(deliveryDate)?.let { visitDate ->
                        otherDetails[AssessmentDefinedParams.NextFollowupDate] =
                            DateUtils.convertDateTimeToDate(
                                DateUtils.getDateStringFromDate(
                                    visitDate, DateUtils.DATE_ddMMyyyy
                                ),
                                DateUtils.DATE_ddMMyyyy,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                            )
                    }
                }
            }
        }

        return if (otherDetails.isEmpty()) null else otherDetails
    }

    private fun getAssessmentDetails(map: HashMap<Any, Any>): Triple<String, String, String> {
        val assessmentDetail = StringConverter.convertGivenMapToString(map) ?: ""
        var motherMapString: String? = null
        var childMapString: String? = null

        // Request modification for syncing PNC Mother to Backend
        if (map.containsKey(RMNCH.PNC)) {
            val pnc = map[RMNCH.PNC] as HashMap<Any, Any>
            if (pnc.containsKey(RMNCH.pncMotherSigns)) {
                val signsList = mutableListOf<String>()
                val list = pnc[RMNCH.pncMotherSigns] as List<*>
                list.forEach { it ->
                    if (it is HashMap<*, *>) {
                        signsList.add(it[DefinedParams.Value] as String)
                    }
                }
                pnc[RMNCH.pncMotherSigns] = signsList
            }

            if (pnc.containsKey(RMNCH.otherPncMotherSigns)) {
                val os = pnc[RMNCH.otherPncMotherSigns] as Any
                pnc.remove(RMNCH.otherPncMotherSigns)
                pnc[RMNCH.otherSigns] = os
            }

            val parentMap = HashMap<String, Any>()
            parentMap[RMNCH.PNC] = pnc
            motherMapString = StringConverter.convertGivenMapToString(parentMap)
        }

        // Request modification for syncing PNC neonatal to Backend
        if (map.containsKey(RMNCH.PNCNeonatal)) {
            val pncNeonate = map[RMNCH.PNCNeonatal] as HashMap<Any, Any>
            if (pncNeonate.containsKey(RMNCH.pncNeonateSigns)) {
                val signsList = mutableListOf<String>()
                val list = pncNeonate[RMNCH.pncNeonateSigns] as List<*>
                list.forEach { it ->
                    if (it is HashMap<*, *>) {
                        signsList.add(it[DefinedParams.Value] as String)
                    }
                }

                pncNeonate.remove(RMNCH.pncNeonateSigns)
                pncNeonate[RMNCH.pncNeonatalSigns] = signsList
            }

            if (pncNeonate.containsKey(RMNCH.otherPncNeonateSigns)) {
                val os = pncNeonate[RMNCH.otherPncNeonateSigns] as Any
                pncNeonate.remove(RMNCH.otherPncNeonateSigns)
                pncNeonate[RMNCH.otherSigns] = os
            }
            val parentMap = HashMap<String, Any>()
            parentMap[RMNCH.PNCNeonatal] = pncNeonate
            childMapString = StringConverter.convertGivenMapToString(parentMap)
        }

        return Triple(assessmentDetail, motherMapString ?: "", childMapString ?: "")
    }

    fun getMemberDetailsByParentId(parentId: Long) {
        viewModelScope.launch(dispatcherIO) {
            childMemberDetailsLiveData.postLoading()
            assessmentRepository.getChildPatientId(parentId)?.let { childLocalId ->
                childMemberDetailsLiveData.postValue(
                    householdMemberRepository.getMemberDetailsByID(childLocalId)
                )
            }
        }
    }

    fun updateOtherAssessmentDetails(
        otherAssessmentDetails: HashMap<String, Any>,
        lastLocation: Location?,
        assessmentUpdateLiveData: MutableLiveData<Resource<String>>
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (otherAssessmentDetails.containsKey(AssessmentDefinedParams.IsClinicTaken)) {
                val isTakenToClinical =
                    otherAssessmentDetails[AssessmentDefinedParams.IsClinicTaken] as String
                otherAssessmentDetails[AssessmentDefinedParams.IsClinicTaken] =
                    (isTakenToClinical.equals(DefinedParams.Yes, true))
            }

            assessmentUpdateLiveData.postValue(
                assessmentRepository.updateOtherAssessmentDetails(
                    assessmentSaveLiveData.value?.data,
                    otherAssessmentDetails,
                    lastLocation
                )
            )
        }
    }
}