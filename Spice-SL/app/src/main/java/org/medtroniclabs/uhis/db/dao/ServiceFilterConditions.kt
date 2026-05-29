package org.medtroniclabs.uhis.db.dao

import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.mappingkey.MemberRegistration

/**
 * SQL condition fragments for service member filters.
 * Used by [MemberDAO.getServiceMembers] and [ServiceMemberCountQueryBuilder].
 */
object ServiceFilterConditions {
    /**
     * Subquery to get the latest pregnancy record for a member.
     * Used by [PREGNANT_WOMEN], [HIGH_RISK_PREGNANT_WOMEN], and related conditions.
     */
    internal const val LATEST_PREGNANCY_SUBQUERY = """
        SELECT pd.id, pd.dateOfDelivery, pd.lastMenstrualPeriod, pd.estimatedDeliveryDate, pd.highRiskPregnantWoman, pd.typeOfAbortion, pd.endAt
        FROM PregnancyDetail AS pd
        WHERE pd.householdMemberLocalId = hhm.id
        ORDER BY pd.endAt DESC, pd.id DESC
        LIMIT 1
    """

    /**
     * Base conditions for an active pregnancy (no delivery, valid LMP, within EDD window).
     */
    private const val ACTIVE_PREGNANCY_CONDITIONS = """
            (lp.lastMenstrualPeriod IS NOT NULL AND lp.lastMenstrualPeriod != '')
            AND (lp.dateOfDelivery IS NULL OR lp.dateOfDelivery = '')
            AND (lp.estimatedDeliveryDate IS NULL OR substr(lp.estimatedDeliveryDate, 1, 10) >= date('now', '-45 days'))
            AND (lp.typeOfAbortion IS NULL OR lp.typeOfAbortion = '')
    """

    /**
     * Condition for pregnant women - checks latest pregnancy has no delivery and valid LMP.
     */
    const val PREGNANT_WOMEN = """
        EXISTS (
            SELECT 1 FROM ($LATEST_PREGNANCY_SUBQUERY) AS lp
            WHERE $ACTIVE_PREGNANCY_CONDITIONS
        )
    """

    /**
     * Condition for high-risk pregnant women (pregnant + high risk flag set).
     */
    const val HIGH_RISK_PREGNANT_WOMEN = """
        EXISTS (
            SELECT 1 FROM ($LATEST_PREGNANCY_SUBQUERY) AS lp
            WHERE $ACTIVE_PREGNANCY_CONDITIONS
            AND (lp.highRiskPregnantWoman IS NOT NULL AND lp.highRiskPregnantWoman != '')
        )
    """

    /**
     * Condition for postnatal care mothers - delivery within last 42 days.
     */
    const val POSTNATAL_MOTHERS = """
        EXISTS (
            SELECT 1 FROM ($LATEST_PREGNANCY_SUBQUERY) AS lp
            WHERE (lp.dateOfDelivery IS NOT NULL AND lp.dateOfDelivery != '')
            AND substr(lp.dateOfDelivery, 1, 10) >= date('now', '-42 days')
        )
    """

    /**
     * Base conditions for delivery tracking (no delivery yet, has valid EDD).
     * Used by [EXPECTED_DELIVERIES] and [PENDING_DELIVERIES].
     */
    private const val AWAITING_DELIVERY_CONDITIONS = """
            (lp.dateOfDelivery IS NULL OR lp.dateOfDelivery = '')
            AND (lp.typeOfAbortion IS NULL OR lp.typeOfAbortion = '')
            AND (lp.estimatedDeliveryDate IS NOT NULL AND lp.estimatedDeliveryDate != '')
    """

    /**
     * Condition for expected deliveries - EDD within next 30 days.
     */
    const val EXPECTED_DELIVERIES = """
        EXISTS (
            SELECT 1 FROM ($LATEST_PREGNANCY_SUBQUERY) AS lp
            WHERE $AWAITING_DELIVERY_CONDITIONS
            AND substr(lp.estimatedDeliveryDate, 1, 10) BETWEEN date('now') AND date('now', '+30 days')
        )
    """

    /**
     * Condition for pending deliveries - EDD more than 45 days overdue.
     */
    const val PENDING_DELIVERIES = """
        EXISTS (
            SELECT 1 FROM ($LATEST_PREGNANCY_SUBQUERY) AS lp
            WHERE $AWAITING_DELIVERY_CONDITIONS
            AND substr(lp.estimatedDeliveryDate, 1, 10) < date('now', '-45 days')
        )
    """

    /**
     * Condition for children under 2 years.
     */
    const val CHILDREN_UNDER_TWO = "substr(hhm.date_of_birth, 1, 10) > date('now', '-2 years')"

    /**
     * Condition for external members (not linked to a household).
     */
    const val EXTERNAL_MEMBER = "hhm.household_id IS NULL"

    /**
     * Condition for active status.
     */
    const val IS_ACTIVE = "hhm.isActive = 1"

    /**
     * Condition for non-external members (linked to a household).
     */
    const val HAS_HOUSEHOLD = "hhm.household_id IS NOT NULL"

    /**
     * Family planning eligibility conditions (gender, marital status, age range).
     * Values are embedded directly since they're constants.
     */
    val FAMILY_PLANNING_BASE =
        """
        hhm.gender = '${DefinedParams.GENDER_FEMALE}'
        AND hhm.marital_status = '${MemberRegistration.MaritalStatus.MARRIED.value}'
        AND substr(hhm.date_of_birth, 1, 10) <= date('now', '-${MemberRegistration.MIN_AGE_PREGNANCY} years')
        AND substr(hhm.date_of_birth, 1, 10) >= date('now', '-${MemberRegistration.MAX_AGE_PREGNANCY} years')
        """.trimIndent()

    /**
     * Full family planning condition (eligible but not currently pregnant).
     */
    val FAMILY_PLANNING =
        """
        $FAMILY_PLANNING_BASE
        AND NOT $PREGNANT_WOMEN
        """.trimIndent()

    /** Member has at least one NCD-related assessment history row (FO/PO service list). */
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
}
