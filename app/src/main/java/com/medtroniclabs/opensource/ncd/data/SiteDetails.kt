package com.medtroniclabs.opensource.ncd.data

data class SiteDetails(
    var category: String = "",
    var categoryType: String? = null,
    var siteName: String = "",
    var siteId: Long? = null,
    var tenantId: Long? = null,
    var categoryDisplayType: String? = null,
    var categoryDisplayName: String? = null,
    var otherType: String? = null,
    var counselorId: Long? = null,
    var userSiteId: Long? = null
)
