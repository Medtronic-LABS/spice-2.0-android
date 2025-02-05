package com.medtroniclabs.opensource.ui.medicalreview.examinations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.medtroniclabs.opensource.data.ExaminationModel
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.repo.ExaminationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExaminationCardViewModel @Inject constructor(
    @IoDispatcher val dispatcherIO: CoroutineDispatcher,
    private val examinationsRepository: ExaminationsRepository
) : ViewModel() {

    private val examinationQuestionsMutableLiveData = MutableLiveData<ArrayList<ExaminationModel>>()
    val examinationQuestionsLiveData: LiveData<ArrayList<ExaminationModel>>
        get() = examinationQuestionsMutableLiveData

    var examinationResultHashMap = HashMap<String, Any>()
    var workFlowType: String =""

    fun getExaminationQuestionsByWorkFlow(workFlowType: String) {
        viewModelScope.launch(dispatcherIO) {
            val examinationListItem =
                examinationsRepository.getExaminationQuestionsByWorkFlow(workFlowType)
            examinationListItem?.formInput?.let {
                val examinationModelList = Gson().fromJson(
                    it,
                    Array<ExaminationModel>::class.java
                ).asList()
                examinationQuestionsMutableLiveData.postValue(ArrayList(examinationModelList))
            }
        }
    }

    fun mapResultMapToExaminationModel() {

    }
}