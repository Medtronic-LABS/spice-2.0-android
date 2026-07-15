package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LabTestModel(
    var testName: String? = null,
    var referredBy: Any? = null,
    var referredByDisplay: String? = null,
    var comment: String? = null,
    var resultComments: String? = null,
    var isReviewed: Boolean? = false,
    var patientLabtestResults: ArrayList<HashMap<String, Any>>? = null,
    var labTestId: Long? = null,
    var labTestName: String? = null,
    var referredDate: String? = null,
    var resultDate: String? = null,
    @SerializedName("id")
    val _id: Long? = null,
    var resultUpdateBy: HashMap<String, String>? = null,
    var patientVisitId: Long? = null,
    var resultDetails: ArrayList<Map<String, Any>>? = null,
    var referDateDisplay: String? = null,
    var isAbnormal: Boolean? = false,
    // FHIR-port fields: the investigation/* endpoints identify records with String ids and need
    // the lab test's form definition (result fields) which is delivered inline in the list response.
    var fhirId: String? = null,
    var recommendedById: String? = null,
    var formInput: String? = null,
    // Result values delivered inline by investigation/list; kept separately from the transient
    // [resultDetails] (which the adapter clears on collapse) so the dropdown can re-render them.
    var labResultDetails: ArrayList<Map<String, Any>>? = null,
) : Serializable
