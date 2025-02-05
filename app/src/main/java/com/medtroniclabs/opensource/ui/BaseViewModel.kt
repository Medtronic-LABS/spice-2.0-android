package com.medtroniclabs.opensource.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.db.AnalyticsRepository
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.app.analytics.utils.CommonUtils
import com.medtroniclabs.opensource.appextensions.convertToLocalDateTime
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.FollowUpCallStatus
import com.medtroniclabs.opensource.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
open class BaseViewModel @Inject constructor(
    @IoDispatcher open var dispatcherIO: CoroutineDispatcher
) : ViewModel() {

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository


    fun setUserJourney(userJourney: String) =
        viewModelScope.launch(dispatcherIO) {
            setUserDetails()
            analyticsRepository.insertUserJourney(userJourney)
        }

    fun setAnalyticsData(
        startDate: String,
        eventType: String? = null,
        eventName: String,
        exitReason: String? = null,
        isCompleted: Boolean = true
    ) {
        viewModelScope.launch(dispatcherIO) {
            setUserDetails()
            Timber.d("userId: ${UserDetail.userId}")
            val parameter = CommonUtils.createEventParameter(
                startDate,
                eventType = eventType,
                exitReason = exitReason,
                isCompleted = isCompleted.toString()
            )
            analyticsRepository.logEvent(eventName, parameter,lastSyncDate())
        }
    }

    suspend fun setAnalyticsFollowUpData(
        id: Long,
        patientId: String?,
        callStatus: FollowUpCallStatus,
        patientStatus: String?,
        unSuccessfulReason: String?,
        startTiming: String?
    ) {
        setUserDetails()
        val parameter = CommonUtils.createFollowUpEventParameter(
            id,
            patientId,
            callStatus.name,
            patientStatus,
            unSuccessfulReason,
            startTiming
        )
        analyticsRepository.logEvent(AnalyticsDefinedParams.MyPatient, parameter,lastSyncDate())

    }

    private fun setUserDetails() {
        UserDetail.userId = SecuredPreference.getUserId().toString()
        UserDetail.role = SecuredPreference.getRole().toString()
        UserDetail.startDateTime = CommonUtils.getCurrentDateTimeInLocalTime()
    }
    private fun lastSyncDate(): String {
        val longSyncedAt =
            SecuredPreference.getLong(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
        return if (longSyncedAt != 0L) longSyncedAt.convertToLocalDateTime() else "--"
    }
}