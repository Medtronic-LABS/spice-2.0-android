package org.medtroniclabs.uhis.ui.phuwalkins.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.repo.HouseholdMemberRepository
import org.medtroniclabs.uhis.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class PhuWalkInsViewModel @Inject constructor(
    private val householdMemberRepository: HouseholdMemberRepository,
    private val houseHoldRepository: HouseHoldRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseViewModel(dispatcherIO) {
    var memberID: Long = -1L

    var fhirMemberID: Long = -1L

    val unAssignedMembers: LiveData<List<UnAssignedHouseholdMemberDetail>> =
        householdMemberRepository.getUnAssignedHouseholdMember()

    fun getPatientName(
        context: Context,
        name: String?,
        dob: String?,
        gender: String?,
    ): String =
        context.getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                context,
            ),
            CommonUtils.getGenderText(gender, context),
        )

    fun getFilteredHouseholdsLiveData(villageIds: Long): LiveData<List<HouseHoldEntityWithLastActivity>> =
        houseHoldRepository.getFilteredHouseholdsLiveData(
            "",
            ssIds = emptyList(),
        )

    fun getSearchHouseholdsLiveData(
        search: String,
        villageId: Long,
    ): LiveData<List<HouseHoldEntityWithLastActivity>> {
        val listVillage = ArrayList<Long>()
        listVillage.add(villageId)
        return houseHoldRepository.getFilteredHouseholdsLiveData(
            search,
            ssIds = emptyList(),
        )
    }

    fun saveCallHistory() {
        viewModelScope.launch(dispatcherIO) {
            val callStartTime = SecuredPreference.getLong(DefinedParams.HOUSE_HOLD_LINK_START_TIMING)
            householdMemberRepository.addLinkMemberCall(
                memberID.toString(),
                callStartTime = callStartTime,
                callEndTime = System.currentTimeMillis(),
            )
        }
    }
}
