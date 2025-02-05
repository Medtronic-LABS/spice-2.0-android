package com.medtroniclabs.opensource.ui.medicalreview.underfiveyears

import android.content.Context
import com.medtroniclabs.opensource.model.medicalreview.ClinicalSummaryAndSigns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.MeasurementDefinedParams
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreResponse
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.UnderFiveYearsRepository
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnderFiveYearsClinicalSummaryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: UnderFiveYearsRepository
) : ViewModel() {

    val resultMotherVitaminHashMap = HashMap<String, Any>()
    val albendazoleHashMap = HashMap<String, Any>()
    var selectedImmunisationStatus: String? = null
    var clinicalSummaryAndSigns = ClinicalSummaryAndSigns()
    val summaryMetaListItems = MutableLiveData<Resource<List<MedicalReviewMetaItems>>>()
    val summaryMuacMetaItems = MutableLiveData<Resource<List<MedicalReviewMetaItems>>>()
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

    fun updateAlbendazole() {
        if (albendazoleHashMap.containsKey(MedicalReviewDefinedParams.Albendazole)) {
            val albendazole =
                albendazoleHashMap[MedicalReviewDefinedParams.Albendazole] as String
            clinicalSummaryAndSigns = if (albendazole == DefinedParams.Yes) {
                clinicalSummaryAndSigns.copy(albendazole = true)
            } else if (albendazole == DefinedParams.No) {
                clinicalSummaryAndSigns.copy(albendazole = false)
            } else {
                clinicalSummaryAndSigns.copy(albendazole = null)
            }
        }
    }

    fun updateHeight(height: String) {
        val heightGiven = height.isEmpty()
        val heightDouble = if (heightGiven) null else height.toDoubleOrNull()
        val heightUnit = if (heightGiven) null else MeasurementDefinedParams.Centimeter
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
        val temperatureGiven = temperature.isEmpty()
        val temperatureInt = if (temperatureGiven) null else {
            temperature.toIntOrNull()
        }
        val temperatureUnit = if (temperatureGiven) null else MeasurementDefinedParams.Celsius
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
        val rateInpData = rate.toIntOrNull()
        val repeatInpData = repeatRate.toIntOrNull()
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            respirationRate = listOfNotNull(rateInpData, repeatInpData)
        )
    }

    fun updateMuac(muac: Double, context: Context) {
        val muacStatus = getMuacStatus(muac, context)
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            muacInCentimeter = muac,
            muacStatus = muacStatus
        )
    }
    private fun getMuacStatus(muacValue: Double, context: Context): String? {
        return if (muacValue <= DefinedParams.RED_MAX_MUAC) {
            context.getString(R.string.red)
        } else if (muacValue > DefinedParams.RED_MAX_MUAC && muacValue <= DefinedParams.YELLOW_MAX_MUAC) {
            context.getString(R.string.yellow)
        } else if (muacValue > DefinedParams.YELLOW_MAX_MUAC && muacValue <= DefinedParams.GREEN_MAX_MUAC) {
            context.getString(R.string.green)
        } else {
            null
        }
    }

    fun getImmunisationStatusMetaItems() {
        viewModelScope.launch(dispatcherIO) {
            summaryMetaListItems.postLoading()
            summaryMetaListItems.postValue(
                repository.getImmunisationStatusMetaItems(
                    MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
                )
            )
        }
    }

    fun getMuAcStatusMetaItems() {
        viewModelScope.launch(dispatcherIO) {
            summaryMuacMetaItems.postLoading()
            summaryMuacMetaItems.postValue(
                repository.getMuAcStatusMetaItems(
                    MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
                )
            )
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