package com.medtroniclabs.opensource.repo

import com.medtroniclabs.opensource.db.local.RoomHelper
import javax.inject.Inject

class ExaminationsRepository @Inject constructor(
    val roomHelper: RoomHelper
) {

    suspend fun getExaminationQuestionsByWorkFlow(workFlowType: String) = roomHelper.getExaminationQuestionsByWorkFlow(workFlowType)

}