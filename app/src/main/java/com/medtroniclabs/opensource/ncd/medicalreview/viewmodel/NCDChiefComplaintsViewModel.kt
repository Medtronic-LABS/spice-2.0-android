package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.db.entity.NCDMedicalReviewMetaEntity
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.Complaints
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NCDChiefComplaintsViewModel @Inject constructor(
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository
) : ViewModel() {
    var comments: String = ""
    var chips: ArrayList<ChipViewItemModel> = ArrayList()
    private val getChip = MutableLiveData<String?>()
    val getChipItems: LiveData<List<NCDMedicalReviewMetaEntity>> = getChip.switchMap {
        ncdMedicalReviewRepository.getComorbiditiesBasedOnType(it, Complaints)
    }

    fun getChips(type: String?) {
        getChip.value = type
    }

}