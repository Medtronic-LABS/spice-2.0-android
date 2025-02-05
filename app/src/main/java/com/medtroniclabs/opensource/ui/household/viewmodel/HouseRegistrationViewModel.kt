package com.medtroniclabs.opensource.ui.household.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.villageId
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.HouseHoldRepository
import com.medtroniclabs.opensource.repo.HouseholdMemberRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseRegistrationViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val houseHoldRepository: HouseHoldRepository,
    private val houseHoldRepositoryMember: HouseholdMemberRepository,
) : BaseViewModel(dispatcherIO) {

    var houseHoldRegistrationLiveData = MutableLiveData<Resource<Long>>()
    var isMemberRegistration: Boolean = false
    var householdEntityDetail: HouseholdEntity? = null
    val formLayoutsLiveData = MutableLiveData<Resource<String>>()
    var houseHoldUpdateLiveData = MutableLiveData<Resource<Long>>()
    val houseHoldDetailLiveData = MutableLiveData<Resource<HouseholdEntity>>()
    var householdId: Long = -1L
    var villageListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var memberID: Long = -1L
    private var lastLocation: Location? = null
    var addNewMember: Boolean = false
    var eventName: String = ""


    var signatureFilename: String? = null
    var initialValue: String? = null


    fun getFormData(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postLoading()
            formLayoutsLiveData.postValue(houseHoldRepository.getFormData(formType))
        }
    }

    fun loadDataCacheByType(type: String, tag: String) {
        viewModelScope.launch(dispatcherIO) {
            when (type) {
                villageId -> {
                    villageListResponse.postLoading()
                    villageListResponse.postValue(houseHoldRepository.getUserVillages(tag))
                }
            }
        }
    }

    fun registerHousehold(map: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            try {
                houseHoldRegistrationLiveData.postLoading()
                householdEntityDetail = houseHoldRepository.createOrUpdateHouseHoldEntity(map)
                houseHoldRegistrationLiveData.postSuccess()
            } catch (e: Exception) {
                houseHoldRegistrationLiveData.postError()
            }
        }
    }

    fun updateHousehold(map: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            try {
                houseHoldUpdateLiveData.postLoading()
                val householdEntity =
                    houseHoldRepository.createOrUpdateHouseHoldEntity(map, householdEntityDetail)
                houseHoldRepository.updateHouseHoldEntity(householdEntity)
                houseHoldRepository.updateHouseholdHeadPhoneNumber(householdEntity.id, householdEntity.headPhoneNumber, householdEntity.headPhoneNumberCategory)
                houseHoldUpdateLiveData.postSuccess()
            } catch (e: Exception) {
                houseHoldUpdateLiveData.postError()
            }
        }
    }

    fun getHouseholdDetailsByID(houseHoldId: Long) {
        try {
            viewModelScope.launch(dispatcherIO) {
                houseHoldDetailLiveData.postLoading()
                householdEntityDetail = houseHoldRepository.getHouseHoldDetailsById(houseHoldId)
                houseHoldDetailLiveData.postSuccess(householdEntityDetail)
            }
        } catch (e: Exception) {
            houseHoldDetailLiveData.postError()
        }
    }

    fun setCurrentLocation(location: Location) {
        this.lastLocation = location
    }

    fun getCurrentLocation(): Location? {
        return this.lastLocation
    }
    fun updateMemberAsAssigned(memberID: Long?) {
        viewModelScope.launch(dispatcherIO) {
            houseHoldRepositoryMember.updateMemberAsAssigned(memberId = memberID.toString())
        }
    }
}
