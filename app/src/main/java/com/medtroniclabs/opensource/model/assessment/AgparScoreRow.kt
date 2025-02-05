package com.medtroniclabs.opensource.model.assessment

import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparRowIdentifierType

data class AgparScoreRow(
    val indicatorType: AgparRowIdentifierType,
    val indicatorName: Int,
    val oneMinute: String? = null,
    val fiveMinute: String? = null,
    val tenMinute: String? = null
)
