package org.medtroniclabs.uhis.ui.patientDelete.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.ncd.data.NCDPatientRemoveRequest
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.repo.NCDMedicalReviewRepository
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.mypatients.repo.PatientRepository
import javax.inject.Inject

@HiltViewModel
class NCDPatientDeleteViewModel @Inject constructor(
    private var ncdMedicalReviewRepo: NCDMedicalReviewRepository,
    private val patientRepository: PatientRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseViewModel(dispatcherIO) {
    val deleteReasonList = MutableLiveData<List<ShortageReasonEntity>>()
    val patientRemoveResponse = MutableLiveData<Resource<Boolean>>()

    fun getDeleteReasonList() {
        viewModelScope.launch(dispatcherIO) {
            val deleteList = ncdMedicalReviewRepo.getNCDShortageReason(NCDMRUtil.TYPE_DELETE)
            val list = ArrayList(deleteList)
            if (list.isNotEmpty()) {
                val itemIndex =
                    list.indexOfFirst { it.name.contains(DefinedParams.OTHER, ignoreCase = true) }
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
                isCompleted = true,
            )
            patientRemoveResponse.postValue(patientRepository.ncdPatientRemove(request))
        }
    }
}
