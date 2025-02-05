package com.medtroniclabs.opensource.ncd.followup.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.followup.repo.NCDFollowUpRepo
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDCallResultViewModel @Inject constructor(
    private val ncdFollowUpRepo: NCDFollowUpRepo,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {
    val getAttempts = MutableLiveData<Long?>()
    fun getAttemptsById(id: Long?) {
        viewModelScope.launch {
            try {
                if (id != null) {
                    val value = ncdFollowUpRepo.getAttemptsById(id)
                    getAttempts.postValue(value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}