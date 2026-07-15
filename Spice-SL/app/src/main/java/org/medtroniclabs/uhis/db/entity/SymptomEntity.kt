package org.medtroniclabs.uhis.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Symptom")
class SymptomEntity(
    @PrimaryKey
    @SerializedName("id")
    val _id: Long,
    @SerializedName("name")
    val symptom: String,
    var type: String? = null,
    @ColumnInfo(name = "culture_value")
    val cultureValue: String? = null,
    @ColumnInfo(name = "display_order")
    var displayOrder: Int? = null,
) {
    @Ignore
    var isSelected = false
}
