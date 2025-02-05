package com.medtroniclabs.opensource.data

data class LabourDeliveryMetaResponse(
    val symptoms: List<LabourDeliveryMetaEntity>,
    val deliveryAt: List<LabourDeliveryMetaEntity>,
    val deliveryBy: List<LabourDeliveryMetaEntity>,
    val deliveryType: List<LabourDeliveryMetaEntity>,
    val deliveryStatus: List<LabourDeliveryMetaEntity>,
    val neonateOutcome: List<LabourDeliveryMetaEntity>,
    val riskFactors: List<LabourDeliveryMetaEntity>,
    val conditionOfMother: List<LabourDeliveryMetaEntity>,
    val motherDeliveryStatus: List<LabourDeliveryMetaEntity>,
    val stateOfPerineum:List<LabourDeliveryMetaEntity>
)
