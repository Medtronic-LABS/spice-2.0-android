package com.medtroniclabs.opensource.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.boarding.repo.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {

    var selectedHouseholdMemberID = -1L
    var selectedMemberDob: String? = null
    var followUpId = -1L
    val resultRMNCHFlowHashMap = HashMap<String, Any>()
    val menuListLiveData = MutableLiveData<Resource<List<MenuEntity>>>()
    val resultANCFlowHashMap = HashMap<String, Any>()

    fun getMenuForClinicalWorkflows(gender: String?) {
        viewModelScope.launch(dispatcherIO) {
            menuListLiveData.postLoading()
            menuListLiveData.postValue(
                metaRepository.getMenuForClinicalWorkflows(
                    selectedHouseholdMemberID, gender
                )
            )
        }
    }

    fun getMyPatientsMenuItemsList() {
        viewModelScope.launch(dispatcherIO) {
            menuListLiveData.postLoading()
            menuListLiveData.postValue(
                metaRepository.getMenu()
            )
        }
    }
}