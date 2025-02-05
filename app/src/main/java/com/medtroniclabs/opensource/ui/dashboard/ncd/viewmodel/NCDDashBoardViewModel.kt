package com.medtroniclabs.opensource.ui.dashboard.ncd.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.NCDUserDashboardRequest
import com.medtroniclabs.opensource.data.NCDUserDashboardResponse
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.boarding.repo.MetaRepository
import com.medtroniclabs.opensource.ui.dashboard.ncd.repository.NCDDashBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class NCDDashBoardViewModel @Inject constructor(
    private val ncdDashBoardRepository: NCDDashBoardRepository,
    private val metaRepository: MetaRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    var userDashboardDetails = MutableLiveData<Resource<NCDUserDashboardResponse>>()
    val menuListLiveData = MutableLiveData<List<String>?>()

    fun getUserDashboardDetails(request: NCDUserDashboardRequest) {
        viewModelScope.launch(dispatcherIO) {
            userDashboardDetails.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDDashBoardCount,
                isCompleted = true
            )
            userDashboardDetails.postValue(ncdDashBoardRepository.getUserDashboardDetails(request))
        }
    }

    fun getMenus() {
        viewModelScope.launch(dispatcherIO) {
            val response = metaRepository.getMenu()
            val menus = response.data?.map { it.name }
            menuListLiveData.postValue(menus)
        }
    }
}