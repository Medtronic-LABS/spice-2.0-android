package com.medtroniclabs.opensource.db.entity

import com.google.gson.annotations.SerializedName


data class RiskClassificationModel(
    var age:String,
    @SerializedName("is_smoker")
    var isSmoker:Boolean,
    var gender:String,
    @SerializedName("risk_factors")
    var riskFactors:ArrayList<RiskFactorModel>
)

