package com.medtroniclabs.opensource.formgeneration

import com.medtroniclabs.opensource.formgeneration.model.FormLayout

interface ExaminationListener {
    fun onDialogueCheckboxListener(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
        diseaseName: String
    )

    fun setResultHashMap(resultMap: HashMap<String, Any>)
}