package org.medtroniclabs.uhis.ui.patient.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.registration.InvestigationReq
import org.medtroniclabs.uhis.data.registration.LabTestListResponse
import org.medtroniclabs.uhis.data.registration.LabTestResult
import org.medtroniclabs.uhis.data.registration.NurseLabTest
import org.medtroniclabs.uhis.data.registration.PatientHistoryRequest
import org.medtroniclabs.uhis.data.registration.PatientLabTestHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientMedicalReviewHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.SessionHistoryResponse
import org.medtroniclabs.uhis.data.registration.SessionModel
import org.medtroniclabs.uhis.data.registration.SummaryResponse
import org.medtroniclabs.uhis.data.registration.VisitDateModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import javax.inject.Inject

@HiltViewModel
class MedicalReviewPatientHistoryViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    var origin: String? = null
    var isFromCMR: Boolean = false
    var isMedicalReviewSummary = false
    val patientLabTestHistoryResponse =
        MutableLiveData<Resource<PatientLabTestHistoryResponse>>()

    val patientLabTestHistoryResponseByID =
        MutableLiveData<Resource<PatientLabTestHistoryResponse>>()

    var selectedPatientId: Long? = null

    var selectedPatientMedicalReviewId: Long? = null

    var selectedPatientPrescription: Long? = null

    val patientMedicalHistoryListResponse = MutableLiveData<Resource<SummaryResponse>>()

    val patientMedicalHistoryListResponseByID = MutableLiveData<Resource<SummaryResponse>>()
    val reviewHistoryList = MutableLiveData<Resource<PatientMedicalReviewHistoryResponse>>()

    val patientPrescriptionHistoryResponse =
        MutableLiveData<Resource<PatientPrescriptionHistoryResponse>>()

    val patientPrescriptionHistoryResponseBYID =
        MutableLiveData<Resource<PatientPrescriptionHistoryResponse>>()

    val sessionHistoryList = MutableLiveData<Resource<SessionHistoryResponse>>()
    var selectedSessionId: Int? = null
    val patientLabTestRecommendation =
        MutableLiveData<Resource<List<NurseLabTest>>>()
    var investigationUIModel: ArrayList<NurseLabTest>? = null
    private val _patientLabTestRecommendation = MutableLiveData<Resource<List<NurseLabTest>>>()
    var recommendedLabTestList: List<NurseLabTest>? = null
    val labTestListResponse = MutableLiveData<Resource<LabTestListResponse>>()
    val labTestListContinousResponse = MutableLiveData<Resource<LabTestListResponse>>()
    var patientTrackId: Long = -1L
    var patientVisitId: Long = -1L

    var chipList = ArrayList<ChipViewItemModel>()
    var chipListLT = ArrayList<ChipViewItemModel>()
    var activityList: MutableList<ChipViewItemModel>? = null
    var prescriptionList: List<PrescriptionModel>? = null

    fun getPatientLabTestHistory(request: PatientHistoryRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientLabTestHistoryResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTestHistory(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientLabTestHistoryResponse.postSuccess(res.entity)
                    } else {
                        patientLabTestHistoryResponse.postError()
                    }
                } else {
                    patientLabTestHistoryResponse.postError()
                }
            } catch (e: Exception) {
                patientLabTestHistoryResponse.postError()
            }
        }
    }

    fun getPatientLabTestHistoryById(request: PatientHistoryRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientLabTestHistoryResponseByID.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTestHistory(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientLabTestHistoryResponseByID.postSuccess(res.entity)
                    } else {
                        patientLabTestHistoryResponseByID.postError()
                    }
                } else {
                    patientLabTestHistoryResponseByID.postError()
                }
            } catch (e: Exception) {
                patientLabTestHistoryResponseByID.postError()
            }
        }
    }

    fun getPatientMedicalHistory(request: MedicalReviewBaseRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientMedicalHistoryListResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientMedicalReviewSummary(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientMedicalHistoryListResponse.postSuccess(res.entity)
                    } else {
                        patientMedicalHistoryListResponse.postError()
                    }
                } else {
                    patientMedicalHistoryListResponse.postError()
                }
            } catch (e: Exception) {
                patientMedicalHistoryListResponse.postError()
            }
        }
    }

    fun getPatientMedicalHistoryByID(request: MedicalReviewBaseRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientMedicalHistoryListResponseByID.postLoading()
            try {
                val response = medicalReviewRepo.getPatientMedicalReviewSummary(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientMedicalHistoryListResponseByID.postSuccess(res.entity)
                    } else {
                        patientMedicalHistoryListResponseByID.postError()
                    }
                } else {
                    patientMedicalHistoryListResponseByID.postError()
                }
            } catch (e: Exception) {
                patientMedicalHistoryListResponseByID.postError()
            }
        }
    }

    fun getPatientMedicalReviewHistoryList(
        request: MedicalReviewBaseRequest,
        canUpdateDate: Boolean = true,
    ) {
        viewModelScope.launch(dispatcherIO) {
            val datesList = reviewHistoryList.value?.data?.patientReviewDates
            reviewHistoryList.postLoading()
            try {
                val response = medicalReviewRepo.getPatientMedicalReviewHistoryList(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        updateMedicalReviewHistoryList(res.entity, reviewHistoryList, datesList, canUpdateDate)
                    } else {
                        reviewHistoryList.postError()
                    }
                } else {
                    reviewHistoryList.postError()
                }
            } catch (e: Exception) {
                reviewHistoryList.postError()
            }
        }
    }

    private fun updateMedicalReviewHistoryList(
        entity: PatientMedicalReviewHistoryResponse?,
        reviewHistoryList: MutableLiveData<Resource<PatientMedicalReviewHistoryResponse>>,
        datesList: ArrayList<VisitDateModel>?,
        canUpdateDate: Boolean,
    ) {
        if (entity != null) {
            entity.let {
                if (!canUpdateDate) {
                    it.patientReviewDates = datesList ?: ArrayList()
                }
                it.canUpdateDate = canUpdateDate
                reviewHistoryList.postSuccess(it)
            }
        } else {
            reviewHistoryList.postSuccess(null)
        }
    }

    private fun updateSessionHistoryList(
        entity: SessionHistoryResponse?,
        sessionHistoryList: MutableLiveData<Resource<SessionHistoryResponse>>,
        datesList: ArrayList<SessionModel>?,
        canUpdateDate: Boolean,
    ) {
        if (entity != null) {
            entity.let {
                if (!canUpdateDate) {
                    it.sessionHistory = datesList ?: ArrayList()
                }
                it.canUpdateDate = canUpdateDate
                sessionHistoryList.postSuccess(it)
            }
        } else {
            sessionHistoryList.postSuccess(null)
        }
    }

    fun getPatientPrescriptionHistory(request: PatientHistoryRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientPrescriptionHistoryResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientPrescriptionHistoryList(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientPrescriptionHistoryResponse.postSuccess(res.entity)
                    } else {
                        patientPrescriptionHistoryResponse.postError()
                    }
                } else {
                    patientPrescriptionHistoryResponse.postError()
                }
            } catch (e: Exception) {
                patientPrescriptionHistoryResponse.postError()
            }
        }
    }

    fun getPatientPrescriptionHistoryById(request: PatientHistoryRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientPrescriptionHistoryResponseBYID.postLoading()
            try {
                val response = medicalReviewRepo.getPatientPrescriptionHistoryList(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientPrescriptionHistoryResponseBYID.postSuccess(res.entity)
                    } else {
                        patientPrescriptionHistoryResponseBYID.postError()
                    }
                } else {
                    patientPrescriptionHistoryResponseBYID.postError()
                }
            } catch (e: Exception) {
                patientPrescriptionHistoryResponseBYID.postError()
            }
        }
    }

    fun getPatientSessionHistoryList(
        request: MedicalReviewBaseRequest,
        canUpdateDate: Boolean = true,
    ) {
        viewModelScope.launch(dispatcherIO) {
            val datesList = sessionHistoryList.value?.data?.sessionHistory
            sessionHistoryList.postLoading()
            try {
                val response = medicalReviewRepo.getPatientSessionHistoryList(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        updateSessionHistoryList(res.entity, sessionHistoryList, datesList, canUpdateDate)
                    } else {
                        sessionHistoryList.postError()
                    }
                } else {
                    sessionHistoryList.postError()
                }
            } catch (e: Exception) {
                sessionHistoryList.postError()
            }
        }
    }

    fun getPatientLabTestRecommendation(countryID: Long) {
        viewModelScope.launch(dispatcherIO) {
            patientLabTestRecommendation.postLoading()
            var request = InvestigationReq(countryID)
            try {
                val response = medicalReviewRepo.getPatientLabTestRecommendation(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientLabTestRecommendation.postSuccess(res.entityList ?: loadStaticLabTests())
                    } else {
                        patientLabTestRecommendation.postError()
                    }
                } else {
                    patientLabTestRecommendation.postError()
                }
            } catch (e: Exception) {
                patientLabTestRecommendation.postError()
            }
        }
    }

    fun loadStaticLabTests(): List<NurseLabTest> =
        listOf(
            NurseLabTest(
                id = 48,
                patientTrackId = null,
                countryId = 3,
                roleNames = null,
                searchTerm = null,
                patientVisitId = null,
                tenantId = 5,
                menuName = null,
                reason = null,
                otherReason = null,
                isSessionDropOut = null,
                prescribedSiteId = null,
                name = "Lipids",
                displayOrder = 0,
                updatedAt = "2023-03-15T00:04:51+06:00",
                labTestResults = listOf(
                    LabTestResult(71, 2, 2, "2023-03-15T00:01:47+06:00", "2023-03-15T00:04:51+06:00", 5, "Low Density Lipoprotein (LDL)", 48, 2, true, false),
                    LabTestResult(69, 2, 2, "2023-03-15T00:01:47+06:00", "2023-03-15T00:04:51+06:00", 5, "HDL", 48, 3, true, false),
                    LabTestResult(70, 2, 2, "2023-03-15T00:01:47+06:00", "2023-03-15T00:04:51+06:00", 5, "Triglycerides, Serum (fasting)", 48, 4, true, false),
                    LabTestResult(68, 2, 2, "2023-03-15T00:01:47+06:00", "2023-03-15T00:04:51+06:00", 5, "Total Cholesterol", 48, 1, true, false),
                ),
                createdBy = 2,
                updatedBy = 2,
                createdAt = "2024-03-15T00:01:47+06:00",
                active = true,
                deleted = false,
                resultTemplate = true,
            ),
            NurseLabTest(
                id = 49,
                patientTrackId = null,
                countryId = 3,
                roleNames = null,
                searchTerm = null,
                patientVisitId = null,
                tenantId = 5,
                menuName = null,
                reason = null,
                otherReason = null,
                isSessionDropOut = null,
                prescribedSiteId = null,
                name = "Lipids1",
                displayOrder = 1,
                updatedAt = "2023-03-16T00:04:51+06:00",
                labTestResults = emptyList(),
                createdBy = 2,
                updatedBy = 2,
                createdAt = "2023-03-16T00:01:47+06:00",
                active = true,
                deleted = false,
                resultTemplate = true,
            ),
            NurseLabTest(
                id = 50,
                patientTrackId = null,
                countryId = 3,
                roleNames = null,
                searchTerm = null,
                patientVisitId = null,
                tenantId = 5,
                menuName = null,
                reason = null,
                otherReason = null,
                isSessionDropOut = null,
                prescribedSiteId = null,
                name = "Lipids2",
                displayOrder = 2,
                updatedAt = "2023-03-17T00:04:51+06:00",
                labTestResults = emptyList(),
                createdBy = 2,
                updatedBy = 2,
                createdAt = "2023-03-17T00:01:47+06:00",
                active = true,
                deleted = false,
                resultTemplate = true,
            ),
            NurseLabTest(
                id = 51,
                patientTrackId = null,
                countryId = 3,
                roleNames = null,
                searchTerm = null,
                patientVisitId = null,
                tenantId = 5,
                menuName = null,
                reason = null,
                otherReason = null,
                isSessionDropOut = null,
                prescribedSiteId = null,
                name = "Lipids3",
                displayOrder = 3,
                updatedAt = "2023-03-18T00:04:51+06:00",
                labTestResults = emptyList(),
                createdBy = 2,
                updatedBy = 2,
                createdAt = "2023-03-18T00:01:47+06:00",
                active = true,
                deleted = false,
                resultTemplate = true,
            ),
            NurseLabTest(
                id = 52,
                patientTrackId = null,
                countryId = 3,
                roleNames = null,
                searchTerm = null,
                patientVisitId = null,
                tenantId = 5,
                menuName = null,
                reason = null,
                otherReason = null,
                isSessionDropOut = null,
                prescribedSiteId = null,
                name = "Lipids4",
                displayOrder = 4,
                updatedAt = "2023-03-19T00:04:51+06:00",
                labTestResults = emptyList(),
                createdBy = 2,
                updatedBy = 2,
                createdAt = "2023-03-19T00:01:47+06:00",
                active = true,
                deleted = false,
                resultTemplate = true,
            ),
        )

    fun getLabTestList(isFromTechnicianLogin: Boolean = false) {
        val request = HashMap<String, Any?>()
        request[DefinedParams.PATIENT_TRACK_ID] = patientTrackId
        request["resultUpdated"] = true
        viewModelScope.launch(dispatcherIO) {
            labTestListResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTests(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        labTestListResponse.postSuccess(res.entity)
                    } else {
                        labTestListResponse.postError()
                    }
                } else {
                    labTestListResponse.postError()
                }
            } catch (e: Exception) {
                labTestListResponse.postError()
            }
        }
    }

    fun getLabTestContinousList() {
        val request = HashMap<String, Any?>()
        request[DefinedParams.PATIENT_TRACK_ID] = patientTrackId
        request["resultUpdated"] = false
        viewModelScope.launch(dispatcherIO) {
            labTestListContinousResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTests(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        labTestListContinousResponse.postSuccess(res.entity)
                    } else {
                        labTestListContinousResponse.postError()
                    }
                } else {
                    labTestListContinousResponse.postError()
                }
            } catch (e: Exception) {
                labTestListContinousResponse.postError()
            }
        }
    }
}
