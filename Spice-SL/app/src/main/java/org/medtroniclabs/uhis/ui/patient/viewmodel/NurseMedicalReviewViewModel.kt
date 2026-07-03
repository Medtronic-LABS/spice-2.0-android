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
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.registration.GlucoseLog
import org.medtroniclabs.uhis.data.registration.NurseCreateResponse
import org.medtroniclabs.uhis.data.registration.NurseMrRequestModel
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.UnselectedDiagnosis
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.db.entity.SymptomEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.PatientDetailRequest
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import org.medtroniclabs.uhis.repo.OnBoardingRepository
import javax.inject.Inject

@HiltViewModel
class NurseMedicalReviewViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    private val onBoardingRepo: OnBoardingRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    @Inject
    lateinit var connectivityManager: ConnectivityManager
    val patientDetailsResponse = MutableLiveData<Resource<PatientDetailsModel>>()
    val latestConfirmDiagnosesList = MutableLiveData<Resource<PatientDetailsModel>>()
    var patientId: Long? = null
    var patientVisitId: Long? = null
    var patientIdString: String? = null
    var patientDetailsValue: PatientDetailsModel? = null
    val worseningSymptomsSelections = HashMap<String, Any>()
    var selectedSymptomsAndAdherence: String? = null
    var systolic: String? = null
    var diastolic: String? = null
    var initialReview: Boolean = false
    var symptomTypeListResponse = MutableLiveData<List<SymptomEntity>>()
    var nurseMrRequestModel: NurseMrRequestModel = NurseMrRequestModel()
    var unselectedDiagnosis: ArrayList<UnselectedDiagnosis>? = null
    var patientTrackId: Long? = null
    var instrctionEntered: String? = null
    var labTestId: Long? = null
    var nurseCreateResponse = MutableLiveData<Resource<NurseCreateResponse>>()
    var newPatientId: Long? = null
    var glucoseType = HashMap<String, Any>()
    var ouSitesLiveData = MutableLiveData<Resource<ArrayList<SiteEntity>>>()
    var selectedSymptomsAndAdherenceLiveData = MutableLiveData<String>()
    var glucoseLog: GlucoseLog? = null
    var glucoseLogList: ArrayList<GlucoseLog>? = null
    var executionCount = 0
    var isConfirmDiagnosis = false
    val patientDetailsDiagnosisResponse = MutableLiveData<Resource<PatientDetailsModel>>()

    fun getPatientDetails(
        context: Context,
        patientId: String,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                patientDetailsResponse.postLoading()
                try {
                    val response = medicalReviewRepo.getPatientDetails(PatientDetailRequest(patientId = patientId))
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientDetailsResponse.postSuccess(res.entity)
                            patientDetailsDiagnosisResponse.postSuccess(res.entity)
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

    fun getLatestDiagnoses(
        context: Context,
        request: PatientDetailsModel,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                latestConfirmDiagnosesList.postLoading()
                try {
                    val response = medicalReviewRepo.getPatientDetails(PatientDetailRequest(patientId = patientIdString))
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            // Keep in-memory patient details in sync so bio-data and the
                            // edit-diagnosis dialog both see the latest confirmations.
                            patientDetailsValue = res.entity
                            patientDetailsDiagnosisResponse.postSuccess(res.entity)
                            latestConfirmDiagnosesList.postSuccess(res.entity)
                        } else {
                            latestConfirmDiagnosesList.postError()
                        }
                    } else {
                        latestConfirmDiagnosesList.postError()
                    }
                } catch (e: Exception) {
                    latestConfirmDiagnosesList.postError()
                }
            }
        } else {
            latestConfirmDiagnosesList.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getSymptomListByType(type: String) {
        viewModelScope.launch(dispatcherIO) {
            try {
                symptomTypeListResponse.postValue(onBoardingRepo.getSymptomListByType(type))
            } catch (_: Exception) {
                // Exception - Catch block
            }
        }
    }

    fun createNurseMedicalReview(
        context: Context,
        nurseMrRequestModel: NurseMrRequestModel,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                nurseCreateResponse.postLoading()
                try {
                    val response = medicalReviewRepo.createNurseMedicalReview(nurseMrRequestModel)

                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            nurseCreateResponse.postSuccess(res.entity)
                        } else {
                            nurseCreateResponse.postError(
                                StringConverter.getErrorMessage(
                                    response.errorBody(),
                                ),
                            )
                        }
                    } else {
                        nurseCreateResponse.postError(
                            StringConverter.getErrorMessage(
                                response.errorBody(),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    nurseCreateResponse.postError()
                }
            }
        } else {
            nurseCreateResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun fetchOUSiteList() {
        viewModelScope.launch(dispatcherIO) {
            ouSitesLiveData.postLoading()
            try {
                val ouSiteList = medicalReviewRepo.getOperatingUnitSites()
                val siteList = Resource(ResourceState.SUCCESS, ArrayList(ouSiteList))
                ouSitesLiveData.postValue(siteList)
            } catch (e: Exception) {
                ouSitesLiveData.postValue(Resource(ResourceState.ERROR))
            }
        }
    }

    fun getPatientDetailsDiagnosis(
        context: Context,
        request: PatientDetailsModel,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                patientDetailsDiagnosisResponse.postLoading()
                try {
                    val response = medicalReviewRepo.getPatientDetails(PatientDetailRequest(patientId = patientIdString))
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientDetailsDiagnosisResponse.postSuccess(res.entity)
                        } else {
                            patientDetailsDiagnosisResponse.postError()
                        }
                    } else {
                        patientDetailsDiagnosisResponse.postError()
                    }
                } catch (e: Exception) {
                    patientDetailsDiagnosisResponse.postError()
                }
            }
        } else {
            patientDetailsDiagnosisResponse.setError(context.getString(R.string.no_internet_error))
        }
    }
}
