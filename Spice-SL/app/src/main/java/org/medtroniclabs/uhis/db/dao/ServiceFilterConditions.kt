package org.medtroniclabs.uhis.db.dao

import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.mappingkey.MemberRegistration
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter

/**
 * SQL condition and query fragments for service member filters.
 * Used by [ServiceMemberQueryBuilder] and [ServiceMemberCountQueryBuilder].
 *
 * Pregnancy cohort predicates assume [LATEST_PREGNANCY_JOIN] is present in the query (`lp` alias).
 */
object ServiceFilterConditions {
    val RECENT_SERVICE_DATE_EXPR =
        """
        (SELECT MAX(strftime('%s', visitDate) * 1000)
         FROM MemberAssessmentHistory mah
         WHERE mah.memberId = hhm.id)
        """.trimIndent()

    val LATEST_PREGNANCY_JOIN =
        """
        LEFT JOIN (
            SELECT
                pd.householdMemberLocalId,
                pd.dateOfDelivery,
                pd.lastMenstrualPeriod,
                pd.estimatedDeliveryDate,
                pd.highRiskPregnantWoman,
                pd.typeOfAbortion,
                ROW_NUMBER() OVER (
                    PARTITION BY pd.householdMemberLocalId
                    ORDER BY pd.endAt DESC, pd.id DESC
                ) AS rn
            FROM PregnancyDetail AS pd
        ) AS lp ON lp.householdMemberLocalId = hhm.id AND lp.rn = 1
        """.trimIndent()

    /** Latest pregnancy is ongoing: valid LMP, no delivery/abortion, and EDD not long past. */
    const val ACTIVE_PREGNANCY =
        "lp.lastMenstrualPeriod IS NOT NULL AND lp.lastMenstrualPeriod != '' " +
            "AND (lp.dateOfDelivery IS NULL OR lp.dateOfDelivery = '') " +
            "AND (lp.estimatedDeliveryDate IS NULL OR substr(lp.estimatedDeliveryDate, 1, 10) >= date('now', '-45 days')) " +
            "AND (lp.typeOfAbortion IS NULL OR lp.typeOfAbortion = '')"

    const val HIGH_RISK_PREGNANT =
        "$ACTIVE_PREGNANCY AND lp.highRiskPregnantWoman IS NOT NULL AND lp.highRiskPregnantWoman != ''"

    const val POSTNATAL =
        "lp.dateOfDelivery IS NOT NULL AND lp.dateOfDelivery != '' " +
            "AND substr(lp.dateOfDelivery, 1, 10) >= date('now', '-42 days')"

    private const val AWAITING_DELIVERY =
        "(lp.dateOfDelivery IS NULL OR lp.dateOfDelivery = '') " +
            "AND (lp.typeOfAbortion IS NULL OR lp.typeOfAbortion = '') " +
            "AND lp.estimatedDeliveryDate IS NOT NULL AND lp.estimatedDeliveryDate != ''"

    const val EXPECTED_DELIVERY =
        "$AWAITING_DELIVERY " +
            "AND substr(lp.estimatedDeliveryDate, 1, 10) BETWEEN date('now') AND date('now', '+30 days')"

    const val PENDING_DELIVERY =
        "$AWAITING_DELIVERY AND substr(lp.estimatedDeliveryDate, 1, 10) < date('now', '-45 days')"

    const val CHILDREN_UNDER_TWO = "substr(hhm.date_of_birth, 1, 10) > date('now', '-2 years')"

    const val EXTERNAL_MEMBER = "hhm.household_id IS NULL"

    /** SK Service Recipient: external members created by SK or with no creator role recorded. */
    val SK_SCOPED_EXTERNAL_CREATOR =
        "(hhm.created_by_role_name IS NULL OR TRIM(hhm.created_by_role_name) = '' " +
            "OR LOWER(hhm.created_by_role_name) = LOWER('${RoleConstant.SHASTIYA_KORMI}'))"

    fun isSkScopedExternalFilter(
        staticFilter: ServiceStaticFilter,
        restrictExternalToSkCreator: Boolean,
    ): Boolean =
        restrictExternalToSkCreator &&
            (
                staticFilter == ServiceStaticFilter.EXTERNAL_MEMBERS ||
                    staticFilter == ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN
                )

    const val IS_ACTIVE = "hhm.isActive = 1"

    const val HAS_HOUSEHOLD = "hhm.household_id IS NOT NULL"

    val FAMILY_PLANNING_BASE =
        """
        hhm.gender = '${DefinedParams.GENDER_FEMALE}'
        AND hhm.marital_status = '${MemberRegistration.MaritalStatus.MARRIED.value}'
        AND substr(hhm.date_of_birth, 1, 10) <= date('now', '-${MemberRegistration.MIN_AGE_PREGNANCY} years')
        AND substr(hhm.date_of_birth, 1, 10) >= date('now', '-${MemberRegistration.MAX_AGE_PREGNANCY} years')
        """.trimIndent()

    val FAMILY_PLANNING_ELIGIBLE = "$FAMILY_PLANNING_BASE AND NOT ($ACTIVE_PREGNANCY)"

    const val HAS_NCD_SERVICE_HISTORY = """
        EXISTS (
            SELECT 1 FROM memberassessmenthistory AS mah
            WHERE (mah.memberId = hhm.id OR (mah.memberFhirId IS NOT NULL AND mah.memberFhirId = hhm.fhir_id))
            AND LOWER(mah.serviceProvided) IN ('ncd', 'bd_ncd')
        )
    """

    const val HAS_CATARACT_SCREENING_HISTORY = """
        EXISTS (
            SELECT 1 FROM memberassessmenthistory AS mah
            WHERE (mah.memberId = hhm.id OR (mah.memberFhirId IS NOT NULL AND mah.memberFhirId = hhm.fhir_id))
            AND LOWER(mah.serviceProvided) = 'cataract'
        )
    """

    const val HAS_EYE_SCREENING_HISTORY = """
        EXISTS (
            SELECT 1 FROM memberassessmenthistory AS mah
            WHERE (mah.memberId = hhm.id OR (mah.memberFhirId IS NOT NULL AND mah.memberFhirId = hhm.fhir_id))
            AND LOWER(mah.serviceProvided) = 'eye_care'
        )
    """

    /** Whether [IS_ACTIVE] should be omitted for this filter in the member list query. */
    fun skipsActiveCheck(staticFilter: ServiceStaticFilter): Boolean =
        staticFilter == ServiceStaticFilter.EXTERNAL_MEMBERS ||
            staticFilter == ServiceStaticFilter.ALL_MEMBERS ||
            staticFilter == ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS

    /** Pregnancy cohort predicate (requires [LATEST_PREGNANCY_JOIN]). */
    fun pregnancyCohortCondition(staticFilter: ServiceStaticFilter): String =
        when (staticFilter) {
            ServiceStaticFilter.FAMILY_PLANNING_COUNSELLING_ELIGIBLE -> FAMILY_PLANNING_ELIGIBLE
            ServiceStaticFilter.PREGNANT_WOMEN -> ACTIVE_PREGNANCY
            ServiceStaticFilter.HIGH_RISK_PREGNANT_WOMEN -> HIGH_RISK_PREGNANT
            ServiceStaticFilter.POSTNATAL_CARE_MOTHERS -> POSTNATAL
            ServiceStaticFilter.EXPECTED_DELIVERIES -> EXPECTED_DELIVERY
            ServiceStaticFilter.PENDING_DELIVERIES -> PENDING_DELIVERY
            else -> error("No pregnancy cohort condition for $staticFilter")
        }

    /** Assessment-history cohort predicate. */
    fun assessmentCohortCondition(staticFilter: ServiceStaticFilter): String =
        when (staticFilter) {
            ServiceStaticFilter.NCD_SERVICES -> HAS_NCD_SERVICE_HISTORY
            ServiceStaticFilter.CATARACT_SCREENING -> HAS_CATARACT_SCREENING_HISTORY
            ServiceStaticFilter.EYE_SCREENING -> HAS_EYE_SCREENING_HISTORY
            else -> error("No assessment cohort condition for $staticFilter")
        }
}
