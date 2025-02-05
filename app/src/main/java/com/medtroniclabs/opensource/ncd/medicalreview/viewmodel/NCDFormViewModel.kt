package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDFormsRepo
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDFormViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val ncdFormsRepo: NCDFormsRepo,
) : ViewModel() {

    val ncdFormResponse = SingleLiveEvent<Resource<List<FormLayout>>>()

    fun getNCDForm(type: String, workFlow: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            ncdFormResponse.postLoading()
            try {
                val gson = Gson()
                val formLayouts = ArrayList<FormLayout>()
                ncdFormsRepo.getNCDForm(type, workFlow = workFlow).forEach { item ->
                    if (item.contains(DefinedParams.ViewScreens)) {
                        gson.fromJson(item, List::class.java)?.let { list ->
                            list.forEach { listItem ->
                                if (listItem is Map<*, *>) {
                                    (listItem[DefinedParams.ViewScreens] as? ArrayList<*>)?.let { viewScreens ->
                                        val hasVs =
                                            viewScreens.any { it.toString().equals(type, true) }
                                        if (hasVs) {
                                            val id = (listItem[DefinedParams.id] as? Double)
                                            (listItem[DefinedParams.FormInput] as? String)?.let { responseStr ->
                                                gson.fromJson(responseStr, FormResponse::class.java)
                                                    ?.let { formResponse ->
                                                        formLayouts.addAll(formResponse.formLayout.map { form ->
                                                            form.copy(
                                                                customizedWorkflowId = id
                                                            )
                                                        })
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        gson.fromJson(item, FormResponse::class.java)?.let { formResponse ->
                            formLayouts.addAll(formResponse.formLayout)
                        }
                    }
                }
                ncdFormResponse.postSuccess(formLayouts)
            } catch (e: Exception) {
                ncdFormResponse.postError()
            }
        }
    }
}