package com.medtroniclabs.opensource.model

import com.medtroniclabs.opensource.db.entity.DosageDurationEntity
import com.medtroniclabs.opensource.db.entity.LifestyleEntity
import com.medtroniclabs.opensource.db.entity.NCDMedicalReviewMetaEntity
import com.medtroniclabs.opensource.db.entity.TreatmentPlanEntity

data class NcdMRStaticDataModel(
    val comorbidity: List<NCDMedicalReviewMetaEntity>,
    val complications: List<NCDMedicalReviewMetaEntity>,
    val lifestyle: List<LifestyleEntity>,
    val complaints: List<NCDMedicalReviewMetaEntity>,
    val physicalExamination: List<NCDMedicalReviewMetaEntity>,
    val currentMedication: List<NCDMedicalReviewMetaEntity>,
    val frequencies: List<TreatmentPlanEntity>,
    val frequencyTypes: List<NCDMedicalReviewMetaEntity>,
    val nutritionLifestyles: List<NCDMedicalReviewMetaEntity>,
    val dosageDuration: List<DosageDurationEntity>? = null
)
