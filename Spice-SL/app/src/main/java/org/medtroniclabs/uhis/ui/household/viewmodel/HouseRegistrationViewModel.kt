package org.medtroniclabs.uhis.ui.household.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant
import org.medtroniclabs.uhis.db.entity.HouseholdEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration
import org.medtroniclabs.uhis.mappingkey.MemberRegistration.ID_GUARDIAN
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.repo.HouseholdMemberRepository
import org.medtroniclabs.uhis.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HouseRegistrationViewModel @Inject constructor(
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val houseHoldRepository: HouseHoldRepository,
    private val houseHoldRepositoryMember: HouseholdMemberRepository,
) : BaseViewModel(dispatcherIO) {
    var houseHoldRegistrationLiveData = MutableLiveData<Resource<Long>>()
    var isMemberRegistration: Boolean = false
    var householdEntityDetail: HouseholdEntity? = null
    var isCreateHouseholdForPhu: Boolean = false
    var isPhuWalkInsFlow: Boolean = false
    val formLayoutsLiveData = MutableLiveData<Resource<String>>()
    var houseHoldUpdateLiveData = MutableLiveData<Resource<Long>>()
    val houseHoldDetailLiveData = MutableLiveData<Resource<HouseholdEntity>>()
    var householdId: Long = -1L
    var villageListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var memberVillageListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var shasthyaShebikaListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var shasthyaKormiListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var subVillageListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var guardianMembers = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var memberID: Long = -1L
    private var lastLocation: Location? = null
    var eventName: String = ""

    /**
     * Selected sub village
     */
    var selectedSubVillageId: Long = -1

    var signatureFilename: String? = null
    var initialValue: String? = null
    val generatedHouseholdNumberLiveData = MutableLiveData<String>()

    /**
     * Generates household number in the format HH<current count of households based on subvillage + 1>
     */
    fun generateHouseholdNumber() {
        viewModelScope.launch(dispatcherIO) {
            val currentHouseholdsCount = houseHoldRepository.getHouseholdsCountBasedSubVillage(selectedSubVillageId)
            val householdNumber = "HH${currentHouseholdsCount + 1}"
            generatedHouseholdNumberLiveData.postValue(householdNumber)
        }
    }

    fun getFormData(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postLoading()
            formLayoutsLiveData.postValue(houseHoldRepository.getFormData(formType))
        }
    }

    fun loadDataCacheByType(
        type: String,
        tag: String,
    ) {
        viewModelScope.launch(dispatcherIO) {
            when (type) {
                HouseHoldRegistration.VILLAGE_ID -> {
                    villageListResponse.postLoading()
                    villageListResponse.postValue(houseHoldRepository.getUserVillages(tag))
                }
            }
        }
    }

    fun loadVillageDataCacheByType(
        type: String,
        tag: String,
    ) {
        viewModelScope.launch(dispatcherIO) {
            when (type) {
                HouseHoldRegistration.VILLAGE_ID -> {
                    memberVillageListResponse.postLoading()
                    memberVillageListResponse.postValue(houseHoldRepository.getUserLinkedVillages(tag))
                }

                ID_GUARDIAN -> {
                    guardianMembers.postLoading()
                    guardianMembers.postValue(houseHoldRepository.getGuardianMembers(tag, householdId, memberID))
                }
            }
        }
    }

    fun loadShasthyaShebikaDataCacheByType() {
        viewModelScope.launch(dispatcherIO) {
            val userId = SecuredPreference.getUserId()
            shasthyaShebikaListResponse.postLoading()
            shasthyaShebikaListResponse.postValue(houseHoldRepository.getShasthyaShebikasByKormiId(userId))
        }
    }

    fun loadAllShasthyaKormis() {
        viewModelScope.launch(dispatcherIO) {
            shasthyaKormiListResponse.postLoading()
            shasthyaKormiListResponse.postValue(houseHoldRepository.getAllShasthyaKormisSpinner())
        }
    }

    fun loadShasthyaShebikaForKormiId(kormiId: Long) {
        viewModelScope.launch(dispatcherIO) {
            shasthyaShebikaListResponse.postLoading()
            shasthyaShebikaListResponse.postValue(houseHoldRepository.getShasthyaShebikasByKormiId(kormiId))
        }
    }

    suspend fun getVillageEntity(villageId: Long): VillageEntity? = houseHoldRepository.getVillageByID(villageId).data

    fun loadSubVillageDataCacheByType(shasthyaShebikaId: Long) {
        viewModelScope.launch(dispatcherIO) {
            subVillageListResponse.postLoading()
            subVillageListResponse.postValue(houseHoldRepository.getSubVillagesByShasthyaShebikaId(shasthyaShebikaId))
        }
    }

    /**
     * CHCP registration loads sub-villages directly from the selected Union ([villageId])
     * instead of via a Shasthya Shebika.
     */
    fun loadSubVillageByVillageId(villageId: Long) {
        viewModelScope.launch(dispatcherIO) {
            subVillageListResponse.postLoading()
            subVillageListResponse.postValue(houseHoldRepository.getSubVillagesByVillageId(villageId))
        }
    }

    fun registerHousehold(map: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            try {
                houseHoldRegistrationLiveData.postLoading()
                householdEntityDetail = houseHoldRepository.createOrUpdateHouseHoldEntity(map)
                houseHoldRegistrationLiveData.postSuccess()
            } catch (_: Exception) {
                houseHoldRegistrationLiveData.postError()
            }
        }
    }

    fun updateHousehold(map: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            try {
                houseHoldUpdateLiveData.postLoading()
                val detail = householdEntityDetail ?: run {
                    houseHoldUpdateLiveData.postError()
                    return@launch
                }
                val before = houseHoldRepository.getHouseHoldDetailsById(detail.id)
                val householdEntity =
                    houseHoldRepository.createOrUpdateHouseHoldEntity(map, detail)
                if (houseHoldRepository.hasMeaningfulHouseholdChanges(before, householdEntity)) {
                    houseHoldRepository.updateHouseHoldEntity(householdEntity)
                } else {
                    householdEntityDetail = before
                }
                houseHoldUpdateLiveData.postSuccess()
            } catch (_: Exception) {
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
        } catch (_: Exception) {
            houseHoldDetailLiveData.postError()
        }
    }

    fun setCurrentLocation(location: Location) {
        this.lastLocation = location
    }

    fun getCurrentLocation(): Location? = this.lastLocation

    fun updateMemberAsAssigned(
        memberID: Long?,
        hhmId: Long? = null,
        hhId: Long? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (hhmId != null && hhId != null) {
                val tbPatientIds = houseHoldRepositoryMember.getTbPatientLocalIdByHouseholdId(hhId)
                val isTbPatient = houseHoldRepositoryMember.isTbPatient(memberID.toString())

                when {
                    tbPatientIds.isNotEmpty() && !isTbPatient -> {
                        // Household has TB patients but this member is not one — update contact tracing status
                        houseHoldRepositoryMember.updateContactTracingStatus(
                            hhmId,
                            OfflineConstant.CONTACT_TRACING_YET_TO_TAKE,
                        )
                    }

                    tbPatientIds.isEmpty() && isTbPatient -> {
                        // No TB patients in household but this member is one — update all household contact tracing statuses
                        houseHoldRepositoryMember.updateContactTracingForLinkTbPatient(hhmId, hhId)
                    }

                    else -> {
                        houseHoldRepositoryMember.updateContactTracingStatus(hhmId, null)
                    }
                }
            }

            houseHoldRepositoryMember.updateMemberAsAssigned(memberID.toString())
        }
    }
}
