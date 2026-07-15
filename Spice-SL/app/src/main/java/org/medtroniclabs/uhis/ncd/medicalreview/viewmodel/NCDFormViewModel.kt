package org.medtroniclabs.uhis.ncd.medicalreview.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.ncd.medicalreview.repo.NCDFormsRepo
import org.medtroniclabs.uhis.network.SingleLiveEvent
import org.medtroniclabs.uhis.network.resource.Resource
import javax.inject.Inject

@HiltViewModel
class NCDFormViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val ncdFormsRepo: NCDFormsRepo,
) : ViewModel() {
    val ncdFormResponse = SingleLiveEvent<Resource<List<FormLayout>>>()

    fun getNCDForm(
        type: String,
        workFlow: String? = null,
    ) {
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
                                            val id = (listItem[DefinedParams.ID] as? Double)
                                            (listItem[DefinedParams.FormInput] as? String)?.let { responseStr ->
                                                gson
                                                    .fromJson(responseStr, FormResponse::class.java)
                                                    ?.let { formResponse ->
                                                        formLayouts.addAll(
                                                            formResponse.formLayout.map { form ->
                                                                form.copy(
                                                                    customizedWorkflowId = id,
                                                                )
                                                            },
                                                        )
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

    /**
     * Loads a form layout bundled as an asset (e.g. the community patient-edit form which is not
     * synced from the backend) and posts it through the same [ncdFormResponse] stream.
     */
    fun getFormFromAsset(
        context: Context,
        fileName: String,
    ) {
        viewModelScope.launch(dispatcherIO) {
            ncdFormResponse.postLoading()
            try {
                val json = CommonUtils.getStringFromAssets(fileName, context.assets)
                val formResponse = Gson().fromJson(json, FormResponse::class.java)
                ncdFormResponse.postSuccess(formResponse.formLayout)
            } catch (e: Exception) {
                ncdFormResponse.postError()
            }
        }
    }
}
