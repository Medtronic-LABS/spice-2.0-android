package com.medtroniclabs.opensource.ui.mypatients.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Active
import com.medtroniclabs.opensource.common.DefinedParams.LIST_LIMIT
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.common.DefinedParams.OnHold
import com.medtroniclabs.opensource.common.DefinedParams.OnTreatment
import com.medtroniclabs.opensource.common.DefinedParams.REFERRED
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.model.MedicalReviewFilterModel
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.SortModel
import com.medtroniclabs.opensource.ncd.data.PatientVisitRequest
import com.medtroniclabs.opensource.ncd.data.PatientVisitResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.mypatients.PatientsDataSource
import com.medtroniclabs.opensource.ui.mypatients.repo.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val apiHelper: ApiHelper,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
    ) : BaseViewModel(dispatcherIO) {

    //Patient list - Grid count
    var spanCount: Int = DefinedParams.span_count_1
    var totalPatientCount = MutableLiveData<String>()
    var searchText = ""
    var medicalReviewDueTag: List<ChipViewItemModel>? = null
    var patientStatusTag: List<ChipViewItemModel>? = null
    var ncdReferredForTag: List<ChipViewItemModel>? = null
    var ncdMedicalReviewDateTag: List<ChipViewItemModel>? = null
    var ncdRedRiskTag: List<ChipViewItemModel>? = null
    var ncdRegistrationTag: List<ChipViewItemModel>? = null
    var ncdCvdRiskTag: List<ChipViewItemModel>? = null
    var ncdAssessmentTag: List<ChipViewItemModel>? = null
    var filterLiveData = MutableLiveData<Boolean>()
    var sortLiveData = MutableLiveData<Boolean>()
    var origin: String? = null
    var selectedPatientDetails: PatientListRespModel? = null
    val patientVisitLiveData = MutableLiveData<Resource<PatientVisitResponse>>()
    val isTiberbuUser = CommonUtils.isTiberbuUser()

    //Sort
    var isRedRisk: Boolean? = null
    var isLatestAssessment: Boolean? = null
    var isMedicalReviewDueDate: Boolean? = null
    var isHighLowBp: Boolean? = null
    var isHighLowBg: Boolean? = null
    var isAssessmentDueDate: Boolean? = null

    val patientsDataSource =
        Pager(config = PagingConfig(pageSize = LIST_LIMIT), pagingSourceFactory = {
            PatientsDataSource(
                apiHelper = apiHelper,
                patientRepository = patientRepository,
                searchText = searchText,
                filter = if (isTiberbuUser) null else myFilter(),
                sort = if (isTiberbuUser) null else mySort(),
                origin = getFormattedOrigin(origin),
                isPatientListRequired = CommonUtils.isPatientListRequired(origin?.lowercase())
            ) { getPatientsCount ->
                totalPatientCount.postValue(getPatientsCount)
            }
        }).flow

    private fun mySort(): SortModel? {
        return if (CommonUtils.canShowSort(origin)) getSort() else null
    }

    private fun myFilter(): MedicalReviewFilterModel? {
        return if (CommonUtils.canShowFilter(origin)) getFilter()
        else if (CommonUtils.isHRIO()) MedicalReviewFilterModel(enrollmentStatus = DefinedParams.ENROLLED)
        else null
    }

    private fun getFormattedOrigin(origin: String?): String? {
        return when (origin?.lowercase()) {
            MenuConstants.SCREENING.lowercase() -> DefinedParams.Screening
            MenuConstants.REGISTRATION.lowercase() -> DefinedParams.Registration
            MenuConstants.ASSESSMENT.lowercase() -> DefinedParams.Assessment
            MenuConstants.LIFESTYLE.lowercase(), MenuConstants.MY_PATIENTS_MENU_ID.lowercase() -> DefinedParams.MyPatients
            MenuConstants.DISPENSE.lowercase() -> DefinedParams.Dispense
            MenuConstants.INVESTIGATION.lowercase() -> DefinedParams.Investigation
            else -> null
        }
    }

    fun setFilter(trigger: Boolean) {
        filterLiveData.value = trigger
    }

    fun setSort(trigger: Boolean) {
        sortLiveData.value = trigger
    }

    private fun getSort(): SortModel {
        return if (allAreNull()) SortModel(isRedRisk = true)
        else
            SortModel(
                isRedRisk = isRedRisk,
                isLatestAssessment = isLatestAssessment,
                isMedicalReviewDueDate = isMedicalReviewDueDate,
                isHighLowBp = isHighLowBp,
                isHighLowBg = isHighLowBg,
                isAssessmentDueDate = isAssessmentDueDate
            )
    }

    private fun allAreNull(): Boolean {
        return isRedRisk == null &&
                isLatestAssessment == null &&
                isMedicalReviewDueDate == null &&
                isHighLowBp == null &&
                isHighLowBg == null &&
                isAssessmentDueDate == null
    }

    private fun getFilter(): MedicalReviewFilterModel? {
        val isPharmacist: Boolean = CommonUtils.isPharmacist()
        return if (!patientStatusTag.isNullOrEmpty() ||
            !medicalReviewDueTag.isNullOrEmpty() ||
            !ncdReferredForTag.isNullOrEmpty() ||
            !ncdMedicalReviewDateTag.isNullOrEmpty() ||
            !ncdRedRiskTag.isNullOrEmpty() ||
            !ncdRegistrationTag.isNullOrEmpty() ||
            !ncdCvdRiskTag.isNullOrEmpty() ||
            !ncdAssessmentTag.isNullOrEmpty()
        ) {
            MedicalReviewFilterModel(
                patientStatus = patientStatusTag?.map {
                    if (it.name.equals(OnTreatment, true)) OnHold else if (it.value.equals(
                            REFERRED,
                            true
                        )
                    ) {
                        Active
                    } else ""
                },
                visitDate = medicalReviewDueTag?.mapNotNull { it.value },
                labTestReferredOn = if (isPharmacist) null else getReferredOn(),
                prescriptionReferredOn = if (isPharmacist) getReferredOn() else null,
                medicalReviewDate = ncdMedicalReviewDateTag?.mapNotNull { it.value }?.get(0),
                enrollmentStatus = ncdRegistrationTag?.mapNotNull { it.value }?.get(0),
                isRedRiskPatient = redRisk(),
                cvdRiskLevel = ncdCvdRiskTag?.mapNotNull { it.value }?.get(0),
                assessmentDate = ncdAssessmentTag?.mapNotNull { it.value }?.get(0)
            )
        } else {
            null
        }
    }

    private fun getReferredOn(): String? {
        return ncdReferredForTag?.mapNotNull { it.value }?.get(0)
    }

    private fun redRisk(): Boolean? {
        val noRedRisk = ncdRedRiskTag?.mapNotNull { it.value }.isNullOrEmpty()
        return if (noRedRisk) null else true
    }

    fun filterCount(): Int {
        return listOf(
            patientStatusTag,
            ncdReferredForTag,
            medicalReviewDueTag,
            ncdMedicalReviewDateTag,
            ncdRedRiskTag,
            ncdRegistrationTag,
            ncdCvdRiskTag,
            ncdAssessmentTag
        ).count { !it.isNullOrEmpty() }
    }

    fun sortCount(): Int {
        val hasSort = isRedRisk != null ||
                isLatestAssessment != null ||
                isMedicalReviewDueDate != null ||
                isHighLowBp != null ||
                isHighLowBg != null ||
                isAssessmentDueDate != null
        return if (hasSort) 1 else 0
    }

    fun createPatientVisit(request: PatientVisitRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientVisitLiveData.postLoading()
            patientVisitLiveData.postValue(
                patientRepository.createPatientVisit(request)
            )
        }
    }
}

