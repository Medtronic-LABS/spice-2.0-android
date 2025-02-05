package com.medtroniclabs.opensource.ui.household.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.data.model.HouseholdCardDetail
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.repo.HouseHoldRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class HouseHoldSummaryViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val houseHoldRepository: HouseHoldRepository
) : BaseViewModel(dispatcherIO) {

    var houseHoldId: Long = -1L
    var isFromHouseHoldRegistration: Boolean = false
    var selectedMemberId = -1L
    var villageId: Long = -1L
    var previousCount: Int = 0
    var selectedMemberDob: String? = null
    var isPhuWalkInsFlow:Boolean=false

    private val houseHoldNoLiveData = MutableLiveData<Long>()
    val householdCardDetailLiveData: LiveData<HouseholdCardDetail> =
        houseHoldNoLiveData.switchMap { id ->
            houseHoldRepository.getHouseholdCardDetailLiveData(id)
        }

    val householdMembersLiveData: LiveData<List<HouseholdMemberEntity>> =
        houseHoldNoLiveData.switchMap { id ->
            houseHoldRepository.getAllHouseHoldMembersLiveData(id)
        }



    fun setHouseholdId(hhId: Long) {
        this.houseHoldId = hhId
        houseHoldNoLiveData.value = hhId
    }
}