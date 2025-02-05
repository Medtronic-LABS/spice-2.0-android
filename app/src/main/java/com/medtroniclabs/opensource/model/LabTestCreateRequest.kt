package com.medtroniclabs.opensource.model

import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.CodeDetailsObject
import com.medtroniclabs.opensource.data.EncounterDetails

data class LabTestCreateRequest(
    val encounter: EncounterDetails,
    val labTests: ArrayList<LabTestDetails>,
    val enrollmentType: String? = null,
    val identityValue: String? = null,
    val requestFrom: String = CommonUtils.requestFrom()
)

data class LabTestDetails(
    val testName: String,
    val recommendedBy: String,
    val recommendedName: String? = null,
    val recommendedOn: String,
    val codeDetails: CodeDetailsObject? = null,
    val labTestResults: ArrayList<LabTestResultObject>? = null,
    val id: String? = null
)

data class LabTestResultObject(
    var name: String,
    var value: Any? = null,
    var performedBy: String,
    val codeDetails: CodeDetailsObject? = null,
    val testedOn: String? = null,
    var resource: String? = null,
    var unit: String? = null
)