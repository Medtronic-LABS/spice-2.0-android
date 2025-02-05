package com.medtroniclabs.opensource.ui.medicalreview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.PhysicalExaminationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PhysicalExaminationViewModel @Inject constructor(
    private var repository: PhysicalExaminationRepository,
) : ViewModel() {

    var congenitalDefect: Boolean?=null
    var exclusiveBreastFeeding: Boolean?=null
    var breastFeeding: Boolean?=null
    var selectedSystemicExaminations = ArrayList<ChipViewItemModel>()
    var cordExaminationMap = HashMap<String, Any>()
    val congenitalDefectMap = HashMap<String, Any>()
    val breastCondition = HashMap<String, Any>()
    val exclusiveBreastCondition = HashMap<String, Any>()
    private val systematicType = MutableLiveData<String>()

    val systemicExaminationListLiveData: LiveData<List<MedicalReviewMetaItems>> =
        systematicType.switchMap {
            val result = repository.getExaminationsComplaintByTypeLiveData(it)
            result

        }

    fun setType(category: String) {
        systematicType.value = category
    }
}