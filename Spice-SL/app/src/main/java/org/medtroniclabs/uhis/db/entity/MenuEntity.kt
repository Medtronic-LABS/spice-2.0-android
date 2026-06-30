package org.medtroniclabs.uhis.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "MenuEntity")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    var menuId: String,
    val roleName: String? = null,
    val name: String,
    val displayOrder: Int,
    var subModule: String? = null,
    @ColumnInfo(name = "culture_value")
    val displayValue: String? = null,
) {
    @Ignore
    var isDisabled: Boolean = false

    @Ignore
    var navigationBlockedMessage: String? = null
}
