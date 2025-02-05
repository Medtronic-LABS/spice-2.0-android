package com.medtroniclabs.opensource.ui.landing.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.data.CulturesEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.CultureLocaleModel
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.boarding.repo.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguagePreferenceViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {
    var selectedCultureId: Long? = null

    val cultureList = MutableLiveData<Resource<List<CulturesEntity>>>()
    val cultureUpdateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()

    fun getCultures() {
        viewModelScope.launch(dispatcherIO) {
            cultureList.postValue(metaRepository.getCultures())
        }
    }

    fun cultureLocaleUpdate(request: CultureLocaleModel) {
        viewModelScope.launch(dispatcherIO) {
            try {
                cultureUpdateResponse.postLoading()
                val response = metaRepository.cultureLocaleUpdate(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        cultureUpdateResponse.postSuccess(res.entityList)
                    } else {
                        cultureUpdateResponse.postError()
                    }
                } else {
                    cultureUpdateResponse.postError()
                }
            } catch (e: Exception) {
                cultureUpdateResponse.postError()
            }
        }
    }
}