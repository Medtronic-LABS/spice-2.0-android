package org.medtroniclabs.uhis.model.services

import androidx.room.ColumnInfo

/**
 * Aggregated service-member counters computed from a single SQL query.
 *
 * Each property maps to one static service filter bucket shown in the Services UI.
 */
data class ServiceMemberCounts(
    /** Total members after applying dynamic filters (search, SS, sub-village). */
    @ColumnInfo("all_members") val allMembers: Int,
    /** Members eligible for family-planning counseling. */
    @ColumnInfo("family_planning") val familyPlanning: Int,
    /** Members currently identified as pregnant. */
    @ColumnInfo("pregnant_women") val pregnantWomen: Int,
    /** Pregnant members marked as high-risk. */
    @ColumnInfo("high_risk_pregnant") val highRiskPregnant: Int,
    /** Mothers in the postnatal-care window. */
    @ColumnInfo("postnatal_mothers") val postnatalMothers: Int,
    /** Children below two years of age. */
    @ColumnInfo("children_under_two") val childrenUnderTwo: Int,
    /** Members registered without a household linkage. */
    @ColumnInfo("external_members") val externalMembers: Int,
    /** External members currently pregnant and active. */
    @ColumnInfo("external_pregnant") val externalPregnant: Int,
    /** Pregnancies with expected delivery in next 30 days. */
    @ColumnInfo("expected_deliveries") val expectedDeliveries: Int,
    /** Pregnancies with overdue expected delivery. */
    @ColumnInfo("pending_deliveries") val pendingDeliveries: Int,
    /** Members with NCD assessment history (FO/PO service recipient filters). */
    @ColumnInfo("ncd_services") val ncdServices: Int,
    @ColumnInfo("cataract_screening") val cataractScreening: Int,
    @ColumnInfo("eye_screening") val eyeScreening: Int,
)
