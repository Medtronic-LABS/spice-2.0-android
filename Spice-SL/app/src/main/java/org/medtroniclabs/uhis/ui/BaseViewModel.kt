package org.medtroniclabs.uhis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.db.AnalyticsRepository
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.di.IoDispatcher
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
open class BaseViewModel @Inject constructor(
    @param:IoDispatcher open var dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    init {
        logViewModelLifecycle("Created")
    }

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    fun setUserJourney(userJourney: String) =
        viewModelScope.launch(dispatcherIO) {
            setUserDetails()
            analyticsRepository.insertUserJourney(userJourney)
            if (userJourney == AnalyticsDefinedParams.LOGOUT) {
                UserDetail.referenceId = UUID.randomUUID().toString()
            }
        }

    fun setAnalyticsData(
        startDate: String,
        eventType: String? = null,
        eventName: String,
        exitReason: String? = null,
        isCompleted: Boolean = true,
    ) {
        viewModelScope.launch(dispatcherIO) {
            setUserDetails()
            Timber.d("userId: ${UserDetail.userId}")
            val parameter = AnalyticsUtils.createEventParameter(
                startDate,
                eventType = eventType,
                exitReason = exitReason,
                isCompleted = isCompleted.toString(),
            )
            analyticsRepository.logEvent(eventName, parameter, lastSyncDate())
        }
    }

    suspend fun setAnalyticsFollowUpData(
        id: Long,
        patientId: String?,
        callStatus: FollowUpCallStatus,
        patientStatus: String?,
        unSuccessfulReason: String?,
        startTiming: String?,
    ) {
        setUserDetails()
        val parameter = AnalyticsUtils.createFollowUpEventParameter(
            id,
            patientId,
            callStatus.name,
            patientStatus,
            unSuccessfulReason,
            startTiming,
        )
        analyticsRepository.logEvent(AnalyticsDefinedParams.MyPatient, parameter, lastSyncDate())
    }

    private fun setUserDetails() {
        UserDetail.userId = SecuredPreference.getUserId().toString()
        UserDetail.role = SecuredPreference.getRole()
        UserDetail.startDateTime = AnalyticsUtils.getCurrentDateTimeInLocalTime()
    }

    private fun lastSyncDate(): String {
        val lastSyncedAt =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
        return lastSyncedAt ?: "--"
    }

    override fun onCleared() {
        super.onCleared()
        logViewModelLifecycle("Cleared")
    }

    fun launch(
        context: CoroutineContext = dispatcherIO,
        block: suspend CoroutineScope.() -> Unit,
    ) = viewModelScope.launch(context, block = block)

    private fun logViewModelLifecycle(state: String) {
        Timber.tag("ViewModelLifecycle").d("${javaClass.simpleName} $state")
    }
}
