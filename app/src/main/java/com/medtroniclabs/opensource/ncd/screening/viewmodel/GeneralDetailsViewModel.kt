package com.medtroniclabs.opensource.ncd.screening.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.ncd.data.SiteDetails
import com.medtroniclabs.opensource.ncd.screening.repo.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GeneralDetailsViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository
) : ViewModel() {
    val siteDetail = SiteDetails()
    private var getSites = MutableLiveData<Boolean>()
    val getSitesLiveData: LiveData<List<HealthFacilityEntity>> =
        getSites.switchMap {
            screeningRepository.getUserHealthFacilityEntity()
        }

    private var toGetCount = MutableLiveData<Boolean>()
    val screenedPatientCount: LiveData<Long> =
        toGetCount.switchMap {
            screeningRepository.getScreenedPatientCount(
                DateUtils.getStartDate(),
                DateUtils.getEndDate(),
                SecuredPreference.getUserFhirId()
            )
        }
    val referredPatientCount: LiveData<Long> =
        toGetCount.switchMap {
            screeningRepository.getScreenedPatientReferredCount(
                DateUtils.getStartDate(),
                DateUtils.getEndDate(),
                SecuredPreference.getUserFhirId(), true
            )
        }
    fun getSites(isTrigger: Boolean) {
        getSites.value = isTrigger
    }

    fun toGetCount(isTrigger: Boolean) {
        toGetCount.value = isTrigger
    }
}