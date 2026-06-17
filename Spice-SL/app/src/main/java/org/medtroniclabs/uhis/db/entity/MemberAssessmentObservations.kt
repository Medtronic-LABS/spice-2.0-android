package org.medtroniclabs.uhis.db.entity

data class MemberAssessmentObservations(
    val height: String? = null,
    val weight: String? = null,
    val bp: String? = null,
    val bg: String? = null,
    val gravida: String? = null,
    val parity: String? = null,
    val modeOfDelivery: String? = null,
    val anyComplicationsDuringDelivery: String? = null,
    val complicationsDuringDelivery: String? = null,
    val ancVisitNumber: String? = null,
    val fundalHeight: String? = null,
    val hemoglobin: String? = null,
    val pncVisitNumber: String? = null,
    val familyPlanningMethods: String? = null,
    val desireForChildrenInFuture: String? = null,
    val numberOfLivingChildren: String? = null,
)
