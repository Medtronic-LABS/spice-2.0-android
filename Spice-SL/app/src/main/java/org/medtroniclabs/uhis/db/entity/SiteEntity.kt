package org.medtroniclabs.uhis.db.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "SiteEntity")
data class SiteEntity(
    @PrimaryKey
    var id: Long,
    var name: String,
    var userSite: Boolean = false,
    var role: String? = null,
    var roleName: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var userId: Long,
    var tenantId: Long,
    var countyId: Long? = null,
    var accountId: Long? = null,
    var subCountyId: Long? = null,
    var isDefault: Boolean = false,
    @ColumnInfo("isQualipharmEnabledSite")
    var isQualipharmEnabledSite: Boolean? = null,
    val siteLevel: String? = null,
    val countryName: String? = null,
    val countyName: String? = null,
    val code: String? = null,
    var bpLog: Boolean = false,
    var glucoseLog: Boolean = false,
    var phq4: Boolean = false,
    var cataract: Boolean = false,
    var eyeCare: Boolean = false,
    val subCountyName: String? = null,
) : Serializable
