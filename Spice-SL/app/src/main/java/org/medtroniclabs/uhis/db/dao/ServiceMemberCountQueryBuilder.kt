package org.medtroniclabs.uhis.db.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter

/**
 * Builds one `SELECT` that returns every requested service-filter count in a single table scan.
 *
 * Each filter maps to a `SUM(CASE WHEN <predicate> THEN 1 ELSE 0 END) AS cnt_<index>` column,
 * where `<index>` is the filter's position in the list. The cohort rules mirror
 * [MemberDAO.getServiceMembers] so list and count stay consistent.
 */
internal object ServiceMemberCountQueryBuilder {
    /** Prefix for the generated count columns; the suffix is the filter's index (`cnt_0`, `cnt_1`, ...). */
    const val COUNT_COLUMN_PREFIX = "cnt_"

    fun buildCombinedCountQuery(
        filters: List<ServiceStaticFilter>,
        searchInput: String,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        allowNullHousehold: Boolean,
        qrCode: String?,
    ): SimpleSQLiteQuery {
        val args = mutableListOf<Any>()

        val area = ServiceMemberQueryBuilder.buildAreaFilter(filterBySs, filterBySubVillages, args)

        val selectColumns =
            filters.mapIndexed { index, filter ->
                val predicate = buildFilterPredicate(filter, allowNullHousehold, area)
                "SUM(CASE WHEN $predicate THEN 1 ELSE 0 END) AS $COUNT_COLUMN_PREFIX$index"
            }

        val whereConditions = mutableListOf<String>()
        if (searchInput.isNotBlank()) {
            whereConditions += "(hhm.name LIKE ? OR hhm.phone_number LIKE ?)"
            val pattern = "%${searchInput.trim()}%"
            args += pattern
            args += pattern
        }
        if (!qrCode.isNullOrBlank()) {
            whereConditions += "hhm.qr_code LIKE ?"
            args += "%${qrCode.trim()}%"
        }
        val whereClause =
            if (whereConditions.isEmpty()) "" else "WHERE ${whereConditions.joinToString(" AND ")}"

        val withPrefix = if (area.withClause.isNotEmpty()) "${area.withClause}\n" else ""

        val query =
            """
            $withPrefix
            SELECT
                ${selectColumns.joinToString(",\n                ")}
            FROM householdmember AS hhm
            LEFT JOIN Household AS hh ON hh.id = hhm.household_id
            ${ServiceFilterConditions.LATEST_PREGNANCY_JOIN}
            $whereClause
            """.trimIndent()

        return SimpleSQLiteQuery(query, args.toTypedArray())
    }

    private fun buildFilterPredicate(
        staticFilter: ServiceStaticFilter,
        allowNullHousehold: Boolean,
        area: ServiceMemberQueryBuilder.AreaFilter,
    ): String {
        val conditions = mutableListOf<String>()

        fun addHouseholdScope() {
            conditions += ServiceFilterConditions.HAS_HOUSEHOLD
            area.householdMatch?.let { conditions += it }
        }

        fun addExternalScope() {
            area.memberMatch?.let { conditions += it }
        }

        when (staticFilter) {
            ServiceStaticFilter.ALL_MEMBERS -> {
                if (allowNullHousehold) addExternalScope() else addHouseholdScope()
            }
            ServiceStaticFilter.EXTERNAL_MEMBERS -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                addExternalScope()
            }
            ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                addExternalScope()
                conditions += ServiceFilterConditions.IS_ACTIVE
                conditions += ServiceFilterConditions.ACTIVE_PREGNANCY
            }
            ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS -> {
                addHouseholdScope()
                conditions += ServiceFilterConditions.CHILDREN_UNDER_TWO
            }
            ServiceStaticFilter.NCD_SERVICES,
            ServiceStaticFilter.CATARACT_SCREENING,
            ServiceStaticFilter.EYE_SCREENING,
            -> {
                if (allowNullHousehold) addExternalScope() else addHouseholdScope()
                conditions += ServiceFilterConditions.IS_ACTIVE
                conditions += ServiceFilterConditions.assessmentCohortCondition(staticFilter)
            }
            else -> {
                addHouseholdScope()
                conditions += ServiceFilterConditions.IS_ACTIVE
                conditions += ServiceFilterConditions.pregnancyCohortCondition(staticFilter)
            }
        }

        return if (conditions.isEmpty()) "1" else conditions.joinToString(" AND ") { "($it)" }
    }
}
