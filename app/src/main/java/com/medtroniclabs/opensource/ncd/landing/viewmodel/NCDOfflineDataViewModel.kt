package com.medtroniclabs.opensource.ncd.landing.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.boarding.repo.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class NCDOfflineDataViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    private var toGetCount = MutableLiveData<Boolean>()
    val screeningCount: LiveData<Long> =
        toGetCount.switchMap {
            metaRepository.getUnSyncedDataCountForNCDScreening()
        }

    val assessmentType: LiveData<Long> =
        toGetCount.switchMap {
            metaRepository.getUnSyncedNCDAssessmentCount()
        }

    val followUpType: LiveData<Long> = toGetCount.switchMap {
        metaRepository.getUnSyncedNCDFollowUpCount()
    }

    fun getCountOfflineData() {
        toGetCount.value = true
    }
}