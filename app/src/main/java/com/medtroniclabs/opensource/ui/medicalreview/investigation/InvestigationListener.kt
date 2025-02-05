package com.medtroniclabs.opensource.ui.medicalreview.investigation

import com.medtroniclabs.opensource.model.medicalreview.InvestigationModel

interface InvestigationListener {
    fun removeInvestigation(investigationGenerator: InvestigationModel)
    fun checkValidation()
    fun markAsReviewed(id: String?, comments: String?)
}