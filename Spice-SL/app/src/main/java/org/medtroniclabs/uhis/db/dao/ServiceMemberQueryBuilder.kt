package org.medtroniclabs.uhis.db.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter

/**
 * Builds the member list query for [MemberDAO.getServiceMembers].
 * Filter predicates come from [ServiceFilterConditions].
 */
internal object ServiceMemberQueryBuilder {
    /**
     * The selected sub-village area, ready to splice into the query.
     */
    data class AreaFilter(
        val withClause: String,
        val householdMatch: String?,
        val memberMatch: String?,
    )

    fun buildAreaFilter(
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        args: MutableList<Any>,
    ): AreaFilter {
        val withClause: String =
            when {
                filterBySubVillages.isNotEmpty() -> {
                    val rows = filterBySubVillages.joinToString(",") { "(?)" }
                    args.addAll(filterBySubVillages)
                    "WITH allowed_sub_villages(subVillageId) AS (VALUES $rows)"
                }
                filterBySs.isNotEmpty() -> {
                    val placeHolders = filterBySs.joinToString(",") { "?" }
                    args.addAll(filterBySs)
                    """
                    WITH allowed_sub_villages(subVillageId) AS (
                        SELECT DISTINCT sslv.subVillageId
                        FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                        WHERE sslv.shasthyaShebikaId IN ($placeHolders)
                    )
                    """.trimIndent()
                }
                else -> return AreaFilter("", null, null)
            }
        return AreaFilter(
            withClause = withClause,
            householdMatch = "hh.sub_village_id IN (SELECT subVillageId FROM allowed_sub_villages)",
            memberMatch = "hhm.sub_village_id IN (SELECT subVillageId FROM allowed_sub_villages)",
        )
    }

    fun buildListQuery(
        searchInput: String?,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        staticFilter: ServiceStaticFilter,
        allowNullHousehold: Boolean,
        qrCode: String?,
        restrictExternalToSkCreator: Boolean = false,
    ): SimpleSQLiteQuery {
        val args = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        val isExternalMember =
            staticFilter == ServiceStaticFilter.EXTERNAL_MEMBERS ||
                staticFilter == ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN

        val useOptionalHouseholdJoins = isExternalMember || allowNullHousehold
        val area = buildAreaFilter(filterBySs, filterBySubVillages, args)
        val areaMatch = if (useOptionalHouseholdJoins) area.memberMatch else area.householdMatch
        areaMatch?.let { conditions += it }

        if (!ServiceFilterConditions.skipsActiveCheck(staticFilter)) {
            conditions += ServiceFilterConditions.IS_ACTIVE
        }

        if (!searchInput.isNullOrBlank()) {
            conditions += "(hhm.name LIKE ? OR hhm.phone_number LIKE ? OR hhm.national_id LIKE ?)"
            val pattern = "%${searchInput.trim()}%"
            args += pattern
            args += pattern
            args += pattern
        }

        if (!qrCode.isNullOrBlank()) {
            conditions += "hhm.qr_code = ?"
            args += qrCode
        }

        when (staticFilter) {
            ServiceStaticFilter.EXTERNAL_MEMBERS -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                if (ServiceFilterConditions.isSkScopedExternalFilter(staticFilter, restrictExternalToSkCreator)) {
                    conditions += ServiceFilterConditions.SK_SCOPED_EXTERNAL_CREATOR
                }
            }
            ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                if (ServiceFilterConditions.isSkScopedExternalFilter(staticFilter, restrictExternalToSkCreator)) {
                    conditions += ServiceFilterConditions.SK_SCOPED_EXTERNAL_CREATOR
                }
                conditions += ServiceFilterConditions.ACTIVE_PREGNANCY
            }
            ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS -> {
                conditions += ServiceFilterConditions.CHILDREN_UNDER_TWO
            }
            ServiceStaticFilter.NCD_SERVICES,
            ServiceStaticFilter.CATARACT_SCREENING,
            ServiceStaticFilter.EYE_SCREENING,
            -> {
                conditions += ServiceFilterConditions.assessmentCohortCondition(staticFilter)
            }
            ServiceStaticFilter.ALL_MEMBERS -> {}
            else -> {
                conditions += ServiceFilterConditions.pregnancyCohortCondition(staticFilter)
            }
        }

        val whereClause =
            if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"

        val householdJoin =
            if (useOptionalHouseholdJoins) {
                "LEFT JOIN Household AS hh ON hh.id = hhm.household_id"
            } else {
                "INNER JOIN Household AS hh ON hh.id = hhm.household_id"
            }

        // ss/sv supply display names only (COALESCE'd below); the village scope is enforced by the
        // WHERE areaMatch. A reassigned household keeps its original SS stamp, which may not exist in
        // this device's ShasthyaShebikaEntity — an INNER join here wrongly hides those members, so the
        // newly-assigned village's members never appear (UHIS-1173). Use LEFT so these never filter.
        // Display new ss name based on village where the old ss is not there in the DB.
        val ssJoin =
            if (useOptionalHouseholdJoins) {
                """
                LEFT JOIN ShasthyaShebikaEntity AS ss
                    ON ss.id = hhm.shasthya_shebika_id

                LEFT JOIN ShasthyaShebikaLinkedVillageEntity AS sslv
                    ON sslv.subVillageId = hhm.sub_village_id
                    AND EXISTS (
                        SELECT 1
                        FROM ShasthyaShebikaEntity AS active_ss
                        WHERE active_ss.id = sslv.shasthyaShebikaId
                            AND active_ss.isActive = true
                    )
                LEFT JOIN ShasthyaShebikaEntity AS ss_fallback
                    ON ss_fallback.id = sslv.shasthyaShebikaId
                """.trimIndent()
            } else {
                """
                LEFT JOIN ShasthyaShebikaEntity AS ss
                    ON ss.id = hh.shasthya_shebika_id

                LEFT JOIN ShasthyaShebikaLinkedVillageEntity AS sslv
                    ON sslv.subVillageId = hh.sub_village_id
                    AND EXISTS (
                        SELECT 1
                        FROM ShasthyaShebikaEntity AS active_ss
                        WHERE active_ss.id = sslv.shasthyaShebikaId
                            AND active_ss.isActive = true
                    )
                LEFT JOIN ShasthyaShebikaEntity AS ss_fallback
                    ON ss_fallback.id = sslv.shasthyaShebikaId
                """.trimIndent()
            }

        val svJoin =
            if (useOptionalHouseholdJoins) {
                "LEFT JOIN SubVillageEntity AS sv ON hhm.sub_village_id = sv.id"
            } else {
                "LEFT JOIN SubVillageEntity AS sv ON sv.id = hh.sub_village_id"
            }

        val orderByClause =
            when (staticFilter) {
                ServiceStaticFilter.EXPECTED_DELIVERIES ->
                    "ORDER BY substr(lp.estimatedDeliveryDate, 1, 10) ASC, hhm.id DESC"
                ServiceStaticFilter.PENDING_DELIVERIES ->
                    "ORDER BY substr(lp.estimatedDeliveryDate, 1, 10) DESC, hhm.id DESC"
                else ->
                    "ORDER BY COALESCE(${ServiceFilterConditions.RECENT_SERVICE_DATE_EXPR}, hhm.created_at) DESC, hhm.id DESC"
            }

        val withPrefix = if (area.withClause.isNotEmpty()) "${area.withClause}\n" else ""

        val query =
            """
            $withPrefix
            SELECT
                hhm.*, td.diagnoses,
                ${ServiceFilterConditions.RECENT_SERVICE_DATE_EXPR} AS recent_service_date,
                COALESCE(ss.name, ss_fallback.name, '') AS shasthya_shebika_name,
                COALESCE(ss.ssId, ss_fallback.ssId, '') AS shasthya_shebika_ssId,
                COALESCE(sv.name, '') AS sub_village_name
            FROM householdmember AS hhm

            $householdJoin

            $ssJoin

            $svJoin

            LEFT JOIN TreatmentDetailsEntity AS td
                ON hhm.fhir_id = td.memberId

            ${ServiceFilterConditions.LATEST_PREGNANCY_JOIN}

            $whereClause
            $orderByClause
            """.trimIndent()

        return SimpleSQLiteQuery(query, args.toTypedArray())
    }
}
