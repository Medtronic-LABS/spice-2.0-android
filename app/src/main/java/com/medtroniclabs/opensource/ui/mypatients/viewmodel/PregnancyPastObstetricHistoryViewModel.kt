package com.medtroniclabs.opensource.ui.mypatients.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo.MotherNeonateANCRepo
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class PregnancyPastObstetricHistoryViewModel@Inject constructor(
    private val motherNeonateANCRepo: MotherNeonateANCRepo,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {
    var pregnancyHistoryNotes: String? = null
    val resultFlowHashMap = HashMap<String, Any>()
    var pregnancyHistoryOther :ChipViewItemModel? = null
    var deliveryKit: Boolean? = null
    var checkSubmitBtn = MutableLiveData<Boolean>()
    var pregnancyHistoryChip:ArrayList<ChipViewItemModel> = ArrayList()
    private val getAncMetaForPregnancyHistory = MutableLiveData<String>()
    val ancMetaLiveDataForPregnancyHistory: LiveData<List<MedicalReviewMetaItems>> = getAncMetaForPregnancyHistory.switchMap {
        motherNeonateANCRepo.getExaminationsComplaintsForAnc(it, MedicalReviewTypeEnums.ANC_REVIEW.name)
    }

    fun setAncReqToGetMetaForPregnancyHistory(category: String) {
        getAncMetaForPregnancyHistory.value = category
    }

    fun checkSubmitBtn() {
        checkSubmitBtn.value = true
    }

}