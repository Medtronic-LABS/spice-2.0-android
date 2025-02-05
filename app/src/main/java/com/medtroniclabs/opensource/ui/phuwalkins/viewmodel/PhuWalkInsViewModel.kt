package com.medtroniclabs.opensource.ui.phuwalkins.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.db.response.HouseHoldEntityWithMemberCount
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.repo.HouseHoldRepository
import com.medtroniclabs.opensource.repo.HouseholdMemberRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhuWalkInsViewModel @Inject constructor(
    private val householdMemberRepository: HouseholdMemberRepository,
    private val houseHoldRepository: HouseHoldRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    var memberID:Long=-1L

    var fhirMemberID:Long=-1L

    val unAssignedMembers: LiveData<List<UnAssignedHouseholdMemberDetail>> =
        householdMemberRepository.getUnAssignedHouseholdMember()

    fun getPatientName(
        context: Context,
        name: String?,
        dob: String?,
        gender: String?
    ): String {
        return context.getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                context
            ),
            CommonUtils.getGenderText(gender, context)
        )
    }


    fun getFilteredHouseholdsLiveData(villageIds: Long): LiveData<List<HouseHoldEntityWithMemberCount>> {
        return houseHoldRepository.getFilteredHouseholdsLiveData(
            "",
            villageIds = listOf(villageIds),
            ""
        )
    }

    fun getSearchHouseholdsLiveData(search: String, villageId: Long): LiveData<List<HouseHoldEntityWithMemberCount>> {
      val listVillage= ArrayList<Long>()
        listVillage.add(villageId)
        return houseHoldRepository.getFilteredHouseholdsLiveData(
            search,
            villageIds = listVillage,
            ""
        )
    }

    fun saveCallHistory() {
        viewModelScope.launch(dispatcherIO) {
            SecuredPreference.getLong(DefinedParams.houseHoldLinkStartTiming).let {
                householdMemberRepository.addLinkMemberCall(
                    memberID.toString(),
                    callStartTime = it,
                    callEndTime =  System.currentTimeMillis()
                )
            }
        }
    }

}