package com.medtroniclabs.opensource.data.model


data class RecommendedDosageListModel(
    val tableId: Int,
    val title: String?,
    val columnName1: String,
    val columnName2: String,
    val columnName3: String,
    val dosageFrequency: ArrayList<DosageItemModel>? = null,
    val descriptionTitle: String?=null,
    val descriptionList: ArrayList<String>? = null
)

data class DosageItemModel(
    val minMonth: Int,
    val maxMonth:Int,
    val monthLabel: String,
    val warning: String?=null,
    val routine: ArrayList<DosageTableModel>? = null
)

data class DosageTableModel(
    val day: String,
    val morning: String,
    val night:String? = null,
)