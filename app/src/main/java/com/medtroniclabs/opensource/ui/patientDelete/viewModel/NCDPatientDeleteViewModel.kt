package com.medtroniclabs.opensource.ui.patientDelete.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.mypatients.repo.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDPatientDeleteViewModel @Inject constructor(
    private var ncdMedicalReviewRepo: NCDMedicalReviewRepository,
    private val patientRepository: PatientRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    val deleteReasonList = MutableLiveData<List<ShortageReasonEntity>>()
    val patientRemoveResponse = MutableLiveData<Resource<Boolean>>()
    fun getDeleteReasonList() {
        viewModelScope.launch(dispatcherIO) {
            val deleteList = ncdMedicalReviewRepo.getNCDShortageReason(NCDMRUtil.TYPE_DELETE)
            val list = ArrayList(deleteList)
            if (list.isNotEmpty()) {
                val itemIndex =
                    list.indexOfFirst { it.name.contains(DefinedParams.Other, ignoreCase = true) }
                if (itemIndex >= 0 && (itemIndex + 1) != list.size) {
                    val item = list.removeAt(itemIndex)
                    list.add(item)
                }
            }
            deleteReasonList.postValue(list)
        }
    }

    fun ncdPatientRemove(request: NCDPatientRemoveRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientRemoveResponse.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPatientDelete,
                isCompleted = true
            )
            patientRemoveResponse.postValue(patientRepository.ncdPatientRemove(request))
        }
    }

}