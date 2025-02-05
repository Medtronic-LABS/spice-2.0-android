package com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.ExaminationComplaintsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemicExaminationViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO : CoroutineDispatcher,
    private var repository: ExaminationComplaintsRepository
): ViewModel() {

    var systemicExaminationsType : String = ""
    var selectedSystemicExaminations = ArrayList<ChipViewItemModel>()
    var enteredExaminationNotes = ""
    val systemicExaminationList = MutableLiveData<Resource<List<MedicalReviewMetaItems>>>()
    var fundalHeight:Double? = null
    var fetalHeartRate:Double? = null
    var isMotherPnc: Boolean = false
    fun getSystemicExaminationList(type: String) {
        viewModelScope.launch(dispatcherIO) {
            systemicExaminationList.postLoading()
            systemicExaminationList.postValue(repository.getComplaintsListByType(type))
        }
    }
}