package org.medtroniclabs.uhis.ncd.assessment.viewmodel

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.getBMIForNcd
import org.medtroniclabs.uhis.common.CommonUtils.getBMIInformation
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.db.entity.RiskFactorEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.model.BPModel
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.ncd.assessment.repo.BloodPressureRepo
import org.medtroniclabs.uhis.ncd.data.BPBGListModel
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.SingleLiveEvent
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class BloodPressureViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val bloodPressureRepo: BloodPressureRepo,
) : BaseViewModel(dispatcherIO) {
    var bpLogCreateResponseLiveData = SingleLiveEvent<Resource<APIResponse<HashMap<String, Any>>>>()
    var bpLogListResponseLiveData = SingleLiveEvent<Resource<BPBGListModel>>()

    private var systolicAverageSummary: Int? = null
    private var diastolicAverageSummary: Int? = null

    fun getSystolicAverage(): Int? = systolicAverageSummary

    fun getDiastolicAverage(): Int? = diastolicAverageSummary

    private val getRiskEntityList = MutableLiveData<Boolean>()
    val getRiskEntityListLiveData: LiveData<List<RiskFactorEntity>> = getRiskEntityList.switchMap {
        bloodPressureRepo.riskFactorListing()
    }

    fun getRiskEntityList() {
        getRiskEntityList.value = true
    }

    fun calculateBPValues(
        formLayout: FormLayout,
        resultMap: Map<String, Any>,
    ) {
        formLayout.apply {
            var systolic = 0.0
            var diastolic = 0.0
            if (resultMap.containsKey(id)) {
                val actualMapList = resultMap[id]
                if (actualMapList is java.util.ArrayList<*>) {
                    var systolicEntries = 0
                    var diastolicEntries = 0
                    actualMapList.forEach { map ->
                        if (map is BPModel) {
                            map.systolic?.let {
                                systolic += it
                                systolicEntries++
                            }
                            map.diastolic?.let {
                                diastolic += it
                                diastolicEntries++
                            }
                        } else {
                            validateMap(map, Screening.Systolic)?.let {
                                systolic += it
                                systolicEntries++
                            }
                            validateMap(map, Screening.Diastolic)?.let {
                                diastolic += it
                                diastolicEntries++
                            }
                        }
                    }
                    updateAverage(
                        actualMapList,
                        systolicEntries,
                        diastolicEntries,
                        systolic,
                        diastolic,
                    )
                }
            }
        }
    }

    private fun updateAverage(
        actualMapList: java.util.ArrayList<*>,
        systolicEntries: Int,
        diastolicEntries: Int,
        systolic: Double,
        diastolic: Double,
    ) {
        if (actualMapList.size > 0 && systolicEntries > 0 && diastolicEntries > 0) {
            systolicAverageSummary = (systolic / systolicEntries).roundToInt()
            diastolicAverageSummary = (diastolic / diastolicEntries).roundToInt()
        }
    }

    private fun validateMap(
        map: Any?,
        value: String,
    ): Double? = if (map is Map<*, *> && map.containsKey(value)) (map[value] as String).toDoubleOrNull() else null

    fun renderBMIValue(
        context: Context,
        formGenerator: FormGenerator,
        resultHashMap: HashMap<String, Any>,
    ) {
        val bmiView = formGenerator.getViewByTag(Screening.BMI) as? AppCompatTextView
        bmiView?.let { view ->
            if (!resultHashMap.containsKey(Screening.Weight) || !resultHashMap.containsKey(Screening.Height)) {
                view.text = context.getString(R.string.hyphen_symbol)
                formGenerator.removeIfContains(Screening.BMI)
            } else {
                if (resultHashMap.containsKey(Screening.Weight) &&
                    resultHashMap.containsKey(
                        Screening.Height,
                    )
                ) {
                    val weight = resultHashMap[Screening.Weight] as? Double
                    val height = resultHashMap[Screening.Height] as? Double

                    if (weight == null || height == null) {
                        view.text = context.getString(R.string.hyphen_symbol)
                    } else {
                        val bmi = getBMIForNcd(height, weight)
                        getBMIInformation(context, bmi?.toDoubleOrNull())
                            ?.let { info ->
                                resultHashMap[Screening.BMI_CATEGORY] = info.first

                                val bmiWithInfoSpannableStringBuilder = if (bmi == null) {
                                    context.getString(R.string.hyphen_symbol)
                                } else {
                                    SpannableStringBuilder()
                                        .append(bmi)
                                        .color(context.getColor(info.second)) {
                                            append(" (${info.first})")
                                        }
                                }
                                view.text = bmiWithInfoSpannableStringBuilder
                            }
                    }
                }
            }
        }
    }

    fun createBpLog(
        hashMap: HashMap<String, Any>,
        patientDetails: PatientListRespModel,
        menuId: String?,
    ) {
        hashMap.apply {
            with(patientDetails) {
                NCDMRUtil.getBioDataBioMetrics(
                    hashMap,
                    this,
                    hashMap[Screening.Height]?.toString()?.toDoubleOrNull(),
                    hashMap[Screening.Weight]?.toString()?.toDoubleOrNull(),
                )
                id?.let { requestRelatedPersonFhirId ->
                    put(DefinedParams.RELATED_PERSON_FHIR_ID, requestRelatedPersonFhirId)
                }
                patientId?.let { requestPatientId ->
                    put(DefinedParams.PATIENT_ID, requestPatientId)
                }
            }
            put(AssessmentDefinedParams.assessmentProcessType, CommonUtils.requestFrom())
            put(DefinedParams.ASSESSMENT_ORGANIZATION_ID, SecuredPreference.getOrganizationFhirId())
            put(DefinedParams.Provenance, ProvanceDto())
        }
        viewModelScope.launch(dispatcherIO) {
            bpLogCreateResponseLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDBloodPressureCreation + " " + menuId,
                isCompleted = true,
            )
            bpLogCreateResponseLiveData.postValue(bloodPressureRepo.createBpLog(hashMap))
        }
    }

    fun bpLogList(patientId: String) {
        val request = BPBGListModel().apply {
            limit = 10
            skip = 0
            memberId = patientId
            latestRequired = true
            sortOrder = -1 // Desc
        }
        viewModelScope.launch(dispatcherIO) {
            bpLogListResponseLiveData.postLoading()
            bpLogListResponseLiveData.postValue(bloodPressureRepo.bpLogList(request))
        }
    }
}
