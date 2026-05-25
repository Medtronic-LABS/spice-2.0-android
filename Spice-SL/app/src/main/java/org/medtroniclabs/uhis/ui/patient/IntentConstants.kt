package org.medtroniclabs.uhis.ui.patient

import android.provider.ContactsContract

object IntentConstants {
    const val INTENT_ENROLLMENT = "enrollment_workflow"
    const val IS_FROM_DIRECT_ENROLLMENT = "isFromDirectEnrollment"
    const val INTENT_PATIENT_ID = "selectedPatientID"
    const val INTENT_VISIT_ID = "patientVisitId"
    const val INTENT_PATIENT_INITIAL = "intentPatientInitial"
    const val INTENT_SPINNER_ITEMS = "intent_spinner_items"
    const val INTENT_SPINNER_SELECTED_ITEM = "intent_spinner_selected_item"
    const val INTENT_ID = "intent_id"
    const val INTENT_SPINNER_LABEL = "intent_spinner_label"
    const val ACTION_SESSION_EXPIRED =
        "${ContactsContract.Directory.PACKAGE_NAME}.action.NCD_SESSION_EXPIRED"
    const val ACTION_UPDATE_APP =
        "${ContactsContract.Directory.PACKAGE_NAME}.action.NCD_UPDATE_APP"
    const val NCD_SESSION = "ncd_session"
    const val INTENT_FORM_ID = "intent_form_id"
    const val SHOW_CONTINUOUS_MEDICAL_REVIEW = "showContinuousMedicalReview"
    const val IS_FROM_SUMMARY_PAGE = "is_from_summary_page"
    const val INTENT_SCREENING = "screening_workflow"

    const val APP_UPDATE_REQUEST_CODE = 258
    const val UPDATE_REQUIRED = "update_required"
    const val INTENT_MESSAGE = "inten_message"

    const val NURSE_RESPONE = "nurseRespone"

    const val INTENT_EYE_SCREENING = "eye_screening_workflow"
    const val INTENT_CATARACT = "cataract_workflow"
}
