package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.db.entity.NCDDiagnosisEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetRequest
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetResponse
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisRequestResponse
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDDiagnosisViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository,
) : BaseViewModel(dispatcherIO) {

    var comments: String = ""
    private val getChips = MutableLiveData<Triple<List<String>,String,Boolean>>()
    var selectedChips: ArrayList<ChipViewItemModel> = ArrayList()
    val getChipLiveData: LiveData<List<NCDDiagnosisEntity>> =
        getChips.switchMap {
            ncdMedicalReviewRepository.getNCDDiagnosisList(it.first, it.second, it.third)
        }

    fun getChip(types: List<String>, gender: String, isPregnant: Boolean) {
        getChips.value = Triple(types, gender, isPregnant)
    }

    val createConfirmDiagonsis = MutableLiveData<Resource<HashMap<String, Any>>>()
    val getConfirmDiagonsis = MutableLiveData<Resource<NCDDiagnosisGetResponse>>()

    fun createConfirmDiagonsis(request: NCDDiagnosisRequestResponse, menuId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            createConfirmDiagonsis.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDConfirmDiagnosisCreation + " " + menuId,
                isCompleted = true
            )
            createConfirmDiagonsis.postValue(ncdMedicalReviewRepository.createConfirmDiagonsis(request))
        }
    }

    fun getConfirmDiagonsis(request: NCDDiagnosisGetRequest) {
        viewModelScope.launch(dispatcherIO) {
            getConfirmDiagonsis.postLoading()
            getConfirmDiagonsis.postValue(ncdMedicalReviewRepository.getConfirmDiagonsis(request))
        }
    }

}