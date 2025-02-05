package com.medtroniclabs.opensource.formgeneration.model

sealed class ConditionModelConfig {
    object VISIBILITY : ConditionModelConfig()
    object ENABLED : ConditionModelConfig()
}
