package com.medtroniclabs.opensource.ui.mypatients

import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity

interface PatientSelectionListener {
    fun onSelectedPatient(item: PatientListRespModel)
}

interface PatientSelectionListenerForFollowUp {
    fun onSelectedPatientForCall(item: PatientFollowUpEntity)
    fun onSelectedPatientForAssessment(item: PatientFollowUpEntity)
    fun onSelectedPatientCard(item: PatientFollowUpEntity)
}

interface PatientSelectionListenerForFollowUpOffline {
    fun onSelectedPatientForCall(item: NCDFollowUp)
    fun onSelectedPatientForAssessment(item: NCDFollowUp)
    fun onSelectedPatientCard(item: NCDFollowUp)
}
