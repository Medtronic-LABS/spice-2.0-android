package com.medtroniclabs.opensource.ncd.assessment.viewmodel

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.getBMIForNcd
import com.medtroniclabs.opensource.common.CommonUtils.getBMIInformation
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.db.entity.RiskFactorEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.model.BPModel
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.assessment.repo.BloodPressureRepo
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class BloodPressureViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val bloodPressureRepo: BloodPressureRepo
) : BaseViewModel(dispatcherIO) {
    var bpLogCreateResponseLiveData = SingleLiveEvent<Resource<APIResponse<HashMap<String, Any>>>>()
    var bpLogListResponseLiveData = SingleLiveEvent<Resource<BPBGListModel>>()

    private var systolicAverageSummary: Int? = null
    private var diastolicAverageSummary: Int? = null

    fun getSystolicAverage(): Int? {
        return systolicAverageSummary
    }

    fun getDiastolicAverage(): Int? {
        return diastolicAverageSummary
    }

    private val getRiskEntityList = MutableLiveData<Boolean>()
    val getRiskEntityListLiveData: LiveData<List<RiskFactorEntity>> = getRiskEntityList.switchMap {
        bloodPressureRepo.riskFactorListing()
    }

    fun getRiskEntityList() {
        getRiskEntityList.value = true
    }

    fun calculateBPValues(formLayout: FormLayout, resultMap: Map<String, Any>) {
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
                        actualMapList, systolicEntries, diastolicEntries, systolic, diastolic
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
        diastolic: Double
    ) {
        if (actualMapList.size > 0 && systolicEntries > 0 && diastolicEntries > 0) {
            systolicAverageSummary = (systolic / systolicEntries).roundToInt()
            diastolicAverageSummary = (diastolic / diastolicEntries).roundToInt()
        }
    }

    private fun validateMap(map: Any?, value: String): Double? {
        return if (map is Map<*, *> && map.containsKey(value)) (map[value] as String).toDoubleOrNull() else null
    }

    fun renderBMIValue(
        context: Context,
        formGenerator: FormGenerator,
        resultHashMap: HashMap<String, Any>
    ) {
        val bmiView = formGenerator.getViewByTag(Screening.BMI) as? AppCompatTextView
        bmiView?.let { view ->
            if (!resultHashMap.containsKey(Screening.Weight) || !resultHashMap.containsKey(Screening.Height)) {
                view.text = context.getString(R.string.hyphen_symbol)
                formGenerator.removeIfContains(Screening.BMI)
            } else {
                if (resultHashMap.containsKey(Screening.Weight) && resultHashMap.containsKey(
                        Screening.Height
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
                                    SpannableStringBuilder().append(bmi)
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
        menuId: String?
    ) {
        hashMap.apply {
            with(patientDetails) {
                NCDMRUtil.getBioDataBioMetrics(
                    hashMap,
                    this,
                    hashMap[Screening.Height]?.toString()?.toDoubleOrNull(),
                    hashMap[Screening.Weight]?.toString()?.toDoubleOrNull()
                )
                id?.let { requestRelatedPersonFhirId ->
                    put(DefinedParams.RelatedPersonFhirId, requestRelatedPersonFhirId)
                }
                patientId?.let { requestPatientId ->
                    put(DefinedParams.PATIENT_ID, requestPatientId)
                }
            }
            put(AssessmentDefinedParams.assessmentProcessType, CommonUtils.requestFrom())
            put(DefinedParams.AssessmentOrganizationId, SecuredPreference.getOrganizationFhirId())
            put(DefinedParams.Provenance, ProvanceDto())
        }
        viewModelScope.launch(dispatcherIO) {
            bpLogCreateResponseLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDBloodPressureCreation + " " + menuId,
                isCompleted = true
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
            sortOrder = -1 //Desc
        }
        viewModelScope.launch(dispatcherIO) {
            bpLogListResponseLiveData.postLoading()
            bpLogListResponseLiveData.postValue(bloodPressureRepo.bpLogList(request))
        }
    }
}