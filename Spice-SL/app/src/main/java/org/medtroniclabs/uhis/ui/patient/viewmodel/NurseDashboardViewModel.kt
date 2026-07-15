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
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.data.registration.UserDashboardRequest
import org.medtroniclabs.uhis.data.registration.UserDashboardResponse
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.OnBoardingRepository
import javax.inject.Inject

@HiltViewModel
class NurseDashboardViewModel @Inject constructor(
    private val onBoardingRepo: OnBoardingRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    var origin: String? = null

    @Inject
    lateinit var connectivityManager: ConnectivityManager
    var userDashboardDetails = MutableLiveData<Resource<UserDashboardResponse>>()

    // Filter-related properties
    var filter: FilterModel? = null

    fun filterCount(): Int {
        var count = 0
        filter?.let {
            if (it.medicalReviewDate != null) count++
            if (it.isRedRiskPatient == true) count++
            if (it.patientStatus != null) count++
            if (it.cvdRiskLevel != null) count++
            if (it.riskStatus != null) count++
            if (it.assessmentDate != null) count++
            if (it.villageId != null) count++
            if (it.selectedParaCounselor != null) count++
            if (it.sessionDate != null) count++
            if (!it.paraCounsellingStatus.isNullOrEmpty()) count++
            if (it.registrationDate != null) count++
            if (it.healthCondition != null) count++
            if (it.customRegistrationDate != null) count++
            if (it.labTestReferredDate != null) count++
            if (it.medicationPrescribedDate != null) count++
            if (!it.diagnosis.isNullOrEmpty()) count++
            if (it.dateRange != null) count++
            if (it.customDate != null) count++
            if (it.subVillageId != null) count++
            if (it.referredSite != null) count++
            if (!it.remainingAttempts.isNullOrEmpty()) count++
            if (it.shasthyaShebikaId != null) count++
            if (it.upazilaTenantId != null) count++
        }
        return count
    }

    fun getUserDashboardDetails(
        context: Context,
        request: UserDashboardRequest,
    ) {
        val role = SecuredPreference.getRole()
        viewModelScope.launch(dispatcherIO) {
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    userDashboardDetails.postLoading()
                    val response = when (role) {
                        // provider-dashboard API
                        RoleConstant.NURSE -> onBoardingRepo.getProviderDashboardDetails(
                            request,
                        )

                        // user-dashboard API
                        else -> onBoardingRepo.getProviderDashboardDetails(request)
                    }
                    if (response.isSuccessful) {
                        if (response.body()?.status == true) {
                            userDashboardDetails.postSuccess(response.body()?.entity)
                        } else {
                            userDashboardDetails.postError()
                        }
                    } else {
                        userDashboardDetails.postError(StringConverter.getErrorMessage(response.errorBody()))
                    }
                } else {
                    userDashboardDetails.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                userDashboardDetails.postError()
            }
        }
    }
}
