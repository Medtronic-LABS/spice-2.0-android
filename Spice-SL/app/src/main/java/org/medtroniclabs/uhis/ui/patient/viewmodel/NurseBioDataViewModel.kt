package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.common.CVDRiskCalculator
import org.medtroniclabs.uhis.common.PatientStatusEvaluator
import org.medtroniclabs.uhis.data.medicalreview.ReqBPBGLogList
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.db.entity.RiskClassificationModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.PatientDetailRequest
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import java.lang.reflect.Type
import javax.inject.Inject

@HiltViewModel
class NurseBioDataViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) :
    ViewModel() {
        @Inject
        lateinit var connectivityManager: ConnectivityManager

        val patientDetailsResponse = MutableLiveData<Resource<PatientDetailsModel>>()
        var isSummary: Boolean = false

        // Holds the locally computed CVD risk used when the backend payload doesn't carry a score.
        val cvdRiskResult = MutableLiveData<CVDRiskCalculator.CVDRiskResult>()
        private var cvdRiskComputed = false

        // Holds the locally derived Controlled / Uncontrolled patient status.
        val patientStatusResult = MutableLiveData<PatientStatusEvaluator.ControlStatus>()
        private var patientStatusComputed = false

        fun getPatientDetails(
            context: Context,
            request: PatientDetailsModel,
        ) {
            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    patientDetailsResponse.postLoading()
                    try {
                        val response = medicalReviewRepo.getPatientDetails(PatientDetailRequest(patientId = null))
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true) {
                                patientDetailsResponse.postSuccess(res.entity)
                            } else {
                                patientDetailsResponse.postError()
                            }
                        } else {
                            patientDetailsResponse.postError()
                        }
                    } catch (e: Exception) {
                        patientDetailsResponse.postError()
                    }
                }
            } else {
                patientDetailsResponse.setError(context.getString(R.string.no_internet_error))
            }
        }

        /**
         * Computes the CVD risk on the device when the backend doesn't return a pre-computed score.
         * Systolic BP is the average across all readings in the patient's bplog list; the remaining
         * inputs (age, gender, bmi, smoker status) come from the patient details payload, and the
         * risk classification table is read from local storage.
         */
        fun computeCvdRiskIfNeeded(details: PatientDetailsModel) {
            if (cvdRiskComputed || details.cvdRiskScore != null) return
            val memberId = details.memberId ?: return
            cvdRiskComputed = true
            viewModelScope.launch(dispatcherIO) {
                try {
                    val avgSystolic = fetchAverageSystolic(memberId)
                    if (avgSystolic == null) {
                        cvdRiskComputed = false
                        return@launch
                    }
                    val riskList = loadRiskClassificationModels()
                    if (riskList.isEmpty()) {
                        cvdRiskComputed = false
                        return@launch
                    }
                    CVDRiskCalculator
                        .calculateCVDRiskScore(
                            list = riskList,
                            age = details.age,
                            gender = details.gender,
                            bmiValue = details.bmi,
                            avgSystolic = avgSystolic,
                            isSmoker = details.isRegularSmoker == true,
                        )?.let { cvdRiskResult.postValue(it) }
                } catch (e: Exception) {
                    cvdRiskComputed = false
                }
            }
        }

        private suspend fun fetchAverageSystolic(memberId: String): Int? {
            val response = medicalReviewRepo.getPatientBPLogList(ReqBPBGLogList(memberId = memberId))
            if (!response.isSuccessful) return null
            val body = response.body()
            if (body?.status != true) return null
            val readings = body.entity?.bpLogList?.mapNotNull { it.avgSystolic } ?: return null
            if (readings.isEmpty()) return null
            return readings.average().toInt()
        }

        /**
         * Derives the Controlled / Uncontrolled status on the device. The latest BP reading and the
         * last two blood-glucose readings are fetched from the patient's logs; the remaining inputs
         * (diagnoses, comorbidities, complications and the medication-adherence answer) are resolved
         * by the caller from the patient details and the current review. Posts a value only when a
         * status can be determined; otherwise the caller keeps the existing hyphen.
         */
        fun computePatientStatusIfNeeded(
            memberId: String?,
            hasComorbidities: Boolean,
            hasComplications: Boolean,
            takingMedication: Boolean?,
        ) {
            if (patientStatusComputed || memberId == null) return
            patientStatusComputed = true
            viewModelScope.launch(dispatcherIO) {
                try {
                    val latestBp = fetchLatestBp(memberId)
                    val lastTwoGlucose = fetchLastTwoGlucose(memberId)
                    val status =
                        PatientStatusEvaluator.evaluate(
                            latestSystolic = latestBp?.first,
                            latestDiastolic = latestBp?.second,
                            lastTwoGlucose = lastTwoGlucose,
                            hasComorbidities = hasComorbidities,
                            hasComplications = hasComplications,
                            takingMedication = takingMedication,
                        )
                    if (status != null) {
                        patientStatusResult.postValue(status)
                    } else {
                        patientStatusComputed = false
                    }
                } catch (e: Exception) {
                    patientStatusComputed = false
                }
            }
        }

        private suspend fun fetchLatestBp(memberId: String): Pair<Int, Int>? {
            val response = medicalReviewRepo.getPatientBPLogList(ReqBPBGLogList(memberId = memberId))
            if (!response.isSuccessful) return null
            val body = response.body()
            if (body?.status != true) return null
            val latest =
                body.entity?.latestBpLog ?: body.entity?.bpLogList?.firstOrNull() ?: return null
            return latest.avgSystolic.toInt() to latest.avgDiastolic.toInt()
        }

        private suspend fun fetchLastTwoGlucose(memberId: String): List<PatientStatusEvaluator.GlucoseReading> {
            val response =
                medicalReviewRepo.getPatientBloodGlucoseList(ReqBPBGLogList(memberId = memberId))
            if (!response.isSuccessful) return emptyList()
            val body = response.body()
            if (body?.status != true) return emptyList()
            return body.entity?.glucoseLogList?.take(2)?.map {
                PatientStatusEvaluator.GlucoseReading(it.glucoseType, it.glucoseValue?.toDoubleOrNull())
            } ?: emptyList()
        }

        private suspend fun loadRiskClassificationModels(): ArrayList<RiskClassificationModel> {
            val riskFactors = medicalReviewRepo.riskFactorListing()
            if (riskFactors.isEmpty()) return arrayListOf()
            val baseType: Type = object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
            return Gson().fromJson<ArrayList<RiskClassificationModel>>(
                riskFactors[0].nonLabEntity,
                baseType,
            ) ?: arrayListOf()
        }
    }
