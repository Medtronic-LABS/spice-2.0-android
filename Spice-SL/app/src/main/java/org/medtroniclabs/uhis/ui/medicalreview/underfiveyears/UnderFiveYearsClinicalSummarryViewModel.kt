package org.medtroniclabs.uhis.ui.medicalreview.underfiveyears

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.MeasurementDefinedParams
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.medicalreview.ClinicalSummaryAndSigns
import org.medtroniclabs.uhis.model.medicalreview.WazWhzScoreRequest
import org.medtroniclabs.uhis.model.medicalreview.WazWhzScoreResponse
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.repo.UnderFiveYearsRepository
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

@HiltViewModel
class UnderFiveYearsClinicalSummaryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: UnderFiveYearsRepository,
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
            clinicalSummaryAndSigns = if (vitaminA == DefinedParams.YES) {
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
            clinicalSummaryAndSigns = if (albendazole == DefinedParams.YES) {
                clinicalSummaryAndSigns.copy(albendazole = true)
            } else if (albendazole == DefinedParams.NO) {
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
        val temperatureInt = if (temperatureGiven) {
            null
        } else {
            temperature.toIntOrNull()
        }
        val temperatureUnit = if (temperatureGiven) null else MeasurementDefinedParams.Celsius
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
        val rateInpData = rate.toIntOrNull()
        val repeatInpData = repeatRate.toIntOrNull()
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            respirationRate = listOfNotNull(rateInpData, repeatInpData),
        )
    }

    fun updateMuac(
        muac: Double,
        context: Context,
    ) {
        val muacStatus = getMuacStatus(muac, context)
        clinicalSummaryAndSigns = clinicalSummaryAndSigns.copy(
            muacInCentimeter = muac,
            muacStatus = muacStatus,
        )
    }

    private fun getMuacStatus(
        muacValue: Double,
        context: Context,
    ): String? =
        if (muacValue <= DefinedParams.RED_MAX_MUAC) {
            context.getString(R.string.red)
        } else if (muacValue > DefinedParams.RED_MAX_MUAC && muacValue <= DefinedParams.YELLOW_MAX_MUAC) {
            context.getString(R.string.yellow)
        } else if (muacValue > DefinedParams.YELLOW_MAX_MUAC && muacValue <= DefinedParams.GREEN_MAX_MUAC) {
            context.getString(R.string.green)
        } else {
            null
        }

    fun getImmunisationStatusMetaItems() {
        viewModelScope.launch(dispatcherIO) {
            summaryMetaListItems.postLoading()
            summaryMetaListItems.postValue(
                repository.getImmunisationStatusMetaItems(
                    MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
                ),
            )
        }
    }

    fun getMuAcStatusMetaItems() {
        viewModelScope.launch(dispatcherIO) {
            summaryMuacMetaItems.postLoading()
            summaryMuacMetaItems.postValue(
                repository.getMuAcStatusMetaItems(
                    MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
                ),
            )
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
