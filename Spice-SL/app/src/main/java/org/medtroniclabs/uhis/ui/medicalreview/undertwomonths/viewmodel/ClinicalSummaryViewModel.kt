package org.medtroniclabs.uhis.ui.medicalreview.undertwomonths.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.MeasurementDefinedParams
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.medicalreview.ClinicalSummaryAndSigns
import org.medtroniclabs.uhis.model.medicalreview.WazWhzScoreRequest
import org.medtroniclabs.uhis.model.medicalreview.WazWhzScoreResponse
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.repo.UnderTwoMonthsRepository
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

@HiltViewModel
class ClinicalSummaryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: UnderTwoMonthsRepository,
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
            clinicalSummaryAndSigns = if (vitaminA == DefinedParams.YES) {
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
            clinicalSummaryAndSigns = if (breastFeeding == DefinedParams.YES) {
                clinicalSummaryAndSigns.copy(breastFeeding = true)
            } else if (breastFeeding == DefinedParams.NO) {
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
            clinicalSummaryAndSigns = if (exclusiveBreastFeeding == DefinedParams.YES) {
                clinicalSummaryAndSigns.copy(exclusiveBreastFeeding = true)
            } else if (exclusiveBreastFeeding == DefinedParams.NO) {
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
        val temperatureInt = if (isEmpty) {
            null
        } else {
            temperature.toIntOrNull()
        }
        val temperatureUnit = if (isEmpty) null else MeasurementDefinedParams.Celsius
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            temperature = temperatureInt,
            temperatureUnit = temperatureUnit,
        )
    }

    fun updateImmunisationStatus() {
        clinicalSummaryAndSigns =
            clinicalSummaryAndSigns.copy(immunisationStatus = selectedImmunisationStatus)
    }

    fun updateRespiratoryRate(
        rate: String,
        repeatRate: String,
    ) {
        val rateInt = rate.toIntOrNull()
        val repeatInt = repeatRate.toIntOrNull()
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            respirationRate = listOfNotNull(rateInt, repeatInt),
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
                repository.getWazWhzScore(request),
            )
        }
    }
}
