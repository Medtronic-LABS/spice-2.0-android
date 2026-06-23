package org.medtroniclabs.uhis.ui.mypatients.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.PatientDetailRequest
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.model.PregnancyDetails
import org.medtroniclabs.uhis.ncd.data.NCDInstructionModel
import org.medtroniclabs.uhis.ncd.data.NCDPregnancyRiskUpdate
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.repo.NCDMedicalReviewRepository
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.mypatients.repo.PatientRepository
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository,
) : BaseViewModel(dispatcherIO) {
    var dateOfDelivery: String? = null
    var childPatientDetails: String? = null
    val updatePregnancyRisk = MutableLiveData<Resource<Boolean>>()
    val ncdInstructionModelResponse = MutableLiveData<Resource<NCDInstructionModel>>()
    val patientDetailsLiveData = MutableLiveData<Resource<PatientListRespModel>>()
    val patientDetailsNeonateLiveData = MutableLiveData<Resource<PatientListRespModel>>()
    var isFamilyPlanning: Boolean = false

    // the below id is one which get from patient details response
    var patientDetailsId: String? = null
    var isSummary: Boolean = false

    var encounterId: String? = null
    var childEncounterId: String? = null

    var origin: String? = null
    var mrMenuId: String? = null
    var isCmr: Boolean = false
    var forceRefresh: Boolean = false

    var neonateOutCome: String? = null

    var chwName: String? = null
    var occupation: String? = null
    var presumptiveTbNo: String? = null
    var maritalStatus: String? = null
    val clinicalWorkflowsMenusLiveData = MutableLiveData<List<ClinicalWorkflowEntity>>()
    var artCode: String? = null
    var patientCurrentStatus = MutableLiveData<String>()

    var isEmtctFlow: Boolean = false
    var hivTestedPositive: Boolean = false

    fun getPatients(
        id: String,
        assessmentType: String? = null,
        origin: String? = null,
        patientId: String? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            patientDetailsLiveData.postLoading()
            patientDetailsLiveData.postValue(
                patientRepository.getPatients(PatientDetailRequest(patientId = patientId ?: id, assessmentType = assessmentType, id = id, type = origin)),
            )
        }
    }

    fun getPatientsForNCD(id: String) {
        viewModelScope.launch(dispatcherIO) {
            patientDetailsLiveData.postLoading()
            patientDetailsLiveData.postValue(patientRepository.getPatientBasedOnId(id))
        }
    }

    fun getLastRefillVisitId(): String? =
        patientDetailsLiveData.value
            ?.data
            ?.prescribedDetails
            ?.lastRefillVisitId

    fun getPatientFHIRId(): String? = patientDetailsLiveData.value?.data?.id

    fun getPatientVillageId(): String? = patientDetailsLiveData.value?.data?.villageId

    fun getPatientMemberId(): String? = patientDetailsLiveData.value?.data?.memberId

    fun getPatientId(): String? = patientDetailsLiveData.value?.data?.patientId

    fun getEnrollmentType(): String? = patientDetailsLiveData.value?.data?.enrollmentType

    fun getIdentityValue(): String? = patientDetailsLiveData.value?.data?.identityValue

    fun getChildPatientName(): String? =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.neonatePatientId

    fun getPatientHouseholdId(): String? = patientDetailsLiveData.value?.data?.houseHoldId

    fun getAncVisit(): Int =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.ancVisitMedicalReview
            ?.takeIf { true }
            ?.plus(1)
            ?: 1

    fun getPncVisit(): Int =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.pncVisitMedicalReview
            ?.takeIf { true }
            ?.plus(1)
            ?: 1

    fun getPatientLmb(): String? =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.lastMenstrualPeriod

    fun getVillageId(): String? = patientDetailsLiveData.value?.data?.villageId

    fun getNCDInitialMedicalReview(): Boolean = patientDetailsLiveData.value?.data?.initialReviewed ?: false

    fun getGenderIsFemale(): Boolean =
        patientDetailsLiveData.value
            ?.data
            ?.gender
            ?.equals(DefinedParams.GENDER_FEMALE, ignoreCase = true) == true

    fun getGender(): String? = patientDetailsLiveData.value?.data?.gender

    fun isPatientEnrolled(): Boolean =
        !patientDetailsLiveData.value
            ?.data
            ?.programId
            .isNullOrBlank()

    fun recentBP(): String {
        val avgBP = patientDetailsLiveData.value?.data?.avgBloodPressure
        return if (avgBP.isNullOrEmpty()) {
            ""
        } else {
            "$avgBP ${NCDMRUtil.mmHg}"
        }
    }

    fun recentGlucose(): String {
        val glucoseValue = patientDetailsLiveData.value?.data?.glucoseValue
        val glucoseUnit = patientDetailsLiveData.value?.data?.glucoseUnit
        return if (glucoseValue.isNullOrEmpty() || glucoseUnit.isNullOrEmpty()) {
            ""
        } else {
            "$glucoseValue ($glucoseUnit)"
        }
    }

    fun getWeightInKG(): Double? = patientDetailsLiveData.value?.data?.weight

    fun getPregnantDetails(): PregnancyDetails? = patientDetailsLiveData.value?.data?.pregnancyDetails

    fun isPregnant(): Boolean = getGenderIsFemale() && patientDetailsLiveData.value?.data?.isPregnant == true

    fun ncdGetInstructions() {
        viewModelScope.launch(dispatcherIO) {
            ncdInstructionModelResponse.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDInstructionPregnancyRisk,
                isCompleted = true,
            )
            ncdInstructionModelResponse.postValue(ncdMedicalReviewRepository.ncdGetInstructions())
        }
    }

    fun ncdUpdatePregnancyRisk(request: NCDPregnancyRiskUpdate) {
        viewModelScope.launch(dispatcherIO) {
            updatePregnancyRisk.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDUpdatePregnancyRisk,
                isCompleted = true,
            )
            updatePregnancyRisk.postValue(ncdMedicalReviewRepository.ncdUpdatePregnancyRisk(request))
        }
    }

    fun getMenuForClinicalWorkflows() {
        viewModelScope.launch(dispatcherIO) {
            clinicalWorkflowsMenusLiveData.postValue(patientRepository.getMenuForClinicalWorkflows())
        }
    }

    fun getNeonatePatients(
        id: String,
        assessmentType: String? = null,
        origin: String? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            patientDetailsNeonateLiveData.postLoading()
            patientDetailsNeonateLiveData.postValue(
                patientRepository.getPatients(PatientDetailRequest(patientId = id, assessmentType = assessmentType, id = id, type = origin)),
            )
        }
    }

    fun getTbMedicalReviewStatus(): Boolean = patientDetailsLiveData.value?.data?.tbIMRCompleted ?: false

    fun getHivMedicalReviewStatus(): Boolean = patientDetailsLiveData.value?.data?.hivIMRCompleted ?: false

    fun getDob(): String? = patientDetailsLiveData.value?.data?.birthDate

    fun getPregnancyBreastFeedStatus(): String? = patientDetailsLiveData.value?.data?.pregnancyBreastFeedStatus

    fun getANCVisitCount(): Int? =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.ancVisitMedicalReview

    fun getEstimatedDeliveryDate(): String? =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.estimatedDeliveryDate

    fun getGestationalAge(): Long? =
        patientDetailsLiveData.value
            ?.data
            ?.pregnancyDetails
            ?.gestationalAge
            .toString()
            .toLongOrNull()
}
