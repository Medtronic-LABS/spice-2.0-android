package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.formgeneration.model.FormLayout

data class ExaminationModel(
    val diseaseName: String,
    val questionnaires: ArrayList<FormLayout>
)
