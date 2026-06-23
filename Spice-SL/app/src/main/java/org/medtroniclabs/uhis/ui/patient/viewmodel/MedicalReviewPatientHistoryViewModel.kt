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
import org.medtroniclabs.uhis.data.medicalreview.ResLabTestRecommendations
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.registration.InvestigationReq
import org.medtroniclabs.uhis.data.registration.LabTestListResponse
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
import org.medtroniclabs.uhis.model.LabTestListRequest
import org.medtroniclabs.uhis.model.ReferralDetailRequest
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
        MutableLiveData<Resource<List<ResLabTestRecommendations>>>()
    var investigationUIModel: ArrayList<ResLabTestRecommendations>? = null
    private val _patientLabTestRecommendation = MutableLiveData<Resource<List<ResLabTestRecommendations>>>()
    var recommendedLabTestList: List<NurseLabTest>? = null
    val labTestListResponse = MutableLiveData<Resource<LabTestListResponse>>()
    val labTestListContinousResponse = MutableLiveData<Resource<LabTestListResponse>>()
    var patientTrackId: Long = -1L
    var patientVisitId: Long = -1L
    var patientReference: String? = null

    var chipList = ArrayList<ChipViewItemModel>()
    var chipListLT = ArrayList<ChipViewItemModel>()
    var activityList: MutableList<ChipViewItemModel>? = null
    var prescriptionList: List<PrescriptionModel>? = null

    fun getPatientLabTestHistory(request: PatientHistoryRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientLabTestHistoryResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTestHistory(
                    ReferralDetailRequest(
                        patientReference = patientReference,
                        encounterId = request.patientVisitId?.toString(),
                    ),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    patientLabTestHistoryResponse.postSuccess(
                        NurseFhirMapper.mapLabTestHistory(response.body()?.entity),
                    )
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
                val response = medicalReviewRepo.getPatientLabTestHistory(
                    ReferralDetailRequest(
                        patientReference = patientReference,
                        encounterId = request.patientVisitId?.toString(),
                    ),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    patientLabTestHistoryResponseByID.postSuccess(
                        NurseFhirMapper.mapLabTestHistory(response.body()?.entity),
                    )
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
                val response = medicalReviewRepo.getPatientPrescriptionHistoryList(
                    ReferralDetailRequest(
                        patientReference = patientReference,
                        encounterId = request.patientVisitId?.toString(),
                    ),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    patientPrescriptionHistoryResponse.postSuccess(
                        NurseFhirMapper.mapPrescriptionHistory(response.body()?.entity),
                    )
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
                val response = medicalReviewRepo.getPatientPrescriptionHistoryList(
                    ReferralDetailRequest(
                        patientReference = patientReference,
                        encounterId = request.patientVisitId?.toString(),
                    ),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    patientPrescriptionHistoryResponseBYID.postSuccess(
                        NurseFhirMapper.mapPrescriptionHistory(response.body()?.entity),
                    )
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
                        patientLabTestRecommendation.postSuccess(res.entityList)
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

    fun loadStaticLabTests(): List<ResLabTestRecommendations> = listOf()

    fun getLabTestList(isFromTechnicianLogin: Boolean = false) {
        viewModelScope.launch(dispatcherIO) {
            labTestListResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTests(
                    LabTestListRequest(patientReference = patientReference ?: ""),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    labTestListResponse.postSuccess(
                        NurseFhirMapper.mapLabTestList(response.body()?.entityList, resultUpdated = true),
                    )
                } else {
                    labTestListResponse.postError()
                }
            } catch (e: Exception) {
                labTestListResponse.postError()
            }
        }
    }

    fun getLabTestContinousList() {
        viewModelScope.launch(dispatcherIO) {
            labTestListContinousResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTests(
                    LabTestListRequest(patientReference = patientReference ?: ""),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    labTestListContinousResponse.postSuccess(
                        NurseFhirMapper.mapLabTestList(response.body()?.entityList, resultUpdated = false),
                    )
                } else {
                    labTestListContinousResponse.postError()
                }
            } catch (e: Exception) {
                labTestListContinousResponse.postError()
            }
        }
    }
}
