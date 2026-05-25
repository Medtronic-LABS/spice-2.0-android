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
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import org.medtroniclabs.uhis.ui.patient.UIConstants
import javax.inject.Inject

@HiltViewModel
class TermsAndConditionViewModel @Inject constructor(
    private val screeningRepository: MetaRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) :
    ViewModel() {
        var enrollmentConsent = false
        var isFromScreening = false
        var isFromSummaryPage = false
        var isFromDirectEnrollment = false
        var consentDoneLiveDate = MutableLiveData<Resource<String>>()
        var patientInitial = MutableLiveData<String?>()
        var isEyeFromScreening = false
        var isCataractScreening = false

        fun fetchConsentRawHTML() {
            viewModelScope.launch(dispatcherIO) {
                try {
                    consentDoneLiveDate.postLoading()
                    val response: String? = SecuredPreference.getUserId()?.let { userId ->
                        val uniqueID = when {
                            enrollmentConsent -> UIConstants.ENROLLMENT_UNIQUE_ID
                            isEyeFromScreening -> UIConstants.EYE_CARE_SCREENING_UNIQUE_ID
                            isCataractScreening -> UIConstants.CATARACT
                            else -> UIConstants.SCREENING_UNIQUE_ID
                        }

                        screeningRepository.getConsentHtmlRawString(uniqueID, userId)?.content
                    }
                    consentDoneLiveDate.postSuccess(
                        response?.let {
                            CommonUtils.formatConsent(
                                it,
                            )
                        },
                    )
                } catch (e: Exception) {
                    consentDoneLiveDate.postError(e.message)
                }
            }
        }
    }
