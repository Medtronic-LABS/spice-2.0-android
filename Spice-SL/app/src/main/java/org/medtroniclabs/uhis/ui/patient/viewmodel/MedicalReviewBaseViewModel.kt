package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.model.SiteRoleResponse
import org.medtroniclabs.uhis.data.registration.BadgeModel
import org.medtroniclabs.uhis.data.registration.BadgeResponseModel
import org.medtroniclabs.uhis.data.registration.CreateContinuousMedicalRequest
import org.medtroniclabs.uhis.data.registration.FillPrescriptionRequest
import org.medtroniclabs.uhis.data.registration.InitialComorbidities
import org.medtroniclabs.uhis.data.registration.InitialEncounterRequest
import org.medtroniclabs.uhis.data.registration.InitialEncounterResponse
import org.medtroniclabs.uhis.data.registration.MedicalReviewEditModel
import org.medtroniclabs.uhis.data.registration.NurseLabTest
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.RegionSiteModel
import org.medtroniclabs.uhis.data.registration.SessionEncounterRequest
import org.medtroniclabs.uhis.data.registration.SingleWindowModel
import org.medtroniclabs.uhis.data.registration.SiteRoleModel
import org.medtroniclabs.uhis.data.registration.TransferCreateRequest
import org.medtroniclabs.uhis.db.entity.ComorbidityEntity
import org.medtroniclabs.uhis.db.entity.ComplaintsEntity
import org.medtroniclabs.uhis.db.entity.ComplicationEntity
import org.medtroniclabs.uhis.db.entity.CurrentMedicationEntity
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.db.entity.LifeStyleUIModel
import org.medtroniclabs.uhis.db.entity.LifestyleEntity
import org.medtroniclabs.uhis.db.entity.PhysicalExaminationEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.ncd.data.RegionSiteResponse
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import org.medtroniclabs.uhis.repo.OnBoardingRepository
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MedicalReviewBaseViewModel @Inject constructor(
    private val onBoardingRepo: OnBoardingRepository,
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    @Inject
    lateinit var connectivityManager: ConnectivityManager
    var origin: String? = null
    val comorbidityListResponse = MutableLiveData<List<ComorbidityEntity>>()
    val complicationListResponse = MutableLiveData<List<ComplicationEntity>>()
    val lifeStyleListResponse = MutableLiveData<List<LifestyleEntity>>()
    var lifeStyleListUIModel: List<LifeStyleUIModel>? = null
    val currentMedicationResponse = MutableLiveData<List<CurrentMedicationEntity>>()
    val physicalExaminationResponse = MutableLiveData<List<PhysicalExaminationEntity>>()
    val chiefCompliants = MutableLiveData<List<ComplaintsEntity>>()
    val continuousMedicalReviewResponse = MutableLiveData<Resource<InitialEncounterResponse>>()
    val initialEncounterRequest = InitialEncounterRequest()
    val continuousMedicalRequest = CreateContinuousMedicalRequest()
    val diagnosisListResponse = MutableLiveData<List<DiagnosisEntity>>()
    var showContinuousMedicalReview: Boolean = false
    var medicalReViewRequest: MedicalReviewBaseRequest? = null
    val badgeCountResponse = MutableLiveData<Resource<BadgeResponseModel>>()
    val medicalReviewEditModel: MedicalReviewEditModel = MedicalReviewEditModel()
    val singleWindowModel: SingleWindowModel = SingleWindowModel()
    var complaintsList: ArrayList<String> = ArrayList()
    var physicalExamsList: ArrayList<String> = ArrayList()
    var patientTrackId: Long = -1
    val searchSiteResponse = MutableLiveData<Resource<ArrayList<RegionSiteResponse>>>()
    val searchRoleUserResponse = MutableLiveData<Resource<ArrayList<SiteRoleResponse>>>()
    val patientTransferResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val validateTransferResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var isTransferDialogVisible = false
    var diagnosisList = MutableLiveData<List<DiagnosisEntity>>()
    var nextMedicalReviewDate: String? = null
    val sessionCreateResponse = MutableLiveData<Resource<InitialEncounterResponse>>()
    var sessionRequest: SessionEncounterRequest = SessionEncounterRequest()

    val rbsAndFbsSelections = HashMap<String, Any>()
    val selfCareSelection = HashMap<String, Any>()
    val medicationSearchResponse = MutableLiveData<Resource<ArrayList<PrescriptionModel>>>()
    var selectedMedication: PrescriptionModel? = null
    var selectedDiagnoses = HashMap<String, Any>()
    var selectedChipMedications = ArrayList<ChipViewItemModel>()
    var formResponseLiveData = MutableLiveData<Resource<FormResponse>>()
    var instructionsList = arrayListOf<String>()
    var initialReview: Boolean = false
    var selectedChipLabTestMedication = ArrayList<ChipViewItemModel>()
    var selectedLabTestMedication: NurseLabTest? = null
    var confirmDiagnosis: ArrayList<String>? = null
    var list: List<DiagnosisEntity>? = null

    fun getComorbidityList(
        gender: String?,
        pregnant: Boolean?,
    ) {
        viewModelScope.launch(dispatcherIO) {
            val workflowList = ArrayList<String>()
            comorbidityListResponse.postValue(medicalReviewRepo.getComorbidityBasedOnWorkflow(workflowList))
        }
    }

    fun getComplicationList() {
        viewModelScope.launch(dispatcherIO) {
            complicationListResponse.postValue(medicalReviewRepo.getComplication())
        }
    }

    fun getLifeStyleList() {
        viewModelScope.launch(dispatcherIO) {
            lifeStyleListResponse.postValue(medicalReviewRepo.getLifeStyle())
        }
    }

    fun getConfirmDiagnosisList(
        gender: ArrayList<String>,
        type: ArrayList<String>,
    ) {
        viewModelScope.launch(dispatcherIO) {
            diagnosisListResponse.postValue(medicalReviewRepo.getDiagnosis(gender, type))
        }
    }

    fun getCurrentMedicationList() {
        viewModelScope.launch(dispatcherIO) {
            currentMedicationResponse.postValue(medicalReviewRepo.getCurrentMedicationList())
        }
    }

    fun getCurrentMedicationList(type: String) {
        viewModelScope.launch(dispatcherIO) {
            currentMedicationResponse.postValue(medicalReviewRepo.getCurrentMedicationList(type))
        }
    }

    fun getPhysicalExaminationEntity(isPregnantWomen: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            physicalExaminationResponse.postValue(medicalReviewRepo.getPhysicalExaminationList(arrayListOf()))
        }
    }

    fun getComplaints(isPregnantWomen: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            chiefCompliants.postValue(medicalReviewRepo.getChiefComplaints(arrayListOf()))
        }
    }

    fun createContinuousMedicalReview(
        context: Context,
        request: MedicalReviewEditModel,
        patientDetails: PatientDetailsModel?,
        pregnancyDetails: HashMap<String, Any>?,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            continuousMedicalReviewResponse.postLoading()
            viewModelScope.launch(dispatcherIO) {
                try {
                    val response = getMRResponse(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            continuousMedicalReviewResponse.postSuccess(res.entity)
                        } else {
                            continuousMedicalReviewResponse.postError()
                        }
                    } else {
                        continuousMedicalReviewResponse.postError()
                    }
                } catch (e: Exception) {
                    continuousMedicalReviewResponse.postError()
                }
            }
        } else {
            continuousMedicalReviewResponse.postError(context.getString(R.string.no_internet_error))
        }
    }

    private suspend fun getMRResponse(request: MedicalReviewEditModel): Response<APIResponse<InitialEncounterResponse>> =
        medicalReviewRepo.createSingleWindowMR(getSingleWindowRequest(request))

    private fun getSingleWindowRequest(request: MedicalReviewEditModel): SingleWindowModel =
        SingleWindowModel(
            diagnosis = request.initialMedicalReview?.diagnosis,
            isPregnant = request.initialMedicalReview?.isPregnant,
            currentMedicationDetails = request.initialMedicalReview?.currentMedicationDetails,
            complaints = request.continuousMedicalReview?.complaints,
            clinicalNote = request.continuousMedicalReview?.clinicalNote,
            complaintComments = request.continuousMedicalReview?.complaintComments,
            comorbidities = handleComorbidites(request.initialMedicalReview?.comorbidities),
            complications = request.initialMedicalReview?.complications,
            patientTrackId = request.patientTrackId,
            patientVisitId = request.patientVisitId,
            tenantId = request.tenantId,
            nextMedicalReviewDate = request.nextMedicalReviewDate,
        )

    private fun handleComorbidites(comorbidities: ArrayList<InitialComorbidities>?): ArrayList<InitialComorbidities>? {
        val returnList = ArrayList<InitialComorbidities>()
        comorbidities
            ?.filter { (it.id != null && it.removed) || (it.id == null && !it.removed) }
            ?.let { returnList.addAll(it) }
        return returnList.ifEmpty { null }
    }

    fun getBadgeCount() {
        viewModelScope.launch(dispatcherIO) {
            badgeCountResponse.postLoading()
            try {
                val response = medicalReviewRepo.getBadgeCount(
                    BadgeModel(
                        patientTrackId,
                    ),
                )
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        badgeCountResponse.postSuccess(res.entity)
                    } else {
                        badgeCountResponse.postError()
                    }
                } else {
                    badgeCountResponse.postError()
                }
            } catch (e: Exception) {
                badgeCountResponse.postError()
            }
        }
    }

    fun searchSite(
        context: Context,
        searchValue: String,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    searchSiteResponse.postLoading()
                    val request = RegionSiteModel(
                        searchTerm = searchValue,
                        countryId = SecuredPreference.getTenantId(),
                        tenantId = SecuredPreference.getTenantId(),
                    )
                    val response = medicalReviewRepo.searchSite(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            searchSiteResponse.postSuccess(res.entityList)
                        } else {
                            searchSiteResponse.postError()
                        }
                    } else {
                        searchSiteResponse.postError()
                    }
                } catch (e: Exception) {
                    searchSiteResponse.postError()
                }
            }
        } else {
            searchSiteResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun searchRoleUser(
        context: Context,
        tenant: Long,
        searchValue: String,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    searchRoleUserResponse.postLoading()
                    val request = SiteRoleModel(tenantId = tenant, searchTerm = searchValue)
                    val response = medicalReviewRepo.searchRoleUser(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            searchRoleUserResponse.postSuccess(res.entityList)
                        } else {
                            searchRoleUserResponse.postError()
                        }
                    } else {
                        searchRoleUserResponse.postError()
                    }
                } catch (e: Exception) {
                    searchRoleUserResponse.postError()
                }
            }
        } else {
            searchRoleUserResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun createPatientTransfer(
        context: Context,
        request: TransferCreateRequest,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    patientTransferResponse.postLoading()
                    val response = medicalReviewRepo.createPatientTransfer(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            isTransferDialogVisible = false
                            val resultMap = HashMap<String, Any>()
                            res.message?.let { successMessage ->
                                resultMap[DefinedParams.MESSAGE] = successMessage
                            }
                            patientTransferResponse.postSuccess(resultMap)
                        } else {
                            patientTransferResponse.postError()
                        }
                    } else {
                        patientTransferResponse.postError(StringConverter.getErrorMessage(response.errorBody()))
                    }
                } catch (e: Exception) {
                    patientTransferResponse.postError()
                }
            }
        } else {
            patientTransferResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun validatePatientTransfer(
        context: Context,
        request: FillPrescriptionRequest,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    validateTransferResponse.postLoading()
                    val response = medicalReviewRepo.validatePatientTransfer(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            validateTransferResponse.postSuccess(res.entity)
                        } else {
                            validateTransferResponse.postError()
                        }
                    } else {
                        validateTransferResponse.postError(StringConverter.getErrorMessage(response.errorBody()))
                    }
                } catch (e: Exception) {
                    validateTransferResponse.postError()
                }
            }
        } else {
            validateTransferResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getDiagnosisList() {
        viewModelScope.launch(dispatcherIO) {
            diagnosisList.postValue(medicalReviewRepo.getDiagnosisList())
        }
    }

    fun sessionCreate(
        context: Context,
        request: SessionEncounterRequest,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    sessionCreateResponse.postLoading()
                    val response = medicalReviewRepo.sessionCreate(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            sessionCreateResponse.postSuccess(res.entity)
                        } else {
                            sessionCreateResponse.postError()
                        }
                    } else {
                        sessionCreateResponse.postError(StringConverter.getErrorMessage(response.errorBody()))
                    }
                } catch (e: Exception) {
                    sessionCreateResponse.postError()
                }
            }
        } else {
            sessionCreateResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun fetchWorkFlow(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            try {
                formResponseLiveData.postLoading()
                val response = onBoardingRepo.getFormData(formType)
                formResponseLiveData.postSuccess(response.data)
            } catch (e: Exception) {
                formResponseLiveData.postError(e.message)
            }
        }
    }
}
