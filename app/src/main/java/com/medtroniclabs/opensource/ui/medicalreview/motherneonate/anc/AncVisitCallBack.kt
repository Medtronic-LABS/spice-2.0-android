package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc

import com.medtroniclabs.opensource.model.PatientListRespModel

interface AncVisitCallBack {
    fun onDataLoaded(details: PatientListRespModel)
}