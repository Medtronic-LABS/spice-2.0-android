package org.medtroniclabs.uhis.ui.household.adapter

import android.content.Context
import android.graphics.Color
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.textOrDoubleHyphen
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentObservations
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.FamilyPlanning
import org.medtroniclabs.uhis.ui.assessment.PregnancyOutcome
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.household.adapter.MemberAssessmentHistoryAdapterUtil.shouldShowNextFollowUpDate
import org.medtroniclabs.uhis.ui.household.adapter.MemberAssessmentHistoryAdapterUtil.shouldShowReferralStatus

/**
 * Builds the label/value rows shown for a single member assessment history entry.
 *
 * Used by [MemberAssessmentHistoryAdapter] to turn a [MemberAssessmentHistoryEntity]
 * into a list of [SummaryItem]s. Common fields (service name, provider, date, status) are
 * always included; additional rows and visibility rules depend on [MemberAssessmentHistoryEntity.serviceProvided].
 */
object MemberAssessmentHistoryAdapterUtil {
    /**
     * Returns the ordered [SummaryItem] list for [history].
     *
     * Always includes service name (with ANC/PNC visit number when present), provider,
     * visit date, and a service-specific status row. High-risk pregnancy/PNC statuses are
     * highlighted in red. Service-specific extras:
     * - Pregnant women profile: gravida, parity
     * - NCD / cataract: blood pressure, blood glucose
     * - Pregnancy outcome: delivery complications
     * - Family planning: recommended method, desire for children
     *
     * Referral status and next follow-up date are appended only when
     * [shouldShowReferralStatus] / [shouldShowNextFollowUpDate] allow it for the service.
     */
    fun constructHistoryAdapterBindItems(
        context: Context,
        history: MemberAssessmentHistoryEntity,
        memberPregnancyDetails: List<PregnancyDetail>?,
    ): List<SummaryItem> {
        val summaryItems = mutableListOf<SummaryItem>()
        val service = history.serviceProvided?.lowercase().orEmpty()
        val observations = history.observations
        val visitNumber = getVisitNumber(service, observations)
        val serviceName = AssessmentUtil.mapServiceToServiceName(service, context) +
            if (visitNumber.isNotBlank()) {
                " ($visitNumber)"
            } else {
                ""
            }
        summaryItems.add(
            SummaryItem(
                context.getString(R.string.service_name),
                serviceName,
            ),
        )
        summaryItems.add(
            SummaryItem(
                context.getString(R.string.service_provided_by),
                AssessmentUtil.formatServiceProviderDisplay(
                    context,
                    history.serviceProvidedByName,
                    history.serviceProvidedByRole,
                ),
            ),
        )
        val visitDateMillis = DateUtils.getLastMenstrualDate(history.visitDate ?: "").timeInMillis
        summaryItems.add(
            SummaryItem(
                context.getString(R.string.service_date),
                DateUtils.formatDateToDisplayFormat(visitDateMillis) ?: "",
            ),
        )
        val currentStatus = if (service.equals(MenuConstants.FP_MENU_ID, true)) {
            resolveArrayValue(
                context,
                FamilyPlanning.FamilyPlanningMethods,
                R.array.family_planning_methods,
                observations?.familyPlanningMethods,
            )
        } else if (service.equals(MenuConstants.PREGNANCY_OUTCOME, true)) {
            resolveArrayValue(
                context,
                PregnancyOutcome.ModeOfDelivery,
                R.array.mode_of_delivery,
                observations?.modeOfDelivery,
            )
        } else {
            AssessmentUtil.formatServiceHistoryCurrentStatus(
                history.customStatus,
                context,
                history.serviceProvided,
                history.referralStatus,
            )
        }
        val statusColor = if (
            currentStatus?.contains(context.getString(R.string.high_risk_pw), true) == true ||
            currentStatus?.contains(context.getString(R.string.high_risk_pnc), true) == true
        ) {
            Color.RED
        } else {
            null
        }
        val statusLabel = getStatusLabel(context, history)
        summaryItems.add(
            SummaryItem(
                statusLabel,
                CommonUtils.getStringElse(currentStatus, context.getString(R.string.separator_double_hyphen)),
                valueColor = statusColor,
            ),
        )
        when (service) {
            MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase() -> {
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.gravida),
                        observations?.gravida.textOrDoubleHyphen(),
                    ),
                )
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.parity),
                        observations?.parity.textOrDoubleHyphen(),
                    ),
                )
            }

            MenuConstants.NCD_MENU_ID.lowercase(), MenuConstants.CATARACT_MENU_ID.lowercase() -> {
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.blood_pressure),
                        AssessmentUtil.formatServiceHistoryBloodPressure(
                            context,
                            observations?.bp,
                        ),
                    ),
                )
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.blood_glucose),
                        AssessmentUtil.formatServiceHistoryBloodGlucose(
                            context,
                            observations?.bg,
                            observations?.bgType,
                        ),
                    ),
                )
            }

            MenuConstants.PREGNANCY_OUTCOME.lowercase() -> {
                val bracAnc = memberPregnancyDetails?.find { it.pregnancyEpisodeId == observations?.pregnancyEpisodeId }?.ancVisitNo?.toInt() ?: 0
                val otherProvider = CommonUtils.getInteger(observations?.ancVisitsOtherProviders)
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.anc_received),
                        context.getString(
                            R.string.anc_received_value,
                            CommonUtils.formatCountForCurrentLocale(bracAnc),
                            CommonUtils.formatCountForCurrentLocale(bracAnc + otherProvider),
                        ),
                    ),
                )
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.complications_during_delivery),
                        if (AssessmentDefinedParams.YES.equals(observations?.anyComplicationsDuringDelivery.orEmpty(), true)) {
                            context.getString(R.string.present)
                        } else {
                            context.getString(R.string.na)
                        },
                    ),
                )
            }

            MenuConstants.FP_MENU_ID.lowercase() -> {
                val recommendedMethod = FamilyPlanning.getRecommendedFamilyPlanningMethod(
                    context,
                    observations?.desireForChildrenInFuture,
                    CommonUtils.getInteger(observations?.numberOfLivingChildren),
                    observations?.familyPlanningMethods,
                )
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.recommended_method),
                        if (recommendedMethod.isNullOrBlank()) {
                            context.getString(R.string.na)
                        } else {
                            recommendedMethod
                        },
                    ),
                )
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.desire_for_children),
                        resolveArrayValue(
                            context,
                            FamilyPlanning.DesireForChildren,
                            R.array.desire_for_children_options,
                            observations?.desireForChildrenInFuture,
                        ),
                    ),
                )
            }
        }
        if (shouldShowReferralStatus(service)) {
            val referralStatus = AssessmentUtil.getReferralStatus(
                context,
                service,
                history.referralStatus,
            )
            summaryItems.add(
                SummaryItem(
                    context.getString(R.string.referral_status),
                    referralStatus,
                ),
            )
            val referredLocation = AssessmentUtil.getReferredLocation(
                service,
                history.referralFacilityType,
                history.referralStatus,
            )
            if (AssessmentUtil.shouldShowReferredLocation(service, referredLocation)) {
                summaryItems.add(
                    SummaryItem(
                        context.getString(R.string.referred_location),
                        referredLocation ?: context.getString(R.string.separator_double_hyphen),
                    ),
                )
            }
        }
        if (shouldShowNextFollowUpDate(service)) {
            val nextFollowUpDate = AssessmentUtil.getNextFollowUpDate(
                context,
                service,
                history.nextFollowUpDate,
            )
            summaryItems.add(
                SummaryItem(
                    context.getString(R.string.next_follow_up_date),
                    nextFollowUpDate,
                ),
            )
        }
        return summaryItems
    }

    /**
     * Maps an option [id] to its localized display value by matching its position in [ids]
     * against [arrayRes]. Returns a double-hyphen when the id is null, empty, or unmapped.
     */
    private fun resolveArrayValue(
        context: Context,
        ids: List<String>,
        arrayRes: Int,
        id: String?,
    ): String {
        val index = id?.let { ids.indexOf(it) } ?: -1
        return context.resources
            .getStringArray(arrayRes)
            .getOrNull(index)
            .textOrDoubleHyphen()
    }

    /**
     * RMNCH and family-planning services omit referral status from history summaries.
     */
    fun shouldShowReferralStatus(service: String): Boolean =
        when (service) {
            MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase(),
            MenuConstants.PREGNANCY_OUTCOME.lowercase(),
            MenuConstants.FP_MENU_ID.lowercase(),
            -> false

            else -> true
        }

    /**
     *  NCD, cataract, eye care, and RMNCH/FP services omit next follow-up from history summaries.
     */
    fun shouldShowNextFollowUpDate(service: String): Boolean =
        when (service) {
            MenuConstants.NCD_MENU_ID.lowercase(),
            MenuConstants.CATARACT_MENU_ID.lowercase(),
            MenuConstants.EYE_CARE_MENU_ID.lowercase(),
            MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase(),
            MenuConstants.PREGNANCY_OUTCOME.lowercase(),
            MenuConstants.FP_MENU_ID.lowercase(),
            -> false

            else -> true
        }

    /**
     * Returns the ANC or PNC visit number from [observations], or an empty string.
     */
    fun getVisitNumber(
        service: String,
        observations: MemberAssessmentObservations?,
    ): String =
        when (service) {
            MenuConstants.ANC.lowercase() -> {
                observations?.ancVisitNumber.orEmpty()
            }

            MenuConstants.PNC_MOTHER.lowercase() -> {
                observations?.pncVisitNumber.orEmpty()
            }

            else -> {
                ""
            }
        }

    /**
     * Returns the localized status column label for ANC, PNC, FP, or the generic current status.
     */
    fun getStatusLabel(
        context: Context,
        history: MemberAssessmentHistoryEntity,
    ): String {
        val service = history.serviceProvided?.lowercase().orEmpty()
        return when (service) {
            MenuConstants.ANC.lowercase() -> {
                context.getString(R.string.pregnancy_status)
            }

            MenuConstants.PNC_MOTHER.lowercase() -> {
                context.getString(R.string.pnc_status)
            }

            MenuConstants.FP_MENU_ID.lowercase() -> {
                context.getString(R.string.fp_status)
            }

            MenuConstants.PREGNANCY_OUTCOME.lowercase() -> {
                context.getString(R.string.delivery_mode)
            }

            else -> context.getString(R.string.current_status)
        }
    }
}
