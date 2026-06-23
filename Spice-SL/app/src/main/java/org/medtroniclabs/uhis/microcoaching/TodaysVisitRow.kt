package org.medtroniclabs.uhis.microcoaching

import com.medtroniclabs.microcoaching.domain.context.TodaysVisit

/**
 * Minimal, PII-free projection of a `FollowUp` row due today — only the clinical-
 * type signal the MicroCoaching SDK needs to match a visit to a coaching module via
 * the synced `assessment_due` trigger bindings. Deliberately carries NO patient name /
 * phone / id. `isPregnant` is derived (an open pregnancy episode), not a raw column.
 */
data class TodaysVisitRow(
    val type: String,
    val encounterType: String?,
    val nextVisitDate: String?,
    val isPregnant: Boolean,
    val villageId: String,
)

/**
 * Map to the SDK's [TodaysVisit] for
 * [com.medtroniclabs.microcoaching.MicroCoachingSDK.onTodaysVisitsUpdated].
 */
fun TodaysVisitRow.toTodaysVisit(): TodaysVisit =
    TodaysVisit(
        type = type,
        encounterType = encounterType,
        dueDateIso = nextVisitDate.orEmpty(),
        isPregnant = isPregnant,
        villageId = villageId,
    )
