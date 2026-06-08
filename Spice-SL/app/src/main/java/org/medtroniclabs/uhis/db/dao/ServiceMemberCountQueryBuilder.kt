package org.medtroniclabs.uhis.db.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter

/**
 * Builds per-filter `SELECT COUNT(*)` queries for service dropdown badge counts.
 *
 * Each query uses the same cohort and dynamic-filter rules as [MemberDAO.getServiceMembers].
 * Counts are fetched one filter at a time (see [MemberDAO.getServiceMemberCountForFilter]).
 */
internal object ServiceMemberCountQueryBuilder {
    /**
     * Household- and member-level sub-village SQL fragments for SS / sub-village chips.
     */
    private data class AreaFilters(
        val householdAreaCondition: String?,
        val householdAreaArgs: List<Any>,
        val externalAreaCondition: String?,
        val externalAreaArgs: List<Any>,
    )

    /**
     * Builds a Room [SimpleSQLiteQuery] that returns a single count for [staticFilter].
     *
     * @param searchInput optional name/phone `LIKE` filter; blank applies no search predicate
     * @param filterBySs when non-empty and [filterBySubVillages] is empty, restricts to SS-linked sub-villages
     * @param filterBySubVillages when non-empty, restricts to these sub-village IDs
     * @param allowNullHousehold FO/PO mode: member-level area for all-members and screening filters
     */
    fun buildCountQuery(
        staticFilter: ServiceStaticFilter,
        searchInput: String,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        allowNullHousehold: Boolean,
        qrCode: String?,
    ): SimpleSQLiteQuery {
        val areaFilters = buildAreaFilters(filterBySs, filterBySubVillages)
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (searchInput.isNotBlank()) {
            conditions += "(hhm.name LIKE ? OR hhm.phone_number LIKE ?)"
            val pattern = "%${searchInput.trim()}%"
            args += pattern
            args += pattern
        }

        // Search by QR code
        if (!qrCode.isNullOrBlank()) {
            conditions += "hhm.qr_code LIKE ?"

            val qrPattern = "%${qrCode.trim()}%"
            args += qrPattern
        }

        when (staticFilter) {
            ServiceStaticFilter.ALL_MEMBERS -> {
                if (allowNullHousehold) {
                    areaFilters.externalAreaCondition?.let { conditions += it }
                    args.addAll(areaFilters.externalAreaArgs)
                } else {
                    conditions += ServiceFilterConditions.HAS_HOUSEHOLD
                    areaFilters.householdAreaCondition?.let { conditions += it }
                    args.addAll(areaFilters.householdAreaArgs)
                }
            }
            ServiceStaticFilter.EXTERNAL_MEMBERS -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                areaFilters.externalAreaCondition?.let { conditions += it }
                args.addAll(areaFilters.externalAreaArgs)
            }
            ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                areaFilters.externalAreaCondition?.let { conditions += it }
                args.addAll(areaFilters.externalAreaArgs)
                conditions += ServiceFilterConditions.IS_ACTIVE
                conditions += ServiceFilterConditions.PREGNANT_WOMEN
            }
            ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS -> {
                conditions += ServiceFilterConditions.HAS_HOUSEHOLD
                areaFilters.householdAreaCondition?.let { conditions += it }
                args.addAll(areaFilters.householdAreaArgs)
                conditions += ServiceFilterConditions.CHILDREN_UNDER_TWO
            }
            ServiceStaticFilter.NCD_SERVICES,
            ServiceStaticFilter.CATARACT_SCREENING,
            ServiceStaticFilter.EYE_SCREENING,
            -> {
                if (allowNullHousehold) {
                    areaFilters.externalAreaCondition?.let { conditions += it }
                    args.addAll(areaFilters.externalAreaArgs)
                } else {
                    conditions += ServiceFilterConditions.HAS_HOUSEHOLD
                    areaFilters.householdAreaCondition?.let { conditions += it }
                    args.addAll(areaFilters.householdAreaArgs)
                }
                conditions += ServiceFilterConditions.IS_ACTIVE
                conditions += cohortCondition(staticFilter)
            }
            else -> {
                conditions += ServiceFilterConditions.HAS_HOUSEHOLD
                areaFilters.householdAreaCondition?.let { conditions += it }
                args.addAll(areaFilters.householdAreaArgs)
                conditions += ServiceFilterConditions.IS_ACTIVE
                conditions += cohortCondition(staticFilter)
            }
        }

        val whereClause =
            if (conditions.isEmpty()) {
                ""
            } else {
                "WHERE ${conditions.joinToString(" AND ")}"
            }

        val query =
            """
            SELECT COUNT(*)
            FROM householdmember AS hhm
            LEFT JOIN Household AS hh ON hh.id = hhm.household_id
            $whereClause
            """.trimIndent()

        return SimpleSQLiteQuery(query, args.toTypedArray())
    }

    /**
     * Cohort SQL for RMNCH and FO/PO screening filters.
     *
     * @throws IllegalArgumentException for filters without a cohort (e.g. [ServiceStaticFilter.ALL_MEMBERS])
     */
    private fun cohortCondition(staticFilter: ServiceStaticFilter): String =
        when (staticFilter) {
            ServiceStaticFilter.FAMILY_PLANNING_COUNSELLING_ELIGIBLE -> ServiceFilterConditions.FAMILY_PLANNING
            ServiceStaticFilter.PREGNANT_WOMEN -> ServiceFilterConditions.PREGNANT_WOMEN
            ServiceStaticFilter.HIGH_RISK_PREGNANT_WOMEN -> ServiceFilterConditions.HIGH_RISK_PREGNANT_WOMEN
            ServiceStaticFilter.POSTNATAL_CARE_MOTHERS -> ServiceFilterConditions.POSTNATAL_MOTHERS
            ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS -> ServiceFilterConditions.CHILDREN_UNDER_TWO
            ServiceStaticFilter.EXPECTED_DELIVERIES -> ServiceFilterConditions.EXPECTED_DELIVERIES
            ServiceStaticFilter.PENDING_DELIVERIES -> ServiceFilterConditions.PENDING_DELIVERIES
            ServiceStaticFilter.NCD_SERVICES -> ServiceFilterConditions.HAS_NCD_SERVICE_HISTORY
            ServiceStaticFilter.CATARACT_SCREENING -> ServiceFilterConditions.HAS_CATARACT_SCREENING_HISTORY
            ServiceStaticFilter.EYE_SCREENING -> ServiceFilterConditions.HAS_EYE_SCREENING_HISTORY
            else -> error("No cohort condition for $staticFilter")
        }

    /**
     * Builds parallel area predicates: `hh.sub_village_id` for household members and
     * `hhm.sub_village_id` for external / FO/PO member-list scoping.
     */
    private fun buildAreaFilters(
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
    ): AreaFilters {
        val placeHolders =
            when {
                filterBySubVillages.isNotEmpty() -> filterBySubVillages.joinToString(",") { "?" }
                filterBySs.isNotEmpty() -> filterBySs.joinToString(",") { "?" }
                else -> ""
            }

        val householdSubVillageFilters = mutableListOf<String>()
        val householdFilterArgs = mutableListOf<Any>()
        if (filterBySubVillages.isNotEmpty()) {
            householdSubVillageFilters += "hh.sub_village_id IN ($placeHolders)"
            householdFilterArgs.addAll(filterBySubVillages)
        } else if (filterBySs.isNotEmpty()) {
            householdSubVillageFilters +=
                """
                hh.sub_village_id IN (
                    SELECT DISTINCT sslv.subVillageId
                    FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                    WHERE sslv.shasthyaShebikaId IN ($placeHolders)
                )
                """.trimIndent()
            householdFilterArgs.addAll(filterBySs)
        }

        val externalSubVillageFilters = mutableListOf<String>()
        val externalFilterArgs = mutableListOf<Any>()
        if (filterBySubVillages.isNotEmpty()) {
            externalSubVillageFilters += "hhm.sub_village_id IN ($placeHolders)"
            externalFilterArgs.addAll(filterBySubVillages)
        } else if (filterBySs.isNotEmpty()) {
            externalSubVillageFilters +=
                """
                hhm.sub_village_id IN (
                    SELECT DISTINCT sslv.subVillageId
                    FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                    WHERE sslv.shasthyaShebikaId IN ($placeHolders)
                )
                """.trimIndent()
            externalFilterArgs.addAll(filterBySs)
        }

        val householdAreaCondition =
            if (householdSubVillageFilters.isNotEmpty()) {
                "(${householdSubVillageFilters.joinToString(" OR ")})"
            } else {
                null
            }
        val externalAreaCondition =
            if (externalSubVillageFilters.isNotEmpty()) {
                "(${externalSubVillageFilters.joinToString(" OR ")})"
            } else {
                null
            }

        return AreaFilters(
            householdAreaCondition = householdAreaCondition,
            householdAreaArgs = householdFilterArgs,
            externalAreaCondition = externalAreaCondition,
            externalAreaArgs = externalFilterArgs,
        )
    }
}
