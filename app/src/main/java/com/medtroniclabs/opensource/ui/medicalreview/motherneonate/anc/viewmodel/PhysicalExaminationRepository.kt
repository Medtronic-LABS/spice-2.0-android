package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.db.local.RoomHelper
import javax.inject.Inject

class PhysicalExaminationRepository @Inject constructor(
    private var roomHelper: RoomHelper
) {
    fun getExaminationsComplaintByTypeLiveData(category: String): LiveData<List<MedicalReviewMetaItems>> {
        return roomHelper.getExaminationsComplaintByTypeLiveData(category)
    }
}