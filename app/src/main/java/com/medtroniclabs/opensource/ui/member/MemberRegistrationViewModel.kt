package com.medtroniclabs.opensource.ui.member

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.mappingkey.MemberRegistration
import com.medtroniclabs.opensource.model.medicalreview.AddMemberRegRequest
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.HouseHoldRepository
import com.medtroniclabs.opensource.repo.HouseholdMemberRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberRegistrationViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val memberRegistrationRepository: HouseholdMemberRepository,
    private val houseHoldRepository: HouseHoldRepository,
) : BaseViewModel(dispatcherIO) {

    var selectedHouseholdId: Long = -1L
    var memberRegistrationLiveData = MutableLiveData<Resource<Long>>()
    var startAssessment: Boolean? = null
    val memberDetailsLiveData = MutableLiveData<Resource<HouseholdMemberEntity>>()
    val formLayoutsLiveData = MutableLiveData<Resource<String>>()
    var medicalReviewFlow=false
    val addnewMemberReq=MutableLiveData<Resource<String>>()
    var villageDetails:List<VillageEntity>?= null
    var addNewMember: Boolean = false
    var memberDob: String?=null
    var isPhuWalkInsFlow:Boolean? = null

    fun getFormData(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postValue(houseHoldRepository.getFormData(formType))
        }
    }

    fun getMemberDetailsByID(memberId: Long) {
        if (memberId == -1L) {
            return
        }
        viewModelScope.launch(dispatcherIO) {
            memberDetailsLiveData.postLoading()
            memberDetailsLiveData.postValue(memberRegistrationRepository.getMemberDetailsByID(memberId))
        }
    }

    fun registerHouseThenMember(
        householdEntity: HouseholdEntity,
        memberResultMap: HashMap<String, Any>,
        location: Location?,
        initial: String? = null,
        signature: String? = null
    ) {
         memberRegistrationLiveData.postLoading()
          try {
              viewModelScope.launch(dispatcherIO) {
                  location?.let {
                      householdEntity.latitude = it.latitude
                      householdEntity.longitude = it.longitude
                  }
                  val houseHoldId = houseHoldRepository.insertHouseHoldEntity(householdEntity)
                  registerMember(memberResultMap, houseHoldId, initial, signature)
              }
          }catch (e: Exception) {
              memberRegistrationLiveData.postError(e.message)
          }
    }

    fun registerMember(map: HashMap<String, Any>, householdId: Long, initial: String? = null, signature: String? = null) {
         memberRegistrationLiveData.postLoading()
        try {
            viewModelScope.launch(dispatcherIO) {
                selectedHouseholdId = householdId
                memberDob = if (map.containsKey(MemberRegistration.dateOfBirth)) {
                    CommonUtils.getStringOrEmptyString(map[MemberRegistration.dateOfBirth])
                } else {
                    null
                }
                val memberId = memberRegistrationRepository.registerMember(
                    map,
                    householdId,
                    memberDetailsLiveData.value?.data,
                    initial = initial,
                    signature = signature,
                    isPhuWalkInFlow = isPhuWalkInsFlow
                )
                memberRegistrationRepository.updateHeadPhoneNumber(householdId, map)
                if (memberId == null) {
                    memberRegistrationLiveData.postError()
                } else {
                    memberRegistrationLiveData.postSuccess(memberId)
                }
            }
        } catch (e: Exception) {
            memberRegistrationLiveData.postError()
        }
    }

    fun addNewMember(map: HashMap<String, Any>?, formGenerator: FormGenerator) {
        if (map == null) return
        val villageId = map[MemberRegistration.villageId]?.toString()?.toIntOrNull()
        val addMemberRegRequest = AddMemberRegRequest().apply {
            name = map[MemberRegistration.name]?.toString().orEmpty()
            this.villageId = villageId?.toString().orEmpty()
            village = villageId?.let { id ->
                villageDetails?.find { it.id == id.toLong() }?.name.orEmpty()
            }.orEmpty()
            dateOfBirth = map[MemberRegistration.dateOfBirth]?.toString().orEmpty()
            gender = map[MemberRegistration.gender]?.toString().orEmpty()
            phoneNumber = map[MemberRegistration.phoneNumber]?.toString().orEmpty()
            phoneNumberCategory = map[MemberRegistration.phoneNumberCategory]?.toString().orEmpty()
            provenance = ProvanceDto()
            isPregnant = map[MemberRegistration.isPregnant]?.let { CommonUtils.getIsBooleanFromString(it) }
        }
        viewModelScope.launch(dispatcherIO) {
            addnewMemberReq.postLoading()
            addnewMemberReq.postValue(houseHoldRepository.addNewMember(addMemberRegRequest))
        }
    }



}
