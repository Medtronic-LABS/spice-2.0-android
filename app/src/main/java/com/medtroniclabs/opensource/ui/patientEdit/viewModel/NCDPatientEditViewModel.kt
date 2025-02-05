package com.medtroniclabs.opensource.ui.patientEdit.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.patientEdit.NCDPatientEditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDPatientEditViewModel @Inject constructor(
    private val repository: NCDPatientEditRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    var updatePatientMap = MutableLiveData<Resource<HashMap<String, Any>>>()

    fun ncdUpdatePatientDetail(request: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            updatePatientMap.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPatientEdit,
                isCompleted = true
            )
            updatePatientMap.postValue(repository.ncdUpdatePatientDetail(request))
        }
    }

}