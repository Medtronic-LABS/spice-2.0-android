package com.medtroniclabs.opensource.model.medicalreview

import com.medtroniclabs.opensource.data.CodeDetailsObject
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.model.LabTestResultObject

data class InvestigationModel(
    val testName: String,
    val resultList: FormResponse? = null,
    val recommendedByName: String? = null,
    val recommendedBy: String,
    val recommendedOn: String,
    var resultHashMap: HashMap<String, Any>? = null,
    val codeDetails: CodeDetailsObject? = null,
    var dataError: Boolean = true,
    var labTestResultList: ArrayList<LabTestResultObject>? = null,
    val id: String ?= null,
    var dropdownState: Boolean = false,
    val isReview: Boolean? = null
)