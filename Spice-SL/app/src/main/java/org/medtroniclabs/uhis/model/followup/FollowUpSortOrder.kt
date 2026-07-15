package org.medtroniclabs.uhis.model.followup

/**
 * Sort options for the referred follow-up patient list.
 */
enum class FollowUpSortOrder {
    /** Sort by remaining screening attempts, highest first (most urgent). */
    DEFAULT,

    /** Sort by screening date, most recent first. */
    LATEST_SCREENING_DATE,

    /** Sort by screening date, oldest first. */
    OLDEST_SCREENING_DATE,
}
