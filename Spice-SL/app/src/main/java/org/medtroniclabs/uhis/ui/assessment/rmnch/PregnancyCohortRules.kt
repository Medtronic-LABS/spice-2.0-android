package org.medtroniclabs.uhis.ui.assessment.rmnch

import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kotlin mirror of pregnancy cohort rules in [org.medtroniclabs.uhis.db.dao.ServiceFilterConditions].
 * Keeps service-recipient SQL filters and RMNCH workflow menu gating aligned.
 */
object PregnancyCohortRules {
    const val OVERDUE_GRACE_DAYS = 45L
    const val POSTNATAL_WINDOW_DAYS = 42L

    fun isActivePregnancy(pregnancyDetail: PregnancyDetail): Boolean {
        if (pregnancyDetail.lastMenstrualPeriod.isNullOrBlank()) return false
        if (!pregnancyDetail.dateOfDelivery.isNullOrBlank()) return false
        if (!pregnancyDetail.typeOfAbortion.isNullOrBlank()) return false
        val eddPrefix = pregnancyDetail.estimatedDeliveryDate?.take(10) ?: return true
        val edd = parseDatePrefix(eddPrefix) ?: return true
        val cutoff = LocalDate.now().minusDays(OVERDUE_GRACE_DAYS)
        return edd.isAfter(cutoff)
    }

    fun isPostnatal(pregnancyDetail: PregnancyDetail): Boolean {
        val deliveryPrefix = pregnancyDetail.dateOfDelivery?.take(10) ?: return false
        val deliveryDate = parseDatePrefix(deliveryPrefix) ?: return false
        val cutoff = LocalDate.now().minusDays(POSTNATAL_WINDOW_DAYS)
        return !deliveryDate.isBefore(cutoff)
    }

    /** Active or postnatal pregnancy — still in an RMNCH care episode. */
    fun isRelevantForRmnchMenus(pregnancyDetail: PregnancyDetail): Boolean = isActivePregnancy(pregnancyDetail) || isPostnatal(pregnancyDetail)

    private fun parseDatePrefix(dateStr: String): LocalDate? {
        val formats = listOf(
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy,
            DateUtils.DATE_FORMAT_yyyyMMdd,
        )
        for (format in formats) {
            try {
                val date = DateUtils.convertStringToDate(dateStr, format) ?: continue
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }
}
