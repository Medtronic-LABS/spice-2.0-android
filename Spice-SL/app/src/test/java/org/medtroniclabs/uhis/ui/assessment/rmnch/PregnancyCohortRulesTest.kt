package org.medtroniclabs.uhis.ui.assessment.rmnch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import java.time.LocalDate

class PregnancyCohortRulesTest {
    @Test
    fun isActivePregnancy_falseWhenLmpMissing() {
        val detail = detail(lmp = null, edd = daysAgo(10))
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_falseWhenDeliveryRecorded() {
        val detail = detail(edd = daysAgo(10), dateOfDelivery = daysAgo(5))
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_falseWhenAbortionRecorded() {
        val detail = detail(edd = daysAgo(10), typeOfAbortion = "spontaneous")
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_trueWhenLmpPresentAndEddMissing() {
        val detail = detail(edd = null)
        assertTrue(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_trueWhenEddIsInFuture() {
        val detail = detail(edd = daysFromNow(14))
        assertTrue(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_trueWhenEddIsYesterday_pendingDeliveryWindow() {
        val detail = detail(edd = daysAgo(1))
        assertTrue(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_trueWhenEddIs44DaysAgo_lastDayOfGraceWindow() {
        val detail = detail(edd = daysAgo(44))
        assertTrue(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_falseWhenEddIsExactly45DaysAgo_firstDayAfterGrace() {
        val detail = detail(edd = daysAgo(45))
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isActivePregnancy_falseWhenEddIsWellPastGrace() {
        val detail = detail(edd = daysAgo(60))
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
    }

    @Test
    fun isPostnatal_falseWhenNoDeliveryDate() {
        val detail = detail(edd = daysAgo(10))
        assertFalse(PregnancyCohortRules.isPostnatal(detail))
    }

    @Test
    fun isPostnatal_trueWhenDeliveryWasToday() {
        val detail = detail(dateOfDelivery = daysAgo(0))
        assertTrue(PregnancyCohortRules.isPostnatal(detail))
    }

    @Test
    fun isPostnatal_trueWhenDeliveryWas42DaysAgo_lastDayOfPncWindow() {
        val detail = detail(dateOfDelivery = daysAgo(42))
        assertTrue(PregnancyCohortRules.isPostnatal(detail))
    }

    @Test
    fun isPostnatal_falseWhenDeliveryWas43DaysAgo() {
        val detail = detail(dateOfDelivery = daysAgo(43))
        assertFalse(PregnancyCohortRules.isPostnatal(detail))
    }

    @Test
    fun isRelevantForRmnchMenus_trueForActivePregnancy() {
        val detail = detail(edd = daysAgo(10))
        assertTrue(PregnancyCohortRules.isRelevantForRmnchMenus(detail))
    }

    @Test
    fun isRelevantForRmnchMenus_trueForPostnatalEvenWhenEddWasLongAgo() {
        val detail = detail(
            edd = daysAgo(120),
            dateOfDelivery = daysAgo(10),
        )
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
        assertTrue(PregnancyCohortRules.isPostnatal(detail))
        assertTrue(PregnancyCohortRules.isRelevantForRmnchMenus(detail))
    }

    @Test
    fun isRelevantForRmnchMenus_falseForStaleUndeliveredPregnancy() {
        val detail = detail(edd = daysAgo(46))
        assertFalse(PregnancyCohortRules.isActivePregnancy(detail))
        assertFalse(PregnancyCohortRules.isPostnatal(detail))
        assertFalse(PregnancyCohortRules.isRelevantForRmnchMenus(detail))
    }

    private fun detail(
        lmp: String? = "2020-01-01",
        edd: String? = null,
        dateOfDelivery: String? = null,
        typeOfAbortion: String? = null,
    ) = PregnancyDetail(
        householdMemberLocalId = 1L,
        lastMenstrualPeriod = lmp,
        estimatedDeliveryDate = edd,
        dateOfDelivery = dateOfDelivery,
        typeOfAbortion = typeOfAbortion,
    )

    private fun daysAgo(days: Long): String = format(LocalDate.now().minusDays(days))

    private fun daysFromNow(days: Long): String = format(LocalDate.now().plusDays(days))

    private fun format(date: LocalDate): String = date.toString()
}
