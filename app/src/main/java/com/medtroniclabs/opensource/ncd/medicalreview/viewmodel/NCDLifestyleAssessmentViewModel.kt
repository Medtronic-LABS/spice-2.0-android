package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.db.entity.LifeStyleUIModel
import com.medtroniclabs.opensource.db.entity.LifestyleEntity
import com.medtroniclabs.opensource.ncd.data.InitialLifeStyle
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NCDLifestyleAssessmentViewModel @Inject constructor(
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository
) : ViewModel() {

    var lifestyle: ArrayList<InitialLifeStyle>? = null
    private val getLifestyleAssessment = MutableLiveData<Boolean>()
    val getLifeStyleLiveData: LiveData<List<LifestyleEntity>> = getLifestyleAssessment.switchMap {
        ncdMedicalReviewRepository.getLifeStyle()
    }
    var lifeStyleListUIModel: List<LifeStyleUIModel>? = null

    fun getLifestyleAssessment(isTrigger: Boolean) {
        getLifestyleAssessment.value = isTrigger
    }
}