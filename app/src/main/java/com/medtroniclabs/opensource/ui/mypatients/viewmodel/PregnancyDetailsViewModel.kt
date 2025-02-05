package com.medtroniclabs.opensource.ui.mypatients.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo.MotherNeonateANCRepo
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class PregnancyDetailsViewModel @Inject constructor(
    private val motherNeonateANCRepo: MotherNeonateANCRepo,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {

    val pregnancyDetailsModel: PregnancyDetailsModel = PregnancyDetailsModel()
    val checkSubmitBtn = MutableLiveData<Boolean>()
    private val getAncMetaForBloodGroup = MutableLiveData<String>()
    val ancMetaLiveDataForBloodGroup: LiveData<List<MedicalReviewMetaItems>> =
        getAncMetaForBloodGroup.switchMap {
            motherNeonateANCRepo.getExaminationsComplaintsForAnc(it, MedicalReviewTypeEnums.ANC_REVIEW.name)
        }
    fun checkSubmitBtn() {
        checkSubmitBtn.value = true
    }
    fun setAncReqToGetMetaForBloodGroup(category: String) {
        getAncMetaForBloodGroup.value = category
    }
}