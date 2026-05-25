package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "UnionEntity")
class UnionEntity(
    @PrimaryKey
    @SerializedName("id")
    val _id: Long,
    val name: String,
    val code: String? = null,
    val upazilaCode: String? = null,
    var userUnion: Boolean = false,
    var subCountyId: Int? = null,
)
