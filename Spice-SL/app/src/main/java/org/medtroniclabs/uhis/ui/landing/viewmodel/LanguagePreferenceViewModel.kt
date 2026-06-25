package org.medtroniclabs.uhis.ui.landing.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.data.CulturesEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.CultureLocaleModel
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import javax.inject.Inject

@HiltViewModel
class LanguagePreferenceViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseViewModel(dispatcherIO) {
    var selectedCultureId: Long? = null

    val cultureList = MutableLiveData<Resource<List<CulturesEntity>>>()
    val cultureUpdateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()

    val selectedCultureForConfirmation = MutableLiveData<CulturesEntity?>()

    fun setSelectedCultureForConfirmation(culture: CulturesEntity?) {
        selectedCultureForConfirmation.value = culture
    }

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
