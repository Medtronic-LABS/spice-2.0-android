package com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel

import com.medtroniclabs.opensource.model.medicalreview.ClinicalSummaryAndSigns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.MeasurementDefinedParams
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreResponse
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.UnderTwoMonthsRepository
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClinicalSummaryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: UnderTwoMonthsRepository
) : ViewModel() {
    val resultBreastFeedingHashMap = HashMap<String, Any>()
    val resultMotherVitaminHashMap = HashMap<String, Any>()
    val resultExclusiveBreastFeedHashMap = HashMap<String, Any>()
    var selectedImmunisationStatus: String? = null
    var clinicalSummaryAndSigns = ClinicalSummaryAndSigns()
    val summaryMetaListItems = MutableLiveData<Resource<List<MedicalReviewMetaItems>>>()
    val wazWhzScoreResponseLiveData = MutableLiveData<Resource<WazWhzScoreResponse>>()


    fun updateWeight(weight: String) {
        val isEmpty = weight.isEmpty()
        val weightDouble = if (isEmpty) null else weight.toDoubleOrNull()
        val weightUnit = if (isEmpty) null else MeasurementDefinedParams.Kilogram
        clinicalSummaryAndSigns =
            clinicalSummaryAndSigns.copy(weight = weightDouble, weightUnit = weightUnit)
    }

    fun updateVitaminAForMother() {
        if (resultMotherVitaminHashMap.containsKey(MedicalReviewDefinedParams.MOTHER_VITAMIN_TAG)) {
            val vitaminA =
                resultMotherVitaminHashMap[MedicalReviewDefinedParams.MOTHER_VITAMIN_TAG] as String
            clinicalSummaryAndSigns = if (vitaminA == DefinedParams.Yes) {
                clinicalSummaryAndSigns.copy(vitAForMother = true)
            } else {
                clinicalSummaryAndSigns.copy(vitAForMother = false)
            }
        }
    }

    fun updateBreastFeeding() {
        if (resultBreastFeedingHashMap.containsKey(MedicalReviewDefinedParams.BREAST_FEEDING_TAG)) {
            val breastFeeding =
                resultBreastFeedingHashMap[MedicalReviewDefinedParams.BREAST_FEEDING_TAG] as String
            clinicalSummaryAndSigns = if (breastFeeding == DefinedParams.Yes) {
                clinicalSummaryAndSigns.copy(breastFeeding = true)
            } else if (breastFeeding == DefinedParams.No) {
                clinicalSummaryAndSigns.copy(breastFeeding = false)
            } else {
                clinicalSummaryAndSigns.copy(breastFeeding = null)
            }
        }
    }

    fun updateExclusiveBreastFeeding() {
        if (resultExclusiveBreastFeedHashMap.containsKey(MedicalReviewDefinedParams.EXCLUSIVE_BREAST_FEED_TAG)) {
            val exclusiveBreastFeeding =
                resultExclusiveBreastFeedHashMap[MedicalReviewDefinedParams.EXCLUSIVE_BREAST_FEED_TAG] as String
            clinicalSummaryAndSigns = if (exclusiveBreastFeeding == DefinedParams.Yes) {
                clinicalSummaryAndSigns.copy(exclusiveBreastFeeding = true)
            } else if (exclusiveBreastFeeding == DefinedParams.No) {
                clinicalSummaryAndSigns.copy(exclusiveBreastFeeding = false)
            } else {
                clinicalSummaryAndSigns.copy(exclusiveBreastFeeding = null)
            }
        }
    }

    fun updateHeight(height: String) {
        val isEmpty = height.isEmpty()
        val heightDouble = if (isEmpty) null else height.toDoubleOrNull()
        val heightUnit = if (isEmpty) null else MeasurementDefinedParams.Centimeter
        clinicalSummaryAndSigns =
            clinicalSummaryAndSigns.copy(height = heightDouble, heightUnit = heightUnit)
    }

    fun updateWaz(waz: String) {
        val wazDouble = if (waz.isEmpty()) null else waz.toDoubleOrNull()
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(waz = wazDouble)
    }

    fun updateWhz(whz: String) {
        val whzDouble = if (whz.isEmpty()) null else whz.toDoubleOrNull()
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(whz = whzDouble)
    }

    fun updateTemperature(temperature: String) {
        val isEmpty = temperature.isEmpty()
        val temperatureInt = if (isEmpty) null else {
            temperature.toIntOrNull()
        }
        val temperatureUnit = if (isEmpty) null else MeasurementDefinedParams.Celsius
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            temperature = temperatureInt,
            temperatureUnit = temperatureUnit
        )
    }

    fun updateImmunisationStatus() {
        clinicalSummaryAndSigns =
            clinicalSummaryAndSigns.copy(immunisationStatus = selectedImmunisationStatus)
    }

    fun updateRespiratoryRate(rate: String, repeatRate: String) {
        val rateInt = rate.toIntOrNull()
        val repeatInt = repeatRate.toIntOrNull()
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            respirationRate = listOfNotNull(rateInt, repeatInt)
        )
    }

    fun getImmunisationStatusMetaItems() {
        viewModelScope.launch(dispatcherIO) {
            summaryMetaListItems.postLoading()
            summaryMetaListItems.postValue(repository.getImmunisationStatusMetaItems(MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name))
        }
    }

    fun getWazWhzScore(request: WazWhzScoreRequest) {
        viewModelScope.launch(dispatcherIO) {
            wazWhzScoreResponseLiveData.postLoading()
            wazWhzScoreResponseLiveData.postValue(
                repository.getWazWhzScore(request)
            )
        }
    }

}